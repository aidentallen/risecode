package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.EnchantEffectModule;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.render.ItemOutlineRenderer;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.util.ColorUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Native image comparisons prove the effect draws an exterior ring without recoloring item faces. */
public final class ColorOutlineIntegrationTest {
    private static int checks;
    private ColorOutlineIntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var client = Client.INSTANCE;
        context.runOnClient(mc -> {
            mc.gui.setScreen(null); client.getModuleManager().disableAll(); client.getThemeManager().reset();
            client.getHudRenderer().setEnabled(false); mc.options.bobView().set(false); mc.options.glintStrength().set(0.0);
            mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.guiScale().set(2);
        });
        world.getServer().runCommand("gamemode creative @a");
        world.getServer().runCommand("fill 96 70 96 104 76 104 minecraft:stone hollow");
        world.getServer().runCommand("tp @a 100.5 71 100.5 0 0");
        world.getServer().runCommand("time set noon"); world.getConnection().waitForChunksRender(); context.waitTicks(20);
        var enchant = client.getModuleManager().getByClass(EnchantEffectModule.class).orElseThrow();
        var fullbright = client.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
        context.runOnClient(mc -> {
            fullbright.getMode().setValue(FullBrightModule.Mode.LIGHTMAP); fullbright.setEnabled(true);
            flag(enchant, "Overlay", false); flag(enchant, "WorldItems", false); number(enchant, "OutlineWidth", 6); number(enchant, "OutlineGlow", 0); number(enchant, "OutlineIntensity", 1);
            var color = color(enchant, "OutlineColor"); color.setValue(0xFFFF11FF); color.setMode(ColorSetting.Mode.SOLID);
        });
        for (String item : new String[] {"diamond_axe", "diamond_sword", "trident"}) {
            context.runOnClient(mc -> enchant.setEnabled(false));
            world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:" + item + "[minecraft:enchantment_glint_override=true]");
            context.waitTicks(35);
            context.waitFor(mc -> mc.player.getAttackStrengthScale(1) == 1 && mc.player.getAttackAnim(1) == 0
                    && settledHeight(mc.getEntityRenderDispatcher().getItemInHandRenderer()), 240);
            context.runOnClient(mc -> { mc.player.xBobO = mc.player.xBob = mc.player.getXRot(); mc.player.yBobO = mc.player.yBob = mc.player.getYRot(); });
            context.waitTicks(2); context.takeScreenshot("outline-base-" + item);
            long frames = ItemOutlineRenderer.getCompositeCount(); context.runOnClient(mc -> enchant.setEnabled(true)); context.waitTicks(12);
            context.takeScreenshot("outline-silhouette-" + item);
            check(ItemOutlineRenderer.getCompositeCount() > frames, "real mask/dilation/composite passes " + item);
            AtomicBoolean ready = new AtomicBoolean();
            context.runOnClient(mc -> Screenshot.takeScreenshot(ItemOutlineRenderer.getMaskTarget(), image -> {
                try (image) { image.writeToFile(Path.of("screenshots", "outline-mask-" + item + ".png")); ready.set(true); }
                catch (IOException failure) { throw new AssertionError("Native silhouette capture", failure); }
            }));
            context.waitFor(mc -> ready.get(), 200);
            AtomicBoolean horizontalReady = new AtomicBoolean();
            context.runOnClient(mc -> Screenshot.takeScreenshot(ItemOutlineRenderer.getHorizontalTarget(), image -> {
                try (image) { image.writeToFile(Path.of("screenshots", "outline-horizontal-" + item + ".png")); horizontalReady.set(true); }
                catch (IOException failure) { throw new AssertionError("Native dilation capture", failure); }
            }));
            context.waitFor(mc -> horizontalReady.get(), 200);
            compare(item);
            if(item.equals("diamond_axe")){
                world.getServer().runCommand("enchant @a minecraft:unbreaking 1");context.waitTicks(6);
                context.runOnClient(mc->mc.player.getMainHandItem().set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,false));context.waitTicks(4);
                long actual=ItemOutlineRenderer.getCompositeCount();context.waitTicks(8);check(ItemOutlineRenderer.getCompositeCount()>actual+2,"Regular enchanted geometry remains outlined with native glint disabled");
            }
        }
        check(ItemOutlineRenderer.specialSubmissions()>0,"Special model geometry enters the silhouette mask");
        world.getServer().runCommand("enchant @a minecraft:loyalty 1");context.waitTicks(6);
        context.runOnClient(mc->{
            var state=new net.minecraft.client.renderer.item.ItemStackRenderState();mc.getItemModelResolver().updateForLiving(state,mc.player.getMainHandItem(),net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,mc.player);
            var metadata=((dev.riserecode.render.ItemVisualContext.State)(Object)state).risecode$itemContext();
            check(metadata.enchanted()&&metadata.foil()&&metadata.display().firstPerson(),"Actual trident enchantment and display context retained");
        });
        long specialBefore=ItemOutlineRenderer.specialSubmissions();context.getInput().holdMouse(1);context.waitTicks(12);context.takeScreenshot("outline-charging-trident");
        check(ItemOutlineRenderer.specialSubmissions()>specialBefore,"Charging trident submits its special-model mask");context.getInput().releaseMouse(1);context.waitTicks(6);
        context.runOnClient(mc->mc.player.getMainHandItem().set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,false));context.waitTicks(4);
        long actualSpecial=ItemOutlineRenderer.specialSubmissions();context.waitTicks(8);check(ItemOutlineRenderer.specialSubmissions()>actualSpecial+2,"Special enchanted models remain outlined with native glint disabled");
        int allocations = ItemOutlineRenderer.getAllocationCount(); context.waitTicks(10);
        check(ItemOutlineRenderer.getAllocationCount() == allocations, "outline targets are reused between frames");
        context.runOnClient(mc -> {
            var color = color(enchant, "OutlineColor"); color.setMode(ColorSetting.Mode.GRADIENT); color.setValue(0xFF22D3EE);
            color.setSecondary(0xFFD946EF); color.setDirection(ColorUtil.GradientDirection.VERTICAL); color.setSpeed(0); color.setSpacing(140);
        });
        context.waitTicks(8); context.takeScreenshot("outline-custom-gradient");
        var capturedPaint = color(enchant, "OutlineColor").paint();
        context.runOnClient(mc -> { check(client.getProfileManager().saveProfile("PaintTest"), "profile saves per-color gradient"); color(enchant, "OutlineColor").reset(); check(client.getProfileManager().loadProfile("PaintTest"), "profile loads per-color gradient"); });
        check(color(enchant, "OutlineColor").paint().equals(capturedPaint), "profile restores full outline gradient policy");
        // Exercise the module's picker through real mouse input and the Screen keyboard routing.
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> gui().isOpen(), 240);
        context.runOnClient(mc -> { gui().selectPage(ClickGui.Page.SEARCH); gui().getSearchField().setValue("EnchantEffect"); }); context.waitTicks(8);
        ModuleButton button = context.computeOnClient(mc -> gui().getModuleButton("EnchantEffect"));
        var bounds = button.getBounds(); click(context, bounds.x() + 10, bounds.y() + 12); // toggle is not needed; right-click below opens settings
        if (!enchant.isEnabled()) { context.runOnClient(mc -> enchant.setEnabled(true)); }
        click(context, bounds.x() + 10, bounds.y() + 12, 1); context.waitTicks(12);
        ColorPicker picker = context.computeOnClient(mc -> (ColorPicker) button.getSettingControls().stream().filter(c -> c.setting().getName().equals("OutlineColor")).findFirst().orElseThrow().component());
        bounds = picker.getBounds(); click(context, bounds.x() + 10, bounds.y() + 10); context.waitTicks(16);
        check(picker.isExpanded(), "every module color opens the full custom picker");
        bounds = picker.getBounds(); int before = color(enchant, "OutlineColor").getValue();
        click(context, bounds.x() + (bounds.width() - 20) * 0.75, bounds.y() + 42 + 12);
        check(color(enchant, "OutlineColor").getValue() != before, "real HSV input updates outline paint");
        bounds = picker.getBounds(); click(context, bounds.x() + bounds.width() * 0.5, bounds.y() + 99);
        check(Math.abs(color(enchant, "OutlineColor").getAlpha() - 128) <= 1, "alpha slider supports translucent colors");
        bounds = picker.getBounds(); click(context, bounds.x() + bounds.width() * 0.5, bounds.y() + 117);
        context.runOnClient(mc -> {
            check(mc.gui.screen().keyPressed(new KeyEvent(65, 0, 2)), "hex field receives Ctrl+A through ClickGUI");
            for (char c : "FF35D7B0".toCharArray()) { check(mc.gui.screen().charTyped(new CharacterEvent(c)), "hex character routing"); }
            mc.gui.screen().keyPressed(new KeyEvent(257, 0, 0));
        });
        check(color(enchant, "OutlineColor").getValue() == 0xFF35D7B0, "exact hex color applies immediately");
        bounds = picker.getBounds(); click(context, bounds.x() + 5, bounds.y() + 135);
        check(color(enchant, "OutlineColor").getMode() == ColorSetting.Mode.RAINBOW, "per-color mode cycles independently of global theme");
        context.takeScreenshot("module-custom-color-picker");
        context.runOnClient(mc -> { gui().selectPage(ClickGui.Page.APPEARANCE); gui().getThemeEditor().reveal("Surface Color"); }); context.waitTicks(20);
        check(context.computeOnClient(mc -> gui().getThemeEditor().getControl("Surface Color") instanceof ColorPicker), "interface backgrounds have the same picker");
        context.runOnClient(mc -> { var paint = client.getThemeManager().colorSetting("Surface Color"); paint.setMode(ColorSetting.Mode.GRADIENT); paint.setValue(0xFF18212D); paint.setSecondary(0xFF283348); paint.setSpeed(0); });
        context.waitTicks(8); context.takeScreenshot("interface-custom-color-picker");
        context.runOnClient(mc -> { mc.gui.setScreen(null); client.getModuleManager().disableAll(); client.getThemeManager().reset(); client.getHudRenderer().setEnabled(true); mc.options.glintStrength().set(0.75); });
        client.getLogger().info("Color picker / whole-item outline native checks passed ({} assertions)", checks);
    }
    private static boolean settledHeight(net.minecraft.client.renderer.ItemInHandRenderer renderer) {
        try {
            java.lang.reflect.Field current = renderer.getClass().getDeclaredField("mainHandHeight"), previous = renderer.getClass().getDeclaredField("oMainHandHeight");
            current.setAccessible(true); previous.setAccessible(true);
            return current.getFloat(renderer) == 1 && previous.getFloat(renderer) == 1;
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Read native held-item animation state", failure); }
    }
    private static void compare(String item) {
        try (NativeImage plain = image("outline-base-" + item); NativeImage outlined = image("outline-silhouette-" + item); NativeImage mask = image("outline-mask-" + item)) {
            int interior = 0, changed = 0, border = 0;
            for (int y = 5; y < plain.getHeight() - 5; y++) { for (int x = 5; x < plain.getWidth() - 5; x++) {
                boolean inside = true;
                for (int[] offset : new int[][] {{0,0},{-4,0},{4,0},{0,-4},{0,4}}) { if ((mask.getPixel(x + offset[0], y + offset[1]) & 255) < 128) { inside = false; break; } }
                int a = plain.getPixel(x,y), b = outlined.getPixel(x,y);
                if (inside) { interior++; if (difference(a,b) > 3) { changed++; } }
                else if ((mask.getPixel(x,y) & 255) < 128 && (b >> 16 & 255) > 180 && (b & 255) > 180 && (b >> 8 & 255) < 100 && difference(a,b) > 20) { border++; }
            } }
            check(interior > 10000, "whole visible item is captured in mask " + item);
            check(changed < Math.max(4, interior / 1000), "item interior stays unchanged " + item + ": " + changed + "/" + interior);
            check(border > 1000, "visible exterior outline hugs silhouette " + item + ": " + border);
            Client.INSTANCE.getLogger().info("Silhouette {}: {} interior pixels, {} changed interior pixels, {} exterior border pixels", item, interior, changed, border);
        } catch (IOException failure) { throw new AssertionError("Read outline screenshots", failure); }
    }
    private static int difference(int a, int b) { return Math.max(Math.abs((a >> 16 & 255) - (b >> 16 & 255)), Math.max(Math.abs((a >> 8 & 255) - (b >> 8 & 255)), Math.abs((a & 255) - (b & 255)))); }
    private static NativeImage image(String name) throws IOException { try (var files = Files.list(Path.of("screenshots"))) { var path = files.filter(p -> p.getFileName().toString().contains(name)).findFirst().orElseThrow(); try (var stream = Files.newInputStream(path)) { return NativeImage.read(stream); } } }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static ColorSetting color(Module module, String name) { return (ColorSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow(); }
    private static void flag(Module module, String name, boolean value) { ((BooleanSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static void number(Module module, String name, double value) { ((DoubleSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static void click(ClientGameTestContext context, double x, double y) { click(context, x, y, 0); }
    private static void click(ClientGameTestContext context, double x, double y, int button) {
        double[] point = context.computeOnClient(mc -> new double[] {x * gui().getCanvasScale() * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(), y * gui().getCanvasScale() * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight()});
        context.getInput().setCursorPos(point[0], point[1]); context.waitTick(); context.getInput().pressMouse(button); context.waitTicks(3);
    }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
}
