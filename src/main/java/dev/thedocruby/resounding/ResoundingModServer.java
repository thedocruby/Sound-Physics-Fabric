package dev.thedocruby.resounding;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;

public class ResoundingModServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Resounding.env = EnvType.SERVER;
        ConfigManager.registerAutoConfig();
    }
}