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
        super(name, description, Category.HUD, bus);
        this.minecraft = minecraft;
    }
    /** Optional graphical content shares placement, scaling, visibility and editor bounds. */
    public double graphicWidth() { return 0; }
    public double graphicHeight() { return 0; }
    public void drawGraphic(net.minecraft.client.gui.GuiGraphicsExtractor graphics, double opacity, boolean edit) { }
    private Object dataKey;
    private long[] settingsKey;
    private String appearanceContext;
    private List<HudContent.Line> cached = List.of();
    private long contentBuilds,appearanceRevision=-1;
    /** Cache text/paint snapshots only. Animated paint still samples time at draw time. */
    public final List<HudContent.Line> content() {
        Object key = contentKey(); String context=dev.riserecode.ui.theme.ModuleAppearance.context();boolean contextChanged=!java.util.Objects.equals(appearanceContext,context);appearanceContext=context;long revision=dev.riserecode.ui.theme.Theme.manager()==null?0:dev.riserecode.ui.theme.Theme.manager().getRevision();boolean changed=contextChanged||appearanceRevision!=revision||!java.util.Objects.equals(dataKey,key);appearanceRevision=revision;
        var settings = getSettings();
        if (settingsKey == null || settingsKey.length != settings.size()) { settingsKey = new long[settings.size()]; changed = true; }
        for (int i = 0; i < settings.size(); i++) {
            long value=settings.get(i).revision();
            if(settingsKey[i]!=value){settingsKey[i]=value;changed=true;}
        }
        if (changed) { dataKey = key; cached = List.copyOf(buildContent()); contentBuilds++; }
        return cached;
    }
    public long contentBuilds() { return contentBuilds; }
    protected abstract Object contentKey();
    protected abstract List<HudContent.Line> buildContent();
}
