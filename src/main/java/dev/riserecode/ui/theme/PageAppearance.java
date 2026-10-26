package dev.riserecode.ui.theme;

import com.google.gson.JsonObject;
import dev.riserecode.setting.*;
import java.util.*;

/** Independent screen overrides. Global settings remain the source when inheritance is enabled. */
public final class PageAppearance {
    public enum ThemeMode { INHERIT, ENABLED, DISABLED }
    public enum ScreenStyle { INHERIT, MINECRAFT, RISECODE }
    public final EnumSetting<ScreenStyle> style=new EnumSetting<>("Screen Style","Inherit the default, Minecraft or risecode",ScreenStyle.INHERIT);
    public final EnumSetting<ThemeMode> background=new EnumSetting<>("Page Background","Custom background on this page",ThemeMode.INHERIT);
    public final EnumSetting<ThemeMode> theme = new EnumSetting<>("Page Theme","Custom styling on this page",ThemeMode.INHERIT);
    public final BooleanSetting inherit = new BooleanSetting("Inherit Background","Follow the shared background settings",true);
    public final BooleanSetting inheritParticles = new BooleanSetting("Inherit Particles","Follow the shared particle settings independently",true);
    private final Map<String,Setting<?>> values = new LinkedHashMap<>();
    private final List<ThemeManager.Control> controls = new ArrayList<>();
    private static String context="";
    private static int editorDepth;
    public static void context(String id){context=id;}
    public static String context(){return context;}
    public static boolean plain(){
        if(editing()||!(context.startsWith("risecode:")||context.startsWith("dev.riserecode."))||Theme.manager()==null){return false;}
        var page=Theme.manager().pages().get(context);return page!=null&&!page.enabled(true);
    }
    public static boolean editing(){return editorDepth>0;}
    public static void editor(Runnable work){editorDepth++;try{work.run();}finally{editorDepth--;}}
    public PageAppearance(ThemeManager manager){
        controls.add(new ThemeManager.Control("This Page",style,0));controls.add(new ThemeManager.Control("This Page",inherit,0));controls.add(new ThemeManager.Control("This Page",background,0));
        controls.add(new ThemeManager.Control("Particles",inheritParticles,0));
        for(var c:manager.getControls()){
            String n=c.setting().getName();
            if(!isBackground(c.group(),n)){continue;}
            boolean particles=isParticle(n);
            Setting<?> copy=copy(c.setting());copy.setVisibleWhen(()->!(particles?inheritParticles:inherit).getValue());copy.onChange(manager::changed);
            values.put(n,copy);controls.add(new ThemeManager.Control(c.group(),copy,c.step()));
        }
        background.onChange(manager::changed);theme.onChange(manager::changed);style.onChange(()->{theme.setValue(ThemeMode.INHERIT);manager.changed();});inherit.onChange(()->{
            if(!inherit.getValue()){values.forEach((n,s)->{if(!isParticle(n)){ThemeManager.assign(s,manager.getSetting(n) instanceof ColorSetting color?color.paint():manager.getSetting(n).getValue());}});}
            manager.changed();
        });
        inheritParticles.onChange(()->{if(!inheritParticles.getValue()){values.forEach((n,s)->{if(isParticle(n)){ThemeManager.assign(s,manager.getSetting(n) instanceof ColorSetting color?color.paint():manager.getSetting(n).getValue());}});}manager.changed();});
    }
    public static boolean isParticle(String name){return name.equals("Background Particles")||name.startsWith("Particle ");}
    private static boolean isBackground(String group,String name){return group.startsWith("Background")||name.equals("GUI Blur Radius")||name.equals("GUI Blur Opacity")||name.equals("Blur Mode")||name.equals("Background Color");}
    private static <E extends Enum<E>> Setting<E> enumCopy(EnumSetting<E> setting){return new EnumSetting<>(setting.getName(),setting.getDescription(),setting.getValue());}
    private static Setting<?> copy(Setting<?> s){
        if(s instanceof DoubleSetting n){return n.copy();}
        if(s instanceof BooleanSetting b){return new BooleanSetting(s.getName(),s.getDescription(),b.getValue());}
        if(s instanceof StringSetting t){return new StringSetting(s.getName(),s.getDescription(),t.getValue(),t.maximum());}
        if(s instanceof EnumSetting<?> option){return enumCopy(option);}
        if(s instanceof ColorSetting c){ColorSetting result=new ColorSetting(s.getName(),s.getDescription(),c.getValue());result.apply(c.paint());return result;}
        throw new IllegalArgumentException("Unsupported page control "+s.getName());
    }
    public List<ThemeManager.Control> controls(){return List.copyOf(controls);}
    public Setting<?> effective(String name){return (isParticle(name)?inheritParticles:inherit).getValue()?null:values.get(name);}
    public Setting<?> get(String name){return values.get(name);}
    public boolean backgroundEnabled(boolean inherited){return background.getValue()==ThemeMode.INHERIT?inherited:background.getValue()==ThemeMode.ENABLED;}
    public boolean enabled(boolean inherited){return style.getValue()==ScreenStyle.INHERIT?(theme.getValue()==ThemeMode.INHERIT?inherited:theme.getValue()==ThemeMode.ENABLED):style.getValue()==ScreenStyle.RISECODE;}
    public JsonObject json(){JsonObject result=new JsonObject();result.addProperty("Page Theme",theme.getValue().name());for(var c:controls){Setting<?> s=c.setting();result.add(s.getName(),s instanceof ColorSetting color?color.toJson():new com.google.gson.Gson().toJsonTree(s.getValue()));}return result;}
    public void load(JsonObject json){
        if(!json.has("Screen Style")&&json.has("Page Theme")){ThemeManager.assign(theme,ThemeManager.decode(theme,json.get("Page Theme")));style.setValue(switch(theme.getValue()){case INHERIT->ScreenStyle.INHERIT;case ENABLED->ScreenStyle.RISECODE;case DISABLED->ScreenStyle.MINECRAFT;});}
        if(json.has("Page Theme")){ThemeManager.assign(theme,ThemeManager.decode(theme,json.get("Page Theme")));}
        Map<Setting<?>,Object> parsed=new LinkedHashMap<>();for(var c:controls){if(json.has(c.setting().getName())){parsed.put(c.setting(),ThemeManager.decode(c.setting(),json.get(c.setting().getName())));}}
        // Apply inheritance first: its initialization callback must not overwrite loaded overrides.
        if(parsed.containsKey(inherit)){ThemeManager.assign(inherit,parsed.remove(inherit));}parsed.forEach(ThemeManager::assign);
        if(!json.has("Inherit Particles")&&json.has("Inherit Background")){inheritParticles.setValue(inherit.getValue());for(var entry:values.entrySet()){if(isParticle(entry.getKey())&&json.has(entry.getKey())){ThemeManager.assign(entry.getValue(),ThemeManager.decode(entry.getValue(),json.get(entry.getKey())));}}}
    }
}
