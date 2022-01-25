package com.thedocruby.soundphysics.mixin.server;

import com.thedocruby.soundphysics.config.BlueTapePack.ConfigManager;
import net.minecraft.server.command.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {
    @Inject(method = "method_13530(Lcom/mojang/brigadier/context/CommandContext;)I", at = @At("HEAD"), remap = false)
    private static void reloadConfigHook(CallbackInfoReturnable<Integer> cir) {
        ConfigManager.reload(true);
    }
}
