package dev.riserecode.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** One live native menu owner across a retained child chain. */
public final class NativeScreenRetention {
    private static Screen owner;
    private NativeScreenRetention(){}
    public static void suspend(Screen screen){if(screen instanceof AbstractContainerScreen<?>){owner=screen;}}
    public static boolean suspends(Screen screen){return screen!=null&&owner==screen;}
    public static void resume(Screen screen){if(owner==screen){owner=null;}}
    public static boolean valid(Screen screen){var mc=Minecraft.getInstance();return !(screen instanceof AbstractContainerScreen<?> container)||mc.player!=null&&mc.player.isAlive()&&mc.player.containerMenu==container.getMenu();}
    public static void abandon(Screen screen){if(owner!=screen||screen==null){return;}owner=null;var mc=Minecraft.getInstance();if(screen instanceof AbstractContainerScreen<?> container&&mc.player!=null&&mc.player.containerMenu==container.getMenu()){mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(container.getMenu().containerId));mc.player.containerMenu=mc.player.inventoryMenu;}screen.removed();}
}
