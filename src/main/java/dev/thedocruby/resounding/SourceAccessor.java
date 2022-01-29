package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sound.SoundCategory;

@Environment(EnvType.CLIENT)
public interface SourceAccessor {
    void calculateReverb(SoundCategory category, String name);
}
