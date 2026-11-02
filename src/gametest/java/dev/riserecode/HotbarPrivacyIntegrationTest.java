package dev.riserecode;

import dev.riserecode.module.modules.HotbarModule;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The privacy blur path runs per frame and item hiding never touches the inventory. */
public final class HotbarPrivacyIntegrationTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static HotbarModule module() { return Client.INSTANCE.getModuleManager().getByClass(HotbarModule.class).orElseThrow(); }

    public static void run(ClientGameTestContext context) {
        context.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady(), 1200);
        context.runOnClient(mc -> { Client.INSTANCE.getModuleManager().disableAll(); mc.options.pauseOnLostFocus = false; });
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @a");
            context.waitTicks(10);
            context.runOnClient(mc -> {
                check(!module().isEnabled(), "Hotbar starts disabled");
                long frames = dev.riserecode.util.BlurUtil.getRenderedFrames();
                module().setEnabled(true);
                check(dev.riserecode.util.BlurUtil.getRenderedFrames() == frames, "no blur without a captured HUD frame");
            });
            context.waitTicks(20);
            context.runOnClient(mc -> {
                check(dev.riserecode.util.BlurUtil.getRenderedFrames() > 0, "hotbar blur composites while enabled");
                // Item privacy stays render-only: give an item, confirm the inventory keeps it.
                mc.player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.DIAMOND_PICKAXE));
            });
            context.waitTicks(10);
            context.runOnClient(mc -> {
                check(!mc.player.getInventory().getItem(0).isEmpty(), "hotbar items are untouched");
                module().blur.setValue(false);
                module().hideHeldItems.setValue(true);
            });
            context.waitTicks(10);
            context.runOnClient(mc -> {
                check(!mc.player.getInventory().getItem(0).isEmpty(), "held item hiding leaves the inventory alone");
                module().setEnabled(false);
            });
            context.waitTicks(10);
            long framesAfterDisable = context.computeOnClient(mc -> dev.riserecode.util.BlurUtil.getRenderedFrames());
            context.waitTicks(5);
            context.runOnClient(mc ->
                    check(dev.riserecode.util.BlurUtil.getRenderedFrames() == framesAfterDisable, "blur stops after the module disables"));
        }
    }
}
