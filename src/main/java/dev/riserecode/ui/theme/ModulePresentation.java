package dev.riserecode.ui.theme;

import dev.riserecode.module.Module;
import dev.riserecode.setting.ColorSetting;
import java.util.Set;

/** Audited output consumers. A category alone never grants HUD or window appearance controls. */
public final class ModulePresentation {
    private ModulePresentation() { }
    private static final Set<String> TEXT_HUD=Set.of("Watermark","ArrayList","FPS","Ping","Coordinates","PotionEffects","MemoryUsage","ArmorHud");
    private static final Set<String> WORLD_PAINT=Set.of("BlockOverlay","Hitboxes","BlockESP","PlayerESP","StorageESP","Tracers","Trajectories","EnchantEffect");
    private static final Set<String> REVIEWED=Set.of("WorldMap","AutoBow","Trajectories","BetterChat","BetterTab","Nametags","Waypoints","AutoTool","FastBreak","Minimap","Zoom","Crosshair","ViewModel","StorageESP","PlayerESP","BlockESP","Ambience","NoRender","ChunkAnimations","CapePhysics","Baritone","Watermark","ArrayList","FullBright","Sprint","KillAura","Tracers","Clutch","FPS","Ping","Coordinates","PotionEffects","MemoryUsage","ArmorHud","FreeLook","FreeCam","EnchantEffect","ItemPhysics","WeatherChanger","BlockOverlay","Hitboxes","BlockAnimations");
    private static final Set<String> PALETTE=Set.of("Primary Accent","Secondary Accent","Accent Gradient","Gradient Mode","Gradient Speed","Gradient Direction","Gradient Spacing");
    private static final Set<String> TYPOGRAPHY=Set.of("Font Selection","Font Scale","Text Shadow","Text Shadow Color","Shadow Offset X","Shadow Offset Y","Shadow Opacity");
    private static final Set<String> LABEL_PANEL=Set.of("Font Selection","Surface Color","Shadow Color","Border Color","Border Thickness","Border Opacity");
    private static final Set<String> HUD_PANEL=Set.of("HUD Background","HUD Background Color","HUD Border","Corner Radius","Panel Opacity","Border Color","Border Thickness","Border Opacity");
    private static final Set<String> MAP=Set.of("Minimap Background","Minimap Image","Map Solid Color","Unexplored Color","Unexplored Opacity","Background","Background File","Background Tint","Background Opacity","Background Color","Gradient Start","Gradient End","Background Motion Speed");
    public static boolean reviewed(Module module){return REVIEWED.contains(module.getName());}
    public static boolean textHud(Module module){return TEXT_HUD.contains(module.getName());}
    public static boolean supports(Module module,ThemeManager.Control control){
        if(module==null)return false;
        String id=module.getName(),name=control.setting().getName(),group=control.group();boolean text=TEXT_HUD.contains(id),map=id.equals("Minimap"),hud=text||map;
        if(PALETTE.contains(name))return text||WORLD_PAINT.contains(id);
        if(name.equals("Reduced Motion"))return hud||Set.of("BlockOverlay","Hitboxes","BlockAnimations","Nametags").contains(id);
        if(id.equals("Nametags")&&(group.equals("Animation: Opening")||name.equals("Font Scale")||name.equals("Animation Speed")))return true;
        if((id.equals("Nametags")||id.equals("Waypoints"))&&LABEL_PANEL.contains(name))return true;
        if(!hud)return false;
        if(TYPOGRAPHY.contains(name))return true;
        if((group.equals("Animation: HUD Entrance")||group.equals("Animation: HUD Exit"))&&name.endsWith(" Movement"))return id.equals("ArrayList");
        if(name.equals("HUD Visible")||name.equals("Animation Speed")||group.equals("Animation: HUD Entrance")||group.equals("Animation: HUD Exit")||group.equals("Animation: Dragging"))return true;
        if(map)return MAP.contains(name);
        if(name.equals("Watermark Color")||group.equals("Branding"))return id.equals("Watermark");
        if(name.equals("ArrayList Color"))return id.equals("ArrayList")||id.equals("MemoryUsage");
        if(name.equals("ArrayList Padding"))return id.equals("ArrayList")||id.equals("Watermark");
        if(name.equals("ArrayList Spacing")||group.equals("Animation: HUD Reordering"))return id.equals("ArrayList");
        if(name.equals("HUD Bar Track Color"))return id.equals("MemoryUsage");
        if(HUD_PANEL.contains(name))return true;
        if(group.equals("Colors & Glow"))return name.contains("Glow");
        // Only these HUD renderers call ModuleBackground. Other output types never draw it.
        return group.startsWith("Background")&&!name.equals("Blur Mode")||name.equals("Background Darkness");
    }
    private static Object value(Module m,String name){return m.getSettings().stream().filter(s->s.getName().equals(name)).findFirst().map(s->(Object)s.getValue()).orElse(null);}
    public static boolean active(Module module,String name){
        if(module==null)return false;
        String id=module.getName();
        if(PALETTE.contains(name)){
            if(id.equals("FPS")&&"GRADIENT".equals(String.valueOf(value(module,"ColorMode"))))return true;
            if(id.equals("MemoryUsage")&&Boolean.TRUE.equals(value(module,"ShowBar"))&&"GRADIENT".equals(String.valueOf(value(module,"BarColorMode"))))return true;
            if(id.equals("Watermark")||id.equals("ArrayList"))return true;
            if(id.equals("Hitboxes")&&Boolean.TRUE.equals(value(module,"Use BlockOverlay Style")))return false;
            return module.getSettings().stream().anyMatch(s->{
                if(!(s instanceof ColorSetting color)||color.getMode()!=ColorSetting.Mode.THEME)return false;
                if(id.equals("FPS")){String mode=String.valueOf(value(module,"ColorMode"));return mode.equals("STATIC")?s.getName().equals("Color"):!s.getName().equals("Color");}
                if(id.equals("MemoryUsage")){if(s.getName().equals("TextColor"))return true;if(!Boolean.TRUE.equals(value(module,"ShowBar")))return false;return "STATIC".equals(String.valueOf(value(module,"BarColorMode")))?s.getName().equals("BarColor"):!s.getName().equals("BarColor");}
                if(id.equals("BlockOverlay"))return s.getName().equals("FillColor")?Boolean.TRUE.equals(value(module,"OverlayEnabled")):Boolean.TRUE.equals(value(module,"OutlineEnabled"));
                return true;
            });
        }
        if(id.equals("MemoryUsage")&&(name.equals("HUD Bar Track Color")||name.equals("ArrayList Color")))return Boolean.TRUE.equals(value(module,"ShowBar"))&&(!name.equals("ArrayList Color")||"GRADIENT".equals(String.valueOf(value(module,"BarColorMode"))));
        if(id.equals("Waypoints")&&LABEL_PANEL.contains(name))return Boolean.TRUE.equals(value(module,"Labels"));
        return true;
    }
}
