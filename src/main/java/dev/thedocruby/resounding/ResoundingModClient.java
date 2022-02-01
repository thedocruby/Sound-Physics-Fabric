package dev.thedocruby.resounding;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ResoundingModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Resounding.env = EnvType.CLIENT;
        ConfigManager.registerAutoConfig();
    }
}
