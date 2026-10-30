package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.gameplay.TrajectoryPrediction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * Releases a bow you are already drawing. The server still measures the draw time itself, so the shot is only
 * released once the bow is really at the chosen power; a bow cannot be fired at full power without drawing it.
 */
public final class AutoBowModule extends Module {
    public enum Mode { FULL_CHARGE, ON_TARGET }
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Release at full power, or only when the shot would hit a selected target", Mode.ON_TARGET));
    private final dev.riserecode.setting.DoubleSetting minCharge = register(new dev.riserecode.setting.DoubleSetting("Min Charge", "Bow power in percent at which it shoots (100 = fully drawn)", 100, 10, 100));
    private final dev.riserecode.setting.BlockListSetting targets = register(new dev.riserecode.setting.BlockListSetting("Targets", "Mobs to shoot at (Target mode only)", defaultTargets()).kind(dev.riserecode.setting.BlockListSetting.Kind.ENTITY));
    private final BooleanSetting ignoreFriends = register(new BooleanSetting("Ignore Friends", "Never auto-shoot at friends", true));
    private final Minecraft mc;
    private final FriendManager friends;
    private final TrajectoriesModule trajectories;

    public AutoBowModule(EventBus bus, Minecraft mc, FriendManager friends, TrajectoriesModule trajectories) {
        super("AutoBow", "Releases a fully drawn bow for you, optionally only when the shot would hit.", Category.COMBAT, bus);
        this.mc = mc; this.friends = friends; this.trajectories = trajectories;
        for (var setting : java.util.List.of(targets, ignoreFriends)) { setting.setVisibleWhen(() -> mode.getValue() == Mode.ON_TARGET); }
    }

    @Override protected void onEnable() { listen(UpdateEvent.class, event -> update()); }

    private void update() {
        var player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) { return; }
        if (!player.isUsingItem() || !player.getUseItem().is(Items.BOW) || net.minecraft.world.item.BowItem.getPowerForTime(player.getTicksUsingItem()) * 100 < minCharge.getValue() - 0.001) { return; }
        if (mode.getValue() == Mode.ON_TARGET && !wouldHitTarget(player)) { return; }
        mc.gameMode.releaseUsingItem(player);
    }

    private boolean wouldHitTarget(Player player) {
        for (var projectile : trajectories.launch(player.getUseItem(), 1)) {
            var path = TrajectoryPrediction.predict(mc.level, projectile, trajectories.length.getValue().intValue());
            if (path.target() != null && wanted(path.target())) { return true; }
        }
        return false;
    }

    private static java.util.List<String> defaultTargets() {
        var ids = new java.util.ArrayList<String>();
        ids.add("minecraft:player");
        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER && type.canSummon()) { ids.add(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()); }
        });
        return ids;
    }

    private boolean wanted(Entity entity) {
        if (entity instanceof Player other && ignoreFriends.getValue() && friends.isFriend(other.getUUID(), other.getGameProfile().name())) { return false; }
        return targets.getValue().contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
}
