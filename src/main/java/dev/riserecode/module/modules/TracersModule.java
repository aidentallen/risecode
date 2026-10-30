package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class TracersModule extends Module {
    private final ColorSetting color = register(new ColorSetting("Color", "Player line color", 0xFF3B82F6));
    private final ColorSetting friendColor = register(new ColorSetting("FriendColor", "Friend line color", 0xFF34D399));
    private final DoubleSetting width = register(DoubleSetting.extended("Width", "Line width in pixels", 1.5, 0.5, 4));
    private final Minecraft minecraft;
    private final FriendManager friends;
    public TracersModule(EventBus bus, Minecraft minecraft, FriendManager friends) {
        super("Tracers", "Depth-tested lines to other players", Category.RENDER, bus);
        this.minecraft = minecraft; this.friends = friends;
    }
    public ColorSetting getColor() { return color; }
    public ColorSetting getFriendColor() { return friendColor; }
    public DoubleSetting getWidth() { return width; }
    @Override
    protected void onEnable() { listen(Render3DEvent.class, this::extract); }
    private void extract(Render3DEvent event) {
        if (minecraft.player == null) { return; }
        var context = event.getContext();
        float partialTick = context.deltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 start = dev.riserecode.render.TracerOrigin.of(event);
        for (var player : context.level().players()) {
            if (player == minecraft.player || !player.isAlive() || player.isRemoved() || player.isSpectator() || player.isInvisible()) { continue; }
            boolean friend = friends.isFriend(player.getUUID(), player.getGameProfile().name());
            Vec3 end = player.getPosition(partialTick).add(0, player.getBbHeight() / 2.0, 0);
            ColorSetting selected = friend ? friendColor : color;
            event.addLine(start, end, selected.sample((start.x + start.z) * 40, start.y * 40), selected.sample((end.x + end.z) * 40, end.y * 40), width.getValue().floatValue());
        }
    }
}
