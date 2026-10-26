package dev.riserecode;

import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.render.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.theme.*;
import dev.riserecode.util.RenderUtil;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Packaged native UI interaction, registry-wide shape sweep and representative complex contours. */
public final class Release023IntegrationTest {
    private static int checks,states;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static ThemeManager theme(){return Client.INSTANCE.getThemeManager();}
    private static void value(String key,Object value){var setting=theme().getSetting(key);if(setting instanceof ColorSetting color&&value instanceof Number number)color.setValue(number.intValue());else ThemeManager.assign(setting,value);theme().changed();}
    private static void shot(ClientGameTestContext c,String name){c.waitTicks(8);c.takeScreenshot("023-"+name);}
    private static void click(ClickGui gui,double x,double y){var event=new MouseButtonEvent(x*gui.getCanvasScale(),y*gui.getCanvasScale(),new MouseButtonInfo(0,0));gui.mouseClicked(event,false);gui.mouseReleased(event);}
    private static java.awt.image.BufferedImage image(String name){try(var files=Files.list(Path.of("screenshots"))){return javax.imageio.ImageIO.read(files.filter(p->p.getFileName().toString().contains("023-"+name+".png")).findFirst().orElseThrow().toFile());}catch(java.io.IOException e){throw new AssertionError(e);}}
    private static final class PopupProbe extends Screen {
        PopupProbe(){super(Component.literal("Uniform popup probe"));}
        @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(0,0,width,height,0xFF101010);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){RenderUtil.drawPopup(g,100,100,206,166,1);}
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();theme().reset();mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);value("Reduced Motion",true);audit();registry();value("Panel Opacity",1.0);value("Border Opacity",0.0);value("Background Color",0xFF203040);value("Sidebar Color",0xFFFF0000);mc.gui.setScreen(new PopupProbe());});
        shot(c,"uniform-popup-probe");var popup=image("uniform-popup-probe");
        check(popup.getRGB(240,300)==popup.getRGB(560,300),"popup has one paint even when the main sidebar is bright red");
        c.runOnClient(mc->{theme().reset();value("Reduced Motion",true);Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().getSettingsPopup().toggle();});shot(c,"popup");
        for(var page:List.of(ClickGui.Page.CLIENT_SOUNDS,ClickGui.Page.DISCORD_ACTIVITY,ClickGui.Page.GUI_SCALE)){
            c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.getSettingsPopup().activate(page==ClickGui.Page.CLIENT_SOUNDS?2:page==ClickGui.Page.DISCORD_ACTIVITY?4:6,true);check(mc.gui.screen()==gui&&gui.getPage()==page,"footer page is embedded: "+page);check(!ClickGui.NAVIGATION_PAGES.contains(page),"footer pages do not create sidebar tabs");});shot(c,page.name().toLowerCase(Locale.ROOT));
            c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var bounds=gui.getClientSettingsEditor().editor().getBounds();var window=gui.getWindowBounds();check(bounds.x()>window.x()+107&&bounds.y()>=window.y()&&bounds.y()+bounds.height()<=window.y()+window.height(),"embedded editor stays inside content area");check(dev.riserecode.ui.menu.MenuTheme.pageId(gui).startsWith("risecode:client:"),"legacy page appearance ID survives");});
        }
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var control=(Slider)gui.getClientSettingsEditor().editor().getControl("Interface Scale");var b=control.getBounds();click(gui,b.x()+b.width()*.7,b.y()+b.height()-5);});c.waitTicks(8);
        c.runOnClient(mc->check(theme().number("Interface Scale")>=.75&&theme().number("Interface Scale")<=1.3,"live scale remains valid after input"));
        for(double scale:new double[]{.75,1.3}){c.runOnClient(mc->value("Interface Scale",scale));shot(c,"scale-"+scale);}
        c.runOnClient(mc->{value("Interface Scale",1.0);value("Reduced Motion",false);value("Press Timing",2.0);value("Press Compression",.12);value("Press Strength",1.0);var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.APPEARANCE);gui.getThemeEditor().reveal("Font Selection");});c.waitTicks(20);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();var b=editor.getBounds();editor.mouseScrolled(b.centerX(),b.centerY(),3);});c.waitTicks(15);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var header=gui.getThemeEditor().section("Typography");var b=header.getBounds();click(gui,b.centerX(),b.centerY());});c.waitTicks(2);c.takeScreenshot("023-whole-card-bounce");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var editor=gui.getThemeEditor();var header=editor.section("Typography");var t=editor.transform(header);check(Math.abs(t.scale()-1)>.0001,"Appearance card uses configurable shared feedback");var b=header.getBounds();boolean was=header.isExpanded();double x=t.cx()+(b.centerX()-t.cx())*t.scale(),y=t.cy()+(b.centerY()-t.cy())*t.scale();editor.mouseClicked(x,y,0);check(header.isExpanded()!=was,"mid-bounce displayed header remains clickable");value("Press Strength",0.0);});c.waitTicks(2);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();check(editor.transform(editor.section("Typography")).scale()==1,"shared strength zero disables Appearance bounce");value("Reduced Motion",true);});shot(c,"appearance-reduced-motion");
        c.runOnClient(mc->{theme().reset();mc.gui.setScreen(null);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -5 64 -5 5 64 5 stone");world.getServer().runCommand("setblock 0 65 0 stone");world.getServer().runCommand("tp @a 2.5 65 3.5 145 0");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");c.waitTicks(10);
            c.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();var module=Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();module.setEnabled(true);module.getSettings().forEach(s->{if(s.getName().equals("OutlineColor"))((ColorSetting)s).setValue(0xFFFF00FF);if(s.getName().equals("OutlineWidth"))ThemeManager.assign(s,4.0);if(s.getName().equals("OverlayEnabled"))ThemeManager.assign(s,false);if(s.getName().equals("Transition Duration"))ThemeManager.assign(s,.5);});});
            for(int pass=0;pass<(Boolean.getBoolean("riserecode.withShaderPack")?2:1);pass++){
            if(pass==1){c.runOnClient(mc->shaders(true));c.waitFor(mc->ShaderWorldOverlay.active()&&mc.gui.overlay()==null,900);}
            final String mode=pass==0?"native":"shader";
            for(String block:List.of("anvil[facing=north]","anvil[facing=east]","oak_stairs[facing=north,half=bottom,shape=inner_left]","oak_fence[north=true,east=true]","cobblestone_wall[north=tall,east=low,up=true]","glass_pane[north=true,east=true]","oak_door[facing=north,half=lower,open=true]","oak_trapdoor[facing=north,open=true]","hopper[facing=east]","cauldron","rail[shape=ascending_north]","dandelion","brewing_stand")){
                world.getServer().runCommand("setblock 0 66 0 "+block);c.waitTicks(4);aim(c,new BlockPos(0,66,0));
                c.waitFor(mc->mc.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(new BlockPos(0,66,0)),160);c.waitTicks(12);
                String name="complex-"+mode+"-"+block.replaceAll("[^a-zA-Z0-9]","-");shot(c,name);
                c.runOnClient(mc->{var pos=new BlockPos(0,66,0);var mesh=HighlightMesh.selection(mc.level.getBlockState(pos).getShape(mc.level,pos));check(Client.INSTANCE.getWorldRenderer().getLines().size()==mesh.edges().size(),"native target submits only merged edges: "+block);});
                var screenshot=image(name);int pixels=0;for(int y=0;y<screenshot.getHeight();y++)for(int x=0;x<screenshot.getWidth();x++){int rgb=screenshot.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,blue=rgb&255;if(r>140&&blue>140&&g<130)pixels++;}
                check(pixels>20,"complex outline reaches framebuffer: "+mode+" "+block);
            }
            switching(c,world.getServer()::runCommand,mode);
            if(pass==1){c.runOnClient(mc->shaders(false));c.waitFor(mc->!ShaderWorldOverlay.active()&&mc.gui.overlay()==null,900);}
            }
            c.runOnClient(mc->{var module=Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();module.setEnabled(false);check(Client.INSTANCE.getTargetHighlights().targetKey()==null,"disable releases complex target state");});
        }
        c.runOnClient(mc->{theme().reset();mc.gui.setScreen(null);});
        Client.INSTANCE.getLogger().info("Release 0.23 native checks passed ({} assertions; {} registered block states)",checks,states);
    }
    private static void switching(ClientGameTestContext c,java.util.function.Consumer<String> command,String mode){
        command.accept("setblock -1 66 0 stone");command.accept("setblock 0 66 0 anvil[facing=north]");c.waitTicks(4);
        var clock=new java.util.concurrent.atomic.AtomicLong(1_000_000_000);
        c.runOnClient(mc->{Client.INSTANCE.getTargetHighlights().reset();Client.INSTANCE.getTargetHighlights().timeSource(clock::get);});aim(c,new BlockPos(-1,66,0));c.waitTicks(3);
        c.runOnClient(mc->clock.addAndGet(1_000_000_000));c.waitTicks(3);
        double start=c.computeOnClient(mc->Client.INSTANCE.getWorldRenderer().getLines().stream().mapToDouble(l->Math.min(l.from().x,l.to().x)).min().orElseThrow());
        aim(c,new BlockPos(0,66,0));c.waitTicks(3);
        c.runOnClient(mc->{double current=Client.INSTANCE.getWorldRenderer().getLines().stream().mapToDouble(l->Math.min(l.from().x,l.to().x)).min().orElseThrow();check(Math.abs(current-start)<.001,"complex switching starts at displayed cube");clock.addAndGet(250_000_000);});c.waitTicks(3);
        c.runOnClient(mc->{double current=Client.INSTANCE.getWorldRenderer().getLines().stream().mapToDouble(l->Math.min(l.from().x,l.to().x)).min().orElseThrow();check(current>start+.3&&current<0,"complex switching moves across the gap");});c.takeScreenshot("023-complex-"+mode+"-switch-midpoint");
        c.runOnClient(mc->clock.addAndGet(1_000_000_000));c.waitTicks(3);
        c.runOnClient(mc->{var module=Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();module.getSettings().forEach(s->{if(s.getName().equals("Outline Style"))ThemeManager.assign(s,BlockOverlayModule.OutlineStyle.CORNERS);if(s.getName().equals("OverlayEnabled"))ThemeManager.assign(s,true);});});shot(c,"complex-"+mode+"-anvil-corners-fill");
        c.runOnClient(mc->{var pos=new BlockPos(0,66,0);var mesh=HighlightMesh.selection(mc.level.getBlockState(pos).getShape(mc.level,pos));check(Client.INSTANCE.getWorldRenderer().getLines().size()==mesh.edges().size()*2,"corner marks apply once per continuous anvil edge");check(Client.INSTANCE.getWorldRenderer().getQuads().size()==mesh.faces().size(),"complex fill submits only exposed surfaces");
            var module=Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();module.getSettings().forEach(s->{if(s.getName().equals("Outline Style"))ThemeManager.assign(s,BlockOverlayModule.OutlineStyle.FULL);if(s.getName().equals("OverlayEnabled"))ThemeManager.assign(s,false);});Client.INSTANCE.getTargetHighlights().timeSource(System::nanoTime);Client.INSTANCE.getTargetHighlights().reset();});
    }
    private static void aim(ClientGameTestContext c,BlockPos pos){
        float[] rotation=c.computeOnClient(mc->{var eye=mc.player.getEyePosition();var mesh=HighlightMesh.selection(mc.level.getBlockState(pos).getShape(mc.level,pos));
            Vec3 best=null;double distance=Double.MAX_VALUE;
            for(var face:mesh.faces()){var a=mesh.vertices().get(face.a());var b=mesh.vertices().get(face.b());var d=mesh.vertices().get(face.d());var center=a.add(mesh.vertices().get(face.c())).scale(.5).add(pos.getX(),pos.getY(),pos.getZ());var normal=b.subtract(a).cross(d.subtract(a)).normalize();
                if(normal.dot(eye.subtract(center))<=0)continue;double next=eye.distanceToSqr(center);if(next<distance){distance=next;best=center.subtract(normal.scale(.01));}}
            if(best==null)best=Vec3.atCenterOf(pos);var delta=best.subtract(eye);return new float[]{(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)))};});
        c.getInput().lookAt(rotation[0],rotation[1]);
    }
    private static void shaders(boolean enabled){
        var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(enabled);
        try{config.save();net.irisshaders.iris.Iris.reload();}catch(java.io.IOException e){throw new AssertionError(e);}
    }
    private static void audit(){
        var report=new com.google.gson.JsonObject();
        for(var module:Client.INSTANCE.getModuleManager().getModules()){
            check(ModulePresentation.reviewed(module),"registered module has an explicit audit: "+module.getName());var appearance=theme().module(module);var controls=new com.google.gson.JsonArray();
            for(var control:appearance.controls()){String name=control.setting().getName();controls.add(name);if(!(module instanceof dev.riserecode.module.modules.hud.HudModule))check(!control.group().equals("HUD")&&!control.group().startsWith("Animation: HUD"),module.getName()+" exposes no HUD-only setting");}
            report.add(module.getName(),controls);
        }
        try{Files.writeString(Path.of("023-appearance-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(java.io.IOException e){throw new AssertionError(e);}
    }
    private static void registry(){
        for(var block:BuiltInRegistries.BLOCK)for(var state:block.getStateDefinition().getPossibleStates()){
            var shape=state.getShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);var mesh=HighlightMesh.selection(shape);states++;
            check(mesh.vertices().stream().allMatch(v->Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)),"finite shape: "+state);
            var edges=new HashSet<String>();for(var edge:mesh.edges()){var a=mesh.vertices().get(edge.a());var b=mesh.vertices().get(edge.b());String key=edge.a()<edge.b()?edge.a()+":"+edge.b():edge.b()+":"+edge.a();check(!a.equals(b)&&edges.add(key),"nonzero unique contour: "+state);}
            check(shape.isEmpty()==mesh.vertices().isEmpty(),"selection emptiness preserved: "+state);
        }
        check(states>10000,"sweep covers all registered states");
        var anvil=Blocks.ANVIL.defaultBlockState().getShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);check(HighlightMesh.selection(anvil).edges().size()<anvil.toAabbs().size()*12,"anvil removes component-box seams");
        Client.INSTANCE.getLogger().info("Release 0.23 registry sweep passed: {} states",states);
        HighlightMesh.clearCache();
    }
}
