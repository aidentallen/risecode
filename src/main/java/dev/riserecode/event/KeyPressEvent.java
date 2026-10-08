package dev.riserecode.event;

import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Raw keyboard input. Cancellation consumes the original Minecraft action. */
public final class KeyPressEvent extends Event {
    private final KeyEvent input;
    private final int action;

    public KeyPressEvent(KeyEvent input, int action) {
        super(true);
        this.input = input;
        this.action = action;
    }

    public KeyEvent getInput() { return input; }
    public int getKey() { return input.key(); }
    public int getAction() { return action; }
    public boolean isPress() { return action == GLFW.GLFW_PRESS; }
    public boolean isRelease() { return action == GLFW.GLFW_RELEASE; }
    public boolean isRepeat() { return action == GLFW.GLFW_REPEAT; }
}
