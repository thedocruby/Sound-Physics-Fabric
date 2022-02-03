package dev.thedocruby.resounding;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.sound.SoundCategory;

@Environment(EnvType.CLIENT)
public interface SourceAccessor {
    void calculateReverb(SoundInstance sound,SoundListener listener);
}
