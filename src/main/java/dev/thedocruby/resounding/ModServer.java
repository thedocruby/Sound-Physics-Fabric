package dev.thedocruby.resounding;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;

public class ModServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Engine.env = EnvType.SERVER;
        ConfigManager.registerAutoConfig();
    }
}

