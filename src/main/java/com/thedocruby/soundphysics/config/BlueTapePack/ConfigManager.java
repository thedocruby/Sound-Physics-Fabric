package com.thedocruby.soundphysics.config.BlueTapePack;

import com.thedocruby.soundphysics.ALstuff.SPEfx;
import com.thedocruby.soundphysics.SPLog;
import com.thedocruby.soundphysics.SoundPhysics;
import com.thedocruby.soundphysics.SoundPhysicsMod;
import com.thedocruby.soundphysics.config.MaterialData;
import com.thedocruby.soundphysics.config.PrecomputedConfig;
import com.thedocruby.soundphysics.config.SoundPhysicsConfig;
import com.thedocruby.soundphysics.config.presets.ConfigPresets;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.util.ActionResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConfigManager {
    private static ConfigHolder<SoundPhysicsConfig> holder;

    public static final SoundPhysicsConfig DEFAULT = new SoundPhysicsConfig(){{
        Map<String, MaterialData> map =
                SoundPhysicsMod.blockSoundGroups.entrySet().stream()
                        .collect(Collectors.toMap((e)-> e.getValue().getLeft(), (e) -> new MaterialData(e.getValue().getRight(), 0.5, 0.5)));
        map.putIfAbsent("DEFAULT", new MaterialData(SoundPhysicsMod.groupMap.get("DEFAULT"), 0.5, 0.5));
        Materials.materialProperties = map;
    }};

    public static void registerAutoConfig() {
        if (holder != null) {throw new IllegalStateException("Configuration already registered");}
        holder = AutoConfig.register(SoundPhysicsConfig.class, JanksonConfigSerializer::new);

        try {GuiRegistryinit.register();} catch (Throwable ignored){SPLog.logError("Failed to register config menu unwrappers. Edit config that isn't working in the config file");}

        holder.registerSaveListener((holder, config) -> onSave(config));
        holder.registerLoadListener((holder, config) -> onSave(config));
        reload(true);
    }

    public static SoundPhysicsConfig getConfig() {
        if (holder == null) {return DEFAULT;}

        return holder.getConfig();
    }

    public static void reload(boolean load) {
        if (holder == null) {return;}

        if(load) holder.load();
        holder.getConfig().preset.setConfig();
        SPEfx.syncReverbParams();
        holder.save();
    }

    public static void save() { if (holder == null) {registerAutoConfig();} else {holder.save();} }

    public static void handleBrokenMaterials( SoundPhysicsConfig c ){
        SPLog.logError("Critical materialProperties error. Resetting materialProperties");
        SoundPhysicsConfig fallback = DEFAULT;
        ConfigPresets.RESET_MATERIALS.configChanger.accept(fallback);
        c.Materials.materialProperties = fallback.Materials.materialProperties;
        c.Materials.blockWhiteList = List.of("block.minecraft.water");
    }

    public static void handleUnstableConfig( SoundPhysicsConfig c ){
        SPLog.logError("Error: Config file is not from a compatible version! Resetting the config...");
        ConfigPresets.DEFAULT_PERFORMANCE.configChanger.accept(c);
        ConfigPresets.RESET_MATERIALS.configChanger.accept(c);
        c.version = "0.5.5";
    }

    public static ActionResult onSave(SoundPhysicsConfig c) {
        if (c.Materials.materialProperties == null || c.Materials.materialProperties.get("DEFAULT") == null) handleBrokenMaterials(c);
        if (c.preset != ConfigPresets.LOAD_SUCCESS) c.preset.configChanger.accept(c);
        if (c.version == null || !Objects.equals(c.version, "0.5.5")) handleUnstableConfig(c);
        if(PrecomputedConfig.pC != null) PrecomputedConfig.pC.deactivate();
        try {PrecomputedConfig.pC = new PrecomputedConfig(c);} catch (CloneNotSupportedException e) {e.printStackTrace(); return ActionResult.FAIL;}
        SPEfx.syncReverbParams();
        SoundPhysics.updateRays();
        return ActionResult.SUCCESS;
    }
}
