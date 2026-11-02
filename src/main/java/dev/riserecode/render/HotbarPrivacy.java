package dev.riserecode.render;

import dev.riserecode.util.BlurRegion;
import dev.riserecode.util.BlurUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/**
 * Region blur for the vanilla hotbar and its selected item name, captured while
 * the HUD extracts and composited after the GUI pass completes. Ported from
 * donut-client (MIT); see licenses/donut-client-MIT.txt.
 */
public final class HotbarPrivacy {
    private static volatile boolean blurRequested;
    private static volatile int strengthPercent = 90;
    private static volatile boolean hideHeldItems;

    private static BlurRegion hotbarRegion, itemNameRegion;

    private HotbarPrivacy() { }

    /** Pushed by the Hotbar module; the render path reads plain volatile config. */
    public static void configure(boolean blur, int strength, boolean hideItems) {
        blurRequested = blur;
        strengthPercent = Math.clamp(strength, 0, 140);
        hideHeldItems = hideItems;
    }

    public static boolean blurActive() { return blurRequested; }
    public static boolean hideHeldItems() { return hideHeldItems; }

    /** Per-frame reset before the HUD extracts; runs on the render thread. */
    public static void beginFrame() {
        hotbarRegion = null;
        itemNameRegion = null;
    }

    /** Matches vanilla's hotbar, selection border, offhand slot and pickup animation bounds. */
    public static void captureHotbar(GuiGraphicsExtractor graphics) {
        if (!blurRequested) return;
        Minecraft client = Minecraft.getInstance();
        if (!(client.getCameraEntity() instanceof Player player)) return;
        int center = graphics.guiWidth() / 2;
        int x1 = center - 92, x2 = center + 92;
        if (!player.getOffhandItem().isEmpty()) {
            // The offhand slot sits opposite the main arm; include it in the blurred band.
            if (player.getMainArm() == HumanoidArm.RIGHT) x1 = center - 121;
            else x2 = center + 121;
        }
        hotbarRegion = guiRegion(graphics, x1, graphics.guiHeight() - 28, x2 - x1, 28);
    }

    /** Matches vanilla's selected item name, including its two-pixel backdrop and shadow. */
    public static void captureItemName(GuiGraphicsExtractor graphics, int x, int y, int textWidth) {
        if (!blurRequested || hotbarRegion == null) return;
        itemNameRegion = guiRegion(graphics, x - 4, y - 2, textWidth + 8, 13);
    }

    private static BlurRegion guiRegion(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        var window = Minecraft.getInstance().getWindow();
        double pixel = window.getGuiScale(), screenWidth = window.getScreenWidth(), screenHeight = window.getScreenHeight();
        double left = Math.max(0, x * pixel / screenWidth), right = Math.min(1, (x + width) * pixel / screenWidth);
        double top = Math.max(0, 1 - y * pixel / screenHeight), bottom = Math.min(1, 1 - (y + height) * pixel / screenHeight);
        return new BlurRegion(left, bottom, right, top);
    }

    /** Runs after the GUI pass drew the HUD; replaces the captured regions with their blur. */
    public static void composite() {
        BlurRegion region = hotbarRegion;
        if (region == null) return;
        if (itemNameRegion != null) region = region.union(itemNameRegion);
        if (region.empty() || dev.riserecode.performance.PerformancePolicy.frame().blur()) return;
        double radius = 4 + 14 * strengthPercent / 100.0;
        BlurUtil.renderPrivacyRegion(Minecraft.getInstance().gameRenderer.mainRenderTarget(), region, radius);
    }
}
