package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BlockListSetting;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Hides every block that is not on the list, so ores and other picked blocks show through the rock. */
public final class XrayModule extends Module {
    private static volatile Set<Block> shown;
    private final BlockListSetting blocks;
    private final Minecraft minecraft;
    private List<String> applied;

    public XrayModule(EventBus bus, Minecraft minecraft) {
        super("Xray", "Hides everything except the blocks you pick.", Category.RENDER, bus);
        this.minecraft = minecraft;
        blocks = register(new BlockListSetting("Blocks", "Blocks that stay visible", BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.getNamespace().equals("minecraft") && (id.getPath().endsWith("_ore") || id.getPath().equals("ancient_debris")))
                .map(Object::toString).toList()));
    }

    /** True for a block the renderer should skip and treat as see-through. Safe to call from chunk builder threads. */
    public static boolean hides(BlockState state) {
        Set<Block> visible = shown;
        return visible != null && !state.isAir() && !visible.contains(state.getBlock());
    }

    @Override protected void onEnable() {
        apply();
        listen(UpdateEvent.class, ignored -> { if (!blocks.getValue().equals(applied)) { apply(); } });
    }
    @Override protected void onDisable() { shown = null; applied = null; rebuild(); }

    private void apply() {
        applied = List.copyOf(blocks.getValue());
        Set<Block> set = new HashSet<>();
        for (String id : applied) { BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse(id)).ifPresent(set::add); }
        shown = set;
        rebuild();
    }
    private void rebuild() {
        if (minecraft.level != null) {
            minecraft.levelRenderer.invalidateCompiledGeometry(minecraft.level, minecraft.options, minecraft.gameRenderer.mainCamera(), minecraft.getBlockColors());
        }
    }
}
