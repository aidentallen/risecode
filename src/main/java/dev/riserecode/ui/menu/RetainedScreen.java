package dev.riserecode.ui.menu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A bounded child workflow that preserves its caller's draft and rendering resources. */
public abstract class RetainedScreen extends Screen implements MenuTheme.TransientScreen {
    protected final Screen parent;
    private boolean returning;
    protected RetainedScreen(Screen parent,String title){
        super(Component.literal(title));this.parent=parent;
        dev.riserecode.ui.component.FocusedInput.commit();
        if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.suspendForChoice();}
        if(parent instanceof PageAppearanceScreen page){page.suspendForChoice();}
    }
    @Override public void tick(){if(parent instanceof PageAppearanceScreen page){page.tick();}}
    @Override public void onClose(){returning=true;if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.resumeFromChoice();}if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();}minecraft.gui.setScreen(parent);}
    @Override public void removed(){if(!returning){if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.abandonChoice();}if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();page.removed();}}}
    @Override public boolean isPauseScreen(){return parent==null||parent.isPauseScreen();}
}
