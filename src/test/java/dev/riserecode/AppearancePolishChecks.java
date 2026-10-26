package dev.riserecode;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.hud.FpsModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.PopIn;
import dev.riserecode.ui.theme.ModuleAppearance;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.world.phys.AABB;
import org.slf4j.LoggerFactory;

/** Consumer-level regressions for extended controls and output-only local appearance. */
public final class AppearancePolishChecks {
    private AppearancePolishChecks() { }
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-7,"numeric round trip: "+a+" / "+b);}
    public static void run() throws Exception {
        var positive=DoubleSetting.extended("Range","",64,8,128);
        var signed=DoubleSetting.extended("Offset X","",0,-1,1);
        var unchanged=DoubleSetting.extended("Opacity","",.5,0,1);
        for(var number:new DoubleSetting[]{positive,signed,unchanged}){
            for(int i=0;i<=100;i++) {double p=i/100.0;near(number.position(number.valueAt(p)),p);}
            var copy=number.copy();near(copy.getMax(),number.getMax());near(copy.position(number.getValue()),number.position(number.getValue()));
            number.setValue(number.getMax());near(number.valueAt(1),number.getMax());
        }
        near(positive.position(128),.75);near(signed.position(-1),.125);near(signed.position(1),.875);
        near(positive.keyboardIncrement(),1);near(unchanged.getMax(),1);
        var manager=new ThemeManager(Files.createTempDirectory("appearance-polish"),LoggerFactory.getLogger("appearance-polish"));Theme.bind(manager);
        var bus=new EventBus();var hud=new FpsModule(bus,null);var outline=new BlockOverlayModule(bus,null);var sprint=new SprintModule(bus,null);
        var h=manager.module(hud);var o=manager.module(outline);
        check(h.hasControls(),"HUD has applicable appearance");check(!manager.module(sprint).hasControls(),"nonvisual module has no appearance section");
        check(o.controls().stream().noneMatch(c->c.group().equals("Typography")||c.group().startsWith("Background")||c.group().equals("Buttons")),"world edges have no fonts, backgrounds or button styles");
        check(h.controls().stream().noneMatch(c->c.setting().getName().equals("Watermark Text")||c.setting().getName().equals("ArrayList Color")),"HUD modules do not inherit unrelated branding or ArrayList controls");
        h.global.setValue(false);((DoubleSetting)h.get("Font Scale")).setValue(3.0);
        near(ModuleAppearance.call(hud.capabilityId(),()->manager.number("Font Scale")),3);
        near(manager.number("Font Scale"),1);
        o.global.setValue(false);((DoubleSetting)o.get("Font Scale")).setValue(3.5);
        near(ModuleAppearance.call(outline.capabilityId(),()->manager.number("Font Scale")),1);
        ((dev.riserecode.setting.ColorSetting)o.get("Primary Accent")).setValue(0xFFFF2244);
        var paint=new dev.riserecode.setting.ColorSetting.Paint(-1,-1,dev.riserecode.setting.ColorSetting.Mode.THEME,dev.riserecode.util.ColorUtil.GradientDirection.HORIZONTAL,0,100);
        var frozen=ModuleAppearance.call(outline.capabilityId(),()->ModuleAppearance.capture(paint));
        check(frozen.primary()==0xFFFF2244&&frozen.mode()!=dev.riserecode.setting.ColorSetting.Mode.THEME,"deferred world draws capture the module palette");
        ((dev.riserecode.setting.ColorSetting)o.get("Primary Accent")).setValue(0xFF112233);
        check(frozen.primary()==0xFFFF2244,"later palette changes cannot mutate queued draws");
        var tab=new dev.riserecode.event.TabDisplayEvent(dev.riserecode.event.TabDisplayEvent.Phase.ROWS,null,null,java.util.List.of());tab.rows(320);check(tab.rows()==320,"native tab consumer accepts expanded rows");
        var history=new dev.riserecode.event.ChatDisplayEvent.History();history.limit(8000);check(history.limit()==8000,"native chat consumer accepts expanded history");
        var snapshot=manager.snapshot();manager.apply(snapshot);near(((DoubleSetting)manager.module(outline).get("Font Scale")).getValue(),3.5);
        check(manager.module(outline).effective("Font Scale")==null,"inactive legacy fields survive without becoming effective");
        var box=BlockOverlayModule.class.getDeclaredMethod("box",Render3DEvent.class,net.minecraft.world.phys.Vec3[].class,double.class,boolean.class);box.setAccessible(true);
        var context=(LevelExtractionContext)Proxy.newProxyInstance(LevelExtractionContext.class.getClassLoader(),new Class<?>[]{LevelExtractionContext.class},(proxy,method,args)->null);
        var through=(BooleanSetting)outline.getSettings().stream().filter(s->s.getName().equals("See-through Outline")).findFirst().orElseThrow();
        var width=(DoubleSetting)outline.getSettings().stream().filter(s->s.getName().equals("OutlineWidth")).findFirst().orElseThrow();width.setValue(32.0);
        for(boolean enabled:new boolean[]{false,true}){
            through.setValue(enabled);var event=new Render3DEvent(context);box.invoke(outline,event,dev.riserecode.render.BlockHighlightGeometry.corners(new AABB(.2,.2,.2,.8,.8,.8)).toArray(net.minecraft.world.phys.Vec3[]::new),1.0,true);
            check(event.getLines().size()==12,"all cube edges survive");check(event.getLines().stream().allMatch(l->l.visibleOnly()!=enabled&&l.width()==32),"depth mode applies to every maximum-width edge");
            check(event.getQuads().stream().allMatch(Render3DEvent.Quad::visibleOnly),"outline depth toggle leaves fill unchanged");
        }
        for(double strength:new double[]{0,1,10,20,40}) {for(double progress:new double[]{0,.1,.5,1,1.1}) {double scale=PopIn.scale(strength,progress);check(Double.isFinite(scale)&&scale>0,"large pop strengths never invert or divide by zero");}}
        near(((DoubleSetting)manager.getSetting("GUI Blur Radius")).getMax(),128);
        near(((DoubleSetting)manager.getSetting("Opening Duration")).getMax(),60);
        ((DoubleSetting)manager.getSetting("Font Scale")).setValue(4.0);check(9*4*dev.riserecode.ui.component.ButtonMetrics.textScale(17)<=13,"large fonts fit uniform button heights");
        Theme.bind(null);System.out.println("Appearance polish checks passed ("+checks+" assertions).");
    }
}
