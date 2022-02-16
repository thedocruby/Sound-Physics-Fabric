package dev.thedocruby.resounding;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;

public class ResoundingModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Resounding.env = EnvType.CLIENT;
        ConfigManager.registerAutoConfig();
    }
}
