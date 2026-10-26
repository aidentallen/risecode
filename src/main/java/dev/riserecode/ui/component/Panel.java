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
