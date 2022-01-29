package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.ResoundingLog;
import dev.thedocruby.resounding.SourceAccessor;
import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.Source;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin(Source.class)
public class SourceMixin implements SourceAccessor {

    @Shadow
    @Final
    private int pointer;

    private Vec3d pos;

    @Inject(method = "setPosition", at = @At("HEAD"))
    private void SoundPosStealer(Vec3d poss, CallbackInfo ci) {this.pos = poss;}

    @Inject(method = "play", at = @At("HEAD"))
    private void OnPlaySoundInjector(CallbackInfo ci) {
        Resounding.playSound(pos.x, pos.y, pos.z, pointer, false);
        ResoundingLog.checkErrorLog("SourceMixin.onPlaySoundInjector");
    }

    // For sounds unchanged by evaluation (noteblocks, menu, ui)
    @ModifyArg(method = "setAttenuation", at = @At(value = "INVOKE", target = "org/lwjgl/openal/AL10.alSourcef (IIF)V", ordinal = 0, remap = false), index = 2)
    private float AttenuationHijack(int pointer2, int param_id, float attenuation) {
        if (param_id != 4131) throw new IllegalArgumentException("Tried modifying wrong field. No attenuation here.");
        return  attenuation / (float)(ConfigManager.getConfig().General.attenuationFactor);
    }

    public void calculateReverb(SoundCategory category, String name) {
        Resounding.setLastSoundCategoryAndName(category, name);
        Resounding.playSound(pos.x, pos.y, pos.z, pointer, false);
        ResoundingLog.checkErrorLog("SourceMixin.calculateReverb");
    }
}
