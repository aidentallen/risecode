package dev.riserecode.ui.hud;

import com.google.gson.JsonObject;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.hud.HudModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.BlurUtil;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Shared live HUD registry and editor rendering; modules supply data, not drawing code. */
public final class HudRenderer implements AutoCloseable {
    public record EntryState(Module module, double visibility, double y) { }
    public record Box(HudElement element, double x, double y, double width, double height) {
        public boolean contains(double mx, double my) { return mx >= x && mx <= x + width && my >= y && my <= y + height; }
    }
    public static final double ROW_HEIGHT = 11.5;
    private static final class Entry {
        private final Module module;
        private final Animation y = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HUD_REORDERING, 0, 0.24, MathUtil.Easing.OUT_QUART);
        private final Animation slide = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HUD_ENTRANCE, 0, 0.32, MathUtil.Easing.OUT_QUART);
        private boolean active;
        private int width;
        private String label;
        private Entry(Module module) { this.module = module; }

    }
    private final ThemeManager themes;
    private final Minecraft minecraft;
    private final ModuleManager modules;
    private String watermark;
    private final EventBus.Subscription subscription;
    private final Map<Module, Entry> entries = new LinkedHashMap<>();
    private final Map<String, HudElement> elements = new LinkedHashMap<>();
    private final Animation visibility = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HUD_ENTRANCE, 1, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    private final BooleanSetting snap = new BooleanSetting("SnapToGrid", "Snap while dragging", true);
    private final DoubleSetting gridSize = new DoubleSetting("GridSize", "Grid pixels in reference canvas", 6, 2, 32);
    private List<Entry> sorted = List.of();
    private List<Box> boxes = List.of();
    private boolean enabled = true;
    private long sorts, measurements, updates, lastExtractionNanos, fontGeneration = -1;
    private Object fontFace;
    private double fontScale = -1;
    private boolean german;
    private int watermarkWidth;
    public record Statistics(long updates, long sorts, long nameMeasurements, long lastExtractionNanos) { }
    public Statistics statistics() { return new Statistics(updates, sorts, measurements, lastExtractionNanos); }
    public HudRenderer(EventBus bus, Minecraft mc, ModuleManager modules, String watermark) { this(bus, mc, modules, watermark, null); }
    public HudRenderer(EventBus bus, Minecraft mc, ModuleManager modules, String watermark, ThemeManager themes) {
        this.themes = themes; minecraft = mc; this.modules = modules; this.watermark = watermark;
        register(new HudElement("watermark", "Watermark", modules.getByClass(dev.riserecode.module.modules.hud.WatermarkModule.class).orElse(null), 0.004, 0.006));
        HudElement array = new HudElement("arraylist", "ArrayList", modules.getByClass(dev.riserecode.module.modules.hud.ArrayListModule.class).orElse(null), 1, 0); array.layout().alignment.setValue(HudLayout.Alignment.RIGHT); register(array);
        double top = 0.055;
        for (Module module : modules.getModules()) {
            if (module instanceof HudModule hud && !container(module)) { register(new HudElement(module.getName(), Theme.displayName(module.getName()), hud, 0.008, top)); top += 0.048; }
        }
        subscription = bus.subscribe(Render2DEvent.class, this::render);
    }
    public void register(HudElement element) {
        if (elements.putIfAbsent(element.id(), element) != null) { throw new IllegalArgumentException("Duplicate HUD element " + element.id()); }
    }
    public List<HudElement> getElements() { return List.copyOf(elements.values()); }
    public List<Box> getBoxes() { return boxes; }
    public BooleanSetting getSnap() { return snap; }
    public DoubleSetting getGridSize() { return gridSize; }
    public boolean isEnabled() { return themes == null ? enabled : themes.bool("HUD Visible"); }
    public void setEnabled(boolean value) { enabled = value; if (themes != null) { ((BooleanSetting) themes.getSetting("HUD Visible")).setValue(value); themes.changed(); } }
    public List<EntryState> getEntries() { return entries.values().stream().map(e -> new EntryState(e.module, e.slide.getValue(), e.y.getValue())).toList(); }
    public JsonObject snapshotLayouts() {
        JsonObject result = new JsonObject(), values = new JsonObject(); result.add("elements", values);
        elements.forEach((id, element) -> values.add(id, element.layout().snapshot()));
        result.addProperty("snap", snap.getValue()); result.addProperty("gridSize", gridSize.getValue()); return result;
    }
    public void validateLayouts(JsonObject data) {
        if (!data.has("elements") || !data.get("elements").isJsonObject()) { throw new IllegalArgumentException("Missing HUD elements"); }
        for (var entry : data.getAsJsonObject("elements").entrySet()) { if (elements.containsKey(entry.getKey())) { HudLayout.validate(entry.getValue().getAsJsonObject()); } }
        if (data.has("snap") && (!data.get("snap").isJsonPrimitive() || !data.getAsJsonPrimitive("snap").isBoolean())) { throw new IllegalArgumentException("Invalid HUD snap"); }
        if (data.has("gridSize")) {
            if (!data.get("gridSize").isJsonPrimitive() || !data.getAsJsonPrimitive("gridSize").isNumber()) { throw new IllegalArgumentException("Invalid HUD grid"); }
            double size = data.get("gridSize").getAsDouble(); if (!Double.isFinite(size) || size < 2 || size > 32) { throw new IllegalArgumentException("Invalid HUD grid"); }
        }
    }
    public void applyLayouts(JsonObject data) {
        validateLayouts(data); data.getAsJsonObject("elements").entrySet().forEach(entry -> { if (elements.containsKey(entry.getKey())) { elements.get(entry.getKey()).layout().apply(entry.getValue().getAsJsonObject()); } });
        if (data.has("snap")) { snap.setValue(data.get("snap").getAsBoolean()); } if (data.has("gridSize")) { gridSize.setValue(data.get("gridSize").getAsDouble()); }
    }
    private static boolean container(Module module) { return module instanceof dev.riserecode.module.modules.hud.WatermarkModule || module instanceof dev.riserecode.module.modules.hud.ArrayListModule; }
    private boolean listed(Module module) {
        var array = modules.getByClass(dev.riserecode.module.modules.hud.ArrayListModule.class).orElse(null);
        return container(module) ? array != null && array.containers.getValue() : !(module instanceof HudModule) || array != null && array.hud.getValue();
    }
    private void update() {
        updates++;
        Object face = themes == null ? minecraft.font : themes.getSetting("Font Selection").getValue();
        double scale = Theme.number("Font Scale", 1);
        String text = themes == null ? watermark : themes.text("Watermark Text"); boolean brandingChanged = !watermark.equals(text); watermark = text;
        boolean metricsChanged = brandingChanged || fontGeneration != CustomTextRenderer.generation() || fontFace != face || fontScale != scale || german != Theme.isGerman();
        if (metricsChanged) {
            fontGeneration = CustomTextRenderer.generation(); fontFace = face; fontScale = scale; german = Theme.isGerman();
            watermarkWidth = RenderUtil.textWidth(minecraft.font, watermark, false); measurements++;
        }
        boolean dirty = metricsChanged;
        for (Module module : modules.getModules()) {
            if (module.isEnabled() && listed(module) && !entries.containsKey(module)) {
                Entry entry = new Entry(module); entry.label = Theme.displayName(module.getName());
                entry.width = RenderUtil.textWidth(minecraft.font, entry.label, false); measurements++;
                entries.put(module, entry); dirty = true;
            }
        }
        var iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next(); entry.active = entry.module.isEnabled() && listed(entry.module); entry.slide.channel(entry.active ? ThemeManager.Channel.HUD_ENTRANCE : ThemeManager.Channel.HUD_EXIT).animateTo(entry.active ? 1 : 0);
            if (!entry.active && !entry.slide.isRunning() && entry.slide.getValue() == 0) { iterator.remove(); dirty = true; }
            else if (metricsChanged) {
                entry.label = Theme.displayName(entry.module.getName()); entry.width = RenderUtil.textWidth(minecraft.font, entry.label, false); measurements++;
            }
        }
        if (dirty) {
            ArrayList<Entry> next = new ArrayList<>(entries.values());
            next.sort(Comparator.comparingInt((Entry e) -> e.width).reversed().thenComparing(e -> e.module.getName())); sorted = next; sorts++;
        }
        double row = rowHeight();
        for (int i = 0; i < sorted.size(); i++) { sorted.get(i).y.animateTo(i * (row + Theme.number("ArrayList Spacing", 0))); }
        visibility.channel(isEnabled() ? ThemeManager.Channel.HUD_ENTRANCE : ThemeManager.Channel.HUD_EXIT).animateTo(isEnabled() ? 1 : 0);
    }
    private double rowHeight() { return 7.5 * Theme.number("Font Scale", 1) + Theme.number("ArrayList Padding", 2) * 2; }
    private void render(Render2DEvent event) {
        if (minecraft.font == null) { return; }
        if (minecraft.gui.screen() instanceof HudEditorScreen || minecraft.player == null || minecraft.gui.hud.isHidden()) { return; }
        extractHud(event.getGraphics(), false);
    }
    public void extractHud(GuiGraphicsExtractor graphics, boolean edit) {
        long start = System.nanoTime(); update(); List<Box> currentBoxes = new ArrayList<>(elements.size());
        double reference = graphics.guiHeight() / 576.0, canvasWidth = graphics.guiWidth() / reference;
        double global = edit ? 1 : MathUtil.clamp01(visibility.getValue());
        for (HudElement element : elements.values()) {
            double opacity = edit ? 1 : element.opacity() * global;
            if (opacity <= 0 && !edit) { continue; }
            List<HudContent.Line> content = element.module() == null ? List.of() : element.module().content();
            boolean logo = element.module() instanceof dev.riserecode.module.modules.hud.WatermarkModule wm && wm.logo.getValue() && dev.riserecode.ui.theme.BrandingLogo.present();
            double logoSize = logo ? Theme.number("Logo Size",16) : 0;
            var placement = themes == null ? ThemeManager.LogoPlacement.LEFT : themes.choice("Logo Placement",ThemeManager.LogoPlacement.class);
            double padding = Theme.number("ArrayList Padding", 2), w, h;
            if (element.id().equals("watermark")) { w = Math.max(watermarkWidth,placement==ThemeManager.LogoPlacement.ABOVE?logoSize:0) + padding * 2 + 1 + (logo && placement!=ThemeManager.LogoPlacement.ABOVE?logoSize+4:0); h = logo && placement==ThemeManager.LogoPlacement.ABOVE ? rowHeight()+logoSize+5 : Math.max(rowHeight()+1,logoSize+padding*2); }
            else if (element.id().equals("arraylist")) { w = (sorted.isEmpty() ? 70 : sorted.getFirst().width) + padding * 2 + 1; h = Math.max(rowHeight(), sorted.size() * (rowHeight() + Theme.number("ArrayList Spacing", 0))); }
            else { w = content.stream().mapToDouble(this::lineWidth).max().orElse(50) + 8; h = content.stream().mapToDouble(this::lineHeight).sum() + 6; }
            double scale = Math.min(element.layout().scale.getValue(), Math.min(canvasWidth / w, 576 / h)), physicalScale = reference * scale;
            double tx = Math.clamp(element.layout().x() * canvasWidth, 0, Math.max(0, canvasWidth - w * scale));
            double ty = Math.clamp(element.layout().y() * 576, 0, Math.max(0, 576 - h * scale));
            double[] xy = element.position(tx, ty); double x = Math.clamp(xy[0], 0, Math.max(0, canvasWidth - w * scale)), y = Math.clamp(xy[1], 0, Math.max(0, 576 - h * scale));
            if (opacity <= 0 && !edit) { continue; }
            currentBoxes.add(new Box(element, x * reference, y * reference, w * physicalScale, h * physicalScale));
            graphics.pose().pushMatrix(); graphics.pose().translate((float) (x * reference), (float) (y * reference)); graphics.pose().scale((float) physicalScale, (float) physicalScale);
            try {
                if (element.id().equals("arraylist")) { drawArray(graphics, element, w, padding, opacity, edit); }
                else {
                    background(graphics, element, 0, 0, w, h, opacity);
                    if (element.id().equals("watermark")) {
                        double textX=padding+(logo && placement==ThemeManager.LogoPlacement.LEFT?logoSize+4:0), textY=padding+(logo && placement==ThemeManager.LogoPlacement.ABOVE?logoSize+4:0);
                        if (logo) { dev.riserecode.ui.theme.BrandingLogo.drawBeside(graphics,textX,textY,watermarkWidth,opacity); }
                        RenderUtil.paintText(graphics, minecraft.font, watermark, textX, textY, 1, Theme.paint("Watermark Color"), opacity, true);
                        RenderUtil.drawPaintRect(graphics, padding, h - 1, w - padding * 2, 0.7, 0.35, Theme.paint("Watermark Color"), opacity * 0.7);
                    } else {
                        double top = 3;
                        for (HudContent.Line line : content) { drawLine(graphics, element, line, w, top, opacity); top += lineHeight(line); }
                    }
                }
            } finally { graphics.pose().popMatrix(); }
        }
        boxes = List.copyOf(currentBoxes); lastExtractionNanos = System.nanoTime() - start;
    }
    private double lineWidth(HudContent.Line line) { return (line.icon() == null ? 0 : 22) + line.spans().stream().mapToDouble(span -> RenderUtil.textWidth(minecraft.font, span.text(), false)).sum(); }
    private double lineHeight(HudContent.Line line) { return Math.max(line.icon() == null ? 0 : 20, 10 * Theme.number("Font Scale", 1)) + (line.bar() >= 0 ? 6 : 0); }
    private void drawLine(GuiGraphicsExtractor graphics, HudElement element, HudContent.Line line, double width, double y, double opacity) {
        double contentWidth = lineWidth(line), x = aligned(element, width, contentWidth, 4);
        if (line.icon() != null) { graphics.blitSprite(RenderPipelines.GUI_TEXTURED, line.icon(), (int) x, (int) y, 18, 18, Theme.opacity(0xFFFFFFFF, opacity)); x += 22; }
        for (HudContent.Span span : line.spans()) {
            if (span.paint() != null) { RenderUtil.paintText(graphics, minecraft.font, span.text(), x, y + (line.icon() == null ? 0 : 5), 1, span.paint(), opacity, true); }
            else if (span.gradient()) { RenderUtil.accentText(graphics, minecraft.font, span.text(), x, y + (line.icon() == null ? 0 : 5), 1, opacity); }
            else { RenderUtil.hudText(graphics, minecraft.font, span.text(), x, y + (line.icon() == null ? 0 : 5), Theme.opacity(span.color(), opacity)); }
            x += RenderUtil.textWidth(minecraft.font, span.text(), false);
        }
        if (line.bar() >= 0) { double top = y + lineHeight(line) - 5;
            RenderUtil.drawRoundedRect(graphics, 4, top, width - 8, 2, 1, Theme.opacity(0xFF34434F, opacity));
            if (line.barPaint() == null) { RenderUtil.drawRoundedRect(graphics, 4, top, (width - 8) * MathUtil.clamp01(line.bar()), 2, 1, Theme.opacity(line.barColor(), opacity)); }
            else { RenderUtil.drawPaintRect(graphics, 4, top, (width - 8) * MathUtil.clamp01(line.bar()), 2, 1, line.barPaint(), opacity); } }
    }
    private static double aligned(HudElement element, double width, double contentWidth, double padding) {
        return switch (element.layout().alignment.getValue()) { case LEFT -> padding; case CENTER -> (width - contentWidth) / 2; case RIGHT -> width - contentWidth - padding; };
    }
    private void drawArray(GuiGraphicsExtractor graphics, HudElement element, double width, double padding, double opacity, boolean edit) {
        if (sorted.isEmpty() && edit) { background(graphics, element, 0, 0, width, rowHeight(), 1); RenderUtil.accentText(graphics, minecraft.font, "ArrayList", padding, padding, 1, 1); }
        for (Entry entry : sorted) {
            double alpha = opacity * MathUtil.clamp01(entry.slide.getValue());
            if (alpha <= 0) { continue; }
            double rowWidth = entry.width + padding * 2 + 1;
            double x = aligned(element, width, rowWidth, 0) + (1 - entry.slide.getValue()) * (width + 8) * Animation.movement(entry.active ? ThemeManager.Channel.HUD_ENTRANCE : ThemeManager.Channel.HUD_EXIT), y = entry.y.getValue();
            background(graphics, element, x, y, rowWidth, rowHeight(), alpha);
            RenderUtil.paintText(graphics, minecraft.font, entry.label, x + padding, y + padding, 1, Theme.paint("ArrayList Color"), alpha, true);
        }
    }
    private void background(GuiGraphicsExtractor g, HudElement element, double x, double y, double w, double h, double opacity) {
        HudLayout.Background kind = element.layout().background.getValue();
        if (kind == HudLayout.Background.INHERIT) { kind = switch (themes == null ? ThemeManager.HudBackground.SOLID_BOX : themes.choice("HUD Background", ThemeManager.HudBackground.class)) {
            case NONE -> HudLayout.Background.NONE; case SOLID_BOX -> HudLayout.Background.SOLID; case BLUR_BOX -> HudLayout.Background.BLUR; }; }
        if (kind == HudLayout.Background.NONE) { return; }
        if (kind == HudLayout.Background.BLUR) { BlurUtil.drawBox(g, x, y, w, h, 3, opacity * 0.8); }
        ColorSetting.Paint paint = element.layout().backgroundColor.getMode() == ColorSetting.Mode.THEME ? Theme.paint("HUD Background Color") : element.layout().backgroundColor.paint();
        RenderUtil.drawPaintRect(g, x, y, w, h, 3, paint, opacity * (kind == HudLayout.Background.BLUR ? 0.3 : 0.8));
        if (element.layout().border.getValue() == HudLayout.Border.ON
                || element.layout().border.getValue() == HudLayout.Border.INHERIT && Theme.bool("HUD Border", false)) {
            RenderUtil.drawRoundedBorder(g, x, y, w, h, 3, Theme.opacity(0xFFFFFFFF, opacity * Theme.number("Border Opacity", 0.08)));
        }
    }
    @Override public void close() { subscription.close(); entries.clear(); elements.clear(); }
}
