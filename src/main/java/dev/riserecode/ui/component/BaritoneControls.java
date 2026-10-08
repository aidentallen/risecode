package dev.riserecode.ui.component;

import dev.riserecode.module.modules.BaritoneModule;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BaritoneControls extends UIComponent {
    private final BaritoneModule module;
    private final List<ActionButton> buttons;
    public BaritoneControls(BaritoneModule module) { this.module = module; buttons = List.of("Start", "Pause", "Resume", "Stop").stream().map(label -> new ActionButton(label, () -> { FocusedInput.commit(); module.action(label); })).toList(); }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double delta, double opacity) {
        RenderUtil.text(g, font, RenderUtil.ellipsize(font, module.status(), width / 0.65), x, y, 0.65, Theme.opacity(module.access().available() ? Theme.SECONDARY : 0xFFFFAA77, opacity));
        for (int i = 0; i < buttons.size(); i++) { var button = buttons.get(i); button.setBounds(x + (i % 2) * (width / 2 + 2), y + 12 + (i / 2) * 21, width / 2 - 3, 18); button.render(g, font, mx, my, delta, opacity); }
    }
    @Override public boolean mouseClicked(double mx, double my, int button) { return buttons.stream().anyMatch(b -> b.mouseClicked(mx, my, button)); }
}
