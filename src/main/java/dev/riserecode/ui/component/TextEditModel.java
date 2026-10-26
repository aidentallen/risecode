package dev.riserecode.ui.component;

import java.util.function.*;
import net.minecraft.client.input.KeyEvent;

/** Unicode, selection and clipboard editing for text fields. */
public final class TextEditModel {
    private String value="";
    private int caret,anchor,maximum;
    private final boolean multiline;
    private long revision;
    private Consumer<String> changed=ignored->{ };
    public TextEditModel(int maximum,boolean multiline){this.maximum=maximum;this.multiline=multiline;}
    public String value(){return value;}
    public int caret(){return caret;}
    public int anchor(){return anchor;}
    public long revision(){return revision;}
    public void maximum(int maximum){this.maximum=maximum;}
    public void onChange(Consumer<String> listener){changed=listener;}
    public void value(String next){value=next;caret=anchor=value.length();revision++;}
    public void selectAll(){anchor=0;caret=value.length();}
    public void move(int index,boolean selecting){caret=Math.clamp(index,0,value.length());if(!selecting){anchor=caret;}}
    public void insert(String text){
        int start=Math.min(caret,anchor),end=Math.max(caret,anchor);
        String clean=text.replace("\r\n","\n").codePoints().filter(cp->cp!=0xA7 && (!Character.isISOControl(cp) || multiline && cp=='\n'))
                .collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
        if(value.length()-(end-start)+clean.length()>maximum){return;}
        value=value.substring(0,start)+clean+value.substring(end);caret=anchor=start+clean.length();revision++;changed.accept(value);
    }
    public boolean key(KeyEvent e,Supplier<String> clipboard,Consumer<String> copy){
        if(e.isSelectAll()){selectAll();return true;}
        if(e.isCopy() || e.isCut()){copy.accept(value.substring(Math.min(caret,anchor),Math.max(caret,anchor)));if(e.isCut()){insert("");}return true;}
        if(e.isPaste()){insert(clipboard.get());return true;}
        if(e.key()==259 || e.key()==261){
            if(caret==anchor){if(e.key()==259 && caret>0){anchor=value.offsetByCodePoints(caret,-1);}if(e.key()==261 && caret<value.length()){anchor=value.offsetByCodePoints(caret,1);}}
            insert("");return true;
        }
        int next=switch(e.key()){
            case 263 -> caret==0?0:value.offsetByCodePoints(caret,-1);
            case 262 -> caret==value.length()?caret:value.offsetByCodePoints(caret,1);
            case 268 -> multiline?value.lastIndexOf('\n',Math.max(-1,caret-1))+1:0;
            case 269 -> {int end=value.indexOf('\n',caret);yield multiline && end>=0?end:value.length();}
            default -> -1;
        };
        if(next>=0){move(next,e.hasShiftDown());return true;}return false;
    }
}
