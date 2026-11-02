package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.ScannerPacketEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ScannerPacketMixin {
    private static void post(int x,int z,Packet<?> packet){if(Client.INSTANCE.isInitialized())Client.INSTANCE.getEventBus().post(new ScannerPacketEvent(x,z,packet));}
    @Inject(method="handleLevelChunkWithLight",at=@At("TAIL"))
    private void chunk(ClientboundLevelChunkWithLightPacket p,CallbackInfo ci){post(p.getX(),p.getZ(),p);}
    @Inject(method="handleLightUpdatePacket",at=@At("TAIL"))
    private void light(ClientboundLightUpdatePacket p,CallbackInfo ci){post(p.getX(),p.getZ(),p);}
    @Inject(method="handleBlockUpdate",at=@At("TAIL"))
    private void block(ClientboundBlockUpdatePacket p,CallbackInfo ci){post(p.getPos().getX()>>4,p.getPos().getZ()>>4,p);}
    @Inject(method="handleChunkBlocksUpdate",at=@At("TAIL"))
    private void section(ClientboundSectionBlocksUpdatePacket p,CallbackInfo ci){var s=((ScannerSectionAccessor)p).riserecode$section();post(s.x(),s.z(),p);}
    @Inject(method="handleForgetLevelChunk",at=@At("TAIL"))
    private void forget(ClientboundForgetLevelChunkPacket p,CallbackInfo ci){post(p.pos().x(),p.pos().z(),p);}
}
