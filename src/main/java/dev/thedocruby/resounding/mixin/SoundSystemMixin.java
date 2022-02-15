package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.SourceAccessor;
import dev.thedocruby.resounding.config.PrecomputedConfig;
import dev.thedocruby.resounding.effects.AirEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

import static dev.thedocruby.resounding.Resounding.mc;
import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Shadow @Final private SoundListener listener;

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "FIELD", target = "net/minecraft/client/sound/SoundSystem.sounds : Lcom/google/common/collect/Multimap;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void SoundInfoYeeter(SoundInstance sound, CallbackInfo ci, WeightedSoundSet weightedSoundSet, Identifier identifier, Sound sound2, float f, float g, SoundCategory soundCategory){
        if (!Resounding.isActive) return;
        Resounding.updateYeetedSoundInfo(sound, this.listener); // TODO: do this better maybe
    }

    @Inject(method = "tick()V", at = @At(value = "HEAD"))
    private void Ticker(CallbackInfo ci){ AirEffects.updateSmoothedRain(); }

    @ModifyArg(method = "getAdjustedVolume", at = @At(value = "INVOKE", target = "net/minecraft/util/math/MathHelper.clamp (FFF)F"), index = 0)
    private float VolumeMultiplierInjector(float vol){ if (!Resounding.isActive) return vol; return vol * PrecomputedConfig.globalVolumeMultiplier; }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "tick()V", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 3), locals = LocalCapture.CAPTURE_FAILHARD)
    private void recalculate(CallbackInfo ci, Iterator<?> iterator, Map.Entry<?, ?> entry, Channel.SourceManager f, SoundInstance g, float vec3d){
        if (!Resounding.isActive) return;
        if (mc.world != null && mc.world.getTime()%pC.sourceRefreshRate ==0){
            f.run((s) -> ((SourceAccessor)s).calculateReverb(g, this.listener));
        }
            //((SourceAccessor)null)
    }

}
