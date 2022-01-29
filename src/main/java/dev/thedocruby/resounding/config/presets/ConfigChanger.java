package dev.thedocruby.resounding.config.presets;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.config.MaterialData;
import dev.thedocruby.resounding.config.ResoundingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ConfigChanger {
    public static void changeConfig(ResoundingConfig config, @Nullable Boolean enabled,
                                    @Nullable Double attenuationFactor, @Nullable Double globalReverbGain, @Nullable Double globalReverbBrightness, @Nullable Double globalBlockAbsorption, @Nullable Double globalBlockReflectance, @Nullable Double soundDistanceAllowance, @Nullable Double airAbsorption, @Nullable Double humidityAbsorption, @Nullable Double rainAbsorption, @Nullable Double underwaterFilter,
                                    @Nullable Boolean skipRainOcclusionTracing, @Nullable Integer environmentEvaluationRays, @Nullable Integer environmentEvaluationRayBounces, @Nullable Boolean simplerSharedAirspaceSimulation,
                                    @Nullable Map<String, MaterialData> materialProperties,
                                    @Nullable Integer continuousRefreshRate, @Nullable Double maxDirectOcclusionFromBlocks, @Nullable Boolean _9RayDirectOcclusion, @Nullable Boolean soundDirectionEvaluation, @Nullable Double directRaysDirEvalMultiplier, @Nullable Boolean notOccludedNoRedirect
    ) {
        if (enabled != null) config.enabled = enabled;
        setGeneral(config.General, attenuationFactor, globalReverbGain, globalReverbBrightness, globalBlockAbsorption, globalBlockReflectance, soundDistanceAllowance, airAbsorption, humidityAbsorption, rainAbsorption, underwaterFilter);
        if(Resounding.env == EnvType.SERVER) return;
        setPerformance(config.Performance, skipRainOcclusionTracing, environmentEvaluationRays, environmentEvaluationRayBounces, simplerSharedAirspaceSimulation);
        setMaterial_Properties(config.Materials, materialProperties);
        setVlads_Tweaks(config.Vlads_Tweaks, continuousRefreshRate, maxDirectOcclusionFromBlocks, _9RayDirectOcclusion, soundDirectionEvaluation, directRaysDirEvalMultiplier, notOccludedNoRedirect);
        config.preset = ConfigPresets.LOAD_SUCCESS;
    }

    public static void setGeneral(ResoundingConfig.General general, @Nullable Double attenuationFactor, @Nullable Double globalReverbGain, @Nullable Double globalReverbBrightness, @Nullable Double globalBlockAbsorption, @Nullable Double globalBlockReflectance, @Nullable Double soundDistanceAllowance, @Nullable Double airAbsorption, @Nullable Double humidityAbsorption, @Nullable Double rainAbsorption, @Nullable Double underwaterFilter) {
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

    @Environment(EnvType.CLIENT)
    public static void setPerformance(ResoundingConfig.Performance performance, @Nullable Boolean skipRainOcclusionTracing, @Nullable Integer environmentEvaluationRays, @Nullable Integer environmentEvaluationRayBounces, @Nullable Boolean simplerSharedAirspaceSimulation) {
        if (skipRainOcclusionTracing != null) performance.skipRainOcclusionTracing = skipRainOcclusionTracing;
        if (environmentEvaluationRays != null) performance.environmentEvaluationRays = environmentEvaluationRays;
        if (environmentEvaluationRayBounces != null) performance.environmentEvaluationRayBounces = environmentEvaluationRayBounces;
        if (simplerSharedAirspaceSimulation != null) performance.simplerSharedAirspaceSimulation = simplerSharedAirspaceSimulation;
    }

    @Environment(EnvType.CLIENT)
    public static void setMaterial_Properties(ResoundingConfig.Materials materials, @Nullable Map<String, MaterialData> materialProperties) {
        if (materialProperties != null) materialProperties.forEach((s, newData) -> materials.materialProperties.compute(s, (k, v) -> (v == null) ?
                new MaterialData( hasExample(s) ? getExample(s) : "error",
                        newData.getReflectivity() == -1 ? 0.5 : newData.getReflectivity(),
                        newData.getAbsorption() == -1 ? 0.5 : newData.getAbsorption())
              : new MaterialData( (v.getExample() == null) ? (hasExample(s) ? getExample(s) : "error") : v.getExample(),
                        newData.getReflectivity() == -1 ? v.getReflectivity() : newData.getReflectivity(),
                        newData.getAbsorption() == -1 ? v.getAbsorption() : newData.getAbsorption())));
    }

    @Environment(EnvType.CLIENT)
    public static void setVlads_Tweaks(ResoundingConfig.Vlads_Tweaks vlads_tweaks, @Nullable Integer continuousRefreshRate, @Nullable Double maxDirectOcclusionFromBlocks, @Nullable Boolean _9RayDirectOcclusion, @Nullable Boolean soundDirectionEvaluation, @Nullable Double directRaysDirEvalMultiplier, @Nullable Boolean notOccludedNoRedirect) {
        if (continuousRefreshRate != null) vlads_tweaks.continuousRefreshRate = continuousRefreshRate;
        if (maxDirectOcclusionFromBlocks != null) vlads_tweaks.maxDirectOcclusionFromBlocks = maxDirectOcclusionFromBlocks;
        if (_9RayDirectOcclusion != null) vlads_tweaks._9RayDirectOcclusion = _9RayDirectOcclusion;
        if (soundDirectionEvaluation != null) vlads_tweaks.soundDirectionEvaluation = soundDirectionEvaluation;
        if (directRaysDirEvalMultiplier != null) vlads_tweaks.directRaysDirEvalMultiplier = directRaysDirEvalMultiplier;
        if (notOccludedNoRedirect != null) vlads_tweaks.notOccludedNoRedirect = notOccludedNoRedirect;
    }
    public static String getExample(String s) {return Resounding.groupMap.get(s);}
    public static boolean hasExample(String s) {return Resounding.groupMap.containsKey(s);}
}
