package com.thedocruby.soundphysics.mixin.server;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.thedocruby.soundphysics.config.PrecomputedConfig.soundDistanceAllowance;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @ModifyArg(method = {"playSound","playSoundFromEntity"}, at = @At(value = "INVOKE", target = "net/minecraft/server/PlayerManager.sendToAround (Lnet/minecraft/entity/player/PlayerEntity;DDDDLnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/network/Packet;)V"),index = 4)
    private double SoundDistanceModifierInjector(double d){
        return d * soundDistanceAllowance;
    }

}
