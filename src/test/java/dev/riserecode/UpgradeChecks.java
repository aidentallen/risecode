package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.StringSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.FocusedInput;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.slf4j.LoggerFactory;

public final class UpgradeChecks {
    private static int checks;
    private UpgradeChecks() { }
    private static final class Fixture extends Module {
        final StringSetting text = register(new StringSetting("Text", "", "old", 64));
        Fixture(EventBus bus) { super("Fixture", "", Category.UTILITY, bus); }
    }
    private static void check(boolean condition,String label) { checks++; if(!condition){throw new AssertionError(label);} }
    private static void key(int key,int mods) { check(FocusedInput.key(new KeyEvent(key,0,mods),()->"7.25",ignored->{}),"Focused key consumed"); }
    public static void main(String[] args) throws Exception {
        var number=new DoubleSetting("Test","",2,-10,10); var slider=new Slider(number,()->{},0.25);
        slider.setBounds(0,0,120,24); slider.beginEditing();
        FocusedInput.character(new CharacterEvent('.')); check(number.getValue()==2 && !slider.input().validation().isEmpty(),"Incomplete retains valid value");
        FocusedInput.character(new CharacterEvent('5')); check(number.getValue()==0.5 && slider.input().validation().isEmpty(),"Valid input previews");
        key(265,0);check(number.getValue()==0.75,"Up adjusts by step"); key(264,1);check(number.getValue()==-1.75,"Shift down larger step");
        key(256,0);check(number.getValue()==2 && !FocusedInput.active(),"Escape restores original and unfocuses");
        slider.beginEditing();key(86,2);check(number.getValue()==7.25,"Clipboard paste previews");key(257,0);check(number.getValue()==7.25 && !FocusedInput.active(),"Enter commits");
        slider.beginEditing();key(65,2);FocusedInput.character(new CharacterEvent('9'));FocusedInput.character(new CharacterEvent('9'));
        check(number.getValue()==9 && !slider.input().validation().isEmpty(),"Out of range retains last valid");key(257,0);check(number.getValue()==9,"Invalid commit retains valid preview");
        slider.beginEditing();key(70,2);check(FocusedInput.active(),"Ctrl F remains in field");key(344,0);check(FocusedInput.active(),"GUI hotkey isolated");FocusedInput.cancel();
        var text=new StringSetting("Name","","risecode",8);try{text.setValue("too long a name");throw new AssertionError();}catch(IllegalArgumentException expected){check(text.getValue().equals("risecode"),"String bounds atomic");}
        var directory=Files.createTempDirectory("risecode-upgrade-");var logger=LoggerFactory.getLogger("upgrade-checks");
        var manager=new ModuleManager(new EventBus(),logger);var fixture=manager.register(new Fixture(new EventBus()));var config=new ConfigManager(directory,manager,()->true,ignored->{},logger);
        fixture.text.setValue("Unicode Ω");check(config.save(),"String saves");fixture.text.setValue("other");config.load();check(fixture.text.getValue().equals("Unicode Ω"),"String config round trip");
        var profile=config.profileSnapshot();fixture.text.setValue("changed");config.applyProfile(profile);check(fixture.text.getValue().equals("Unicode Ω"),"String profile round trip");
        var theme=new ThemeManager(directory,logger);Theme.bind(theme);Animation.configureChannels(theme);
        var font=theme.getSetting("Font Scale").getValue();var snapshot=theme.snapshot();
        ((StringSetting)theme.getSetting("Display Name")).setValue("custom");
        for(var preset:ThemeManager.Preset.values()) {
            theme.applyPreset(preset);check(theme.color("Primary Accent")==ThemeManager.palette(preset)[0],"Preset palette "+preset);
            check(theme.text("Display Name").equals("custom") && theme.getSetting("Font Scale").getValue().equals(font),"Preset preserves branding and fonts");
            check(theme.undoPreset(),"Preset undo");
        }
        check(theme.snapshot().settings().get("Primary Accent").equals(snapshot.settings().get("Primary Accent")),"Undo restores full color policy");
        check(theme.saveProfile("Upgrade"),"Appearance saves");theme.reset();check(theme.loadProfile("Upgrade") && theme.text("Display Name").equals("custom"),"Branding profile round trip");
        var a=Animation.ui(ThemeManager.Channel.OPENING,0,0.2,MathUtil.Easing.OUT_QUART);var b=Animation.ui(ThemeManager.Channel.HOVER,0,0.2,MathUtil.Easing.OUT_QUART);
        ((DoubleSetting)theme.getSetting("Opening Duration")).setValue(1.0);((DoubleSetting)theme.getSetting("Hover Duration")).setValue(0.1);
        a.animateTo(1);b.animateTo(1);a.update(0.1);b.update(0.1);check(a.getValue()<1 && b.getValue()==1,"Independent concurrent durations");
        double before=a.getValue();a.animateTo(0);check(a.getValue()==before,"Smooth reversal");a.update(0.05);before=a.getValue();
        ((DoubleSetting)theme.getSetting("Opening Duration")).setValue(0.5);a.update(0);check(a.getValue()==before,"Duration update no jump");
        ((dev.riserecode.setting.BooleanSetting)theme.getSetting("Reduced Motion")).setValue(true);a.update(0.01);check(a.getValue()==0,"Reduced motion completes");
        var policy=theme.policySnapshot();check(policy.has("Opening Duration") && policy.has("HUD Exit Easing"),"Channels in profile policy");
        var legacy=new com.google.gson.JsonObject();legacy.addProperty("Easing Curve","IN_OUT_CUBIC");
        theme.apply(new dev.riserecode.ui.theme.ThemeProfile(1,legacy));
        check(theme.channelCurve(ThemeManager.Channel.OPENING)==MathUtil.Easing.IN_OUT_CUBIC && theme.channelCurve(ThemeManager.Channel.HUD_EXIT)==MathUtil.Easing.IN_OUT_CUBIC,"Legacy appearance easing migrates across channels");
        legacy.addProperty("Easing Curve","OUT_QUART");theme.applyPolicy(legacy);
        check(theme.channelCurve(ThemeManager.Channel.OPENING)==MathUtil.Easing.OUT_QUART && theme.channelCurve(ThemeManager.Channel.HUD_EXIT)==MathUtil.Easing.OUT_QUART,"Legacy module-profile animation policy migrates");
        Theme.bind(null);Animation.configureChannels(null);System.out.println("Upgrade checks passed ("+checks+" assertions).");
    }
}
