package dev.riserecode.ui.component;

/** One header row shared by its label/input and right-aligned actions. */
public record HeaderLayout(UIComponent.Bounds label, UIComponent.Bounds gear, UIComponent.Bounds action) {
    public static HeaderLayout at(double x, double y, double width, double height, double actionWidth) {
        double gap = 6;
        var gear = new UIComponent.Bounds(x + width - height, y, height, height);
        double labelRight = gear.x() - gap;
        UIComponent.Bounds action = null;
        if (actionWidth > 0) {
            actionWidth = Math.min(actionWidth, Math.max(0, width - height - 2 * gap - 32));
            action = new UIComponent.Bounds(labelRight - actionWidth, y, actionWidth, height);
            labelRight = action.x() - gap;
        }
        return new HeaderLayout(new UIComponent.Bounds(x, y, Math.max(0, labelRight - x), height), gear, action);
    }
}
