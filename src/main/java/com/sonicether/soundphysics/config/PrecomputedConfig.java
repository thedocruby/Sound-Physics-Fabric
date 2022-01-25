package com.sonicether.soundphysics.config;

import com.sonicether.soundphysics.SPLog;
import com.sonicether.soundphysics.SoundPhysicsMod;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
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
    public static double soundDistanceAllowance = 4;
    public static double defaultAttenuationFactor = 1;
    public static PrecomputedConfig pC = null;

    public final boolean off;

    public final float globalReverbGain;
    public final float globalReverbBrightness;
    public final double globalBlockAbsorption;
    public final double globalBlockReflectance;
    public final float airAbsorption;
    public final float humidityAbsorption;
    public final float rainAbsorption;
    public final double underwaterFilter;

    public final boolean skipRainOcclusionTracing;
    public final int nRays;
    public final double rcpNRays;
    public final int nRayBounces;
    public final double rcpTotRays;
    public final boolean simplerSharedAirspaceSimulation;

    public final Reference2DoubleOpenHashMap<BlockSoundGroup> reflectivityMap;
    public final double defaultReflectivity;
    public final Reference2DoubleOpenHashMap<BlockSoundGroup> absorptionMap;
    public final double defaultAbsorption;
    public final Set<String> blockWhiteSet;
    public final Map<String, MaterialData> blockWhiteMap;

    public final boolean recordsDisable;
    public final int continuousRefreshRate;
    public final double maxDirectOcclusionFromBlocks;
    public final boolean _9Ray;
    public final boolean soundDirectionEvaluation;
    public final double directRaysDirEvalMultiplier;
    public final boolean notOccludedRedirect;

    public final boolean dLog;
    public final boolean oLog;
    public final boolean eLog;
    public final boolean pLog;
    public final boolean dRays;

    public final int traceRange;
    public final double minEnergy;
    public final double maxDistance;

    private boolean active = true;

    public PrecomputedConfig(SoundPhysicsConfig c) throws CloneNotSupportedException {
        if (pC != null && pC.active) throw new CloneNotSupportedException("Tried creating second instance of precomputedConfig");
        off = !c.enabled;

        defaultAttenuationFactor = c.General.attenuationFactor;
        globalReverbGain = (float) (c.General.globalReverbGain);
        globalReverbBrightness = (float) (c.General.globalReverbBrightness * 0.7);
        globalBlockAbsorption = c.General.globalBlockAbsorption * 2;
        soundDistanceAllowance = c.General.soundDistanceAllowance;
        globalBlockReflectance = c.General.globalBlockReflectance;
        airAbsorption = (float) c.General.airAbsorption;
        humidityAbsorption = (float) c.General.humidityAbsorption;
        rainAbsorption = (float) c.General.rainAbsorption;
        underwaterFilter = 1 - c.General.underwaterFilter;

        skipRainOcclusionTracing = c.Performance.skipRainOcclusionTracing;
        nRays = c.Performance.environmentEvaluationRays;
        rcpNRays = 1d/nRays;
        nRayBounces = c.Performance.environmentEvaluationRayBounces;
        rcpTotRays = rcpNRays/nRayBounces;
        simplerSharedAirspaceSimulation = c.Performance.simplerSharedAirspaceSimulation;

        blockWhiteSet = new HashSet<>(c.Materials.blockWhiteList);
        defaultReflectivity = c.Materials.materialProperties.get("DEFAULT").reflectivity;
        defaultAbsorption = c.Materials.materialProperties.get("DEFAULT").absorption;
        blockWhiteMap = c.Materials.blockWhiteList.stream()
                .map((a) -> new Pair<>(a, c.Materials.materialProperties.get(a)))
                .map((e) -> {
                    if (e.getRight() != null) return e;
                    SPLog.logError("Missing material data for "+e.getLeft()+", Default entry created");
                    final MaterialData newData = new MaterialData(e.getLeft(), defaultReflectivity, defaultAbsorption);
                    c.Materials.materialProperties.put(e.getLeft(), newData);
                    e.setRight(newData); return e;
                }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        reflectivityMap = new Reference2DoubleOpenHashMap<>();
        absorptionMap = new Reference2DoubleOpenHashMap<>();
        final List<String> wrong = new java.util.ArrayList<>();
        final List<String> toRemove = new java.util.ArrayList<>();
        c.Materials.materialProperties.forEach((k, v) -> {
            BlockSoundGroup bsg = SoundPhysicsMod.groupSoundBlocks.get(k);
            if (bsg != null){
                reflectivityMap.put(bsg, v.reflectivity);
                absorptionMap.put(bsg, v.absorption * 2);
            }
            else {
                if (!k.equals("DEFAULT") && !blockWhiteSet.contains(k)){
                    wrong.add(k+" ("+v.example+")");
                    toRemove.add(k);
                }
            }
        });
        if (!wrong.isEmpty()) {
            SPLog.logError("MaterialData map contains "+wrong.size()+" extra entries: "+ Arrays.toString(new List[]{wrong})+"\nRemoving...");
            toRemove.forEach((e) -> c.Materials.materialProperties.remove(e));
        }

        recordsDisable = c.Vlads_Tweaks.recordsDisable;
        continuousRefreshRate = c.Vlads_Tweaks.continuousRefreshRate;
        maxDirectOcclusionFromBlocks = c.Vlads_Tweaks.maxDirectOcclusionFromBlocks;
        _9Ray = c.Vlads_Tweaks._9RayDirectOcclusion;
        soundDirectionEvaluation = c.Vlads_Tweaks.soundDirectionEvaluation;
        directRaysDirEvalMultiplier = Math.pow(c.Vlads_Tweaks.directRaysDirEvalMultiplier, 10.66);
        notOccludedRedirect = !c.Vlads_Tweaks.notOccludedNoRedirect;

        dLog = c.Misc.debugLogging;
        oLog = c.Misc.occlusionLogging;
        eLog = c.Misc.environmentLogging;
        pLog = c.Misc.performanceLogging;
        dRays = c.Misc.raytraceParticles;

        traceRange = 256;
        minEnergy = 0.01;
        maxDistance = pC.traceRange * pC.nRayBounces;
    }

    public void deactivate(){ active = false;}
}
