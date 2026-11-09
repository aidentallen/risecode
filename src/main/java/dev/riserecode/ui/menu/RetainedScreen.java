package dev.riserecode.ui.menu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A bounded child workflow that preserves its caller's draft and rendering resources. */
public abstract class RetainedScreen extends Screen implements MenuTheme.TransientScreen {
    protected final Screen parent;
    private boolean returning;
    private int children;
    public void suspendForChild(){children++;}
    public void resumeFromChild(){if(children>0){children--;}}
    protected RetainedScreen(Screen parent,String title){
        super(Component.literal(title));this.parent=parent;
        NativeScreenRetention.suspend(parent);
        dev.riserecode.ui.component.FocusedInput.commit();
        if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.suspendForChoice();}
        if(parent instanceof PageAppearanceScreen page){page.suspendForChoice();}
        if(parent instanceof RetainedScreen retained){retained.suspendForChild();}
    }
    @Override public void tick(){if(parent instanceof PageAppearanceScreen page){page.tick();}else if(parent instanceof RetainedScreen retained){retained.tick();}else if(!NativeScreenRetention.valid(parent)){minecraft.gui.setScreen(null);}}
    @Override public void onClose(){returning=true;NativeScreenRetention.resume(parent);if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.resumeFromChoice();}if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();}if(parent instanceof RetainedScreen retained){retained.resumeFromChild();}minecraft.gui.setScreen(parent);}
    @Override public void removed(){if(!returning&&children==0){NativeScreenRetention.abandon(parent);if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.abandonChoice();}if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();page.removed();}if(parent instanceof RetainedScreen retained){retained.resumeFromChild();retained.removed();}}}
    @Override public boolean isPauseScreen(){return parent==null||parent.isPauseScreen();}
}
