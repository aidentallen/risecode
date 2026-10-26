package dev.riserecode.module.modules;

import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.LegacyCapeMotion;
import dev.riserecode.render.LegacyCapePose;
import dev.riserecode.render.LegacyCapeState;
import dev.riserecode.setting.DoubleSetting;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;

/** Original cape response for equivalent input traces; never alters player movement or packets. */
public final class CapePhysicsModule extends Module {
    private final DoubleSetting strength = register(DoubleSetting.extended("Motion Strength", "1.0 restores original cape motion", 1, 0, 2));
    private final Map<AbstractClientPlayer, LegacyCapeMotion> histories = new IdentityHashMap<>();
    private Object level;
    public CapePhysicsModule(EventBus bus) { super("CapePhysics", "Original 1.8.9 motion for vanilla player capes", Category.RENDER, bus); }
    public DoubleSetting strength() { return strength; }
    public int trackedPlayers() { return histories.size(); }

    @Override protected void onEnable() {
        listen(CapeTickEvent.class, this::tick);
        listen(CapeMoveEvent.class, event -> { var history = histories.get(event.player); if (history != null) { history.moved(event.dx, event.dz); } });
        listen(CapeExtractEvent.class, this::extract);
        listen(CapePoseEvent.class, event -> {
            var angles = ((LegacyCapeState) event.state).risecode$legacyCape();
            if (angles != null) { LegacyCapePose.apply(event.body, event.cape, angles); }
        });
        listen(UpdateEvent.class, event -> {
            var current = event.getMinecraft().level;
            if (current == null || current != level) { histories.clear(); level = current; }
            else { histories.keySet().removeIf(player -> player.isRemoved() || !current.players().contains(player)); }
        });
    }
    @Override protected void onDisable() { histories.clear(); level = null; }

    private void tick(CapeTickEvent event) {
        var player = event.player;
        if (player.level() != level) { histories.clear(); level = player.level(); }
        if (event.phase == CapeTickEvent.Phase.BEGIN) {
            histories.computeIfAbsent(player, p -> new LegacyCapeMotion(p.getX(), p.getY(), p.getZ(), p.yBodyRot)).begin();
            return;
        }
        var history = histories.get(player);
        if (history == null) { return; }
        if (event.phase == CapeTickEvent.Phase.BOB) {
            var velocity = player.getDeltaMovement();
            history.updateBob(velocity.x, velocity.z, player.onGround() && player.getHealth() > 0);
        } else {
            if (player.isPassenger()) { history.resetBob(); }
            history.finish(player.getX(), player.getY(), player.getZ(), player.yBodyRot);
        }
    }

    public static boolean supported(Pose pose, AvatarRenderState state) {
        return (pose == Pose.STANDING || pose == Pose.CROUCHING) && !state.isFallFlying
                && !state.isVisuallySwimming && state.swimAmount == 0;
    }
    private void extract(CapeExtractEvent event) {
        var state = event.state;
        var history = histories.get(event.player);
        if (history == null || !history.ready() || !state.showCape || state.isInvisible
                || !supported(event.player.getPose(), state)) { return; }
        var angles = history.sample(event.partial, event.player.isShiftKeyDown(), strength.getValue().floatValue());
        state.capeFlap = angles.flap(); state.capeLean = angles.lean(); state.capeLean2 = angles.sway();
        ((LegacyCapeState) state).risecode$legacyCape(angles);
    }
}
