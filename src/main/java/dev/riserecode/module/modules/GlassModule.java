package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.glass.*;
import dev.riserecode.setting.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Opt-in glass. All values use normal settings codecs, including retained disabled overrides. */
public final class GlassModule extends Module {
    public enum Preset { CRYSTAL, CLEAR, FROSTED, VIOLET, CUSTOM }
    public final BooleanSetting selfBody=bool("Your body","Your third-person skin and skin layers",true);
    public final BooleanSetting arms=bool("First-person arms","Native arms and sleeves",true);
    public final BooleanSetting selfArmor=bool("Your armor","Wherever native armor geometry is rendered",true);
    public final BooleanSetting firstItems=bool("First-person items","Held items in first person",true);
    public final BooleanSetting thirdItems=bool("Third-person items","Your held items in third person",true);
    public final BooleanSetting others=bool("Other players","Apply glass to other players",false);
    public final BooleanSetting otherBody=bool("Other body","Other players' skin and skin layers",true);
    public final BooleanSetting otherArmor=bool("Other armor","Other players' armor",true);
    public final BooleanSetting otherItems=bool("Other items","Other players' held items",true);
    public final BooleanSetting mainHand=bool("Main hand","Include the main hand, including left-handed players",true);
    public final BooleanSetting offHand=bool("Offhand","Include the offhand",true);
    public final BooleanSetting head=bool("Helmet","Include head armor",true);
    public final BooleanSetting chest=bool("Chestplate","Include chest armor",true);
    public final BooleanSetting legs=bool("Leggings","Include leg armor",true);
    public final BooleanSetting feet=bool("Boots","Include foot armor",true);
    public final MaterialSettings shared,body,armor,items;
    private final List<Setting<?>> targets;
    public GlassModule(EventBus bus) {
        super("Glass","Crystal surfaces, frosted refraction and luminous edges.",Category.RENDER,bus);
        targets=List.copyOf(getSettings().subList(4,getSettings().size()));
        otherBody.setVisibleWhen(others::getValue);otherArmor.setVisibleWhen(others::getValue);otherItems.setVisibleWhen(others::getValue);
        shared=new MaterialSettings("Shared",false);body=new MaterialSettings("Body",true);armor=new MaterialSettings("Armor",true);items=new MaterialSettings("Items",true);
    }
    private BooleanSetting bool(String name,String description,boolean value){return register(new BooleanSetting(name,description,value));}
    public List<Setting<?>> targets(){return targets;}
    public GlassPolicy capture(){return dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId(),this::captureScoped);}
    private GlassPolicy captureScoped(){
        var s=shared.capture();
        return new GlassPolicy(selfBody.getValue(),arms.getValue(),selfArmor.getValue(),firstItems.getValue(),thirdItems.getValue(),others.getValue(),otherBody.getValue(),otherArmor.getValue(),otherItems.getValue(),mainHand.getValue(),offHand.getValue(),head.getValue(),chest.getValue(),legs.getValue(),feet.getValue(),s,body.resolve(s),armor.resolve(s),items.resolve(s));
    }
    public String status(){return GlassRenderer.status();}
    @Override protected void onEnable(){GlassRenderer.retry();listen(dev.riserecode.event.GlassExtractEvent.class,event->event.policy(capture()));}
    @Override protected void onDisable(){GlassRenderer.closeGameplay();}
    public final class MaterialSettings {
        public final String label;
        public final BooleanSetting override;
        public final EnumSetting<Preset> preset;
        public final ColorSetting color,edges;
        public final DoubleSetting opacity,frosted,refraction,highlights,edgeStrength,edgeThickness,glow,spread,shimmerStrength,shimmerSpeed,originalTexture;
        public final BooleanSetting shimmer,glint;
        private boolean applying;
        private final List<Setting<?>> settings;
        private MaterialSettings(String label,boolean optional){
            this.label=label;int start=getSettings().size();
            override=optional?bool(label+" override","Use a separate material; disabled values are retained",false):null;
            color=register(new ColorSetting(label+" color","Glass tint; supports animated colors",0xFFE4F5FF));
            opacity=num("opacity","Tint opacity",.22,0,1);
            frosted=num("frosted","Scene blur through glass",.35,0,1);
            refraction=num("refraction","Scene distortion",.25,0,1);
            highlights=num("highlights","View-dependent surface gloss",.8,0,1);
            edges=register(new ColorSetting(label+" edge color","Independent edge lighting color",0xFFD9F4FF));
            edgeStrength=num("edge strength","Edge lighting intensity",.85,0,1);
            edgeThickness=num("edge thickness","Edge width in pixels",1.5,.25,12);
            glow=num("glow","Bloom intensity",.8,0,1);
            spread=num("glow spread","Bloom radius in pixels",10,0,32);
            shimmer=bool(label+" shimmer","Animated surface shimmer",true);
            shimmerStrength=num("shimmer strength","Shimmer brightness",.35,0,1);
            shimmerSpeed=num("shimmer speed","Shimmer animation speed",1,0,5);
            originalTexture=num("original texture","Blend skin, equipment, trim and item details",.12,0,1);
            glint=bool(label+" enchantment glint","Keep glint; EnchantEffect styling remains available",true);
            preset=register(new EnumSetting<>(label+" preset","Material preset; target selections are preserved",Preset.CRYSTAL));
            settings=List.copyOf(getSettings().subList(start,getSettings().size()));
            BooleanSupplier visible=()->override==null||override.getValue();
            for(var setting:settings)if(setting!=override)setting.setVisibleWhen(visible);
            for(var setting:settings)if(setting!=override&&setting!=preset)setting.onChange(()->{if(!applying)preset.setValue(Preset.CUSTOM);});
            // Presets are applied explicitly by the UI. Loading serialized settings must not overwrite
            // later values or depend on JSON order; the enum is persisted metadata, not a command.
        }
        private DoubleSetting num(String suffix,String help,double v,double min,double max){return register(new DoubleSetting(label+" "+suffix,help,v,min,max));}
        public List<Setting<?>> settings(){return settings;}
        public void applyPreset(Preset p){
            if(p==Preset.CUSTOM){preset.setValue(p);return;}
            applying=true;
            try{
                for(var s:settings)if(s!=override&&s!=preset)s.reset();
                switch(p){
                    case CLEAR -> {opacity.setValue(.08);frosted.setValue(0.0);refraction.setValue(.12);glow.setValue(.3);originalTexture.setValue(0.0);}
                    case FROSTED -> {opacity.setValue(.3);frosted.setValue(.85);refraction.setValue(.15);glow.setValue(.55);}
                    case VIOLET -> {color.setValue(0xFFA88AFF);edges.setValue(0xFFE9CAFF);opacity.setValue(.3);refraction.setValue(.4);}
                    default -> { }
                }
                preset.setValue(p);
            }finally{applying=false;}
        }
        public GlassMaterial resolve(GlassMaterial fallback){return override!=null&&!override.getValue()?fallback:capture();}
        public GlassMaterial capture(){return new GlassMaterial(dev.riserecode.ui.theme.ModuleAppearance.capture(color.paint()),opacity.getValue(),frosted.getValue(),refraction.getValue(),highlights.getValue(),dev.riserecode.ui.theme.ModuleAppearance.capture(edges.paint()),edgeStrength.getValue(),edgeThickness.getValue(),glow.getValue(),spread.getValue(),shimmer.getValue(),shimmerStrength.getValue(),shimmerSpeed.getValue(),originalTexture.getValue(),glint.getValue());}
    }
}
