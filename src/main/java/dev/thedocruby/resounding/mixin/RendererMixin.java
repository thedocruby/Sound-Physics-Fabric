package dev.thedocruby.resounding.mixin;

import dev.thedocruby.resounding.raycast.RaycastRenderer;
import dev.thedocruby.resounding.Resounding;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(DebugRenderer.class)
public class RendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDD)V", at = @At("HEAD"))
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!Resounding.isActive) return;
        RaycastRenderer.renderRays(cameraX, cameraY, cameraZ, MinecraftClient.getInstance().world);
    }

}