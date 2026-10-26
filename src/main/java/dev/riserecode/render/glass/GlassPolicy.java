package dev.riserecode.render.glass;

import java.util.Objects;

/** Shared by gameplay, preview and policy tests. Hand/slot indices are independent of handedness. */
public record GlassPolicy(boolean selfBody, boolean arms, boolean selfArmor, boolean firstItems,
        boolean thirdItems, boolean others, boolean otherBody, boolean otherArmor, boolean otherItems,
        boolean mainHand, boolean offHand, boolean head, boolean chest, boolean legs, boolean feet,
        GlassMaterial shared, GlassMaterial body, GlassMaterial armor, GlassMaterial items) {
    public enum Component { BODY, ARM, ARMOR, ITEM }
    public enum View { WORLD, HAND, PREVIEW }
    public enum Slot { NONE, HEAD, CHEST, LEGS, FEET }
    public GlassPolicy { Objects.requireNonNull(shared);Objects.requireNonNull(body);Objects.requireNonNull(armor);Objects.requireNonNull(items); }
    public GlassMaterial select(boolean self, Component component, View view, boolean main, Slot slot) {
        if (!self && !others) return null;
        boolean selected = switch(component) {
            case BODY -> self ? selfBody : otherBody;
            case ARM -> self && arms && (main ? mainHand : offHand);
            case ARMOR -> (self ? selfArmor : otherArmor) && switch(slot) {
                case HEAD -> head;case CHEST -> chest;case LEGS -> legs;case FEET -> feet;case NONE -> false;
            };
            case ITEM -> (main ? mainHand : offHand) && (self ? view == View.HAND ? firstItems : thirdItems : otherItems);
        };
        return !selected ? null : switch(component) { case BODY,ARM -> body; case ARMOR -> armor; case ITEM -> items; };
    }
}
