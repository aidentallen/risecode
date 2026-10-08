package dev.riserecode.ui.menu;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

/** Rendering adapters only: native controls retain their input, focus and narration. */
public final class MenuTheme {
    private static final Map<AbstractWidget, Animation> HOVERS = new WeakHashMap<>();
    private MenuTheme() { }
    public static String family(Screen screen) {
        if (screen != null) {
            String name = screen.getClass().getName();
            if (name.startsWith("net.caffeinemc.mods.sodium.") || name.startsWith("net.irisshaders.iris.gui.") || name.startsWith("me.flashyrek.sodiumextra.") || name.startsWith("com.terraformersmc.modmenu.")) { return "Options"; }
            if (name.contains("ConnectScreen") || name.contains("DisconnectedScreen")) { return "Multiplayer"; }
            if (name.contains("Loading") || name.contains("ProgressScreen") || name.contains("ReceivingLevel") || name.contains("GenericMessageScreen") || name.contains("screens.worldselection")) { return "Loading"; }
        }
        if (screen instanceof TitleScreen) { return "Start Screen"; }
        if (screen instanceof net.minecraft.client.gui.screens.MultiplayerOptionsScreen || screen instanceof net.minecraft.client.gui.screens.CreditsAndAttributionScreen || screen instanceof OptionsScreen || screen instanceof OptionsSubScreen || screen != null && (screen.getClass().getPackageName().contains("screens.options") || screen.getClass().getPackageName().contains("screens.packs") || screen.getClass().getPackageName().contains("screens.telemetry") || screen.getClass().getPackageName().contains("screens.controls"))) { return "Options"; }
        if (screen instanceof net.minecraft.client.gui.screens.ManageServerScreen || screen instanceof net.minecraft.client.gui.screens.DirectJoinServerScreen || screen != null && screen.getClass().getPackageName().contains("screens.multiplayer")) { return "Multiplayer"; }
        return null;
    }
    public static boolean enabled(Screen screen) {
        String family = family(screen); return Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && family != null && Theme.bool(family + " Theme", true);
    }
    public static boolean activeMenu() {
        if (!Client.INSTANCE.isInitialized()) { return false; }
        var mc = Minecraft.getInstance();
        return enabled(mc.gui.screen()) || mc.gui.screen() instanceof dev.riserecode.ui.clickgui.ClickGui
            || mc.gui.screen() instanceof dev.riserecode.ui.hud.HudEditorScreen || mc.gui.overlay() instanceof net.minecraft.client.gui.screens.LoadingOverlay && Theme.bool("Loading Theme", true);
    }
    public static boolean enabled() { return enabled(Minecraft.getInstance().gui.screen()); }
    public static boolean photo(Screen screen) { String family = family(screen); return enabled(screen) && Theme.bool(family + " Background", true); }
    public static double hover(AbstractWidget widget) {
        Animation value = HOVERS.computeIfAbsent(widget, ignored -> Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER, 0, 0.16, MathUtil.Easing.OUT_QUART));
        value.animateTo(widget.isActive() && widget.isHoveredOrFocused() ? 1 : 0); return value.getValue();
    }
    public static void button(GuiGraphicsExtractor graphics, AbstractWidget widget) {
        double opacity = widget.getAlpha() * (widget.isActive() ? 1 : 0.45), hover = hover(widget);
        var screen = Minecraft.getInstance().gui.screen();
        if (photo(screen)) { MenuBackdrop.drawBlur(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, opacity * 0.7); }
        RenderUtil.drawDropShadow(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, Theme.opacity(0x60000000, opacity));
        RenderUtil.drawSurfaceTransition(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, hover, opacity);
        if (widget.isFocused()) { RenderUtil.drawRoundedBorder(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, Theme.opacity(Theme.accent(), opacity * 0.8), 1); }
        String text = widget.getMessage().getString(); var font = Minecraft.getInstance().font;
        double scale = Math.min(1, (widget.getWidth() - 10.0) / Math.max(1, RenderUtil.textWidth(font, text, false)));
        RenderUtil.scissorPush(graphics, widget.getX() + 3, widget.getY() + 1, widget.getWidth() - 6, widget.getHeight() - 2);
        try { RenderUtil.text(graphics, font, text, widget.getX() + (widget.getWidth() - RenderUtil.textWidth(font, text, false) * scale) / 2,
            widget.getY() + (widget.getHeight() - 9 * scale * Theme.number("Font Scale", 1)) / 2 - hover * 0.3 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER), scale, Theme.opacity(Theme.TEXT, opacity)); }
        finally { RenderUtil.scissorPop(graphics); }
    }
    public static void iconButton(GuiGraphicsExtractor graphics, AbstractWidget widget) {
        double opacity = widget.getAlpha() * (widget.isActive() ? 1 : 0.45);
        RenderUtil.drawSurfaceTransition(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, hover(widget), opacity);
        String key = widget.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text ? text.getKey() : "profile";
        dev.riserecode.ui.component.Icon icon = key.equals("options.language") ? dev.riserecode.ui.component.Icon.WORLD
                : key.equals("options.accessibility") ? dev.riserecode.ui.component.Icon.PLAYER : dev.riserecode.ui.component.Icon.PROFILES;
        icon.draw(graphics, widget.getX() + (widget.getWidth() - 10) / 2.0, widget.getY() + (widget.getHeight() - 10) / 2.0, Theme.opacity(Theme.TEXT, opacity));
        if (widget.isFocused()) { RenderUtil.drawRoundedBorder(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, Theme.opacity(Theme.accent(), opacity), 1); }
    }
    public static void list(GuiGraphicsExtractor graphics, AbstractWidget widget) {
        if (photo(Minecraft.getInstance().gui.screen())) { MenuBackdrop.drawBlur(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 12, 0.85); }
        RenderUtil.drawSurfaceTransition(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 12, 0, 0.65);
    }
}
