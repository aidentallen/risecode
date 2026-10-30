package dev.riserecode.ui.clickgui;

import com.google.gson.JsonObject;
import dev.riserecode.Client;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.Icon;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.Sidebar;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.ThemeEditor;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native 26.2 screen hosting an original, fully extracted sidebar/card interface. */
public final class ClickGui extends Screen {
    public enum Page { SEARCH, COMBAT, MOVEMENT, PLAYER, RENDER, WORLD, UTILITY, HUD, PROFILES, FRIENDS, APPEARANCE, RESOURCES, SETTINGS, WAYPOINTS, MAP_SETTINGS, COSMETICS, GAMEPLAY, VISUALS, CLIENT_SOUNDS, DISCORD_ACTIVITY, GUI_SCALE, PERFORMANCE }
    public static final List<Page> VISIBLE_PAGES = List.of(Page.SEARCH, Page.COMBAT, Page.MOVEMENT, Page.PLAYER, Page.RENDER, Page.WORLD, Page.UTILITY, Page.HUD, Page.APPEARANCE, Page.RESOURCES, Page.PERFORMANCE);
    public static final List<Page> NAVIGATION_PAGES = java.util.stream.Stream.concat(VISIBLE_PAGES.stream(),java.util.stream.Stream.of(Page.WAYPOINTS,Page.MAP_SETTINGS,Page.COSMETICS)).toList();
    private double windowWidth = 428, windowHeight = 320;
    private boolean resizing;
    private final dev.riserecode.ui.animation.Animation resizeHint=dev.riserecode.ui.animation.Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER,0,.12,dev.riserecode.util.MathUtil.Easing.OUT_QUART);
    private double resizeStartX, resizeStartY, resizeWidth, resizeHeight;
    public void resetPosition() { offsetX=offsetY=0; }

    private final List<Panel> panels = new ArrayList<>();
    private final Panel searchPanel;
    private final Sidebar sidebar;
    private final dev.riserecode.ui.component.ClientSettingsPopup settingsPopup=new dev.riserecode.ui.component.ClientSettingsPopup(this);
    public dev.riserecode.ui.component.ClientSettingsPopup getSettingsPopup(){return settingsPopup;}
    private final TextField field = new TextField(this::placeholder);
    private final ToggleSwitch hudToggle;
    private final ActionButton editHud = new ActionButton("Edit HUD", () -> Client.INSTANCE.openHudEditor());
    private final Runnable saveChanges;
    private final List<ActionButton> actions = new ArrayList<>();
    private net.minecraft.client.KeyMapping listeningKey;
    private final Animation opening = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.OPENING, 0, 0.26, MathUtil.Easing.OUT_QUART);
    private final Animation canvasZoom = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 1, 0.24, MathUtil.Easing.OUT_QUART);
    private final Animation pageFade = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 1, 0.18, MathUtil.Easing.OUT_QUART);
    private final Animation positionX = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.DRAGGING, 0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation positionY = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.DRAGGING, 0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation utilityScroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.18, MathUtil.Easing.OUT_QUART);
    private final Animation feedback = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
    private Screen parent;
    private Page page = Page.SEARCH;
    private UIComponent captured;
    private final ThemeManager themes;
    private final ThemeEditor themeEditor;
    private final java.util.Map<Page,dev.riserecode.ui.component.ClientSettingsEditor> clientEditors=new java.util.EnumMap<>(Page.class);
    public void openClientSettings(dev.riserecode.ui.menu.ClientSettingsScreen.Section section){
        selectPage(switch(section){case SOUNDS->Page.CLIENT_SOUNDS;case DISCORD->Page.DISCORD_ACTIVITY;case SCALE->Page.GUI_SCALE;case PERFORMANCE->Page.PERFORMANCE;});
        getClientSettingsEditor().revealDetails();
    }
    public dev.riserecode.ui.component.ClientSettingsEditor getClientSettingsEditor(){
        var section=switch(page){case CLIENT_SOUNDS->dev.riserecode.ui.menu.ClientSettingsScreen.Section.SOUNDS;case DISCORD_ACTIVITY->dev.riserecode.ui.menu.ClientSettingsScreen.Section.DISCORD;case GUI_SCALE->dev.riserecode.ui.menu.ClientSettingsScreen.Section.SCALE;case PERFORMANCE->dev.riserecode.ui.menu.ClientSettingsScreen.Section.PERFORMANCE;default->null;};
        return section==null?null:clientEditors.computeIfAbsent(page,ignored->new dev.riserecode.ui.component.ClientSettingsEditor(section));
    }
    private final dev.riserecode.ui.component.ResourcesEditor resourcesEditor;
    private final dev.riserecode.ui.component.CosmeticsEditor cosmeticsEditor;
    public dev.riserecode.ui.component.CosmeticsEditor getCosmeticsEditor(){return cosmeticsEditor;}
    private final ActionButton pageGear=new ActionButton("",()->minecraft.gui.setScreen(new dev.riserecode.ui.menu.PageAppearanceScreen(this,dev.riserecode.ui.menu.MenuTheme.pageId(this)))).icon(Icon.GEAR);
    private dev.riserecode.ui.component.HeaderLayout header;
    private UIComponent.Bounds contentBounds;
    public dev.riserecode.ui.component.HeaderLayout getHeaderLayout() { return header; }
    private boolean closing;
    private boolean dragging;
    private double dragX;
    private final HudRenderer hud;
    /** HUD element being moved straight from the menu, with the pointer offset inside it. */
    private HudRenderer.Box hudDrag;
    private double hudDragX, hudDragY;
    private double dragY;
    private double offsetX;
    private double offsetY;
    private double windowX;
    private double windowY;
    private double canvasScale = 1;
    private double preferredScale = 1;
    private double canvasWidth;
    private double canvasHeight;
    private double maxUtilityScroll;
    private long previousFrame;
    private double messageTime;
    private String message = "";

    public ClickGui(ModuleManager modules, HudRenderer hud, Runnable saveChanges) {
        super(Component.literal(Client.NAME));
        this.hud = hud;
        this.saveChanges = saveChanges;
        themes = Client.INSTANCE.getThemeManager();
        opening.timing(()->dev.riserecode.ui.animation.PopIn.windowDuration("Client"));
        themeEditor = new ThemeEditor(themes, this::notice);
        resourcesEditor = new dev.riserecode.ui.component.ResourcesEditor(Client.INSTANCE.getResourcesManager(), this::notice);
        cosmeticsEditor = new dev.riserecode.ui.component.CosmeticsEditor(Client.INSTANCE.getCosmetics(), this::notice);
        searchPanel = new Panel(null, modules, saveChanges);
        for (Category category : Category.values()) { panels.add(new Panel(category, modules, saveChanges)); }
        sidebar = new Sidebar(List.of(
                new Sidebar.Item("Search", Icon.SEARCH), new Sidebar.Item("Combat", Icon.COMBAT),
                new Sidebar.Item("Movement", Icon.MOVEMENT), new Sidebar.Item("Player", Icon.PLAYER),
                new Sidebar.Item("Render", Icon.RENDER), new Sidebar.Item("World", Icon.WORLD),
                new Sidebar.Item("Utility", Icon.UTILITY), new Sidebar.Item("HUD", Icon.HUD),
                new Sidebar.Item("Appearance", Icon.THEMES), new Sidebar.Item("Resources", Icon.RENDER),
                new Sidebar.Item("Performance", Icon.GEAR), new Sidebar.Item("Waypoints", Icon.WORLD), new Sidebar.Item("Map settings", Icon.THEMES), new Sidebar.Item("Cosmetics", Icon.PLAYER)), index -> selectPage(NAVIGATION_PAGES.get(index)));
        hudToggle = new ToggleSwitch("HUD", hud::isEnabled, next -> { hud.setEnabled(next); saveChanges.run(); });
        loadPreferences();
    }
    public List<Panel> getPanels() { return List.copyOf(panels); }
    public Panel getActivePanel() {
        if (page == Page.SEARCH) { return searchPanel; }
        if (page.ordinal() > Page.HUD.ordinal()) { return null; }
        Category category = Category.valueOf(page.name());
        return panels.stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow();
    }
    public ModuleButton getModuleButton(String name) {
        Panel panel = getActivePanel();
        if (panel == null) { throw new IllegalStateException("The selected page has no modules"); }
        return panel.getModuleButtons().stream().filter(button -> button.getModule().getName().equals(name)).findFirst().orElseThrow();
    }
    public dev.riserecode.ui.component.ResourcesEditor getResourcesEditor() { return resourcesEditor; }
    public ThemeEditor getThemeEditor() { return themeEditor; }
    public ToggleSwitch getHudToggle() { return hudToggle; }
    public List<ActionButton> getActionButtons() { return List.copyOf(actions); }
    public ActionButton getEditHudButton() { return editHud; }
    public Sidebar getSidebar() { return sidebar; }
    public TextField getSearchField() { return field; }
    public Page getPage() { return page; }
    public double getResizeHintOpacity(){return resizeHint.getValue();}
    public double getCanvasScale() { return canvasScale; }
    public boolean isOpen() { return !closing && !opening.isRunning() && opening.getValue() == 1; }
    public UIComponent.Bounds getWindowBounds() { return new UIComponent.Bounds(windowX, windowY, windowWidth, windowHeight); }

    public UIComponent.Bounds getDisplayedWindowBounds(){double scale=windowScale();return new UIComponent.Bounds(windowX+windowWidth*(1-scale)/2,windowY+windowHeight*(1-scale)/2,windowWidth*scale,windowHeight*scale);}
    private void loadPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        try {
            if (ui.has("selectedTab")) { String saved = ui.get("selectedTab").getAsString(); page = switch (saved) { case "THEMES" -> Page.APPEARANCE; case "STYLES" -> Page.RESOURCES; case "AI", "AGENT" -> Page.SEARCH; case "SETTINGS" -> Page.FRIENDS; case "GAMEPLAY" -> Page.COMBAT; case "VISUALS" -> Page.RENDER; default -> Page.valueOf(saved); }; sidebar.setSelected(NAVIGATION_PAGES.indexOf(page)); }
            if (ui.has("german")) { Theme.setGerman(ui.get("german").getAsBoolean()); }
            preferredScale = themes.number("Interface Scale");
            if (ui.has("offsetX")) { offsetX = finite(ui.get("offsetX").getAsDouble(), -2000, 2000); }
            if (ui.has("offsetY")) { offsetY = finite(ui.get("offsetY").getAsDouble(), -2000, 2000); }
        } catch (RuntimeException invalid) { Client.INSTANCE.getLogger().warn("Ignoring invalid UI preference: {}", invalid.getMessage()); }
    }
    private static double finite(double value, double min, double max) {
        if (!Double.isFinite(value)) { throw new IllegalArgumentException("Non-finite UI value"); }
        return Math.max(min, Math.min(max, value));
    }
    private void persistPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        ui.addProperty("selectedTab", page.name()); ui.remove("accent"); ui.remove("scale"); ui.addProperty("german", Theme.isGerman());
        ui.addProperty("offsetX", offsetX); ui.addProperty("offsetY", offsetY);
        Client.INSTANCE.getConfigManager().setUiPreferences(ui);
    }
    public void refresh() {
        Client.INSTANCE.getKeybindManager().cancelCapture();
        releaseCapture(); searchPanel.refresh(); panels.forEach(Panel::refresh);
        if (page == Page.RESOURCES) { resourcesEditor.resetDraft(); }
        if (page == Page.COSMETICS) { cosmeticsEditor.discard(); }
        if (page == Page.PROFILES || page == Page.FRIENDS) { rebuildActions(); }
    }
    public void selectCategory(Category category) { selectPage(Page.valueOf(category.name())); }
    public void selectPage(Page next) {
        next = switch (next) { case GAMEPLAY -> Page.COMBAT; case VISUALS -> Page.RENDER; default -> next; };
        if (next == Page.SETTINGS) { sidebar.toggleExpanded(); return; }
        if (closing || next == page) { return; }
        Client.INSTANCE.getKeybindManager().cancelCapture();
        if(page==Page.COSMETICS){cosmeticsEditor.discard();}
        dev.riserecode.ui.component.FocusedInput.commit(); releaseCapture(); page = next; sidebar.setSelected(NAVIGATION_PAGES.indexOf(next));
        themeEditor.unfocus(); clientEditors.values().forEach(dev.riserecode.ui.component.ClientSettingsEditor::unfocus); resourcesEditor.unfocus();
        if (next == Page.RESOURCES) { resourcesEditor.open(); }
        if (next == Page.COSMETICS) { cosmeticsEditor.open(); }
        field.setValue(""); field.setFocused(next == Page.SEARCH || next == Page.PROFILES || next == Page.FRIENDS);
        searchPanel.setQuery(""); utilityScroll.reset(0); pageFade.reset(0); pageFade.animateTo(1);
        Panel panel = getActivePanel(); if (panel != null) { panel.open(); }
        rebuildActions();
    }
    public void open(Screen parent) {
        if(!Client.INSTANCE.resourcesReady()||minecraft.gui.overlay()!=null){return;}
        this.parent = parent; closing = false; previousFrame = System.nanoTime(); resourcesEditor.resetDraft();
        opening.reset(0); opening.animateTo(1); searchPanel.open(); panels.forEach(Panel::open);
        field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        if (page == Page.RESOURCES) { resourcesEditor.open(); }
        if (page == Page.COSMETICS) { cosmeticsEditor.open(); }
        minecraft.gui.setScreen(this);
    }
    @Override
    protected void init() {
        windowWidth=themes.number("Window Width"); windowHeight=themes.number("Window Height");
        canvasScale = fittedScale(); canvasZoom.reset(canvasScale);
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        clampOffsets();
        windowX = (canvasWidth - windowWidth) / 2 + offsetX; windowY = (canvasHeight - windowHeight) / 2 + offsetY;
        positionX.reset(windowX); positionY.reset(windowY); layout(); rebuildActions();
    }
    private double fittedScale() {
        return Math.max(0.01, Math.min(preferredScale * height / 576.0, Math.min((width - 20.0) / windowWidth, (height - 20.0) / windowHeight)));
    }
    private void clampOffsets() {
        offsetX = Math.max(-(canvasWidth - windowWidth) / 2 + 8, Math.min((canvasWidth - windowWidth) / 2 - 8, offsetX));
        offsetY = Math.max(-(canvasHeight - windowHeight) / 2 + 8, Math.min((canvasHeight - windowHeight) / 2 - 8, offsetY));
    }
    private long layoutRevision=-1,layoutFontGeneration=-1;
    private Page layoutPage;
    private boolean layoutGerman;
    private double layoutX,layoutY,layoutWidth,layoutHeight,layoutCanvasWidth,layoutCanvasHeight,layoutShift;
    private void layout() {
        double shift=pageShift();long revision=themes.getRevision(),generation=dev.riserecode.ui.font.CustomTextRenderer.generation();
        if(layoutRevision==revision&&layoutFontGeneration==generation&&layoutPage==page&&layoutGerman==Theme.isGerman()
                &&layoutX==windowX&&layoutY==windowY&&layoutWidth==windowWidth&&layoutHeight==windowHeight
                &&layoutCanvasWidth==canvasWidth&&layoutCanvasHeight==canvasHeight&&layoutShift==shift){return;}
        layoutRevision=revision;layoutFontGeneration=generation;layoutPage=page;layoutGerman=Theme.isGerman();
        layoutX=windowX;layoutY=windowY;layoutWidth=windowWidth;layoutHeight=windowHeight;layoutCanvasWidth=canvasWidth;layoutCanvasHeight=canvasHeight;layoutShift=shift;
        sidebar.setBounds(windowX, windowY, 107, windowHeight-32);
        settingsPopup.setBounds(windowX+10,windowY+windowHeight-27,87,17);settingsPopup.viewport(canvasWidth,canvasHeight);
        double pad = themes.number("Content Padding");
        double left = windowX + 107 + pad, contentWidth = windowWidth - 107 - pad * 2;
        double rowHeight = dev.riserecode.ui.component.ButtonMetrics.HEIGHT;
        double actionWidth = page == Page.HUD ? dev.riserecode.ui.component.ButtonMetrics.width(font,"Edit HUD") : 0;
        header = dev.riserecode.ui.component.HeaderLayout.at(left, windowY + pad, contentWidth, rowHeight, actionWidth);
        var label = header.label(); var gear = header.gear();
        field.setBounds(label.x(), label.y(), label.width(), label.height());
        pageGear.setBounds(gear.x(), gear.y(), gear.width(), gear.height());
        if (header.action() != null) { var action = header.action(); editHud.setBounds(action.x(), action.y(), action.width(), action.height()); }
        double bodyY = windowY + pad + rowHeight + pad;
        contentBounds = new UIComponent.Bounds(left, bodyY, contentWidth, Math.max(1, windowY + windowHeight - pad - bodyY));
        searchPanel.setBounds(left, bodyY, contentWidth, contentBounds.height());
        for (Panel panel : panels) { panel.setBounds(left, bodyY, contentWidth, contentBounds.height()); }
        resourcesEditor.setBounds(left, bodyY + pageShift(), contentWidth, contentBounds.height());
        cosmeticsEditor.setBounds(left, bodyY + pageShift(), contentWidth, contentBounds.height());
        themeEditor.setBounds(left, bodyY + pageShift(), contentWidth, contentBounds.height());
        var clientEditor=getClientSettingsEditor();if(clientEditor!=null){clientEditor.setBounds(left,bodyY+pageShift(),contentWidth,contentBounds.height());}
        hudToggle.setBounds(windowX + 129, windowY + 234, 265, 17);
    }
    private String placeholder() {
        return switch (page) {
            case SEARCH -> Theme.label("Start typing to search...");
            case PROFILES -> Theme.isGerman() ? "Profilname eingeben..." : "Enter a profile name...";
            case FRIENDS -> Theme.isGerman() ? "Spielername eingeben..." : "Enter a player name...";
            case CLIENT_SOUNDS -> "Client Sounds";
            case DISCORD_ACTIVITY -> "Discord Activity";
            case GUI_SCALE -> "GUI Scale";
            case PERFORMANCE -> "Performance";
            case HUD -> "HUD";
            case MAP_SETTINGS -> "Map settings";
            default -> Theme.label(page.name().substring(0, 1) + page.name().substring(1).toLowerCase(java.util.Locale.ROOT));
        };
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if(minecraft.level==null){var page=themes.pages().get(dev.riserecode.ui.menu.MenuTheme.pageId(this));if(page==null||page.enabled(true)&&page.backgroundEnabled(true)){if(themes.choice("Background",ThemeManager.Background.class)==ThemeManager.Background.NONE){super.extractBackground(graphics,mouseX,mouseY,delta);}else{dev.riserecode.ui.menu.MenuBackdrop.draw(graphics);}}else{super.extractBackground(graphics,mouseX,mouseY,delta);}}
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime(); double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1e9); previousFrame = now;
        windowWidth=themes.number("Window Width"); windowHeight=themes.number("Window Height");
        preferredScale = themes.number("Interface Scale"); canvasZoom.animateTo(fittedScale());
        canvasScale = Math.max(0.01, canvasZoom.update(seconds));
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        double alpha = opening.update(seconds);
        pageFade.update(seconds);
        if (closing && !opening.isRunning()) { minecraft.gui.setScreen(parent); return; }
        clampOffsets();
        positionX.animateTo((canvasWidth - windowWidth) / 2 + offsetX);
        positionY.animateTo((canvasHeight - windowHeight) / 2 + offsetY);
        windowX = positionX.update(seconds); windowY = positionY.update(seconds); layout();
        double pointerX = pointerX(mouseX); double pointerY = pointerY(mouseY);
        double scale = windowScale();
        drawHudHandles(graphics, mouseX, mouseY, alpha);
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) canvasScale, (float) canvasScale);
        graphics.pose().translate((float) (windowX + windowWidth / 2), (float) (windowY + windowHeight / 2));
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) (-windowX - windowWidth / 2), (float) (-windowY - windowHeight / 2));
        dev.riserecode.ui.render.UiBatch.beginOrdered(graphics);
        try {
            RenderUtil.drawDropShadow(graphics, windowX, windowY, windowWidth, windowHeight, 10,
                    Theme.opacity(Theme.SHADOW, alpha * 0.7));
            BlurUtil.requestWorld(alpha);
            var pageAppearance=themes.pages().get(dev.riserecode.ui.menu.MenuTheme.pageId(this));
            boolean customImage=themes.choice("Background",ThemeManager.Background.class)!=ThemeManager.Background.NONE&&pageAppearance!=null&&!pageAppearance.inherit.getValue()&&pageAppearance.backgroundEnabled(true);
            if(minecraft.level!=null&&!customImage){BlurUtil.drawBox(graphics,windowX,windowY,windowWidth,windowHeight,10,alpha*Theme.number("GUI Blur Opacity",.8));}
            else {dev.riserecode.ui.menu.MenuBackdrop.drawBlur(graphics,windowX,windowY,windowWidth,windowHeight,10,alpha*Theme.number("GUI Blur Opacity",.8));}
            RenderUtil.drawWindow(graphics, windowX, windowY, windowWidth,windowHeight,alpha*.82);
            double cornerDistance=Math.hypot(pointerX-(windowX+windowWidth-6),pointerY-(windowY+windowHeight-6));
            resizeHint.animateTo(resizing?1:Math.clamp((24-cornerDistance)/12,0,1));double hint=resizeHint.update(seconds);
            if(hint>.001){RenderUtil.line(graphics,windowX+windowWidth-10,windowY+windowHeight-4,windowX+windowWidth-4,windowY+windowHeight-10,1,Theme.opacity(Theme.SECONDARY,alpha*hint));}
            sidebar.render(graphics, font, pointerX, pointerY, seconds, alpha);
            pageGear.render(graphics, font, pointerX, pointerY, seconds, alpha);
            double contentAlpha = alpha * pageFade.getValue();
            if (page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) {
                field.render(graphics, font, pointerX, pointerY, seconds, contentAlpha);
            } else {
                var label = header.label();
                String title = RenderUtil.ellipsize(font, placeholder(), label.width());
                RenderUtil.text(graphics, font, title, label.x() + (label.width() - RenderUtil.textWidth(font, title, false)) / 2,
                        label.y() + (label.height() - 9 * Theme.number("Font Scale", 1)) / 2, 1, Theme.opacity(Theme.TEXT, contentAlpha));
            }
            if (page == Page.HUD) { editHud.render(graphics, font, pointerX, pointerY, seconds, contentAlpha); }
            Panel panel = getActivePanel();
            if (panel != null) {
                if (page == Page.SEARCH) { panel.setQuery(field.getValue()); }
                panel.render(graphics, font, closing ? -1e6 : pointerX, closing ? -1e6 : pointerY, seconds, contentAlpha);
            } else { renderUtility(graphics, pointerX, pointerY, seconds, contentAlpha); }
            messageTime = Math.max(0, messageTime - seconds); feedback.animateTo(messageTime > 0 ? 1 : 0);
            double notice = feedback.update(seconds);
            if (notice > 0) {
                double noticeY = windowY + windowHeight - 30;
                double noticeWidth = Math.min(292, RenderUtil.textWidth(font, message, false) * 0.8 + 18);
                RenderUtil.drawDropShadow(graphics, windowX + 119, noticeY, noticeWidth, 21, 5, Theme.opacity(Theme.SHADOW, alpha * notice));
                RenderUtil.drawRoundedRect(graphics, windowX + 119, noticeY, noticeWidth, 21, 5, Theme.opacity(Theme.SURFACE, alpha * notice));
                RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, message, (noticeWidth - 18) / 0.8), windowX + 128,
                        noticeY + 6, 0.8, Theme.opacity(Theme.accent(), alpha * notice));
            }
            settingsPopup.render(graphics,font,pointerX,pointerY,seconds,alpha);
        } finally { try{dev.riserecode.ui.render.UiBatch.end();}finally{graphics.pose().popMatrix();} }
    }
    private void notice(String text) { message = text; messageTime = 3; }
    private void rebuildActions() {
        actions.clear();
        if (page == Page.SETTINGS) {
            actions.add(new ActionButton("Friends", () -> selectPage(Page.FRIENDS)));
            actions.add(new ActionButton("Profiles", () -> selectPage(Page.PROFILES)));
            actions.add(new ActionButton("Waypoints", () -> selectPage(Page.WAYPOINTS)));
            actions.add(new ActionButton("Map settings", () -> selectPage(Page.MAP_SETTINGS)));
        } else if (page == Page.WAYPOINTS) {
            actions.add(new ActionButton("Manage waypoints", () -> minecraft.gui.setScreen(new dev.riserecode.map.WaypointManagerScreen(this, Client.INSTANCE.getMaps()))));
            // waypoint hotkeys are changed right here: click one, then press the new key (Backspace clears, Escape cancels)
            for (var mapping : dev.riserecode.RiseRecodeClient.waypointKeys()) {
                String name = net.minecraft.network.chat.Component.translatable(mapping.getName()).getString();
                actions.add(new ActionButton(() -> listeningKey == mapping ? name + ": press a key..." : name + ": " + (mapping.isUnbound() ? "None" : mapping.getTranslatedKeyMessage().getString()),
                    () -> listeningKey = listeningKey == mapping ? null : mapping));
            }
            actions.add(new ActionButton("Open world map", () -> minecraft.gui.setScreen(new dev.riserecode.map.WorldMapScreen(this, Client.INSTANCE.getMaps()))));
        } else if (page == Page.MAP_SETTINGS) {
            var maps = Client.INSTANCE.getMaps();
            actions.add(new ActionButton(() -> "Record explored terrain: " + (maps.recording() ? "On" : "Off"), () -> maps.recording(!maps.recording())));
            actions.add(new ActionButton(()->"Layer mode: "+maps.mode().name(),maps::cycleMode));
            actions.add(new ActionButton("Map appearance",()->{selectPage(Page.APPEARANCE);themeEditor.reveal("World Map Background");}));
            actions.add(new ActionButton("Open world map", () -> minecraft.gui.setScreen(new dev.riserecode.map.WorldMapScreen(this, maps))));
            actions.add(new ActionButton("Waypoints", () -> selectPage(Page.WAYPOINTS)));
        }
        if (page == Page.PROFILES) {
            actions.add(new ActionButton("Save client profile", () -> {
                try { notice(Client.INSTANCE.getProfileManager().saveProfile(field.getValue()) ? "Profile saved" : "Could not save profile"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (String profile : Client.INSTANCE.getProfileManager().getProfiles()) {
                actions.add(new ActionButton("Client: " + profile, () -> { notice("Loading " + profile); Client.INSTANCE.getProfileManager().loadProfileAsync(profile).thenAccept(success -> notice(success ? "Loaded " + profile : "Could not load " + profile)); }));
            }
            actions.add(new ActionButton("Save appearance profile", () -> {
                try { notice(themes.saveProfile(field.getValue()) ? "Appearance profile saved" : "Could not save appearance profile"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (String profile : themes.getProfiles()) {
                actions.add(new ActionButton("Appearance: " + profile, () -> notice(themes.loadProfile(profile) ? "Appearance loaded" : "Could not load appearance profile")));
            }
        } else if (page == Page.FRIENDS) {
            actions.add(new ActionButton("Add", () -> {
                try { Client.INSTANCE.getFriendManager().add(field.getValue()); field.setValue(""); notice("Friend added"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (var friend : Client.INSTANCE.getFriendManager().getFriends()) {
                actions.add(new ActionButton(friend.name() + "  ×", () -> { Client.INSTANCE.getFriendManager().remove(friend.name()); notice("Friend removed"); rebuildActions(); }));
            }

        }
    }
    private void renderUtility(GuiGraphicsExtractor graphics, double mouseX, double mouseY, double seconds, double alpha) {
        double left = contentBounds.x(); double top = contentBounds.y() + pageShift();
        if (getClientSettingsEditor()!=null) {
            getClientSettingsEditor().render(graphics,font,mouseX,mouseY,seconds,alpha);
        } else if (page == Page.COSMETICS) {
            cosmeticsEditor.render(graphics, font, mouseX, mouseY, seconds, alpha);
        } else if (page == Page.RESOURCES) {
            resourcesEditor.render(graphics, font, mouseX, mouseY, seconds, alpha);
        } else if (page == Page.APPEARANCE) {
            themeEditor.render(graphics, font, mouseX, mouseY, seconds, alpha);
        } else {
            var buttonPositions=dev.riserecode.ui.component.ButtonMetrics.flow(font,actions.stream().map(ActionButton::label).toList(),contentBounds.width());
            double buttonsHeight=dev.riserecode.ui.component.ButtonMetrics.flowHeight(buttonPositions);
            maxUtilityScroll = Math.max(0, buttonsHeight + 25 - contentBounds.height());
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget())));
            double scroll = utilityScroll.update(seconds);
            RenderUtil.scissorPush(graphics, left, top, contentBounds.width(), contentBounds.height());
            try {
                for (int i = 0; i < actions.size(); i++) {
                    var b=buttonPositions.get(i);actions.get(i).setBounds(left+b.x(),top+b.y()-scroll,b.width(),b.height());
                    actions.get(i).render(graphics, font, mouseX, mouseY, seconds, alpha);
                }
                String hint = page == Page.PROFILES ? "Select a profile to load it." : page == Page.FRIENDS ? "Select a friend to remove them." : "";
                RenderUtil.text(graphics, font, hint, left + 7, top + buttonsHeight - scroll + 5, 0.8, Theme.opacity(Theme.SECONDARY, alpha));
            } finally { RenderUtil.scissorPop(graphics); }
        }
    }
    private void card(GuiGraphicsExtractor graphics, double x, double y, double w, double h, String title, String description, double alpha) {
        RenderUtil.drawRoundedRect(graphics, x, y, w, h, 5, Theme.opacity(Theme.SURFACE, alpha));
        RenderUtil.text(graphics, font, title, x + 7, y + 7, 1, Theme.opacity(Theme.TEXT, alpha));
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, description, (w - 14) / 0.8), x + 7, y + 25, 0.8, Theme.opacity(Theme.SECONDARY, alpha * 0.8));
    }
    private double pageShift() { return (1 - pageFade.getValue()) * 5 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION); }
    private double windowScale() { return dev.riserecode.ui.animation.PopIn.windowScale("Client",opening.getValue()); }
    private double pointerX(double mouseX) { double center = windowX + windowWidth / 2; return center + (mouseX / canvasScale - center) / windowScale(); }
    private double pointerY(double mouseY) { double center = windowY + windowHeight / 2; return center + (mouseY / canvasScale - center) / windowScale(); }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || opening.getValue() < 0.5) { return true; }
        if (event.button() == 0 && !overWindow(event.x(), event.y())) {
            HudRenderer.Box box = hud.boxAt(event.x(), event.y());
            if (box != null) { hudDrag = box; hudDragX = event.x() - box.x(); hudDragY = event.y() - box.y(); return true; }
        }
        double x = pointerX(event.x()); double y = pointerY(event.y());
        if(settingsPopup.mouseClicked(x,y,event.button())){return true;}
        if(pageGear.mouseClicked(x,y,event.button())){return true;}
        dev.riserecode.ui.component.FocusedInput.pointer(x,y);
        Panel focusedPanel = getActivePanel();
        if (focusedPanel != null) { focusedPanel.getVisibleButtons().forEach(child -> child.pointerFocus(x, y)); }
        if (page == Page.HUD && editHud.mouseClicked(x, y, event.button())) { return true; }
        if (event.button()==0 && x>=windowX+windowWidth-12 && x<windowX+windowWidth+3 && y>=windowY+windowHeight-12 && y<windowY+windowHeight+3) {
            resizing=true;resizeStartX=event.x()/canvasScale;resizeStartY=event.y()/canvasScale;resizeWidth=windowWidth;resizeHeight=windowHeight;return true;
        }
        if (sidebar.mouseClicked(x, y, event.button())) { return true; }
        if ((page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) && field.mouseClicked(x, y, event.button())) { return true; }
        if (x >= windowX && x < windowX + 107 && y >= windowY && y < windowY + 39 && event.button() == 0) {
            dragging = true; dragX = event.x() / canvasScale - windowX; dragY = event.y() / canvasScale - windowY; return true;
        }
        Panel panel = getActivePanel();
        if (panel != null && panel.mouseClicked(x, y, event.button())) { captured = panel; return true; }
        if(getClientSettingsEditor()!=null&&getClientSettingsEditor().mouseClicked(x,y,event.button())){captured=getClientSettingsEditor();return true;}
        if (page == Page.COSMETICS && cosmeticsEditor.mouseClicked(x, y, event.button())) { captured = cosmeticsEditor; return true; }
        if (page == Page.RESOURCES && resourcesEditor.mouseClicked(x, y, event.button())) { captured = resourcesEditor; return true; }
        if (page == Page.APPEARANCE && themeEditor.mouseClicked(x, y, event.button())) { captured = themeEditor; return true; }
        if (x >= contentBounds.x() && x < contentBounds.x() + contentBounds.width() && y >= contentBounds.y() && y < contentBounds.y() + contentBounds.height()) {
            for (ActionButton action : List.copyOf(actions)) { if (action.mouseClicked(x, y, event.button())) { return true; } }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (closing) { return true; }
        if (hudDrag != null && event.button() == 0) {
            hud.dragTo(hudDrag, event.x() - hudDragX, event.y() - hudDragY, width, height);
            return true;
        }
        if (resizing && event.button()==0) {
            ((dev.riserecode.setting.DoubleSetting)themes.getSetting("Window Width")).setValue(Math.clamp(resizeWidth+(event.x()/canvasScale-resizeStartX)*2,360,900));
            ((dev.riserecode.setting.DoubleSetting)themes.getSetting("Window Height")).setValue(Math.clamp(resizeHeight+(event.y()/canvasScale-resizeStartY)*2,240,700)); themes.changed();return true;
        }
        if (dragging && event.button() == 0) {
            offsetX = event.x() / canvasScale - dragX - (canvasWidth - windowWidth) / 2;
            offsetY = event.y() / canvasScale - dragY - (canvasHeight - windowHeight) / 2; clampOffsets(); return true;
        }
        return captured != null && captured.mouseDragged(pointerX(event.x()), pointerY(event.y()), event.button());
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (hudDrag != null) { hudDrag = null; saveChanges.run(); return true; }
        settingsPopup.mouseReleased(pointerX(event.x()),pointerY(event.y()),event.button());
        if(event.button()==0||event.button()==1){dev.riserecode.ui.animation.ButtonFeedback.releaseAll();sidebar.mouseReleased(pointerX(event.x()),pointerY(event.y()),0);}
        boolean result = resizing || dragging || captured != null; resizing=false;
        if (captured != null) { captured.mouseReleased(pointerX(event.x()), pointerY(event.y()), event.button()); }
        if (dragging) { persistPreferences(); }
        dragging = false; captured = null; return result;
    }
    /** True when the pointer (GUI pixels) is over the menu window, which takes the click instead of HUD elements. */
    private boolean overWindow(double mouseX, double mouseY) {
        var b = getDisplayedWindowBounds(); double x = mouseX / canvasScale, y = mouseY / canvasScale;
        return x >= b.x() && x <= b.x() + b.width() && y >= b.y() && y <= b.y() + b.height();
    }
    /** Outlines every HUD element while the menu is open so it is clear they can be dragged; grid while moving. */
    private void drawHudHandles(GuiGraphicsExtractor graphics, int mouseX, int mouseY, double alpha) {
        if (hudDrag != null && hud.getSnap().getValue()) {
            double step = hud.getGridSize().getValue() * height / 576.0;
            for (double x = 0; x < width; x += step) { RenderUtil.line(graphics, x, 0, x, height, 0.4, Theme.opacity(0x10FFFFFF, alpha)); }
            for (double y = 0; y < height; y += step) { RenderUtil.line(graphics, 0, y, width, y, 0.4, Theme.opacity(0x10FFFFFF, alpha)); }
        }
        HudRenderer.Box hovered = hudDrag != null ? null : overWindow(mouseX, mouseY) ? null : hud.boxAt(mouseX, mouseY);
        for (HudRenderer.Box box : hud.getBoxes()) {
            boolean active = hudDrag != null && hudDrag.element() == box.element() || hovered != null && hovered.element() == box.element();
            int color = active ? Theme.accent() : Theme.opacity(Theme.SECONDARY, 0.45);
            RenderUtil.drawRoundedBorder(graphics, box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4, 2, Theme.opacity(color, alpha), 0.8);
            if (active) {
                double labelY = box.y() >= 12 ? box.y() - 10 : box.y() + box.height() + 3;
                RenderUtil.text(graphics, font, box.element().label(), box.x(), Math.min(height - 9, labelY), 0.8, Theme.opacity(color, alpha));
            }
        }
    }
    private void releaseCapture() { hudDrag = null; resizing=false; dragging = false; if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; } }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) { return true; }
        double x = pointerX(mouseX); double y = pointerY(mouseY);
        if(settingsPopup.isOpen()){return true;}
        if(sidebar.mouseScrolled(x,y,vertical)){return true;}
        Panel panel = getActivePanel(); if (panel != null) { return panel.mouseScrolled(x, y, vertical); }
        if(getClientSettingsEditor()!=null){return getClientSettingsEditor().mouseScrolled(x,y,vertical);}
        if (page == Page.COSMETICS) { return cosmeticsEditor.mouseScrolled(x, y, vertical); }
        if (page == Page.RESOURCES) { return resourcesEditor.mouseScrolled(x, y, vertical); }
        if (page == Page.APPEARANCE) { return themeEditor.mouseScrolled(x, y, vertical); }
        if (x >= contentBounds.x() && x < contentBounds.x() + contentBounds.width() && y >= contentBounds.y() && y < contentBounds.y() + contentBounds.height()) {
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget() - vertical * 24))); return true;
        }
        return false;
    }
    @Override
    public boolean charTyped(CharacterEvent event) { if(settingsPopup.isOpen()){return true;} if (dev.riserecode.ui.component.FocusedInput.character(event)) { return true; }
        if (closing) { return true; }
        Panel panel = getActivePanel();
        if(getClientSettingsEditor()!=null){return getClientSettingsEditor().charTyped(event);}
        return page == Page.APPEARANCE ? themeEditor.charTyped(event) : panel != null && panel.charTyped(event) || field.charTyped(event);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (dev.riserecode.ui.component.setting.KeyControl.capture(event, () -> { })) { return true; }
        if (listeningKey != null) {
            if (event.key() != 256) {
                listeningKey.setKey(event.key() == 259 ? com.mojang.blaze3d.platform.InputConstants.UNKNOWN : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(event.key()));
                net.minecraft.client.KeyMapping.resetMapping(); minecraft.options.save();
            }
            listeningKey = null; return true;
        }
        if(settingsPopup.keyPressed(event)){return true;}
        if (page == Page.COSMETICS && cosmeticsEditor.keyPressed(event)) { return true; }
        if (page == Page.RESOURCES && resourcesEditor.keyPressed(event)) { return true; }
        if (dev.riserecode.ui.component.FocusedInput.key(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)) { return true; }
        if (closing) { return true; }
        if (event.isEscape()) { onClose(); return true; }
        if (event.hasControlDown() && event.key() == 70) { if (page == Page.RESOURCES) { resourcesEditor.search().setFocused(true); resourcesEditor.search().selectAll(); } else { selectPage(Page.SEARCH); field.setFocused(true); } return true; }
        if(page==Page.PERFORMANCE&&getClientSettingsEditor().keyPressed(event,minecraft.keyboardHandler::getClipboard,minecraft.keyboardHandler::setClipboard)){return true;}
        if (!dev.riserecode.ui.component.FocusedInput.active() && (event.key()==262 || event.key()==263)) { if(sidebar.isExpanded() != (event.key()==262)){sidebar.toggleExpanded();} return true; }
        if (event.key() == 258) {
            var keyboardPages=sidebar.keyboardItems().stream().map(NAVIGATION_PAGES::get).toList();
            int current = keyboardPages.indexOf(page) < 0 && event.hasShiftDown() ? 0 : keyboardPages.indexOf(page);
            int next = Math.floorMod(current + (event.hasShiftDown() ? -1 : 1), keyboardPages.size());
            selectPage(keyboardPages.get(next)); sidebar.activateSelected(); dev.riserecode.ui.sound.UiSounds.play(); return true;
        }
        if (event.isConfirmation() && (page == Page.PROFILES || page == Page.FRIENDS) && !actions.isEmpty()) {
            var b = actions.getFirst().getBounds(); actions.getFirst().mouseClicked(b.centerX(), b.centerY(), 0); return true;
        }
        Panel panel = getActivePanel();
        if (panel != null && panel.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)) { return true; }
        if(getClientSettingsEditor()!=null){return getClientSettingsEditor().keyPressed(event,minecraft.keyboardHandler::getClipboard,minecraft.keyboardHandler::setClipboard);}
        return page == Page.APPEARANCE ? themeEditor.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)
                : field.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard);
    }
    /** Right Shift may reverse an exit before the screen is removed, preserving the rendered pose. */
    public void toggleVisibility() {
        if (closing) {
            closing = false; opening.animateTo(1);
            field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        } else { onClose(); }
    }
    @Override
    public void onClose() {
        dev.riserecode.ui.component.FocusedInput.commit();
        if (closing) { return; }
        settingsPopup.close();
        Client.INSTANCE.getKeybindManager().cancelCapture();
        dev.riserecode.ui.animation.ButtonFeedback.cancelAll();releaseCapture(); themeEditor.unfocus(); clientEditors.values().forEach(dev.riserecode.ui.component.ClientSettingsEditor::unfocus); resourcesEditor.discard(); cosmeticsEditor.discard(); themes.save(); closing = true; field.setFocused(false); opening.animateTo(0); persistPreferences(); saveChanges.run();
    }
    @Override
    public void removed() { listeningKey = null; dev.riserecode.ui.component.setting.KeyControl.cancelCapture(); dev.riserecode.ui.animation.ButtonFeedback.cancelAll(); dev.riserecode.ui.component.FocusedInput.commit(); Client.INSTANCE.getKeybindManager().cancelCapture(); themeEditor.unfocus(); clientEditors.values().forEach(dev.riserecode.ui.component.ClientSettingsEditor::unfocus); resourcesEditor.discard(); cosmeticsEditor.discard(); themes.save(); releaseCapture(); persistPreferences(); saveChanges.run(); }
    @Override
    public boolean isPauseScreen() { return false; }
}
