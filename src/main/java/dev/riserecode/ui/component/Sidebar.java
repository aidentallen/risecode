package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed quarter-width navigation with a compact animated selection pill. */
public final class Sidebar extends UIComponent {
    public record Item(String label, Icon icon) { }
    private static final double ROW_HEIGHT = 20.75;
    private final List<Item> items;
    private final IntConsumer selectedAction;
    private final List<Animation> hovers;
    private final Animation selectedY = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 0, 0.2, MathUtil.Easing.OUT_QUART);
    private final Animation selectedWidth = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 52, 0.2, MathUtil.Easing.OUT_QUART);
    private int selected;
    public Sidebar(List<Item> items, IntConsumer selectedAction) {
        this.items = List.copyOf(items); this.selectedAction = selectedAction;
        hovers = items.stream().map(item -> Animation.ui(0, 0.16, MathUtil.Easing.IN_OUT_CUBIC)).toList();
    }
    public void setSelected(int index) { selected = index; selectedY.animateTo(index * ROW_HEIGHT); }
    public int getSelected() { return selected; }
    public Bounds getItemBounds(int index) { return new Bounds(x + 10, y + 43 + index * ROW_HEIGHT, width - 20, 17); }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double logoScale = Math.min(1.65, (width - 37) / Math.max(1, RenderUtil.textWidth(font, Client.INSTANCE.getName(), false)));
        RenderUtil.text(graphics, font, Client.INSTANCE.getName(), x + 14, y + 14, logoScale, Theme.opacity(Theme.TEXT, opacity));
        dev.riserecode.ui.theme.BrandingLogo.drawBeside(graphics, x + 14, y + 14, RenderUtil.textWidth(font, Client.INSTANCE.getName(), false) * logoScale, opacity);
        double versionX = x + 14 + RenderUtil.textWidth(font, Client.INSTANCE.getName(), false) * logoScale + 1;
        RenderUtil.text(graphics, font, Client.INSTANCE.getVersion(), versionX, y + 12, 0.55,
                Theme.opacity(Theme.accent(), opacity * 0.85));
        String selectedLabel = Theme.label(items.get(selected).label());
        selectedWidth.animateTo(Math.min(width - 20, RenderUtil.textWidth(font, selectedLabel, false) * 0.88 + 28));
        double pillWidth = selectedWidth.update(seconds);
        double top = y + 43 + selectedY.update(seconds);
        RenderUtil.drawGlow(graphics, x + 10, top, pillWidth, 16, 4,
                Theme.opacity(Theme.accent(), opacity));
        if (Theme.hasGradient()) {
            RenderUtil.drawAccentRoundedRect(graphics, x + 10, top, pillWidth, 16, 4, opacity);
        } else {
            RenderUtil.drawRoundedRect(graphics, x + 10, top, pillWidth, 16, 4,
                    Theme.opacity(Theme.lerp(Theme.accent(), 0xFF1AA99E, 0.2), opacity));
        }
        for (int i = 0; i < items.size(); i++) {
            Bounds row = getItemBounds(i);
            boolean inside = mouseX >= row.x() && mouseX < row.x() + row.width() && mouseY >= row.y() && mouseY < row.y() + row.height();
            hovers.get(i).animateTo(inside ? 1 : 0);
            double amount = hovers.get(i).update(seconds);
            if (i != selected) {
                RenderUtil.drawRoundedRect(graphics, row.x(), row.y(), Math.min(width - 20,
                        RenderUtil.textWidth(font, Theme.label(items.get(i).label()), false) * 0.88 + 28), 16, 4,
                        Theme.opacity(Theme.HOVER, opacity * amount * 0.4));
            }
            int color = i == selected ? Theme.opacity(Theme.manager().colorSetting("Selected Tab Color").sample(row.x(), row.y()), opacity)
                    : Theme.opacity(Theme.blendPalette(Theme.SECONDARY, Theme.TEXT, amount), opacity);
            items.get(i).icon().draw(graphics, x + 14 + amount * 0.65 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER), row.y() + 3, color);
            double labelScale = Math.min(0.88, (width - 40) / Math.max(1, RenderUtil.textWidth(font, Theme.label(items.get(i).label()), false)));
            if (i == selected) {
                RenderUtil.paintText(graphics, font, Theme.label(items.get(i).label()), x + 29 + amount * 0.65 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER),
                        row.y() + 4, labelScale, Theme.paint("Selected Tab Color"), opacity, false);
            } else {
                RenderUtil.text(graphics, font, Theme.label(items.get(i).label()), x + 29 + amount * 0.65 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER),
                        row.y() + 4, labelScale, color);
            }
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) { return false; }
        for (int i = 0; i < items.size(); i++) {
            Bounds b = getItemBounds(i);
            if (mouseX >= b.x() && mouseX < b.x() + b.width() && mouseY >= b.y() && mouseY < b.y() + b.height()) {
                selectedAction.accept(i); return true;
            }
        }
        return false;
    }
}
