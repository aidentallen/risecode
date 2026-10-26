package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.performance.PerformancePreferences;
import dev.riserecode.render.WorldResolution.Extents;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.font.CustomTextRenderer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.slf4j.LoggerFactory;

public final class Release025Checks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var prefs=new PerformancePreferences(new JsonObject(),ignored->{});
        check(prefs.worldResolution.getValue()==100&&prefs.upscaler.getValue()==PerformancePreferences.Upscaler.FSR1&&prefs.sharpness.getValue()==.2,"native defaults");
        prefs.worldResolution.setValue(37.0);prefs.sharpness.setValue(.7);
        for(var mode:PerformancePreferences.Mode.values()){
            prefs.mode.setValue(mode);check(prefs.worldResolution.getValue()==37&&prefs.sharpness.getValue()==.7,"preset leaves independent rendering preferences intact: "+mode);
        }
        var restored=new PerformancePreferences(prefs.snapshot(),ignored->{});
        check(restored.worldResolution.getValue()==37&&restored.sharpness.getValue()==.7,"additive preferences round trip");
        var corrupt=new JsonObject();var rendering=new JsonObject();rendering.addProperty("World Resolution",Double.NaN);rendering.addProperty("Upscaler","INVALID");corrupt.add("rendering",rendering);
        check(new PerformancePreferences(corrupt,ignored->{}).worldResolution.getValue()==100,"invalid additive values preserve defaults");
        for(int width:new int[]{1,17,1279,1920})for(int height:new int[]{1,719,1080})for(int percent:new int[]{25,50,75,100}){
            var e=Extents.of(width,height,percent);check(e.worldWidth()>=1&&e.worldHeight()>=1&&e.worldWidth()<=width&&e.worldHeight()<=height,"bounded extents");
            check(e.worldWidth()==Math.max(1,(int)Math.ceil(width*percent/100.0)),"odd sizes round deterministically");
            if(percent==100)check(e.worldWidth()==width&&e.worldHeight()==height&&e.pixelScale()==1,"native bypass extent");
        }
        var theme=new ThemeManager(Files.createTempDirectory("025-theme"),LoggerFactory.getLogger("025"));Theme.bind(theme);
        var bold=(dev.riserecode.setting.DoubleSetting)theme.getSetting("Font Boldness");
        check(bold.getMin()==0&&bold.getMax()==100,"font stroke slider bounds");
        check(bold.getValue()==0&&CustomTextRenderer.stroke()==0,"zero boldness preserves draw path");bold.setValue(100.0);check(CustomTextRenderer.stroke()==.75,"maximum stroke is .75 logical pixels");Theme.bind(null);
        var block=new dev.riserecode.module.modules.BlockOverlayModule(new dev.riserecode.event.EventBus(),null);
        var originalFill=block.highlightStyle().faces();
        var effect=block.getSettings().stream().filter(s->s.getName().equals("Color Effect")).findFirst().orElseThrow();
        ThemeManager.assign(effect,dev.riserecode.module.modules.BlockOverlayModule.ColorEffect.RAINBOW_CYCLE);
        var cycle=block.highlightStyle().edges().getFirst().paint();check(cycle.sample(0,0,1)==cycle.sample(100,90,1),"cycle has one hue across the outline");
        ThemeManager.assign(effect,dev.riserecode.module.modules.BlockOverlayModule.ColorEffect.SPATIAL_RAINBOW);
        var spatial=block.highlightStyle().edges().getFirst().paint();check(spatial.sample(0,0,1)!=spatial.sample(40,0,1),"spatial hue varies by world position");
        check(block.highlightStyle().faces().equals(originalFill),"outline color effect leaves fill independent");
        var bitmap=Path.of("src/main/resources/assets/riserecode/textures/font/qraftys.png");
        check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(bitmap))).equals("b3aa3de3f4a8ecc6f1b71c05f03765b5202178b915df478869b0d74717b7e0df"),"original bitmap checksum pinned");
        for(String name:new String[]{"google_sans_regular.ttf","google_sans_medium.ttf","qraftys.json"})check(Files.isRegularFile(Path.of("src/main/resources/assets/riserecode/font",name)),"offline font asset "+name);
        var manifest=com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/riserecode/shaders/include/fsr1/source.json"))).getAsJsonObject();
        for(String name:new String[]{"ffx_a.h","ffx_fsr1.h"}){
            var data=Files.readAllBytes(Path.of("src/main/resources/assets/riserecode/shaders/include/fsr1",name.replace(".h",".glsl")));
            check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).equals(manifest.getAsJsonObject("sha256").get(name).getAsString()),"AMD reference checksum "+name);
        }
        System.out.println("Release 0.25 checks passed ("+checks+" assertions).");
    }
}
