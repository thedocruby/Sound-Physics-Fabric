package dev.thedocruby.resounding.toolbox;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;

@Environment(EnvType.CLIENT)
public interface SourceAccessor {
    void calculateReverb(SoundInstance sound,SoundListener listener);
}
