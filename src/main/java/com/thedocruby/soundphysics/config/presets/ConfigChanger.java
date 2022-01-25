package com.thedocruby.soundphysics.config.presets;

import com.thedocruby.soundphysics.config.MaterialData;
import com.thedocruby.soundphysics.config.SoundPhysicsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.thedocruby.soundphysics.SoundPhysicsMod.groupMap;

public class ConfigChanger {
    public static void changeConfig(SoundPhysicsConfig config, @Nullable Boolean enabled,
                                    @Nullable Double attenuationFactor, @Nullable Double globalReverbGain, @Nullable Double globalReverbBrightness, @Nullable Double globalBlockAbsorption, @Nullable Double globalBlockReflectance, @Nullable Double soundDistanceAllowance, @Nullable Double airAbsorption, @Nullable Double humidityAbsorption, @Nullable Double rainAbsorption, @Nullable Double underwaterFilter,
                                    @Nullable Boolean skipRainOcclusionTracing, @Nullable Integer environmentEvaluationRays, @Nullable Integer environmentEvaluationRayBounces, @Nullable Boolean simplerSharedAirspaceSimulation,
                                    @Nullable Map<String, MaterialData> materialProperties,
                                    @Nullable Integer continuousRefreshRate, @Nullable Double maxDirectOcclusionFromBlocks, @Nullable Boolean _9RayDirectOcclusion, @Nullable Boolean soundDirectionEvaluation, @Nullable Double directRaysDirEvalMultiplier, @Nullable Boolean notOccludedNoRedirect
    ) {
        if (enabled != null) config.enabled = enabled;
        setGeneral(config.General, attenuationFactor, globalReverbGain, globalReverbBrightness, globalBlockAbsorption, globalBlockReflectance, soundDistanceAllowance, airAbsorption, humidityAbsorption, rainAbsorption, underwaterFilter);
        setPerformance(config.Performance, skipRainOcclusionTracing, environmentEvaluationRays, environmentEvaluationRayBounces, simplerSharedAirspaceSimulation);
        setMaterial_Properties(config.Materials, materialProperties);
        setVlads_Tweaks(config.Vlads_Tweaks, continuousRefreshRate, maxDirectOcclusionFromBlocks, _9RayDirectOcclusion, soundDirectionEvaluation, directRaysDirEvalMultiplier, notOccludedNoRedirect);
        config.preset = ConfigPresets.LOAD_SUCCESS;
    }

    public static void setGeneral(SoundPhysicsConfig.General general, @Nullable Double attenuationFactor, @Nullable Double globalReverbGain, @Nullable Double globalReverbBrightness, @Nullable Double globalBlockAbsorption, @Nullable Double globalBlockReflectance, @Nullable Double soundDistanceAllowance, @Nullable Double airAbsorption, @Nullable Double humidityAbsorption, @Nullable Double rainAbsorption, @Nullable Double underwaterFilter) {
        if (attenuationFactor != null) general.attenuationFactor = attenuationFactor;
        if (globalReverbGain != null) general.globalReverbGain = globalReverbGain;
        if (globalReverbBrightness != null) general.globalReverbBrightness = globalReverbBrightness;
        if (globalBlockAbsorption != null) general.globalBlockAbsorption = globalBlockAbsorption;
        if (globalBlockReflectance != null) general.globalBlockReflectance = globalBlockReflectance;
        if (soundDistanceAllowance != null) general.soundDistanceAllowance = soundDistanceAllowance;
        if (airAbsorption != null) general.airAbsorption = airAbsorption;
        if (humidityAbsorption != null) general.humidityAbsorption = humidityAbsorption;
        if (rainAbsorption != null) general.rainAbsorption = rainAbsorption;
        if (underwaterFilter != null) general.underwaterFilter = underwaterFilter;
    }

    public static void setPerformance(SoundPhysicsConfig.Performance performance, @Nullable Boolean skipRainOcclusionTracing, @Nullable Integer environmentEvaluationRays, @Nullable Integer environmentEvaluationRayBounces, @Nullable Boolean simplerSharedAirspaceSimulation) {
        if (skipRainOcclusionTracing != null) performance.skipRainOcclusionTracing = skipRainOcclusionTracing;
        if (environmentEvaluationRays != null) performance.environmentEvaluationRays = environmentEvaluationRays;
        if (environmentEvaluationRayBounces != null) performance.environmentEvaluationRayBounces = environmentEvaluationRayBounces;
        if (simplerSharedAirspaceSimulation != null) performance.simplerSharedAirspaceSimulation = simplerSharedAirspaceSimulation;
    }

    public static void setMaterial_Properties(SoundPhysicsConfig.Materials materials, @Nullable Map<String, MaterialData> materialProperties) {
        if (materialProperties != null) materialProperties.forEach((s, newData) -> materials.materialProperties.compute(s, (k, v) -> (v == null) ?
                new MaterialData( hasExample(s) ? getExample(s) : "error",
                        newData.getReflectivity() == -1 ? 0.5 : newData.getReflectivity(),
                        newData.getAbsorption() == -1 ? 0.5 : newData.getAbsorption())
              : new MaterialData( (v.getExample() == null) ? (hasExample(s) ? getExample(s) : "error") : v.getExample(),
                        newData.getReflectivity() == -1 ? v.getReflectivity() : newData.getReflectivity(),
                        newData.getAbsorption() == -1 ? v.getAbsorption() : newData.getAbsorption())));
    }

    public static void setVlads_Tweaks(SoundPhysicsConfig.Vlads_Tweaks vlads_tweaks, @Nullable Integer continuousRefreshRate, @Nullable Double maxDirectOcclusionFromBlocks, @Nullable Boolean _9RayDirectOcclusion, @Nullable Boolean soundDirectionEvaluation, @Nullable Double directRaysDirEvalMultiplier, @Nullable Boolean notOccludedNoRedirect) {
        if (continuousRefreshRate != null) vlads_tweaks.continuousRefreshRate = continuousRefreshRate;
        if (maxDirectOcclusionFromBlocks != null) vlads_tweaks.maxDirectOcclusionFromBlocks = maxDirectOcclusionFromBlocks;
        if (_9RayDirectOcclusion != null) vlads_tweaks._9RayDirectOcclusion = _9RayDirectOcclusion;
        if (soundDirectionEvaluation != null) vlads_tweaks.soundDirectionEvaluation = soundDirectionEvaluation;
        if (directRaysDirEvalMultiplier != null) vlads_tweaks.directRaysDirEvalMultiplier = directRaysDirEvalMultiplier;
        if (notOccludedNoRedirect != null) vlads_tweaks.notOccludedNoRedirect = notOccludedNoRedirect;
    }
    public static String getExample(String s) {return groupMap.get(s);}
    public static boolean hasExample(String s) {return groupMap.containsKey(s);}
}
