package dev.riserecode.event;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Cancellable intent before any AttackManager session's ordinary game-mode attack. */
public final class AttackEvent extends Event {
    private final Player attacker;
    private final LivingEntity target;
    public AttackEvent(Player attacker, LivingEntity target) {
        super(true); this.attacker = Objects.requireNonNull(attacker); this.target = Objects.requireNonNull(target);
    }
    public Player getAttacker() { return attacker; }
    public LivingEntity getTarget() { return target; }
}
