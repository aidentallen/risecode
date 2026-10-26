package dev.riserecode.event;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
/** Display-only decoration. Original signed payload, source, signature and tag remain owned by vanilla. */
public final class ChatDisplayEvent extends Event {
    private final Object key;public Object key(){return key;}
    private final Component original;private final MessageSignature signature;private Component display;
    public ChatDisplayEvent(Object key,Component original,MessageSignature signature){this.key=key;this.original=original;this.signature=signature;display=original;}
    public Component original(){return original;}public MessageSignature signature(){return signature;}public Component display(){return display;}public void display(Component value){display=java.util.Objects.requireNonNull(value);}
    public static final class History extends Event {private int limit=100;public int limit(){return limit;}public void limit(int value){limit=Math.clamp(value,100,8000);}}
}
