package dev.thedocruby.resounding.mixin.server;

import dev.thedocruby.resounding.config.PrecomputedConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow
    public int getViewDistance() {
        return 0;
    }

    @Shadow
    public int getSimulationDistance() {
        return 0;
    }

    @Inject(method = {"sendToAround(Lnet/minecraft/entity/player/PlayerEntity;DDDDLnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/network/Packet;)V"}, at = @At(value = "HEAD"))
    private void SoundDistanceModifierInjector(PlayerEntity player, double x, double y, double z, double distance, RegistryKey<World> worldKey, Packet<?> packet, CallbackInfo ci) {

        distance = (packet instanceof PlaySoundS2CPacket || packet instanceof PlaySoundFromEntityS2CPacket) ? Math.min(distance * PrecomputedConfig.pC.soundSimulationDistance, 16 * Math.min(getViewDistance(), getSimulationDistance()) ) : distance;
    }
}
