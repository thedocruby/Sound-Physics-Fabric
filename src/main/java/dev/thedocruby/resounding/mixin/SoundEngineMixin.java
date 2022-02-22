package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.ResoundingEngine;
import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
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
    private void resoundingStartInjector(CallbackInfo ci){
        if (!ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine has already been started! You may need to reload the sound system using SoundManager.reloadSounds()");
        if (!pC.enabled){
            ResoundingEngine.LOGGER.info("Skipped starting Resounding engine: disabled in config.");
            ResoundingEngine.isOff = true;
            return;
        }
        ResoundingEngine.LOGGER.info("Starting Resounding engine...");
        if (!ResoundingEFX.setUpEXTEfx()) {
            ResoundingEngine.LOGGER.info("Failed to prime OpenAL EFX for Resounding effects. ResoundingEngine will not be active.");
            ResoundingEngine.isOff = true;
            return;
        }
        ResoundingEngine.LOGGER.info("OpenAL EFX successfully primed for Resounding effects");
        if (ConfigManager.resetOnReload){
            ConfigManager.resetToDefault();
            ConfigManager.resetOnReload = false;
        }
        ResoundingEngine.mc = MinecraftClient.getInstance();
        ResoundingEngine.updateRays();
        ResoundingEngine.isOff = false;
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcDestroyContext(J)V", ordinal = 0))
    private void resoundingStopInjector(CallbackInfo ci){
        if (ResoundingEngine.isOff) return;
        ResoundingEngine.LOGGER.info("Stopping Resounding engine...");
        ResoundingEFX.cleanUpEXTEfx();
        ResoundingEngine.mc = null;
        ResoundingEngine.isOff = true;
    }
}
