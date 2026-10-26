package dev.riserecode;

import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Test-only reload failure, excluded from release artifacts. */
public final class ResourcesTestBootstrap implements ClientModInitializer {
    static final AtomicBoolean failNextReload=new AtomicBoolean();
    @Override public void onInitializeClient(){
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath("riserecode-test","resources_failure"),
                (ResourceManagerReloadListener) manager->{if(failNextReload.getAndSet(false))throw new IllegalStateException("Intentional Resources reload failure");});
    }
}
