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
    public enum Page { SEARCH, COMBAT, MOVEMENT, PLAYER, RENDER, WORLD, UTILITY, HUD, PROFILES, FRIENDS, APPEARANCE }
    private static final double WINDOW_WIDTH = 428;
    private static final double WINDOW_HEIGHT = 320;
    private final List<Panel> panels = new ArrayList<>();
    private final Panel searchPanel;
    private final Sidebar sidebar;
    private final TextField field = new TextField(this::placeholder);
    private final ToggleSwitch hudToggle;
    private final ActionButton editHud = new ActionButton("Edit HUD", () -> Client.INSTANCE.openHudEditor());
    private final Runnable saveChanges;
    private final List<ActionButton> actions = new ArrayList<>();
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
    private boolean closing;
    private boolean dragging;
    private double dragX;
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
        this.saveChanges = saveChanges;
        themes = Client.INSTANCE.getThemeManager();
        themeEditor = new ThemeEditor(themes, this::notice);
        searchPanel = new Panel(null, modules, saveChanges);
        for (Category category : Category.values()) { panels.add(new Panel(category, modules, saveChanges)); }
        sidebar = new Sidebar(List.of(
                new Sidebar.Item("Search", Icon.SEARCH), new Sidebar.Item("Combat", Icon.COMBAT),
                new Sidebar.Item("Movement", Icon.MOVEMENT), new Sidebar.Item("Player", Icon.PLAYER),
                new Sidebar.Item("Render", Icon.RENDER), new Sidebar.Item("World", Icon.WORLD),
                new Sidebar.Item("Utility", Icon.UTILITY), new Sidebar.Item("HUD", Icon.HUD), new Sidebar.Item("Profiles", Icon.PROFILES),
                new Sidebar.Item("Friends", Icon.FRIENDS), new Sidebar.Item("Appearance", Icon.THEMES)), index -> selectPage(Page.values()[index]));
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
    public ThemeEditor getThemeEditor() { return themeEditor; }
    public ToggleSwitch getHudToggle() { return hudToggle; }
    public List<ActionButton> getActionButtons() { return List.copyOf(actions); }
    public ActionButton getEditHudButton() { return editHud; }
    public Sidebar getSidebar() { return sidebar; }
    public TextField getSearchField() { return field; }
    public Page getPage() { return page; }
    public double getCanvasScale() { return canvasScale; }
    public boolean isOpen() { return !closing && !opening.isRunning() && opening.getValue() == 1; }
    public UIComponent.Bounds getWindowBounds() { return new UIComponent.Bounds(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT); }

    private void loadPreferences() {
        JsonObject ui = Client.INSTANCE.getConfigManager().getUiPreferences();
        try {
            if (ui.has("selectedTab")) { String saved = ui.get("selectedTab").getAsString(); page = Page.valueOf(saved.equals("THEMES") ? "APPEARANCE" : saved); sidebar.setSelected(page.ordinal()); }
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
        if (page == Page.PROFILES || page == Page.FRIENDS) { rebuildActions(); }
    }
    public void selectCategory(Category category) { selectPage(Page.valueOf(category.name())); }
    public void selectPage(Page next) {
        if (closing || next == page) { return; }
        Client.INSTANCE.getKeybindManager().cancelCapture();
        dev.riserecode.ui.component.FocusedInput.commit(); releaseCapture(); page = next; sidebar.setSelected(next.ordinal());
        themeEditor.unfocus();
        field.setValue(""); field.setFocused(next == Page.SEARCH || next == Page.PROFILES || next == Page.FRIENDS);
        searchPanel.setQuery(""); utilityScroll.reset(0); pageFade.reset(0); pageFade.animateTo(1);
        Panel panel = getActivePanel(); if (panel != null) { panel.open(); }
        rebuildActions();
    }
    public void open(Screen parent) {
        this.parent = parent; closing = false; previousFrame = System.nanoTime();
        opening.reset(0); opening.animateTo(1); searchPanel.open(); panels.forEach(Panel::open);
        field.setFocused(page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS);
        minecraft.gui.setScreen(this);
    }
    @Override
    protected void init() {
        canvasScale = fittedScale(); canvasZoom.reset(canvasScale);
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        clampOffsets();
        windowX = (canvasWidth - WINDOW_WIDTH) / 2 + offsetX; windowY = (canvasHeight - WINDOW_HEIGHT) / 2 + offsetY;
        positionX.reset(windowX); positionY.reset(windowY); layout(); rebuildActions();
    }
    private double fittedScale() {
        return Math.max(0.1, Math.min(preferredScale * height / 576.0, Math.min((width - 20.0) / WINDOW_WIDTH, (height - 20.0) / WINDOW_HEIGHT)));
    }
    private void clampOffsets() {
        offsetX = Math.max(-(canvasWidth - WINDOW_WIDTH) / 2 + 8, Math.min((canvasWidth - WINDOW_WIDTH) / 2 - 8, offsetX));
        offsetY = Math.max(-(canvasHeight - WINDOW_HEIGHT) / 2 + 8, Math.min((canvasHeight - WINDOW_HEIGHT) / 2 - 8, offsetY));
    }
    private void layout() {
        sidebar.setBounds(windowX, windowY, 107, 320);
        double pad = themes.number("Content Padding");
        double left = windowX + 107 + pad, contentWidth = WINDOW_WIDTH - 107 - pad * 2;
        field.setBounds(left, windowY + pad - 1, contentWidth, 27);
        searchPanel.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2);
        for (Panel panel : panels) { panel.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2); }
        themeEditor.setBounds(left, windowY + 30 + pad, contentWidth, WINDOW_HEIGHT - 30 - pad * 2);
        hudToggle.setBounds(windowX + 129, windowY + 234, 265, 17);
    }
    private String placeholder() {
        return switch (page) {
            case SEARCH -> Theme.label("Start typing to search...");
            case PROFILES -> Theme.isGerman() ? "Profilname eingeben..." : "Enter a profile name...";
            case FRIENDS -> Theme.isGerman() ? "Spielername eingeben..." : "Enter a player name...";
            case HUD -> "HUD";
            default -> Theme.label(page.name().substring(0, 1) + page.name().substring(1).toLowerCase(java.util.Locale.ROOT));
        };
    }
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) { extractPanorama(graphics, delta); }
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime(); double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1e9); previousFrame = now;
        preferredScale = themes.number("Interface Scale"); canvasZoom.animateTo(fittedScale());
        canvasScale = Math.max(0.1, canvasZoom.update(seconds));
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        double alpha = opening.update(seconds);
        if (closing && !opening.isRunning()) { minecraft.gui.setScreen(parent); return; }
        clampOffsets();
        positionX.animateTo((canvasWidth - WINDOW_WIDTH) / 2 + offsetX);
        positionY.animateTo((canvasHeight - WINDOW_HEIGHT) / 2 + offsetY);
        windowX = positionX.update(seconds); windowY = positionY.update(seconds); layout();
        double pointerX = pointerX(mouseX); double pointerY = pointerY(mouseY);
        double scale = windowScale();
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) canvasScale, (float) canvasScale);
        graphics.pose().translate((float) (windowX + WINDOW_WIDTH / 2), (float) (windowY + WINDOW_HEIGHT / 2));
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) (-windowX - WINDOW_WIDTH / 2), (float) (-windowY - WINDOW_HEIGHT / 2));
        try {
            RenderUtil.drawDropShadow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10,
                    Theme.opacity(Theme.SHADOW, alpha * 0.7));
            BlurUtil.drawGui(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10, alpha);
            RenderUtil.drawWindow(graphics, windowX, windowY, alpha);
            sidebar.render(graphics, font, pointerX, pointerY, seconds, alpha);
            double contentAlpha = alpha * pageFade.update(seconds);
            if (page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) {
                field.render(graphics, font, pointerX, pointerY, seconds, contentAlpha);
            } else {
                String title = placeholder();
                RenderUtil.text(graphics, font, title, windowX + 267.5 - RenderUtil.textWidth(font, title, false) / 2,
                        windowY + 18, 1, Theme.opacity(Theme.TEXT, contentAlpha));
            }
            if (page == Page.HUD) { editHud.setBounds(windowX + 352, windowY + 9, 64, 21); editHud.render(graphics, font, pointerX, pointerY, seconds, contentAlpha); }
            Panel panel = getActivePanel();
            if (panel != null) {
                if (page == Page.SEARCH) { panel.setQuery(field.getValue()); }
                panel.render(graphics, font, closing ? -1e6 : pointerX, closing ? -1e6 : pointerY, seconds, contentAlpha);
            } else { renderUtility(graphics, pointerX, pointerY, seconds, contentAlpha); }
            messageTime = Math.max(0, messageTime - seconds); feedback.animateTo(messageTime > 0 ? 1 : 0);
            double notice = feedback.update(seconds);
            if (notice > 0) {
                double noticeWidth = Math.min(292, RenderUtil.textWidth(font, message, false) * 0.8 + 18);
                RenderUtil.drawDropShadow(graphics, windowX + 119, windowY + 290, noticeWidth, 21, 5, Theme.opacity(Theme.SHADOW, alpha * notice));
                RenderUtil.drawRoundedRect(graphics, windowX + 119, windowY + 290, noticeWidth, 21, 5, Theme.opacity(Theme.SURFACE, alpha * notice));
                RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, message, (noticeWidth - 18) / 0.8), windowX + 128,
                        windowY + 296, 0.8, Theme.opacity(Theme.accent(), alpha * notice));
            }
        } finally { graphics.pose().popMatrix(); }
    }
    private void notice(String text) { message = text; messageTime = 3; }
    private void rebuildActions() {
        actions.clear();
        if (page == Page.PROFILES) {
            actions.add(new ActionButton("Save", () -> {
                try { notice(Client.INSTANCE.getProfileManager().saveProfile(field.getValue()) ? "Profile saved" : "Could not save profile"); rebuildActions(); }
                catch (IllegalArgumentException invalid) { notice(invalid.getMessage()); }
            }));
            for (String profile : Client.INSTANCE.getProfileManager().getProfiles()) {
                actions.add(new ActionButton(profile, () -> notice(Client.INSTANCE.getProfileManager().loadProfile(profile)
                        ? "Loaded " + profile : "Could not load " + profile)));
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
        double left = windowX + 115; double top = windowY + 38;
        if (page == Page.APPEARANCE) {
            themeEditor.render(graphics, font, mouseX, mouseY, seconds, alpha);
        } else {
            maxUtilityScroll = Math.max(0, actions.size() * 35.0 + 25 - 274);
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget())));
            double scroll = utilityScroll.update(seconds);
            RenderUtil.scissorPush(graphics, left, top, 305, 274);
            try {
                for (int i = 0; i < actions.size(); i++) {
                    actions.get(i).setBounds(left, top + i * 35 - scroll, 305, 27);
                    actions.get(i).render(graphics, font, mouseX, mouseY, seconds, alpha);
                }
                String hint = page == Page.PROFILES ? "Select a profile to load it." : "Select a friend to remove them.";
                RenderUtil.text(graphics, font, hint, left + 7, top + actions.size() * 35 - scroll + 5, 0.8, Theme.opacity(Theme.SECONDARY, alpha));
            } finally { RenderUtil.scissorPop(graphics); }
        }
    }
    private void card(GuiGraphicsExtractor graphics, double x, double y, double w, double h, String title, String description, double alpha) {
        RenderUtil.drawRoundedRect(graphics, x, y, w, h, 5, Theme.opacity(Theme.SURFACE, alpha));
        RenderUtil.text(graphics, font, title, x + 7, y + 7, 1, Theme.opacity(Theme.TEXT, alpha));
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, description, (w - 14) / 0.8), x + 7, y + 25, 0.8, Theme.opacity(Theme.SECONDARY, alpha * 0.8));
    }
    private double windowScale() { return MathUtil.lerp(1 - 0.06 * Animation.scale(ThemeManager.Channel.OPENING), 1, opening.getValue()); }
    private double pointerX(double mouseX) { double center = windowX + WINDOW_WIDTH / 2; return center + (mouseX / canvasScale - center) / windowScale(); }
    private double pointerY(double mouseY) { double center = windowY + WINDOW_HEIGHT / 2; return center + (mouseY / canvasScale - center) / windowScale(); }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing || opening.getValue() < 0.5) { return true; }
        double x = pointerX(event.x()); double y = pointerY(event.y());
        dev.riserecode.ui.component.FocusedInput.pointer(x,y);
        Panel focusedPanel = getActivePanel();
        if (focusedPanel != null) { focusedPanel.getVisibleButtons().forEach(child -> child.pointerFocus(x, y)); }
        if (page == Page.HUD && editHud.mouseClicked(x, y, event.button())) { return true; }
        if (sidebar.mouseClicked(x, y, event.button())) { return true; }
        if ((page == Page.SEARCH || page == Page.PROFILES || page == Page.FRIENDS) && field.mouseClicked(x, y, event.button())) { return true; }
        if (x >= windowX && x < windowX + 107 && y >= windowY && y < windowY + 39 && event.button() == 0) {
            dragging = true; dragX = event.x() / canvasScale - windowX; dragY = event.y() / canvasScale - windowY; return true;
        }
        Panel panel = getActivePanel();
        if (panel != null && panel.mouseClicked(x, y, event.button())) { captured = panel; return true; }
        if (page == Page.APPEARANCE && themeEditor.mouseClicked(x, y, event.button())) { captured = themeEditor; return true; }
        if (x >= windowX + 115 && x < windowX + 420 && y >= windowY + 38 && y < windowY + 312) {
            for (ActionButton action : List.copyOf(actions)) { if (action.mouseClicked(x, y, event.button())) { return true; } }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (closing) { return true; }
        if (dragging && event.button() == 0) {
            offsetX = event.x() / canvasScale - dragX - (canvasWidth - WINDOW_WIDTH) / 2;
            offsetY = event.y() / canvasScale - dragY - (canvasHeight - WINDOW_HEIGHT) / 2; clampOffsets(); return true;
        }
        return captured != null && captured.mouseDragged(pointerX(event.x()), pointerY(event.y()), event.button());
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean result = dragging || captured != null;
        if (captured != null) { captured.mouseReleased(pointerX(event.x()), pointerY(event.y()), event.button()); }
        if (dragging) { persistPreferences(); }
        dragging = false; captured = null; return result;
    }
    private void releaseCapture() { dragging = false; if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; } }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) { return true; }
        double x = pointerX(mouseX); double y = pointerY(mouseY);
        Panel panel = getActivePanel(); if (panel != null) { return panel.mouseScrolled(x, y, vertical); }
        if (page == Page.APPEARANCE) { return themeEditor.mouseScrolled(x, y, vertical); }
        if (x >= windowX + 115 && x < windowX + 420 && y >= windowY + 38 && y < windowY + 312) {
            utilityScroll.animateTo(Math.max(0, Math.min(maxUtilityScroll, utilityScroll.getTarget() - vertical * 24))); return true;
        }
        return false;
    }
    @Override
    public boolean charTyped(CharacterEvent event) { if (dev.riserecode.ui.component.FocusedInput.character(event)) { return true; }
        if (closing) { return true; }
        Panel panel = getActivePanel();
        return page == Page.APPEARANCE ? themeEditor.charTyped(event) : panel != null && panel.charTyped(event) || field.charTyped(event);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (dev.riserecode.ui.component.FocusedInput.key(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)) { return true; }
        if (closing) { return true; }
        if (event.isEscape()) { onClose(); return true; }
        if (event.hasControlDown() && event.key() == 70) { selectPage(Page.SEARCH); field.setFocused(true); return true; }
        if (event.key() == 258) {
            int next = Math.floorMod(sidebar.getSelected() + (event.hasShiftDown() ? -1 : 1), Page.values().length);
            selectPage(Page.values()[next]); return true;
        }
        if (event.isConfirmation() && (page == Page.PROFILES || page == Page.FRIENDS) && !actions.isEmpty()) {
            var b = actions.getFirst().getBounds(); actions.getFirst().mouseClicked(b.centerX(), b.centerY(), 0); return true;
        }
        Panel panel = getActivePanel();
        if (panel != null && panel.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)) { return true; }
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
        if (closing) { return; }
        Client.INSTANCE.getKeybindManager().cancelCapture();
        releaseCapture(); themeEditor.unfocus(); themes.save(); closing = true; field.setFocused(false); opening.animateTo(0); persistPreferences(); saveChanges.run();
    }
    @Override
    public void removed() { dev.riserecode.ui.component.FocusedInput.commit(); Client.INSTANCE.getKeybindManager().cancelCapture(); themeEditor.unfocus(); themes.save(); releaseCapture(); persistPreferences(); saveChanges.run(); }
    @Override
    public boolean isPauseScreen() { return false; }
}
