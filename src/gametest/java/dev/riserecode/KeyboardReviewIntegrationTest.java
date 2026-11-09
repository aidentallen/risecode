package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.ui.clickgui.ClickGui;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;

/** Shared custom-control keyboard navigation in the actual extracted screen, with no pointer clicks. */
public final class KeyboardReviewIntegrationTest {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    static void key(ClientGameTestContext context, int key) {
        context.getInput().pressKey(key); context.waitTicks(2);
    }
    static void focus(ClientGameTestContext context, String prefix) {
        for (int i = 0; i < 100; i++) {
            key(context, InputConstants.KEY_TAB);
            if (context.computeOnClient(mc -> gui().focusedControlLabel().startsWith(prefix))) { return; }
        }
        throw new AssertionError("Keyboard cannot reach " + prefix + ": " + context.computeOnClient(mc -> gui().accessibleControlLabels()));
    }
    public static void run(ClientGameTestContext context) {
        context.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        var module = Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
        context.runOnClient(mc -> {
            if (Boolean.getBoolean("riserecode.requireNarrator")) {
                check(mc.getNarrator().isActive(), "Native narrator engine is available");
                mc.options.narrator().set(net.minecraft.client.NarratorStatus.ALL);
            }
            Client.INSTANCE.getModuleManager().disableAll();
            module.getMode().setValue(FullBrightModule.Mode.GAMMA); module.getBrightness().setValue(.5);
            gui().open(null); gui().selectPage(ClickGui.Page.SEARCH); gui().getSearchField().setValue("FullBright");
        });
        context.waitFor(mc -> gui().isOpen(), 240); context.waitTicks(20);
        focus(context, "FullBright, module");
        key(context, InputConstants.KEY_SPACE);
        context.runOnClient(mc -> check(module.isEnabled(), "Space enables focused module"));
        key(context, InputConstants.KEY_RIGHT); context.waitTicks(20);
        context.runOnClient(mc -> check(gui().getModuleButton("FullBright").isExpanded(), "Right expands module settings"));
        focus(context, "Brightness, slider");
        key(context, InputConstants.KEY_LEFT);
        context.runOnClient(mc -> check(module.getBrightness().getValue() < .5, "Arrow adjusts slider without pointer"));
        key(context, InputConstants.KEY_RETURN);
        context.runOnClient(mc -> check(dev.riserecode.ui.component.FocusedInput.active(), "Enter starts slider text editing"));
        key(context, InputConstants.KEY_ESCAPE);
        context.runOnClient(mc -> check(!dev.riserecode.ui.component.FocusedInput.active() && mc.gui.screen() == gui(), "Escape ends editor before closing screen"));
        focus(context, "Mode, choice");
        var mode = module.getMode().getValue();
        key(context, InputConstants.KEY_RIGHT);
        context.runOnClient(mc -> check(module.getMode().getValue() != mode, "Arrow changes enum choice"));
        context.runOnClient(mc -> { module.setEnabled(false); mc.gui.setScreen(null); });
        colorControls(context); configurationFlows(context); auxiliaryControls(context); hudControls(context);
        context.runOnClient(mc->mc.gui.setScreen(null));
        Client.INSTANCE.getLogger().info("Keyboard review native checks passed ({} assertions)", checks);
    }
    private static final class ColorProbe extends net.minecraft.client.gui.screens.Screen {
        final dev.riserecode.setting.ColorSetting setting=new dev.riserecode.setting.ColorSetting("Probe","",0xFF112233);
        final dev.riserecode.ui.component.setting.ColorPicker picker=new dev.riserecode.ui.component.setting.ColorPicker(setting,()->{});
        final dev.riserecode.ui.component.KeyboardNavigation navigation=new dev.riserecode.ui.component.KeyboardNavigation();
        ColorProbe(){super(net.minecraft.network.chat.Component.literal("Color keyboard acceptance"));}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float delta){
            g.fill(0,0,width,height,dev.riserecode.ui.theme.Theme.SURFACE);
            picker.setBounds(20,20,300,picker.getPreferredHeight());navigation.begin();
            try{picker.render(g,font,mx,my,.05,1);}finally{navigation.end();}
        }
        @Override public boolean keyPressed(KeyEvent event){if(navigation.key(event)){return true;}return dev.riserecode.ui.component.FocusedInput.key(event,minecraft.keyboardHandler::getClipboard,minecraft.keyboardHandler::setClipboard);}
        @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent event){return dev.riserecode.ui.component.FocusedInput.character(event);}
    }
    private static void colorFocus(ClientGameTestContext context,ColorProbe probe,String prefix){
        for(int i=0;i<100;i++){key(context,InputConstants.KEY_TAB);if(context.computeOnClient(mc->probe.navigation.focusedLabel().startsWith(prefix))){return;}}
        throw new AssertionError("Cannot reach color control: "+prefix);
    }
    private static void colorControls(ClientGameTestContext context){
        var probe=context.computeOnClient(mc->new ColorProbe());context.runOnClient(mc->mc.gui.setScreen(probe));context.waitTicks(3);
        colorFocus(context,probe,"Probe, color,");key(context,InputConstants.KEY_SPACE);context.waitTicks(15);
        colorFocus(context,probe,"Probe, end color");key(context,InputConstants.KEY_SPACE);
        colorFocus(context,probe,"Probe, end, hex AARRGGBB, text");key(context,InputConstants.KEY_RETURN);
        type(context,"FF778899");
        key(context,InputConstants.KEY_RETURN);
        context.runOnClient(mc->check(probe.setting.getSecondary()==0xFF778899&&probe.setting.getValue()==0xFF112233,"Keyboard selects and edits end color without changing start"));
        colorFocus(context,probe,"Probe, start color");key(context,InputConstants.KEY_SPACE);
        colorFocus(context,probe,"Probe, color mode");
        var visited=new java.util.HashSet<dev.riserecode.setting.ColorSetting.Mode>();
        for(int i=0;i<dev.riserecode.setting.ColorSetting.Mode.values().length;i++){key(context,InputConstants.KEY_RIGHT);visited.add(context.computeOnClient(mc->probe.setting.getMode()));}
        check(visited.size()==dev.riserecode.setting.ColorSetting.Mode.values().length,"Keyboard reaches every color mode");
        colorFocus(context,probe,"Probe, gradient direction");var previous=context.computeOnClient(mc->probe.setting.getDirection());key(context,InputConstants.KEY_RIGHT);
        check(context.computeOnClient(mc->probe.setting.getDirection())!=previous,"Keyboard adjusts gradient direction");context.takeScreenshot("review-color-keyboard");
        context.runOnClient(mc->{probe.setting.setMode(dev.riserecode.setting.ColorSetting.Mode.SOLID);probe.setting.setSpeed(0);});
        context.waitTicks(3);colorPixels(context,probe,"solid");
        context.runOnClient(mc->{probe.setting.setMode(dev.riserecode.setting.ColorSetting.Mode.GRADIENT);probe.setting.setSpacing(1000);probe.setting.setDirection(dev.riserecode.util.ColorUtil.GradientDirection.HORIZONTAL);});
        context.waitTicks(3);colorPixels(context,probe,"gradient");
        context.runOnClient(mc->mc.gui.setScreen(null));
    }

    private static void colorPixels(ClientGameTestContext context,ColorProbe probe,String state){
        var image=new java.util.concurrent.atomic.AtomicReference<com.mojang.blaze3d.platform.NativeImage>();
        context.runOnClient(mc->net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image::set));
        context.waitFor(mc->image.get()!=null,200);
        try(var pixels=image.get()){
            double scale=context.computeOnClient(mc->pixels.getWidth()/(double)mc.getWindow().getGuiScaledWidth());
            int x=(int)(298*scale),y=(int)(29*scale),actual=pixels.getPixel(x,y);
            int expected=context.computeOnClient(mc->probe.setting.paint().sample((x+.5)/scale,(y+.5)/scale,0));
            boolean matches=true;for(int shift:new int[]{0,8,16}){matches&=Math.abs((actual>>shift&255)-(expected>>shift&255))<=3;}
            check(matches,"State-controlled "+state+" header swatch matches paint: actual="+Integer.toHexString(actual)+", expected="+Integer.toHexString(expected));
            int bright=0;
            for(int py=(int)(171*scale);py<(int)(182*scale);py++){for(int px=(int)(22*scale);px<(int)(170*scale);px++){
                int rgb=pixels.getPixel(px,py);if((rgb>>16&255)>180&&(rgb>>8&255)>180&&(rgb&255)>180){bright++;}
            }}
            check(bright>8,"Direction control uses readable active text rather than dim placeholder color, "+state);
            Client.INSTANCE.getLogger().info("Color visual oracle {} swatch={} expected={} brightDirectionPixels={}",state,Integer.toHexString(actual),Integer.toHexString(expected),bright);
        }
        context.takeScreenshot("review-color-"+state+"-verified");
    }

    static void type(ClientGameTestContext context,String value) {
        context.getInput().typeChars(value);context.waitTicks(2);
    }
    private static void configurationFlows(ClientGameTestContext context) {
        String name="ReviewKeyboard";
        context.runOnClient(mc->{gui().open(null);gui().selectPage(ClickGui.Page.PROFILES);});context.waitTicks(20);
        focus(context,"Enter a profile name...");key(context,InputConstants.KEY_RETURN);type(context,name);
        focus(context,"Save client profile, button");key(context,InputConstants.KEY_RETURN);
        var manager=Client.INSTANCE.getProfileManager();
        check(java.nio.file.Files.isRegularFile(manager.getFile(name)),"Keyboard saves named client profile");
        var module=Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
        double saved=context.computeOnClient(mc->module.getBrightness().getValue());
        context.runOnClient(mc->module.getBrightness().setValue(.1));
        focus(context,"Client: "+name+", button");key(context,InputConstants.KEY_RETURN);
        context.waitFor(mc->Math.abs(module.getBrightness().getValue()-saved)<.0001,240);
        check(true,"Keyboard reloads saved client profile");
        focus(context,"Delete client: "+name+", button");key(context,InputConstants.KEY_RETURN);
        context.waitFor(mc->mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen,240);
        nativeFocus(context,"Cancel");key(context,InputConstants.KEY_RETURN);
        context.waitFor(mc->mc.gui.screen()==gui(),240);
        check(java.nio.file.Files.exists(manager.getFile(name)),"Keyboard cancellation preserves profile");
        focus(context,"Delete client: "+name+", button");key(context,InputConstants.KEY_RETURN);
        context.waitFor(mc->mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen,240);
        nativeFocus(context,"Delete");key(context,InputConstants.KEY_RETURN);
        context.waitFor(mc->!java.nio.file.Files.exists(manager.getFile(name))&&mc.gui.screen()==gui(),240);
        check(manager.getActiveProfile()==null,"Keyboard confirmation deletes profile and clears active pointer");
        context.runOnClient(mc->gui().selectPage(ClickGui.Page.FRIENDS));context.waitTicks(20);
        focus(context,"Enter a player name...");key(context,InputConstants.KEY_RETURN);type(context,"ReviewTester");
        focus(context,"Add, button");key(context,InputConstants.KEY_RETURN);
        check(Client.INSTANCE.getFriendManager().isFriend("ReviewTester"),"Keyboard adds named friend");
        focus(context,"ReviewTester  ×, button");key(context,InputConstants.KEY_RETURN);
        check(!Client.INSTANCE.getFriendManager().isFriend("ReviewTester"),"Keyboard removes named friend");
        context.runOnClient(mc->mc.gui.setScreen(null));
    }
    private static void auxiliaryFocus(ClientGameTestContext context,String prefix) {
        for(int i=0;i<100;i++) {
            key(context,InputConstants.KEY_TAB);
            if(context.computeOnClient(mc->{var nav=dev.riserecode.ui.component.KeyboardNavigation.auxiliary(mc.gui.screen());return nav!=null&&nav.focusedLabel().startsWith(prefix);})){return;}
        }
        throw new AssertionError("Auxiliary keyboard cannot reach "+prefix);
    }
    private static void nativeFocus(ClientGameTestContext context,String name) {
        for(int i=0;i<12;i++){
            if(context.computeOnClient(mc->mc.gui.screen().getFocused() instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.getMessage().getString().equals(name))){return;}
            key(context,InputConstants.KEY_TAB);
        }
        throw new AssertionError("Cannot focus native confirmation button "+name);
    }
    private static void hudControls(ClientGameTestContext context) {
        var hud=Client.INSTANCE.getHudRenderer();
        context.runOnClient(mc->mc.gui.setScreen(new dev.riserecode.ui.hud.HudEditorScreen(hud)));context.waitTicks(5);
        auxiliaryFocus(context,"HUD element, Watermark");key(context,InputConstants.KEY_RETURN);context.waitTicks(15);
        var element=hud.getElements().stream().filter(e->e.id().equals("watermark")).findFirst().orElseThrow();
        context.runOnClient(mc->check(((dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen()).getSelected()==element,"Keyboard selects HUD canvas element"));
        auxiliaryFocus(context,"Layout & Style, section");
        if(context.computeOnClient(mc->!((dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen()).getSettingsSection().isExpanded())){key(context,InputConstants.KEY_SPACE);context.waitTicks(15);}
        auxiliaryFocus(context,"Position X, slider");double before=element.layout().x();key(context,InputConstants.KEY_RIGHT);
        check(element.layout().x()>before,"Keyboard moves selected HUD element through layout control");
        key(context,InputConstants.KEY_ESCAPE);context.runOnClient(mc->check(!(mc.gui.screen() instanceof dev.riserecode.ui.hud.HudEditorScreen),"Keyboard saves and leaves HUD canvas"));
    }
    private static double number(Object owner,String name) {
        try{var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.getDouble(owner);}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static void revealFocus(ClientGameTestContext context,String prefix) {
        for(int page=0;page<16;page++) {
            if(context.computeOnClient(mc->dev.riserecode.ui.component.KeyboardNavigation.auxiliary(mc.gui.screen()).labels().stream().anyMatch(label->label.startsWith(prefix)))) {auxiliaryFocus(context,prefix);return;}
            key(context,InputConstants.KEY_PAGEDOWN);
        }
        throw new AssertionError("Cannot reveal "+prefix);
    }
    private static void glassScrolling(ClientGameTestContext context) {
        var glass=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.GlassModule.class).orElseThrow();
        for(int[] size:new int[][]{{2000,1200},{1280,800}}) {
            context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(4);
            var screen=context.computeOnClient(mc->mc.gui.screen());
            context.runOnClient(mc->((dev.riserecode.ui.component.KeyboardNavigation.PageScrollable)screen).scrollPage(-100));context.waitTicks(3);
            double zoom=context.computeOnClient(mc->number(screen,"zoom"));
            revealFocus(context,"Shared color, color,");key(context,InputConstants.KEY_SPACE);context.waitTicks(15);
            revealFocus(context,"Shared enchantment glint, toggle");
            boolean before=glass.shared.glint.getValue();key(context,InputConstants.KEY_SPACE);
            check(glass.shared.glint.getValue()!=before,"Keyboard reaches and edits final expanded material setting at "+size[0]+" pixels");
            double scroll=context.computeOnClient(mc->number(screen,"scroll"));
            check(scroll>0&&context.computeOnClient(mc->number(screen,"zoom"))==zoom,"Page navigation moves Glass settings while preview zoom stays unchanged");
            key(context,InputConstants.KEY_PAGEUP);
            check(context.computeOnClient(mc->number(screen,"scroll"))<scroll,"Page Up reverses Glass settings scroll");
        }
        context.getInput().resizeWindow(2048,1152);context.waitTicks(4);
    }
    private static void auxiliaryControls(ClientGameTestContext context) {
        var parent=context.computeOnClient(mc->new ColorProbe());
        context.runOnClient(mc->{var glass=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.GlassModule.class).orElseThrow();mc.gui.setScreen(new dev.riserecode.ui.component.GlassEditorScreen(glass,parent));});context.waitTicks(20);
        auxiliaryFocus(context,"Glass preview, Rotation");
        String previous=context.computeOnClient(mc->dev.riserecode.ui.component.KeyboardNavigation.auxiliary(mc.gui.screen()).focusedLabel());
        key(context,InputConstants.KEY_RIGHT);
        check(!previous.equals(context.computeOnClient(mc->dev.riserecode.ui.component.KeyboardNavigation.auxiliary(mc.gui.screen()).focusedLabel())),"Auxiliary preview rotation responds through real keyboard hook");
        glassScrolling(context);
        auxiliaryFocus(context,"Done, button");key(context,InputConstants.KEY_RETURN);
        context.runOnClient(mc->check(mc.gui.screen()==parent,"Keyboard returns from auxiliary Glass editor"));
        var blocks=new dev.riserecode.setting.BlockListSetting("Probe blocks","",java.util.List.of());
        context.runOnClient(mc->mc.gui.setScreen(new dev.riserecode.ui.component.setting.BlockPickerScreen(parent,blocks,()->{})));context.waitTicks(3);
        auxiliaryFocus(context,"minecraft:acacia_button, block");key(context,InputConstants.KEY_SPACE);
        check(blocks.getValue().contains("minecraft:acacia_button"),"Keyboard selects auxiliary registry row");
        key(context,InputConstants.KEY_ESCAPE);context.runOnClient(mc->check(mc.gui.screen()==parent,"Keyboard returns from auxiliary block picker"));
    }

}
