package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Local reset demonstration. The modern wire packet contains no fall-distance field. */
public final class NoFallModule extends Module {
    public enum Mode { PACKET, VANILLA }
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Reset locally on tick or movement-packet send", Mode.VANILLA));
    private final Minecraft minecraft;
    public NoFallModule(EventBus bus, Minecraft minecraft) {
        super("NoFall", "Reset local fall distance; server fall damage is unchanged", Category.PLAYER, bus);
        this.minecraft = minecraft;
    }
    public EnumSetting<Mode> getMode() { return mode; }
    @Override
    protected void onEnable() {
        listen(UpdateEvent.class, event -> { if (mode.getValue() == Mode.VANILLA) { reset(); } });
        listen(PacketEvent.Send.class, event -> {
            if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) { return; }
            if (minecraft.isSameThread()) { resetForPacket(); }
            else { minecraft.execute(this::resetForPacket); }
        });
    }
    private void resetForPacket() {
        if (isEnabled() && mode.getValue() == Mode.PACKET) { reset(); }
    }
    private void reset() {
        if (minecraft.player != null && minecraft.player.fallDistance > 0) { minecraft.player.fallDistance = 0; }
    }
}
