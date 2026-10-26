package dev.riserecode.event;

/** Adapter for suppressing the vanilla selection edges while a module supplies its own. */
public final class BlockOutlineEvent extends Event {
    public BlockOutlineEvent(){super(true);}
}
