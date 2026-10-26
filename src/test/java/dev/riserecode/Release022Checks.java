package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.ModuleClassification;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.module.modules.HitboxesModule;
import dev.riserecode.render.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.theme.*;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Continuity, event ordering, live target geometry and persistent independent settings. */
public final class Release022Checks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(double a,double b){check(Math.abs(a-b)<.000001,a+" != "+b);}
    private static final LevelExtractionContext CONTEXT=(LevelExtractionContext)Proxy.newProxyInstance(LevelExtractionContext.class.getClassLoader(),new Class<?>[]{LevelExtractionContext.class},(p,m,a)->null);
    private static Render3DEvent frame(EventBus bus,TargetHighlight.Request request){var event=new Render3DEvent(CONTEXT);if(request!=null)event.targetHighlight(request);bus.post(event);return event;}
    private static TargetHighlight.Request request(TargetHighlight.Key key,List<List<Vec3>> boxes,TargetHighlight.Motion motion,TargetHighlight.Style style){return new TargetHighlight.Request("test",key,"stable",boxes,motion,style,false);}
    private static dev.riserecode.setting.Setting<?> setting(BlockOverlayModule m,String name){return m.getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow();}
    public static void main(String[] args)throws Exception {
        var bus=new EventBus();var renderer=new WorldRenderer(bus);var coordinator=new TargetHighlightCoordinator(bus,null);var clock=new AtomicLong(1_000_000_000);coordinator.timeSource(clock::get);
        var blockKey=new TargetHighlight.Key(TargetHighlight.Kind.BLOCK,0L);var entityKey=new TargetHighlight.Key(TargetHighlight.Kind.ENTITY,UUID.randomUUID());
        var block=List.of(BlockHighlightGeometry.corners(new AABB(0,0,0,1,1,1)));var entity=List.of(BlockHighlightGeometry.corners(new AABB(3,0,0,3.6,1.8,.6)));
        var green=new ColorSetting("Green","",0xFF00FF00);var red=new ColorSetting("Red","",0xFFFF0000);
        var blockStyle=TargetHighlight.Style.of(green,1,2,0,false,green,.4);var entityStyle=TargetHighlight.Style.of(red,1,6,0,false,null,0);
        var motion=new TargetHighlight.Motion(true,.2,true,false,.2,false);
        var first=frame(bus,request(blockKey,block,motion,blockStyle));near(first.getLines().getFirst().from().x,.5);
        check(renderer.getLines().equals(first.getLines()),"coordinator runs before immutable world snapshot");
        clock.addAndGet(300_000_000);var settled=frame(bus,request(blockKey,block,motion,blockStyle));near(settled.getLines().getFirst().from().x,0);
        var switched=frame(bus,request(entityKey,entity,motion,entityStyle));
        check(switched.getLines().size()==12&&switched.getLines().getFirst().from().equals(settled.getLines().getFirst().from()),"block-to-entity starts at the displayed box without another pop");
        near(switched.getLines().getFirst().width(),2);check(!switched.getQuads().isEmpty(),"outgoing block fill is retained at switch start");
        clock.addAndGet(100_000_000);var midpoint=frame(bus,request(entityKey,entity,motion,entityStyle));
        check(midpoint.getLines().getFirst().from().x>2&&midpoint.getLines().getFirst().from().x<3,"switch travels between targets");
        check(midpoint.getLines().getFirst().width()>2&&midpoint.getLines().getFirst().width()<6,"independent widths blend");
        check((midpoint.getLines().getFirst().color()>>16&255)>0&&(midpoint.getLines().getFirst().color()>>8&255)>0,"entity color blends with the block palette");
        for(int i=0;i<12;i++){clock.addAndGet(20_000_000);var moving=List.of(BlockHighlightGeometry.corners(new AABB(3+i*.01,0,0,3.6+i*.01,i<6?1.8:1.5,.6)));
            var current=frame(bus,request(entityKey,moving,motion,entityStyle));if(i==11){near(current.getLines().getFirst().from().x,3.11);check(current.getQuads().isEmpty(),"entity is outline-only after fill fades");}}
        var air=frame(bus,null);check(air.getLines().isEmpty()&&coordinator.targetKey()==null,"empty space immediately clears state and geometry");
        var acquired=frame(bus,request(entityKey,entity,motion,entityStyle));near(acquired.getLines().getFirst().from().x,3.3);
        var reduced=new TargetHighlight.Motion(true,10,true,true,10,true);
        var immediate=frame(bus,request(blockKey,block,reduced,blockStyle));near(immediate.getLines().getFirst().from().x,0);
        coordinator.release("test");check(coordinator.targetKey()==null,"module release clears its highlight");
        var pieces=List.of(BlockHighlightGeometry.corners(new AABB(0,0,0,1,.5,1)),BlockHighlightGeometry.corners(new AABB(0,.5,.5,1,1,1)));
        var noPop=new TargetHighlight.Motion(true,.2,false,false,.2,false);
        frame(bus,request(blockKey,pieces,noPop,blockStyle));frame(bus,request(entityKey,entity,noPop,entityStyle));clock.addAndGet(100_000_000);
        var multipart=frame(bus,request(entityKey,entity,noPop,entityStyle));check(multipart.getLines().stream().allMatch(l->l.from().x>2),"multipart source and destination travel together instead of fading at old locations");
        var duplicate=new Render3DEvent(CONTEXT);duplicate.targetHighlight(request(blockKey,block,motion,blockStyle));boolean rejected=false;
        try{duplicate.targetHighlight(request(entityKey,entity,motion,entityStyle));}catch(IllegalStateException expected){rejected=true;}check(rejected,"duplicate aimed providers cannot render two highlights");
        coordinator.close();renderer.close();check(bus.getListenerCount()==0,"coordinator and renderer release subscriptions");

        var manager=new ModuleManager(new EventBus(),LoggerFactory.getLogger("022"));var blocks=manager.register(new BlockOverlayModule(new EventBus(),null));
        var boxes=manager.register(new HitboxesModule(new EventBus(),null,blocks));
        check(!boxes.isEnabled()&&boxes.mode.getValue()==HitboxesModule.Mode.ONLY_WHEN_LOOKING,"target-only default and disabled module");
        check(boxes.classification()==ModuleClassification.CLIENT&&boxes.players.getValue()&&boxes.mobs.getValue()&&boxes.others.getValue(),"visual classification and all target groups enabled");
        check(boxes.inheritAnimation.getValue()&&boxes.inheritStyle.getValue()&&!boxes.color.isVisible(),"inheritance enabled and individual controls hidden");
        ((DoubleSetting)setting(blocks,"OutlineWidth")).setValue(12.0);((BooleanSetting)setting(blocks,"Pop Animation")).setValue(true);((DoubleSetting)setting(blocks,"Block Pop Duration")).setValue(.4);
        boxes.inheritStyle.setValue(false);boxes.inheritAnimation.setValue(false);near(boxes.width.getValue(),12);near(boxes.popDuration.getValue(),.4);check(boxes.pop.getValue(),"first override copies inherited pop toggle");
        boxes.width.setValue(4.0);boxes.popDuration.setValue(.7);boxes.inheritStyle.setValue(true);boxes.inheritAnimation.setValue(true);
        ((DoubleSetting)setting(blocks,"OutlineWidth")).setValue(14.0);near(boxes.highlightStyle().width(),14);
        boxes.inheritStyle.setValue(false);boxes.inheritAnimation.setValue(false);near(boxes.width.getValue(),4);near(boxes.popDuration.getValue(),.7);
        var config=new ConfigManager(Files.createTempDirectory("022-profile"),manager,()->true,v->{},LoggerFactory.getLogger("022-profile"));
        var profile=config.profileSnapshot();boxes.width.setValue(9.0);boxes.popDuration.setValue(.9);config.applyProfile(profile);
        near(boxes.width.getValue(),4);near(boxes.popDuration.getValue(),.7);check(!boxes.inheritStyle.getValue()&&!boxes.inheritAnimation.getValue(),"profile restores individual controls without reseeding");
        boxes.mode.setValue(HitboxesModule.Mode.ALWAYS);check(boxes.range.isVisible()&&!boxes.smooth.isVisible()&&!boxes.popOnSwitch.isVisible(),"Always exposes only applicable animation controls");
        var theme=new ThemeManager(Files.createTempDirectory("022-theme"),LoggerFactory.getLogger("022-theme"));Theme.bind(theme);
        var appearance=theme.module(boxes);appearance.global.setValue(false);((BooleanSetting)appearance.get("Reduced Motion")).setValue(true);
        check(boxes.highlightMotion().reduced(),"module Reduced Motion reaches hitbox animation");
        check(appearance.controls().stream().noneMatch(c->c.group().equals("Typography")||c.group().startsWith("Background")),"hitbox appearance excludes irrelevant controls");
        Theme.bind(null);System.out.println("Release 0.22 checks passed ("+checks+" assertions).");
    }
}
