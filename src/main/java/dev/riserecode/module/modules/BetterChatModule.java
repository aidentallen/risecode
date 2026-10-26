package dev.riserecode.module.modules;
import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
public final class BetterChatModule extends Module {
    public final BooleanSetting timestamps=register(new BooleanSetting("Timestamps","Prefix messages with local time",true));
    public final BooleanSetting duplicates=register(new BooleanSetting("Duplicate Counts","Count consecutive identical messages while retaining signed entries",true));
    public final BooleanSetting mentions=register(new BooleanSetting("Mention Highlight","Highlight your name without removing styles",true));
    public final BooleanSetting copy=register(new BooleanSetting("Copy Action","Append a click-to-copy action",true));
    public final DoubleSetting history=register(DoubleSetting.extended("History","Maximum retained chat lines/messages",500,100,2000));
    public final ColorSetting mentionColor=register(new ColorSetting("Mention Color","Mention text color",0xFFFFCE62));
    private final Minecraft mc;private Component previous;private int count;private long displayRevision=-1;
    private record Metadata(String time,int count) { }
    private final Map<Object,Metadata> metadata=new IdentityHashMap<>();
    private final ArrayDeque<Object> messageOrder=new ArrayDeque<>();
    private static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm");
    public BetterChatModule(EventBus bus,Minecraft mc){super("BetterChat","Timestamps, mentions, message copying and bounded history.",Category.UTILITY,bus);this.mc=mc;}
    @Override protected void onEnable(){listen(ChatDisplayEvent.class,this::decorate);listen(ChatDisplayEvent.History.class,e->e.limit(history.getValue().intValue()));listen(UpdateEvent.class,e->{if(mc.level==null){previous=null;count=0;}if(displayRevision!=stateRevision()){displayRevision=stateRevision();mc.gui.hud.getChat().rescaleChat();}});}
    private void decorate(ChatDisplayEvent e){
        Metadata info=metadata.get(e.key());
        if(info==null){if(e.original().equals(previous)){count=Math.min(9999,count+1);}else{previous=e.original().copy();count=1;}info=new Metadata(LocalTime.now().format(CLOCK),count);metadata.put(e.key(),info);messageOrder.addLast(e.key());while(messageOrder.size()>Math.max(4000,history.getValue().intValue())){metadata.remove(messageOrder.removeFirst());}}
        final Metadata captured=info;
        var display=Component.empty();if(timestamps.getValue()){display.append(Component.literal("["+captured.time()+"] ").withStyle(s->s.withColor(0x8A9AA8)));}
        Component message=e.original().copy();if(mentions.getValue()&&mc.player!=null){message=highlight(message,mc.player.getGameProfile().name(),mentionColor.getValue()&0xFFFFFF);}
        display.append(message);
        if(duplicates.getValue()&&captured.count()>1){display.append(Component.literal(" ×"+captured.count()).withStyle(s->s.withColor(0x8A9AA8)));}
        if(copy.getValue()){display.append(Component.literal(" [⧉]").withStyle(s->s.withColor(0x8A9AA8).withClickEvent(new ClickEvent.CopyToClipboard(e.original().getString())).withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy message")))));}
        e.display(display);
    }
    public static Component highlight(Component original,String name,int color){
        if(name.isBlank()){return original.copy();}var out=Component.empty();String needle=name.toLowerCase(Locale.ROOT);
        original.visit((style,text)->{String lower=text.toLowerCase(Locale.ROOT);int start=0,index;while((index=lower.indexOf(needle,start))>=0){if(index>start){out.append(Component.literal(text.substring(start,index)).setStyle(style));}out.append(Component.literal(text.substring(index,index+name.length())).setStyle(style.withColor(color)));start=index+name.length();}if(start<text.length()){out.append(Component.literal(text.substring(start)).setStyle(style));}return Optional.empty();},Style.EMPTY);return out;
    }
    @Override protected void onDisable(){previous=null;count=0;metadata.clear();messageOrder.clear();displayRevision=-1;if(mc.gui!=null&&mc.gui.hud!=null){mc.gui.hud.getChat().rescaleChat();}}
}
