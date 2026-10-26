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
