package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.EspGeometry;
import dev.riserecode.render.SilhouetteOutlineRenderer;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import net.minecraft.client.Minecraft;

public final class PlayerEspModule extends Module {
    public enum Mode { BOX, OUTLINE }
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Bounds or exterior model silhouette", Mode.BOX));
    private final ColorSetting color = register(new ColorSetting("Color", "Player color", 0xFF59D5CB));
    private final ColorSetting friendColor = register(new ColorSetting("FriendColor", "Friend color", 0xFF59D5A2));
    private final DoubleSetting width = register(new DoubleSetting("Width", "Outline pixels", 2, 0.5, 5));
    private final DoubleSetting range = register(new DoubleSetting("Range", "Maximum distance", 128, 8, 256));
    private final BooleanSetting visible = register(new BooleanSetting("VisibleOnly", "Depth-test against terrain", false));
    private final BooleanSetting invisible = register(new BooleanSetting("IncludeInvisible", "Include invisible players", false));
    private final Minecraft mc;
    private final FriendManager friends;
    public PlayerEspModule(EventBus bus, Minecraft mc, FriendManager friends) {
        super("PlayerESP", "Player bounds or whole-model outlines.", Category.RENDER, bus); this.mc = mc; this.friends = friends;
    }
    @Override protected void onEnable() { listen(Render3DEvent.class, this::extract); }
    private void extract(Render3DEvent event) {
        if (mc.player == null || mc.level == null) { return; }
        float partial = event.getContext().deltaTracker().getGameTimeDeltaPartialTick(false);
        for (var player : mc.level.players()) {
            if (player == mc.player || !player.isAlive() || player.isSpectator() || player.isRemoved() || !invisible.getValue() && player.isInvisible() || mc.player.distanceToSqr(player) > range.getValue() * range.getValue()) { continue; }
            ColorSetting selected = friends.isFriend(player.getUUID(), player.getGameProfile().name()) ? friendColor : color;
            var position = player.getPosition(partial); var bounds = player.getBoundingBox().move(position.subtract(player.position()));
            if (mode.getValue() == Mode.BOX) { EspGeometry.box(event, bounds, selected, width.getValue(), visible.getValue()); }
            else {
                var state = mc.getEntityRenderDispatcher().extractEntity(player, partial); var style = EspGeometry.style(selected, width.getValue()); boolean depth = visible.getValue();
                event.submit(context -> {
                    var camera = context.levelState().cameraRenderState;
                    var collector = SilhouetteOutlineRenderer.collector("players", style, depth, bounds.inflate(0.5), camera, context.submitNodeCollector());
                    mc.getEntityRenderDispatcher().submit(state, camera, position.x - camera.pos.x, position.y - camera.pos.y, position.z - camera.pos.z, context.poseStack(), collector);
                });
            }
        }
    }
}
