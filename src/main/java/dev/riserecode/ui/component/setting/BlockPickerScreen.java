package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;

/** Registry search with block icons, immediate selection and explicit preservation of unavailable IDs. */
public final class BlockPickerScreen extends Screen {
    private static final int ROW = 24, TOP = 64;
    private static final int[] PALETTE = {0xFF55FFFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFAA00, 0xFFFFFF55, 0xFFFF55FF, 0xFF5555FF, 0xFFFFFFFF, 0xFFAAAAAA};
    private final Screen parent;
    private final BlockListSetting setting;
    private final Runnable changed;
    private final List<String> registry;
    private final java.util.Map<String, String> names = new java.util.HashMap<>();
    private final java.util.Map<String, ItemStack> icons = new java.util.HashMap<>();
    private List<String> filtered;
    private String query = "";
    private int scroll;
    private boolean onlySelected;
    public BlockPickerScreen(Screen parent, BlockListSetting setting, Runnable changed) {
        super(Component.literal(setting.kind() == BlockListSetting.Kind.ENTITY ? "Mobs" : "Blocks")); this.parent = parent; this.setting = setting; this.changed = changed;
        var ids = new java.util.TreeSet<String>();
        var candidates = setting.candidates();
        if (candidates != null) { ids.addAll(candidates); } else if (setting.kind() == BlockListSetting.Kind.ENTITY) { entityIds().forEach(ids::add); } else { BuiltInRegistries.BLOCK.keySet().forEach(id -> ids.add(id.toString())); }
        ids.addAll(setting.getValue()); registry = List.copyOf(ids);
        for (String id : registry) {
            if (setting.kind() == BlockListSetting.Kind.ENTITY) {
                var type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
                names.put(id, type == null ? id : type.getDescription().getString());
                icons.put(id, type == null ? ItemStack.EMPTY : entityIcon(type));
                continue;
            }
            var block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null);
            names.put(id, block == null ? id : block.getName().getString());
            icons.put(id, block == null || block.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem()));
        }
        filtered = registry;
    }
    /** Everything that can be targeted: players and all summonable non-technical entity types. */
    public static List<String> entityIds() {
        var ids = new java.util.ArrayList<String>();
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            if (type == EntityTypes.PLAYER || type.canSummon() && type.getCategory() != net.minecraft.world.entity.MobCategory.MISC || type.canSummon() && SpawnEggItem.byId(type).isPresent()) {
                ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            }
        });
        return ids;
    }
    /** Spawn egg as the mob's icon; a few well-known entities without an egg get a matching item. */
    private static ItemStack entityIcon(EntityType<?> type) {
        if (type == EntityTypes.PLAYER) { return new ItemStack(Items.PLAYER_HEAD); }
        var egg = SpawnEggItem.byId(type);
        if (egg.isPresent()) { return new ItemStack(egg.get().value()); }
        if (type == EntityTypes.ARMOR_STAND) { return new ItemStack(Items.ARMOR_STAND); }
        if (type == EntityTypes.IRON_GOLEM) { return new ItemStack(Items.IRON_BLOCK); }
        if (type == EntityTypes.SNOW_GOLEM) { return new ItemStack(Items.CARVED_PUMPKIN); }
        if (type == EntityTypes.WITHER) { return new ItemStack(Items.WITHER_SKELETON_SKULL); }
        if (type == EntityTypes.ENDER_DRAGON) { return new ItemStack(Items.DRAGON_HEAD); }
        return ItemStack.EMPTY;
    }
    private void filter() {
        String q = query.toLowerCase(Locale.ROOT);
        filtered = registry.stream().filter(id -> (id.contains(q) || names.get(id).toLowerCase(Locale.ROOT).contains(q)) && (!onlySelected || setting.getValue().contains(id))).toList();
        scroll = 0;
    }
    private int left() { return width / 2 - Math.min(180, width / 2 - 12); }
    private int paneWidth() { return width - left() * 2; }
    private int rows() { return Math.max(0, (height - TOP - 40) / ROW); }
    private int rowAt(double x, double y) {
        if (x < left() + 6 || x >= left() + paneWidth() - 6 || y < TOP || y >= TOP + rows() * ROW) { return -1; }
        int index = (int) ((y - TOP) / ROW) + scroll;
        return index < filtered.size() ? index : -1;
    }
    private boolean allSelected() { return !filtered.isEmpty() && setting.getValue().containsAll(filtered); }
    private boolean overAllButton(double x, double y) { return x >= left() + paneWidth() - 202 && x < left() + paneWidth() - 144 && y >= 38 && y < 56; }
    /** Marks every entry currently listed (respecting search and filter), or unmarks them if they all are already marked. */
    private void toggleAll() {
        var values = new java.util.ArrayList<>(setting.getValue());
        if (allSelected()) { values.removeAll(filtered); } else { filtered.forEach(id -> { if (!values.contains(id)) { values.add(id); } }); }
        setting.setValue(values); changed.run();
        if (onlySelected) { filter(); }
    }
    private boolean overFilterButton(double x, double y) { return x >= left() + paneWidth() - 138 && x < left() + paneWidth() - 12 && y >= 38 && y < 56; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xB30F1115);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = left(), w = paneWidth(); var colors = setting.colorMap();
        RenderUtil.drawSurfaceTransition(graphics, left, 12, w, height - 24, 12, 0, 1);
        RenderUtil.text(graphics, font, setting.kind() == BlockListSetting.Kind.ENTITY ? "Select mobs" : "Select blocks", left + 12, 24, 1.2, Theme.TEXT);
        RenderUtil.text(graphics, font, "Search: " + query + "_", left + 12, 44, 1, Theme.SECONDARY);
        int ax = left + w - 202;
        RenderUtil.drawRoundedRect(graphics, ax, 38, 58, 18, 6, overAllButton(mouseX, mouseY) ? 0x40FFFFFF : 0x30FFFFFF);
        RenderUtil.text(graphics, font, allSelected() ? "None" : "All", ax + 10, 43, 0.85, Theme.TEXT);
        int bx = left + w - 138;
        RenderUtil.drawRoundedRect(graphics, bx, 38, 126, 18, 6, onlySelected ? Theme.opacity(Theme.accent(), 0.55) : overFilterButton(mouseX, mouseY) ? 0x40FFFFFF : 0x30FFFFFF);
        RenderUtil.text(graphics, font, onlySelected ? "Selected only ✓" : "Selected only", bx + 8, 43, 0.85, Theme.TEXT);
        RenderUtil.text(graphics, font, setting.getValue().size() + " selected" + (setting.hasColors() ? " · Right-click = color" : "") + " · Escape to return", left + 12, height - 28, 0.85, Theme.SECONDARY);
        RenderUtil.scissorPush(graphics, left + 6, TOP, w - 12, rows() * ROW);
        try {
            int hovered = rowAt(mouseX, mouseY);
            for (int i = 0; i < rows() && i + scroll < filtered.size(); i++) {
                String id = filtered.get(i + scroll); boolean selected = setting.getValue().contains(id); int y = TOP + i * ROW;
                if (selected) { RenderUtil.drawRoundedRect(graphics, left + 8, y + 1, w - 16, ROW - 2, 6, Theme.opacity(Theme.accent(), 0.18)); }
                if (i + scroll == hovered) { RenderUtil.drawRoundedRect(graphics, left + 8, y + 1, w - 16, ROW - 2, 6, 0x18FFFFFF); }
                var icon = icons.get(id);
                RenderUtil.drawRoundedRect(graphics, left + 12, y + 3, 18, 18, 4, 0x20FFFFFF);
                if (!icon.isEmpty()) {
                    graphics.pose().pushMatrix(); graphics.pose().translate(left + 13, y + 4);
                    graphics.item(icon, 0, 0); graphics.pose().popMatrix();
                }
                RenderUtil.text(graphics, font, names.get(id), left + 38, y + 4, 0.95, selected ? Theme.accent() : Theme.TEXT);
                RenderUtil.text(graphics, font, id, left + 38, y + 14, 0.7, Theme.SECONDARY);
                Integer custom = selected ? colors.get(id) : null;
                if (custom != null) { RenderUtil.drawRoundedRect(graphics, left + w - 34, y + 6, 16, 12, 4, custom); }
                else if (selected) { RenderUtil.text(graphics, font, "✓", left + w - 30, y + 8, 1, Theme.accent()); }
            }
        } finally { RenderUtil.scissorPop(graphics); }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && overAllButton(event.x(), event.y())) { toggleAll(); return true; }
        if (event.button() == 0 && overFilterButton(event.x(), event.y())) { onlySelected = !onlySelected; filter(); return true; }
        int index = rowAt(event.x(), event.y());
        if (index >= 0 && event.button() == 1 && setting.hasColors()) {
            String id = filtered.get(index);
            if (setting.getValue().contains(id)) {
                Integer current = setting.colorMap().get(id); int next = current == null ? 0 : -1;
                for (int i = 0; current != null && i < PALETTE.length; i++) { if (PALETTE[i] == current) { next = i + 1; break; } }
                setting.setColor(id, next >= 0 && next < PALETTE.length ? PALETTE[next] : null); changed.run();
            }
            return true;
        }
        if (index >= 0 && event.button() == 0) {
            setting.toggle(filtered.get(index)); changed.run();
            if (onlySelected) { int keep = scroll; filter(); scroll = Math.min(keep, Math.max(0, filtered.size() - 1)); }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = (int) Math.clamp(scroll - Math.signum(vertical) * 3, 0, Math.max(0, filtered.size() - Math.max(1, rows()))); return true;
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if (query.length() < 128 && !Character.isISOControl(event.codepoint())) { query += new String(Character.toChars(event.codepoint())); filter(); } return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 259 && !query.isEmpty()) { query = query.substring(0, query.offsetByCodePoints(query.length(), -1)); filter(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
