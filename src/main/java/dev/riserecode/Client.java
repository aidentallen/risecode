package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Client {
    INSTANCE;

    public static final String MOD_ID = "riserecode";
    public static final String NAME = "RiseRecode";
    private final Logger logger = LoggerFactory.getLogger(NAME);
    private final EventBus eventBus = new EventBus((event, failure) ->
            logger.error("Listener failed for {}", event.getClass().getSimpleName(), failure));
    private String version = "development";
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private KeybindManager keybindManager;
    private HudRenderer hudRenderer;
    private ClickGui clickGui;
    private boolean initialized;

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME + " " + version);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.load();
        moduleManager.setChangeListener(configManager::requestSave);
        configManager.init(eventBus);
        keybindManager = new KeybindManager(eventBus, minecraft, moduleManager, this::getClickGui);
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
        configManager.close();
        moduleManager.disableAll();
        Minecraft.getInstance().options.save();
        initialized = false;
    }

    public boolean isInitialized() { return initialized; }
    public String getName() { return NAME; }
    public String getVersion() { return version; }
    public Logger getLogger() { return logger; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public KeybindManager getKeybindManager() { return keybindManager; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
    /** Screens capture the font, so create this only after Minecraft initializes it. */
    public ClickGui getClickGui() {
        if (clickGui == null) {
            if (!initialized || Minecraft.getInstance().font == null) {
                throw new IllegalStateException("The Minecraft GUI is not ready");
            }
            clickGui = new ClickGui(moduleManager, hudRenderer, configManager::requestSave);
        }
        return clickGui;
    }
}
