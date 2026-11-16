package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.map.MapMarkers;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.KeySetting;

/** Settings of the fullscreen world map; while disabled the map key does nothing. */
public final class WorldMapModule extends Module {
    public enum Window { FULLSCREEN, PANEL }
    public enum Names { HOVER, ALWAYS, NEVER }
    public enum Backdrop { DARK, THEME }
    public final KeySetting openKey=register(new KeySetting("Open Map Key","Open the world map",77));
    public final EnumSetting<Window> window=register(new EnumSetting<>("Window","Fullscreen map or a movable panel",Window.FULLSCREEN));
    public final EnumSetting<Backdrop> backdrop=register(new EnumSetting<>("Backdrop","Dark unexplored area or the theme background",Backdrop.DARK));
    public final BooleanSetting follow=register(new BooleanSetting("Follow Player","Keep the map centered on you until you drag it",true));
    public final BooleanSetting rememberZoom=register(new BooleanSetting("Remember Zoom","Reopen the map at the last zoom",true));
    public final DoubleSetting defaultZoom=register(DoubleSetting.extended("Default Zoom","Pixels per block when the map opens",3,.1,24));
    public final BooleanSetting chunkGrid=register(new BooleanSetting("Chunk Grid","Draw chunk and region borders",false));
    public final BooleanSetting toolbar=register(new BooleanSetting("Toolbar","Show the buttons in the corner",true));
    public final BooleanSetting coordinates=register(new BooleanSetting("Coordinates","Show the block under the cursor",true));
    public final BooleanSetting biome=register(new BooleanSetting("Biome","Show the biome under the cursor in loaded chunks",true));
    public final BooleanSetting scaleBar=register(new BooleanSetting("Scale Bar","Show the map scale",true));
    public final BooleanSetting waypoints=register(new BooleanSetting("Waypoints","Show waypoints on the map",true));
    public final EnumSetting<Names> waypointNames=register(new EnumSetting<>("Waypoint Names","When waypoint names are shown",Names.HOVER));
    public final EnumSetting<MapMarkers.PlayerMarker> marker=register(new EnumSetting<>("Your Marker","Arrow or your head",MapMarkers.PlayerMarker.ARROW));
    public final BooleanSetting players=register(new BooleanSetting("Players","Show other loaded players",true));
    public final BooleanSetting playerNames=register(new BooleanSetting("Player Names","Show names under other players",true));
    public final BooleanSetting entities=register(new BooleanSetting("Entities","Show loaded mobs",false));

    public WorldMapModule(EventBus bus){
        super("WorldMap","Fullscreen explored map with layers, waypoints and players.",Category.HUD,bus);
        waypointNames.setVisibleWhen(waypoints::getValue);
        playerNames.setVisibleWhen(players::getValue);
        defaultZoom.setVisibleWhen(()->!rememberZoom.getValue());
        setEnabled(true);
    }
}
