package com.thedocruby.soundphysics;

import net.minecraft.sound.SoundCategory;

public interface SourceAccessor {
    void calculateReverb(SoundCategory category, String name);
}
