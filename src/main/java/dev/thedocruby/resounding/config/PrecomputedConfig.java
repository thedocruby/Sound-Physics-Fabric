package dev.thedocruby.resounding.config;

import dev.thedocruby.resounding.ResoundingEngine;
import dev.thedocruby.resounding.toolbox.MaterialData;
import dev.thedocruby.resounding.toolbox.OcclusionMode;
import dev.thedocruby.resounding.toolbox.SharedAirspaceMode;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.stream.Collectors;

import static dev.thedocruby.resounding.ResoundingEngine.nameToGroup;
import static java.util.Map.entry;

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
    public static double speedOfSound = 343.3;;
    @Environment(EnvType.CLIENT) // TODO: Make sure this is used everywhere, add example text
    public static Map<String, MaterialData> materialDefaults = //<editor-fold desc="Map.ofEntries();">
            Map.<String, MaterialData>ofEntries(
                    entry("Coral", new MaterialData(null, 0.2,                  0.45 )),    // Coral              (coral_block)
                    entry("Gravel, Dirt", new MaterialData(null, 0.5,                  0.4  )),    // Gravel, Dirt       (gravel, rooted_dirt)
                    entry("Amethyst", new MaterialData(null, 0.75,                 0.45 )),    // Amethyst           (amethyst_block, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, amethyst_cluster)
                    entry("Sand", new MaterialData(null, 0.1,                  0.45 )),    // Sand               (sand)
                    entry("Candle Wax", new MaterialData(null, 0.4,                  0.3  )),    // Candle Wax         (candle)
                    entry("Weeping Vines", new MaterialData(null, 0.2,                  0.2  )),    // Weeping Vines      (weeping_vines, weeping_vines_low_pitch)
                    entry("Soul Sand", new MaterialData(null, 0.05,                 0.65 )),    // Soul Sand          (soul_sand)
                    entry("Soul Soil", new MaterialData(null, 0.1,                  0.7  )),    // Soul Soil          (soul_soil)
                    entry("Basalt", new MaterialData(null, 0.8,                  0.3  )),    // Basalt             (basalt)
                    entry("Netherrack", new MaterialData(null, 0.75,                 0.45 )),    // Netherrack         (netherrack, nether_ore, nether_gold_ore)
                    entry("Nether Brick", new MaterialData(null, 0.85,                 0.55 )),    // Nether Brick       (nether_bricks)
                    entry("Honey", new MaterialData(null, 0.08,                 0.85 )),    // Honey              (honey_block)
                    entry("Bone", new MaterialData(null, 0.7,                  0.55 )),    // Bone               (bone_block)
                    entry("Nether Wart", new MaterialData(null, 0.2,                  0.8  )),    // Nether Wart        (nether_wart, wart_block)
                    entry("Grass, Foliage", new MaterialData(null, 0.2,                  0.6  )),    // Grass, Foliage  (grass, crop, bamboo_sapling, sweet_berry_bush)
                    entry("Metal", new MaterialData(null, 0.85,                 0.5  )),    // Metal              (metal, copper, anvil)
                    entry("Aquatic Foliage", new MaterialData(null, 0.15,                 0.8  )),    // Aquatic Foliage    (wet_grass, lily_pad)
                    entry("Glass, Ice", new MaterialData(null, 0.5,                  0.45 )),    // Glass, Ice         (glass)
                    entry("Sculk Sensor", new MaterialData(null, 0.4,                  0.6  )),    // Sculk Sensor       (sculk_sensor)
                    entry("Nether Foliage", new MaterialData(null, 0.15,                 0.55 )),    // Nether Foliage     (roots, nether_sprouts)
                    entry("Shroomlight", new MaterialData(null, 0.85,                 0.75 )),    // Shroomlight        (shroomlight)
                    entry("Chain", new MaterialData(null, 0.4,                  0.4  )),    // Chain              (chain)
                    entry("Deepslate", new MaterialData(null, 0.88,                 0.55 )),    // Deepslate          (deepslate)
                    entry("Wood", new MaterialData(null, 0.65,                 0.45 )),    // Wood               (wood, ladder)
                    entry("Deepslate Tiles", new MaterialData(null, 0.95,                 0.55 )),    // Deepslate Tiles    (deepslate_tiles)
                    entry("Stone, Blackstone", new MaterialData(null, 0.83,                 0.5  )),    // Stone, Blackstone  (stone, calcite, gilded_blackstone)
                    entry("Slime", new MaterialData(null, 1.0,                  0.25 )),    // Slime              (slime_block)
                    entry("Polished Deepslate", new MaterialData(null, 0.99,                 0.55 )),    // Polished Deepslate (polished_deepslate, deepslate_bricks)
                    entry("Snow", new MaterialData(null, 0.1,                  0.5  )),    // Snow               (snow)
                    entry("Azalea Leaves", new MaterialData(null, 0.3,                  0.35 )),    // Azalea Leaves      (azalea_leaves)
                    entry("Bamboo", new MaterialData(null, 0.5,                  0.4  )),    // Bamboo             (bamboo, scaffolding)
                    entry("Mushroom Stems", new MaterialData(null, 0.6,                  0.65 )),    // Mushroom Stems     (stem)
                    entry("Wool", new MaterialData(null, 0.02,                 1.0  )),    // Wool               (wool)
                    entry("Dry Foliage", new MaterialData(null, 0.1,                  0.15 )),    // Dry Foliage        (vine, hanging_roots, glow_lichen)
                    entry("Azalea Bush", new MaterialData(null, 0.15,                 0.5  )),    // Azalea Bush        (azalea)
                    entry("Lush Cave Foliage", new MaterialData(null, 0.2,                  0.2  )),    // Lush Foliage       (cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
                    entry("Netherite", new MaterialData(null, 1.0,                  0.6  )),    // Netherite          (netherite_block, lodestone)
                    entry("Ancient Debris", new MaterialData(null, 0.45,                 0.8  )),    // Ancient Debris     (ancient_debris)
                    entry("Nether Fungus Stem", new MaterialData(null, 0.3,                  0.55 )),    // Nether Fungus Stem (nether_stem)
                    entry("Powder Snow", new MaterialData(null, 0.01,                 0.1  )),    // Powder Snow        (powder_snow)
                    entry("Tuff", new MaterialData(null, 0.35,                 0.4  )),    // Tuff               (tuff)
                    entry("Moss", new MaterialData(null, 0.1,                  0.85 )),    // Moss               (moss, moss_carpet)
                    entry("Nylium", new MaterialData(null, 0.4,                  0.55 )),    // Nylium             (nylium)
                    entry("Nether Mushroom", new MaterialData(null, 0.4,                  0.6  )),    // Nether Mushroom      (fungus)
                    entry("Lanterns", new MaterialData(null, 0.75,                 0.4  )),    // Lanterns           (lantern)
                    entry("Dripstone", new MaterialData(null, 0.9,                  0.6  )),    // Dripstone          (dripstone_block, pointed_dripstone)
                    entry("DEFAULT", new MaterialData("Default Material", 0.5,                  0.5  ))     // Default Material   ()
            );//</editor-fold>
    @Environment(EnvType.CLIENT)
    public double maxDecayTime = 4.142; // TODO: add config setting for this
    public static PrecomputedConfig pC = null;

    public boolean enabled;

    @Environment(EnvType.CLIENT)
    public double globalRvrbGain;
    @Environment(EnvType.CLIENT)
    public double minEnergy;
    @Environment(EnvType.CLIENT)
    public int resolution;
    @Environment(EnvType.CLIENT)
    public double warpFactor;
    @Environment(EnvType.CLIENT)
    public double globalRvrbHFRcp;
    @Environment(EnvType.CLIENT)
    public double rvrbDensity;
    @Environment(EnvType.CLIENT)
    public double globalAbs;
    @Environment(EnvType.CLIENT)
    public double globalAbsHFRcp;
    public final int soundSimulationDistance;
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
    public Reference2DoubleOpenHashMap<BlockSoundGroup> reflMap;
    @Environment(EnvType.CLIENT)
    public double defaultRefl;
    @Environment(EnvType.CLIENT)
    public Reference2DoubleOpenHashMap<BlockSoundGroup> absMap;
    @Environment(EnvType.CLIENT)
    public double defaultAbs;
    @Environment(EnvType.CLIENT)
    public Set<String> blockWhiteSet;
    @Environment(EnvType.CLIENT)
    public Map<String, MaterialData> blockWhiteMap;

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

    public PrecomputedConfig(ResoundingConfig c) throws CloneNotSupportedException { // TODO: Rework this
        if (pC != null && pC.active) throw new CloneNotSupportedException("Tried creating second instance of precomputedConfig");
        enabled = c.enabled;

        soundSimulationDistance = c.quality.soundSimulationDistance;

        if(ResoundingEngine.env == EnvType.CLIENT) { // TODO: organize
            defaultAttenuationFactor = c.general.attenuationFactor;
            globalRvrbGain = MathHelper.clamp(c.general.globalReverbGain/100d, 0.0d, 1.0d);
            minEnergy = Math.exp(-Math.max(c.general.globalReverbStrength, 0));
            resolution = c.quality.reverbResolution;
            warpFactor = 1 / MathHelper.clamp(c.misc.reverbBias, 1.0, 5.0);
            globalRvrbHFRcp = (float) (1 / Math.max(c.general.globalReverbBrightness, Math.pow(10, -64)));
            rvrbDensity = 1 - MathHelper.clamp(c.general.globalReverbSmoothness, 0.0, 1.0);
            globalAbs = c.general.globalBlockAbsorption;
            globalAbsHFRcp = globalRvrbHFRcp /*c.general.globalAbsorptionBrightness*/; // TODO: Occlusion
            globalRefl = c.general.globalBlockReflectance;
            globalReflRcp = 1 / globalRefl;
            airAbs = (float) MathHelper.clamp(c.effects.airAbsorption, 0.0, 10.0);
            humAbs = (float) MathHelper.clamp(c.effects.humidityAbsorption, 0.0, 4.0);
            rainAbs = (float) MathHelper.clamp(c.effects.rainAbsorption, 0.0, 2.0);
            waterFilt = 1 - MathHelper.clamp(c.effects.underwaterFilter, 0.0, 1.0);

            skipRainOccl = c.misc.skipRainOcclusionTracing;
            nRays = c.quality.envEvalRays;
            rcpNRays = 1d / nRays;
            nRayBounces = c.quality.envEvalRayBounces + resolution;
            rcpAllRays = rcpNRays / nRayBounces;
            maxTraceDist = MathHelper.clamp(c.quality.rayLength, 1.0, 16.0) * nRayBounces * 16 * Math.sqrt(2);
            fastShared = c.quality.sharedAirspaceMode == SharedAirspaceMode.FAST;
            occlMode = c.quality.occlusionMode;
            fastPick = true; // TODO: Make config setting for this

            defaultRefl = c.materials.materialProperties.get("DEFAULT").reflectivity;
            defaultAbs = c.materials.materialProperties.get("DEFAULT").absorption;
            blockWhiteSet = new HashSet<>(c.materials.blockWhiteList);
            Map<String, MaterialData> matProp = new HashMap<>(c.materials.materialProperties);
            blockWhiteMap = c.materials.blockWhiteList.stream()
                    .map(a -> new Pair<>(a, matProp.get(a)))
                    .map(e -> {
                        if (e.getRight() != null) {
                            if (e.getRight().example == null || e.getRight().example.isBlank()) {
                                matProp.replace(e.getLeft(), new MaterialData(e.getLeft(), e.getRight().reflectivity, e.getRight().absorption));
                                e.setRight(matProp.get(e.getLeft()));
                            }
                            return e;
                        }
                        ResoundingEngine.LOGGER.error("Missing material data for {}, Default entry created.", e.getLeft());
                        final MaterialData newData = new MaterialData(e.getLeft(), defaultRefl, defaultAbs);
                        matProp.put(e.getLeft(), newData);
                        e.setRight(newData);
                        return e;
                    }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

            reflMap = new Reference2DoubleOpenHashMap<>();
            absMap = new Reference2DoubleOpenHashMap<>();
            final List<String> wrong = new java.util.ArrayList<>();
            final List<String> toRemove = new java.util.ArrayList<>();
            matProp.forEach((k, v) -> { //TODO Materials need to be reworked.
                if (nameToGroup.containsKey(k)) {
                    BlockSoundGroup bsg = nameToGroup.get(k);
                    reflMap.put(bsg, v.reflectivity);
                    absMap.put(bsg, v.absorption);
                } else {
                    if (!blockWhiteSet.contains(k) && !k.equals("DEFAULT")) {
                        wrong.add(k + " (" + v.example + ")");
                        toRemove.add(k);
                    }
                }
            });
            if (!wrong.isEmpty()) {
                ResoundingEngine.LOGGER.error("Material Data map contains {} extra entries:\n{}\nPatching Material Data...", wrong.size(), Arrays.toString(new List[]{wrong}));
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
        }

        dLog = c.debug.debugLogging;
        oLog = c.debug.occlusionLogging;
        eLog = c.debug.environmentLogging;
        pLog = c.debug.performanceLogging;
        dRays = c.debug.raytraceParticles;
    }

    public void deactivate(){ active = false;}
}
