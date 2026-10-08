package dev.riserecode.render;

import net.minecraft.resources.Identifier;

/** Small adapter exposing the native material without importing another client's renderer. */
public interface TextureSource { Identifier riserecode$texture(); }
