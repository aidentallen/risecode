package dev.riserecode.testmixin;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Set;
@Mixin(ClientPacketListener.class)
public interface TabStateAccessor {
    @Accessor("listedPlayers") Set<PlayerInfo> risecode$listed();
}
