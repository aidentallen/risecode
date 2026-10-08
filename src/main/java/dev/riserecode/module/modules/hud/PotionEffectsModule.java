package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.hud.HudContent;
import dev.riserecode.ui.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;

public final class PotionEffectsModule extends HudModule {
    public enum Mode { ICONS, TEXT }
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Effect presentation", Mode.TEXT));
    private final BooleanSetting hideVanilla = register(new BooleanSetting("HideVanilla", "Hide vanilla effect HUD", true));
    private final BooleanSetting duration = register(new BooleanSetting("Duration", "Display remaining time", true));
    private final ColorSetting textColor = register(new ColorSetting("TextColor", "Effect name and duration color", Theme.TEXT));
    public PotionEffectsModule(EventBus bus, Minecraft mc) { super("PotionEffects", "Custom active status effects display.", bus, mc); }
    public boolean hidesVanilla() { return isEnabled() && hideVanilla.getValue(); }
    @Override protected Object contentKey() { return minecraft.player == null ? "disconnected" : java.util.List.of(minecraft.getLanguageManager().getSelected(), minecraft.player.getActiveEffects().stream().map(e -> java.util.List.of(e.getEffect(), e.getAmplifier(), e.isInfiniteDuration(), Math.max(0, e.getDuration() / 20))).toList()); }
    @Override protected List<HudContent.Line> buildContent() {
        List<HudContent.Line> result = new ArrayList<>();
        if (minecraft.player != null) {
            for (var effect : minecraft.player.getActiveEffects().stream().sorted(java.util.Comparator.comparing(e -> e.getEffect().value().getDisplayName().getString())).toList()) {
                int ticks = effect.getDuration(), seconds = Math.max(0, ticks / 20);
                String time = effect.isInfiniteDuration() ? "∞" : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                String name = mode.getValue() == Mode.TEXT ? effect.getEffect().value().getDisplayName().getString() + " " + (effect.getAmplifier() + 1) : "";
                if (duration.getValue()) { name += (name.isEmpty() ? "" : " · ") + time; }
                result.add(new HudContent.Line(List.of(HudContent.Span.paint(name, textColor.paint())),
                        mode.getValue() == Mode.ICONS ? Hud.getMobEffectSprite(effect.getEffect()) : null, -1, 0));
            }
        }
        return result.isEmpty() ? List.of(HudContent.Line.text("No active effects", 0xFF7F868E, false)) : result;
    }
}
