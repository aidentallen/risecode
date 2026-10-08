package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.event.ItemRenderEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.*;
import dev.riserecode.module.modules.hud.HudModule;
import dev.riserecode.render.ItemPhysicsState;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.hud.HudEditorScreen;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.ColorUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Exercises native camera/input, untouched server state, shader draws and persistent HUD editing. */
public final class Phase8IntegrationTest {
    private static int assertions;
    private Phase8IntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        context.getInput().resizeWindow(2048, 1152);
        var client = Client.INSTANCE;
        var modules = client.getModuleManager();
        context.runOnClient(mc -> {
            modules.disableAll(); client.getThemeManager().reset(); mc.options.guiScale().set(2);
            mc.options.bobView().set(false); mc.options.setCameraType(CameraType.FIRST_PERSON);
            client.getHudRenderer().setEnabled(true);
        });
        // An enclosed room in the disposable integrated test world has zero block and sky light.
        world.getServer().runCommand("fill 96 70 96 104 76 104 minecraft:stone hollow");
        world.getServer().waitFor(server -> server.overworld().getBrightness(LightLayer.SKY, new BlockPos(100, 71, 100)) == 0, 400);
        world.getServer().runCommand("tp @a 100.5 71 100.5 0 0");
        world.getServer().runCommand("time set midnight");
        world.getConnection().waitForChunksRender(); context.waitTicks(25);
        context.waitFor(mc -> mc.player.getX() > 99 && mc.player.getX() < 102);
        context.runOnClient(mc -> mc.options.gamma().set(0.0)); context.waitTicks(8);
        context.takeScreenshot("phase8-cave-lighting-check");
        String caveStatus = context.computeOnClient(mc -> mc.player.blockPosition() + " block=" + mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()) + " sky=" + mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()));
        client.getLogger().info("Cave setup: {}", caveStatus);
        context.waitFor(mc -> mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()) == 0 && mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()) == 0, 400);
        check(context.computeOnClient(mc -> mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()) == 0
                && mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition()) == 0), "test cave has no light sources");
        var fullbright = module(context, FullBrightModule.class);
        Object original = context.computeOnClient(mc -> mc.gameRenderer.levelLightmap());
        context.takeScreenshot("phase8-cave-dark"); double dark = average("phase8-cave-dark");
        context.runOnClient(mc -> { fullbright.getMode().setValue(FullBrightModule.Mode.LIGHTMAP); fullbright.setEnabled(true); });
        context.waitTicks(10);
        check(context.computeOnClient(mc -> mc.gameRenderer.levelLightmap() != original && mc.options.gamma().get() == 0), "lightmap mode substitutes texture without changing gamma");
        context.takeScreenshot("phase8-cave-fullbright"); double bright = average("phase8-cave-fullbright");
        check(bright > dark * 2 && bright > 25, "zero-light cave becomes visibly illuminated");
        context.runOnClient(mc -> fullbright.setEnabled(false)); context.waitTicks(8);
        check(context.computeOnClient(mc -> mc.gameRenderer.levelLightmap() == original && mc.options.gamma().get() == 0), "native lightmap identity and gamma restore exactly");
        context.takeScreenshot("phase8-cave-restored"); double restored = average("phase8-cave-restored");
        check(Math.abs(restored - dark) < 3, "disabled cave returns to original darkness");
        context.runOnClient(mc -> {
            fullbright.getMode().setValue(FullBrightModule.Mode.GAMMA); fullbright.getBrightness().setValue(0.7); fullbright.setEnabled(true);
        });
        context.waitTick(); check(context.computeOnClient(mc -> mc.options.gamma().get() == 700), "gamma mode bypasses slider clamp");
        context.runOnClient(mc -> { fullbright.setEnabled(false); fullbright.getMode().setValue(FullBrightModule.Mode.LIGHTMAP); fullbright.setEnabled(true); });

        var overlay = module(context, BlockOverlayModule.class); context.runOnClient(mc -> overlay.setEnabled(true));
        context.waitFor(mc -> mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK && client.getWorldRenderer().getQuads().size() == 6);
        check(context.computeOnClient(mc -> client.getWorldRenderer().getLines().size() == 12), "targeted block has twelve independent edges");
        context.waitTicks(8); context.takeScreenshot("phase8-block-overlay");
        context.runOnClient(mc -> flag(overlay, "OverlayEnabled", false)); context.waitFor(mc -> client.getWorldRenderer().getQuads().isEmpty());
        check(context.computeOnClient(mc -> client.getWorldRenderer().getLines().size() == 12), "fill disabled preserves outline");
        context.runOnClient(mc -> { flag(overlay, "OverlayEnabled", true); flag(overlay, "OutlineEnabled", false); });
        context.waitFor(mc -> client.getWorldRenderer().getLines().isEmpty() && client.getWorldRenderer().getQuads().size() == 6);
        context.runOnClient(mc -> flag(overlay, "OutlineEnabled", true));

        var weather = module(context, WeatherChangerModule.class);
        float serverRain = world.getServer().computeOnServer(server -> server.overworld().getRainLevel(1));
        context.runOnClient(mc -> { choice(weather, "Weather", WeatherChangerModule.Weather.THUNDER); weather.setEnabled(true); }); context.waitTicks(4);
        check(context.computeOnClient(mc -> mc.level.getRainLevel(1) == 1 && mc.level.getThunderLevel(1) == 1), "local thunder and rain override");
        check(world.getServer().computeOnServer(server -> server.overworld().getRainLevel(1)) == serverRain, "server weather remains unchanged");
        context.runOnClient(mc -> { choice(weather, "Weather", WeatherChangerModule.Weather.CLEAR); }); context.waitTick();
        check(context.computeOnClient(mc -> mc.level.getRainLevel(1) == 0 && mc.level.getThunderLevel(1) == 0), "weather setting updates immediately");
        context.runOnClient(mc -> weather.setEnabled(false));

        // Camera input is consumed locally; body position and yaw stay vanilla.
        var freelook = module(context, FreeLookModule.class);
        var freecam = module(context, FreeCamModule.class);
        context.runOnClient(mc -> freelook.setEnabled(true));
        float yaw = context.computeOnClient(mc -> mc.player.getYRot());
        context.getInput().holdKey(InputConstants.KEY_LALT); context.waitTicks(3);
        check(context.computeOnClient(mc -> freelook.isHeld() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK), "hold key enters freelook perspective");
        Vec3 eye = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position());
        context.getInput().moveCursor(180, 0); context.waitTicks(2);
        context.getInput().moveCursor(120, 0); context.waitTicks(3);
        context.runOnClient(mc -> client.getLogger().info("Freelook native input: held={} bodyYaw={} cameraYaw={} grabbed={}", freelook.isHeld(), mc.player.getYRot(), mc.gameRenderer.mainCamera().yRot(), mc.mouseHandler.isMouseGrabbed()));
        check(context.computeOnClient(mc -> mc.player.getYRot() == yaw && Math.abs(mc.gameRenderer.mainCamera().yRot() - yaw) > 1), "freelook rotates camera without rotating body");
        check(context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position().distanceTo(eye) > 0.1), "freelook third-person camera orbits the player");
        context.getInput().releaseKey(InputConstants.KEY_LALT); context.waitTicks(3);
        check(context.computeOnClient(mc -> !freelook.isHeld() && mc.options.getCameraType() == CameraType.FIRST_PERSON), "freelook release restores original view");
        context.runOnClient(mc -> { freelook.setEnabled(false); freecam.setEnabled(true); });
        Vec3 playerPosition = context.computeOnClient(mc -> mc.player.position());
        Vec3 cameraPosition = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position());
        context.getInput().holdKey(InputConstants.KEY_W); context.getInput().holdKey(InputConstants.KEY_SPACE); context.waitTicks(8);
        context.getInput().releaseKey(InputConstants.KEY_W); context.getInput().releaseKey(InputConstants.KEY_SPACE);
        check(context.computeOnClient(mc -> mc.player.position().distanceTo(playerPosition) < 0.05 && mc.gameRenderer.mainCamera().position().distanceTo(cameraPosition) > 1), "freecam moves independently while player remains in place");
        context.runOnClient(mc -> freecam.setEnabled(false)); context.waitTicks(4);
        check(context.computeOnClient(mc -> mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition()) < 0.02), "freecam disable restores native player camera");

        // Server-spawned drops exercise the native renderer rather than fabricated render state.
        world.getServer().runCommand("summon minecraft:item 100.5 71 102 {Item:{id:\"minecraft:diamond\",count:1},PickupDelay:32767,Tags:[\"phase8_drop\"]}");
        world.getServer().runCommand("give @a minecraft:diamond_sword[minecraft:enchantment_glint_override=true]");
        context.waitTicks(20);
        var physics = module(context, ItemPhysicsModule.class); var enchant = module(context, EnchantEffectModule.class);
        AtomicInteger flatRenders = new AtomicInteger(), styledItems = new AtomicInteger();
        var itemAudit = client.getEventBus().subscribe(ItemRenderEvent.class, -100, true, event -> {
            if (event.isCancelled() && ((ItemPhysicsState) event.state()).riserecode$onGround()) { flatRenders.incrementAndGet(); }
        });
        var enchantAudit = client.getEventBus().subscribe(EnchantmentRenderEvent.class, -100, false, event -> {
            if (event.style() != null && event.style().outline() && event.style().overlay()) { styledItems.incrementAndGet(); }
        });
        try {
            context.runOnClient(mc -> {
                physics.setEnabled(true); flag(enchant, "Overlay", true); enchant.setEnabled(true);
                for (int slot = 0; slot < 9; slot++) {
                    if (mc.player.getInventory().getItem(slot).is(net.minecraft.world.item.Items.DIAMOND_SWORD)) { mc.player.getInventory().setSelectedSlot(slot); break; }
                }
                mc.player.setXRot(25); mc.player.setYRot(0); mc.player.setOldPosAndRot();
            });
            context.waitFor(mc -> flatRenders.get() > 0 && styledItems.get() > 0);
            context.waitTicks(8); context.takeScreenshot("phase8-item-physics-enchant");
            check(flatRenders.get() > 0 && styledItems.get() > 0, "item mesh transforms and enchant shaders receive real render submissions");
            context.runOnClient(mc -> { number(enchant, "GlintSpeed", 2); number(enchant, "GlintScale", 0.7); });
            context.waitTicks(8); context.takeScreenshot("phase8-enchant-live-settings");
            check(world.getServer().computeOnServer(server -> java.util.stream.StreamSupport.stream(server.overworld().getAllEntities().spliterator(), false)
                    .anyMatch(entity -> entity.entityTags().contains("phase8_drop") && entity.onGround())), "dropped item keeps ordinary server physics");
        } finally { itemAudit.close(); enchantAudit.close(); }

        context.runOnClient(mc -> {
            for (Module module : modules.getModules()) { if (module instanceof HudModule) { module.setEnabled(true); } }
            choice(client.getThemeManager(), "Gradient Mode", ColorUtil.GradientMode.RAINBOW);
            choice(client.getThemeManager(), "Gradient Direction", ColorUtil.GradientDirection.VERTICAL);
            ((DoubleSetting) client.getThemeManager().getSetting("Gradient Speed")).setValue(0.4);
            client.getThemeManager().changed();
        });
        world.getServer().runCommand("effect give @a minecraft:speed 90 0 true"); context.waitTicks(10);
        context.runOnClient(mc -> mc.player.setXRot(0));
        context.waitTicks(15); context.takeScreenshot("phase8-gradient-arraylist");
        context.getInput().pressKey(InputConstants.KEY_F1);
        check(context.computeOnClient(mc -> mc.gui.hud.isHidden()), "native HUD can be hidden");
        double hiddenTime = context.computeOnClient(mc -> dev.riserecode.ui.theme.Theme.gradientTime()); context.waitTicks(8);
        check(context.computeOnClient(mc -> dev.riserecode.ui.theme.Theme.gradientTime() > hiddenTime), "frame gradient clock keeps running while HUD extraction is hidden");
        context.getInput().pressKey(InputConstants.KEY_F1);
        int color = context.computeOnClient(mc -> client.getThemeManager().getGradientColor(20, dev.riserecode.ui.theme.Theme.gradientTime()));
        context.waitTicks(15);
        check(context.computeOnClient(mc -> client.getThemeManager().getGradientColor(20, dev.riserecode.ui.theme.Theme.gradientTime()) != color), "gradient changes smoothly with live frame time");
        context.takeScreenshot("phase8-gradient-arraylist-later");
        var sprint = module(context, SprintModule.class);
        context.runOnClient(mc -> { sprint.setEnabled(true); weather.setEnabled(true); });
        context.waitFor(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint || e.module() == weather).count() == 2
                && client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint || e.module() == weather).allMatch(e -> e.visibility() > 0 && e.visibility() < 1), 100);
        check(context.computeOnClient(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint || e.module() == weather)
                .allMatch(e -> e.visibility() > 0 && e.visibility() < 1)), "two new array entries animate concurrently");
        double entering = context.computeOnClient(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint).findFirst().orElseThrow().visibility());
        context.runOnClient(mc -> sprint.setEnabled(false));
        double retargeted = context.computeOnClient(mc -> client.getHudRenderer().getEntries().stream().filter(e -> e.module() == sprint).findFirst().orElseThrow().visibility());
        check(retargeted <= entering + 0.25 && retargeted > 0, "mid-flight disable retains visible entry");

        command(context, ".hud"); context.waitForScreen(HudEditorScreen.class); context.waitTicks(10);
        check(context.computeOnClient(mc -> editor().isPauseScreen() && client.getHudRenderer().getBoxes().size() == 7), "HUD editor pauses and previews every element");
        var fps = context.computeOnClient(mc -> client.getHudRenderer().getElements().stream().filter(e -> e.id().equals("FPS")).findFirst().orElseThrow());
        double oldX = fps.layout().x();
        moveGui(context, () -> box("FPS"), 0.5, 0.5); context.getInput().holdMouse(0);
        setGuiCursor(context, 512, 288); context.waitTicks(5);
        check(context.computeOnClient(mc -> editor().isDraggingElement()), "real mouse captures HUD drag");
        context.takeScreenshot("phase8-hud-editor-dragged");
        context.getInput().releaseMouse(0); context.waitTicks(5);
        check(fps.layout().x() > oldX + 0.1, "drag changes normalized HUD anchor");
        double savedX = fps.layout().x(), savedY = fps.layout().y();
        moveGui(context, () -> editor().getControls().getFirst().getBounds(), 0.35, 0.75);
        context.getInput().holdMouse(0);
        moveGui(context, () -> editor().getControls().getFirst().getBounds(), 0.45, 0.75);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(mc -> editor().getControls().getFirst() instanceof Slider && fps.layout().scale.getValue() > 1.5), "editor context scale slider updates live through mouse input");
        moveGui(context, () -> editor().getControls().get(3).getBounds(), 0.5, 0.5); context.getInput().pressMouse(0);
        check(fps.layout().alignment.getValue() == HudLayout.Alignment.CENTER, "editor context alignment control");
        context.runOnClient(mc -> { fps.layout().scale.setValue(1.35); fps.layout().alignment.setValue(HudLayout.Alignment.CENTER); fps.layout().background.setValue(HudLayout.Background.BLUR); });
        context.waitTicks(8); context.takeScreenshot("phase8-hud-editor-customized");
        command(context, ".profile save Phase8");
        context.runOnClient(mc -> { fps.layout().position(0.1, 0.1); ((DoubleSetting) client.getThemeManager().getSetting("Animation Speed")).setValue(2.0); choice(client.getThemeManager(), "Gradient Mode", ColorUtil.GradientMode.STATIC); });
        command(context, ".profile load Phase8");
        check(fps.layout().x() == savedX && fps.layout().y() == savedY && fps.layout().scale.getValue() == 1.35, "profile restores HUD position and element styling");
        check(context.computeOnClient(mc -> client.getThemeManager().choice("Gradient Mode", ColorUtil.GradientMode.class) == ColorUtil.GradientMode.RAINBOW
                && client.getThemeManager().number("Animation Speed") == 1), "profile restores gradient and animation speed");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(mc -> mc.gui.screen() == null);
        check(Files.exists(client.getConfigManager().getFile()), "editor exit saves config");
        command(context, ".hudedit"); context.waitForScreen(HudEditorScreen.class);
        double editedX = savedX + 0.05;
        context.runOnClient(mc -> fps.layout().position(editedX, savedY));
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> mc.gui.screen() == null);
        context.runOnClient(mc -> fps.layout().position(0, 0)); command(context, ".profile load Phase8");
        check(fps.layout().x() == editedX && fps.layout().y() == savedY, "editor exit persists the selected named profile without another save command");
        context.runOnClient(mc -> { fps.layout().position(0, 0); client.getConfigManager().load(); });
        check(fps.layout().x() == editedX && fps.layout().y() == savedY, "layout survives main config reload");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitForScreen(ClickGui.class); context.waitFor(mc -> client.getClickGui().isOpen(), 240);
        context.runOnClient(mc -> client.getClickGui().selectPage(ClickGui.Page.HUD)); context.waitTicks(8);
        context.takeScreenshot("phase8-hud-clickgui");
        double canvas = context.computeOnClient(mc -> client.getClickGui().getCanvasScale());
        UIComponent.Bounds edit = context.computeOnClient(mc -> client.getClickGui().getEditHudButton().getBounds());
        setGuiCursor(context, edit.centerX() * canvas, edit.centerY() * canvas);
        context.getInput().pressMouse(0); context.waitForScreen(HudEditorScreen.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitFor(mc -> mc.gui.screen() == null);
        context.runOnClient(mc -> { modules.disableAll(); client.getThemeManager().save(); client.getConfigManager().save(); });
        context.runOnClient(mc -> {
            mc.options.gamma().set(0.23); fullbright.getMode().setValue(FullBrightModule.Mode.GAMMA); fullbright.getBrightness().setValue(1.0); fullbright.setEnabled(true);
            check(mc.options.gamma().get() == 1000, "shutdown fixture has an active gamma override");
            client.shutdown();
            check(mc.options.gamma().get() == 0.23 && mc.gameRenderer.levelLightmap() == original, "client shutdown restores gamma and native lightmap");
        });
        client.getLogger().info("Phase 8 native OpenGL checks passed ({} assertions); cave brightness dark={} fullbright={} restored={}", assertions, dark, bright, restored);
    }
    private static UIComponent.Bounds box(String id) { var b = Client.INSTANCE.getHudRenderer().getBoxes().stream().filter(v -> v.element().id().equals(id)).findFirst().orElseThrow(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), b.height()); }
    private static HudEditorScreen editor() { return (HudEditorScreen) net.minecraft.client.Minecraft.getInstance().gui.screen(); }
    private static <T extends Module> T module(ClientGameTestContext context, Class<T> type) { return context.computeOnClient(mc -> Client.INSTANCE.getModuleManager().getByClass(type).orElseThrow()); }
    private static void number(Module module, String name, double value) { ((DoubleSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static void flag(Module module, String name, boolean value) { ((BooleanSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static <E extends Enum<E>> void choice(Module module, String name, E value) { @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow(); setting.setValue(value); }
    private static <E extends Enum<E>> void choice(ThemeManager themes, String name, E value) { @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) themes.getSetting(name); setting.setValue(value); }
    private static void command(ClientGameTestContext context, String command) { context.runOnClient(mc -> mc.getConnection().sendChat(command)); context.waitTick(); }
    private static void moveGui(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) { double[] p = context.computeOnClient(mc -> { var b = bounds.get(); return new double[] {b.x() + b.width()*x, b.y() + b.height()*y}; }); setGuiCursor(context, p[0], p[1]); }
    private static void setGuiCursor(ClientGameTestContext context, double x, double y) { double[] p = context.computeOnClient(mc -> new double[] {x * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(), y * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight()}); context.getInput().setCursorPos(p[0], p[1]); context.waitTick(); }
    private static double average(String name) {
        Path directory = Path.of("screenshots");
        try (var paths = Files.list(directory)) {
            Path path = paths.filter(p -> p.getFileName().toString().contains(name)).findFirst().orElseThrow();
            try (var stream = Files.newInputStream(path); NativeImage image = NativeImage.read(stream)) {
                double sum = 0; int count = 0;
                for (int y = image.getHeight()/3; y < image.getHeight()*2/3; y += 3) { for (int x = image.getWidth()/3; x < image.getWidth()*2/3; x += 3) {
                    int rgb = image.getPixel(x,y); sum += ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255))/3.0; count++;
                } }
                return sum/count;
            }
        } catch (IOException failure) { throw new AssertionError("Read native screenshot", failure); }
    }
    private static void check(boolean passed, String message) { assertions++; if (!passed) { throw new AssertionError(message); } }
}
