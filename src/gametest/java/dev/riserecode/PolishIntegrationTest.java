package dev.riserecode;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.CrosshairEvent;
import dev.riserecode.event.HeldItemTransformEvent;
import dev.riserecode.event.MouseScrollEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudLayout;
import dev.riserecode.ui.menu.MenuBackdrop;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.item.ItemDisplayContext;

public final class PolishIntegrationTest {
    private static int checks;
    private PolishIntegrationTest() { }
    public static void menus(ClientGameTestContext context) {
        context.runOnClient(mc -> Client.INSTANCE.getThemeManager().reset());
        context.setScreen(TitleScreen::new); context.waitTicks(12); context.takeScreenshot("polish-start-themed");
        long builds = MenuBackdrop.builds(); context.waitTicks(8); check(MenuBackdrop.builds() == builds, "photo blur is cached between frames");
        context.runOnClient(mc -> { ((BooleanSetting) Theme.manager().getSetting("Start Screen Background")).setValue(false); });
        context.waitTicks(6); context.takeScreenshot("polish-start-native-background");
        check(context.computeOnClient(mc -> MenuTheme.enabled() && !MenuTheme.photo(mc.gui.screen())), "background toggle preserves themed controls");
        context.runOnClient(mc -> ((BooleanSetting) Theme.manager().getSetting("Start Screen Theme")).setValue(false));
        context.waitTicks(6); context.takeScreenshot("polish-start-vanilla"); check(context.computeOnClient(mc -> !MenuTheme.enabled()), "family theme off restores vanilla");
        context.runOnClient(mc -> { Theme.manager().reset(); mc.gui.setScreen(new JoinMultiplayerScreen(new TitleScreen())); });
        context.waitTicks(10); context.takeScreenshot("polish-multiplayer");
        context.runOnClient(mc -> mc.gui.setScreen(new OptionsScreen(new TitleScreen(), mc.options, false)));
        context.waitTicks(8); context.takeScreenshot("polish-options");
        context.runOnClient(mc -> mc.gui.setScreen(new SoundOptionsScreen(mc.gui.screen(), mc.options)));
        context.waitTicks(8); context.takeScreenshot("polish-options-audio");
        check(context.computeOnClient(mc -> MenuTheme.enabled()), "Options submenus inherit their theme family");
        context.setScreen(TitleScreen::new);
    }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var client = Client.INSTANCE;
        context.runOnClient(mc -> { mc.gui.setScreen(null); client.getModuleManager().disableAll(); mc.options.bobView().set(false); client.getHudRenderer().setEnabled(true); });
        world.getServer().runCommand("gamemode creative @a"); world.getServer().runCommand("time set noon");
        world.getServer().runCommand("tp @a 100.5 72 100.5 0 0");
        world.getServer().runCommand("fill 90 70 90 110 70 110 minecraft:stone");
        world.getServer().runCommand("fill 95 71 104 105 77 104 minecraft:stone");
        world.getServer().runCommand("setblock 99 73 107 minecraft:chest");
        world.getServer().runCommand("setblock 101 73 107 minecraft:diamond_ore");
        world.getServer().runCommand("setblock 103 73 107 minecraft:barrel");
        world.getServer().runCommand("summon minecraft:chest_minecart 97 73 107");
        world.getConnection().waitForChunksRender(); context.waitTicks(20);
        context.runOnClient(mc -> { mc.player.setXRot(0); mc.player.setYRot(0); mc.player.setYHeadRot(0); });
        var storage = client.getModuleManager().getByClass(StorageEspModule.class).orElseThrow();
        var players = client.getModuleManager().getByClass(PlayerEspModule.class).orElseThrow();
        var blocks = client.getModuleManager().getByClass(BlockEspModule.class).orElseThrow();
        var view = client.getModuleManager().getByClass(ViewModelModule.class).orElseThrow();
        var zoom = client.getModuleManager().getByClass(ZoomModule.class).orElseThrow();
        var crosshair = client.getModuleManager().getByClass(CrosshairModule.class).orElseThrow();
        check(!storage.isEnabled() && !players.isEnabled() && !blocks.isEnabled() && !view.isEnabled() && !zoom.isEnabled() && !crosshair.isEnabled(), "new modules default disabled");
        context.runOnClient(mc -> {
            var remote = new RemotePlayer(mc.level, new GameProfile(UUID.fromString("12345678-1234-1234-1234-123456789abc"), "OutlineFixture"));
            remote.setId(999990); remote.setPos(100.5, 73, 107.5); mc.level.addEntity(remote);
            storage.setEnabled(true); players.setEnabled(true); blocks.setEnabled(true);
            number(storage, "Range", 32); number(blocks, "Range", 32);
        });
        context.waitFor(mc -> client.getBlockIndex().blocks().stream().anyMatch(pos -> pos.getX() == 101 && pos.getY() == 73 && pos.getZ() == 107), 240);
        check(client.getBlockIndex().containers().size() >= 2, "loaded container index discovers chest and barrel");
        context.waitTicks(10); context.takeScreenshot("polish-esp-box-through-walls");
        context.runOnClient(mc -> { choice(storage, "Mode", PlayerEspModule.Mode.OUTLINE); choice(players, "Mode", PlayerEspModule.Mode.OUTLINE); choice(blocks, "Mode", PlayerEspModule.Mode.OUTLINE); });
        context.waitTicks(12); context.takeScreenshot("polish-esp-silhouette-through-walls");
        check(dev.riserecode.render.SilhouetteOutlineRenderer.batchCount() >= 3, "independent player/storage/block silhouette batches");
        long composites = dev.riserecode.render.SilhouetteOutlineRenderer.composites();
        int allocations = dev.riserecode.render.SilhouetteOutlineRenderer.allocations(); context.waitTicks(6);
        check(dev.riserecode.render.SilhouetteOutlineRenderer.composites() > composites && dev.riserecode.render.SilhouetteOutlineRenderer.allocations() == allocations, "silhouette passes run and targets are reused");
        context.runOnClient(mc -> { flag(storage, "VisibleOnly", true); flag(players, "VisibleOnly", true); flag(blocks, "VisibleOnly", true); });
        context.waitTicks(10); context.takeScreenshot("polish-esp-silhouette-visible-only");
        int throughPixels = outlinePixels("polish-esp-silhouette-through-walls"), visiblePixels = outlinePixels("polish-esp-silhouette-visible-only");
        check(throughPixels > 300 && visiblePixels < throughPixels / 10, "native screenshots prove wall depth policy: " + throughPixels + "/" + visiblePixels);
        client.getLogger().info("ESP outline pixels through-wall={} visible-only={}", throughPixels, visiblePixels);
        world.getServer().runCommand("setblock 101 73 107 minecraft:air"); context.waitTicks(6);
        check(client.getBlockIndex().blocks().stream().noneMatch(pos -> pos.getX() == 101 && pos.getY() == 73 && pos.getZ() == 107), "block updates invalidate cached matches");
        context.runOnClient(mc -> { storage.setEnabled(false); players.setEnabled(false); blocks.setEnabled(false); crosshair.setEnabled(true); number(crosshair,"Scale",2); });
        world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword");
        world.getServer().runCommand("item replace entity @a weapon.offhand with minecraft:shield"); context.waitTicks(30);
        context.takeScreenshot("polish-crosshair-2x");
        context.runOnClient(mc -> { var event = client.getEventBus().post(new CrosshairEvent()); check(event.scale() == 2, "crosshair scale reaches native event");
            number(view, "Right X", -0.3); number(view, "Right Rotation Z", 25); number(view,"Left Scale",0.7); view.setEnabled(true);
            var pose = new PoseStack(); var before = new org.joml.Matrix4f(pose.last().pose());
            client.getEventBus().post(new HeldItemTransformEvent(pose, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)); check(!before.equals(pose.last().pose()), "view model changes first-person item pose");
        }); context.waitTicks(12); context.takeScreenshot("polish-viewmodel-both-hands");
        check(zoom.getKeybind() == InputConstants.KEY_C && zoom.getBindMode() == Module.BindMode.HOLD, "Zoom defaults Hold C");
        context.getInput().holdKey(InputConstants.KEY_C); context.waitTicks(12);
        check(zoom.isEnabled() && client.getCameraEffects().magnification() > 2.9, "Hold key drives smooth Zoom");
        double previous = zoom.getMagnification().getValue(); int slot = context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
        context.getInput().scroll(1); context.waitTicks(6);
        check(zoom.getMagnification().getValue() > previous && context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == slot, "zoom scroll adjusts magnification without hotbar changes");
        context.takeScreenshot("polish-zoom"); context.getInput().releaseKey(InputConstants.KEY_C); context.waitTicks(12);
        check(!zoom.isEnabled() && client.getCameraEffects().magnification() == 1, "Zoom releases and finishes zoom-out animation");
        context.runOnClient(mc -> {
            var layout = client.getHudRenderer().getElements().getFirst().layout(); layout.background.setValue(HudLayout.Background.SOLID); layout.border.setValue(HudLayout.Border.ON);
            client.getThemeManager().colorSetting("Border Color").setValue(0xFFFF11FF); ((DoubleSetting) client.getThemeManager().getSetting("Border Opacity")).setValue(1.0);
        }); context.waitTicks(10); context.takeScreenshot("polish-hud-border");
        long sorts = client.getHudRenderer().statistics().sorts(); long measures = client.getHudRenderer().statistics().nameMeasurements();
        long hits = dev.riserecode.ui.font.CustomTextRenderer.layoutHits(); context.waitTicks(10);
        check(client.getHudRenderer().statistics().sorts() == sorts && client.getHudRenderer().statistics().nameMeasurements() == measures, "steady HUD skips measurement and sorting");
        check(dev.riserecode.ui.font.CustomTextRenderer.layoutHits() > hits, "repeated text uses glyph layout cache");
        var times = new java.util.concurrent.CopyOnWriteArrayList<Long>();
        var timing = client.getEventBus().subscribe(dev.riserecode.event.Render2DEvent.class, Integer.MIN_VALUE, false, event -> {
            if (times.size() < 120) { times.add(client.getHudRenderer().statistics().lastExtractionNanos()); }
        });
        context.waitFor(mc -> times.size() == 120, 240); timing.close();
        var measured = times.stream().sorted().toList();
        client.getLogger().info("Polish steady HUD extraction: median={}us p95={}us layouts={} hits={}", measured.get(60) / 1000.0,
                measured.get(114) / 1000.0, dev.riserecode.ui.font.CustomTextRenderer.cachedLayouts(), dev.riserecode.ui.font.CustomTextRenderer.layoutHits());
        context.runOnClient(mc -> {
            ((BlockListSetting) blocks.getSettings().stream().filter(s -> s.getName().equals("Blocks")).findFirst().orElseThrow()).setValue(List.of("minecraft:diamond_ore", "missing_mod:future_block"));
            check(client.getProfileManager().saveProfile("Polish"), "profile saves new block and camera controls");
            number(view,"Right X",0); check(client.getProfileManager().loadProfile("Polish"), "profile restores new controls");
            check(((DoubleSetting) view.getSettings().stream().filter(s -> s.getName().equals("Right X")).findFirst().orElseThrow()).getValue() == -0.3, "independent hand pose survives profile round trip");
        });
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> client.getClickGui().isOpen(),240);
        context.runOnClient(mc -> client.getClickGui().selectPage(ClickGui.Page.APPEARANCE)); context.waitTicks(8); context.takeScreenshot("polish-theme-controls");
        context.runOnClient(mc -> { mc.gui.setScreen(null); client.getModuleManager().disableAll(); client.getThemeManager().reset(); });
        client.getLogger().info("Polish OpenGL checks passed ({} assertions)", checks);
    }
    private static int outlinePixels(String name) {
        try (var files = java.nio.file.Files.list(java.nio.file.Path.of("screenshots"))) {
            var path = files.filter(file -> file.getFileName().toString().endsWith(name + ".png")).findFirst().orElseThrow();
            try (var input = java.nio.file.Files.newInputStream(path); var image = com.mojang.blaze3d.platform.NativeImage.read(input)) {
                int count = 0;
                for (int y = image.getHeight() / 6; y < image.getHeight() * 3 / 5; y++) { for (int x = image.getWidth() / 5; x < image.getWidth() * 4 / 5; x++) {
                    int pixel = image.getPixel(x, y), r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
                    if (g > 120 && (b > r + 30 || r > b + 60 && g > b + 50)) { count++; }
                } } return count;
            }
        } catch (java.io.IOException failure) { throw new AssertionError("Read native ESP screenshot", failure); }
    }
    private static void check(boolean condition, String label) { checks++; if (!condition) { throw new AssertionError(label); } }
    private static void number(Module module, String name, double value) { ((DoubleSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static void flag(Module module, String name, boolean value) { ((BooleanSetting) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow()).setValue(value); }
    private static <E extends Enum<E>> void choice(Module module, String name, E value) {
        @SuppressWarnings("unchecked") EnumSetting<E> setting = (EnumSetting<E>) module.getSettings().stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow(); setting.setValue(value);
    }
}
