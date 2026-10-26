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
import dev.riserecode.ui.component.TextField;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

/** Input semantics without a graphics driver; real rendering is covered by client gametests. */
public final class UiChecks {
    private enum Choice { FIRST, SECOND, THIRD }
    private static int assertions;
    private UiChecks() { }

    public static void main(String[] args) {
        assertions += dev.riserecode.ui.render.UiMeshScissorChecks.run();
        assertions += BlurRegionChecks.run();
        assertions += dev.riserecode.ui.render.OrderedBatchChecks.run();
        for (double width : new double[] {213, 305, 753}) {
            for (double height : new double[] {22, 28}) {
                var header = dev.riserecode.ui.component.HeaderLayout.at(115, 8, width, height, 96);
                check(header.gear().centerY() == header.action().centerY() && header.label().centerY() == header.gear().centerY(), "header controls share a vertical center");
                check(header.label().x() + header.label().width() < header.action().x() && header.action().x() + header.action().width() < header.gear().x(), "header label and actions cannot overlap");
                check(header.gear().x() + header.gear().width() == 115 + width, "gear aligns with content right edge");
            }
        }
        var searchHeader = dev.riserecode.ui.component.HeaderLayout.at(115, 8, 305, 22, 0);
        check(searchHeader.action() == null && searchHeader.label().x() + searchHeader.label().width() < searchHeader.gear().x(), "search field leaves room for the gear");
        check(Theme.lerp(0x00000000, 0xFFFFFFFF, 0.5) == 0x80808080, "all channels interpolate");
        check(Theme.lerp(Theme.BLUE, Theme.FUCHSIA, -1) == Theme.BLUE, "negative color progress clamps");
        check(Theme.lerp(Theme.BLUE, Theme.FUCHSIA, 2) == Theme.FUCHSIA, "overshoot color progress clamps");
        check(Theme.opacity(Theme.PANEL, 0.5) == 0x80151D20, "panel alpha is multiplied");
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
        check(Theme.displayName("KillAura").equals("Kill Aura"), "camel case display name");
        check(Theme.displayName("MinCPS").equals("Min CPS"), "acronym spacing");
        TextField input = new TextField(() -> "Search");
        input.setFocused(true);
        input.charTyped(new CharacterEvent(0x1F680));
        input.charTyped(new CharacterEvent('a'));
        check(input.getValue().equals("🚀a"), "Unicode input");
        input.keyPressed(new KeyEvent(263, 0, 0), () -> "", text -> { });
        input.keyPressed(new KeyEvent(259, 0, 0), () -> "", text -> { });
        check(input.getValue().equals("a"), "backspace removes one code point");
        input.keyPressed(new KeyEvent(65, 0, 2), () -> "", text -> { });
        input.keyPressed(new KeyEvent(86, 0, 2), () -> "Sprint\n", text -> { });
        check(input.getValue().equals("Sprint"), "paste replaces selection and strips controls");
        input.setFocused(false);
        check(!input.charTyped(new CharacterEvent('x')), "unfocused input ignores typing");
        System.out.println("UI checks passed (" + assertions + " assertions).");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) { throw new AssertionError(message); }
    }
}
