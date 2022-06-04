package dev.thedocruby.resounding;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Engine.env = EnvType.CLIENT;
        ConfigManager.registerAutoConfig();
    }
}
