package dev.riserecode.ui.component;

import dev.riserecode.resources.ResourceCatalog;
import dev.riserecode.resources.ResourceEntry;
import dev.riserecode.resources.ResourcesManager;
import dev.riserecode.resources.ResourcesState;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

/** Native offline picker: a searchable catalog, explicit draft, conflict details and top-first ordering. */
public final class ResourcesEditor extends UIComponent {
    private record Card(ResourceEntry entry, ActionButton action) { }
    private record Row(String id, ActionButton up, ActionButton down, ActionButton remove, ActionButton detail) { }
    private final ResourcesManager manager;
    private final Consumer<String> notice;
    private final TextField search = new TextField(() -> "Search resources…");
    private final List<String> selected = new ArrayList<>();
    private final List<Card> cards = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<ActionButton> categoryButtons = new ArrayList<>();
    private final ActionButton category, tools, selectedView, cache, stop, refresh, apply, cancel, back, choose, clear;
    private ResourceCatalog known;
    private String revision = "", query = "", categoryName = "All categories", dragging, focused;
    private ResourceEntry detail;
    private boolean showSelected, categoryMenu, toolsMenu;
    private long menuOpened;
    private double menuScale=1;
    private double menuX(double mx){return x+width/2+(mx-x-width/2)/menuScale;}
    private double menuY(double my){double center=y+28+Math.max(1,height-64)/2;return center+(my-center)/menuScale;}
    private double scroll, maxScroll;
    private final dev.riserecode.ui.animation.Animation scrolling = dev.riserecode.ui.animation.Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.18, dev.riserecode.util.MathUtil.Easing.OUT_QUART);
    public double scrollPosition() { return scroll; }
    public double scrollDestination() { return scrolling.getTarget(); }
    public double maximumScroll() { return maxScroll; }
    private void resetScroll() { scroll = 0; scrolling.reset(0); }
    private void clampScroll(double seconds) {
        if (scrolling.getValue() > maxScroll) { scrolling.reset(maxScroll); }
        scrolling.animateTo(Math.clamp(scrolling.getTarget(), 0, maxScroll));
        scroll = Math.clamp(scrolling.update(seconds), 0, maxScroll);
    }
    private String detailLayout = "";
    private List<String> detailLines = List.of();
    private final SettingsSection maintenanceSection=new SettingsSection("resources:tools","Catalog Tools",()->FocusedInput.commit());
    public ResourcesEditor(ResourcesManager manager, Consumer<String> notice) {
        this.manager = manager; this.notice = notice; search.setMaximum(160);
        tools = new ActionButton("Tools", () -> { toolsMenu=!toolsMenu;menuOpened=System.nanoTime(); categoryMenu=false; detail=null; resetScroll(); });
        category = new ActionButton(() -> categoryName, () -> { categoryMenu = !categoryMenu;toolsMenu=false;menuOpened=System.nanoTime(); detail = null; resetScroll(); });
        selectedView = new ActionButton(() -> showSelected ? "Browse" : "Selected (" + selected.size() + ")", () -> { showSelected = !showSelected; categoryMenu = false; toolsMenu=false; detail = null; resetScroll(); rebuild(); });
        cache = new ActionButton(() -> manager.library().progress().running() ? manager.library().progress().paused() ? "Resume" : "Pause" : manager.library().progress().ready() ? "Check cache" : "Cache all", () -> {
            if (manager.library().progress().running()) { manager.library().pause(); } else { manager.library().start(false); }
        });
        stop = new ActionButton("Stop", () -> manager.library().cancel()).enabled(() -> manager.library().progress().running());
        refresh = new ActionButton("Refresh", () -> manager.library().start(true)).enabled(() -> !manager.library().progress().running());
        apply = new ActionButton(() -> manager.busy() ? "Loading…" : "Apply", this::apply).enabled(() -> !manager.busy() && (selected.isEmpty() || manager.library().progress().ready()));
        cancel = new ActionButton("Cancel", () -> { FocusedInput.cancel(); resetDraft(); notice.accept("Unapplied resource changes discarded"); });
        back = new ActionButton("Back", () -> { detail = null; resetScroll(); });
        choose = new ActionButton(() -> detail != null && selected.contains(detail.id()) ? "Deselect" : "Select", () -> { if (detail != null) { toggle(detail); } });
        clear = new ActionButton("Clear", () -> { selected.clear(); focused = null; rebuild(); });
        resetDraft();
    }
    public TextField search() { return search; }
    public ResourcesState draft() { return selected.isEmpty() ? ResourcesState.empty() : new ResourcesState(revision, selected); }
    public ActionButton getToolsButton(){return tools;}
    public SettingsSection getToolsSection(){return maintenanceSection;}
    public void open() { if (!manager.library().progress().ready() && !manager.library().progress().running()) { manager.library().start(false); } }
    public void resetDraft() { selected.clear(); selected.addAll(manager.state().selected()); revision = manager.state().revision(); dragging = null; detail = null; resetScroll(); rebuild(); }
    public void apply() {
        FocusedInput.commit(); manager.apply(draft()).thenAccept(result -> { notice.accept(result.message()); if (result.success()) { resetDraft(); } });
    }
    public void toggle(ResourceEntry entry) {
        var catalog = manager.library().catalog(); if (catalog == null) { return; }
        if (!revision.equals(catalog.revision()) && selected.stream().anyMatch(id -> !catalog.byId().containsKey(id))) { notice.accept("Remove unavailable selections before using the refreshed catalog"); return; }
        revision = catalog.revision(); if (!selected.remove(entry.id())) { selected.addFirst(entry.id()); }
        focused = entry.id(); rebuild();
        var conflicts = conflicts(entry); if (!conflicts.isEmpty()) { notice.accept("Overlap · use Selected to set top-first priority"); }
        else if (entry.description().toLowerCase(Locale.ROOT).contains("incompatible")) { notice.accept("Compatibility notice · right-click this resource for details"); }
    }
    public void move(String id, int offset) {
        int index = selected.indexOf(id); if (index < 0) { return; }
        int target = Math.clamp(index + offset, 0, selected.size() - 1);
        if (index >= 0 && target != index) { selected.remove(index); selected.add(target, id); focused = id; rebuild(); }
    }
    public boolean keyPressed(KeyEvent event) {
        if (showSelected && detail == null && !categoryMenu && !FocusedInput.active() && !event.hasControlDown()
                && (event.key() == 265 || event.key() == 264) && !selected.isEmpty()) {
            int index = selected.indexOf(focused);
            focused = selected.get(index < 0 ? 0 : Math.clamp(index + (event.key() == 265 ? -1 : 1), 0, selected.size() - 1));
            int next = selected.indexOf(focused); scrolling.animateTo(Math.clamp(next * 46, 0, maxScroll)); return true;
        }
        if (showSelected && detail == null && !categoryMenu && event.hasControlDown()
                && (event.key() == 265 || event.key() == 264) && focused != null && !FocusedInput.active()) {
            move(focused, event.key() == 265 ? -1 : 1); return true;
        }
        return false;
    }
    public void showSelected() { showSelected = true; detail = null; categoryMenu = false; toolsMenu=false; resetScroll(); rebuild(); }
    private List<String> conflicts(ResourceEntry entry) {
        var catalog = manager.library().catalog(); if (catalog == null) { return List.of(); }
        return selected.stream().filter(id -> !id.equals(entry.id()) && (entry.incompatible().contains(id)
                || catalog.byId().containsKey(id) && catalog.byId().get(id).incompatible().contains(entry.id())))
                .map(id -> catalog.byId().containsKey(id) ? catalog.byId().get(id).name() : id).toList();
    }
    private void rebuild() {
        known = manager.library().catalog(); cards.clear(); rows.clear(); categoryButtons.clear();
        categoryButtons.add(new ActionButton("All categories", () -> setCategory("All categories")));
        if (known != null) {
            var categories = new LinkedHashSet<String>();
            for (var entry : known.entries()) { categories.add(entry.category().split(" / ")[0]); categories.add(entry.category()); }
            for (String name : categories) { categoryButtons.add(new ActionButton(name, () -> setCategory(name))); }
            String text = search.getValue().toLowerCase(Locale.ROOT);
            for (var entry : known.entries()) {
                if ((categoryName.equals("All categories") || entry.category().equals(categoryName) || entry.category().startsWith(categoryName + " / ")) && entry.searchText().contains(text)) {
                    cards.add(new Card(entry, new ActionButton(entry.name(), () -> toggle(entry)).tile()));
                }
            }
        }
        for (String id : selected) {
            rows.add(new Row(id, new ActionButton("Up", () -> move(id, -1)), new ActionButton("Down", () -> move(id, 1)),
                    new ActionButton("×", () -> { selected.remove(id); rebuild(); }), new ActionButton("Details", () -> {
                        detail = known == null ? null : known.byId().get(id); focused = id; resetScroll();
                    })));
        }
    }
    public void setCategory(String name) { categoryName = name; categoryMenu = false; toolsMenu=false; showSelected = false; detail = null; resetScroll(); rebuild(); }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double seconds, double alpha) {
        if (known != manager.library().catalog() || !query.equals(search.getValue())) { query = search.getValue(); rebuild(); }
        double categoryWidth=70,selectedWidth=74;
        search.setBounds(x,y,Math.max(40,width-categoryWidth-selectedWidth-48),21);
        category.setBounds(x+width-categoryWidth-selectedWidth-42,y,categoryWidth,21);
        selectedView.setBounds(x+width-selectedWidth-39,y,selectedWidth,21); tools.setBounds(x+width-36,y,36,21);
        for (var control : List.of(search,category,selectedView,tools)) { control.render(g,font,mx,my,seconds,alpha); }
        double top=y+28,available=Math.max(1,height-64),bottom=top+available;
        int columns=Math.clamp((int)((width+8)/140),1,6);
        if (categoryMenu) { maxScroll = Math.max(0, categoryButtons.size() * 24 - available); }
        else if (detail != null) { layoutDetail(font); maxScroll = Math.max(0, 127 + detailLines.size() * 12 - available); }
        else if (showSelected) { maxScroll = Math.max(0, rows.size() * 46 + 25 - available); }
        else { maxScroll = Math.max(0, ((cards.size() + columns - 1) / columns) * 66 - available); }
        clampScroll(seconds);
        double rawMx=mx,rawMy=my,savedAlpha=alpha;
        boolean dropdown=toolsMenu||categoryMenu;
        double menuProgress=dropdown?dev.riserecode.ui.animation.PopIn.progress((System.nanoTime()-menuOpened)*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Dropdowns")):1;
        menuScale=dropdown?dev.riserecode.ui.animation.PopIn.windowScale("Dropdowns",menuProgress):1;
        g.pose().pushMatrix();g.pose().translate((float)(x+width/2),(float)(top+available/2));g.pose().scale((float)menuScale,(float)menuScale);g.pose().translate((float)(-x-width/2),(float)(-top-available/2));
        mx=menuX(mx);my=menuY(my);alpha*=menuProgress;
        RenderUtil.scissorPush(g, x, top, width, available);
        try {
            if (toolsMenu) {
                maxScroll=0;
                var maintenance=List.of(cache,stop,refresh);
                maintenanceSection.setBounds(x,top,width,SettingsSection.HEADER);maintenanceSection.render(g,font,mx,my,seconds,alpha);
                double body=maintenanceSection.bodyHeight(104);RenderUtil.scissorPush(g,x,top+SettingsSection.HEADER,width,body);
                try{for(int i=0;i<maintenance.size();i++){var button=maintenance.get(i);button.setBounds(x,top+SettingsSection.HEADER+i*26,width,22);button.render(g,font,maintenanceSection.isExpanded()?mx:-1e6,maintenanceSection.isExpanded()?my:-1e6,seconds,alpha);}}finally{RenderUtil.scissorPop(g);}
                if(maintenanceSection.isExpanded()&&body>100){RenderUtil.text(g,font,"Cache, pause, cancel or refresh the catalog",x+4,top+SettingsSection.HEADER+86,.7,Theme.opacity(Theme.SECONDARY,alpha));}
            } else if (categoryMenu) {
                maxScroll = Math.max(0, categoryButtons.size() * 24 - available);
                for (int i = 0; i < categoryButtons.size(); i++) { var button = categoryButtons.get(i); button.setBounds(x, top + i * 24 - scroll, width - 3, 21); if (button.y + 21 >= top && button.y < bottom) { button.render(g, font, mx, my, seconds, alpha); } }
            } else if (detail != null) { renderDetail(g, font, mx, my, seconds, alpha, top, available); }
            else if (showSelected) { renderSelected(g, font, mx, my, seconds, alpha, top, available); }
            else {
                double cardWidth = (width - (columns-1)*8) / columns; maxScroll = Math.max(0, ((cards.size() + columns - 1) / columns) * 66 - available);
                for (int i = 0; i < cards.size(); i++) {
                    var card = cards.get(i); double cx = x + i % columns * (cardWidth + 8), cy = top + i / columns * 66 - scroll;
                    card.action().setBounds(cx, cy, cardWidth, 60); if (cy + 60 < top || cy >= bottom) { continue; }
                    boolean picked = selected.contains(card.entry().id());
                    RenderUtil.drawRoundedRect(g, cx, cy, cardWidth, 60, 5, Theme.opacity(card.action().contains(mx, my) ? Theme.HOVER : Theme.SURFACE, alpha));
                    if (picked) { RenderUtil.drawRoundedBorder(g, cx, cy, cardWidth, 60, 5, Theme.opacity(Theme.accent(), alpha), 1); }
                    ResourcesPreviews.draw(g, manager.library(), card.entry(), false, cx + 5, cy + 4, 30, alpha);
                    boolean conflict = !conflicts(card.entry()).isEmpty();
                    RenderUtil.text(g, font, picked ? "Selected" : "Right-click · details", cx + 40, cy + 8, 0.54, Theme.opacity(picked ? Theme.accent() : Theme.SECONDARY, alpha));
                    if (conflict) { RenderUtil.text(g, font, "Overlap", cx + 40, cy + 22, 0.62, Theme.opacity(Theme.secondaryAccent(), alpha)); }
                    RenderUtil.text(g, font, RenderUtil.ellipsize(font, card.entry().name(), (cardWidth - 10) / 0.71), cx + 5, cy + 43, 0.71, Theme.opacity(Theme.TEXT, alpha));
                }
                if (cards.isEmpty()) { RenderUtil.text(g, font, known == null ? "Loading the Vanilla Tweaks catalog…" : "No matching resources", x + 4, top + 12, 0.74, Theme.opacity(Theme.SECONDARY, alpha)); }
            }
        } finally { RenderUtil.scissorPop(g);g.pose().popMatrix(); }
        mx=rawMx;my=rawMy;alpha=savedAlpha;
        if (maxScroll > 0) { double thumb = Math.max(12, available * available / (available + maxScroll)); RenderUtil.drawRoundedRect(g, x + width - 2, top + scroll / maxScroll * (available - thumb), 1.5, thumb, 0.75, Theme.opacity(Theme.accent(), alpha * 0.6)); }
        var progress = manager.library().progress(); String status = manager.busy() ? manager.status() : progress.phase();
        if (progress.running() && progress.total() > 0) { status = (progress.paused() ? "Paused · " : "") + progress.phase() + " " + progress.completed() + "/" + progress.total(); }
        RenderUtil.text(g, font, RenderUtil.ellipsize(font, status, width / 0.6), x, y + height - 32, 0.6, Theme.opacity(Theme.SECONDARY, alpha));
        if (progress.running() && progress.total() > 0) {
            RenderUtil.drawRoundedRect(g, x, y + height - 24, width, 2, 1, Theme.opacity(Theme.SURFACE, alpha));
            RenderUtil.drawRoundedRect(g, x, y + height - 24, width * Math.clamp(progress.completed() / (double) progress.total(), 0, 1), 2, 1, Theme.opacity(Theme.accent(), alpha));
        }
        apply.setBounds(x + width - 103, y + height - 21, 49, 19); cancel.setBounds(x + width - 49, y + height - 21, 49, 19);
        RenderUtil.text(g, font, "Vanilla Tweaks · vanillatweaks.net", x, y + height - 12, 0.5, Theme.opacity(Theme.SECONDARY, alpha));
        apply.render(g, font, mx, my, seconds, alpha); cancel.render(g, font, mx, my, seconds, alpha);
    }
    private void renderSelected(GuiGraphicsExtractor g, Font font, double mx, double my, double seconds, double alpha, double top, double available) {
        clear.setBounds(x + width - 44, top, 44, 19); clear.render(g, font, mx, my, seconds, alpha);
        RenderUtil.text(g, font, "Top wins · drag rows or Ctrl+Up/Down", x + 3, top + 5, 0.63, Theme.opacity(Theme.SECONDARY, alpha));
        maxScroll = Math.max(0, rows.size() * 46 + 25 - available);
        RenderUtil.scissorPush(g, x, top + 24, width, Math.max(0, available - 24));
        try { for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i); double at = top + 25 + i * 46 - scroll;
            row.detail().setBounds(x + 38, at + 20, 47, 17); row.up().setBounds(x + width - 100, at + 20, 29, 17);
            row.down().setBounds(x + width - 67, at + 20, 35, 17); row.remove().setBounds(x + width - 28, at + 20, 21, 17);
            if (at + 42 < top + 24 || at >= top + available) { continue; }
            RenderUtil.drawRoundedRect(g, x, at, width - 4, 42, 4, Theme.opacity(row.id().equals(focused) ? Theme.HOVER : Theme.SURFACE, alpha));
            var entry = known == null ? null : known.byId().get(row.id());
            if (entry != null) { ResourcesPreviews.draw(g, manager.library(), entry, false, x + 5, at + 6, 28, alpha); }
            String label = (i + 1) + ". " + (entry == null ? row.id() + " (unavailable)" : entry.name());
            RenderUtil.text(g, font, RenderUtil.ellipsize(font, label, (width - 44) / 0.73), x + 38, at + 5, 0.73, Theme.opacity(Theme.TEXT, alpha));
            row.up().enabled(() -> selected.indexOf(row.id()) > 0); row.down().enabled(() -> selected.indexOf(row.id()) + 1 < selected.size());
            for (var button : List.of(row.detail(), row.up(), row.down(), row.remove())) { button.render(g, font, mx, my, seconds, alpha); }
        }
        } finally { RenderUtil.scissorPop(g); }
    }
    private void renderDetail(GuiGraphicsExtractor g, Font font, double mx, double my, double seconds, double alpha, double top, double available) {
        back.setBounds(x, top, 45, 20); choose.setBounds(x + width - 65, top, 65, 20); back.render(g, font, mx, my, seconds, alpha); choose.render(g, font, mx, my, seconds, alpha);
        RenderUtil.scissorPush(g, x, top + 25, width, Math.max(0, available - 25));
        try { double start = top + 27 - scroll; ResourcesPreviews.draw(g, manager.library(), detail, true, x + 3, start, 88, alpha);
        RenderUtil.text(g, font, RenderUtil.ellipsize(font, detail.name(), (width - 101) / 0.78), x + 99, start + 5, 0.78, Theme.opacity(Theme.TEXT, alpha));
        RenderUtil.text(g, font, RenderUtil.ellipsize(font, detail.category(), (width - 101) / 0.61), x + 99, start + 22, 0.61, Theme.opacity(Theme.SECONDARY, alpha));
        for (int i = 0; i < detailLines.size(); i++) { RenderUtil.text(g, font, detailLines.get(i), x + 4, start + 98 + i * 12, 0.72, Theme.opacity(Theme.TEXT, alpha)); }
        maxScroll = Math.max(0, 27 + 100 + detailLines.size() * 12 - available);
        } finally { RenderUtil.scissorPop(g); }
    }
    private void layoutDetail(Font font) {
        var overlap = conflicts(detail); String text = detail.description() + (overlap.isEmpty() ? "" : " Overlaps selected: " + String.join(", ", overlap) + ". Set priority in Selected.");
        String layout = text + width + font.hashCode() + dev.riserecode.ui.font.CustomTextRenderer.generation() + Theme.number("Font Scale", 1) + Theme.manager().getSetting("Font Selection").getValue();
        if (!layout.equals(detailLayout)) {
            detailLayout = layout; var lines = new ArrayList<String>(); String line = "";
            for (String word : text.split("\\s+")) { String next = line.isEmpty() ? word : line + " " + word; if (!line.isEmpty() && RenderUtil.textWidth(font, next, false) * 0.72 > width - 10) { lines.add(line); line = word; } else { line = next; } }
            if (!line.isEmpty()) { lines.add(line); } detailLines = List.copyOf(lines);
        }
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!contains(mx, my)) { return false; }
        if (search.mouseClicked(mx, my, button)) { return true; }
        for (var action : List.of(selectedView, category, tools, apply, cancel)) { if (action.mouseClicked(mx, my, button)) { return true; } }
        double top = y + 28, bottom = y + height - 36; if (my < top || my >= bottom) { return true; }
        if(toolsMenu||categoryMenu){mx=menuX(mx);my=menuY(my);}
        if (toolsMenu) { if(maintenanceSection.mouseClicked(mx,my,button)){return true;}if(!maintenanceSection.isExpanded()){return true;}for (var action : List.of(cache,stop,refresh)) { if(action.getBounds().y()+action.getBounds().height()<=top+SettingsSection.HEADER+maintenanceSection.bodyHeight(104)&&action.mouseClicked(mx,my,button)){return true;} } }
        else if (categoryMenu) { for (var action : categoryButtons) { if (action.mouseClicked(mx, my, button)) { return true; } } }
        else if (detail != null) { if (back.mouseClicked(mx, my, button) || choose.mouseClicked(mx, my, button)) { return true; } }
        else if (showSelected) {
            if (clear.mouseClicked(mx, my, button)) { return true; }
            if (my < top + 24) { return true; }
            for (var row : List.copyOf(rows)) {
                for (var action : List.of(row.detail(), row.up(), row.down(), row.remove())) { if (action.mouseClicked(mx, my, button)) { return true; } }
            }
            int index = (int) ((my - top - 25 + scroll) / 46);
            if (button == 0 && index >= 0 && index < selected.size()) { dragging = selected.get(index); focused = dragging; return true; }
        } else {
            for (var card : List.copyOf(cards)) { if (card.action().contains(mx, my)) { if (button == 1) { detail = card.entry(); focused = detail.id(); resetScroll(); } else { card.action().mouseClicked(mx, my, button); } return true; } }
        }
        return true;
    }
    @Override public boolean mouseDragged(double mx, double my, int button) {
        if (dragging == null || button != 0 || !showSelected) { return false; }
        int index = selected.indexOf(dragging), target = Math.clamp((int) ((my - y - 53 + scroll) / 46), 0, selected.size() - 1);
        if (index != target) { selected.remove(index); selected.add(target, dragging); rebuild(); } return true;
    }
    @Override public boolean mouseReleased(double mx, double my, int button) { boolean captured = dragging != null; dragging = null; return captured; }
    @Override public boolean mouseScrolled(double mx, double my, double amount) { if (!contains(mx, my)) { return false; } scrolling.animateTo(Math.clamp(scrolling.getTarget() - amount * 24, 0, maxScroll)); return true; }
    public void unfocus() { search.setFocused(false); dragging = null; }
    public void discard() { unfocus(); resetDraft(); ResourcesPreviews.close(); }
}
