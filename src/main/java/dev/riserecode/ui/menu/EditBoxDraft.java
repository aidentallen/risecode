package dev.riserecode.ui.menu;

import net.minecraft.client.gui.components.EditBox;

/** Native field drafts survive both resize rebuilds and retained-child return. */
public record EditBoxDraft(String value,int cursor,int highlight,boolean focused) {
    public interface SelectionAnchor { int riserecode$selectionAnchor(); }
    public static EditBoxDraft capture(EditBox field){
        return new EditBoxDraft(field.getValue(),field.getCursorPosition(),((SelectionAnchor)field).riserecode$selectionAnchor(),field.isFocused());
    }
    public void restore(EditBox field){field.setValue(value);field.setCursorPosition(cursor);field.setHighlightPos(highlight);}
}
