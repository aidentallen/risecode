package dev.riserecode;

import com.mojang.authlib.GameProfile;
import dev.riserecode.event.*;
import dev.riserecode.module.modules.CapePhysicsModule;
import dev.riserecode.render.LegacyCapePose;
import dev.riserecode.render.LegacyCapeState;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

/** Exercises real player ticks, extraction, cape models and generated test textures. No account cape needed. */
public final class CapePhysicsIntegrationTest {
    private static int checks;
    private CapePhysicsIntegrationTest() { }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    private static CapePhysicsModule module() { return Client.INSTANCE.getModuleManager().getByClass(CapePhysicsModule.class).orElseThrow(); }
    private static final class Actor extends RemotePlayer {
        private Vec3 step = Vec3.ZERO;
        private boolean capeShown = true;
        private final PlayerSkin testSkin;
        Actor(ClientLevel level) {
            super(level, new GameProfile(UUID.fromString("ca9e0000-1890-4262-8000-000000000001"), "CapeReference"));
            var base = DefaultPlayerSkin.getDefaultSkin();
            var texture = Identifier.fromNamespaceAndPath("riserecode-test", "textures/cape-reference.png");
            testSkin = PlayerSkin.insecure(base.body(), new ClientAsset.ResourceTexture(texture, texture), null, base.model());
        }
        @Override public PlayerSkin getSkin() { return testSkin; }
        @Override public boolean isModelPartShown(PlayerModelPart part) { return part == PlayerModelPart.CAPE ? capeShown : super.isModelPartShown(part); }
        @Override public void aiStep() {
            setDeltaMovement(step); super.aiStep();
            if (!step.equals(Vec3.ZERO)) { move(MoverType.SELF, step); }
            step = Vec3.ZERO;
        }
    }
    private static AvatarRenderState extract(Actor actor, AvatarRenderState state) {
        var renderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(actor);
        renderer.extractRenderState(actor, state, .5F); return state;
    }
    private static void matrix(AvatarRenderState state, PlayerCapeModel model) {
        model.setupAnim(state);
        var part = model.body.getChild("cape");
        var combined = new Matrix3f().rotationZYX(model.body.zRot, model.body.yRot, model.body.xRot).rotateZYX(part.zRot, part.yRot, part.xRot);
        var expected = new Matrix3f().rotation(LegacyCapePose.rotation(((LegacyCapeState) state).risecode$legacyCape()));
        float[] a = combined.get(new float[9]), b = expected.get(new float[9]);
        for (int i = 0; i < a.length; i++) { check(Math.abs(a[i] - b[i]) < .00001F, "baked model matches reference rotation matrix"); }
    }
    @SuppressWarnings("try") // Subscriptions intentionally exist only for their scoped lifetime.
    public static void run(ClientGameTestContext c) {
        c.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        c.runOnClient(mc -> { mc.options.enableVsync().set(false); mc.options.framerateLimit().set(160); check(!module().isEnabled(), "new cape module starts disabled"); });
        int[] counts = new int[5];
        var bus = Client.INSTANCE.getEventBus();
        try (var tick = bus.subscribe(CapeTickEvent.class, e -> counts[e.phase.ordinal()]++);
             var move = bus.subscribe(CapeMoveEvent.class, e -> counts[3]++);
             var pose = bus.subscribe(CapePoseEvent.class, e -> counts[4]++);
             var world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("fill -8 63 -8 8 63 16 minecraft:stone");
            world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 0.5 64 -5.5 0 8");
            Actor actor = c.computeOnClient(mc -> {
                var p = new Actor(mc.level); p.setId(9189262); p.setPos(.5,64,.5); p.setOnGround(true); mc.level.addEntity(p);
                mc.gui.setScreen(null); module().setEnabled(true); return p;
            });
            c.waitTicks(6);
            var state = c.computeOnClient(mc -> {
                var s = extract(actor, new AvatarRenderState());
                check(((LegacyCapeState) s).risecode$legacyCape() != null, "remote extraction uses tracked legacy history");
                var local = new AvatarRenderState();mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).extractRenderState(mc.player,local,.5F);
                check(((LegacyCapeState) local).risecode$legacyCape() != null, "local player uses the same module");
                check(module().trackedPlayers() >= 2, "histories are independent for local and remote players");
                matrix(s, new PlayerCapeModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_CAPE)));
                return s;
            });
            c.takeScreenshot("cape-legacy-idle");
            for (int i=0;i<10;i++) { c.runOnClient(mc -> actor.step = new Vec3(0,0,.14)); c.waitTick(); }
            c.takeScreenshot("cape-legacy-walk");
            c.runOnClient(mc -> {
                extract(actor,state);var first=((LegacyCapeState)state).risecode$legacyCape();extract(actor,state);
                check(first.equals(((LegacyCapeState)state).risecode$legacyCape()), "repeated extraction does not advance history");
                check(counts[0]>0&&counts[1]>0&&counts[2]>0&&counts[3]>0&&counts[4]>0,"all real tick/move/model hooks executed");
                actor.setShiftKeyDown(true); actor.setPose(Pose.CROUCHING);
            });c.waitTicks(5);
            c.runOnClient(mc -> {extract(actor,state);check(((LegacyCapeState)state).risecode$legacyCape()!=null,"sneaking uses legacy motion");matrix(state,new PlayerCapeModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_CAPE)));});
            c.takeScreenshot("cape-legacy-sneak");
            c.runOnClient(mc -> {actor.setShiftKeyDown(false);actor.setPose(Pose.SWIMMING);actor.setSwimming(true);});c.waitTicks(4);
            c.runOnClient(mc -> {
                extract(actor,state);check(((LegacyCapeState)state).risecode$legacyCape()==null,"reused state clears legacy marker for swimming");
                float flap=state.capeFlap,lean=state.capeLean,sway=state.capeLean2;
                module().setEnabled(false);extract(actor,state);
                check(flap==state.capeFlap&&lean==state.capeLean&&sway==state.capeLean2,"modern-pose fields match vanilla exactly");
                check(module().trackedPlayers()==0,"disable discards histories");
                module().setEnabled(true);actor.setSwimming(false);actor.setPose(Pose.STANDING);
            });c.waitTicks(14);
            c.runOnClient(mc -> {
                extract(actor,state);check(((LegacyCapeState)state).risecode$legacyCape()!=null,"legacy motion resumes after modern pose");
                actor.capeShown=false;extract(actor,state);check(!state.showCape&&((LegacyCapeState)state).risecode$legacyCape()==null,"hidden capes remain hidden and clear state");
                actor.capeShown=true;actor.setInvisible(true);extract(actor,state);check(((LegacyCapeState)state).risecode$legacyCape()==null,"invisible players stay vanilla");
                actor.setInvisible(false);module().setEnabled(false);extract(actor,state);check(((LegacyCapeState)state).risecode$legacyCape()==null,"disabled module leaves vanilla extraction");
                module().setEnabled(true);
            });c.waitTicks(3);
            c.runOnClient(mc -> {mc.level.removeEntity(actor.getId(),Entity.RemovalReason.DISCARDED);});c.waitTicks(3);
            c.runOnClient(mc -> {check(module().trackedPlayers()==1,"removed remote history is pruned");var replacement=new Actor(mc.level);replacement.setId(9189262);replacement.setPos(.5,64,.5);mc.level.addEntity(replacement);});c.waitTicks(4);
            c.runOnClient(mc -> check(module().trackedPlayers()==2,"replacement entity gets a fresh history"));
        } finally {
            c.waitTicks(3);
            c.runOnClient(mc -> { int tracked = module().trackedPlayers(); module().setEnabled(false); check(tracked == 0, "disconnect clears history while module is enabled"); });
        }
        c.runOnClient(mc -> check(module().trackedPlayers()==0,"world shutdown leaves no tracked entities"));
        Client.INSTANCE.getLogger().info("Cape physics native checks passed ({} assertions).",checks);
    }
}
