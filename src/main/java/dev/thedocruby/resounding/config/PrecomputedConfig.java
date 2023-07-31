package dev.thedocruby.resounding.config;

import dev.thedocruby.resounding.Engine;
import dev.thedocruby.resounding.toolbox.MaterialData;
import dev.thedocruby.resounding.toolbox.OcclusionMode;
import dev.thedocruby.resounding.toolbox.SharedAirspaceMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;

import java.util.*;

import static dev.thedocruby.resounding.Cache.nameToGroup;

/*
    Values, which remain constant after the config has changed
    Only one instance allowed
 */
public class PrecomputedConfig {
    @Environment(EnvType.CLIENT)
    public static final float globalVolumeMultiplier = 4f;
    @Environment(EnvType.CLIENT)
    public static final double speedOfSound = 343.3;
    public static final double minEnergy = Math.exp(-9.21);
    @Environment(EnvType.CLIENT)
    public double maxDecayTime = 4.142; // TODO: add config setting for this
    public static PrecomputedConfig pC = null;

    public boolean enabled;
    public int soundSimulationDistance;

    @Environment(EnvType.CLIENT)
    public double globalRvrbGain;
    @Environment(EnvType.CLIENT)
    public double energyFix;
    @Environment(EnvType.CLIENT)
    public int resolution;
    @Environment(EnvType.CLIENT)
    public double globalRvrbHFRcp;
    @Environment(EnvType.CLIENT)
    public double rvrbDiff;
    @Environment(EnvType.CLIENT)
    public double globalAbs;
    @Environment(EnvType.CLIENT)
    public double globalAbsHFRcp;
    @Environment(EnvType.CLIENT)
    public double globalRefl;
    @Environment(EnvType.CLIENT)
    public double globalReflRcp;
    @Environment(EnvType.CLIENT)
    public float airAbs;
    @Environment(EnvType.CLIENT)
    public float humAbs;
    @Environment(EnvType.CLIENT)
    public float rainAbs;
    @Environment(EnvType.CLIENT)
    public double waterFilt;

    @Environment(EnvType.CLIENT)
    public boolean skipRainOccl;
    @Environment(EnvType.CLIENT)
    public int nRays;
    @Environment(EnvType.CLIENT)
    public double rcpNRays;
    @Environment(EnvType.CLIENT)
    public int nRayBounces;
    @Environment(EnvType.CLIENT)
    public double rcpAllRays;
    @Environment(EnvType.CLIENT)
    public double maxTraceDist;
    @Environment(EnvType.CLIENT)
    public OcclusionMode occlMode;
    @Environment(EnvType.CLIENT)
    public boolean fastShared;
    @Environment(EnvType.CLIENT)
    public boolean fastPick;

    @Environment(EnvType.CLIENT)
    public Map<String, Double> reflMap;
    @Environment(EnvType.CLIENT)
    public double defaultRefl;
    @Environment(EnvType.CLIENT)
    public Map<String, Double> absMap;
    @Environment(EnvType.CLIENT)
    public double defaultAbs;
    @Environment(EnvType.CLIENT)
    public Set<String> blockWhiteSet;

    @Environment(EnvType.CLIENT)
    public boolean recordsDisable;
    @Environment(EnvType.CLIENT)
    public int srcRefrRate;
    @Environment(EnvType.CLIENT)
    public double maxBlckOccl;
    @Environment(EnvType.CLIENT)
    public boolean nineRay;
    @Environment(EnvType.CLIENT)
    public boolean dirEval;
    @Environment(EnvType.CLIENT)
    public double dirEvalBias;
    @Environment(EnvType.CLIENT)
    public boolean notOcclRedir;

    @Environment(EnvType.CLIENT)
    public boolean dLog;
    @Environment(EnvType.CLIENT)
    public boolean oLog;
    @Environment(EnvType.CLIENT)
    public boolean eLog;
    @Environment(EnvType.CLIENT)
    public boolean pLog;
    @Environment(EnvType.CLIENT)
    public boolean dRays;
    @Environment(EnvType.CLIENT)
    public boolean debug; // for each log/debug
    @Environment(EnvType.CLIENT)
    public boolean log; // for each log

    @Environment(EnvType.CLIENT)
    public double threshold = 0.1; // distance value for material properties to be considered equal

    private boolean active = true;

    public PrecomputedConfig(ResoundingConfig c) throws CloneNotSupportedException { // TODO: Rework this
        if (pC != null && pC.active) throw new CloneNotSupportedException("Tried creating second instance of precomputedConfig");
        enabled = c.enabled;

        if(Engine.env == EnvType.CLIENT) { // TODO: organize
            globalRvrbGain = MathHelper.clamp(c.general.globalReverbGain/100d, 0.0d, 1.0d);
            energyFix = 1 / Math.max(c.general.globalReverbStrength, Double.MIN_NORMAL);
            resolution = c.quality.reverbResolution;
            globalRvrbHFRcp = 1 / Math.max(c.general.globalReverbBrightness, Double.MIN_NORMAL);
            rvrbDiff = MathHelper.clamp(c.general.globalReverbSmoothness, 0.0, 1.0);
            globalAbs = c.general.globalBlockAbsorption;
            globalAbsHFRcp = 1 / Math.max(c.general.globalAbsorptionBrightness, Double.MIN_NORMAL);
            globalRefl = c.general.globalBlockReflectance;
            globalReflRcp = 1 / globalRefl;
            // TODO implement environment functions
            // airAbs = (float) MathHelper.clamp(c.effects.airAbsorption, 0.0, 10.0);
            // humAbs = (float) MathHelper.clamp(c.effects.humidityAbsorption, 0.0, 4.0);
            // rainAbs = (float) MathHelper.clamp(c.effects.rainAbsorption, 0.0, 2.0);
            waterFilt = 1 - MathHelper.clamp(c.effects.underwaterFilter, 0.0, 1.0);
            soundSimulationDistance = c.quality.soundSimulationDistance;

            skipRainOccl = c.misc.skipRainOcclusionTracing;
            nRays = c.quality.envEvalRays;
            rcpNRays = 1d / nRays;
            nRayBounces = c.quality.envEvalRayBounces + resolution;
            rcpAllRays = rcpNRays / nRayBounces;
            maxTraceDist = MathHelper.clamp(c.quality.rayLength, 1.0, 16.0) * nRayBounces * 16 * Math.sqrt(2);
            occlMode = c.quality.occlusionMode;
            fastShared = c.quality.sharedAirspaceMode == SharedAirspaceMode.FAST;
            fastPick = true; // TODO: Make config setting for this

            defaultRefl = c.materials.materialProperties.get("DEFAULT").reflectivity();
            defaultAbs = c.materials.materialProperties.get("DEFAULT").permeability();
            blockWhiteSet = new HashSet<>(c.materials.blockWhiteList);
            Map<String, MaterialData> matProp = new HashMap<>(c.materials.materialProperties);
            c.materials.blockWhiteList.stream()
                    .map(a -> new Pair<>(a, matProp.get(a)))
                    .forEach(e -> {
                        if (e.getRight() != null && Double.isFinite(e.getRight().reflectivity()) && Double.isFinite(e.getRight().permeability())) {
                            if (e.getRight().example() == null || e.getRight().example().isBlank()) {
                                matProp.put(e.getLeft(), new MaterialData(e.getLeft(), e.getRight().reflectivity(), e.getRight().permeability()));
                            }
                            return;
                        }
                        Engine.LOGGER.error("Missing material data for {}, Default entry created.", e.getLeft());
                        final MaterialData newData = new MaterialData(e.getLeft(), defaultRefl, defaultAbs);
                        matProp.put(e.getLeft(), newData);
                    });

            reflMap = new HashMap<>();
            absMap = new HashMap<>();
            final List<String> wrong = new ArrayList<>();
            final List<String> toRemove = new ArrayList<>();
            matProp.forEach((k, v) -> { //TODO Materials need to be reworked.
                if (nameToGroup.containsKey(k) || blockWhiteSet.contains(k)) {
                    reflMap.put(k, Math.pow(v.reflectivity(), globalReflRcp));
                    absMap.put(k, v.permeability());
                } else if (!k.equals("DEFAULT")) {
                    wrong.add(k + " (" + v.example() + ")");
                    toRemove.add(k);
                }
            });
            if (!wrong.isEmpty()) {
                Engine.LOGGER.error("Material Data map contains {} extra entries:\n{}\nPatching Material Data...", wrong.size(), wrong);
                toRemove.forEach(matProp::remove);
            }

            c.materials.materialProperties = matProp;

            recordsDisable = c.misc.recordsDisable;
            srcRefrRate = Math.max(c.quality.sourceRefreshRate, 1);
            maxBlckOccl = c.quality.maxBlockOcclusion;
            nineRay = c.quality.nineRayBlockOcclusion;
            dirEval = c.effects.soundDirectionEvaluation; // TODO: DirEval
            dirEvalBias = Math.pow(c.effects.directRaysDirEvalMultiplier, 10.66); // TODO: DirEval
            notOcclRedir = !c.misc.notOccludedNoRedirect;

            dLog = c.debug.debugLogging;
            oLog = c.debug.occlusionLogging;
            eLog = c.debug.environmentLogging;
            pLog = c.debug.performanceLogging;
            dRays = c.debug.raytraceParticles;
            log = dLog || oLog || eLog || pLog;
            debug = log || dRays;
        } else {
            soundSimulationDistance = c.server.soundSimulationDistance;
        }
    }

    public void deactivate(){ active = false;}
}
