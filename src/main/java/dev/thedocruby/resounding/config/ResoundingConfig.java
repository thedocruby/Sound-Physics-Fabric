package dev.thedocruby.resounding.config;

import dev.thedocruby.resounding.config.presets.ConfigPresets;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.thedocruby.resounding.config.BlueTapePack.ConfigManager.configVersion;

@SuppressWarnings("CanBeFinal")
@Config(name = "resounding")
@Config.Gui.Background("minecraft:textures/block/note_block.png")
// TODO: Add color and performance impact to the tooltips.
// TODO: Lang file
public class ResoundingConfig implements ConfigData {

    @Comment("Enable reverb?")
    public boolean enabled = true;

    @ConfigEntry.Gui.CollapsibleObject
    public General general = new General();

    @ConfigEntry.Gui.CollapsibleObject
    public Quality quality = new Quality();

    @ConfigEntry.Gui.CollapsibleObject
    public Effects effects = new Effects();

    @ConfigEntry.Gui.CollapsibleObject
    public Materials materials = new Materials();

    @ConfigEntry.Gui.CollapsibleObject
    public Misc misc = new Misc();

    @ConfigEntry.Gui.CollapsibleObject
    public Debug debug = new Debug();

    public static class General{
            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: remove this
            @Comment("Affects how quiet a sound gets based on distance.\nLower values mean distant sounds are louder.\n1.0 is the physically correct value.\n0.1 - 1.0")
            public double attenuationFactor = 1.0;

        @Environment(EnvType.CLIENT)
        @Comment("The global volume of simulated reverberations. 0.0 - 1.0")
        public double globalReverbGain = 0.5;

        @Environment(EnvType.CLIENT)
        @Comment("The strength of the reverb effect. Minimum 0.0.\nHigher values make the reverb last longer.\nLower values make the reverb tails shorter.")
        public double globalReverbStrength = 1.0;

        @Environment(EnvType.CLIENT)
        @Comment("The smoothness of the reverb. 0.0 - 1.0.\nAffects how uniform the reverb is.\nLow values cause a distinct fluttering or bouncing echo.\nHigh values make this effect less distinct by smoothing out the reverb.")
        public double globalReverbSmoothness = 0.62;

        @Environment(EnvType.CLIENT)
        @Comment("The brightness of reverberation.\nHigher values result in more high frequencies in reverberation.\nLower values give a more muffled sound to the reverb.\n0.1 - 2.0")
        public double globalReverbBrightness = 1.0;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: Occlusion
            public double globalAbsorptionBrightness = 1.0;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: Occlusion. Remove?
            @Comment("The global amount of sound that will be absorbed when traveling through blocks.\n 0.1 - 4.0")
            public double globalBlockAbsorption = 1.0;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: remove this
            @Comment("The global amount of sound reflectance energy of all blocks.\nLower values result in more conservative reverb simulation with shorter reverb tails.\nHigher values result in more generous reverb simulation with higher reverb tails.\n0.1 - 4.0")
            public double globalBlockReflectance = 1.0;
    }

    public static class Quality{

        @Environment(EnvType.CLIENT) @ConfigEntry.BoundedDiscrete(max = 32, min = 4)
        @Comment("The resolution quality of the reverb effect.\nHigher values create a fuller, more colorful, more immersive reverb effect.")
        public int reverbResolution = 10;

        @Environment(EnvType.CLIENT) @ConfigEntry.BoundedDiscrete(max = 768, min = 8)
        @Comment("The number of rays to trace to determine reverberation for each sound source.\nMore rays provides more consistent tracing results, but takes more time to calculate.\nDecrease this value if you experience lag spikes when sounds play.")
        public int envEvalRays = 128;

        @Environment(EnvType.CLIENT) @ConfigEntry.BoundedDiscrete(max = 32, min = 2)
        @Comment("The number of rays bounces to trace to determine reverberation for each sound source.\nMore bounces provides more echo and sound ducting but takes more time to calculate.\n Capped by max tracing distance.")
        public int envEvalRayBounces = 8;

        @ConfigEntry.BoundedDiscrete(max = 32, min = 1)
        @Comment("Minecraft won't allow most sounds to play if they are more than a chunk from the player;\nResounding makes that configurable by multiplying this parameter by the default distance.\nValues too high can cause polyphony issues/ Increasing past the simulation/render distance has no effect.\n1 - 32")
        public int soundSimulationDistance = 10;

        @Environment(EnvType.CLIENT)
        @Comment("The maximum distance to trace each ray, per each bounce, in chunks. 1.0 - 16.0.\nFor the best balance of performance and quality, increase this:\n  - When you increase the sound simulation distance\n  - When you decrease the number of ray bounces.\n  - If you often find yourself in large enclosed spaces, e.g. large 1.18 caves, or large open buildings")
        public double traceRange = 4.0;

        @Environment(EnvType.CLIENT)
        @Comment("Reverb refresh interval (ticks per refresh or 1/(20Hz)). Minimum 1.\nDecreasing this value causes the reverb effect of long sounds to update more frequently.")
        public int sourceRefreshRate = 4;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: Occlusion
            @Comment("The amount at which occlusion is capped. 10 * block_occlusion is the theoretical limit")
            public double maxBlockOcclusion = 10;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: Occlusion
            @Comment("Calculate direct occlusion as the minimum of 9 rays from vertices of a block")
            public boolean nineRayBlockOcclusion = true;
    }

    public static class Effects {
        @Environment(EnvType.CLIENT)
        @Comment("Represents how aggressively air absorbs high frequencies over distance.\nA value of 1.0 is physically correct for air with normal humidity and temperature.\nHigher values mean air will absorb more high frequencies with distance.\nA value of 0.0 disables this effect. 0.0 - 10.0")
        public double airAbsorption = 1.0;

        @Environment(EnvType.CLIENT)
        @Comment("How much humidity contributes to the air absorption.\nA value of 1.0 is physically correct.\nHigher values mean air will absorb more high frequencies with distance, depending on the local humidity.\nA value of 0.0 disables this effect. 0.0 - 4.0")
        public double humidityAbsorption = 1.0;

        @Environment(EnvType.CLIENT)
        @Comment("How much rain drops contribute to the air absorption.\nA value of 1.0 is approximately physically correct.\nHigher values mean air will absorb more high frequencies with distance, depending on the local rainfall.\nA value of 0.0 disables this effect. 0.0 - 2.0")
        public double rainAbsorption = 1.0;

        @Environment(EnvType.CLIENT)
        @Comment("How much sound is filtered when the player is underwater.\n0.0 means no filter. 1.0 means fully filtered.\n0.0 - 1.0")
        public double underwaterFilter = 0.8;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: DirEval
            @Comment("Whether to try calculating where the sound should come from based on reflections")
            public boolean soundDirectionEvaluation = true;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: DirEval
            @Comment("How much the sound direction depends on reflected sounds.\nRequires \"Re-calculate sound direction\" to be enabled.\n0.0 is no reflected sounds, 1.0 is 100% reflected sounds.\n0.5 is approximately physically accurate.")
            public double directRaysDirEvalMultiplier = 0.5;
    }

    public static class Materials {
        @Environment(EnvType.CLIENT)
        @Comment("Material properties for blocks.\n0.0 - 1.0")
        public Map<String, MaterialData> materialProperties = null;

        @Environment(EnvType.CLIENT)
        @Comment("Makes blocks use ID (e.g. block.minecraft.stone) instead of sound group to determine material")
        public List<String> blockWhiteList = new ArrayList<>();
    }

    public static class Misc {
        @Environment(EnvType.CLIENT)
        @Comment("Disable occlusion of jukeboxes and note blocks.\nUseful if you have an audio signaling system in your base\n    that you need to hear clearly through the walls.")
        public boolean recordsDisable = false;

        @Environment(EnvType.CLIENT)
        @Comment("How strongly the reverb quality is biased toward shorter tails. 1.0 - 5.0\nThis bias helps the reverb sound more accurate in smaller spaces.\nThis setting shouldn't need to be changed, and can cause horrible-sounding reverb if handled incorrectly.\nHowever, If you know what you're doing, this value is somewhat similar to the exponent used\n    for warping the shadowmap of a shader to increase the resolution around the player.")
        public double reverbBias = 3;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: DirEval, Occlusion
            @Comment("Skip redirecting non-occluded sounds (the ones you can see directly).\nCan be inaccurate in some situations, especially when \"Re-calculate sound direction\" is enabled.")
            public boolean notOccludedNoRedirect = false;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: Occlusion
            @Comment("If true, rain sound sources won't trace for sound occlusion.\nThis can help performance during rain.")
            public boolean skipRainOcclusionTracing = true;

            @Environment(EnvType.CLIENT) @ConfigEntry.Gui.Excluded // TODO: Remove
            @Comment("If true, enables a simpler technique for determining when the player and a sound source share airspace.\nMight sometimes miss recognizing shared airspace, but it's faster to calculate.")
            public boolean simplerSharedAirspaceSimulation = false;
    }

    public static class Debug {
        @Comment("General debug logging")
        public boolean debugLogging = false;
            @ConfigEntry.Gui.Excluded // TODO: Occlusion
            @Comment("Occlusion tracing information logging")
            public boolean occlusionLogging = false;
        @Comment("Environment evaluation information logging")
        public boolean environmentLogging = false;
        @Comment("Performance information logging")
        public boolean performanceLogging = false;
        @Comment("Particles on traced blocks (structure_void is a block)")
        public boolean raytraceParticles = false;
    }

    // TODO: change preset back to "Balanced" when performance permits
    @ConfigEntry.Gui.Excluded // TODO: update presets and config changer
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @Comment("Soft presets. Some of these can be applied one after another to stack effects onto a base profile.")
    public ConfigPresets preset = ConfigPresets.LOAD_SUCCESS/*ConfigPresets.DEFAULT_PERFORMANCE*/;

    @ConfigEntry.Gui.Excluded
    public String version = configVersion;
}
