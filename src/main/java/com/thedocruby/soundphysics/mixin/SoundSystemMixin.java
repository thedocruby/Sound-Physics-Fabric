package com.thedocruby.soundphysics.mixin;

import com.thedocruby.soundphysics.ALstuff.SPEfx;
import com.thedocruby.soundphysics.SoundPhysics;
import com.thedocruby.soundphysics.SourceAccessor;
import com.thedocruby.soundphysics.config.PrecomputedConfig;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

import static com.thedocruby.soundphysics.SoundPhysics.mc;
import static com.thedocruby.soundphysics.config.PrecomputedConfig.pC;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "start", at = @At(value = "INVOKE", target = "net/minecraft/client/sound/SoundListener.init ()V"))
    private void SoundPhysicsInitInjector(CallbackInfo ci){
            SoundPhysics.init();
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "FIELD", target = "net/minecraft/client/sound/SoundSystem.sounds : Lcom/google/common/collect/Multimap;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void SoundInfoStealer(SoundInstance sound, CallbackInfo ci, WeightedSoundSet weightedSoundSet, Identifier identifier, Sound sound2, float f, float g, SoundCategory soundCategory){
        SoundPhysics.setLastSoundCategoryAndName(soundCategory, sound.getId().getPath());
    }

    @Inject(method = "tick()V", at = @At(value = "HEAD"))
    private void Ticker(CallbackInfo ci){SPEfx.updateSmoothedRain();}

    @ModifyArg(method = "getAdjustedVolume", at = @At(value = "INVOKE", target = "net/minecraft/util/math/MathHelper.clamp (FFF)F"), index = 0)
    private float VolumeMultiplierInjector(float vol){
        return vol * PrecomputedConfig.globalVolumeMultiplier;
    }
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "tick()V", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 3), locals = LocalCapture.CAPTURE_FAILHARD)
    private void recalculate(CallbackInfo ci, Iterator<?> iterator, Map.Entry<?, ?> entry, Channel.SourceManager f, SoundInstance g, float vec3d){
        if (mc.world != null && mc.world.getTime()%pC.continuousRefreshRate==0){
            f.run((s) -> ((SourceAccessor)s).calculateReverb(g.getCategory(), g.getId().getPath()));
        }
            //((SourceAccessor)null)
    }

}
