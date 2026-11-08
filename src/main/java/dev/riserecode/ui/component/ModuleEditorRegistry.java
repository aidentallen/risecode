package dev.riserecode.ui.component;

import dev.riserecode.module.Module;
import dev.riserecode.module.modules.*;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.component.setting.NumericField;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Feature UI providers are registered at composition, independent of the shared module card. */
public final class ModuleEditorRegistry {
    public record Extra(UIComponent component, double height, boolean beforeSettings) { }
    public record Editor(List<Extra> extras, boolean replacesSettings, BiConsumer<SettingControls,Runnable> controls, Supplier<String> details) {
        public Editor { extras=List.copyOf(extras); }
    }
    private final Map<Class<?>,Function<Module,Editor>> providers=new LinkedHashMap<>();
    public <M extends Module> void register(Class<M> type, Function<M,Editor> provider) { providers.put(type,module->provider.apply(type.cast(module))); }
    public Editor create(Module module) {
        for(var entry:providers.entrySet()){if(entry.getKey().isInstance(module)){return entry.getValue().apply(module);}}
        return simple(module::getDescription);
    }
    private static Editor simple(Supplier<String> details) { return new Editor(List.of(),false,(controls,changed)->{},details); }
    public static ModuleEditorRegistry defaults(Minecraft minecraft) {
        var registry=new ModuleEditorRegistry();
        registry.register(SeedCrackerModule.class,module->new Editor(List.of(new Extra(new SeedCrackerControls(module),SeedCrackerControls.HEIGHT,true)),false,(controls,changed)->{},()->module.statusLines().getFirst()));
        registry.register(BaritoneModule.class,module->new Editor(List.of(new Extra(new BaritoneControls(module),58,false)),false,(controls,changed)->
                controls.register(DoubleSetting.class,(number,listener)->number.getName().startsWith("Destination ")
                        ?new SettingControls.Control(new NumericField(number,listener),32):new SettingControls.Control(new Slider(number,listener),24)),module::getDescription));
        registry.register(GlassModule.class,module->new Editor(List.of(new Extra(new GlassControl(module,minecraft),40,true)),true,(controls,changed)->{},module::getDescription));
        registry.register(CapePhysicsModule.class,module->new Editor(List.of(),false,(controls,changed)->controls.register(DoubleSetting.class,(number,listener)->
                new SettingControls.Control(new Slider(number,listener,number==module.strength()?.01:0),24)),module::getDescription));
        registry.register(ChunkAnimationsModule.class,module->new Editor(List.of(new Extra(new Status(module::status),13,false)),false,(controls,changed)->{},module::getDescription));
        registry.register(NoFallModule.class,module->simple(module::status));
        registry.register(BlockAnimationsModule.class,module->simple(module::status));
        registry.register(FullBrightModule.class,module->simple(()->module.getMode().getValue()==FullBrightModule.Mode.SHADER_NIGHT_VISION?module.shaderStatus():module.getDescription()));
        return registry;
    }
    private static final class Status extends UIComponent {
        private final Supplier<String> text;
        Status(Supplier<String> text){this.text=text;}
        @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){RenderUtil.text(g,font,RenderUtil.ellipsize(font,text.get(),width/.6),x,y,.6,Theme.opacity(Theme.SECONDARY,alpha));}
    }
    private static final class GlassControl extends UIComponent {
        private final GlassModule module;
        private final ActionButton button;
        GlassControl(GlassModule module,Minecraft minecraft){this.module=module;button=new ActionButton("Customize / Preview",()->{FocusedInput.commit();minecraft.gui.setScreen(new GlassEditorScreen(module,minecraft.gui.screen()));});}
        @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){button.setBounds(x,y,width,26);button.render(g,font,mx,my,dt,alpha);RenderUtil.text(g,font,RenderUtil.ellipsize(font,module.status(),width/.6),x,y+29,.6,Theme.opacity(Theme.SECONDARY,alpha));}
        @Override public boolean mouseClicked(double mx,double my,int button){return this.button.mouseClicked(mx,my,button);}
        @Override public boolean mouseReleased(double mx,double my,int button){return this.button.mouseReleased(mx,my,button);}
    }
}
