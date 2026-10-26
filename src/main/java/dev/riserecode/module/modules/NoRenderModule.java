package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.ChunkRenderPolicyEvent;
import dev.riserecode.event.FogEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import java.util.EnumMap;
import java.util.Map;

/** Independent fog and terrain-fade policies owned through module subscriptions. */
public final class NoRenderModule extends Module {
    public final BooleanSetting allFog = register(new BooleanSetting("All Fog", "Disable every native fog type", true));
    public final BooleanSetting chunkFade = register(new BooleanSetting("Chunk Fade", "Disable fog-colored blending when terrain loads", true));
    private final Map<FogEvent.Type, BooleanSetting> selected = new EnumMap<>(FogEvent.Type.class);

    public NoRenderModule(EventBus bus) {
        super("NoRender", "Disable fog environments and chunk fade independently", Category.RENDER, bus);
        for (FogEvent.Type type : FogEvent.Type.values()) {
            String label = switch (type) {
                case OVERWORLD -> "Overworld";
                case NETHER -> "Nether";
                case END -> "End";
                case OTHER_DIMENSIONS -> "Other Dimensions";
                case WATER -> "Water";
                case LAVA -> "Lava";
                case POWDER_SNOW -> "Powder Snow";
                case BLINDNESS -> "Blindness";
                case DARKNESS -> "Darkness";
            };
            var setting = register(new BooleanSetting(label + " Fog", "Disable " + label.toLowerCase(java.util.Locale.ROOT) + " fog", false));
            setting.setVisibleWhen(() -> !allFog.getValue());
            selected.put(type, setting);
        }
    }
    public BooleanSetting fogSetting(FogEvent.Type type) { return selected.get(type); }
    @Override protected void onEnable() {
        listen(ChunkRenderPolicyEvent.class, event -> { if (chunkFade.getValue()) { event.suppressFade(); } });
        listen(FogEvent.class, event -> {
            if (allFog.getValue() || selected.get(event.type()).getValue()) { event.disableFog(); }
        });
    }
    @Override protected void onDisable() { dev.riserecode.render.ChunkTerrainRenderer.invalidate(); }
}
