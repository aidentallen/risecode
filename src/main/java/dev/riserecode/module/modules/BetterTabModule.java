package dev.riserecode.module.modules;
import dev.riserecode.Client;
import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.*;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.util.*;
public final class BetterTabModule extends Module {
    public enum Sort { VANILLA, NAME, PING, FRIEND }
    public final BooleanSetting numeric=register(new BooleanSetting("Numeric Ping","Show ping in milliseconds",true));
    public final BooleanSetting highlight=register(new BooleanSetting("Highlight Friends","Mark friends in the player list",true));
    public final ColorSetting friendColor=register(new ColorSetting("Friend Color","Friend text color",0xFF68DFBA));
    public final DoubleSetting rows=register(DoubleSetting.extended("Rows","Maximum rows per column",20,10,80));
    public final EnumSetting<Sort> sort=register(new EnumSetting<>("Sorting","Optional player list sorting",Sort.VANILLA));
    public BetterTabModule(EventBus bus){super("BetterTab","Numeric latency, friend highlighting and configurable player lists.",Category.UTILITY,bus);}
    private static boolean friend(PlayerInfo p){return Client.INSTANCE.getFriendManager().isFriend(p.getProfile().id(),p.getProfile().name());}
    @Override protected void onEnable(){listen(TabDisplayEvent.class,e->{switch(e.phase()){
        case NAME -> {if(highlight.getValue()&&friend(e.player())){e.name(net.minecraft.network.chat.Component.literal("◆ ").withColor(friendColor.getValue()&0xFFFFFF).append(e.name().copy()));}}
        case CAPACITY -> e.capacity(1000);
        case PING -> e.numeric(numeric.getValue());case ROWS -> e.rows(rows.getValue().intValue());
        case SORT -> {Comparator<PlayerInfo> order=switch(sort.getValue()){case VANILLA->null;case NAME->Comparator.comparing(p->p.getProfile().name(),String.CASE_INSENSITIVE_ORDER);case PING->Comparator.comparingInt(PlayerInfo::getLatency);case FRIEND->Comparator.comparing((PlayerInfo p)->!friend(p));};if(order!=null){e.players(e.players().stream().sorted(order).toList());}}
    }});}
}
