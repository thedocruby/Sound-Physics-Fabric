package dev.thedocruby.resounding.config;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.ResoundingLog;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/*
    Values, which remain constant after the config has changed
    Only one instance allowed
 */
public class PrecomputedConfig {
    public final static float globalVolumeMultiplier = 4f;
    public static double soundDistanceAllowance = 6;
    public static double defaultAttenuationFactor = 1;
    public static double speedOfSound = 343.3;
    public static PrecomputedConfig pC = null;

    public final boolean off;

    public final float globalReverbGain;
    public final float globalReverbBrightness;
    public final double globalBlockAbsorption;
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

    public final int resolution;
    public final double warpFactor;
    public final double globalAbsorptionBrightness;
    public final double maxDecayTime;
    public final int traceRange;
    public final double minEnergy;
    public final double maxDistance;

    private boolean active = true;

    public PrecomputedConfig(ResoundingConfig c) throws CloneNotSupportedException {
        if (pC != null && pC.active) throw new CloneNotSupportedException("Tried creating second instance of precomputedConfig");
        off = !c.enabled;

        defaultAttenuationFactor = c.General.attenuationFactor;
        globalReverbGain = (float) (1 / c.General.globalReverbGain);
        globalReverbBrightness = (float) c.General.globalReverbBrightness;
        globalBlockAbsorption = c.General.globalBlockAbsorption;
        soundDistanceAllowance = c.General.soundDistanceAllowance;
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
            c.Materials.materialProperties.forEach((k, v) -> {
                BlockSoundGroup bsg = Resounding.groupSoundBlocks.get(k);
                if (bsg != null) {
                    reflectivityMap.put(bsg, v.reflectivity);
                    absorptionMap.put(bsg, v.absorption * 2);
                } else {
                    if (!k.equals("DEFAULT") && !blockWhiteSet.contains(k)) {
                        wrong.add(k + " (" + v.example + ")");
                        toRemove.add(k);
                    }
                }
            });
            if (!wrong.isEmpty()) {
                Resounding.LOGGER.error("Material Data map contains {} extra entries:\n{}\nPatching Material Data...", wrong.size(), Arrays.toString(new List[]{wrong}));
                toRemove.forEach(c.Materials.materialProperties::remove);
            }

            recordsDisable = c.Vlads_Tweaks.recordsDisable;
            continuousRefreshRate = c.Vlads_Tweaks.continuousRefreshRate;
            maxDirectOcclusionFromBlocks = c.Vlads_Tweaks.maxDirectOcclusionFromBlocks;
            _9Ray = c.Vlads_Tweaks._9RayDirectOcclusion;
            soundDirectionEvaluation = c.Vlads_Tweaks.soundDirectionEvaluation;
            directRaysDirEvalMultiplier = Math.pow(c.Vlads_Tweaks.directRaysDirEvalMultiplier, 10.66);
            notOccludedRedirect = !c.Vlads_Tweaks.notOccludedNoRedirect;
        }

        dLog = c.Misc.debugLogging;
        oLog = c.Misc.occlusionLogging;
        eLog = c.Misc.environmentLogging;
        pLog = c.Misc.performanceLogging;
        dRays = c.Misc.raytraceParticles;

        resolution = 4;
        warpFactor = 4;
        globalAbsorptionBrightness = 1d / 1d;
        maxDecayTime = Math.min(20, 4.142);
        traceRange = 256;
        minEnergy = Math.exp(-1 * c.Misc.minEnergy);
        maxDistance = traceRange * nRayBounces;
    }

    public void deactivate(){ active = false;}
}
