package dev.thedocruby.resounding.config.presets;

import dev.thedocruby.resounding.Resounding;
import dev.thedocruby.resounding.config.MaterialData;
import dev.thedocruby.resounding.config.ResoundingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ConfigChanger {
    private ConfigChanger() {}

    public static void changeConfig(ResoundingConfig config, @Nullable Boolean enabled,
                                    @Nullable Double attenuationFactor, @Nullable Double globalReverbGain, @Nullable Double globalReverbBrightness, @Nullable Double globalBlockAbsorption, @Nullable Double globalBlockReflectance, @Nullable Integer soundSimulationDistance, @Nullable Double airAbsorption, @Nullable Double humidityAbsorption, @Nullable Double rainAbsorption, @Nullable Double underwaterFilter,
                                    @Nullable Boolean skipRainOcclusionTracing, @Nullable Integer environmentEvaluationRays, @Nullable Integer environmentEvaluationRayBounces, @Nullable Boolean simplerSharedAirspaceSimulation,
                                    @Nullable Map<String, MaterialData> materialProperties,
                                    @Nullable Integer continuousRefreshRate, @Nullable Double maxDirectOcclusionFromBlocks, @Nullable Boolean _9RayDirectOcclusion, @Nullable Boolean soundDirectionEvaluation, @Nullable Double directRaysDirEvalMultiplier, @Nullable Boolean notOccludedNoRedirect
    ) {
        if (enabled != null) config.enabled = enabled;
        setGeneral(config.General, attenuationFactor, globalReverbGain, globalReverbBrightness, globalBlockAbsorption, globalBlockReflectance, soundSimulationDistance, airAbsorption, humidityAbsorption, rainAbsorption, underwaterFilter);
        if(Resounding.env == EnvType.SERVER) return;
        setPerformance(config.Performance, skipRainOcclusionTracing, environmentEvaluationRays, environmentEvaluationRayBounces, simplerSharedAirspaceSimulation);
        setMaterialProperties(config.Materials, materialProperties);
        setMisc(config.Misc, continuousRefreshRate, maxDirectOcclusionFromBlocks, _9RayDirectOcclusion, soundDirectionEvaluation, directRaysDirEvalMultiplier, notOccludedNoRedirect);
        config.preset = ConfigPresets.LOAD_SUCCESS;
    }

    public static void setGeneral(ResoundingConfig.General general, @Nullable Double attenuationFactor, @Nullable Double globalReverbGain, @Nullable Double globalReverbBrightness, @Nullable Double globalBlockAbsorption, @Nullable Double globalBlockReflectance, @Nullable Integer soundSimulationDistance, @Nullable Double airAbsorption, @Nullable Double humidityAbsorption, @Nullable Double rainAbsorption, @Nullable Double underwaterFilter) {
        if (attenuationFactor != null) general.attenuationFactor = attenuationFactor;
        if (globalReverbGain != null) general.globalReverbGain = globalReverbGain;
        if (globalReverbBrightness != null) general.globalReverbBrightness = globalReverbBrightness;
        if (globalBlockAbsorption != null) general.globalBlockAbsorption = globalBlockAbsorption;
        if (globalBlockReflectance != null) general.globalBlockReflectance = globalBlockReflectance;
        if (soundSimulationDistance != null) general.soundSimulationDistance = soundSimulationDistance;
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
    public static void setMaterialProperties(ResoundingConfig.Materials materials, @Nullable Map<String, MaterialData> materialProperties) {
        if (materialProperties != null) materialProperties.forEach((s, newData) -> materials.materialProperties.compute(s, (k, v) -> (v == null) ?
                new MaterialData( s,
                        newData.getReflectivity() == -1 ? 0.5 : newData.getReflectivity(),
                        newData.getAbsorption() == -1 ? 0.5 : newData.getAbsorption())
              : new MaterialData( (v.getExample() == null) ? s : v.getExample(),
                        newData.getReflectivity() == -1 ? v.getReflectivity() : newData.getReflectivity(),
                        newData.getAbsorption() == -1 ? v.getAbsorption() : newData.getAbsorption())));
    }

    @Environment(EnvType.CLIENT)
    public static void setMisc(ResoundingConfig.Misc misc, @Nullable Integer continuousRefreshRate, @Nullable Double maxDirectOcclusionFromBlocks, @Nullable Boolean _9RayDirectOcclusion, @Nullable Boolean soundDirectionEvaluation, @Nullable Double directRaysDirEvalMultiplier, @Nullable Boolean notOccludedNoRedirect) {
        if (continuousRefreshRate != null) misc.continuousRefreshRate = continuousRefreshRate;
        if (maxDirectOcclusionFromBlocks != null) misc.maxDirectOcclusionFromBlocks = maxDirectOcclusionFromBlocks;
        if (_9RayDirectOcclusion != null) misc._9RayDirectOcclusion = _9RayDirectOcclusion;
        if (soundDirectionEvaluation != null) misc.soundDirectionEvaluation = soundDirectionEvaluation;
        if (directRaysDirEvalMultiplier != null) misc.directRaysDirEvalMultiplier = directRaysDirEvalMultiplier;
        if (notOccludedNoRedirect != null) misc.notOccludedNoRedirect = notOccludedNoRedirect;
    }
}
