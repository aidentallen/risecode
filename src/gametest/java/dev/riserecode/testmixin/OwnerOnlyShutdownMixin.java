package dev.riserecode.testmixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.riserecode.CameraFovClientTest;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fabric's teardown barrier cannot service halt's new executeBlocking cleanup.
 * The camera fixture checks on the server thread that there are no non-owner
 * players to remove, making precisely this cleanup task empty. Normal halt,
 * world saving and all production code still run. Packaged only in the test mod. */
@Mixin(IntegratedServer.class)
public abstract class OwnerOnlyShutdownMixin {
    @WrapOperation(method="halt",at=@At(value="INVOKE",target="Lnet/minecraft/client/server/IntegratedServer;executeBlocking(Ljava/lang/Runnable;)V"))
    private void riserecode$emptyTestCleanup(IntegratedServer server,Runnable action,Operation<Void> original){
        if(CameraFovClientTest.ownerOnlyShutdown&&!server.isSameThread()){
            CameraFovClientTest.skippedEmptyShutdownTasks++;
            return;
        }
        original.call(server,action);
    }
}
