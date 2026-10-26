package dev.riserecode.event;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class TabDisplayEvent extends Event {
    public enum Phase { NAME, PING, SORT, ROWS, CAPACITY }
    private final Phase phase;private final PlayerInfo player;private Component name;private List<PlayerInfo> players;private boolean numeric;private int rows=20,capacity=80;
    public TabDisplayEvent(Phase phase,PlayerInfo player,Component name,List<PlayerInfo> players){this.phase=phase;this.player=player;this.name=name;this.players=players;}
    public int capacity(){return capacity;}public void capacity(int value){capacity=Math.clamp(value,80,1000);}
    public Phase phase(){return phase;}public PlayerInfo player(){return player;}public Component name(){return name;}public void name(Component value){name=value;}public List<PlayerInfo> players(){return players;}public void players(List<PlayerInfo> value){players=List.copyOf(value);}public boolean numeric(){return numeric;}public void numeric(boolean value){numeric=value;}public int rows(){return rows;}public void rows(int value){rows=Math.clamp(value,10,320);}
}
