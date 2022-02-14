package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Resounding;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundEngine;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "init", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/sound/AlUtil;checkErrors(Ljava/lang/String;)Z", ordinal = 0))
    private void ResoundingStartInjector(CallbackInfo ci){ Resounding.start(); }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcDestroyContext(J)V", ordinal = 0))
    private void ResoundingStopInjector(CallbackInfo ci){ Resounding.stop(); }
/*
    @Inject(method = "isDeviceUnavailable", at = @At(value = "RETURN"), cancellable = true) // TODO: figure out a better way to do this
    private void DeviceCheckFix(@NotNull CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(false);
        cir.cancel();
    }
 */
}
