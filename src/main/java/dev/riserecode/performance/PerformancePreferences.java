package dev.riserecode.performance;

import com.google.gson.JsonObject;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.setting.*;
import dev.riserecode.ui.theme.ThemeManager;
import java.util.*;
import java.util.function.Consumer;

/** Global preferences. Presets never mutate theme, profile, module or cosmetic selections. */
public final class PerformancePreferences {
    public enum Upscaler { BILINEAR, FSR1 }
    public final DoubleSetting worldResolution=new DoubleSetting("World Resolution", "World rendering percentage; GUI stays native",100,25,100);
    public final EnumSetting<Upscaler> upscaler=new EnumSetting<>("Upscaler", "Spatial world upscaling",Upscaler.FSR1);
    public final DoubleSetting sharpness=new DoubleSetting("FSR1 Sharpness", "0 is unsharpened; 1 is maximum RCAS",.2,0,1);
    private List<Setting<?>> rendering(){return List.of(worldResolution,upscaler,sharpness);}
    public enum Mode { OFF, CONSERVATIVE, BALANCED, STRONG, CUSTOM }
    public final EnumSetting<Mode> mode=new EnumSetting<>("Performance Mode","Off preserves your selected effects",Mode.OFF);
    private final List<Setting<?>> options=List.of(
        new BooleanSetting("Reduce GUI Blur","Suppress GUI and menu world blur",false),
        new BooleanSetting("Reduce GUI Glow","Suppress interface glow",false),
        new BooleanSetting("Reduce Soft Shadows","Use crisp interface shadows",false),
        new BooleanSetting("Reduce Menu Particles","Suppress background particles",false),
        new BooleanSetting("Reduce Cosmetic Detail","Fewer procedural ring and heart segments",false),
        new EnumSetting<>("Cosmetic Particle Density","Maximum fraction of selected density",PerformancePolicy.Density.FULL),
        new BooleanSetting("Suppress Cosmetic Trails","Suppress selected trails",false),
        new BooleanSetting("Reduce Background Work","Slower map capture and bounded completions",false),
        new BooleanSetting("Reduce Map CPU Cache","256 tiles / 32 MiB",false),
        new BooleanSetting("Reduce Map GPU Cache","128 textures; pending draws remain pinned",false),
        new BooleanSetting("Reduce Resource Previews","24 cached / 8 pending",false),
        new BooleanSetting("Reduce Cosmetic Cache","8 unused assets / 32 MiB plus active working set",false));
    private PerformancePolicy custom=PerformancePolicy.OFF;
    private final Consumer<JsonObject> save;
    private boolean syncing;
    public PerformancePreferences(ConfigManager config){this(read(config),json->{var ui=config.getUiPreferences();ui.add("performance",json);config.setUiPreferences(ui);});}
    private static JsonObject read(ConfigManager config){var ui=config.getUiPreferences();return ui.has("performance")&&ui.get("performance").isJsonObject()?ui.getAsJsonObject("performance"):new JsonObject();}
    public PerformancePreferences(JsonObject data,Consumer<JsonObject> save){
        this.save=save;syncing=true;
        upscaler.setVisibleWhen(()->worldResolution.getValue()<100);sharpness.setVisibleWhen(()->worldResolution.getValue()<100&&upscaler.getValue()==Upscaler.FSR1);
        try{
            if(data.has("rendering")&&data.get("rendering").isJsonObject()){
                var values=data.getAsJsonObject("rendering");for(var setting:rendering())if(values.has(setting.getName()))try{ThemeManager.assign(setting,ThemeManager.decode(setting,values.get(setting.getName())));}catch(RuntimeException ignored){}
            }
            if(data.has("custom")&&data.get("custom").isJsonObject()){
                var values=data.getAsJsonObject("custom");for(var s:options){if(values.has(s.getName())){try{ThemeManager.assign(s,ThemeManager.decode(s,values.get(s.getName())));}catch(RuntimeException ignored){}}}
                custom=readOptions();
            }
            if(data.has("mode")){try{mode.setValue(Mode.valueOf(data.get("mode").getAsString()));}catch(RuntimeException ignored){}}
        }finally{syncing=false;}
        worldResolution.setValue((double)Math.round(worldResolution.getValue()));
        for(var setting:rendering())setting.onChange(()->{
            if(!syncing){
                syncing=true;try{worldResolution.setValue((double)Math.round(worldResolution.getValue()));}finally{syncing=false;}
                save.accept(snapshot());
            }
        });
        mode.onChange(()->{if(!syncing){sync(effective());publish();}});
        for(var setting:options){setting.onChange(()->edit(setting));}
        sync(effective());PerformancePolicy.publish(effective());
    }
    public List<ThemeManager.Control> controls(){var result=new ArrayList<ThemeManager.Control>();result.add(new ThemeManager.Control("Performance",mode,0));for(var s:rendering())result.add(new ThemeManager.Control("World Rendering",s,s==worldResolution?1:s==sharpness?.01:0));for(var s:options){result.add(new ThemeManager.Control("Individual Controls",s,0));}return List.copyOf(result);}
    public List<Setting<?>> options(){return options;}
    public PerformancePolicy custom(){return custom;}
    public PerformancePolicy effective(){return preset(mode.getValue(),custom);}
    public static PerformancePolicy preset(Mode mode,PerformancePolicy custom){return switch(mode){
        case OFF->PerformancePolicy.OFF;case CUSTOM->custom;
        case CONSERVATIVE,BALANCED,STRONG->new PerformancePolicy(true,true,true,true,mode!=Mode.CONSERVATIVE,
            mode==Mode.STRONG?PerformancePolicy.Density.OFF:mode==Mode.BALANCED?PerformancePolicy.Density.HALF:PerformancePolicy.Density.FULL,
            mode==Mode.STRONG,true,true,true,true,true);
    };}
    private void edit(Setting<?> edited){
        if(syncing){return;}Object value=edited.getValue();
        // The widgets show the effective preset. Copy all of it before applying this edit.
        syncing=true;try{sync(effective());ThemeManager.assign(edited,value);custom=readOptions();mode.setValue(Mode.CUSTOM);}finally{syncing=false;}publish();
    }
    private PerformancePolicy readOptions(){return new PerformancePolicy(b(0),b(1),b(2),b(3),b(4),(PerformancePolicy.Density)options.get(5).getValue(),b(6),b(7),b(8),b(9),b(10),b(11));}
    private boolean b(int index){return (Boolean)options.get(index).getValue();}
    private void sync(PerformancePolicy p){boolean old=syncing;syncing=true;try{Object[] values={p.blur(),p.glow(),p.softShadows(),p.menuParticles(),p.reducedDetail(),p.particleDensity(),p.suppressTrails(),p.backgroundWork(),p.mapCpu(),p.mapGpu(),p.previews(),p.cosmeticCache()};for(int i=0;i<values.length;i++){ThemeManager.assign(options.get(i),values[i]);}}finally{syncing=old;}}
    private void publish(){PerformancePolicy.publish(effective());save.accept(snapshot());}
    public JsonObject snapshot(){var data=new JsonObject();data.addProperty("mode",mode.getValue().name());var values=new JsonObject();var p=custom;Object[] v={p.blur(),p.glow(),p.softShadows(),p.menuParticles(),p.reducedDetail(),p.particleDensity().name(),p.suppressTrails(),p.backgroundWork(),p.mapCpu(),p.mapGpu(),p.previews(),p.cosmeticCache()};for(int i=0;i<v.length;i++){values.add(options.get(i).getName(),new com.google.gson.Gson().toJsonTree(v[i]));}data.add("custom",values);var rendering=new JsonObject();for(var setting:rendering())rendering.add(setting.getName(),new com.google.gson.Gson().toJsonTree(setting.getValue()));data.add("rendering",rendering);return data;}
}
