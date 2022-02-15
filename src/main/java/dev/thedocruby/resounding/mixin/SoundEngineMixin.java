package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.openal.ResoundingEFX;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(method = "init", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/sound/AlUtil;checkErrors(Ljava/lang/String;)Z", ordinal = 0))
    private void ResoundingStartInjector(CallbackInfo ci){
        if (Resounding.isActive) throw new IllegalStateException("Resounding has already been started! You may need to reload the sound system using SoundManager.reloadSounds()");
        Resounding.isActive = pC.enabled;
        if (!Resounding.isActive){
            Resounding.LOGGER.info("Skipped starting Resounding engine: disabled in config.");
            return;
        }
        Resounding.LOGGER.info("Starting Resounding engine...");
        Resounding.isActive = ResoundingEFX.setUpEXTEfx();
        if (!Resounding.isActive) {
            Resounding.LOGGER.info("Failed to prime OpenAL EFX for Resounding effects. Resounding will not be active.");
            return;
        }
        Resounding.LOGGER.info("OpenAL EFX successfully primed for Resounding effects");
        Resounding.mc = MinecraftClient.getInstance();
        Resounding.updateRays();
        Resounding.isActive = true;
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcDestroyContext(J)V", ordinal = 0))
    private void ResoundingStopInjector(CallbackInfo ci){
        if (!Resounding.isActive) return;
        Resounding.LOGGER.info("Stopping Resounding engine...");
        ResoundingEFX.cleanUpEXTEfx();
        Resounding.mc = null;
        Resounding.isActive = false;
    }
}
