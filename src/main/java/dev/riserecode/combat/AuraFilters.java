package dev.riserecode.combat;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Original RiseCode selection helpers; Meteor is a behavioral reference only. */
public final class AuraFilters {
    private AuraFilters() { }
    public static Set<String> words(String value) {
        return Stream.of(value.toLowerCase(Locale.ROOT).strip().split("[ ,]+"))
                .filter(word -> !word.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
    public static boolean weapon(ItemStack stack, Set<String> families) {
        return (families.contains("sword") && stack.is(ItemTags.SWORDS))
                || (families.contains("axe") && stack.is(ItemTags.AXES))
                || (families.contains("pickaxe") && stack.is(ItemTags.PICKAXES))
                || (families.contains("shovel") && stack.is(ItemTags.SHOVELS))
                || (families.contains("hoe") && stack.is(ItemTags.HOES))
                || (families.contains("spear") && stack.is(ItemTags.SPEARS))
                || (families.contains("mace") && stack.getItem() instanceof MaceItem)
                || (families.contains("trident") && stack.getItem() instanceof TridentItem);
    }
    public static double distanceSquared(Vec3 point, AABB box) {
        double x = point.x - Math.clamp(point.x, box.minX, box.maxX);
        double y = point.y - Math.clamp(point.y, box.minY, box.maxY);
        double z = point.z - Math.clamp(point.z, box.minZ, box.maxZ);
        return x * x + y * y + z * z;
    }
}
