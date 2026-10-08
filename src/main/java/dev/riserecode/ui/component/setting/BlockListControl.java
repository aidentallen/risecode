package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BlockListControl extends UIComponent {
    private final BlockListSetting setting;
    private final Runnable changed;
    public BlockListControl(BlockListSetting setting, Runnable changed) { this.setting = setting; this.changed = changed; }
    @Override public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double amount = updateHover(mouseX, mouseY, seconds);
        RenderUtil.text(graphics, font, "Blocks: " + setting.getValue().size() + " selected  ›", x + amount, y + 4, 0.88, Theme.opacity(Theme.TEXT, opacity));
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        Minecraft mc = Minecraft.getInstance(); mc.gui.setScreen(new BlockPickerScreen(mc.gui.screen(), setting, changed)); return true;
    }
}
