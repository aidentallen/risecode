package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.Client;
import dev.riserecode.event.PacketEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.CancellationException;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow private Channel channel;

    /** The deepest send boundary covers every overload and queued sends exactly once. */
    @WrapMethod(method = "sendPacket")
    private void riserecode$send(Packet<?> packet, ChannelFutureListener listener, boolean flush, Operation<Void> original) {
        if (!riserecode$isClient()) { original.call(packet, listener, flush); return; }
        PacketEvent.Send event = Client.INSTANCE.getEventBus().post(new PacketEvent.Send(packet));
        if (!event.isCancelled()) { original.call(event.getPacket(), listener, flush); return; }
        if (listener != null) {
            // Preserve Netty's callback thread and report cancellation instead of leaving callers waiting.
            channel.eventLoop().execute(() -> {
                try { listener.operationComplete(channel.newFailedFuture(new CancellationException("RiseRecode cancelled packet"))); }
                catch (Exception failure) { Client.INSTANCE.getLogger().error("Cancelled packet callback failed", failure); }
            });
        }
    }

    @WrapMethod(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V")
    private void riserecode$receive(ChannelHandlerContext context, Packet<?> packet, Operation<Void> original) {
        if (!riserecode$isClient() || !context.channel().isOpen()) { original.call(context, packet); return; }
        PacketEvent.Receive event = Client.INSTANCE.getEventBus().post(new PacketEvent.Receive(packet));
        if (!event.isCancelled()) { original.call(context, event.getPacket()); }
    }

    @Unique
    private boolean riserecode$isClient() {
        return Client.INSTANCE.isInitialized() && ((Connection) (Object) this).getReceiving() == PacketFlow.CLIENTBOUND;
    }
}
