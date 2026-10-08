package dev.riserecode.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Hotbar-only selection; no inventory transactions, synthetic clicks, or hidden slot packets. */
public final class ItemUtil {
    public enum WeaponKind { NONE, SWORD, AXE, TOOL, OTHER }
    private ItemUtil() { }
    public static WeaponKind weaponKind(ItemStack stack) {
        if (stack.isEmpty()) { return WeaponKind.NONE; }
        if (stack.is(ItemTags.SWORDS)) { return WeaponKind.SWORD; }
        if (stack.is(ItemTags.AXES)) { return WeaponKind.AXE; }
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) { return WeaponKind.TOOL; }
        boolean[] damage = {false};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.amount() > 0) { damage[0] = true; }
        });
        return stack.has(DataComponents.WEAPON) || damage[0] ? WeaponKind.OTHER : WeaponKind.NONE;
    }
    public static boolean isWeapon(ItemStack stack) { return weaponKind(stack) != WeaponKind.NONE; }
    public static boolean holdingWeapon(Player player) { return isWeapon(player.getMainHandItem()); }
    public static double attackDamage(Player player, ItemStack stack) { return attributeWithItem(player, stack, Attributes.ATTACK_DAMAGE); }
    public static double attackSpeed(Player player, ItemStack stack) { return attributeWithItem(player, stack, Attributes.ATTACK_SPEED); }

    /** Replace current main-hand modifiers in a copy, preserving effects and other equipped items. */
    private static double attributeWithItem(Player player, ItemStack stack, Holder<Attribute> attribute) {
        var instance = player.getAttribute(attribute);
        if (instance == null) { return 0; }
        Map<Identifier, AttributeModifier> modifiers = new LinkedHashMap<>();
        for (AttributeModifier modifier : instance.getModifiers()) { modifiers.put(modifier.id(), modifier); }
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
            if (holder.equals(attribute)) { modifiers.remove(modifier.id()); }
        });
        stack.forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
            if (holder.equals(attribute)) { modifiers.put(modifier.id(), modifier); }
        });
        return attribute.value().sanitizeValue(evaluateAttribute(instance.getBaseValue(), modifiers.values()));
    }
    /** Vanilla operation order: additions, multiplied base, then multiplied total. */
    public static double evaluateAttribute(double base, Collection<AttributeModifier> modifiers) {
        if (!Double.isFinite(base)) { throw new IllegalArgumentException("Base attribute must be finite"); }
        double additions = 0; double multiplier = 0; double total = 1;
        for (AttributeModifier modifier : modifiers) {
            if (!Double.isFinite(modifier.amount())) { throw new IllegalArgumentException("Attribute modifiers must be finite"); }
            switch (modifier.operation()) {
                case ADD_VALUE -> additions += modifier.amount();
                case ADD_MULTIPLIED_BASE -> multiplier += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> total *= 1 + modifier.amount();
            }
        }
        return (base + additions) * (1 + multiplier) * total;
    }
    /** Rank single-hit damage first, speed second, and retain the current slot on an exact tie. */
    public static OptionalInt bestHotbarWeapon(Player player) {
        int selected = player.getInventory().getSelectedSlot();
        int best = -1; double bestDamage = Double.NEGATIVE_INFINITY; double bestSpeed = Double.NEGATIVE_INFINITY;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isWeapon(stack)) { continue; }
            double damage = attackDamage(player, stack); double speed = attackSpeed(player, stack);
            if (best < 0 || damage > bestDamage || (Double.compare(damage, bestDamage) == 0
                    && (speed > bestSpeed || (Double.compare(speed, bestSpeed) == 0 && slot == selected)))) {
                best = slot; bestDamage = damage; bestSpeed = speed;
            }
        }
        return best < 0 ? OptionalInt.empty() : OptionalInt.of(best);
    }

    /** A reversible selection session that yields to manual hotbar changes. */
    public static final class HotbarSelection implements AutoCloseable {
        private final Player player;
        private int originalSlot = -1;
        private int managedSlot = -1;
        private boolean manualOverride;
        private boolean closed;
        public HotbarSelection(Player player) { this.player = Objects.requireNonNull(player); }
        public Player getPlayer() { return player; }
        /** Returns true only when this call changes the selected slot. */
        public boolean selectBestWeapon() {
            if (closed) { throw new IllegalStateException("Hotbar selection is closed"); }
            int current = player.getInventory().getSelectedSlot();
            if (managedSlot >= 0 && current != managedSlot) { manualOverride = true; }
            if (manualOverride) { return false; }
            OptionalInt best = bestHotbarWeapon(player);
            if (best.isEmpty() || best.getAsInt() == current) { return false; }
            if (originalSlot < 0) { originalSlot = current; }
            managedSlot = best.getAsInt();
            player.getInventory().setSelectedSlot(managedSlot);
            // Do not spend the old weapon's charged attack on the newly selected weapon.
            player.resetAttackStrengthTicker();
            return true;
        }
        /** Player replacement/disconnection discards ownership without touching the old inventory. */
        public void abandon() { closed = true; }
        @Override
        public void close() {
            if (closed) { return; }
            closed = true;
            if (!manualOverride && originalSlot >= 0 && player.getInventory().getSelectedSlot() == managedSlot) {
                player.getInventory().setSelectedSlot(originalSlot); player.resetAttackStrengthTicker();
            }
        }
    }
}
