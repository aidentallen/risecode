package dev.riserecode;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.ModuleClassification;
import dev.riserecode.module.modules.CapePhysicsModule;
import dev.riserecode.render.LegacyCapeMotion;
import dev.riserecode.render.LegacyCapePose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import org.joml.Matrix3f;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Numeric expectations recorded by the original 1.8.9 cape renderer, not by the implementation. */
public final class CapePhysicsChecks {
    private static int checks;
    private CapePhysicsChecks() { }
    private static void check(boolean condition, String message) { checks++; if (!condition) { throw new AssertionError(message); } }
    private static void close(float actual, float expected, String scenario) {
        check(Math.abs(actual - expected) <= Math.max(0.00001F, Math.abs(expected) * 0.000001F), scenario + ": " + actual + " vs " + expected);
    }
    public static void main(String[] args) throws Exception {
        var resource = CapePhysicsChecks.class.getResourceAsStream("/dev/riserecode/cape/legacy-1.8.9.csv");
        if (resource == null) { throw new AssertionError("Missing original-renderer fixtures"); }
        String trace = ""; int tick = -1; double x = 0, z = 0; LegacyCapeMotion history = null;
        try (var reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            reader.readLine(); String line;
            while ((line = reader.readLine()) != null) {
                String[] f = line.split(",");
                if (!trace.equals(f[0])) {
                    trace = f[0]; tick = -1; x = z = 0;
                    history = new LegacyCapeMotion(0, 64, 0, trace.equals("yawwrap") ? 179 : 0);
                }
                int nextTick = Integer.parseInt(f[1]); double nextX = Double.parseDouble(f[2]), y = Double.parseDouble(f[3]), nextZ = Double.parseDouble(f[4]);
                if (tick != nextTick) {
                    history.begin();
                    if (Boolean.parseBoolean(f[10])) { history.moved(nextX - x, nextZ - z); }
                    history.updateBob(Double.parseDouble(f[6]), Double.parseDouble(f[7]), Boolean.parseBoolean(f[8]));
                    if (Boolean.parseBoolean(f[11])) { history.resetBob(); }
                    history.finish(nextX, y, nextZ, Float.parseFloat(f[5]));
                    tick = nextTick; x = nextX; z = nextZ;
                }
                float partial = Float.parseFloat(f[12]); boolean sneak = Boolean.parseBoolean(f[9]);
                var sample = history.sample(partial, sneak, 1);
                close(sample.pitch(), Float.parseFloat(f[13]), trace + " pitch @" + tick + "/" + partial);
                close(sample.sway() / 2, Float.parseFloat(f[14]), trace + " side @" + tick + "/" + partial);
                check(sample.equals(history.sample(partial, sneak, 1)), "repeated renders cannot advance history");
            }
        }
        var idle = new LegacyCapeMotion(10, 64, -10, 0); idle.begin(); idle.finish(10, 64, -10, 0);
        for (float strength : new float[] {0, .93F, 1, 2}) {
            close(idle.sample(.5F, false, strength).pitch(), 6, "strength preserves resting angle");
            close(idle.sample(.5F, true, strength).pitch(), 31, "strength preserves sneak angle");
        }
        var fast = new LegacyCapeMotion(0, 64, 0, 0); fast.begin(); fast.finish(3, 64, 3, 0);
        var angles = fast.sample(1, false, 1);
        check(angles.lean() > 150 && Math.abs(angles.sway()) > 20, "modern horizontal caps are removed");
        for (float bodyX : new float[] {0, .5F, -.3F}) {
            var body = new ModelPart(List.of(), Map.of()); var cape = new ModelPart(List.of(), Map.of());
            body.setRotation(bodyX, .12F, -.07F); cape.x = 2; cape.y = 3; cape.z = 4;
            LegacyCapePose.apply(body, cape, angles);
            var combined = new Matrix3f().rotationZYX(body.zRot, body.yRot, body.xRot).rotateZYX(cape.zRot, cape.yRot, cape.xRot);
            var reference = new Matrix3f().rotateX(angles.pitch() * ((float) Math.PI / 180)).rotateZ(angles.sway() / 2 * ((float) Math.PI / 180))
                    .rotateY(-angles.sway() / 2 * ((float) Math.PI / 180)).rotateY((float) Math.PI);
            float[] actual = combined.get(new float[9]), expected = reference.get(new float[9]);
            for (int i = 0; i < actual.length; i++) { close(actual[i], expected[i], "parent-compensated rotation matrix"); }
            check(cape.x == 2 && cape.y == 3 && cape.z == 4 && body.xRot == bodyX, "modern attachment and body pose stay unchanged");
        }
        var state = new AvatarRenderState();
        check(CapePhysicsModule.supported(Pose.STANDING, state) && CapePhysicsModule.supported(Pose.CROUCHING, state), "legacy poses supported");
        for (Pose pose : Pose.values()) { if (pose != Pose.STANDING && pose != Pose.CROUCHING) { check(!CapePhysicsModule.supported(pose, state), "modern pose stays vanilla: " + pose); } }
        state.swimAmount = .1F; check(!CapePhysicsModule.supported(Pose.STANDING, state), "swim transitions stay vanilla");
        state.swimAmount = 0; state.isFallFlying = true; check(!CapePhysicsModule.supported(Pose.STANDING, state), "elytra stays vanilla");
        var bus = new EventBus(); var module = new CapePhysicsModule(bus);
        check(!module.isEnabled() && module.getCategory() == Category.RENDER && module.classification() == ModuleClassification.CLIENT, "optional client render module");
        check(module.strength().getValue() == 1 && module.strength().getMin() == 0 && module.strength().getMax() == 8, "legacy strength default and tuning range");
        module.setEnabled(true); check(bus.getListenerCount() == 5, "module owns motion, extraction and pose subscriptions");
        module.setEnabled(false); check(bus.getListenerCount() == 0 && module.trackedPlayers() == 0, "disable releases subscriptions and histories");
        var manager = new dev.riserecode.module.ModuleManager(bus, org.slf4j.LoggerFactory.getLogger("CapePhysicsChecks")); manager.register(module);
        var directory = java.nio.file.Files.createTempDirectory("risecode-cape-profile-");
        try (var config = new dev.riserecode.config.ConfigManager(directory, manager, () -> true, ignored -> { }, org.slf4j.LoggerFactory.getLogger("CapePhysicsChecks"))) {
            module.strength().setValue(1.37); module.setEnabled(true); var saved = config.profileSnapshot();
            module.setEnabled(false); module.strength().setValue(.5); config.applyProfile(saved);
            check(module.isEnabled() && module.strength().getValue() == 1.37 && module.trackedPlayers() == 0, "profile restores tuning without stale history");
        } finally { module.setEnabled(false); }
        System.out.println("Cape physics checks passed (" + checks + " assertions).");
    }
}
