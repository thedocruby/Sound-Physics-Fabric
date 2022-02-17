package dev.thedocruby.resounding.config.presets;

import dev.thedocruby.resounding.config.BlueTapePack.ConfigManager;
import dev.thedocruby.resounding.config.PrecomputedConfig;
import dev.thedocruby.resounding.config.ResoundingConfig;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

// TODO: Update Profiles after rewrite, before first beta
@SuppressWarnings({"unused", "RedundantTypeArguments"})
public enum ConfigPresets {
    // Press ctrl+shift+numpad_'-' to collapse all
    LOAD_SUCCESS("Choose", null),
    /*
    //<editor-fold desc="DEFAULT_BALANCED,">
    DEFAULT_BALANCED("Balanced (Base)", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,

            1.0, 1.0, 1.0, 1.0,
            1.0, 4, 1.0, 1.0, 1.0, 0.8,

            true, 224, 12, false,

            PrecomputedConfig.materialDefaults,

            4, 10.0, true, true, 0.5, false
    )),//</editor-fold>
    //<editor-fold desc="DEFAULT_PERFORMANCE,">
    DEFAULT_PERFORMANCE("Performance (Base)", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,

            1.0, 1.0, 1.0, 1.0,
            1.0, 4, 1.0, 1.0, 1.0, 0.8,

            true, 96, 6, true,

            PrecomputedConfig.materialDefaults,

            4, 10.0, true, true, 0.5, true
    )),//</editor-fold>
    //<editor-fold desc="DEFAULT_QUALITY,">
    DEFAULT_QUALITY("Quality (Base)", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,

            1.0, 1.0, 1.0, 1.0,
            1.0, 4, 1.0, 1.0, 1.0, 0.8,

            false, 512, 24, false,

            PrecomputedConfig.materialDefaults,

            4, 10.0, true, true, 0.5, false
    )),//</editor-fold>
    //<editor-fold desc="THEDOCRUBY,">
    THEDOCRUBY("Dr. Rubisco's Signature Sound", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,

            1.0, 0.8, 1.0, 0.8,
            1.0, 3.5, 1.0, 1.0, 1.0, 0.8,

            true, 256, 16, false,

            PrecomputedConfig.materialDefaults,

            4, 10.0, true, true, 0.5, false

    )),//</editor-fold>
    //<editor-fold desc="SUPER_REVERB,">
    SUPER_REVERB("Super Reverb", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,
            null, 1.8, null, null,
            4.0, null, null, null, null, null,

            null, null, null, null,

            null,

            null, null, null, null, null, null

    )),//</editor-fold>
    //<editor-fold desc="LUSH_REVERB,">
    LUSH_REVERB("More Lush Cave Reverb", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,
            null, null, null, null,
            null, null, null, null, null, null,

            null, null, null, null,

            //<editor-fold desc="Map.ofEntries(),">
            Map.<String, MaterialData>ofEntries(
                    entry("field_28697", new MaterialData(0.85,                 0.85 )),    // Moss               (moss, moss_carpet)
                    entry("field_11529", new MaterialData(0.7,                  0.4  )),    // Gravel, Dirt       (gravel, rooted_dirt)
                    entry("field_23083", new MaterialData(0.25,                 0.15 )),    // Dry Foliage        (vine, hanging_roots, glow_lichen)
                    entry("field_28694", new MaterialData(0.45,                 0.5  )),    // Azalea Bush        (azalea)
                    entry("field_28692", new MaterialData(0.65,                 0.2  ))     // Lush Foliage       (cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
            ),//</editor-fold>

            null, null, null, null, null, null
    )),//</editor-fold>
    //<editor-fold desc="NO_ABSORPTION,">
    NO_ABSORPTION("No Absorption", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,
            null, null, null, 0.0,
            null, null, 0.0, 0.0, 0.0, null,

            null, null, null, null,

            null,

            null, 0.0, null, null, null, null
    )),//</editor-fold>
    //<editor-fold desc="LOW_FREQ,">
    LOW_FREQ("Bass Boost", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,
            null, null, 0.2, null,
            null, null, 2.0, 2.0, 2.0, null,

            null, null, null, null,

            null,

            null, null, null, null, null, null
    )),//</editor-fold>
    //<editor-fold desc="HIGH_FREQ,">
    HIGH_FREQ("Treble Boost", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,
            null, null, 1.8, null,
            null, null, 0.5, 0.5, 0.5, null,

            null, null, null, null,

            null,

            null, null, null, null, null, null
    )),//</editor-fold>
    //<editor-fold desc="FOG,">
    FOG("Foggy Air", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,
            2.5, null, null, null,
            null, null, 7.5, 0.5, 1.0, null,

            null, null, null, null,

            null,

            null, null, null, null, null, null
    )),//</editor-fold>
    //<editor-fold desc="TOTAL_OCCLUSION,">
    TOTAL_OCCLUSION("Total Occlusion", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,

            null, null, null, 10.0,
            null, null, null, null, null, null,

            null, null, null, null,

            null,

            null, 10.0,  null, null, null, null
    )),//</editor-fold>
     */ // TODO add back
    //<editor-fold desc="RESET_MATERIALS;">
    RESET_MATERIALS("Reset Materials", (ResoundingConfig c) -> ConfigChanger.changeConfig(c, true,

            null, null, null, null,
            null, null, null, null,

            null, null, null, null, null, null,

            PrecomputedConfig.materialDefaults,

            null, null, null, null,null, null
    ));//</editor-fold>

    public final Consumer<ResoundingConfig> configChanger;
    public final String text;
    public void setConfig(){ if (configChanger != null) {configChanger.accept(ConfigManager.getConfig());ConfigManager.save();}}

    ConfigPresets(String text, @Nullable Consumer<ResoundingConfig> c) {
        this.configChanger = c; 
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
