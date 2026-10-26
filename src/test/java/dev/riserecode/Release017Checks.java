package dev.riserecode;

import dev.riserecode.ui.theme.*;
import dev.riserecode.setting.*;
import dev.riserecode.config.SettingCodecs;
import com.google.gson.*;
import java.nio.file.Files;
import org.slf4j.LoggerFactory;

/** Persistence and validation contracts for page overrides and extended controls. */
public final class Release017Checks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private Release017Checks(){}
    public static void main(String[] args)throws Exception{
        var manager=new ThemeManager(Files.createTempDirectory("risecode-017-check-"),LoggerFactory.getLogger("017-check"));
        var editor=new dev.riserecode.ui.component.ThemeEditor(manager,ignored->{});
        check(editor.getActions().stream().noneMatch(a->a.label().equals("Save")||a.label().equals("Load")),"Appearance has no profile actions");
        check(editor.getAction("Reset Theme")!=null&&editor.getAction("Import Image")!=null&&editor.getAction("Preview Sound")!=null,"appearance tools remain beside their settings");
        var page=manager.page("test.Options");
        check(page.enabled(true)&&!page.enabled(false),"default theme inherits family");
        page.theme.setValue(PageAppearance.ThemeMode.DISABLED);check(!page.enabled(true),"page disabling overrides enabled family");
        page.theme.setValue(PageAppearance.ThemeMode.ENABLED);check(page.enabled(false),"page can override disabled family");
        ((DoubleSetting)manager.getSetting("GUI Blur Radius")).setValue(20.0);
        page.inherit.setValue(false);check(((DoubleSetting)page.get("GUI Blur Radius")).getValue()==20,"detach copies current global background");
        ((DoubleSetting)page.get("GUI Blur Radius")).setValue(60.0);
        PageAppearance.context("test.Options");check(manager.number("GUI Blur Radius")==60,"effective page override");
        PageAppearance.context("test.Other");check(manager.number("GUI Blur Radius")==20,"page override does not leak");PageAppearance.context("");
        var snapshot=manager.snapshot();var target=new ThemeManager(Files.createTempDirectory("risecode-017-roundtrip-"),LoggerFactory.getLogger("017-check"));target.apply(snapshot);
        check(target.page("test.Options").theme.getValue()==PageAppearance.ThemeMode.ENABLED,"profile preserves page theme");
        check(((DoubleSetting)target.page("test.Options").get("GUI Blur Radius")).getValue()==60,"profile preserves page background");
        var invalid=snapshot.settings();invalid.getAsJsonObject("Page Overrides").getAsJsonObject("test.Options").addProperty("GUI Blur Radius",Double.NaN);
        try{target.apply(new ThemeProfile(1,invalid));throw new AssertionError("invalid page accepted");}catch(IllegalArgumentException expected){checks++;}
        check(((DoubleSetting)target.page("test.Options").get("GUI Blur Radius")).getValue()==60,"invalid override cannot partially apply");
        target.resetPage("test.Options");check(!target.pages().containsKey("test.Options"),"page reset removes override");
        for(String key:new String[]{"Opening Duration","Opening Scale","Client Pop Scale","Navigation Movement","Press Strength","Animation Speed"}){
            var number=(DoubleSetting)manager.getSetting(key);number.setValue(9.0);check(number.getValue()==9,"extended control "+key);
        }
        check(manager.choice("Button Style",ThemeManager.ButtonStyle.class)==ThemeManager.ButtonStyle.SUBTLE,"existing profiles default to subtle buttons");
        ThemeManager.assign(manager.getSetting("Button Style"),ThemeManager.ButtonStyle.ACCENT);
        check(manager.saveProfile("accent"),"accent profile saves");
        ThemeManager.assign(manager.getSetting("Button Style"),ThemeManager.ButtonStyle.SUBTLE);
        check(manager.saveProfile("subtle"),"subtle profile saves");
        check(manager.loadProfile("accent")&&manager.choice("Button Style",ThemeManager.ButtonStyle.class)==ThemeManager.ButtonStyle.ACCENT,"accent choice survives profile reload");
        check(manager.loadProfile("subtle")&&manager.choice("Button Style",ThemeManager.ButtonStyle.class)==ThemeManager.ButtonStyle.SUBTLE,"switching profiles restores subtle choice");
        var legacy=manager.snapshot().settings();legacy.remove("Button Style");ThemeManager.assign(manager.getSetting("Button Style"),ThemeManager.ButtonStyle.ACCENT);
        manager.apply(new ThemeProfile(1,legacy));check(manager.choice("Button Style",ThemeManager.ButtonStyle.class)==ThemeManager.ButtonStyle.SUBTLE,"profiles without button style reset to subtle");
        check(ThemeManager.Background.values().length==11,"animated backgrounds registered");check(ThemeManager.ClickSound.values().length==11,"all sound presets registered");
        // Numeric text uses the existing numeric persistence contract.
        DoubleSetting coordinate=new DoubleSetting("Destination X","",0,-30_000_000,30_000_000);SettingCodecs.apply(coordinate,new JsonPrimitive(-29_999_999));check(coordinate.getValue()==-29_999_999,"large negative coordinates round-trip exactly");
        System.out.println("Release 0.17 checks passed ("+checks+" assertions).");
    }
}
