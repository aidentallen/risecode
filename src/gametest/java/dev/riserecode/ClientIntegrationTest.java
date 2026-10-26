package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.util.RenderUtil;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.hud.HudRenderer;
import net.minecraft.client.input.CharacterEvent;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real keyboard/mouse input and render frames; fixtures never enter the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    private static final int ANIMATION_TIMEOUT = 240;
    private enum Mode { FIRST, SECOND }
    private static final class ControlsModule extends Module {
        private final BooleanSetting advanced = register(new BooleanSetting("Advanced", "", false));
        private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "", Mode.FIRST));
        private final ColorSetting color = register(new ColorSetting("Accent", "", 0x803B82F6));
        private final DoubleSetting conditional = register(new DoubleSetting("Conditional", "", 0.5, 0, 1));
        private ControlsModule(String name) {
            super(name, "Client test fixture", Category.UTILITY, Client.INSTANCE.getEventBus());
            conditional.setVisibleWhen(advanced::getValue);
            for (int i = 0; i < 8; i++) { register(new DoubleSetting("Slider " + (i + 1), "", 0.5, 0, 1)); }
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(2048, 1152);
        context.runOnClient(minecraft -> minecraft.options.guiScale().set(2));
        if(Boolean.getBoolean("riserecode.glassOnly")){GlassIntegrationTest.run(context);return;}
        if(Boolean.getBoolean("riserecode.panoramaOnly")){PanoramaIntegrationTest.run(context);return;}
        if(Boolean.getBoolean("riserecode.font025Reference")){Release025FontReference.run(context);return;}
        if(Boolean.getBoolean("riserecode.release025Only")){Release025IntegrationTest.run(context);return;}
        if(Boolean.getBoolean("riserecode.guiPerformanceOnly")){GuiPerformanceTest.run(context);return;}
        if(Boolean.getBoolean("riserecode.cameraFovOnly")){CameraFovClientTest.run(context);return;}
        if(Boolean.getBoolean("riserecode.remediationOnly")){RemediationClientTest.run(context);if(Boolean.getBoolean("riserecode.chunkAnimationsOnly")){ChunkAnimationIntegrationTest.run(context);}return;}
        if (Boolean.getBoolean("riserecode.release023Only")) {
            Release023IntegrationTest.run(context);
            Release022IntegrationTest.run(context);
            if(Boolean.getBoolean("riserecode.blockOverlayOnly"))BlockOverlayIntegrationTest.run(context);
            if(Boolean.getBoolean("riserecode.release021Only"))Release021IntegrationTest.run(context);
            return;
        }
        if (Boolean.getBoolean("riserecode.release022Only")) {
            Release022IntegrationTest.run(context);
            if (Boolean.getBoolean("riserecode.blockOverlayOnly")) { BlockOverlayIntegrationTest.run(context); }
            if (Boolean.getBoolean("riserecode.release021Only")) { Release021IntegrationTest.run(context); }
            return;
        }
        if (Boolean.getBoolean("riserecode.release021Only")) {
            Release021IntegrationTest.run(context);
            if (Boolean.getBoolean("riserecode.blockOverlayOnly")) { BlockOverlayIntegrationTest.run(context); }
            return;
        }
        if (Boolean.getBoolean("riserecode.blockOverlayOnly")) { BlockOverlayIntegrationTest.run(context); return; }
        if(Boolean.getBoolean("riserecode.release020Only")){Release020IntegrationTest.run(context);return;}
        if(Boolean.getBoolean("riserecode.release019Only")){Release019IntegrationTest.run(context);return;}
        if (Boolean.getBoolean("riserecode.cosmeticsOnly")) { CosmeticsIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.capePhysicsOnly")) { CapePhysicsIntegrationTest.run(context); return; }
        if(Boolean.getBoolean("riserecode.baselineSmoke")){
            context.runOnClient(mc->{mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);});
            context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady(),1200);
            context.setScreen(TitleScreen::new);context.waitTicks(60);context.getInput().pressKey(InputConstants.KEY_RSHIFT);context.waitTicks(60);context.takeScreenshot("baseline-window");context.setScreen(TitleScreen::new);context.waitTicks(20);Client.INSTANCE.getLogger().info("Native menu/window smoke passed");return;
        }
        if(Boolean.getBoolean("riserecode.release017Only")){Release017IntegrationTest.run(context);return;}
        if (Boolean.getBoolean("riserecode.uiRepairOnly")) { UiRepairIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.release016Only")) { Release016IntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.utilitiesOnly")) { UtilitiesIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.resourcesOnly")) { ResourcesIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.chunkAnimationsOnly")) { ChunkAnimationIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.noRenderOnly")) { NoRenderIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.upgradeOnly")) { UpgradeIntegrationTest.run(context); return; }
        if (Boolean.getBoolean("riserecode.benchmarkOnly")) {
            UpgradeBenchmark.run(context); return;
        }
        if (Boolean.getBoolean("riserecode.polishOnly")) {
            PolishIntegrationTest.menus(context);
            try (var world = context.worldBuilder().create()) {
                world.getConnection().waitForChunksRender(); PolishIntegrationTest.verify(context, world);
            }
            return;
        }
        if (Boolean.getBoolean("riserecode.keybindOnly")) {
            try (var world = context.worldBuilder().create()) {
                world.getConnection().waitForChunksRender(); KeybindIntegrationTest.verify(context, world);
            }
            return;
        }
        if (Boolean.getBoolean("riserecode.colorOutlineOnly")) {
            try (var world = context.worldBuilder().create()) {
                world.getConnection().waitForChunksRender(); ColorOutlineIntegrationTest.verify(context, world);
            }
            return;
        }
        if (Boolean.getBoolean("riserecode.phase8Only")) {
            try (var world = context.worldBuilder().create()) {
                world.getConnection().waitForChunksRender(); Phase8IntegrationTest.verify(context, world);
            }
            return;
        }
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getThemeManager().reset();
            var fullBright = fullBright();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            fullBright.getMode().setValue(FullBrightModule.Mode.GAMMA);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> gui().children().isEmpty()), "no native widget children");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitTick();
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> minecraft.gui.screen() == gui()), "Right Shift reverses an unfinished exit");
        context.runOnClient(minecraft -> { gui().selectPage(ClickGui.Page.SEARCH); gui().getSearchField().setValue("FullBright"); });
        context.waitTicks(5);
        ModuleButton fullBrightButton = context.computeOnClient(minecraft -> button("FullBright"));
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1000.0), "FullBright applies gamma");
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.APPEARANCE));
        context.waitTicks(5);
        context.runOnClient(minecraft -> gui().getThemeEditor().reveal("HUD Visible"));
        context.waitTicks(8);
        click(context, () -> gui().getThemeEditor().getControl("HUD Visible").getBounds(), 0);
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD switch");
        click(context, () -> gui().getThemeEditor().getControl("HUD Visible").getBounds(), 0);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.SEARCH));
        context.waitTicks(5);
        // Search through the real character-input adapter; a space matches formatted module names.
        context.runOnClient(minecraft -> {
            gui().getSearchField().setFocused(true);
            gui().getSearchField().setValue("");
            "full bright".codePoints().forEach(cp -> gui().charTyped(new CharacterEvent(cp)));
        });
        context.waitFor(minecraft -> gui().getActivePanel().getVisibleButtons().size() == 1);
        check(context.computeOnClient(minecraft -> gui().getActivePanel().getVisibleButtons().getFirst().getModule() == fullBright()), "search filters formatted names");
        context.runOnClient(minecraft -> gui().getSearchField().setValue("no matching module"));
        context.waitFor(minecraft -> gui().getActivePanel().getVisibleButtons().isEmpty());
        context.runOnClient(minecraft -> gui().getSearchField().setValue("FullBright"));
        context.waitTicks(5);
        click(context, fullBrightButton::getBounds, 1);
        context.waitFor(minecraft -> fullBrightButton.getBounds().height() > 63, ANIMATION_TIMEOUT);
        Slider brightness = context.computeOnClient(minecraft -> (Slider) fullBrightButton.getSettingControls().stream().filter(control -> control.setting() == fullBright().getBrightness()).findFirst().orElseThrow().component());
        move(context, () -> brightness.getBounds(), 0.5, 0.7);
        context.getInput().holdMouse(0);
        move(context, () -> brightness.getBounds(), 0.22, 0.7);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() > 0.15
                && fullBright().getBrightness().getValue() < 0.3), "real mouse slider drag");
        click(context, fullBrightButton::getBounds, 0);
        context.waitTick();
        check(context.computeOnClient(minecraft -> Math.abs(minecraft.options.gamma().get()
                - fullBright().getBrightness().getValue() * 1000) < 1e-9), "slider setting reaches module tick logic");
        click(context, fullBrightButton::getBounds, 0);
        context.runOnClient(minecraft -> fullBright().getBrightness().setValue(1.0));
        context.waitTicks(12);
        context.takeScreenshot("riserecode-clickgui");

        ControlsModule controls = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Controls")));
        ControlsModule longer = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Longer Name")));
        context.runOnClient(minecraft -> gui().selectCategory(Category.UTILITY));
        context.waitFor(minecraft -> panel(Category.UTILITY).getModuleButtons().stream()
                .map(ModuleButton::getModule).toList().containsAll(java.util.List.of(controls, longer)));
        context.waitTicks(5);
        ModuleButton controlsButton = context.computeOnClient(minecraft -> button(controls.getName()));
        click(context, controlsButton::getBounds, 1);
        context.waitFor(minecraft -> panel(Category.UTILITY).getMaxScroll() > 40, ANIMATION_TIMEOUT);
        click(context, () -> controlsButton.getSettingControls().get(0).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.advanced.getValue() && controls.conditional.isVisible()), "boolean control and visibleWhen");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(1).component());
        click(context, () -> controlsButton.getSettingControls().get(1).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.mode.getValue() == Mode.SECOND), "enum control");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(2).component());
        click(context, () -> controlsButton.getSettingControls().get(2).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> ((dev.riserecode.ui.component.setting.ColorPicker) controlsButton.getSettingControls().get(2).component()).isExpanded() && controls.color.getAlpha() == 128), "custom picker opens and preserves alpha");
        move(context, () -> panel(Category.UTILITY).getBounds(), 0.5, 0.8);
        context.getInput().scroll(-4);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() > 50, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> panel(Category.UTILITY).getScrollOffset() <= panel(Category.UTILITY).getMaxScroll()), "scroll stays bounded");
        context.takeScreenshot("riserecode-settings-scroll");
        context.getInput().scroll(100);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() == 0, ANIMATION_TIMEOUT);
        // Drag the single window through the actual native mouse handler.
        double startX = context.computeOnClient(minecraft -> gui().getWindowBounds().x());
        move(context, () -> gui().getWindowBounds(), 0.08, 0.04);
        context.getInput().holdMouse(0);
        context.getInput().moveCursor(-40, 15);
        context.waitTicks(3);
        context.getInput().releaseMouse(0);
        context.waitFor(minecraft -> gui().getWindowBounds().x() < startX - 8, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> gui().getWindowBounds().y() >= 8), "window drag constrained to canvas");
        context.getInput().resizeWindow(640, 400);
        context.waitTicks(5);
        check(context.computeOnClient(minecraft -> gui().getWindowBounds().x() >= 7.99
                && (gui().getWindowBounds().x() + gui().getWindowBounds().width()) * gui().getCanvasScale() <= minecraft.getWindow().getGuiScaledWidth()
                && (gui().getWindowBounds().y() + gui().getWindowBounds().height()) * gui().getCanvasScale() <= minecraft.getWindow().getGuiScaledHeight()), "small window fits and stays clamped");
        context.takeScreenshot("riserecode-small-window");
        context.getInput().resizeWindow(2048, 1152);
        context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.SEARCH));
        context.waitTicks(5);
        context.getInput().pressKey(InputConstants.KEY_RETURN);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.getInput().pressKey(InputConstants.KEY_RETURN);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        AtomicInteger hudFrames = new AtomicInteger();
        AtomicInteger worldFrames = new AtomicInteger();
        var hudSubscription = Client.INSTANCE.getEventBus().subscribe(Render2DEvent.class, event -> hudFrames.incrementAndGet());
        var worldSubscription = Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class, event -> worldFrames.incrementAndGet());
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(minecraft -> Client.INSTANCE.getModuleManager().setKeybind(fullBright(), InputConstants.KEY_B));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> fullBright().isEnabled()), "module key toggles once");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == fullBright() && entry.visibility() == 1), ANIMATION_TIMEOUT);
            check(hudFrames.get() > 0 && worldFrames.get() > 0, "HUD/world events reach custom bus");
            context.runOnClient(minecraft -> { controls.setEnabled(true); longer.setEnabled(true); });
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 3);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.visibility() == 1 && entry.y() % HudRenderer.ROW_HEIGHT == 0), ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> {
                var entries = Client.INSTANCE.getHudRenderer().getEntries();
                var widest = entries.stream().max(java.util.Comparator.comparingInt(entry ->
                        RenderUtil.textWidth(minecraft.font, Theme.displayName(entry.module().getName()), false))).orElseThrow();
                return widest.y() == 0;
            }), "array sorted by styled text width");
            context.takeScreenshot("riserecode-hud");
            context.runOnClient(minecraft -> longer.setEnabled(false));
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == longer && entry.visibility() > 0)), "disabled entry retained for exit");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 2, ANIMATION_TIMEOUT);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.y() == 0 || entry.y() == HudRenderer.ROW_HEIGHT), ANIMATION_TIMEOUT);
            context.runOnClient(minecraft -> controls.setEnabled(false));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            waitOpen(context);
            context.takeScreenshot("riserecode-glass-in-world");
            context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.APPEARANCE));
            context.waitTicks(5);
            context.runOnClient(minecraft -> {
                Theme.setAccent(Theme.BLUE);
                ((DoubleSetting) Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.15);
                Client.INSTANCE.getThemeManager().changed();
            });
            context.waitFor(minecraft -> Theme.accent() == Theme.BLUE && Math.abs(gui().getCanvasScale() - 1.15) < 0.001, ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> Theme.accentTarget() == Theme.BLUE), "theme selection animates across GUI and HUD");
            context.runOnClient(minecraft -> {
                ((DoubleSetting) Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.0);
                Theme.setAccent(Theme.TEAL); gui().getThemeEditor().reveal("Font Selection");
            });
            context.waitFor(minecraft -> gui().getCanvasScale() == 1 && Theme.accent() == Theme.TEAL, ANIMATION_TIMEOUT);
            context.waitTicks(8);
            context.takeScreenshot("riserecode-themes");
            context.runOnClient(minecraft -> gui().selectPage(ClickGui.Page.FRIENDS));
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue("GuiFriend"));
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getFriendManager().isFriend("GuiFriend")), "friend UI adds through the existing manager");
            context.takeScreenshot("riserecode-friends");
            click(context, () -> gui().getActionButtons().get(1).getBounds(), 0);
            check(context.computeOnClient(minecraft -> !Client.INSTANCE.getFriendManager().isFriend("GuiFriend")), "friend UI removes through the existing manager");
            context.runOnClient(minecraft -> { gui().selectPage(ClickGui.Page.PROFILES); fullBright().getBrightness().setValue(0.6); });
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue("GuiProfile"));
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            check(Files.exists(Client.INSTANCE.getProfileManager().getFile("GuiProfile")), "profile UI saves a real snapshot");
            context.runOnClient(minecraft -> fullBright().getBrightness().setValue(0.2));
            click(context, () -> gui().getActionButtons().get(1).getBounds(), 0);
            check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() == 0.6), "profile UI restores settings and refreshes");
            context.takeScreenshot("riserecode-profiles");
            context.runOnClient(minecraft -> {
                fullBright().getBrightness().setValue(1.0); gui().selectPage(ClickGui.Page.SEARCH);
                gui().getSearchField().setValue("tracer");
            });
            context.waitTicks(6);
            context.takeScreenshot("riserecode-search");
            context.getInput().resizeWindow(2560, 1440);
            context.waitTicks(5);
            double referenceWidth = context.computeOnClient(minecraft -> gui().getWindowBounds().width() * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth());
            context.runOnClient(minecraft -> minecraft.options.guiScale().set(3));
            context.waitTicks(5);
            check(context.computeOnClient(minecraft -> Math.abs(gui().getWindowBounds().width() * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth() - referenceWidth) < 1), "reference window size is independent of Minecraft GUI scale");
            context.runOnClient(minecraft -> {
                gui().getSearchField().setValue("render");
                var module = Client.INSTANCE.getModuleManager().getByName("Tracers").orElseThrow();
                module.setEnabled(true);
            });
            context.waitTicks(6);
            context.runOnClient(minecraft -> {
                var button = gui().getModuleButton("Tracers");
                if (!button.isExpanded()) { button.mouseClicked(button.getBounds().x() + 10, button.getBounds().y() + 10, 1); }
            });
            context.waitTicks(8);
            context.takeScreenshot("risecode-reference-layout");
            context.runOnClient(minecraft -> {
                Client.INSTANCE.getModuleManager().getByName("Tracers").orElseThrow().setEnabled(false);
                minecraft.options.guiScale().set(2);
            });
            context.getInput().resizeWindow(2048, 1152);
            context.waitTicks(5);
            context.runOnClient(minecraft -> gui().getSearchField().setValue(""));
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
            EngineIntegrationTest.verify(context, world);
            CombatIntegrationTest.verify(context, world);
            ThemeIntegrationTest.verify(context);
            FontIntegrationTest.verify(context);
        } finally {
            hudSubscription.close(); worldSubscription.close();
        }
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); KeybindIntegrationTest.verify(context, world);
        }
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); ColorOutlineIntegrationTest.verify(context, world);
        }
        PolishIntegrationTest.menus(context);
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); PolishIntegrationTest.verify(context, world);
        }
        // Each phase owns its world fixture; prior combat/lighting state cannot leak into the cave proof.
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender(); Phase8IntegrationTest.verify(context, world);
        }
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getModuleManager().setKeybind(fullBright(), -1);
            Client.INSTANCE.getConfigManager().save();
        });
    }

    private static FullBrightModule fullBright() { return Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow(); }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static Panel panel(Category category) { return gui().getPanels().stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow(); }
    private static ModuleButton button(String name) {
        return gui().getModuleButton(name);
    }
    private static void waitOpen(ClientGameTestContext context) {
        context.waitFor(minecraft -> gui().isOpen(), ANIMATION_TIMEOUT);
    }
    private static void reveal(ClientGameTestContext context, Panel panel, UIComponent component) {
        double amount = context.computeOnClient(minecraft -> {
            var body = panel.getBounds(); var control = component.getBounds();
            return (body.centerY() - control.centerY()) / 24;
        });
        move(context, panel::getBounds, 0.5, 0.8);
        double previous = context.computeOnClient(minecraft -> panel.getScrollOffset());
        context.getInput().scroll(amount);
        double target = Math.max(0, Math.min(panel.getMaxScroll(), previous - amount * 24));
        context.waitFor(minecraft -> Math.abs(panel.getScrollOffset() - target) < 0.01, ANIMATION_TIMEOUT);
    }
    private static void move(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> {
            var b = bounds.get();
            return new double[] {(b.x() + b.width() * x) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                    (b.y() + b.height() * y) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.waitTick();
    }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, int mouseButton) {
        // Headers stay fixed when a module expands; clicking the center of the full body would edit a setting.
        Supplier<UIComponent.Bounds> header = () -> {
            var b = bounds.get(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), Math.min(28, b.height()));
        };
        move(context, header, 0.4, 0.5);
        context.getInput().pressMouse(mouseButton);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
