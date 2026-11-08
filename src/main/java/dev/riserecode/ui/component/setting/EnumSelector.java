package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Inline cycle selector: left advances, right reverses, with a short value transition. */
public final class EnumSelector<E extends Enum<E>> extends UIComponent {
    private final EnumSetting<E> setting;
    private final Runnable changed;
    private final Animation change = Animation.ui(1, 0.16, MathUtil.Easing.OUT_QUART);
    private E displayed;
    private final Runnable choicePresenter;
    public EnumSelector(EnumSetting<E> setting, Runnable changed) { this(setting,changed,null); }
    public EnumSelector(EnumSetting<E> setting,Runnable changed,Runnable choicePresenter) {
        this.choicePresenter=choicePresenter;
        this.setting = setting; this.changed = changed; displayed = setting.getValue();change.timing(()->dev.riserecode.ui.animation.PopIn.windowDuration("Dropdowns"));
    }
    private String option() {
        if (displayed instanceof dev.riserecode.render.ChunkMotionTracker.Curve curve) {
            return switch (curve) { case OUT_QUART -> "Ease-Out-Quart"; case IN_OUT_CUBIC -> "Ease-In-Out-Cubic"; case OUT_ELASTIC -> "Elastic-Out"; };
        }
        if (displayed instanceof MathUtil.Easing easing) {
            return switch (easing) {
                case LINEAR -> "Linear";
                case OUT_QUART -> "Ease-Out-Quart";
                case IN_OUT_CUBIC -> "Ease-In-Out-Cubic";
                case OUT_ELASTIC -> "Elastic-Out";
                case OUT_BOUNCE -> "Bounce";
            };
        }
        if(displayed instanceof dev.riserecode.scanner.ChunkHeuristics.Mode mode){return switch(mode){case CLASSIC->"Classic";case ANBS_PLUS->"ANBS+";case ACTIVITY_CLUES->"Activity Clues";};}
        if(displayed instanceof dev.riserecode.scanner.ChunkHeuristics.Method method){return switch(method){case BYPASS->"Bypass";case LIGHT->"Light";case ANBS_PLUS_SCAN->"ANBS+ Scan";};}
        return Theme.displayName(displayed.name());
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        keyboard(graphics, opacity, () -> setting.getName() + ", choice, " + setting.getValue()+", "+setting.getDescription(), this::openChoices, this::choose);
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        if (displayed != setting.getValue()) { displayed = setting.getValue(); change.reset(0); change.animateTo(1); }
        double progress = change.update(deltaSeconds);
        String label = Theme.displayName(setting.getName()) + ": ";
        String option = option()+" ▾";
        double scale=.85;
        double top=y+(height-18*Theme.number("Font Scale",1)*scale)/2;
        RenderUtil.text(graphics,font,RenderUtil.ellipsize(font,label,(width-4)/scale),x,top,scale,Theme.opacity(Theme.SECONDARY,opacity));
        RenderUtil.text(graphics,font,RenderUtil.ellipsize(font,option,(width-4)/scale),x,top+9*Theme.number("Font Scale",1)*scale+(1-progress)*2*Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION),scale,Theme.opacity(Theme.TEXT,opacity*(.65+.35*progress)));
        if(contains(mouseX,mouseY)){graphics.setTooltipForNextFrame(font,java.util.List.of(net.minecraft.network.chat.Component.literal(label+option),net.minecraft.network.chat.Component.literal(setting.getDescription())),java.util.Optional.empty(),(int)mouseX,(int)mouseY);}

    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY) || (button != 0 && button != 1)) { return false; }
        if(button==0){openChoices();}else{choose(-1);} return true;
    }
    private void openChoices(){
        if(choicePresenter!=null){choicePresenter.run();return;}
        var mc=net.minecraft.client.Minecraft.getInstance();
        dev.riserecode.ui.component.FocusedInput.commit();
        if(mc.gui.screen() instanceof dev.riserecode.ui.menu.PageAppearanceScreen page){page.suspendForChoice();}
        mc.gui.setScreen(new dev.riserecode.ui.menu.ChoiceScreen<>(mc.gui.screen(),setting.getName(),setting.getOptions(),setting.getValue(),value->{setting.setValue(value);changed.run();dev.riserecode.ui.sound.UiSounds.play();}));
    }
    private void choose(int direction) {
        var options = setting.getOptions();
        setting.setValue(options.get(Math.floorMod(options.indexOf(setting.getValue()) + direction, options.size())));
        dev.riserecode.ui.sound.UiSounds.play(); changed.run();
    }
}
