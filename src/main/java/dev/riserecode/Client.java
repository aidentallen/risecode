package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.config.ProfileManager;
import dev.riserecode.command.CommandManager;
import dev.riserecode.command.commands.BindCommand;
import dev.riserecode.command.commands.FriendCommand;
import dev.riserecode.command.commands.HelpCommand;
import dev.riserecode.command.commands.HudCommand;
import dev.riserecode.ui.hud.HudEditorScreen;
import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.command.commands.ProfileCommand;
import dev.riserecode.command.commands.ToggleCommand;
import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.render.WorldRenderer;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.AttackManager;
import dev.riserecode.util.RotationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Client {
    INSTANCE;

    public static final String MOD_ID = "riserecode";
    public static final String NAME = "risecode";
    private final Logger logger = LoggerFactory.getLogger(NAME);
    private final EventBus eventBus = new EventBus((event, failure) ->
            logger.error("Listener failed for {}", event.getClass().getSimpleName(), failure));
    private String version = "development";
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private KeybindManager keybindManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private ProfileManager profileManager;
    private WorldRenderer worldRenderer;
    private dev.riserecode.render.LoadedBlockIndex blockIndex;
    private RotationManager rotationManager;
    private AttackManager attackManager;
    private dev.riserecode.util.CameraEffectsManager cameraEffects;
    private HudRenderer hudRenderer;
    private ThemeManager themeManager;
    private ClickGui clickGui;
    private volatile boolean initialized;
    private boolean resourcesReady;
    public boolean resourcesReady() { return resourcesReady; }
    public void resourcesReady(boolean value) { resourcesReady = value; }

    public void init() {
        if (initialized) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        version = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("development");
        friendManager = new FriendManager();
        worldRenderer = new WorldRenderer(eventBus);
        blockIndex = new dev.riserecode.render.LoadedBlockIndex(eventBus, minecraft);
        rotationManager = new RotationManager(eventBus, minecraft);
        attackManager = new AttackManager(eventBus, minecraft);
        cameraEffects = new dev.riserecode.util.CameraEffectsManager(eventBus, minecraft);
        moduleManager = new ModuleManager(eventBus, logger);
        moduleManager.initDefaults(minecraft, friendManager, rotationManager, attackManager, cameraEffects, blockIndex);
        themeManager = new ThemeManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID), logger);
        themeManager.load(); themeManager.bind(eventBus); CustomTextRenderer.configure(themeManager);
        hudRenderer = new HudRenderer(eventBus, minecraft, moduleManager, NAME, themeManager);
        configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID),
                moduleManager, friendManager, hudRenderer::isEnabled, hudRenderer::setEnabled, logger);
        configManager.bindHudStore(hudRenderer::snapshotLayouts, hudRenderer::applyLayouts, hudRenderer::validateLayouts);
        configManager.bindVisualPolicyStore(themeManager::policySnapshot, themeManager::applyPolicy, themeManager::validatePolicy);
        configManager.useExternalVisualPreferences(); configManager.load();
        if (!themeManager.wasLoaded()) {
            var oldUi = configManager.getUiPreferences();
            hudRenderer.setEnabled(configManager.getLegacyHudEnabled());
            try {
                if (oldUi.has("accent")) { ((dev.riserecode.setting.ColorSetting) themeManager.getSetting("Primary Accent")).setValue(oldUi.get("accent").getAsInt()); }
                if (oldUi.has("scale")) { ((dev.riserecode.setting.DoubleSetting) themeManager.getSetting("Interface Scale")).setValue(oldUi.get("scale").getAsDouble()); }
                themeManager.changed(); themeManager.save();
            } catch (RuntimeException invalid) { logger.warn("Could not migrate legacy UI preferences", invalid); }
        }
        moduleManager.setChangeListener(configManager::requestSave);
        friendManager.setChangeListener(configManager::requestSave);
        configManager.init(eventBus);
        keybindManager = new KeybindManager(eventBus, minecraft, moduleManager, this::getClickGui);
        configManager.bindEnabledSnapshot(keybindManager::persistentEnabled);
        profileManager = new ProfileManager(configManager, () -> {
            if (clickGui != null && minecraft.gui.screen() == clickGui) { clickGui.refresh(); }
        }, logger);
        commandManager = new CommandManager(configManager::getCommandPrefix, configManager::setCommandPrefix, message -> {
            if (minecraft.player != null) {
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal(NAME + " » ").withStyle(ChatFormatting.BLUE)
                        .append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
            } else { logger.info("[Command] {}", message); }
        }, logger);
        commandManager.register(new ToggleCommand(moduleManager, commandManager));
        commandManager.register(new BindCommand(moduleManager, keybindManager, commandManager));
        commandManager.register(new FriendCommand(friendManager, commandManager, name -> {
            var connection = minecraft.getConnection();
            var info = connection == null ? null : connection.getPlayerInfoIgnoreCase(name);
            return info == null ? null : info.getProfile().id();
        }));
        commandManager.register(new ProfileCommand(profileManager, commandManager));
        commandManager.register(new HelpCommand(commandManager));
        commandManager.register(new HudCommand());
        commandManager.register(new dev.riserecode.command.commands.BaritoneCommand(moduleManager.getByClass(dev.riserecode.module.modules.BaritoneModule.class).orElseThrow()));
        initialized = true;
        logger.info("{} {} initialized with {} module(s)", NAME, version, moduleManager.getModules().size());
    }

    public void shutdown() {
        if (!initialized) { return; }
        // Save user choices before cleanup temporarily disables active modules.
        configManager.save();
        keybindManager.close();
        hudRenderer.close();
        themeManager.close(); dev.riserecode.ui.theme.BrandingLogo.close(); dev.riserecode.render.ItemOutlineRenderer.close(); dev.riserecode.render.SilhouetteOutlineRenderer.close(); BlurUtil.close(); dev.riserecode.ui.menu.MenuBackdrop.close(); CustomTextRenderer.close();
        worldRenderer.close(); blockIndex.close();
        configManager.close();
        moduleManager.disableAll();
        rotationManager.close();
        attackManager.close(); cameraEffects.close();
        Minecraft.getInstance().options.save();
        AnimationManager.ui().clear();
        resourcesReady = false; initialized = false;
    }

    public void openHudEditor() { Minecraft.getInstance().gui.setScreen(new HudEditorScreen(hudRenderer)); }

    public dev.riserecode.util.CameraEffectsManager getCameraEffects() { return cameraEffects; }
    public boolean isInitialized() { return initialized; }
    public String getName() { return themeManager == null ? NAME : themeManager.text("Display Name"); }
    public String getVersion() { return version; }
    public Logger getLogger() { return logger; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public KeybindManager getKeybindManager() { return keybindManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public ProfileManager getProfileManager() { return profileManager; }
    public dev.riserecode.render.LoadedBlockIndex getBlockIndex() { return blockIndex; }
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    public RotationManager getRotationManager() { return rotationManager; }
    public AttackManager getAttackManager() { return attackManager; }
    public ThemeManager getThemeManager() { return themeManager; }
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
