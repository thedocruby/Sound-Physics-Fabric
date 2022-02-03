package dev.thedocruby.resounding.config;

import dev.thedocruby.resounding.Resounding;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static dev.thedocruby.resounding.Resounding.nameToGroup;

/*
    Values, which remain constant after the config has changed
    Only one instance allowed
 */
public class PrecomputedConfig {
    public final static float globalVolumeMultiplier = 4f;
    public static double defaultAttenuationFactor = 1;
    public static double speedOfSound = 343.3;
    public static PrecomputedConfig pC = null;

    public final boolean off;

    public final float globalReverbGain;
    public final double minEnergy;
    public final int resolution;
    public final double warpFactor;
    public final float globalReverbBrightness;
    public final double reverbCondensationFactor;
    public final double globalBlockAbsorption;
    public final double globalAbsorptionBrightness;
    public final int soundSimulationDistance;
    public final double globalBlockReflectance;
    public final double globalReflRcp;
    public final float airAbsorption;
    public final float humidityAbsorption;
    public final float rainAbsorption;
    public final double underwaterFilter;

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
    public int continuousRefreshRate;
    @Environment(EnvType.CLIENT)
    public double maxDirectOcclusionFromBlocks;
    @Environment(EnvType.CLIENT)
    public boolean _9Ray;
    @Environment(EnvType.CLIENT)
    public boolean soundDirectionEvaluation;
    @Environment(EnvType.CLIENT)
    public double directRaysDirEvalMultiplier;
    @Environment(EnvType.CLIENT)
    public boolean notOccludedRedirect;

    public final boolean dLog;
    public final boolean oLog;
    public final boolean eLog;
    public final boolean pLog;
    public final boolean dRays;

    private boolean active = true;

    public PrecomputedConfig(ResoundingConfig c) throws CloneNotSupportedException {
        if (pC != null && pC.active) throw new CloneNotSupportedException("Tried creating second instance of precomputedConfig");
        off = !c.enabled;

        defaultAttenuationFactor = c.General.attenuationFactor;
        globalReverbGain = (float) c.General.globalReverbGain;
        minEnergy = Math.exp(-4 * c.General.globalReverbStrength);
        resolution = c.General.reverbResolution;
        warpFactor = c.General.reverbWarpFactor;
        globalReverbBrightness = (float) c.General.globalReverbBrightness;
        reverbCondensationFactor = 1 - c.General.globalReverbSmoothness;
        globalBlockAbsorption = c.General.globalBlockAbsorption;
        globalAbsorptionBrightness = c.General.globalAbsorptionBrightness;
        soundSimulationDistance = c.General.soundSimulationDistance;
        globalBlockReflectance = c.General.globalBlockReflectance;
        globalReflRcp = 1 / globalBlockReflectance;
        airAbsorption = (float) c.General.airAbsorption;
        humidityAbsorption = (float) c.General.humidityAbsorption;
        rainAbsorption = (float) c.General.rainAbsorption;
        underwaterFilter = 1 - c.General.underwaterFilter;

        if(Resounding.env == EnvType.CLIENT) {
            skipRainOcclusionTracing = c.Performance.skipRainOcclusionTracing;
            nRays = c.Performance.environmentEvaluationRays;
            rcpNRays = 1d / nRays;
            nRayBounces = c.Performance.environmentEvaluationRayBounces;
            rcpTotRays = rcpNRays / nRayBounces;
            maxDistance = c.Performance.traceRange * nRayBounces * 16 * Math.sqrt(2);
            simplerSharedAirspaceSimulation = c.Performance.simplerSharedAirspaceSimulation;

            blockWhiteSet = new HashSet<>(c.Materials.blockWhiteList);
            defaultReflectivity = c.Materials.materialProperties.get("DEFAULT").reflectivity;
            defaultAbsorption = c.Materials.materialProperties.get("DEFAULT").absorption;
            blockWhiteMap = c.Materials.blockWhiteList.stream()
                    .map(a -> new Pair<>(a, c.Materials.materialProperties.get(a)))
                    .map(e -> {
                        if (e.getRight() != null) return e;
                        Resounding.LOGGER.error("Missing material data for {}, Default entry created.", e.getLeft());
                        final MaterialData newData = new MaterialData(e.getLeft(), defaultReflectivity, defaultAbsorption);
                        c.Materials.materialProperties.put(e.getLeft(), newData);
                        e.setRight(newData);
                        return e;
                    }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

            reflectivityMap = new Reference2DoubleOpenHashMap<>();
            absorptionMap = new Reference2DoubleOpenHashMap<>();
            final List<String> wrong = new java.util.ArrayList<>();
            final List<String> toRemove = new java.util.ArrayList<>();
            c.Materials.materialProperties.forEach((k, v) -> { //TODO Materials need to be reworked.
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
                toRemove.forEach(c.Materials.materialProperties::remove);
            }

            recordsDisable = c.Misc.recordsDisable;
            continuousRefreshRate = c.Misc.continuousRefreshRate;
            maxDirectOcclusionFromBlocks = c.Misc.maxDirectOcclusionFromBlocks;
            _9Ray = c.Misc._9RayDirectOcclusion;
            soundDirectionEvaluation = c.Misc.soundDirectionEvaluation; // TODO: DirEval
            directRaysDirEvalMultiplier = Math.pow(c.Misc.directRaysDirEvalMultiplier, 10.66); // TODO: DirEval
            notOccludedRedirect = !c.Misc.notOccludedNoRedirect;
        }

        dLog = c.Debug.debugLogging;
        oLog = c.Debug.occlusionLogging;
        eLog = c.Debug.environmentLogging;
        pLog = c.Debug.performanceLogging;
        dRays = c.Debug.raytraceParticles;
    }

    public void deactivate(){ active = false;}
}
