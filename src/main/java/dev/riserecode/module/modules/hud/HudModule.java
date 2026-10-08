package dev.riserecode.module.modules.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.ui.hud.HudContent;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class HudModule extends Module {
    protected final Minecraft minecraft;
    protected HudModule(String name, String description, EventBus bus, Minecraft minecraft) {
        super(name, description, Category.HUD, bus); this.minecraft = minecraft;
    }
    private Object dataKey;
    private Object[] settingsKey;
    private List<HudContent.Line> cached = List.of();
    private long contentBuilds;
    /** Cache text/paint snapshots only. Animated paint still samples time at draw time. */
    public final List<HudContent.Line> content() {
        Object key = contentKey(); boolean changed = !java.util.Objects.equals(dataKey, key);
        var settings = getSettings();
        if (settingsKey == null || settingsKey.length != settings.size()) { settingsKey = new Object[settings.size()]; changed = true; }
        for (int i = 0; i < settings.size(); i++) {
            var setting = settings.get(i); Object value = setting instanceof dev.riserecode.setting.ColorSetting color ? color.paint() : setting.getValue();
            if (!java.util.Objects.equals(settingsKey[i], value)) { settingsKey[i] = value; changed = true; }
        }
        if (changed) { dataKey = key; cached = List.copyOf(buildContent()); contentBuilds++; }
        return cached;
    }
    public long contentBuilds() { return contentBuilds; }
    protected abstract Object contentKey();
    protected abstract List<HudContent.Line> buildContent();
}
