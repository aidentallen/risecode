package dev.riserecode.render.glass;

import dev.riserecode.Client;
import dev.riserecode.module.modules.GlassModule;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EquipmentSlot;
import dev.riserecode.render.glass.GlassPolicy.*;

public final class GlassCapture {
    public record Owner(UUID id, boolean self, GlassPolicy policy, View view) { }
    public record Surface(Owner owner, Component component, boolean mainHand, Slot slot, GlassMaterial material) { }
    public interface State { Owner risecode$glassOwner(); void risecode$glassOwner(Owner owner); }
    public interface ItemState { Surface risecode$glassSurface(); }
    public static final ThreadLocal<Surface> CURRENT=new ThreadLocal<>();
    private GlassCapture() { }
    public static GlassModule module(){return Client.INSTANCE.isInitialized()?Client.INSTANCE.getModuleManager().getByClass(GlassModule.class).orElse(null):null;}
    public static Owner owner(UUID id,boolean self,boolean preview){
        var module=module();if(module==null||!preview&&!module.isEnabled()||!preview&&dev.riserecode.render.ShaderWorldOverlay.shadowPass())return null;
        var policy=preview?module.capture():Client.INSTANCE.getEventBus().post(new dev.riserecode.event.GlassExtractEvent()).policy();
        return policy==null?null:new Owner(id,self,policy,preview?View.PREVIEW:View.WORLD);
    }
    public static Owner local(){var mc=Minecraft.getInstance();var p=mc.player;var owner=p==null?null:owner(p.getUUID(),true,false);return owner==null?null:new Owner(owner.id(),true,owner.policy(),View.HAND);}
    public static Surface surface(Owner owner,Component component,boolean main,Slot slot){
        if(owner==null||owner.view()!=View.PREVIEW&&dev.riserecode.render.ShaderWorldOverlay.shadowPass())return null;
        var material=owner.policy().select(owner.self(),component,owner.view(),main,slot);
        return material==null?null:new Surface(owner,component,main,slot,material);
    }
    public static Surface surface(Object state,Component component,boolean main,Slot slot){return state instanceof State s?surface(s.risecode$glassOwner(),component,main,slot):null;}
    public static Slot slot(EquipmentSlot slot){return switch(slot){case HEAD->Slot.HEAD;case CHEST->Slot.CHEST;case LEGS->Slot.LEGS;case FEET->Slot.FEET;default->Slot.NONE;};}
    public static Surface held(AvatarRenderState state,HumanoidArm arm){return surface(state,Component.ITEM,state.mainArm==arm,Slot.NONE);}
    public static void with(Surface surface,Runnable action){var before=CURRENT.get();if(surface==null)CURRENT.remove();else CURRENT.set(surface);try{action.run();}finally{if(before==null)CURRENT.remove();else CURRENT.set(before);}}
}
