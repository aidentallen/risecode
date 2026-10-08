# Phase 4 — Rise-inspired UI/UX

Implemented in `/home/toast/Work/riserecode` for Minecraft 26.2, Fabric API 0.161.0+26.2, Java 25. The original foundations, settings, event hooks, managers, and keyboard mixin are unchanged. This document includes complete new and updated source files, in the requested delivery order.

## 1. Updated project file tree

Generated build/tool caches and runtime output are excluded.

```text
riserecode/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── scripts/
│   └── verify.sh
├── src/
│   ├── gametest/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           └── ClientIntegrationTest.java
│   │   └── resources/
│   │       └── fabric.mod.json
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/
│   │   │       └── riserecode/
│   │   │           ├── config/
│   │   │           │   └── ConfigManager.java
│   │   │           ├── event/
│   │   │           │   ├── Event.java
│   │   │           │   ├── EventBus.java
│   │   │           │   ├── KeyPressEvent.java
│   │   │           │   ├── Render2DEvent.java
│   │   │           │   ├── Render3DEvent.java
│   │   │           │   └── UpdateEvent.java
│   │   │           ├── keybind/
│   │   │           │   └── KeybindManager.java
│   │   │           ├── mixin/
│   │   │           │   └── KeyboardHandlerMixin.java
│   │   │           ├── module/
│   │   │           │   ├── modules/
│   │   │           │   │   └── FullBrightModule.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Module.java
│   │   │           │   └── ModuleManager.java
│   │   │           ├── setting/
│   │   │           │   ├── BooleanSetting.java
│   │   │           │   ├── ColorSetting.java
│   │   │           │   ├── DoubleSetting.java
│   │   │           │   ├── EnumSetting.java
│   │   │           │   └── Setting.java
│   │   │           ├── ui/
│   │   │           │   ├── animation/
│   │   │           │   │   └── Animation.java
│   │   │           │   ├── clickgui/
│   │   │           │   │   └── ClickGui.java
│   │   │           │   ├── component/
│   │   │           │   │   ├── setting/
│   │   │           │   │   │   ├── ColorSwatch.java
│   │   │           │   │   │   ├── EnumSelector.java
│   │   │           │   │   │   ├── Slider.java
│   │   │           │   │   │   └── ToggleSwitch.java
│   │   │           │   │   ├── ActionButton.java
│   │   │           │   │   ├── ModuleButton.java
│   │   │           │   │   ├── Panel.java
│   │   │           │   │   └── UIComponent.java
│   │   │           │   ├── hud/
│   │   │           │   │   └── HudRenderer.java
│   │   │           │   └── theme/
│   │   │           │       └── Theme.java
│   │   │           ├── util/
│   │   │           │   ├── MathUtil.java
│   │   │           │   └── RenderUtil.java
│   │   │           ├── Client.java
│   │   │           └── RiseRecodeClient.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── riserecode/
│   │       │       ├── font/
│   │       │       │   ├── inter_medium.json
│   │       │       │   ├── inter_medium.ttf
│   │       │       │   ├── inter_regular.json
│   │       │       │   └── inter_regular.ttf
│   │       │       └── lang/
│   │       │           └── en_us.json
│   │       ├── licenses/
│   │       │   └── inter-OFL.txt
│   │       ├── fabric.mod.json
│   │       └── riserecode.mixins.json
│   └── test/
│       └── java/
│           └── dev/
│               └── riserecode/
│                   ├── FoundationChecks.java
│                   ├── RuntimeChecks.java
│                   └── UiChecks.java
├── .gitignore
├── PHASE3.md
├── PHASE4.md
├── README.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle
```

## 2. Code files

### `src/main/java/dev/riserecode/ui/theme/Theme.java`

```java
package dev.riserecode.ui.theme;

import dev.riserecode.util.MathUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Packed ARGB design tokens. UI colors and font choices never enter modules. */
public final class Theme {
    public static final int BACKGROUND = 0xFF0F1115;
    public static final int PANEL = 0xBF181A20;
    public static final int SURFACE = 0xE022252E;
    public static final int HOVER = 0xEC2D3240;
    public static final int TEXT = 0xFFF4F6FC;
    public static final int SECONDARY = 0xFF989FAF;
    public static final int MUTED = 0xFF616A7D;
    public static final int BLUE = 0xFF3B82F6;
    public static final int FUCHSIA = 0xFFD946EF;
    public static final int BORDER = 0x14FFFFFF;
    public static final int SHADOW = 0x70000000;
    public static final int PANEL_RADIUS = 12;
    public static final int BUTTON_RADIUS = 8;
    public static final int PADDING = 16;
    private static final Style BODY = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "inter_regular")));
    private static final Style TITLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "inter_medium")));

    private Theme() { }

    public static Component text(String text, boolean title) {
        return Component.literal(text).withStyle(title ? TITLE : BODY);
    }

    public static int lerp(int from, int to, double progress) {
        double t = MathUtil.clamp01(progress);
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int channel = (int) Math.round(MathUtil.lerp((from >>> shift) & 255, (to >>> shift) & 255, t));
            result |= channel << shift;
        }
        return result;
    }

    /** Multiplies existing alpha; translucent panel tokens stay translucent. */
    public static int opacity(int color, double opacity) {
        int alpha = (int) Math.round((color >>> 24) * MathUtil.clamp01(opacity));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
```

### `src/main/java/dev/riserecode/util/RenderUtil.java`

```java
package dev.riserecode.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.riserecode.ui.theme.Theme;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Modern GUI extraction using cached, antialiased nine-slice distance masks. */
public final class RenderUtil {
    private static final int DENSITY = 4;
    private static final int SHADOW_PADDING = 16;
    private static final Map<MaskKey, Mask> MASKS = new HashMap<>();
    private enum Kind { FILL, BORDER, SHADOW }
    private record MaskKey(int radius, Kind kind) { }
    private record Mask(Identifier texture, int side, int edge) { }

    private RenderUtil() { }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawMask(graphics, x, y, width, height, radius, color, Kind.FILL);
    }

    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawMask(graphics, x, y, width, height, radius, color, Kind.BORDER);
    }

    public static void drawDropShadow(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawMask(graphics, x, y + 3, width, height, radius, color, Kind.SHADOW);
    }

    public static void drawGradientRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, int top, int bottom) {
        graphics.fillGradient((int) x, (int) y, (int) Math.ceil(x + width),
                (int) Math.ceil(y + height), top, bottom);
    }

    public static void drawStringWithShadow(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color) {
        drawText(graphics, font, text, x, y, color, false, true);
    }

    public static void drawText(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color, boolean title, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.text(font, Theme.text(text, title), 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    public static int textWidth(Font font, String text, boolean title) {
        return font.width(Theme.text(text, title));
    }

    public static String ellipsize(Font font, String text, double width) {
        if (textWidth(font, text, false) <= width) { return text; }
        String shortened = text;
        while (!shortened.isEmpty() && textWidth(font, shortened + "…", false) > width) {
            int end = shortened.offsetByCodePoints(shortened.length(), -1);
            shortened = shortened.substring(0, end);
        }
        return shortened + "…";
    }

    /** Coordinates are GUI units; Minecraft captures the current pose transform. */
    public static void scissorPush(GuiGraphicsExtractor graphics, double x, double y, double width, double height) {
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }

    public static void scissorPop(GuiGraphicsExtractor graphics) { graphics.disableScissor(); }

    private static void drawMask(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, int color, Kind kind) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) { return; }
        int r = Math.max(1, Math.min(24, (int) Math.floor(Math.min(radius, Math.min(width, height) / 2))));
        Mask mask = MASKS.computeIfAbsent(new MaskKey(r, kind), RenderUtil::createMask);
        int padding = kind == Kind.SHADOW ? SHADOW_PADDING : 0;
        int w = Math.max(2 * mask.edge, (int) Math.round(width) + padding * 2);
        int h = Math.max(2 * mask.edge, (int) Math.round(height) + padding * 2);
        int edge = mask.edge;
        int[] destinationX = {0, edge, w - edge, w};
        int[] destinationY = {0, edge, h - edge, h};
        int[] source = {0, edge * DENSITY, (mask.side - edge) * DENSITY, mask.side * DENSITY};
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x - padding), (float) (y - padding));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int sliceWidth = destinationX[column + 1] - destinationX[column];
                int sliceHeight = destinationY[row + 1] - destinationY[row];
                if (sliceWidth == 0 || sliceHeight == 0) { continue; }
                graphics.blit(RenderPipelines.GUI_TEXTURED, mask.texture,
                        destinationX[column], destinationY[row], source[column], source[row], sliceWidth, sliceHeight,
                        source[column + 1] - source[column], source[row + 1] - source[row],
                        mask.side * DENSITY, mask.side * DENSITY, color);
            }
        }
        graphics.pose().popMatrix();
    }

    private static Mask createMask(MaskKey key) {
        int padding = key.kind == Kind.SHADOW ? SHADOW_PADDING : 0;
        int edge = key.radius + padding;
        int side = edge * 2 + 2;
        NativeImage image = new NativeImage(side * DENSITY, side * DENSITY, false);
        for (int py = 0; py < side * DENSITY; py++) {
            for (int px = 0; px < side * DENSITY; px++) {
                double x = (px + 0.5) / DENSITY;
                double y = (py + 0.5) / DENSITY;
                double qx = Math.abs(x - side / 2.0) - 1.0;
                double qy = Math.abs(y - side / 2.0) - 1.0;
                double distance = Math.hypot(Math.max(qx, 0), Math.max(qy, 0))
                        + Math.min(Math.max(qx, qy), 0) - key.radius;
                double coverage = switch (key.kind) {
                    case FILL -> MathUtil.clamp01(0.5 - distance);
                    case BORDER -> MathUtil.clamp01(0.5 - distance) * MathUtil.clamp01(distance + 1.5);
                    case SHADOW -> Math.exp(-Math.pow(Math.max(distance, 0), 2) / 40.0);
                };
                image.setPixel(px, py, ((int) Math.round(coverage * 255) << 24) | 0xFFFFFF);
            }
        }
        Identifier texture = Identifier.fromNamespaceAndPath("riserecode",
                "ui/mask_" + key.kind.name().toLowerCase(java.util.Locale.ROOT) + "_" + key.radius);
        DynamicTexture dynamic = new DynamicTexture(() -> texture.toString(), image) {
            @Override
            public GpuSampler getSampler() {
                return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            }
        };
        // TextureManager owns the GPU lifetime and releases these at shutdown.
        Minecraft.getInstance().getTextureManager().register(texture, dynamic);
        return new Mask(texture, side, edge);
    }
}
```

### `src/main/java/dev/riserecode/ui/component/UIComponent.java`

```java
package dev.riserecode.ui.component;

import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class UIComponent {
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected final Animation hover = new Animation(0, 0.14, MathUtil.Easing.IN_OUT_CUBIC);

    public record Bounds(double x, double y, double width, double height) {
        public double centerX() { return x + width / 2; }
        public double centerY() { return y + height / 2; }
    }

    public void setBounds(double x, double y, double width, double height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public Bounds getBounds() { return new Bounds(x, y, width, height); }
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    protected double updateHover(double mouseX, double mouseY, double deltaSeconds) {
        hover.animateTo(contains(mouseX, mouseY) ? 1 : 0);
        return hover.update(deltaSeconds);
    }

    public abstract void render(GuiGraphicsExtractor graphics, Font font, double mouseX,
            double mouseY, double deltaSeconds, double opacity);
    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) { return false; }
}
```

### `src/main/java/dev/riserecode/ui/component/ActionButton.java`

```java
package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ActionButton extends UIComponent {
    private final String label;
    private final Runnable action;

    public ActionButton(String label, Runnable action) { this.label = label; this.action = action; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered), opacity));
        RenderUtil.drawRoundedBorder(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawText(graphics, font, label, x + (width - RenderUtil.textWidth(font, label, false)) / 2
                + hovered * 2, y + height / 2 - 5, Theme.opacity(Theme.TEXT, opacity), false, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        action.run(); return true;
    }
}
```

### `src/main/java/dev/riserecode/ui/component/Panel.java`

```java
package dev.riserecode.ui.component;

import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Panel extends UIComponent {
    private static final double HEADER = 39;
    private final Category category;
    private final ModuleManager manager;
    private final Runnable changed;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation opening = new Animation(0, 0.28, MathUtil.Easing.OUT_QUART);
    private final Animation positionX = new Animation(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation positionY = new Animation(0, 0.12, MathUtil.Easing.OUT_QUART);
    private final Animation scroll = new Animation(0, 0.16, MathUtil.Easing.OUT_QUART);
    private double targetX;
    private double targetY;
    private double dragOffsetX;
    private double dragOffsetY;
    private double viewportWidth;
    private double viewportHeight;
    private double maxScroll;
    private boolean dragging;
    private ModuleButton captured;

    public Panel(Category category, ModuleManager manager, Runnable changed) {
        this.category = category; this.manager = manager; this.changed = changed;
        syncModules();
    }

    public Category getCategory() { return category; }
    public List<ModuleButton> getModuleButtons() { return List.copyOf(buttons); }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public boolean isOpen() { return !opening.isRunning() && opening.getValue() == 1; }
    public double getScale() { return MathUtil.lerp(0.95, 1, opening.getValue()); }

    public void open() { opening.reset(0); opening.animateTo(1); }

    public void place(double x, double y, double width, double height, double viewportWidth, double viewportHeight) {
        setBounds(x, y, width, height);
        this.viewportWidth = viewportWidth; this.viewportHeight = viewportHeight;
        targetX = x; targetY = y; clampTarget();
        this.x = targetX; this.y = targetY;
        positionX.reset(targetX); positionY.reset(targetY);
    }

    private void syncModules() {
        List<Module> registered = manager.getByCategory(category);
        if (registered.size() == buttons.size()) { return; }
        for (Module module : registered) {
            if (buttons.stream().noneMatch(button -> button.getModule() == module)) {
                buttons.add(new ModuleButton(module, manager, changed));
            }
        }
    }

    private void clampTarget() {
        targetX = Math.max(10, Math.min(viewportWidth - width - 10, targetX));
        targetY = Math.max(58, Math.min(viewportHeight - height - 28, targetY));
    }

    private double localX(double mouseX) { return x + width / 2 + (mouseX - x - width / 2) / getScale(); }
    private double localY(double mouseY) { return y + height / 2 + (mouseY - y - height / 2) / getScale(); }
    private boolean inBody(double mouseX, double mouseY) {
        return mouseX >= x + 8 && mouseX < x + width - 8 && mouseY >= y + HEADER && mouseY < y + height - 10;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        syncModules();
        opening.update(deltaSeconds);
        if (dragging) {
            double weight = 1 - Math.exp(-24 * deltaSeconds);
            x = MathUtil.lerp(x, targetX, weight); y = MathUtil.lerp(y, targetY, weight);
            positionX.reset(x); positionY.reset(y);
        } else {
            positionX.animateTo(targetX); positionY.animateTo(targetY);
            x = positionX.update(deltaSeconds); y = positionY.update(deltaSeconds);
        }
        double contentHeight = 0;
        for (ModuleButton button : buttons) {
            button.update(deltaSeconds); contentHeight += button.getPreferredHeight() + 6;
        }
        maxScroll = Math.max(0, contentHeight - (height - HEADER - 16));
        double scrollTarget = Math.max(0, Math.min(maxScroll, scroll.getTarget()));
        if (scroll.getValue() > maxScroll) { scroll.reset(maxScroll); }
        scroll.animateTo(scrollTarget); scroll.update(deltaSeconds);
        double alpha = opacity * opening.getValue();
        double pointerX = localX(mouseX); double pointerY = localY(mouseY);
        double hovered = updateHover(pointerX, pointerY, deltaSeconds);
        double scale = getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + width / 2), (float) (y + height / 2));
        graphics.pose().scale((float) scale, (float) scale);
        graphics.pose().translate((float) (-x - width / 2), (float) (-y - height / 2));
        try {
            RenderUtil.drawDropShadow(graphics, x, y + 3, width, height, Theme.PANEL_RADIUS,
                    Theme.opacity(Theme.SHADOW, alpha));
            RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.PANEL_RADIUS,
                    Theme.opacity(Theme.PANEL, alpha));
            RenderUtil.drawRoundedBorder(graphics, x, y, width, height, Theme.PANEL_RADIUS,
                    Theme.opacity(Theme.lerp(Theme.BORDER, 0x303B82F6, hovered), alpha));
            RenderUtil.drawText(graphics, font, category.getDisplayName(), x + 14, y + 14,
                    Theme.opacity(Theme.TEXT, alpha), true, false);
            RenderUtil.drawText(graphics, font, Integer.toString(buttons.size()), x + width - 23, y + 15,
                    Theme.opacity(Theme.MUTED, alpha), false, false);
            RenderUtil.drawGradientRect(graphics, x + 12, y + HEADER - 2, width - 24, 1,
                    Theme.opacity(Theme.BORDER, alpha), Theme.opacity(Theme.BORDER, alpha));
            RenderUtil.scissorPush(graphics, x + 8, y + HEADER, width - 16, height - HEADER - 10);
            try {
                double nextY = y + HEADER + 4 - scroll.getValue();
                for (ModuleButton button : buttons) {
                    button.setBounds(x + 10, nextY, width - 20, button.getPreferredHeight());
                    // Invisible rows still update their animation so scrolling never restarts a transition.
                    boolean pointerInside = inBody(pointerX, pointerY);
                    button.render(graphics, font, pointerInside ? pointerX : -Double.MAX_VALUE,
                            pointerInside ? pointerY : -Double.MAX_VALUE, deltaSeconds, alpha);
                    nextY += button.getPreferredHeight() + 6;
                }
                if (buttons.isEmpty()) {
                    RenderUtil.drawText(graphics, font, "No modules", x + 14, y + HEADER + 12,
                            Theme.opacity(Theme.MUTED, alpha), false, false);
                }
            } finally { RenderUtil.scissorPop(graphics); }
            if (maxScroll > 0) {
                double track = height - HEADER - 18;
                double thumb = Math.max(16, track * track / (track + maxScroll));
                double top = y + HEADER + 4 + (track - thumb) * scroll.getValue() / maxScroll;
                RenderUtil.drawRoundedRect(graphics, x + width - 6, top, 2, thumb, 1,
                        Theme.opacity(Theme.BLUE, alpha * 0.65));
            }
        } finally { graphics.pose().popMatrix(); }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double pointerX = localX(mouseX); double pointerY = localY(mouseY);
        if (!contains(pointerX, pointerY)) { return false; }
        if (pointerY < y + HEADER && button == 0) {
            dragging = true; dragOffsetX = mouseX - x; dragOffsetY = mouseY - y; return true;
        }
        if (inBody(pointerX, pointerY)) {
            for (ModuleButton child : buttons) {
                if (child.mouseClicked(pointerX, pointerY, button)) { captured = child; return true; }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            targetX = mouseX - dragOffsetX; targetY = mouseY - dragOffsetY; clampTarget(); return true;
        }
        return captured != null && captured.mouseDragged(localX(mouseX), localY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = dragging;
        dragging = false;
        if (captured != null) {
            consumed |= captured.mouseReleased(localX(mouseX), localY(mouseY), button); captured = null;
        }
        return consumed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        double pointerX = localX(mouseX); double pointerY = localY(mouseY);
        if (!contains(pointerX, pointerY)) { return false; }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - amount * 32)));
        return true;
    }
}
```

### `src/main/java/dev/riserecode/ui/component/ModuleButton.java`

```java
package dev.riserecode.ui.component;

import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorSwatch;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Owns presentation and input adapters, never the module's feature logic. */
public final class ModuleButton extends UIComponent {
    public static final double HEADER_HEIGHT = 34;
    public record SettingControl(Setting<?> setting, UIComponent component, double height) { }

    private final Module module;
    private final ModuleManager manager;
    private final List<SettingControl> controls = new ArrayList<>();
    private final Animation enabled;
    private final Animation expansion = new Animation(0, 0.22, MathUtil.Easing.OUT_QUART);
    private boolean expanded;
    private UIComponent captured;

    public ModuleButton(Module module, ModuleManager manager, Runnable changed) {
        this.module = module;
        this.manager = manager;
        enabled = new Animation(module.isEnabled() ? 1 : 0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
        for (Setting<?> setting : module.getSettings()) {
            UIComponent component;
            double controlHeight;
            if (setting instanceof BooleanSetting bool) {
                component = new ToggleSwitch(bool, changed); controlHeight = 28;
            } else if (setting instanceof DoubleSetting number) {
                component = new Slider(number, changed); controlHeight = 36;
            } else if (setting instanceof EnumSetting<?> option) {
                component = enumControl(option, changed); controlHeight = 42;
            } else if (setting instanceof ColorSetting color) {
                component = new ColorSwatch(color, changed); controlHeight = 28;
            } else {
                throw new IllegalArgumentException("No UI adapter for " + setting.getClass().getName());
            }
            controls.add(new SettingControl(setting, component, controlHeight));
        }
    }

    private static <E extends Enum<E>> UIComponent enumControl(EnumSetting<E> setting, Runnable changed) {
        return new EnumSelector<>(setting, changed);
    }

    public Module getModule() { return module; }
    public boolean isExpanded() { return expanded; }
    public List<SettingControl> getSettingControls() { return List.copyOf(controls); }

    public void update(double deltaSeconds) {
        expansion.animateTo(expanded ? 1 : 0);
        expansion.update(deltaSeconds);
    }

    private double settingsHeight() {
        double result = 8;
        for (SettingControl control : controls) {
            if (control.setting().isVisible()) { result += control.height() + 6; }
        }
        return result;
    }

    public double getPreferredHeight() {
        return HEADER_HEIGHT + settingsHeight() * expansion.getValue();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        hover.animateTo(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT ? 1 : 0);
        double hovered = hover.update(deltaSeconds);
        enabled.animateTo(module.isEnabled() ? 1 : 0);
        double active = enabled.update(deltaSeconds);
        int background = Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered);
        background = Theme.lerp(background, 0xD02A3653, active * 0.65);
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(background, opacity));
        RenderUtil.drawRoundedBorder(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, module.getName(), width - 48),
                x + 10 + hovered * 2, y + 11, Theme.opacity(Theme.lerp(Theme.TEXT, Theme.BLUE, active), opacity),
                false, false);
        RenderUtil.drawRoundedRect(graphics, x + width - 32, y + 14, 6, 6, 3,
                Theme.opacity(Theme.lerp(Theme.MUTED, Theme.BLUE, active), opacity));
        if (!controls.isEmpty()) {
            RenderUtil.drawText(graphics, font, expanded ? "−" : "+", x + width - 19, y + 10,
                    Theme.opacity(Theme.SECONDARY, opacity), false, false);
        }
        double bodyHeight = height - HEADER_HEIGHT;
        if (bodyHeight <= 0.5) { return; }
        RenderUtil.scissorPush(graphics, x + 2, y + HEADER_HEIGHT, width - 4, Math.max(0, bodyHeight - 2));
        try {
            double controlY = y + HEADER_HEIGHT + 4;
            for (SettingControl control : controls) {
                if (!control.setting().isVisible()) { continue; }
                control.component().setBounds(x + 10, controlY, width - 20, control.height());
                control.component().render(graphics, font, mouseX, mouseY, deltaSeconds,
                        opacity * expansion.getValue());
                controlY += control.height() + 6;
            }
        } finally {
            RenderUtil.scissorPop(graphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) { return false; }
        if (mouseY < y + HEADER_HEIGHT) {
            if ((button == 1 || (button == 0 && mouseX >= x + width - 22)) && !controls.isEmpty()) {
                expanded = !expanded;
                if (!expanded && captured != null) {
                    captured.mouseReleased(mouseX, mouseY, 0); captured = null;
                }
            } else if (button == 0) {
                manager.toggle(module);
            }
            return true;
        }
        if (!expanded) { return true; }
        for (SettingControl control : controls) {
            if (control.setting().isVisible() && control.component().mouseClicked(mouseX, mouseY, button)) {
                captured = control.component(); return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return captured != null && captured.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(mouseX, mouseY, button);
        captured = null;
        return consumed;
    }
}
```

### `src/main/java/dev/riserecode/ui/component/setting/ToggleSwitch.java`

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ToggleSwitch extends UIComponent {
    private final String label;
    private final BooleanSupplier value;
    private final Consumer<Boolean> setter;
    private final Animation slide;

    public ToggleSwitch(BooleanSetting setting, Runnable changed) {
        this(setting.getName(), setting::getValue, enabled -> { setting.setValue(enabled); changed.run(); });
    }

    public ToggleSwitch(String label, BooleanSupplier value, Consumer<Boolean> setter) {
        this.label = label; this.value = value; this.setter = setter;
        slide = new Animation(value.getAsBoolean() ? 1 : 0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        slide.animateTo(value.getAsBoolean() ? 1 : 0);
        double progress = slide.update(deltaSeconds);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, label, width - 40),
                x + hovered * 2, y + height / 2 - 5, Theme.opacity(Theme.TEXT, opacity), false, false);
        double left = x + width - 30;
        double top = y + height / 2 - 7;
        RenderUtil.drawRoundedRect(graphics, left, top, 30, 14, 7,
                Theme.opacity(Theme.lerp(Theme.HOVER, Theme.BLUE, progress), opacity));
        RenderUtil.drawRoundedBorder(graphics, left, top, 30, 14, 7, Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawRoundedRect(graphics, left + 2 + 16 * progress, top + 2, 10, 10, 5,
                Theme.opacity(Theme.TEXT, opacity));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        setter.accept(!value.getAsBoolean());
        return true;
    }
}
```

### `src/main/java/dev/riserecode/ui/component/setting/Slider.java`

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Slider extends UIComponent {
    private final DoubleSetting setting;
    private final Runnable changed;
    private final Animation position;
    private boolean dragging;

    public Slider(DoubleSetting setting, Runnable changed) {
        this.setting = setting; this.changed = changed;
        position = new Animation(normalized(), 0.1, MathUtil.Easing.OUT_QUART);
    }

    private double normalized() {
        double range = setting.getMax() - setting.getMin();
        return range == 0 ? 0 : (setting.getValue() - setting.getMin()) / range;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        position.animateTo(normalized());
        double progress = position.update(deltaSeconds);
        String value = String.format(Locale.ROOT, "%.2f", setting.getValue());
        int valueWidth = RenderUtil.textWidth(font, value, false);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, setting.getName(), width - valueWidth - 10),
                x + hovered * 2, y + 2, Theme.opacity(Theme.SECONDARY, opacity), false, false);
        RenderUtil.drawText(graphics, font, value, x + width - valueWidth, y + 2,
                Theme.opacity(Theme.TEXT, opacity), false, false);
        double trackX = x + 5;
        double trackWidth = width - 10;
        double trackY = y + 23;
        RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth, 4, 2, Theme.opacity(Theme.HOVER, opacity));
        if (progress > 0.01) {
            RenderUtil.drawRoundedRect(graphics, trackX, trackY, Math.max(4, trackWidth * progress), 4, 2,
                    Theme.opacity(Theme.BLUE, opacity));
        }
        double thumb = trackX + trackWidth * progress;
        RenderUtil.drawDropShadow(graphics, thumb - 5, trackY - 3, 10, 10, 5,
                Theme.opacity(Theme.BLUE, opacity * (0.22 + hovered * 0.18)));
        RenderUtil.drawRoundedRect(graphics, thumb - 5, trackY - 3, 10, 10, 5, Theme.opacity(Theme.TEXT, opacity));
    }

    private void setFromMouse(double mouseX) {
        double progress = MathUtil.clamp01((mouseX - x - 5) / Math.max(1, width - 10));
        setting.setValue(Math.max(setting.getMin(), Math.min(setting.getMax(),
                MathUtil.lerp(setting.getMin(), setting.getMax(), progress))));
        changed.run();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        dragging = true;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) { return false; }
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = dragging;
        dragging = false;
        return consumed;
    }
}
```

### `src/main/java/dev/riserecode/ui/component/setting/EnumSelector.java`

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A compact cycle selector: left advances, right goes to the previous option. */
public final class EnumSelector<E extends Enum<E>> extends UIComponent {
    private final EnumSetting<E> setting;
    private final Runnable changed;

    public EnumSelector(EnumSetting<E> setting, Runnable changed) { this.setting = setting; this.changed = changed; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        RenderUtil.drawText(graphics, font, setting.getName(), x, y + 1,
                Theme.opacity(Theme.SECONDARY, opacity), false, false);
        RenderUtil.drawRoundedRect(graphics, x, y + 16, width, 23, 6,
                Theme.opacity(Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered), opacity));
        RenderUtil.drawRoundedBorder(graphics, x, y + 16, width, 23, 6, Theme.opacity(Theme.BORDER, opacity));
        String option = setting.getValue().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        option = Character.toUpperCase(option.charAt(0)) + option.substring(1);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, option, width - 26),
                x + 8 + hovered * 2, y + 22, Theme.opacity(Theme.TEXT, opacity), false, false);
        RenderUtil.drawText(graphics, font, "›", x + width - 15, y + 21,
                Theme.opacity(Theme.BLUE, opacity), true, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY) || (button != 0 && button != 1)) { return false; }
        var options = setting.getOptions();
        int index = options.indexOf(setting.getValue());
        setting.setValue(options.get(Math.floorMod(index + (button == 0 ? 1 : -1), options.size())));
        changed.run();
        return true;
    }
}
```

### `src/main/java/dev/riserecode/ui/component/setting/ColorSwatch.java`

```java
package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Palette cycle control. Preserves the setting's existing alpha channel. */
public final class ColorSwatch extends UIComponent {
    private static final int[] PALETTE = {Theme.BLUE, Theme.FUCHSIA, 0xFF22D3EE, 0xFF34D399, 0xFFFBBF24, Theme.TEXT};
    private final ColorSetting setting;
    private final Runnable changed;

    public ColorSwatch(ColorSetting setting, Runnable changed) { this.setting = setting; this.changed = changed; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, setting.getName(), width - 30),
                x + hovered * 2, y + 9, Theme.opacity(Theme.SECONDARY, opacity), false, false);
        RenderUtil.drawRoundedRect(graphics, x + width - 22, y + 5, 20, 20, 6,
                Theme.opacity(setting.getValue() | 0xFF000000, opacity));
        RenderUtil.drawRoundedBorder(graphics, x + width - 22, y + 5, 20, 20, 6, Theme.opacity(Theme.BORDER, opacity));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        int index = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if ((PALETTE[i] & 0xFFFFFF) == (setting.getValue() & 0xFFFFFF)) { index = i; break; }
        }
        setting.setValue((setting.getValue() & 0xFF000000) | (PALETTE[(index + 1) % PALETTE.length] & 0xFFFFFF));
        changed.run();
        return true;
    }
}
```

### `src/main/java/dev/riserecode/ui/clickgui/ClickGui.java`

```java
package dev.riserecode.ui.clickgui;

import dev.riserecode.Client;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.hud.HudRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A native screen host; all internal rendering and input belong to our components. */
public final class ClickGui extends Screen {
    private final List<Panel> panels = new ArrayList<>();
    private final ToggleSwitch hudToggle;
    private final ActionButton closeButton = new ActionButton("Close", this::onClose);
    private final Runnable saveChanges;
    private Screen parent;
    private Panel captured;
    private double canvasScale = 1;
    private double canvasWidth;
    private double canvasHeight;
    private boolean arranged;
    private long previousFrame;

    public ClickGui(ModuleManager modules, HudRenderer hud, Runnable saveChanges) {
        super(Component.literal(Client.NAME));
        this.saveChanges = saveChanges;
        hudToggle = new ToggleSwitch("HUD", hud::isEnabled, value -> { hud.setEnabled(value); saveChanges.run(); });
        for (Category category : Category.values()) { panels.add(new Panel(category, modules, saveChanges)); }
    }

    public List<Panel> getPanels() { return List.copyOf(panels); }
    public ToggleSwitch getHudToggle() { return hudToggle; }
    public double getCanvasScale() { return canvasScale; }

    public void open(Screen parent) {
        this.parent = parent;
        previousFrame = System.nanoTime();
        for (Panel panel : panels) { panel.open(); }
        minecraft.gui.setScreen(this);
    }

    @Override
    protected void init() {
        canvasScale = Math.min(1, Math.min(width / 600.0, height / 380.0));
        canvasWidth = width / canvasScale; canvasHeight = height / canvasScale;
        int columns = Math.min(6, Math.max(3, (int) ((canvasWidth - 20) / 190)));
        int rows = (panels.size() + columns - 1) / columns;
        double panelWidth = Math.min(184, (canvasWidth - 32 - (columns - 1) * 14) / columns);
        double panelHeight = Math.min(240, (canvasHeight - 98 - (rows - 1) * 14) / rows);
        double startX = (canvasWidth - columns * panelWidth - (columns - 1) * 14) / 2;
        for (Panel panel : panels) {
            int index = panel.getCategory().ordinal();
            double left = arranged ? panel.getBounds().x() : startX + (index % columns) * (panelWidth + 14);
            double top = arranged ? panel.getBounds().y() : 64 + (index / columns) * (panelHeight + 14);
            panel.place(left, top, panelWidth, panelHeight, canvasWidth, canvasHeight);
        }
        arranged = true;
        hudToggle.setBounds(canvasWidth - 166, 15, 76, 26);
        closeButton.setBounds(canvasWidth - 76, 14, 60, 28);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) { extractPanorama(graphics, delta); }
        extractBlurredBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double seconds = previousFrame == 0 ? 0 : Math.max(0, (now - previousFrame) / 1_000_000_000.0);
        previousFrame = now;
        RenderUtil.drawGradientRect(graphics, 0, 0, width, height, 0xB80F1115, 0xDE0F1115);
        double pointerX = mouseX / canvasScale; double pointerY = mouseY / canvasScale;
        graphics.pose().pushMatrix();
        graphics.pose().scale((float) canvasScale, (float) canvasScale);
        try {
            Panel hovered = null;
            for (int i = panels.size() - 1; i >= 0; i--) {
                if (panels.get(i).contains(pointerX, pointerY)) { hovered = panels.get(i); break; }
            }
            for (Panel panel : panels) {
                graphics.nextStratum();
                panel.render(graphics, font, panel == hovered ? pointerX : -Double.MAX_VALUE,
                        panel == hovered ? pointerY : -Double.MAX_VALUE, seconds, 1);
            }
            graphics.nextStratum();
            RenderUtil.drawRoundedRect(graphics, 16, 15, 27, 27, 8, Theme.BLUE);
            RenderUtil.drawText(graphics, font, "R", 24, 23, Theme.TEXT, true, false);
            RenderUtil.drawText(graphics, font, Client.NAME, 53, 16, Theme.TEXT, true, false);
            RenderUtil.drawText(graphics, font, "Utility client · " + Client.INSTANCE.getVersion(), 53, 32,
                    Theme.SECONDARY, false, false);
            hudToggle.render(graphics, font, pointerX, pointerY, seconds, 1);
            closeButton.render(graphics, font, pointerX, pointerY, seconds, 1);
            RenderUtil.drawText(graphics, font, "Left click · Toggle    Right click · Settings    Drag header · Move",
                    16, canvasHeight - 19, Theme.MUTED, false, false);
        } finally { graphics.pose().popMatrix(); }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double pointerX = event.x() / canvasScale; double pointerY = event.y() / canvasScale;
        if (closeButton.mouseClicked(pointerX, pointerY, event.button())
                || hudToggle.mouseClicked(pointerX, pointerY, event.button())) { return true; }
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (panel.mouseClicked(pointerX, pointerY, event.button())) {
                captured = panel;
                panels.remove(i); panels.add(panel);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return captured != null && captured.mouseDragged(event.x() / canvasScale, event.y() / canvasScale, event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(event.x() / canvasScale, event.y() / canvasScale, event.button());
        captured = null;
        return consumed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseScrolled(mouseX / canvasScale, mouseY / canvasScale, vertical)) { return true; }
        }
        return false;
    }

    @Override
    public void onClose() {
        if (captured != null) { captured.mouseReleased(0, 0, 0); captured = null; }
        saveChanges.run();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
```

### `src/main/java/dev/riserecode/ui/hud/HudRenderer.java`

```java
package dev.riserecode.ui.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer implements AutoCloseable {
    /** Read-only presentation state, useful to future HUD editors. */
    public record EntryState(Module module, double visibility, double y) { }
    private static final class Entry {
        private final Module module;
        private final Animation y;
        private Animation slide = new Animation(0, 0.38, MathUtil.Easing.OUT_ELASTIC);
        private boolean active;
        private int width;

        private Entry(Module module) {
            this.module = module;
            y = new Animation(8, 0.24, MathUtil.Easing.OUT_QUART);
        }

        private void update(boolean enabled, double deltaSeconds) {
            if (enabled != active) {
                active = enabled;
                slide = new Animation(slide.getValue(), enabled ? 0.38 : 0.22,
                        enabled ? MathUtil.Easing.OUT_ELASTIC : MathUtil.Easing.OUT_QUART);
                slide.animateTo(enabled ? 1 : 0);
            }
            slide.update(deltaSeconds);
        }
    }

    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final String watermark;
    private final EventBus.Subscription subscription;
    private final Map<Module, Entry> entries = new LinkedHashMap<>();
    private final Animation visibility = new Animation(1, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    private boolean enabled = true;

    public HudRenderer(EventBus eventBus, Minecraft minecraft, ModuleManager modules, String watermark) {
        this.minecraft = minecraft; this.modules = modules; this.watermark = watermark;
        subscription = eventBus.subscribe(Render2DEvent.class, this::render);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<EntryState> getEntries() {
        return entries.values().stream().map(entry -> new EntryState(entry.module, entry.slide.getValue(), entry.y.getValue())).toList();
    }

    private void render(Render2DEvent event) {
        double seconds = event.getDeltaSeconds();
        visibility.animateTo(enabled ? 1 : 0);
        double opacity = MathUtil.clamp01(visibility.update(seconds));
        for (Module module : modules.getModules()) {
            if (module.isEnabled()) { entries.computeIfAbsent(module, Entry::new); }
        }
        for (Entry entry : entries.values()) { entry.update(entry.module.isEnabled(), seconds); }
        entries.values().removeIf(entry -> !entry.active && !entry.slide.isRunning() && entry.slide.getValue() == 0);
        if (minecraft.font == null) { return; }
        var sorted = new ArrayList<>(entries.values());
        for (Entry entry : sorted) { entry.width = RenderUtil.textWidth(minecraft.font, entry.module.getName(), false); }
        sorted.sort(Comparator.comparingInt((Entry entry) -> entry.width).reversed().thenComparing(entry -> entry.module.getName()));
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).y.animateTo(8 + i * 24);
            sorted.get(i).y.update(seconds);
        }
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || opacity <= 0) { return; }
        GuiGraphicsExtractor graphics = event.getGraphics();
        int watermarkWidth = RenderUtil.textWidth(minecraft.font, watermark, true) + 24;
        RenderUtil.drawDropShadow(graphics, 8, 8, watermarkWidth, 28, 8, Theme.opacity(Theme.SHADOW, opacity));
        RenderUtil.drawRoundedRect(graphics, 8, 8, watermarkWidth, 28, 8, Theme.opacity(Theme.PANEL, opacity));
        RenderUtil.drawRoundedBorder(graphics, 8, 8, watermarkWidth, 28, 8, Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawText(graphics, minecraft.font, watermark, 20, 16, Theme.opacity(Theme.TEXT, opacity), true, false);
        RenderUtil.drawGradientRect(graphics, 20, 32, watermarkWidth - 24, 1,
                Theme.opacity(Theme.BLUE, opacity), Theme.opacity(Theme.FUCHSIA, opacity));
        for (int i = 0; i < sorted.size(); i++) {
            Entry entry = sorted.get(i);
            double amount = entry.slide.getValue();
            double alpha = opacity * MathUtil.clamp01(amount);
            double boxWidth = entry.width + 24;
            double x = graphics.guiWidth() - boxWidth - 8 + (1 - amount) * (boxWidth + 24);
            double y = entry.y.getValue();
            int accent = Theme.lerp(Theme.BLUE, Theme.FUCHSIA, sorted.size() <= 1 ? 0 : (double) i / (sorted.size() - 1));
            RenderUtil.drawDropShadow(graphics, x, y, boxWidth, 21, 6, Theme.opacity(Theme.SHADOW, alpha * 0.6));
            RenderUtil.drawRoundedRect(graphics, x, y, boxWidth, 21, 6, Theme.opacity(Theme.PANEL, alpha));
            RenderUtil.drawRoundedBorder(graphics, x, y, boxWidth, 21, 6, Theme.opacity(Theme.BORDER, alpha));
            RenderUtil.drawRoundedRect(graphics, x + boxWidth - 3, y + 5, 2, 11, 1, Theme.opacity(accent, alpha));
            RenderUtil.drawStringWithShadow(graphics, minecraft.font, entry.module.getName(), x + 10, y + 6,
                    Theme.opacity(Theme.TEXT, alpha));
        }
    }

    @Override
    public void close() { subscription.close(); entries.clear(); }
}
```

### `src/test/java/dev/riserecode/UiChecks.java`

```java
package dev.riserecode;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.component.setting.ColorSwatch;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import java.util.concurrent.atomic.AtomicInteger;

/** Input semantics without a graphics driver; real rendering is covered by client gametests. */
public final class UiChecks {
    private enum Choice { FIRST, SECOND, THIRD }
    private static int assertions;
    private UiChecks() { }

    public static void main(String[] args) {
        check(Theme.lerp(0x00000000, 0xFFFFFFFF, 0.5) == 0x80808080, "all channels interpolate");
        check(Theme.lerp(Theme.BLUE, Theme.FUCHSIA, -1) == Theme.BLUE, "negative color progress clamps");
        check(Theme.lerp(Theme.BLUE, Theme.FUCHSIA, 2) == Theme.FUCHSIA, "overshoot color progress clamps");
        check(Theme.opacity(Theme.PANEL, 0.5) == 0x60181A20, "panel alpha is multiplied");
        check(Theme.opacity(Theme.BLUE, 0) == 0x003B82F6, "zero opacity preserves RGB");
        AtomicInteger saves = new AtomicInteger();
        BooleanSetting bool = new BooleanSetting("Toggle", "", false);
        ToggleSwitch toggle = new ToggleSwitch(bool, saves::incrementAndGet);
        toggle.setBounds(10, 10, 100, 28);
        check(!toggle.mouseClicked(5, 20, 0) && !bool.getValue(), "toggle ignores outside input");
        check(!toggle.mouseClicked(20, 20, 1), "toggle ignores right click");
        check(toggle.mouseClicked(20, 20, 0) && bool.getValue(), "toggle updates setting");
        check(saves.get() == 1, "toggle requests persistence once");
        DoubleSetting number = new DoubleSetting("Slider", "", 0, -2, 2);
        Slider slider = new Slider(number, saves::incrementAndGet);
        slider.setBounds(10, 10, 100, 36);
        check(slider.mouseClicked(60, 35, 0) && number.getValue() == 0, "slider center maps to range midpoint");
        check(slider.mouseDragged(500, 100, 0) && number.getValue() == 2, "captured slider clamps outside right");
        check(slider.mouseDragged(-500, 100, 0) && number.getValue() == -2, "captured slider clamps outside left");
        check(slider.mouseReleased(-500, 100, 0), "slider releases capture outside control");
        check(!slider.mouseDragged(60, 35, 0) && number.getValue() == -2, "released slider ignores motion");
        DoubleSetting constant = new DoubleSetting("Constant", "", 3, 3, 3);
        Slider fixed = new Slider(constant, saves::incrementAndGet);
        fixed.setBounds(0, 0, 100, 36);
        check(fixed.mouseClicked(80, 20, 0) && constant.getValue() == 3, "zero range slider remains finite");
        EnumSetting<Choice> choice = new EnumSetting<>("Choice", "", Choice.FIRST);
        EnumSelector<Choice> selector = new EnumSelector<>(choice, saves::incrementAndGet);
        selector.setBounds(0, 0, 100, 42);
        check(selector.mouseClicked(20, 20, 1) && choice.getValue() == Choice.THIRD, "enum reverse wraps");
        check(selector.mouseClicked(20, 20, 0) && choice.getValue() == Choice.FIRST, "enum forward wraps");
        ColorSetting color = new ColorSetting("Color", "", 0x803B82F6);
        ColorSwatch swatch = new ColorSwatch(color, saves::incrementAndGet);
        swatch.setBounds(0, 0, 100, 28);
        check(swatch.mouseClicked(20, 20, 0) && color.getValue() == 0x80D946EF, "palette changes RGB and preserves alpha");
        check(!swatch.contains(100, 15), "right boundary belongs to next control");
        System.out.println("UI checks passed (" + assertions + " assertions).");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) { throw new AssertionError(message); }
    }
}
```

### `src/gametest/java/dev/riserecode/ClientIntegrationTest.java`

```java
package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.Panel;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.util.RenderUtil;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Real keyboard/mouse input and render frames; fixtures never enter the release JAR. */
public final class ClientIntegrationTest implements FabricClientGameTest {
    private static final int ANIMATION_TIMEOUT = 2000;
    private enum Mode { FIRST, SECOND }
    private static final class ControlsModule extends Module {
        private final BooleanSetting advanced = register(new BooleanSetting("Advanced", "", false));
        private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "", Mode.FIRST));
        private final ColorSetting color = register(new ColorSetting("Accent", "", 0x803B82F6));
        private final DoubleSetting conditional = register(new DoubleSetting("Conditional", "", 0.5, 0, 1));
        private ControlsModule(String name) {
            super(name, "Client test fixture", Category.UTILITY, Client.INSTANCE.getEventBus());
            conditional.setVisibleWhen(advanced::getValue);
            for (int i = 0; i < 8; i++) { register(new DoubleSetting("Slider " + (i + 1), "", 0.5, 0, 1)); }
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(1280, 800);
        check(context.computeOnClient(minecraft -> Client.INSTANCE.isInitialized()), "Fabric initialization");
        check(Files.exists(Client.INSTANCE.getConfigManager().getFile()), "default config creation");
        context.runOnClient(minecraft -> {
            var fullBright = fullBright();
            fullBright.setEnabled(false);
            fullBright.getBrightness().setValue(1.0);
            Client.INSTANCE.getHudRenderer().setEnabled(true);
            minecraft.options.gamma().set(0.35);
        });
        context.setScreen(TitleScreen::new);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        waitOpen(context);
        check(context.computeOnClient(minecraft -> gui().children().isEmpty()), "no native widget children");
        ModuleButton fullBrightButton = context.computeOnClient(minecraft -> button("FullBright"));
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 1.0), "FullBright applies gamma");
        click(context, fullBrightButton::getBounds, 0);
        check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "FullBright restores gamma");
        click(context, () -> gui().getHudToggle().getBounds(), 0);
        check(context.computeOnClient(minecraft -> !Client.INSTANCE.getHudRenderer().isEnabled()), "HUD switch");
        click(context, () -> gui().getHudToggle().getBounds(), 0);
        click(context, fullBrightButton::getBounds, 1);
        context.waitFor(minecraft -> fullBrightButton.getBounds().height() > 75, ANIMATION_TIMEOUT);
        Slider brightness = context.computeOnClient(minecraft -> (Slider) fullBrightButton.getSettingControls().getFirst().component());
        move(context, () -> brightness.getBounds(), 0.5, 0.7);
        context.getInput().holdMouse(0);
        move(context, () -> brightness.getBounds(), 0.22, 0.7);
        context.getInput().releaseMouse(0);
        check(context.computeOnClient(minecraft -> fullBright().getBrightness().getValue() > 0.15
                && fullBright().getBrightness().getValue() < 0.3), "real mouse slider drag");
        click(context, fullBrightButton::getBounds, 0);
        context.waitTick();
        check(context.computeOnClient(minecraft -> Math.abs(minecraft.options.gamma().get()
                - fullBright().getBrightness().getValue()) < 1e-9), "slider setting reaches module tick logic");
        click(context, fullBrightButton::getBounds, 0);
        context.runOnClient(minecraft -> fullBright().getBrightness().setValue(1.0));
        context.waitTicks(12);
        context.takeScreenshot("riserecode-clickgui");

        ControlsModule controls = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Controls")));
        ControlsModule longer = context.computeOnClient(minecraft -> Client.INSTANCE.getModuleManager().register(new ControlsModule("UI Test Longer Name")));
        context.waitFor(minecraft -> panel(Category.UTILITY).getModuleButtons().size() == 2);
        ModuleButton controlsButton = context.computeOnClient(minecraft -> button(controls.getName()));
        click(context, controlsButton::getBounds, 1);
        context.waitFor(minecraft -> panel(Category.UTILITY).getMaxScroll() > 250, ANIMATION_TIMEOUT);
        click(context, () -> controlsButton.getSettingControls().get(0).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.advanced.getValue() && controls.conditional.isVisible()), "boolean control and visibleWhen");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(1).component());
        click(context, () -> controlsButton.getSettingControls().get(1).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.mode.getValue() == Mode.SECOND), "enum control");
        reveal(context, panel(Category.UTILITY), controlsButton.getSettingControls().get(2).component());
        click(context, () -> controlsButton.getSettingControls().get(2).component().getBounds(), 0);
        check(context.computeOnClient(minecraft -> controls.color.getValue() == 0x80D946EF), "color palette preserves alpha");
        move(context, () -> panel(Category.UTILITY).getBounds(), 0.5, 0.8);
        context.getInput().scroll(-2);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() > 60, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> panel(Category.UTILITY).getScrollOffset() <= panel(Category.UTILITY).getMaxScroll()), "scroll stays bounded");
        context.takeScreenshot("riserecode-settings-scroll");
        context.getInput().scroll(100);
        context.waitFor(minecraft -> panel(Category.UTILITY).getScrollOffset() == 0, ANIMATION_TIMEOUT);
        // Close the settings and drag the category through Minecraft's actual mouse handler.
        click(context, controlsButton::getBounds, 1);
        Panel renderPanel = context.computeOnClient(minecraft -> panel(Category.RENDER));
        double startX = context.computeOnClient(minecraft -> renderPanel.getBounds().x());
        move(context, renderPanel::getBounds, 0.4, 0.07);
        context.getInput().holdMouse(0);
        context.getInput().moveCursor(-40, 15);
        context.waitTicks(3);
        context.getInput().releaseMouse(0);
        context.waitFor(minecraft -> renderPanel.getBounds().x() < startX - 8, ANIMATION_TIMEOUT);
        check(context.computeOnClient(minecraft -> renderPanel.getBounds().y() >= 58), "drag constrained to canvas");
        context.getInput().resizeWindow(640, 400);
        context.waitTicks(5);
        check(context.computeOnClient(minecraft -> gui().getPanels().stream().allMatch(p -> p.getBounds().x() >= 10)), "small window layout clamped");
        context.takeScreenshot("riserecode-small-window");
        context.getInput().resizeWindow(1280, 800);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(TitleScreen.class);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT);
        context.waitForScreen(ClickGui.class);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitForScreen(TitleScreen.class);

        AtomicInteger hudFrames = new AtomicInteger();
        AtomicInteger worldFrames = new AtomicInteger();
        var hudSubscription = Client.INSTANCE.getEventBus().subscribe(Render2DEvent.class, event -> hudFrames.incrementAndGet());
        var worldSubscription = Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class, event -> worldFrames.incrementAndGet());
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(minecraft -> Client.INSTANCE.getModuleManager().setKeybind(fullBright(), InputConstants.KEY_B));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> fullBright().isEnabled()), "module key toggles once");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == fullBright() && entry.visibility() == 1), ANIMATION_TIMEOUT);
            check(hudFrames.get() > 0 && worldFrames.get() > 0, "HUD/world events reach custom bus");
            context.runOnClient(minecraft -> { controls.setEnabled(true); longer.setEnabled(true); });
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 3);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.visibility() == 1 && entry.y() % 24 == 8), ANIMATION_TIMEOUT);
            check(context.computeOnClient(minecraft -> {
                var entries = Client.INSTANCE.getHudRenderer().getEntries();
                var widest = entries.stream().max(java.util.Comparator.comparingInt(entry ->
                        RenderUtil.textWidth(minecraft.font, entry.module().getName(), false))).orElseThrow();
                return widest.y() == 8;
            }), "array sorted by styled text width");
            context.takeScreenshot("riserecode-hud");
            context.runOnClient(minecraft -> longer.setEnabled(false));
            check(context.computeOnClient(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .anyMatch(entry -> entry.module() == longer && entry.visibility() > 0)), "disabled entry retained for exit");
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().size() == 2, ANIMATION_TIMEOUT);
            context.waitFor(minecraft -> Client.INSTANCE.getHudRenderer().getEntries().stream()
                    .allMatch(entry -> entry.y() == 8 || entry.y() == 32), ANIMATION_TIMEOUT);
            context.runOnClient(minecraft -> controls.setEnabled(false));
            context.getInput().pressKey(InputConstants.KEY_B);
            check(context.computeOnClient(minecraft -> minecraft.options.gamma().get() == 0.35), "key disable restores gamma");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitForScreen(ClickGui.class);
            waitOpen(context);
            context.takeScreenshot("riserecode-glass-in-world");
            context.getInput().pressKey(InputConstants.KEY_RSHIFT);
            context.waitFor(minecraft -> minecraft.gui.screen() == null);
            context.waitTicks(3);
            check(context.computeOnClient(minecraft -> minecraft.gui.screen() == null), "GUI close does not reopen");
        } finally {
            hudSubscription.close(); worldSubscription.close();
        }
        context.runOnClient(minecraft -> {
            Client.INSTANCE.getModuleManager().setKeybind(fullBright(), -1);
            Client.INSTANCE.getConfigManager().save();
        });
    }

    private static FullBrightModule fullBright() { return Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow(); }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static Panel panel(Category category) { return gui().getPanels().stream().filter(panel -> panel.getCategory() == category).findFirst().orElseThrow(); }
    private static ModuleButton button(String name) {
        return gui().getPanels().stream().flatMap(panel -> panel.getModuleButtons().stream())
                .filter(button -> button.getModule().getName().equals(name)).findFirst().orElseThrow();
    }
    private static void waitOpen(ClientGameTestContext context) {
        context.waitFor(minecraft -> gui().getPanels().stream().allMatch(Panel::isOpen), ANIMATION_TIMEOUT);
    }
    private static void reveal(ClientGameTestContext context, Panel panel, UIComponent component) {
        double amount = context.computeOnClient(minecraft -> {
            var body = panel.getBounds(); var control = component.getBounds();
            return (body.y() + 39 + (body.height() - 49) / 2 - control.centerY()) / 32;
        });
        move(context, panel::getBounds, 0.5, 0.8);
        double previous = context.computeOnClient(minecraft -> panel.getScrollOffset());
        context.getInput().scroll(amount);
        double target = Math.max(0, Math.min(panel.getMaxScroll(), previous - amount * 32));
        context.waitFor(minecraft -> Math.abs(panel.getScrollOffset() - target) < 0.01, ANIMATION_TIMEOUT);
    }
    private static void move(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, double x, double y) {
        double[] cursor = context.computeOnClient(minecraft -> {
            var b = bounds.get();
            return new double[] {(b.x() + b.width() * x) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth(),
                    (b.y() + b.height() * y) * gui().getCanvasScale()
                    * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.waitTick();
    }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, int mouseButton) {
        // Headers stay fixed when a module expands; clicking the center of the full body would edit a setting.
        Supplier<UIComponent.Bounds> header = () -> {
            var b = bounds.get(); return new UIComponent.Bounds(b.x(), b.y(), b.width(), Math.min(28, b.height()));
        };
        move(context, header, 0.4, 0.5);
        context.getInput().pressMouse(mouseButton);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
```

## 3. Resource and build integration

Inter 4.1 Regular (400) and Medium (500) are included as actual TTF resources with their SIL Open Font License. The JSON providers fall back to Minecraft glyphs for characters Inter does not cover. No shaders or additional mixins are needed. Existing Fabric entrypoint and metadata still apply; `${version}` now expands to `0.2.0`. The Java toolchain and compiler release remain 25.

### `src/main/resources/assets/riserecode/font/inter_regular.json`

```json
{
  "providers": [
    { "type": "ttf", "file": "riserecode:inter_regular.ttf", "size": 10, "oversample": 4, "shift": [0, 0] },
    { "type": "reference", "id": "minecraft:default" }
  ]
}
```

### `src/main/resources/assets/riserecode/font/inter_medium.json`

```json
{
  "providers": [
    { "type": "ttf", "file": "riserecode:inter_medium.ttf", "size": 11, "oversample": 4, "shift": [0, 0] },
    { "type": "reference", "id": "minecraft:default" }
  ]
}
```

### `build.gradle`

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group
base { archivesName = project.archives_base_name }

repositories {
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
    compileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
    testCompileOnly 'com.google.errorprone:error_prone_annotations:2.48.0'
}

processResources {
    inputs.property 'version', project.version
    filesMatching('fabric.mod.json') { expand version: project.version }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    options.release = 25
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-Xlint:all', '-Werror']
}

tasks.register('foundationCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.FoundationChecks'
}

tasks.register('runtimeCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.RuntimeChecks'
}

tasks.register('uiCheck', JavaExec) {
    dependsOn testClasses
    classpath = sourceSets.test.runtimeClasspath
    mainClass = 'dev.riserecode.UiChecks'
}

tasks.named('check') { dependsOn 'foundationCheck', 'runtimeCheck', 'uiCheck' }

// The dependency-free checks above use JavaExec rather than a JUnit runner.
tasks.named('test') { enabled = false }

fabricApi {
    configureTests {
        createSourceSet = true
        modId = 'riserecode-test'
        enableGameTests = false
        enableClientGameTests = true
    }
}

loom.runs.clientGameTest {
    runDir = 'build/run/clientGameTest'
    programArgs '--graphicsBackend', providers.gradleProperty('graphicsBackend').getOrElse('OPENGL')
}
```

### `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=false
minecraft_version=26.2
loader_version=0.19.5
loom_version=1.17.21
fabric_api_version=0.161.0+26.2
mod_version=0.2.0
maven_group=dev.riserecode
archives_base_name=riserecode
```

Bundled binary/font-license resources:

- `src/main/resources/assets/riserecode/font/inter_regular.ttf`
- `src/main/resources/assets/riserecode/font/inter_medium.ttf`
- `src/main/resources/licenses/inter-OFL.txt`

Official font source: [Inter 4.1](https://github.com/rsms/inter/releases/tag/v4.1).

## 4. Setup and integration notes

- `ClickGui` still extends `Screen`, but contains no native widget children. It manually extracts all component render states and routes Minecraft 26.2 `MouseButtonEvent` input. Right Shift opens/closes; Escape or Close returns to the previous screen.
- `RenderUtil` submits `GUI_TEXTURED` elements through `GuiGraphicsExtractor`. Lazily generated distance-field masks provide actual antialiased rounded fills, inner borders, and Gaussian-profile soft shadows. Nine-slice sampling preserves corner radii. The texture manager owns mask GPU resources; no raw OpenGL or custom shaders are used.
- Native backdrop blur happens before UI extraction and respects Minecraft's Menu Background Blur option. This uses the game's configured radius, rather than forcing a separate fixed 20px blur shader. UI strata preserve front-to-back floating-panel order.
- `scissorPush` uses the extractor's nested scissor stack. The panel body clips scrolling rows; an expanded module adds a second clip for its animated body. Minecraft transforms logical coordinates by the current pose and framebuffer scale; pop restores the previous intersection. Rounded panel corners remain outside the inset body clip. Hit testing rejects clipped settings and inverts both screen scale and opening scale.
- Panel opening uses OutQuart, 0.95→1 scale and fade. Dragging uses time-based exponential damping, then settles with an OutQuart animation. Toggle/hover colors use InOutCubic. Sliders, setting expansion, and scroll offsets use OutQuart. The HUD uses elastic entry, quartic exit, and quartic Y reordering, retaining disabled rows until their exits end. Delta time comes from elapsed monotonic seconds.
- Right click a module header or use +/− to expand settings. Boolean switches slide; double sliders capture dragging until mouse release; enum selectors cycle forward/backward with left/right click; color swatches cycle a palette while preserving alpha. `visibleWhen` is evaluated every layout. Changes use the existing config save callback. The only production feature remains FullBright.
- Panel position and expanded state survive reopening during the same session. Position persistence, a full color picker, and module-key editing are outside this phase.

Run from this project folder:

```bash
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
```

Use a Minecraft 26.2 Fabric profile with Java 25 and Fabric API to install `build/libs/riserecode-0.2.0.jar`.

Open with Right Shift, hover headers and switches, toggle FullBright repeatedly, expand Brightness, drag its slider, drag category headers, resize the window, and scroll an overflowing settings list. In a world, toggle modules rapidly and watch entry slides, exits, and Y reordering. F1 and the GUI HUD switch control HUD visibility.

Automated checks:

```bash
./gradlew build
./gradlew runClientGameTest
./gradlew runClientGameTest -PgraphicsBackend=VULKAN
```

The build runs 82 foundation, 19 runtime/config, and 19 UI input/color assertions. The separate client test mod supplies extra utility modules to exercise all setting types, overflow, and HUD sorting; these modules and test classes are excluded from the shipped JAR. Game tests boot the actual client and use real keyboard and mouse handlers. They require a display and supported graphics driver, use an isolated test world/config, and exit automatically.

Rendering API references: [GUI extraction and scissor stack](https://docs.fabricmc.net/develop/rendering/gui-graphics), [custom screens and modern input](https://docs.fabricmc.net/develop/rendering/gui/custom-screens).

Verification evidence: OpenGL completed all real-client interaction checks. The Vulkan run selected Mesa RADV 26.2.2 on this machine, then reported a guilty GPU context loss and stalled in `VulkanGpuSurface.present` / `vkQueuePresentKHR` while the test waited for the opening animation. Only that isolated test process was terminated. Vulkan runtime behavior remains unverified; the stack does not establish the underlying driver or rendering cause. Logs and thread dump are preserved in `build/verification/vulkan`.

Final verification completed successfully: `./gradlew build runClientGameTest --offline --no-daemon` on OpenGL. All 120 dependency-free assertions and the full real-client test passed. The release JAR was checked for Java class version 69 (Java 25), client-only metadata/version 0.2.0, both Inter TTFs and their license, and exclusion of test fixtures. Final screenshots and log are in `build/verification/opengl`.
