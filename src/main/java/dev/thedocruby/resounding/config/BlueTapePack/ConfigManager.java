package dev.thedocruby.resounding.config.BlueTapePack;

import dev.thedocruby.resounding.ResoundingEngine;
import dev.thedocruby.resounding.config.PrecomputedConfig;
import dev.thedocruby.resounding.config.ResoundingConfig;
import dev.thedocruby.resounding.config.presets.ConfigPresets;
import dev.thedocruby.resounding.toolbox.MaterialData;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConfigManager {

    private ConfigManager() {}

    private static ConfigHolder<ResoundingConfig> holder;

    public static boolean resetOnReload;

    public static final String configVersion = "1.0.0-bc.5";

    @Environment(EnvType.CLIENT)
    public static final ResoundingConfig DEFAULT = ResoundingEngine.env == EnvType.CLIENT ? new ResoundingConfig(){{
        Map<String, MaterialData> map = ResoundingEngine.nameToGroup.keySet().stream()
                .collect(Collectors.toMap(e -> e, e -> new MaterialData(e, 0.5, 0.5)));
        map.putIfAbsent("DEFAULT", new MaterialData("DEFAULT", 0.5, 0.5));
        materials.materialProperties = map;
    }} : null;

    public static void registerAutoConfig() {
        if (holder != null) {throw new IllegalStateException("Configuration already registered");}
        holder = AutoConfig.register(ResoundingConfig.class, JanksonConfigSerializer::new);

        if (ResoundingEngine.env == EnvType.CLIENT) try {GuiRegistryinit.register();} catch (Throwable ignored){
            ResoundingEngine.LOGGER.error("Failed to register config menu unwrappers. Edit config that isn't working in the config file");}

        holder.registerSaveListener((holder, config) -> onSave(config));
        holder.registerLoadListener((holder, config) -> onSave(config));
        reload(true);
    }

    public static ResoundingConfig getConfig() {
        if (holder == null) {return DEFAULT;}

        return holder.getConfig();
    }

    public static void reload(boolean load) {
        if (holder == null) {return;}

        if(load) holder.load();
        holder.getConfig().preset.setConfig();
        holder.save();
    }

    public static void save() { if (holder == null) {registerAutoConfig();} else {holder.save();} }

    @Environment(EnvType.CLIENT)
    public static void handleBrokenMaterials(@NotNull ResoundingConfig c ){
        ResoundingEngine.LOGGER.error("Critical materialProperties error. Resetting materialProperties");
        c.materials.materialProperties = PrecomputedConfig.materialDefaults;
        c.materials.blockWhiteList = Collections.emptyList();
    }

    public static void resetToDefault() {
        holder.resetToDefault();
        reload(false);
    }

    public static void handleUnstableConfig( ResoundingConfig c ){
        ResoundingEngine.LOGGER.error("Error: Config file is not from a compatible version! Resetting the config...");
        resetOnReload = true;
    }

    public static ActionResult onSave(ResoundingConfig c) {
        if (ResoundingEngine.env == EnvType.CLIENT && (c.materials.materialProperties == null || c.materials.materialProperties.get("DEFAULT") == null)) handleBrokenMaterials(c);
        if (ResoundingEngine.env == EnvType.CLIENT && c.preset != ConfigPresets.LOAD_SUCCESS) c.preset.configChanger.accept(c);
        if ((c.version == null || !Objects.equals(c.version, configVersion)) && !resetOnReload) handleUnstableConfig(c);
        if (PrecomputedConfig.pC != null) PrecomputedConfig.pC.deactivate();
        try {PrecomputedConfig.pC = new PrecomputedConfig(c);} catch (CloneNotSupportedException e) {e.printStackTrace(); return ActionResult.FAIL;}
        if (ResoundingEngine.env == EnvType.CLIENT && !ResoundingEngine.isOff) {
            ResoundingEngine.updateRays();
            ResoundingEngine.mc.getSoundManager().reloadSounds();
        }
        return ActionResult.SUCCESS;
    }
}
