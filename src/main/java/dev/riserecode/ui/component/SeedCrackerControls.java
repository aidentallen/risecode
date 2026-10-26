package dev.riserecode.ui.component;

import dev.riserecode.module.modules.SeedCrackerModule;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Uses the same themed action primitives as other module controls. */
public final class SeedCrackerControls extends UIComponent {
    public static final double HEIGHT=262;
    private final SeedCrackerModule module;
    private final List<ActionButton> buttons=new ArrayList<>();
    public SeedCrackerControls(SeedCrackerModule module) {
        this.module=module;
        add("Start",module::start); add("Stop",module::stop);
        add("Rescan Loaded Chunks",module::rescan); add("Clear Data",module::clearData);
        add("Restore Saved Data",module::restore); add("Copy Seed",module::copySeed);
        add("Lookup",module::lookup); add("Submit Verified Seed",module::submit);
    }
    private void add(String label, Runnable action) { buttons.add(new ActionButton(label,() -> { FocusedInput.commit(); action.run(); }).enabled(module::isEnabled)); }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double delta, double opacity) {
        int row=0;
        for (String line : module.statusLines().stream().limit(6).toList()) {
            RenderUtil.text(g,font,RenderUtil.ellipsize(font,line,width/.58),x,y+row*12,.58,Theme.opacity(Theme.SECONDARY,opacity)); row++;
        }
        double top=y+76;
        for (int i=0;i<buttons.size();i++) {
            var button=buttons.get(i); button.setBounds(x+(i%2)*(width/2+2),top+(i/2)*27,width/2-3,24); button.render(g,font,mx,my,delta,opacity);
        }
        String[] disclosure={"Submission sends server address, dimension,", "verified seed, version, username and anonymous", "flag to the upstream database. It authenticates", "your licensed account. Anonymous asks the service", "to hide your username; the service still receives it."};
        for (int i=0;i<disclosure.length;i++) { RenderUtil.text(g,font,RenderUtil.ellipsize(font,disclosure[i],width/.55),x,y+190+i*12,.55,Theme.opacity(Theme.SECONDARY,opacity)); }
    }
    @Override public boolean mouseClicked(double mx,double my,int button) { return buttons.stream().anyMatch(b -> b.getBounds().y()+b.getBounds().height()<=y+height && b.mouseClicked(mx,my,button)); }
}
