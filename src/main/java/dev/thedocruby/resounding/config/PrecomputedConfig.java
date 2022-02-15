package dev.thedocruby.resounding.config;

import dev.thedocruby.resounding.Resounding;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.stream.Collectors;

import static dev.thedocruby.resounding.Resounding.nameToGroup;

/*
    Values, which remain constant after the config has changed
    Only one instance allowed
 */
public class PrecomputedConfig {
    @Environment(EnvType.CLIENT)
    public final static float globalVolumeMultiplier = 4f;
    @Environment(EnvType.CLIENT)
    public static double defaultAttenuationFactor = 1;
    @Environment(EnvType.CLIENT)
    public static double speedOfSound = 343.3;
    public static PrecomputedConfig pC = null;

    public boolean enabled;

    @Environment(EnvType.CLIENT)
    public float globalReverbGain;
    @Environment(EnvType.CLIENT)
    public double minEnergy;
    @Environment(EnvType.CLIENT)
    public int resolution;
    @Environment(EnvType.CLIENT)
    public double warpFactor;
    @Environment(EnvType.CLIENT)
    public float globalReverbBrightness;
    @Environment(EnvType.CLIENT)
    public double reverbCondensationFactor;
    @Environment(EnvType.CLIENT)
    public double globalBlockAbsorption;
    @Environment(EnvType.CLIENT)
    public double globalAbsorptionBrightness;
    public final int soundSimulationDistance;
    @Environment(EnvType.CLIENT)
    public double globalBlockReflectance;
    @Environment(EnvType.CLIENT)
    public double globalReflRcp;
    @Environment(EnvType.CLIENT)
    public float airAbsorption;
    @Environment(EnvType.CLIENT)
    public float humidityAbsorption;
    @Environment(EnvType.CLIENT)
    public float rainAbsorption;
    @Environment(EnvType.CLIENT)
    public double underwaterFilter;

    @Environment(EnvType.CLIENT)
    public boolean skipRainOcclusionTracing;
    @Environment(EnvType.CLIENT)
    public int nRays;
    @Environment(EnvType.CLIENT)
    public double rcpNRays;
    @Environment(EnvType.CLIENT)
    public int nRayBounces;
    @Environment(EnvType.CLIENT)
    public double rcpTotRays;
    @Environment(EnvType.CLIENT)
    public double maxDistance;
    @Environment(EnvType.CLIENT)
    public boolean simplerSharedAirspaceSimulation;

    @Environment(EnvType.CLIENT)
    public Reference2DoubleOpenHashMap<BlockSoundGroup> reflectivityMap;
    @Environment(EnvType.CLIENT)
    public double defaultReflectivity;
    @Environment(EnvType.CLIENT)
    public Reference2DoubleOpenHashMap<BlockSoundGroup> absorptionMap;
    @Environment(EnvType.CLIENT)
    public double defaultAbsorption;
    @Environment(EnvType.CLIENT)
    public Set<String> blockWhiteSet;
    @Environment(EnvType.CLIENT)
    public Map<String, MaterialData> blockWhiteMap;

    @Environment(EnvType.CLIENT)
    public boolean recordsDisable;
    @Environment(EnvType.CLIENT)
    public int sourceRefreshRate;
    @Environment(EnvType.CLIENT)
    public double maxDirectOcclusionFromBlocks;
    @Environment(EnvType.CLIENT)
    public boolean nineRay;
    @Environment(EnvType.CLIENT)
    public boolean soundDirectionEvaluation;
    @Environment(EnvType.CLIENT)
    public double directRaysDirEvalMultiplier;
    @Environment(EnvType.CLIENT)
    public boolean notOccludedRedirect;

    public final boolean dLog;
    @Environment(EnvType.CLIENT)
    public final boolean oLog;
    @Environment(EnvType.CLIENT)
    public final boolean eLog;
    @Environment(EnvType.CLIENT)
    public final boolean pLog;
    @Environment(EnvType.CLIENT)
    public final boolean dRays;

    private boolean active = true;

    public PrecomputedConfig(ResoundingConfig c) throws CloneNotSupportedException {
        if (pC != null && pC.active) throw new CloneNotSupportedException("Tried creating second instance of precomputedConfig");
        enabled = c.enabled;

        soundSimulationDistance = c.quality.soundSimulationDistance;

        if(Resounding.env == EnvType.CLIENT) { // TODO: organize
            defaultAttenuationFactor = c.general.attenuationFactor;
            globalReverbGain = (float) MathHelper.clamp(c.general.globalReverbGain, 0.0, 1.0);
            minEnergy = Math.exp(-4 * Math.max(c.general.globalReverbStrength, 0));
            resolution = c.quality.reverbResolution;
            warpFactor = MathHelper.clamp(c.misc.reverbBias, 1.0, 5.0);
            globalReverbBrightness = (float) MathHelper.clamp(c.general.globalReverbBrightness,0.1, 2.0);
            reverbCondensationFactor = 1 - MathHelper.clamp(c.general.globalReverbSmoothness, 0.0, 1.0);
            globalBlockAbsorption = c.general.globalBlockAbsorption;
            globalAbsorptionBrightness = c.general.globalAbsorptionBrightness;
            globalBlockReflectance = c.general.globalBlockReflectance;
            globalReflRcp = 1 / globalBlockReflectance;
            airAbsorption = (float) MathHelper.clamp(c.effects.airAbsorption, 0.0, 10.0);
            humidityAbsorption = (float) MathHelper.clamp(c.effects.humidityAbsorption, 0.0, 4.0);
            rainAbsorption = (float) MathHelper.clamp(c.effects.rainAbsorption, 0.0, 2.0);
            underwaterFilter = 1 - MathHelper.clamp(c.effects.underwaterFilter, 0.0, 1.0);

            skipRainOcclusionTracing = c.misc.skipRainOcclusionTracing;
            nRays = c.quality.envEvalRays;
            rcpNRays = 1d / nRays;
            nRayBounces = c.quality.envEvalRayBounces;
            rcpTotRays = rcpNRays / nRayBounces;
            maxDistance = MathHelper.clamp(c.quality.rayLength, 1.0, 16.0) * nRayBounces * 16 * Math.sqrt(2);
            simplerSharedAirspaceSimulation = c.misc.simplerSharedAirspaceSimulation;

            blockWhiteSet = new HashSet<>(c.materials.blockWhiteList);
            defaultReflectivity = c.materials.materialProperties.get("DEFAULT").reflectivity;
            defaultAbsorption = c.materials.materialProperties.get("DEFAULT").absorption;
            blockWhiteMap = c.materials.blockWhiteList.stream()
                    .map(a -> new Pair<>(a, c.materials.materialProperties.get(a)))
                    .map(e -> {
                        if (e.getRight() != null) return e;
                        Resounding.LOGGER.error("Missing material data for {}, Default entry created.", e.getLeft());
                        final MaterialData newData = new MaterialData(e.getLeft(), defaultReflectivity, defaultAbsorption);
                        c.materials.materialProperties.put(e.getLeft(), newData);
                        e.setRight(newData);
                        return e;
                    }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

            reflectivityMap = new Reference2DoubleOpenHashMap<>();
            absorptionMap = new Reference2DoubleOpenHashMap<>();
            final List<String> wrong = new java.util.ArrayList<>();
            final List<String> toRemove = new java.util.ArrayList<>();
            c.materials.materialProperties.forEach((k, v) -> { //TODO Materials need to be reworked.
                if (nameToGroup.containsKey(k)) {
                    BlockSoundGroup bsg = nameToGroup.get(k);
                    reflectivityMap.put(bsg, v.reflectivity);
                    absorptionMap.put(bsg, v.absorption);
                } else {
                    if (!blockWhiteSet.contains(k) && !k.equals("DEFAULT")) {
                        wrong.add(k + " (" + v.example + ")");
                        toRemove.add(k);
                    }
                }
            });
            if (!wrong.isEmpty()) {
                Resounding.LOGGER.error("Material Data map contains {} extra entries:\n{}\nPatching Material Data...", wrong.size(), Arrays.toString(new List[]{wrong}));
                toRemove.forEach(c.materials.materialProperties::remove);
            }

            recordsDisable = c.misc.recordsDisable;
            sourceRefreshRate = Math.max(c.quality.sourceRefreshRate, 1);
            maxDirectOcclusionFromBlocks = c.quality.maxBlockOcclusion;
            nineRay = c.quality.nineRayBlockOcclusion;
            soundDirectionEvaluation = c.effects.soundDirectionEvaluation; // TODO: DirEval
            directRaysDirEvalMultiplier = Math.pow(c.effects.directRaysDirEvalMultiplier, 10.66); // TODO: DirEval
            notOccludedRedirect = !c.misc.notOccludedNoRedirect;
        }

        dLog = c.debug.debugLogging;
        oLog = c.debug.occlusionLogging;
        eLog = c.debug.environmentLogging;
        pLog = c.debug.performanceLogging;
        dRays = c.debug.raytraceParticles;
    }

    public void deactivate(){ active = false;}
}
