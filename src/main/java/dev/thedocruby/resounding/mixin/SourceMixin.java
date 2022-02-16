package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.toolbox.SourceAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.client.sound.Source;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin(Source.class)
public class SourceMixin implements SourceAccessor {

    @Shadow
    @Final
    private int pointer;

    private Vec3d pos;

    @Inject(method = "setPosition", at = @At("HEAD"))
    private void soundPosStealer(Vec3d poss, CallbackInfo ci) { if (!Resounding.isActive) return; this.pos = poss; }

    @Inject(method = "play", at = @At("HEAD"))
    private void onPlaySoundInjector(CallbackInfo ci) {
        if (!Resounding.isActive) return;
        Resounding.playSound(pos.x, pos.y, pos.z, pointer, false);
    }

    public void calculateReverb(SoundInstance sound, SoundListener listener) {
        if (!Resounding.isActive) return;
        Resounding.updateYeetedSoundInfo(sound, listener);
        Resounding.playSound(pos.x, pos.y, pos.z, pointer, false);
    }
}
