package dev.riserecode.ui.component;

import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Clipped, smoothly scrolling module column. Window movement belongs to the screen host. */
public final class Panel extends UIComponent {
    private final Category category;
    private final ModuleManager manager;
    private final Runnable changed;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation opening = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 0, 0.2, MathUtil.Easing.OUT_QUART);
    private final Animation scroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.18, MathUtil.Easing.OUT_QUART);
    private long registryRevision = -1;
    private List<ModuleButton> filtered = List.of();
    private boolean filterDirty = true;
    private boolean filterGerman;
    private String query = "";
    private boolean revealQuery;
    private final java.util.Map<ModuleButton,Boolean> beforeSearch=new java.util.IdentityHashMap<>();
    private java.util.function.Predicate<Module> group = ignored -> true;
    public void setGroup(java.util.function.Predicate<Module> value) { group=value;filterDirty=true; }

    private double maxScroll;
    private ModuleButton captured;
    private boolean scrollbarDragging;
    private double scrollbarGrab;
    private double thumbHeight(){return Math.max(18,height*height/(height+maxScroll));}
    private void dragScrollbar(double mouseY){double travel=Math.max(1,height-thumbHeight());scroll.reset(Math.clamp((mouseY-y-scrollbarGrab)/travel,0,1)*maxScroll);}
    public void scrollToBoundary(boolean end){scroll.animateTo(end?maxScroll:0);}

    /** A null category represents the global Search page. */
    private final ModuleEditorServices services;
    public Panel(Category category, ModuleManager manager, Runnable changed) { this(category,manager,changed,ModuleEditorServices.live()); }
    public Panel(Category category, ModuleManager manager, Runnable changed, ModuleEditorServices services) {
        this.services=services;
        this.category = category; this.manager = manager; this.changed = changed; syncModules();
    }
    public Category getCategory() { return category; }
    public List<ModuleButton> getModuleButtons() { syncModules(); return List.copyOf(buttons); }
    public List<ModuleButton> getVisibleButtons() { if(filterGerman!=Theme.isGerman()){filterGerman=Theme.isGerman();filterDirty=true;}if (filterDirty) { filtered = buttons.stream().filter(this::matches).toList(); filterDirty = false; } return filtered; }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public boolean isOpen() { return !opening.isRunning() && opening.getValue() == 1; }
    public void open() { mouseReleased(0, 0, 0); opening.reset(0); opening.animateTo(1); }
    public void refresh() { mouseReleased(0, 0, 0); syncModules(); }
    public void setQuery(String value) {
        String next = value.strip().toLowerCase(Locale.ROOT);
        if (next.equals(query)) { return; }
        if(query.isEmpty()&&!next.isEmpty()){buttons.forEach(button->beforeSearch.put(button,button.isExpanded()));}
        query = next; filterDirty = true;
        buttons.forEach(button->button.revealSearchMatch(query));if(query.isEmpty()){beforeSearch.forEach(ModuleButton::setExpanded);beforeSearch.clear();}revealQuery=!query.isEmpty(); mouseReleased(0, 0, 0); scroll.reset(0);
    }
    private boolean matches(ModuleButton button) {
        if (!group.test(button.getModule())) { return false; }
        String haystack = (button.getModule().getName() + " " + Theme.displayName(button.getModule().getName()) + " "
                + (button.getModule() instanceof dev.riserecode.module.modules.NoFallModule ? "NoFall " : "") + button.getModule().getSettings().stream().map(s -> s.getName()+" "+Theme.displayName(s.getName())+" "+s.getDescription()).collect(java.util.stream.Collectors.joining(" ")) + " " + button.getModule().getDescription() + " " + button.getModule().getCategory().getDisplayName()).toLowerCase(Locale.ROOT);
        for (String word : query.split("\\s+")) { if (!haystack.contains(word)) { return false; } }
        return true;
    }
    private void syncModules() {
        if (registryRevision == manager.registryRevision()) { return; }
        registryRevision = manager.registryRevision(); filterDirty = true;
        List<Module> registered = category == null ? manager.getModules() : manager.getByCategory(category);
        buttons.removeIf(button -> !registered.contains(button.getModule()));
        for (Module module : registered) {
            if (buttons.stream().noneMatch(button -> button.getModule() == module)) {
                ModuleButton button = new ModuleButton(module, manager, changed, services); button.setShowCategory(category == null); buttons.add(button);
            }
        }
        buttons.sort(Comparator.comparing(button -> button.getModule().getName(), String.CASE_INSENSITIVE_ORDER));
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        syncModules();
        double amount = opening.update(seconds);
        List<ModuleButton> visible = getVisibleButtons();
        double padding=Theme.number("Content Padding",8);
        double content = 0;
        for (ModuleButton button : visible) { button.update(seconds); content += button.getPreferredHeight() + padding; }
        maxScroll = Math.max(0, content - padding - height);
        if (scroll.getValue() > maxScroll) { scroll.reset(maxScroll); }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget()))); scroll.update(seconds);
        KeyboardNavigation.pushScrollRegion(this::revealControl);
        RenderUtil.scissorPush(graphics, x, y, width, height);
        try {
            double next = y - scroll.getValue() + (1 - amount) * 5 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION);
            for (ModuleButton button : visible) {
                button.setBounds(x, next, width, button.getPreferredHeight());
                if (KeyboardNavigation.collectingOffscreen() || next + button.getPreferredHeight() >= y && next < y + height) { button.render(graphics, font, contains(mouseX, mouseY) ? mouseX : -1e6,
                        contains(mouseX, mouseY) ? mouseY : -1e6, seconds, opacity); }
                next += button.getPreferredHeight() + padding;
            }
            if (visible.isEmpty()) {
                RenderUtil.text(graphics, font, Theme.label(query.isEmpty() ? "No modules in this category" : "No matching modules"),
                        x + 7, y + 9, 0.85, Theme.opacity(Theme.SECONDARY, opacity * amount));
                RenderUtil.text(graphics, font, Theme.isGerman() ? "Suche kürzen oder löschen" : "Try fewer words or clear the search",
                        x + 7, y + 25, .75, Theme.opacity(Theme.SECONDARY, opacity));
            }
        } finally { RenderUtil.scissorPop(graphics); KeyboardNavigation.popScrollRegion(); }
        if(revealQuery){for(var button:visible){var match=button.settledSearchMatch();if(match!=null){revealControl(match);revealQuery=false;break;}}}
        if (maxScroll > 0) {
            double thumb = thumbHeight();
            RenderUtil.drawRoundedRect(graphics, x + width - 5, y + (height - thumb) * scroll.getValue() / maxScroll,
                    4, thumb, 2, Theme.opacity(Theme.accent(), opacity * (scrollbarDragging?.95:.65)));
        }
    }
    private void revealControl(Bounds bounds){
        double shift=bounds.y()<y?bounds.y()-y:Math.max(0,bounds.y()+bounds.height()-y-height);
        if(shift!=0){scroll.reset(Math.clamp(scroll.getValue()+shift,0,maxScroll));}
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        getVisibleButtons().forEach(child -> child.pointerFocus(mouseX, mouseY));
        if (!contains(mouseX, mouseY)) { return false; }
        if(button==0&&maxScroll>0&&mouseX>=x+width-8){double thumbY=y+(height-thumbHeight())*scroll.getValue()/maxScroll;scrollbarGrab=mouseY>=thumbY&&mouseY<thumbY+thumbHeight()?mouseY-thumbY:thumbHeight()/2;scrollbarDragging=true;dragScrollbar(mouseY);return true;}
        for (ModuleButton child : getVisibleButtons()) {
            if (child.mouseClicked(mouseX, mouseY, button)) { captured = child; return true; }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) { if(scrollbarDragging){dragScrollbar(mouseY);return true;}return captured != null && captured.mouseDragged(mouseX, mouseY, button); }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(scrollbarDragging){scrollbarDragging=false;return true;}
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(mouseX, mouseY, button); captured = null; return consumed;
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!contains(mouseX, mouseY)) { return false; }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - amount * 24))); return true;
    }
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) { return getVisibleButtons().stream().anyMatch(button -> button.charTyped(event)); }
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event, java.util.function.Supplier<String> clipboard, java.util.function.Consumer<String> copy) { return getVisibleButtons().stream().anyMatch(button -> button.keyPressed(event, clipboard, copy)); }

}
