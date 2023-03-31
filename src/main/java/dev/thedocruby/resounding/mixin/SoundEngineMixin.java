package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import dev.thedocruby.resounding.openal.Context;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.thedocruby.resounding.Engine.LOGGER;
import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Environment(EnvType.CLIENT)
@Mixin(SoundEngine.class)
public class SoundEngineMixin {
	@Inject(method = "init", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/sound/AlUtil;checkErrors(Ljava/lang/String;)Z", ordinal = 0))
	private void resoundingStartInjector(CallbackInfo ci){
		assert Engine.isOff;
		if (!pC.enabled) {
			LOGGER.info("Resounding disabled.");
			Engine.isOff = true;
			return;
		}
		LOGGER.info("Starting Resounding engine...");
		Engine.setRoot(new Context());
		if (!Engine.root.setup("Base Game")) {
			LOGGER.info("Failed to prime OpenAL EFX for Resounding effects. Resounding disabled.");
			Engine.isOff = true;
			return;
		}
		LOGGER.info("OpenAL EFX successfully primed for Resounding effects");
		if (ConfigManager.resetOnReload) {
			ConfigManager.resetToDefault();
			ConfigManager.resetOnReload = false;
		}
		Engine.mc = MinecraftClient.getInstance();
		Engine.updateRays();
		Engine.isOff = false;
	}

	@Inject(method = "close", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcDestroyContext(J)V", ordinal = 0))
	private void resoundingStopInjector(CallbackInfo ci){
		if (Engine.isOff) return;
		LOGGER.info("Stopping Resounding engine...");
		if (!Engine.root.clean(false)) LOGGER.info("Failed to (fully) clean OpenAL Context(s).");
		Engine.mc = null;
		Engine.isOff = true;
	}
}
