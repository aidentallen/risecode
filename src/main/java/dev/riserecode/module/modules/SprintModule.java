package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SprintModule extends Module {
    private final BooleanSetting omniSprint = register(new BooleanSetting("OmniSprint", "Request sprint while moving in any direction", false));
    private final Minecraft minecraft;
    private LocalPlayer controlled;
    private boolean originalSprint;
    public SprintModule(EventBus bus, Minecraft minecraft) {
        super("Sprint", "Automatically request sprint while moving", Category.MOVEMENT, bus); this.minecraft = minecraft;
    }
    public BooleanSetting getOmniSprint() { return omniSprint; }
    @Override
    protected void onEnable() { listen(UpdateEvent.class, this::update); }
    private void update(UpdateEvent event) {
        LocalPlayer player = minecraft.player;
        if (controlled != player) { controlled = null; }
        if (player == null) { return; }
        boolean moving = omniSprint.getValue() ? player.input.getMoveVector().lengthSquared() > 1.0e-4
                : player.input.hasForwardImpulse();
        boolean eligible = moving && minecraft.gui.screen() == null && !player.isSpectator() && !player.isPassenger()
                && !player.isShiftKeyDown() && !player.isUsingItem() && !player.isFallFlying()
                && (!player.horizontalCollision || player.minorHorizontalCollision)
                && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly);
        if (eligible) {
            if (controlled == null) { controlled = player; originalSprint = player.isSprinting(); }
            player.setSprinting(true);
        } else if (controlled != null) { player.setSprinting(false); }
    }
    @Override
    protected void onDisable() {
        if (controlled != null && controlled == minecraft.player) { controlled.setSprinting(originalSprint); }
        controlled = null;
    }
}
