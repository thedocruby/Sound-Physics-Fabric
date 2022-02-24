package dev.thedocruby.resounding.mixin.server;

import dev.thedocruby.resounding.ResoundingEngine;
import net.fabricmc.api.EnvType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Shadow @Final
    private MinecraftServer server;

    @ModifyArg(method = {"playSound","playSoundFromEntity"}, at = @At(value = "INVOKE", target = "net/minecraft/server/PlayerManager.sendToAround (Lnet/minecraft/entity/player/PlayerEntity;DDDDLnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/network/Packet;)V"),index = 4)
    private double SoundDistanceModifierInjector(double distance){
        if ((ResoundingEngine.env == EnvType.CLIENT && ResoundingEngine.isOff) || (ResoundingEngine.env == EnvType.SERVER && !pC.enabled)) return distance;
        return Math.min(distance * pC.soundSimulationDistance, 16 * Math.min(this.server.getPlayerManager().getViewDistance(), this.server.getPlayerManager().getSimulationDistance()) );
    }
}
