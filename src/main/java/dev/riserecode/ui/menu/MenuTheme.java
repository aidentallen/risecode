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
    private static final Map<Object, Animation> HOVERS = new WeakHashMap<>();
    private static final Map<AbstractWidget, dev.riserecode.ui.animation.ButtonFeedback> PRESSES = new WeakHashMap<>();
    public static void press(AbstractWidget widget) { PRESSES.computeIfAbsent(widget, ignored -> new dev.riserecode.ui.animation.ButtonFeedback()).activate(); }
    public static void hold(AbstractWidget widget){PRESSES.computeIfAbsent(widget,ignored->new dev.riserecode.ui.animation.ButtonFeedback()).press();}
    public static double pressScale(AbstractWidget widget) { var feedback = PRESSES.get(widget); return feedback == null ? 1 : feedback.scale(); }
    private static final Map<Object,Double> CUSTOM_SCALES=new WeakHashMap<>();
    public static void customScale(Object widget,double scale){CUSTOM_SCALES.put(widget,scale);}
    public static double customScale(Object widget){return enabled()?CUSTOM_SCALES.getOrDefault(widget,1.0):1;}
    private MenuTheme() { }
    private static boolean backdropDrawn;
    private static boolean particlesDrawn;
    private static int backdropPasses;
    private static long frameSerial;
    public static long frameSerial(){return frameSerial;}
    public static void beginFrame() { frameSerial++;backdropDrawn=false; particlesDrawn=false; backdropPasses=0; }
    public static void drawParticles(GuiGraphicsExtractor graphics) {
        if(!particlesDrawn && Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady()) {
            particlesDrawn=true;BackgroundParticles.draw(graphics,1);
        }
    }
    public static int backdropPasses() { return backdropPasses; }
    public static void drawBackdrop(GuiGraphicsExtractor graphics) {
        if (!backdropDrawn) { backdropDrawn=true; backdropPasses++; MenuBackdrop.draw(graphics); }
    }
    private static final Map<Screen,Double> FRAME_PROGRESS=new WeakHashMap<>();
    public static double frameProgress(Screen screen) { return FRAME_PROGRESS.getOrDefault(screen,1.0); }
    public static boolean animated(Screen screen){return enabled(screen)&&!(screen instanceof dev.riserecode.ui.clickgui.ClickGui)&&!(screen instanceof dev.riserecode.ui.hud.HudEditorScreen);}
    public static double beginScreen(Screen screen){FRAME_PROGRESS.put(screen,animated(screen)?entrance(screen):1.0);return 1;}
    public static double screenScale(Screen screen){return 1;}
    public static double pointerX(Screen screen,double x){return screen.width/2.0+(x-screen.width/2.0)/screenScale(screen);}
    public static double pointerY(Screen screen,double y){return screen.height/2.0+(y-screen.height/2.0)/screenScale(screen);}
    public static net.minecraft.client.input.MouseButtonEvent pointer(Screen screen,net.minecraft.client.input.MouseButtonEvent event){return new net.minecraft.client.input.MouseButtonEvent(pointerX(screen,event.x()),pointerY(screen,event.y()),event.buttonInfo());}
    public static double buttonEntranceScale() {
        Screen screen=Minecraft.getInstance().gui.screen();
        return animated(screen)?dev.riserecode.ui.animation.PopIn.windowScale("Dialogs",frameProgress(screen)):1;
    }
    public static double buttonEntranceOpacity() {
        Screen screen=Minecraft.getInstance().gui.screen();
        return animated(screen)?Math.clamp(frameProgress(screen),0,1):1;
    }
    public static double widgetScale(AbstractWidget widget) {
        return (widget instanceof net.minecraft.client.gui.components.AbstractButton?buttonEntranceScale():1)*(enabled()?pressScale(widget):1);
    }
    public static double localX(AbstractWidget widget,double x) { return widget.getX()+widget.getWidth()/2.0+(x-widget.getX()-widget.getWidth()/2.0)/widgetScale(widget); }
    public static double localY(AbstractWidget widget,double y) { return widget.getY()+widget.getHeight()/2.0+(y-widget.getY()-widget.getHeight()/2.0)/widgetScale(widget); }
    private static final Map<Screen,Long> OPENED=new WeakHashMap<>();
    public static void opened(Screen screen){OPENED.put(screen,System.nanoTime());}
    public static double entrance(Screen screen){return dev.riserecode.ui.animation.PopIn.progress((System.nanoTime()-OPENED.computeIfAbsent(screen,ignored->System.nanoTime()))*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Dialogs"));}
    public static String pageId(Screen screen){
        if(screen instanceof PageAppearanceScreen editor){return editor.target();}
        if(screen instanceof ClientSettingsScreen settings){return "risecode:client:"+settings.section().name();}
        if(screen instanceof dev.riserecode.ui.clickgui.ClickGui gui){return switch(gui.getPage()){case CLIENT_SOUNDS->"risecode:client:SOUNDS";case DISCORD_ACTIVITY->"risecode:client:DISCORD";case GUI_SCALE->"risecode:client:SCALE";default->"risecode:"+gui.getPage().name();};}
        return screen==null?"":screen.getClass().getName();
    }
    public static boolean gearSupported(Screen screen){return screen!=null&&!(screen instanceof dev.riserecode.ui.clickgui.ClickGui)&&!(screen instanceof PageAppearanceScreen)&&!(screen instanceof net.minecraft.client.gui.screens.ChatScreen);}
    public static String family(Screen screen) {
        if(screen instanceof PageAppearanceScreen){return null;}
        if(screen instanceof net.minecraft.client.gui.screens.ChatScreen || screen instanceof dev.riserecode.ui.clickgui.ClickGui || screen instanceof dev.riserecode.ui.hud.HudEditorScreen || screen instanceof dev.riserecode.map.WorldMapScreen){return null;}
        if (screen != null) {
            String name = screen.getClass().getName();
            if (name.startsWith("net.caffeinemc.mods.sodium.") || name.startsWith("net.irisshaders.iris.gui.") || name.startsWith("me.flashyrek.sodiumextra.") || name.startsWith("com.terraformersmc.modmenu.")) { return "Options"; }
            if (name.contains("ConnectScreen") || name.contains("DisconnectedScreen")) { return "Multiplayer"; }
            if (name.contains("Loading") || name.contains("ProgressScreen") || name.contains("ReceivingLevel") || name.contains("GenericMessageScreen") || name.contains("screens.worldselection")) { return "Loading"; }
        }
        if (screen instanceof TitleScreen) { return "Start Screen"; }
        if (screen instanceof net.minecraft.client.gui.screens.MultiplayerOptionsScreen || screen instanceof net.minecraft.client.gui.screens.CreditsAndAttributionScreen || screen instanceof OptionsScreen || screen instanceof OptionsSubScreen || screen != null && (screen.getClass().getPackageName().contains("screens.options") || screen.getClass().getPackageName().contains("screens.packs") || screen.getClass().getPackageName().contains("screens.telemetry") || screen.getClass().getPackageName().contains("screens.controls"))) { return "Options"; }
        if (screen instanceof net.minecraft.client.gui.screens.ManageServerScreen || screen instanceof net.minecraft.client.gui.screens.DirectJoinServerScreen || screen != null && screen.getClass().getPackageName().contains("screens.multiplayer")) { return "Multiplayer"; }
        return screen == null ? null : "Options";
    }
    public static boolean enabled(Screen screen) {
        String family=family(screen);
        if(!Client.INSTANCE.isInitialized()||!Client.INSTANCE.resourcesReady()||family==null){return false;}
        var page=Theme.manager().pages().get(pageId(screen));boolean inherited=screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> ? Theme.manager().choice("Container Style",dev.riserecode.ui.theme.ThemeManager.ContainerStyle.class)==dev.riserecode.ui.theme.ThemeManager.ContainerStyle.RISECODE : Theme.bool(family+" Theme",true);
        return page==null?inherited:page.enabled(inherited);
    }
    public static boolean activeMenu() {
        if (!Client.INSTANCE.isInitialized()) { return false; }
        var mc = Minecraft.getInstance();
        return enabled(mc.gui.screen()) || mc.gui.screen() instanceof dev.riserecode.ui.clickgui.ClickGui
            || mc.gui.screen() instanceof dev.riserecode.ui.hud.HudEditorScreen || mc.gui.overlay() instanceof net.minecraft.client.gui.screens.LoadingOverlay && Theme.bool("Loading Theme", true);
    }
    public static boolean enabled() { return enabled(Minecraft.getInstance().gui.screen()); }
    public static boolean photo(Screen screen) {
        if(dev.riserecode.ui.panorama.LivingPanorama.active()&&dev.riserecode.ui.panorama.LivingPanorama.renderer().texture()!=null&&(Theme.manager().choice("Background",dev.riserecode.ui.theme.ThemeManager.Background.class)!=dev.riserecode.ui.theme.ThemeManager.Background.NONE||dev.riserecode.ui.panorama.LivingPanorama.settled()))return true;
        if(!Client.INSTANCE.isInitialized()||!Client.INSTANCE.resourcesReady()||family(screen)==null||Theme.manager().choice("Background",dev.riserecode.ui.theme.ThemeManager.Background.class)==dev.riserecode.ui.theme.ThemeManager.Background.NONE){return false;}String family=family(screen);var page=Theme.manager().pages().get(pageId(screen));
        boolean inherited=Theme.bool(family+" Background",true);return page==null?inherited:page.backgroundEnabled(inherited||!page.inherit.getValue());
    }
    public static double hover(AbstractWidget widget) {
        return hover(widget,widget.isActive() && widget.isHoveredOrFocused());
    }
    public static double hover(Object widget,boolean hovered) {
        Animation value = HOVERS.computeIfAbsent(widget, ignored -> Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER, 0, 0.16, MathUtil.Easing.OUT_QUART));
        value.animateTo(hovered ? 1 : 0); return value.getValue();
    }
    public static void button(GuiGraphicsExtractor graphics, AbstractWidget widget) {
        double opacity = widget.getAlpha() * (widget.isActive() ? 1 : 0.45), hover = hover(widget);
        var screen = Minecraft.getInstance().gui.screen();
        if (photo(screen)) { MenuBackdrop.drawBlur(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, opacity * 0.7); }
        RenderUtil.drawDropShadow(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, Theme.opacity(0x60000000, opacity));
        RenderUtil.drawButton(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), hover, opacity);
        if (widget.isFocused()) { RenderUtil.drawRoundedBorder(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), 8, Theme.opacity(Theme.accent(), opacity * 0.8), 1); }
        var text = dev.riserecode.ui.font.StyledText.of(widget.getMessage()); var font = Minecraft.getInstance().font;
        double scale = Math.min(dev.riserecode.ui.component.ButtonMetrics.textScale(widget.getHeight()), (widget.getWidth() - 10.0) / Math.max(1, text.width(font)));
        RenderUtil.scissorPush(graphics, widget.getX() + 3, widget.getY() + 1, widget.getWidth() - 6, widget.getHeight() - 2);
        try { text.draw(graphics, font, widget.getX() + (widget.getWidth() - text.width(font) * scale) / 2,
            widget.getY() + (widget.getHeight() - 9 * scale * Theme.number("Font Scale", 1)) / 2 - hover * 0.3 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER), scale, Theme.opacity(Theme.TEXT, opacity)); }
        finally { RenderUtil.scissorPop(graphics); }
    }
    public static void iconButton(GuiGraphicsExtractor graphics, AbstractWidget widget) {
        double opacity = widget.getAlpha() * (widget.isActive() ? 1 : 0.45);
        RenderUtil.drawButton(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), hover(widget), opacity);
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
