package dev.riserecode.module.modules;

import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.BlockState;

/** Owns at most one inventory exchange; never restores over an unexpected slot. */
public final class AutoToolModule extends Module {
    public final BooleanSetting inventoryTools = register(new BooleanSetting("Inventory Tools", "Use synchronized swaps for inventory tools", false));
    public final BooleanSetting restore = register(new BooleanSetting("Restore Slot", "Return to the previous slot after mining", true));
    public final BooleanSetting protect = register(new BooleanSetting("Protect Durability", "Avoid tools near breaking", true));
    public final DoubleSetting remaining = register(DoubleSetting.extended("Minimum Durability", "Minimum durability remaining", 5, 1, 100));
    private final Minecraft mc;
    private int previous = -1, selected = -1, source = -1;
    private ItemStack expectedSource = ItemStack.EMPTY, expectedTool = ItemStack.EMPTY;
    private net.minecraft.client.player.LocalPlayer owner;
    private boolean interrupted;
    private long lastMining;
    private volatile boolean serverInventoryUpdate;
    public AutoToolModule(EventBus bus, Minecraft mc) { super("AutoTool", "Select the fastest suitable mining tool.", Category.PLAYER, bus); this.mc = mc; }
    @Override protected void onEnable() {
        listen(MiningEvent.class, this::mine);
        listen(UpdateEvent.class, e -> {
            if (!mc.options.keyAttack.isDown()) { interrupted = false; }
            if (owner == null) { return; }
            if (!valid()) { abandon(); interrupted = true; return; }
            if (!valid() || !mc.options.keyAttack.isDown() || mc.gui.screen() != null || mc.level == null || mc.level.getGameTime() - lastMining > 3) { finish(); }
        });
        listen(PacketEvent.Receive.class, e -> {
            if (e.getPacket() instanceof net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
                    || e.getPacket() instanceof net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket) { serverInventoryUpdate = true; }
        });
    }
    private void mine(MiningEvent e) {
        if (e.phase() == MiningEvent.Phase.STOP) { finish(); interrupted = false; return; }
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null || mc.player.containerMenu != mc.player.inventoryMenu || mc.player.isCreative()) { return; }
        lastMining = mc.level.getGameTime();
        if (owner != null && !valid()) { abandon(); interrupted = true; }
        if (interrupted) { if (!mc.options.keyAttack.isDown()) { interrupted = false; } return; }
        BlockState state = mc.level.getBlockState(e.position());
        if (owner != null && lowDurability(owner.getInventory().getItem(selected))) { finish(); }
        if (owner != null) { return; }
        var inv = mc.player.getInventory(); int current = inv.getSelectedSlot(), best = current; boolean unsafeCurrent = lowDurability(inv.getItem(current)); double score = unsafeCurrent ? -Double.MAX_VALUE : score(inv.getItem(current), state);
        for (int slot = 0; slot < (inventoryTools.getValue() ? 36 : 9); slot++) {
            ItemStack stack = inv.getItem(slot); if (stack.isEmpty() && (!unsafeCurrent || slot >= 9) || lowDurability(stack)) { continue; }
            double candidate = score(stack, state); if (candidate > score + 0.0001) { best = slot; score = candidate; }
        }
        if (best == current) { return; }
        owner = mc.player; previous = current;
        if (best < 9) { selected = best; inv.setSelectedSlot(best); expectedTool = inv.getItem(best).copy(); }
        else {
            source = best; selected = current; expectedSource = inv.getItem(current).copy(); expectedTool = inv.getItem(best).copy();
            mc.gameMode.handleContainerInput(owner.inventoryMenu.containerId, best, current, ContainerInput.SWAP, owner);
            if (!valid()) { abandon(); interrupted = true; }
        }
    }
    private boolean lowDurability(ItemStack stack) { return protect.getValue() && stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= remaining.getValue(); }
    /** Native tool component speed and correct-drop rules, including efficiency. */
    public static double score(ItemStack stack, BlockState state) {
        if (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) { return -1; }
        double speed = stack.getDestroySpeed(state);
        int efficiency = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            net.minecraft.client.Minecraft.getInstance().level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), stack);
        if (speed > 1 && efficiency > 0) { speed += efficiency * efficiency + 1; }
        return speed / (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state) ? 100.0 : 30.0);
    }
    private boolean valid() {
        if (mc.player != owner || mc.level == null || mc.gameMode == null || owner.containerMenu != owner.inventoryMenu || owner.getInventory().getSelectedSlot() != selected) { return false; }
        var inv = owner.getInventory(); ItemStack tool = inv.getItem(selected);
        // Mining may change durability; all other components and counts must still match.
        ItemStack normalized = tool.copy(); if (normalized.isDamageableItem()) { normalized.setDamageValue(expectedTool.getDamageValue()); }
        boolean matches = ItemStack.matches(normalized, expectedTool) && (source < 0 || ItemStack.matches(inv.getItem(source), expectedSource));
        if (serverInventoryUpdate) { serverInventoryUpdate = false; if (!matches) { return false; } }
        return matches;
    }
    private void finish() {
        if (owner != null && valid() && mc.gui.screen() == null) {
            if (source >= 0) { mc.gameMode.handleContainerInput(owner.inventoryMenu.containerId, source, selected, ContainerInput.SWAP, owner); }
            if (restore.getValue()) { owner.getInventory().setSelectedSlot(previous); }
        }
        abandon();
    }
    private void abandon() { owner = null; previous = selected = source = -1; expectedSource = expectedTool = ItemStack.EMPTY; serverInventoryUpdate = false; }
    @Override protected void onDisable() { finish(); interrupted = false; }
}
