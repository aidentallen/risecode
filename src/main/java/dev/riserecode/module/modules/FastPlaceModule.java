package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.mixin.MinecraftAccessor;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;

/** Shortens the pause between placements while the use button is held. */
public final class FastPlaceModule extends Module {
    private final DoubleSetting delay = register(DoubleSetting.extended("Delay", "Ticks between placements; vanilla is 4", 0, 0, 4));
    private final BooleanSetting blocksOnly = register(new BooleanSetting("Blocks Only", "Only speed up placing blocks, not eating or throwing", true));
    private final Minecraft minecraft;

    public FastPlaceModule(EventBus bus, Minecraft minecraft) {
        super("FastPlace", "Places blocks without the right click delay.", Category.PLAYER, bus);
        this.minecraft = minecraft;
    }

    @Override protected void onEnable() {
        listen(UpdateEvent.class, event -> {
            var player = minecraft.player;
            if (player == null || minecraft.gui.screen() != null) { return; }
            if (blocksOnly.getValue() && !(player.getMainHandItem().getItem() instanceof BlockItem) && !(player.getOffhandItem().getItem() instanceof BlockItem)) { return; }
            var access = (MinecraftAccessor) minecraft;
            int limit = (int) Math.round(delay.getValue());
            if (access.riserecode$rightClickDelay() > limit) { access.riserecode$setRightClickDelay(limit); }
        });
    }
}
