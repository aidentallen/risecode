package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.AmbienceModule;
import dev.riserecode.render.AmbienceRenderer;
import dev.riserecode.resources.ResourceArchive;
import dev.riserecode.resources.ResourcesManager;
import dev.riserecode.resources.ResourcesPackSource;
import dev.riserecode.resources.ResourcesState;
import dev.riserecode.resources.VanillaTweaksProvider;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ResourcesEditor;
import dev.riserecode.ui.component.UIComponent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

/** Actual offline packs, native input, profile reload/rollback, module lifecycles and optional renderer handoff. */
public final class ResourcesIntegrationTest {
    private static int checks;
    private ResourcesIntegrationTest() { }
    private static void check(boolean condition, String message) { checks++; if (!condition) { throw new AssertionError(message); } }
    private static ResourcesManager.ApplyResult apply(ClientGameTestContext c, ResourcesState state) {
        c.waitFor(mc -> mc.gui.overlay() == null && !manager().busy(), 1200);
        CompletableFuture<ResourcesManager.ApplyResult> future = c.computeOnClient(mc -> manager().apply(state));
        c.waitFor(mc -> future.isDone(), 1200); var result = future.join(); c.waitFor(mc -> mc.gui.overlay() == null, 1200); c.waitTicks(3); return result;
    }
    private static ResourcesManager manager() { return Client.INSTANCE.getResourcesManager(); }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static ResourcesEditor editor() { return gui().getResourcesEditor(); }
    private static ResourcesState selected(String... ids) { return new ResourcesState(manager().library().catalog().revision(), List.of(ids)); }
    public static void run(ClientGameTestContext c) {
        c.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        c.runOnClient(mc -> { mc.options.enableVsync().set(false); mc.options.framerateLimit().set(120); });
        check(manager().library().progress().ready(), "Full official offline cache supplied before launch");
        var catalog = manager().library().catalog();
        check(catalog.entries().size() >= 600, "Complete public 26.2 catalog, rather than a fixed demo subset");
        check(java.util.Arrays.stream(ClickGui.Page.values()).noneMatch(page -> page.name().equals("STYLES") || page.name().equals("AGENT")), "Old Styles and Agent pages absent");
        var modules = Client.INSTANCE.getModuleManager(); var ambience = modules.getByClass(AmbienceModule.class).orElseThrow();
        check(!ambience.isEnabled(), "Ambience is a normal disabled-by-default module");
        ResourcesState previous = c.computeOnClient(mc -> manager().state()); long requests = VanillaTweaksProvider.requestCount();
        try (var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a"); world.getServer().runCommand("time set noon");
            world.getServer().runCommand("fill -8 62 -8 8 63 10 stone"); world.getServer().runCommand("fill -5 64 2 5 64 10 water");
            world.getServer().runCommand("tp @a 0.5 66 0.5 0 -45");
            c.runOnClient(mc -> { modules.disableAll(); mc.options.renderDistance().set(4); }); c.waitTicks(10);
            var ordinary = c.computeOnClient(mc -> mc.getResourcePackRepository().getSelectedPacks().stream().map(net.minecraft.server.packs.repository.Pack::getId).filter(id -> !id.equals(ResourcesPackSource.ID)).toList());
            c.getInput().pressKey(InputConstants.KEY_RSHIFT); c.waitFor(mc -> gui().isOpen(), 240);
            c.runOnClient(mc -> gui().selectPage(ClickGui.Page.RESOURCES)); c.waitTicks(12); c.takeScreenshot("resources-catalog");
            c.runOnClient(mc -> {var b=editor().getBounds();editor().mouseScrolled(b.centerX(),b.centerY(),-6);});c.waitTick();
            c.runOnClient(mc -> {check(editor().scrollDestination()>0&&editor().scrollPosition()>=0,"Resources scrolling has animated state and destination");
                var b=editor().getBounds();int row=(int)Math.ceil(editor().scrollPosition()/66);var expected=catalog.entries().get(row*Math.clamp((int)((b.width()+8)/140),1,6));double y=b.y()+28+row*66-editor().scrollPosition()+30;
                editor().mouseClicked(b.x()+15,y,0);check(editor().draft().selected().contains(expected.id()),"Card hit detection follows displayed scroll");editor().search().setValue("Cherry Picking");});c.waitTicks(4);
            check(editor().scrollPosition()<=editor().maximumScroll()&&editor().scrollDestination()<=editor().maximumScroll(),"Filtering clamps scroll position and destination");
            c.runOnClient(mc -> {editor().discard();editor().search().setValue("Cherry Picking");}); c.waitTicks(5);
            click(c, 20, 75, 0); check(editor().draft().selected().contains("CherryPicking"), "Native card click edits draft only");
            check(manager().state().equals(previous), "Draft browsing does not apply resources");
            c.runOnClient(mc -> editor().discard()); check(editor().draft().equals(previous), "Cancel discards draft");
            c.runOnClient(mc -> { editor().search().setValue("Warm Glow"); }); c.waitTicks(4); click(c, 20, 75, 1); c.waitTicks(5); c.takeScreenshot("resources-compatibility-detail");
            check(catalog.byId().get("WarmGlow").description().contains("Sodium"), "Official renderer compatibility notice retained");
            c.runOnClient(mc -> { editor().discard(); mc.gui.setScreen(null); });
            var languageState = selected("StemToLog", "DirtPathToGrassPath");
            check(apply(c, languageState).success(), "Previously unused language combination applies without network");
            check(c.computeOnClient(mc -> ResourcesPackSource.matches(manager().state())), "Managed generation matches saved ordered IDs");
            check(c.computeOnClient(mc -> mc.getResourceManager().getResource(Identifier.withDefaultNamespace("lang/en_us.json")).orElseThrow().sourcePackId()).equals(ResourcesPackSource.ID), "Applied resources come from managed pack");
            c.runOnClient(mc -> {
                try (var input = mc.getResourceManager().getResource(Identifier.withDefaultNamespace("lang/en_us.json")).orElseThrow().open()) {
                    var language = ResourceArchive.json(input.readAllBytes());
                    check(language.has("block.minecraft.crimson_stem") && language.has("block.minecraft.dirt_path"), "Native loaded language contains both tweaks");
                } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            });
            check(!ambience.isEnabled(), "Applying Resources does not activate Ambience");
            check(apply(c, selected("SmootherFont", "QuieterRain", "NoCaveSounds")).success(), "Font and audio tweaks reload together offline");
            check(c.computeOnClient(mc -> mc.getResourceManager().getResource(Identifier.withDefaultNamespace("textures/font/ascii.png")).orElseThrow().sourcePackId()).equals(ResourcesPackSource.ID), "Native font texture uses the selected resource");
            c.runOnClient(mc -> {
                try (var input = mc.getResourceManager().getResource(Identifier.withDefaultNamespace("sounds.json")).orElseThrow().open()) {
                    var sounds = ResourceArchive.json(input.readAllBytes());
                    check(sounds.getAsJsonObject("ambient.cave").getAsJsonArray("sounds").isEmpty() && sounds.has("weather.rain"), "Loaded sound metadata retains independent selected tweaks");
                } catch (java.io.IOException error) { throw new IllegalStateException(error); }
            });
            check(apply(c, languageState).success(), "Language selection restored before profile fixture");
            check(c.computeOnClient(mc -> Client.INSTANCE.getProfileManager().saveProfile("ResourcesRoundTrip")), "Existing profiles save Resources");
            check(apply(c, ResourcesState.empty()).success(), "Clearing selection removes managed resources");
            var load = c.computeOnClient(mc -> Client.INSTANCE.getProfileManager().loadProfileAsync("ResourcesRoundTrip"));
            c.waitFor(mc -> load.isDone() && mc.gui.overlay() == null, 1200); check(load.join() && manager().state().equals(languageState), "Asynchronous profile load restores resources offline");
            c.runOnClient(mc -> ResourcesTestBootstrap.failNextReload.set(true));
            var failure = apply(c, selected("CherryPicking")); check(!failure.success() && manager().state().equals(languageState), "Reload failure restores previous resource state");
            check(c.computeOnClient(mc -> mc.getResourcePackRepository().getSelectedPacks().stream().map(net.minecraft.server.packs.repository.Pack::getId).filter(id -> !id.equals(ResourcesPackSource.ID)).toList()).equals(ordinary), "Rollback preserves ordinary pack order");
            check(apply(c, selected("HorizontalNuggets", "SidewaysNuggets")).success(), "Overlapping resources remain selectable with explicit priority");
            c.runOnClient(mc -> { gui().open(null); gui().selectPage(ClickGui.Page.RESOURCES); editor().showSelected(); }); c.waitFor(mc -> gui().isOpen(), 240); c.waitTicks(5);
            c.takeScreenshot("resources-priority");
            c.getInput().pressKey(InputConstants.KEY_DOWN); c.getInput().pressKey(InputConstants.KEY_DOWN);
            // Fabric TestInput emits modifier=0 even while Control is held. Exercise the screen's real modifier event.
            c.runOnClient(mc -> mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_UP, 0, 2))); c.waitTicks(3);
            check(editor().draft().selected().getFirst().equals("SidewaysNuggets"), "Native keyboard priority controls move the intended selected resource");
            var reordered = c.computeOnClient(mc -> editor().draft()); c.runOnClient(mc -> mc.gui.setScreen(null)); check(apply(c, reordered).success(), "Reordered draft applies offline");
            check(apply(c, selected("3DDoors", "GlassDoors")).success(), "Special combined model assets bake in real client");
            String model = manager().library().compose(manager().state()).keySet().stream().filter(path -> path.startsWith("assets/minecraft/models/") && path.endsWith(".json")).sorted().findFirst().orElseThrow();
            check(c.computeOnClient(mc -> mc.getResourceManager().getResource(Identifier.withDefaultNamespace(model.substring("assets/minecraft/".length()))).orElseThrow().sourcePackId()).equals(ResourcesPackSource.ID), "Combined model resource is loaded from the generated pack");
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("entity_model_features") && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("entity_texture_features")) {
                check(apply(c, selected("MZSteve", "MZAlex", "MZAri", "MZEfe", "MZKai", "MZMakena", "MZNoor", "MZSunny", "MZZuri")).success(), "Official nine-skin combination reloads with EMF/ETF");
                world.getServer().runCommand("summon zombie 0.5 64 4.5 {NoAI:1b,Silent:1b}"); c.runOnClient(mc -> mc.player.setXRot(0)); c.waitTicks(20); c.takeScreenshot("resources-zombie-variants");
                check(c.computeOnClient(mc -> {
                    try { var type = Class.forName("traben.entity_model_features.EMFManager"); var instance = type.getMethod("getInstance").invoke(null); return type.getField("anyJemLoaded").getBoolean(instance) && ((java.util.List<?>) type.getField("loadingExceptions").get(instance)).isEmpty(); }
                    catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                }), "EMF loaded official models without model errors");
            }
            c.runOnClient(mc -> { modules.setEnabled(ambience, true); ambience.sky.setValue(true); ambience.water.setValue(true); mc.player.setXRot(-55); });
            long frames = AmbienceRenderer.frames(); c.waitTicks(10); check(AmbienceRenderer.frames() > frames, "Restored Ambience module renders its sky");
            c.takeScreenshot("resources-ambience-module-sky");
            c.runOnClient(mc -> { ambience.sky.setValue(false); }); c.waitTicks(3); check(c.computeOnClient(mc -> !AmbienceRenderer.skyActive() && AmbienceRenderer.waterActive()), "Independent Ambience sky/water controls");
            c.runOnClient(mc -> { ambience.sky.setValue(true); modules.setEnabled(ambience, false); Client.INSTANCE.getKeybindManager().bind(ambience, "F8"); Client.INSTANCE.getKeybindManager().setMode(ambience, Module.BindMode.HOLD); });
            c.getInput().holdKey(InputConstants.KEY_F8); c.waitTicks(4); check(ambience.isEnabled(), "Ambience uses ordinary module Hold binding");
            c.getInput().releaseKey(InputConstants.KEY_F8); c.waitTicks(3); check(!ambience.isEnabled() && !AmbienceRenderer.skyActive(), "Hold release cleans rendering subscriptions");
            c.runOnClient(mc -> { Client.INSTANCE.getKeybindManager().reset(ambience); modules.setEnabled(ambience, true); });
            if (Boolean.getBoolean("riserecode.withShaderPack")) {
                c.runOnClient(mc -> {
                    net.irisshaders.iris.Iris.getIrisConfig().setShaderPackName("ComplementaryUnbound_r5.9.3.zip"); net.irisshaders.iris.Iris.getIrisConfig().setShadersEnabled(true);
                    try { net.irisshaders.iris.Iris.getIrisConfig().save(); net.irisshaders.iris.Iris.reload(); } catch (java.io.IOException error) { throw new IllegalStateException(error); }
                });
                c.waitFor(mc -> mc.gui.overlay() == null && AmbienceRenderer.shaderPackActive(), 1200); c.waitTicks(5);
                check(c.computeOnClient(mc -> !AmbienceRenderer.skyActive() && !AmbienceRenderer.waterActive() && ambience.isEnabled()), "Iris owns sky/water without changing Ambience settings");
                c.runOnClient(mc -> {
                    net.irisshaders.iris.Iris.getIrisConfig().setShadersEnabled(false);
                    try { net.irisshaders.iris.Iris.getIrisConfig().save(); net.irisshaders.iris.Iris.reload(); } catch (java.io.IOException error) { throw new IllegalStateException(error); }
                });
                c.waitFor(mc -> mc.gui.overlay() == null && !AmbienceRenderer.shaderPackActive(), 1200); c.waitTicks(5);
                check(c.computeOnClient(mc -> AmbienceRenderer.skyActive() && AmbienceRenderer.waterActive()), "Disabling Iris restores the module's effects");
            }
            c.runOnClient(mc -> { modules.setEnabled(ambience, false); gui().open(null); gui().selectCategory(dev.riserecode.module.Category.RENDER); }); c.waitFor(mc -> gui().isOpen(), 240); c.waitTicks(5); c.takeScreenshot("resources-ambience-module-card");
            c.runOnClient(mc -> mc.gui.setScreen(null)); check(apply(c, previous).success(), "Original selection restored");
            check(VanillaTweaksProvider.requestCount() == requests, "All native selection changes made zero provider requests");
            Client.INSTANCE.getLogger().info("Resources native checks passed ({} assertions)", checks);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Resources native verification failed", failure); }
    }
    private static void click(ClientGameTestContext c, double dx, double dy, int button) {
        double[] point = c.computeOnClient(mc -> {
            UIComponent.Bounds bounds = editor().getBounds(); return new double[] {(bounds.x() + dx) * gui().getCanvasScale() * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(),
                    (bounds.y() + dy) * gui().getCanvasScale() * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight()};
        });
        c.getInput().setCursorPos(point[0], point[1]); c.waitTick(); c.getInput().pressMouse(button); c.waitTicks(3);
    }
}
