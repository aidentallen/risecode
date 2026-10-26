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
        double seconds = previousFrame == 0 ? 0 : Math.min(0.1, (now - previousFrame) / 1_000_000_000.0);
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
