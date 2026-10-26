package dev.riserecode.ui.theme;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.riserecode.setting.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Per-module presentation, owned by appearance profiles rather than gameplay profiles. */
public final class ModuleAppearance {
    private static String context = "";
    public static String context() { return context; }
    public static void with(String id, Runnable work) { String old=context;context=id;try{work.run();}finally{context=old;} }
    public static <T> T call(String id, Supplier<T> work) { String old=context;context=id;try{return work.get();}finally{context=old;} }
    public static String id(dev.riserecode.module.Module module){return module==null?"":module.capabilityId();}
    private static final Set<String> GLOBAL_ONLY = Set.of("Container Style","Window Width","Window Height","Interface Scale","Sidebar Color","Selected Tab Color","Display Name","Interface Tagline","World Blur","World Blur Radius","World Map Background","World Map Image","Uncapped Themed Menus");
    public static boolean applicable(ThemeManager.Control c) {
        return !GLOBAL_ONLY.contains(c.setting().getName()) && (!c.group().equals("Menu Screens")||c.setting().getName().equals("Background Darkness")) && !c.group().startsWith("Window Animation:")
                && !c.group().equals("Animation: Navigation");
    }
    public static ColorSetting.Paint capture(ColorSetting.Paint paint) {
        var theme=Theme.manager();if(paint.mode()!=ColorSetting.Mode.THEME||theme==null){return paint;}
        var mode=theme.choice("Gradient Mode",dev.riserecode.util.ColorUtil.GradientMode.class);
        var primary=theme.colorSetting("Primary Accent").paint();
        if(mode==dev.riserecode.util.ColorUtil.GradientMode.STATIC&&primary.mode()!=ColorSetting.Mode.THEME){return primary.opacity((paint.primary()>>>24)/255.0);}
        ColorSetting.Mode actual=switch(mode){case STATIC->ColorSetting.Mode.SOLID;case RAINBOW->ColorSetting.Mode.RAINBOW;case ASTOLFO->ColorSetting.Mode.ASTOLFO;case SINE->ColorSetting.Mode.SINE;};
        return new ColorSetting.Paint(theme.color("Primary Accent"),theme.color("Secondary Accent"),actual,theme.choice("Gradient Direction",dev.riserecode.util.ColorUtil.GradientDirection.class),theme.number("Gradient Speed"),theme.number("Gradient Spacing")).opacity((paint.primary()>>>24)/255.0);
    }
    private dev.riserecode.module.Module module;
    public void bind(dev.riserecode.module.Module value) { module=value;supportedValues.clear();for(var control:controls){if(supported(control)){supportedValues.put(control.setting().getName(),control.setting());}} }
    public boolean hasControls() { return controls.stream().anyMatch(c->c.setting()!=global&&supported(c)&&visible(c.setting().getName())); }
    private boolean supported(ThemeManager.Control control){return ModulePresentation.supports(module,control);}
    private boolean visible(String name){return ModulePresentation.active(module,name)&&modeVisible(name);}
    private Object local(String name){var setting=values.get(name);return setting==null?null:setting.getValue();}
    private boolean modeVisible(String name){
        if(name.equals("Shadow Offset X")||name.equals("Shadow Offset Y")||name.equals("Shadow Opacity")||name.equals("Text Shadow Color"))return !Boolean.FALSE.equals(local("Text Shadow"));
        if(name.equals("Glow Radius")||name.equals("Glow Intensity"))return !Boolean.FALSE.equals(local("Global Glow"));
        if(module!=null&&module.getName().equals("Minimap")){
            Object background=local("Minimap Background");
            if(name.equals("Map Solid Color"))return background==ThemeManager.MapBackground.SOLID;
            if(name.equals("Minimap Image"))return background==ThemeManager.MapBackground.IMAGE;
            if(name.equals("Background"))return background==ThemeManager.MapBackground.INHERIT;
            if(Set.of("Background File","Background Tint","Background Opacity","Background Color","Gradient Start","Gradient End","Background Motion Speed").contains(name)){
                if(background!=ThemeManager.MapBackground.INHERIT&&background!=ThemeManager.MapBackground.IMAGE)return false;
                Object image=local(background==ThemeManager.MapBackground.IMAGE?"Minimap Image":"Background");
                if(name.equals("Background File"))return image==ThemeManager.Background.LOCAL_IMAGE;
                if(name.equals("Background Color"))return image==ThemeManager.Background.SOLID;
                if(name.startsWith("Gradient "))return image==ThemeManager.Background.GRADIENT;
                if(name.equals("Background Motion Speed"))return image==ThemeManager.Background.ANIMATED_GRADIENT||image==ThemeManager.Background.AURORA||image==ThemeManager.Background.STARFIELD;
                return image!=ThemeManager.Background.SOLID&&image!=ThemeManager.Background.GRADIENT&&image!=ThemeManager.Background.NONE;
            }
        }
        if(module!=null&&ModulePresentation.textHud(module)){
            Object image=local("Background");
            if(name.equals("Background File"))return image==ThemeManager.Background.LOCAL_IMAGE;
            if(name.equals("Gradient Start")||name.equals("Gradient End"))return image==ThemeManager.Background.GRADIENT;
            if(name.equals("Image Fit")||name.equals("Image Motion")||name.equals("GUI Blur Radius")||name.equals("GUI Blur Opacity"))return image==ThemeManager.Background.LOCAL_IMAGE||image==ThemeManager.Background.LAKE||image==ThemeManager.Background.SCENIC||image==ThemeManager.Background.SPACE||image==ThemeManager.Background.ABSTRACT;
            if(name.startsWith("Particle "))return local("Background Particles")!=ThemeManager.Particles.NONE;
        }
        return true;
    }
    public final BooleanSetting global = new BooleanSetting("Use Global Appearance","Follow the shared appearance settings",true);
    private final Map<String,Setting<?>> supportedValues=new LinkedHashMap<>();
    private final Map<String,Setting<?>> values=new LinkedHashMap<>();
    private final List<ThemeManager.Control> controls=new ArrayList<>();
    private final ThemeManager manager;
    private boolean initialized, loading, legacyMigrated;
    public ModuleAppearance(ThemeManager manager){
        this.manager=manager;
        controls.add(new ThemeManager.Control("Appearance",global,0));
        for(var control:manager.getControls()){
            if(!applicable(control)){continue;}
            Setting<?> copy=copy(control.setting());copy.setVisibleWhen(()->!global.getValue()&&visible(copy.getName()));copy.onChange(this::changed);
            if(copy.getName().equals("Appearance Preset")){copy.onChange(()->{if(!loading){preset((ThemeManager.Preset)copy.getValue());}});}
            values.put(copy.getName(),copy);controls.add(new ThemeManager.Control(control.group(),copy,control.step()));
        }
        global.onChange(()->{if(!global.getValue()&&!initialized){seed();}changed();});
    }
    private void changed(){if(!loading){manager.changed();}}
    private void seed(){
        initialized=true;boolean previous=loading;loading=true;
        try{values.forEach((name,setting)->{var shared=manager.getSetting(name);ThemeManager.assign(setting,shared instanceof ColorSetting color?color.paint():shared.getValue());});}finally{loading=previous;}
    }
    public void resetLocal(){seed();changed();}
    private void preset(ThemeManager.Preset preset){
        int[] colors=ThemeManager.palette(preset);
        for(String name:List.of("Primary Accent","Secondary Accent","Background Color","Surface Color","Hover Color")){
            int color=switch(name){case "Primary Accent"->colors[0];case "Secondary Accent"->colors[1];case "Background Color"->colors[2];case "Surface Color"->Theme.lerp(colors[2],0xFF000000,.32);default->Theme.lerp(colors[2],colors[0],.12);};
            ThemeManager.assign(get(name),new ColorSetting.Paint(color,colors[1],ColorSetting.Mode.SOLID,dev.riserecode.util.ColorUtil.GradientDirection.HORIZONTAL,.18,100));
        }
        ThemeManager.assign(get("Accent Gradient"),true);ThemeManager.assign(get("Global Glow"),true);
        ThemeManager.assign(get("Gradient Mode"),preset==ThemeManager.Preset.AURORA?dev.riserecode.util.ColorUtil.GradientMode.SINE:dev.riserecode.util.ColorUtil.GradientMode.STATIC);
        ThemeManager.assign(get("Glow Radius"),preset==ThemeManager.Preset.MONOCHROME?3.0:5.0);ThemeManager.assign(get("Glow Intensity"),preset==ThemeManager.Preset.MONOCHROME?.12:.2);
        ThemeManager.assign(get("Background Darkness"),preset==ThemeManager.Preset.EMBER?.48:.42);changed();
    }
    public List<ThemeManager.Control> controls(){return controls.stream().filter(c->c.setting()==global||supported(c)).toList();}
    public Setting<?> get(String name){return values.get(name);}
    public Setting<?> effective(String name){return global.getValue()?null:supportedValues.get(name);}
    private static <E extends Enum<E>> Setting<E> copyEnum(EnumSetting<E> s){return new EnumSetting<>(s.getName(),s.getDescription(),s.getValue());}
    private static Setting<?> copy(Setting<?> s){
        if(s instanceof ColorSetting color){var result=new ColorSetting(s.getName(),s.getDescription(),color.getValue());result.apply(color.paint());return result;}
        if(s instanceof DoubleSetting n){return n.copy();}
        if(s instanceof BooleanSetting b){return new BooleanSetting(s.getName(),s.getDescription(),b.getValue());}
        if(s instanceof StringSetting text){return new StringSetting(s.getName(),s.getDescription(),text.getValue(),text.maximum());}
        if(s instanceof EnumSetting<?> option){return copyEnum(option);}
        throw new IllegalArgumentException("Unsupported module appearance setting "+s.getName());
    }
    public JsonObject json(){
        var json=new JsonObject();json.addProperty(global.getName(),global.getValue());json.addProperty("Custom Initialized",initialized);json.addProperty("Legacy Animation Migrated",legacyMigrated);
        values.forEach((name,setting)->json.add(name,setting instanceof ColorSetting color?color.toJson():new Gson().toJsonTree(setting.getValue())));return json;
    }
    public void load(JsonObject json){
        Map<Setting<?>,Object> parsed=new LinkedHashMap<>();
        for(var control:controls){var setting=control.setting();if(json.has(setting.getName())){parsed.put(setting,ThemeManager.decode(setting,json.get(setting.getName())));}}
        boolean ready=json.has("Custom Initialized")&&json.get("Custom Initialized").isJsonPrimitive()&&json.getAsJsonPrimitive("Custom Initialized").isBoolean()?json.get("Custom Initialized").getAsBoolean():json.size()>1;
        boolean migrated=json.has("Legacy Animation Migrated")&&(Boolean)ThemeManager.decode(new BooleanSetting("Legacy Animation Migrated","",false),json.get("Legacy Animation Migrated"));
        loading=true;try{legacyMigrated|=migrated;initialized=true;parsed.forEach(ThemeManager::assign);initialized=ready;}finally{loading=false;}
    }
    public void migrate(dev.riserecode.module.Module module){
        if(legacyMigrated){return;}legacyMigrated=true;
        if(initialized||!global.getValue()||(!module.popOverride.getValue()&&module.popIn.getValue())){changed();return;}
        seed();global.setValue(false);
        ThemeManager.assign(get("Opening Duration"),module.popIn.getValue()?(module.popOverride.getValue()?module.popTiming.getValue():manager.number("Opening Duration")):0.0);
        ThemeManager.assign(get("Opening Scale"),module.popOverride.getValue()?module.popStrength.getValue():manager.number("Opening Scale"));
        if(!module.popIn.getValue()){ThemeManager.assign(get("HUD Entrance Duration"),0.0);ThemeManager.assign(get("HUD Exit Duration"),0.0);}
        changed();
    }
}
