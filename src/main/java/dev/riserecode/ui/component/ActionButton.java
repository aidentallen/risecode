package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

public final class ActionButton extends UIComponent {
    private final dev.riserecode.ui.animation.ButtonFeedback press = new dev.riserecode.ui.animation.ButtonFeedback();
    private final Supplier<String> label;
    private final Runnable action;
    private BooleanSupplier enabled=()->true;
    private boolean clickSound = true;
    private Icon icon;
    private boolean tile;
    private double slotX,slotY,slotWidth,slotHeight;
    public ActionButton tile(){tile=true;return this;}
    @Override public void setBounds(double x,double y,double width,double height){
        slotX=x;slotY=y;slotWidth=width;slotHeight=height;
        var mc=net.minecraft.client.Minecraft.getInstance();fit(mc==null?null:mc.font);
    }
    private void fit(Font font){
        if(tile){super.setBounds(slotX,slotY,slotWidth,slotHeight);return;}
        double w=Math.min(slotWidth,icon==null?ButtonMetrics.width(font,label.get()):ButtonMetrics.ICON_SIZE);
        super.setBounds(slotX+(slotWidth-w)/2,slotY+(slotHeight-ButtonMetrics.HEIGHT)/2,w,ButtonMetrics.HEIGHT);
    }
    public ActionButton icon(Icon value) { icon = value; return this; }
    public String label() { return label.get(); }

    public ActionButton(String label, Runnable action) { this(() -> label, action); }
    public ActionButton(Supplier<String> label, Runnable action) { this.label = label; this.action = action; }
    public ActionButton enabled(BooleanSupplier enabled) { this.enabled=enabled;return this; }
    public ActionButton sound(boolean value) { clickSound = value; return this; }
    public boolean isEnabled() { return enabled.getAsBoolean(); }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        fit(font);boolean active=isEnabled();
        double hovered = updateHover(active?mouseX:-1e6, active?mouseY:-1e6, deltaSeconds);
        if(!active){opacity*=0.5;}
        double compression = press.scale();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + width / 2), (float) (y + height / 2));
        graphics.pose().scale((float) compression, (float) compression);
        graphics.pose().translate((float) (-x - width / 2), (float) (-y - height / 2));
        RenderUtil.drawButton(graphics, x, y, width, height, hovered, opacity);
        if (icon != null) {
            icon.drawCentered(graphics, x, y, width, height, Theme.opacity(Theme.TEXT, opacity));
        } else {
            String text = Theme.label(label.get());
            text=RenderUtil.ellipsize(font,text,Math.max(1,width-12)/ButtonMetrics.textScale(height));
            double scale = ButtonMetrics.textScale(height);
            RenderUtil.text(graphics, font, text, x + (width - RenderUtil.textWidth(font, text, false) * scale) / 2,
                    y + (height - 9 * Theme.number("Font Scale", 1) * scale) / 2, scale,
                    Theme.opacity(Theme.blendPalette(Theme.TEXT, Theme.accent(), hovered * 0.45), opacity));
        }
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled() || button != 0 || !contains(mouseX, mouseY)) { return false; }
        press.press(); if (clickSound) { dev.riserecode.ui.sound.UiSounds.play(); } action.run(); return true;
    }
    @Override public boolean mouseReleased(double mx,double my,int button){if(button!=0){return false;}boolean was=press.isHeld();press.release();return was;}
}
