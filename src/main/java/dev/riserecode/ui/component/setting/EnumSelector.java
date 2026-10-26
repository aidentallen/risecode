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
