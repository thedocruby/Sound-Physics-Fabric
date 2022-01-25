package com.thedocruby.soundphysics.mixin.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static com.thedocruby.soundphysics.SoundPhysics.stepPattern;

@Mixin(Entity.class)
public class EntityMixin {

    @Shadow @SuppressWarnings("SameReturnValue")
    public float getStandingEyeHeight(){return 0.0f;}

    @ModifyArg(method = "playSound", at = @At(value = "INVOKE", target = "net/minecraft/world/World.playSound (Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"), index = 2)
    private double EyeHeightOffsetInjector(@Nullable PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        return y + calculateEntitySoundOffset(getStandingEyeHeight(),sound);
    }


    private static double calculateEntitySoundOffset(float standingEyeHeight, SoundEvent sound)
    {
        if (stepPattern.matcher(sound.getId().getPath()).matches())
        {
            return 0.01;
        }
        return standingEyeHeight;
    }
}
