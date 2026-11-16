package dev.riserecode.map;

import dev.riserecode.RiseRecodeClient;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** All waypoints of the current world, one dimension at a time, with the keys that create and open them. */
public final class WaypointManagerScreen extends Screen {
    private static final int ROW = 28, LIST_TOP = 70, FOOTER = 66;
    private record Hit(double x, double y, double width, double height, boolean inList, Runnable action) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private final Screen parent;
    private final MapService maps;
    private final List<Hit> hits = new ArrayList<>();
    private String dimension;
    private double scroll;
    private boolean drawingRows;
    private UUID confirmDelete;
    private long confirmUntil;

    public WaypointManagerScreen(Screen parent, MapService maps) {
        super(Component.literal("Waypoints"));
        this.parent = parent; this.maps = maps; this.dimension = maps.dimension();
    }

    private int left() { return width / 2 - Math.min(230, width / 2 - 10); }
    private int paneWidth() { return width - left() * 2; }
    private int listHeight() { return Math.max(ROW, height - LIST_TOP - FOOTER); }

    public static String dimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> {
                String path = id.substring(id.indexOf(':') + 1).replace('_', ' ');
                yield path.isEmpty() ? id : Character.toUpperCase(path.charAt(0)) + path.substring(1);
            }
        };
    }

    private List<WaypointStore.Waypoint> visible() {
        var player = minecraft.player;
        var list = new ArrayList<>(maps.waypoints().displayed().stream().filter(p -> p.dimension().equals(dimension)).toList());
        boolean here = dimension.equals(maps.dimension()) && player != null;
        list.sort(here
            ? Comparator.comparingDouble(p -> player.blockPosition().distSqr(new net.minecraft.core.BlockPos(p.x(), p.y(), p.z())))
            : Comparator.comparing(WaypointStore.Waypoint::name, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private void button(GuiGraphicsExtractor g, double x, double y, double w, double h, String label, boolean active, int accent, int mx, int my, Runnable action) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        int fill = active ? Theme.opacity(accent, hover ? 0.7 : 0.5) : hover ? 0x40FFFFFF : 0x28FFFFFF;
        RenderUtil.drawRoundedRect(g, x, y, w, h, Math.min(8, h / 2), fill);
        double textScale = 0.85, textWidth = RenderUtil.textWidth(font, label, false) * textScale;
        RenderUtil.text(g, font, label, x + (w - textWidth) / 2, y + (h - 9 * textScale) / 2 + 0.5, textScale, Theme.TEXT);
        hits.add(new Hit(x, y, w, h, drawingRows, action));
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xB30F1115);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        hits.clear();
        int left = left(), w = paneWidth();
        RenderUtil.drawSurfaceTransition(g, left, 8, w, height - 16, 12, 0, 1);
        RenderUtil.text(g, font, "Waypoints", left + 14, 18, 1.2, Theme.TEXT);

        // dimension tabs
        double tabX = left + 14;
        for (String id : maps.dimensions()) {
            long count = maps.waypoints().displayed().stream().filter(p -> p.dimension().equals(id)).count();
            if (count == 0 && !id.equals(dimension) && !id.equals(maps.dimension())) { continue; }
            String label = dimensionName(id) + (count > 0 ? " " + count : "");
            double tabWidth = RenderUtil.textWidth(font, label, false) * 0.85 + 18;
            if (tabX + tabWidth > left + w - 14) { break; }
            button(g, tabX, 38, tabWidth, 20, label, id.equals(dimension), Theme.accent(), mouseX, mouseY, () -> { dimension = id; scroll = 0; });
            tabX += tabWidth + 5;
        }

        // list
        var points = visible();
        int viewTop = LIST_TOP, viewHeight = listHeight();
        double maximum = Math.max(0, points.size() * ROW - viewHeight);
        scroll = Math.clamp(scroll, 0, maximum);
        RenderUtil.scissorPush(g, left + 6, viewTop, w - 12, viewHeight);
        drawingRows = true;
        try {
            if (points.isEmpty()) {
                RenderUtil.text(g, font, "No waypoints in " + dimensionName(dimension) + " yet. Press " + keyName(RiseRecodeClient.waypointKey()) + " in the world or use Add here.",
                    left + 14, viewTop + 10, 0.85, Theme.SECONDARY);
            }
            for (int i = 0; i < points.size(); i++) {
                double y = viewTop + i * ROW - scroll;
                if (y + ROW < viewTop || y > viewTop + viewHeight) { continue; }
                row(g, points.get(i), left, y, w, mouseX, mouseY);
            }
        } finally { drawingRows = false; RenderUtil.scissorPop(g); }

        // key bindings
        double keyY = height - FOOTER + 4;
        button(g, left + 14, keyY, w - 28, 20, "Keybinds: add " + keyName(RiseRecodeClient.waypointKey()) + " · map " + keyName(RiseRecodeClient.mapKey()) + " · manager " + keyName(RiseRecodeClient.waypointKeys().get(2)) + "  (click to change)", false, 0, mouseX, mouseY,
            () -> minecraft.gui.setScreen(new WaypointKeysScreen(this)));

        // footer
        double footY = height - 34, footWidth = (w - 28 - 12) / 4.0;
        button(g, left + 14, footY, footWidth, 22, "Add here", false, 0, mouseX, mouseY, () -> {
            var point = maps.addAtPlayer();
            if (point != null) { dimension = point.dimension(); minecraft.gui.setScreen(new WaypointEditScreen(this, maps, point)); }
        });
        button(g, left + 14 + (footWidth + 4), footY, footWidth, 22, maps.waypoints().hideReached() ? "Show reached" : "Hide reached", false, 0, mouseX, mouseY,
            () -> maps.waypoints().hideReached(!maps.waypoints().hideReached()));
        button(g, left + 14 + 2 * (footWidth + 4), footY, footWidth, 22, "Deaths", false, 0, mouseX, mouseY,
            () -> minecraft.gui.setScreen(new WaypointScreen(this, maps)));
        button(g, left + 14 + 3 * (footWidth + 4), footY, footWidth, 22, "Done", true, Theme.accent(), mouseX, mouseY, this::onClose);
    }

    private void row(GuiGraphicsExtractor g, WaypointStore.Waypoint p, int left, double y, int w, int mx, int my) {
        boolean hover = mx >= left + 8 && mx < left + w - 8 && my >= y && my < y + ROW - 2 && my >= LIST_TOP && my < LIST_TOP + listHeight();
        if (hover) { RenderUtil.drawRoundedRect(g, left + 8, y + 1, w - 16, ROW - 3, 8, 0x18FFFFFF); }
        int color = p.color() | 0xFF000000;
        double alpha = p.visible() ? 1 : 0.4;
        // colored diamond
        g.pose().pushMatrix(); g.pose().translate((float) (left + 24), (float) (y + ROW / 2.0 - 1)); g.pose().rotate((float) (Math.PI / 4));
        RenderUtil.drawRoundedRect(g, -4, -4, 8, 8, 1.5, Theme.opacity(color, alpha));
        g.pose().popMatrix();
        String name = p.name() + (p.death() ? " (death)" : "") + (p.reached() ? " (reached)" : "");
        RenderUtil.text(g, font, name, left + 38, y + 5, 0.95, Theme.opacity(Theme.TEXT, alpha));
        var player = minecraft.player;
        String where = p.x() + ", " + p.y() + ", " + p.z();
        if (player != null && p.dimension().equals(maps.dimension())) {
            double meters = player.position().distanceTo(new net.minecraft.world.phys.Vec3(p.x() + 0.5, p.y() + 0.5, p.z() + 0.5));
            where += " · " + (meters >= 1000 ? String.format(java.util.Locale.ROOT, "%.1fkm", meters / 1000) : Math.round(meters) + "m");
        }
        RenderUtil.text(g, font, where, left + 38, y + 15, 0.75, Theme.SECONDARY);
        // buttons on the right: show/hide, track, delete
        double bx = left + w - 14, by = y + 4, bh = 19;
        boolean confirming = p.id().equals(confirmDelete) && System.currentTimeMillis() < confirmUntil;
        double deleteWidth = confirming ? 52 : 24; bx -= deleteWidth;
        button(g, bx, by, deleteWidth, bh, confirming ? "Sure?" : "X", confirming, 0xFFE5484D, mx, my, () -> {
            if (confirming) { maps.waypoints().remove(p.id()); maps.saveWaypoints(); confirmDelete = null; }
            else { confirmDelete = p.id(); confirmUntil = System.currentTimeMillis() + 2500; }
        });
        bx -= 4 + 58;
        button(g, bx, by, 58, bh, p.tracked() ? "Tracking" : "Track", p.tracked(), Theme.accent(), mx, my, () -> { maps.waypoints().put(p.tracked(!p.tracked())); maps.saveWaypoints(); });
        bx -= 4 + 52;
        button(g, bx, by, 52, bh, p.visible() ? "Shown" : "Hidden", p.visible(), Theme.accent(), mx, my, () -> { maps.waypoints().put(p.visible(!p.visible())); maps.saveWaypoints(); });
        // clicking the rest of the row edits the waypoint; registered first so the buttons above win
        hits.add(0, new Hit(left + 8, Math.max(y, LIST_TOP), bx - left - 12, ROW - 2, true, () -> minecraft.gui.setScreen(new WaypointEditScreen(this, maps, p))));
    }

    private static String keyName(KeyMapping mapping) {
        return mapping == null ? "-" : mapping.isUnbound() ? "None" : mapping.getTranslatedKeyMessage().getString();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            // rows are only clickable inside the list; footer and tabs are always clickable
            for (int i = hits.size() - 1; i >= 0; i--) {
                var hit = hits.get(i);
                if (hit.contains(event.x(), event.y()) && (!hit.inList() || event.y() >= LIST_TOP && event.y() < LIST_TOP + listHeight())) { hit.action().run(); return true; }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = Math.max(0, scroll - vertical * ROW); return true;
    }

    @Override public void onClose() { maps.saveWaypoints(); minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
