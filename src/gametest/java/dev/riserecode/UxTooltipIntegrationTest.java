package dev.riserecode;

import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.font.StyledText;
import dev.riserecode.ui.menu.*;
import dev.riserecode.ui.theme.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.network.chat.*;
import java.util.*;

/** Native tooltip geometry, styled wrapping, retained full details and live-menu ownership. */
public final class UxTooltipIntegrationTest {
    private static int checks;
    public static boolean containerTooltip;
    private static final List<Component> LONG=List.of(Component.literal("Styled Ω 中 ").withStyle(net.minecraft.ChatFormatting.AQUA).append(Component.literal("bold context").withStyle(net.minecraft.ChatFormatting.BOLD)),Component.literal("Long exact path: /tmp/"+"unbroken_segment_".repeat(70)+"/final-cause.png"));
    public static List<Component> content(){return LONG;}
    private static String full(){return LONG.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void number(String name,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value);}
    private static void key(ClientGameTestContext c,int key){c.getInput().pressKey(key);c.waitTicks(2);}
    private static final class Probe extends Screen {
        private Probe(){super(Component.literal("Tooltip viewport probe"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);g.setTooltipForNextFrame(font,LONG,Optional.empty(),width-3,height-3);}
    }
    private static void geometry(net.minecraft.client.Minecraft mc,String label){var p=TooltipPresentation.last();var b=TooltipPresentation.bounds();check(p!=null&&b.x()>=0&&b.y()>=0&&b.x()+b.width()<=mc.gui.screen().width&&b.y()+b.height()<=mc.gui.screen().height,label+": complete displayed text box fits native viewport");}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);c.getInput().resizeWindow(960,720);c.runOnClient(mc->{mc.options.guiScale().set(3);Client.INSTANCE.getThemeManager().reset();((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);});
        for(double font:new double[]{.75,1,2}){
            c.runOnClient(mc->{number("Font Scale",font);mc.gui.setScreen(new Probe());});c.waitTicks(5);
            var prepared=c.computeOnClient(mc->{geometry(mc,"font "+font);var p=TooltipPresentation.last();check(p.overflow()&&p.fullText().equals(full()),"Long preview preserves complete source and signals full details");check(p.height()<=mc.gui.screen().height*.42+1,"Tooltip does not occupy most of the small viewport");check(p.components().stream().filter(TooltipMotion.Text.class::isInstance).map(TooltipMotion.Text.class::cast).flatMap(t->t.risecode$layout().runs().stream()).anyMatch(r->r.style().isBold()),"Wrapped preview retains bold/color style runs");var styled=StyledText.of(LONG.getFirst().copy().append(Component.literal(" abcabcabc "+"👩‍💻".repeat(4))));var wrapped=styled.wrapWords(mc.font,80);check(wrapped.stream().map(StyledText::plain).collect(java.util.stream.Collectors.joining()).equals(styled.plain()),"Styled Unicode wrapping preserves every character");check(wrapped.stream().allMatch(line->line.width(mc.font)<=80+.01),"Every styled line obeys measured width");return p;});c.waitTicks(3);c.runOnClient(mc->check(TooltipPresentation.last()==prepared,"Unchanged tooltip reuses measured layout"));c.takeScreenshot("ux11-tooltip-compact-font-"+font);
            key(c,290);c.runOnClient(mc->check(mc.gui.screen() instanceof InfoScreen&&((InfoScreen)mc.gui.screen()).text().equals(full()),"Native F1 opens retained complete text"));for(int i=0;i<50&&c.computeOnClient(mc->((InfoScreen)mc.gui.screen()).scrollPosition()<((InfoScreen)mc.gui.screen()).maximumScroll());i++)key(c,267);c.takeScreenshot("ux11-tooltip-full-last-"+font);key(c,256);c.runOnClient(mc->check(mc.gui.screen() instanceof Probe,"Full tooltip returns to its caller"));
        }
        c.runOnClient(mc->{var marker=new ClientTooltipComponent(){@Override public int getWidth(Font f){return 32;}@Override public int getHeight(Font f){return 16;}};var text=ClientTooltipComponent.create(Component.literal("Image caption").getVisualOrderText());var mixed=TooltipPresentation.prepare(mc.font,List.of(text,marker),320,240);check(mixed.components().contains(marker)&&!mixed.overflow(),"Native nontext tooltip components retain identity and are not silently truncated");});
        for(double scale:new double[]{.75,1,1.2}){
            c.getInput().resizeWindow(1280,960);c.runOnClient(mc->{mc.options.guiScale().set(2);number("Font Scale",2);number("Interface Scale",scale);number("Window Width",360);number("Window Height",240);var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("AutoTool");gui.getSearchField().setFocused(false);});c.waitTicks(8);
            double[] point=c.computeOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getModuleButton("AutoTool").getBounds();return new double[]{(b.x()+15)*gui.getCanvasScale()*mc.getWindow().getGuiScale(),(b.y()+25)*gui.getCanvasScale()*mc.getWindow().getGuiScale()};});c.getInput().setCursorPos(point[0],point[1]);c.waitTicks(8);c.runOnClient(mc->{geometry(mc,"client canvas "+scale);check(TooltipPresentation.last().fullText().contains("fastest suitable mining tool"),"Actual module hover uses responsive shared tooltip");});c.takeScreenshot("ux11-module-tooltip-canvas-"+scale);
        }
        c.runOnClient(mc->mc.gui.setScreen(null));
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(p->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(p.nameAndId())),"Tooltip lifecycle world has only owner");CameraFovClientTest.ownerOnlyShutdown=true;var p=server.getPlayerList().getPlayers().getFirst();p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->net.minecraft.world.inventory.ChestMenu.threeRows(id,inv,new net.minecraft.world.SimpleContainer(27)),Component.literal("Tooltip chest")));p.containerMenu.setCarried(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,3));p.containerMenu.broadcastChanges();});c.waitFor(mc->mc.gui.screen() instanceof AbstractContainerScreen<?> &&mc.player.containerMenu.getCarried().getCount()==3,200);
            var chest=c.computeOnClient(mc->(AbstractContainerScreen<?>)mc.gui.screen());c.runOnClient(mc->{Client.INSTANCE.getThemeManager().page(MenuTheme.pageId(chest)).style.setValue(PageAppearance.ScreenStyle.RISECODE);containerTooltip=true;});c.waitTicks(4);key(c,290);c.runOnClient(mc->check(mc.gui.screen() instanceof InfoScreen&&NativeScreenRetention.suspends(chest)&&mc.player.containerMenu==chest.getMenu()&&chest.getMenu().getCarried().getCount()==3,"Full tooltip child retains actual native menu and carried stack"));c.takeScreenshot("ux11-container-full-tooltip");key(c,256);c.runOnClient(mc->check(mc.gui.screen()==chest&&!NativeScreenRetention.suspends(chest)&&chest.getMenu().getCarried().getCount()==3,"Returning full tooltip resumes live container"));c.waitTicks(3);key(c,290);world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().closeContainer());c.waitFor(mc->mc.gui.screen()==null&&mc.player.containerMenu==mc.player.inventoryMenu,200);c.runOnClient(mc->check(!NativeScreenRetention.suspends(chest),"Remote menu closure while full tooltip is open clears retention"));
        }finally{containerTooltip=false;CameraFovClientTest.ownerOnlyShutdown=false;}
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});Client.INSTANCE.getLogger().info("UX responsive tooltip and native details checks passed: {}",checks);
    }
}
