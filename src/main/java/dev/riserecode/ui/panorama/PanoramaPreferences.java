package dev.riserecode.ui.panorama;

import dev.riserecode.setting.*;
import java.util.List;
import java.util.function.BiConsumer;

/** Global title-scene preferences. Presets touch only environmental values. */
public final class PanoramaPreferences {
    public enum Atmosphere { MISTY_DAWN, SUNLIT_DAY, GOLDEN_HOUR, RAINFALL, FIREFLY_DUSK, AURORA_NIGHT, CUSTOM }
    public enum Quality { LOW, BALANCED, CINEMATIC }
    public enum Refresh { FPS_30, FPS_60, FPS_120, UNCAPPED; public int fps(){return switch(this){case FPS_30->30;case FPS_60->60;case FPS_120->120;case UNCAPPED->0;};} }
    public final BooleanSetting enabled=new BooleanSetting("Living Panorama","A living forest and lake on the start screen",false);
    public final EnumSetting<Atmosphere> atmosphere=new EnumSetting<>("Panorama Atmosphere","Blend environments over three seconds",Atmosphere.MISTY_DAWN);
    public final DoubleSetting trees=number("Tree Density",.65,0,1), plants=number("Undergrowth Density",.65,0,1);
    public final DoubleSetting wind=number("Panorama Wind",.3,0,1), time=number("Panorama Time of Day",6,0,24), weather=number("Panorama Weather Strength",.12,0,1), fog=number("Panorama Fog",.55,0,1);
    public final DoubleSetting interaction=number("Panorama Interaction Strength",.7,0,1), sensitivity=number("Panorama Camera Sensitivity",1,.1,3), volume=number("Panorama Volume",.15,0,1);
    public final BooleanSetting cycle=new BooleanSetting("Panorama Day Night Cycle","A twenty minute environmental cycle",false), mute=new BooleanSetting("Mute Panorama","Mute environmental audio independently of menu music",false);
    public final EnumSetting<Quality> quality=new EnumSetting<>("Panorama Quality","Low, Balanced or Cinematic reflections",Quality.BALANCED);
    public final EnumSetting<Refresh> refresh=new EnumSetting<>("Panorama Refresh","Scene updates; interface remains native resolution",Refresh.FPS_60);
    private boolean applying;
    public PanoramaPreferences(BiConsumer<Setting<?>,Double> add){
        for(Setting<?> s:List.of(enabled,atmosphere,trees,plants,wind,time,weather,fog,interaction,sensitivity,quality,refresh,cycle,volume,mute))add.accept(s,s==time?.25:s instanceof DoubleSetting?.01:0);
        atmosphere.onChange(()->{if(!applying&&atmosphere.getValue()!=Atmosphere.CUSTOM)applyPreset(atmosphere.getValue());});
        for(Setting<?> s:List.of(wind,time,weather,fog))s.onChange(()->{if(!applying)atmosphere.setValue(Atmosphere.CUSTOM);});
    }
    private static DoubleSetting number(String name,double value,double min,double max){return new DoubleSetting(name,name,value,min,max);}
    public void transaction(Runnable action){boolean previous=applying;applying=true;try{action.run();}finally{applying=previous;}}
    public void applyPreset(Atmosphere preset){
        if(preset==Atmosphere.CUSTOM)return;
        double[] values=switch(preset){case MISTY_DAWN->new double[]{6,.3,.12,.55};case SUNLIT_DAY->new double[]{12,.35,0,.12};case GOLDEN_HOUR->new double[]{17.7,.25,0,.24};case RAINFALL->new double[]{13,.7,1,.65};case FIREFLY_DUSK->new double[]{19.5,.18,0,.32};case AURORA_NIGHT->new double[]{0,.2,0,.2};case CUSTOM->throw new AssertionError();};
        transaction(()->{time.setValue(values[0]);wind.setValue(values[1]);weather.setValue(values[2]);fog.setValue(values[3]);atmosphere.setValue(preset);});
    }
    public String label(){return dev.riserecode.ui.theme.Theme.displayName(atmosphere.getValue().name());}
    public void next(){var a=atmosphere.getValue();applyPreset(Atmosphere.values()[(a.ordinal()+1)%6]);}
}
