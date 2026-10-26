package dev.riserecode.ui.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;

/** Regression coverage for glow expansion crossing Minecraft's render-area bounds. */
public final class UiMeshScissorChecks {
    private UiMeshScissorChecks() { }

    public static int run() {
        expect(new ScreenRectangle(0, 0, 32, 32), new ScreenRectangle(6, 6, 17, 17), 9, 320, 180,
                "reported -3 origin");
        expect(new ScreenRectangle(0, 0, 38, 38), new ScreenRectangle(0, 0, 29, 29), 9, 320, 180,
                "reported -9 origin");
        expect(new ScreenRectangle(0, 31, 29, 38), new ScreenRectangle(1, 40, 19, 20), 9, 320, 180,
                "left edge");
        expect(new ScreenRectangle(31, 0, 38, 29), new ScreenRectangle(40, 1, 20, 19), 9, 320, 180,
                "top edge");
        expect(new ScreenRectangle(291, 31, 29, 38), new ScreenRectangle(300, 40, 19, 20), 9, 320, 180,
                "right edge");
        expect(new ScreenRectangle(31, 151, 38, 29), new ScreenRectangle(40, 160, 20, 19), 9, 320, 180,
                "bottom edge");
        expect(new ScreenRectangle(291, 151, 29, 29), new ScreenRectangle(300, 160, 19, 19), 9, 320, 180,
                "bottom-right corner");
        expect(new ScreenRectangle(31, 31, 38, 38), new ScreenRectangle(40, 40, 20, 20), 9, 320, 180,
                "interior retains expanded glow tail");
        expect(new ScreenRectangle(0, 0, 320, 180), new ScreenRectangle(0, 0, 320, 180), 80, 320, 180,
                "full-screen clip with large glow");
        expect(new ScreenRectangle(301, 161, 20, 20), new ScreenRectangle(310, 170, 10, 10), 9, 321, 181,
                "rounded-up GUI dimensions");
        expect(null, new ScreenRectangle(400, 250, 20, 20), 9, 320, 180,
                "fully off-screen expansion is discarded");
        expect(null, new ScreenRectangle(0, 0, 20, 20), 9, 0, 0,
                "empty render area is discarded");
        return 12;
    }

    private static void expect(ScreenRectangle expected, ScreenRectangle source, int pad, int width, int height,
            String scenario) {
        ScreenRectangle actual = UiMesh.expandGlowScissor(source, pad, width, height);
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
        }
    }
}
