package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Worn armor (and optionally held items) with durability. Position and scale come from the HUD editor. */
public final class ArmorHudModule extends HudModule {
    public enum Layout { VERTICAL, HORIZONTAL }
    public enum Durability { PERCENT, VALUE, FRACTION, OFF }
    /** GREY_OUT dims the lost part of the item from the top; TINT also colors the remaining part. */
    public enum ItemFill { OFF, GREY_OUT, TINT }

    private static final double PAD = 3, TEXT_GAP = 4;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final EnumSetting<Layout> layout = register(new EnumSetting<>("Layout", "Slots below each other or next to each other", Layout.VERTICAL));
    private final EnumSetting<Durability> durability = register(new EnumSetting<>("Durability", "Percent, remaining uses, uses/max or nothing", Durability.PERCENT));
    private final BooleanSetting durabilityBar = register(new BooleanSetting("DurabilityBar", "Durability bar on the item", false));
    private final EnumSetting<ItemFill> itemFill = register(new EnumSetting<>("ItemFill", "The item itself becomes the durability bar", ItemFill.OFF));
    private final BooleanSetting colorByDurability = register(new BooleanSetting("ColorByDurability", "Green to red with remaining durability; off = Color", true));
    private final ColorSetting plainColor = register(new ColorSetting("Color", "Text and tint color when not colored by durability", Theme.TEXT));
    private final BooleanSetting showHands = register(new BooleanSetting("ShowHands", "Also show the items in your hands", false));
    private final DoubleSetting iconSize = register(new DoubleSetting("IconSize", "Item size", 16, 8, 32));
    private final BooleanSetting showBackground = register(new BooleanSetting("ShowBackground", "Panel and border behind the items", true));

    private static ItemStack[] samples;

    public ArmorHudModule(EventBus bus, Minecraft mc) { super("ArmorHud", "Worn armor and held items with durability.", bus, mc); }

    /** One row/column: the item, its text and the text color (green to red with remaining durability). */
    private record Slot(ItemStack stack, String text, int color, double textWidth, double fraction) { }
    private record Geometry(List<Slot> slots, double width, double height, double column) { }

    public boolean showBackground() { return showBackground.getValue(); }

    @Override protected Object contentKey() { return 0; }
    @Override protected List<HudContent.Line> buildContent() { return List.of(); }

    @Override public double graphicWidth() { return geometry(false).width(); }
    @Override public double graphicHeight() { return geometry(false).height(); }

    private static ItemStack[] samples() {
        if (samples == null) {
            ItemStack[] s = {new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS)};
            double[] worn = {0.1, 0.45, 0.8, 0.95};
            for (int i = 0; i < s.length; i++) { s[i].setDamageValue((int) (s[i].getMaxDamage() * worn[i])); }
            samples = s;
        }
        return samples;
    }

    private List<Slot> slots(boolean preview) {
        var player = minecraft == null ? null : minecraft.player;
        List<ItemStack> stacks = new ArrayList<>();
        boolean armorEmpty = true;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
            armorEmpty &= stack.isEmpty();
            stacks.add(stack);
        }
        if (preview && armorEmpty) {
            // HUD editor with nothing worn: show example armor so the HUD can be placed
            stacks.clear();
            stacks.addAll(List.of(samples()));
        }
        if (showHands.getValue() && player != null) {
            stacks.add(player.getItemBySlot(EquipmentSlot.MAINHAND));
            stacks.add(player.getItemBySlot(EquipmentSlot.OFFHAND));
        }
        List<Slot> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() || result.size() < ARMOR.length) { result.add(describe(stack)); }
        }
        return result;
    }

    private Slot describe(ItemStack stack) {
        String label = "";
        int color = plainColor.paint().primary();
        double fraction = -1;
        if (stack.isDamageableItem()) {
            int max = Math.max(1, stack.getMaxDamage()), remaining = Math.max(0, max - stack.getDamageValue());
            fraction = (double) remaining / max;
            label = switch (durability.getValue()) {
                case PERCENT -> Math.round(fraction * 100) + "%";
                case VALUE -> Integer.toString(remaining);
                case FRACTION -> remaining + "/" + max;
                case OFF -> "";
            };
            if (colorByDurability.getValue()) { color = 0xFF000000 | java.awt.Color.HSBtoRGB((float) (0.33 * fraction), 0.75f, 1f); }
        } else if (stack.getCount() > 1) {
            label = "×" + stack.getCount();
        }
        return new Slot(stack, label, color, label.isEmpty() ? 0 : textWidth(label), fraction);
    }

    private double textWidth(String label) {
        if (minecraft == null || minecraft.font == null) { return label.length() * 6; }
        return RenderUtil.textWidth(minecraft.font, label, false);
    }

    private double fontHeight() { return 9 * Theme.number("Font Scale", 1); }

    private Geometry geometry(boolean preview) {
        List<Slot> slots = slots(preview);
        int n = Math.max(1, slots.size());
        double cell = iconSize.getValue() + 4, widest = 0;
        for (Slot slot : slots) { widest = Math.max(widest, slot.textWidth()); }
        if (layout.getValue() == Layout.VERTICAL) {
            return new Geometry(slots, PAD * 2 + cell + (widest > 0 ? widest + TEXT_GAP : 0), PAD * 2 + n * cell, cell);
        }
        double column = Math.max(cell, widest + 2);
        return new Geometry(slots, PAD * 2 + n * column, PAD * 2 + cell + (widest > 0 ? fontHeight() + 2 : 0), column);
    }

    @Override public void drawGraphic(GuiGraphicsExtractor g, double alpha, boolean edit) {
        Geometry geo = geometry(edit);
        double icon = iconSize.getValue(), scale = icon / 16, cell = icon + 4;
        boolean vertical = layout.getValue() == Layout.VERTICAL;
        for (int i = 0; i < geo.slots().size(); i++) {
            Slot slot = geo.slots().get(i);
            double itemX, itemY, textX, textY;
            if (vertical) {
                double top = PAD + i * cell;
                itemX = PAD + 2;
                itemY = top + 2;
                textX = PAD + cell + TEXT_GAP;
                textY = top + (cell - fontHeight()) / 2;
            } else {
                double left = PAD + i * geo.column();
                itemX = left + (geo.column() - cell) / 2 + 2;
                itemY = PAD + 2;
                textX = left + (geo.column() - slot.textWidth()) / 2;
                textY = PAD + cell + 2;
            }
            if (slot.stack().isEmpty()) {
                RenderUtil.drawRoundedRect(g, itemX, itemY, icon, icon, 3, Theme.opacity(0x30FFFFFF, alpha));
                continue;
            }
            g.pose().pushMatrix();
            g.pose().translate((float) itemX, (float) itemY);
            g.pose().scale((float) scale, (float) scale);
            g.item(slot.stack(), 0, 0);
            if (durabilityBar.getValue()) { g.itemDecorations(minecraft.font, slot.stack(), 0, 0); }
            g.pose().popMatrix();
            if (itemFill.getValue() != ItemFill.OFF && slot.fraction() >= 0 && slot.fraction() < 1) {
                double lost = icon * (1 - slot.fraction());
                RenderUtil.drawRoundedRect(g, itemX, itemY, icon, lost, 1, Theme.opacity(0xB0262A30, alpha));
                if (itemFill.getValue() == ItemFill.TINT) {
                    RenderUtil.drawRoundedRect(g, itemX, itemY + lost, icon, icon - lost, 1, Theme.opacity((slot.color() & 0xFFFFFF) | 0x50000000, alpha));
                }
            }
            if (!slot.text().isEmpty()) {
                RenderUtil.hudText(g, minecraft.font, slot.text(), textX, textY, Theme.opacity(slot.color(), alpha));
            }
        }
    }
}
