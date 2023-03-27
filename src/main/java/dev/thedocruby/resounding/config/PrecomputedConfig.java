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

import static dev.thedocruby.resounding.Engine.nameToGroup;
import static java.util.Map.entry;

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
    @Environment(EnvType.CLIENT) // TODO: Make sure this is used everywhere, add example text
    public static final Map<String, MaterialData> materialDefaults = //<editor-fold desc="Map.ofEntries();">
            Map.<String, MaterialData>ofEntries(
                    entry("Coral",              new MaterialData(null, 0.350, 0.250)),  // Coral              (coral_block)
                    entry("Gravel, Dirt",       new MaterialData(null, 0.500, 0.650)),  // Gravel, Dirt       (gravel, rooted_dirt)
                    entry("Amethyst",           new MaterialData(null, 0.850, 0.400)),  // Amethyst           (amethyst_block, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, amethyst_cluster)
                    entry("Sand",               new MaterialData(null, 0.400, 0.600)),  // Sand               (sand)
                    entry("Candle Wax",         new MaterialData(null, 0.350, 0.400)),  // Candle Wax         (candle)
                    entry("Weeping Vines",      new MaterialData(null, 0.300, 0.300)),  // Weeping Vines      (weeping_vines, weeping_vines_low_pitch)
                    entry("Soul Sand",          new MaterialData(null, 0.050, 0.850)),  // Soul Sand          (soul_sand)
                    entry("Soul Soil",          new MaterialData(null, 0.100, 0.900)),  // Soul Soil          (soul_soil)
                    entry("Basalt",             new MaterialData(null, 0.800, 0.375)),  // Basalt             (basalt)
                    entry("Netherrack",         new MaterialData(null, 0.750, 0.450)),  // Netherrack         (netherrack, nether_ore, nether_gold_ore)
                    entry("Nether Brick",       new MaterialData(null, 0.880, 0.400)),  // Nether Brick       (nether_bricks)
                    entry("Honey",              new MaterialData(null, 0.120, 0.350)),  // Honey              (honey_block)
                    entry("Bone",               new MaterialData(null, 0.900, 0.300)),  // Bone               (bone_block)
                    entry("Nether Wart",        new MaterialData(null, 0.200, 0.800)),  // Nether Wart        (nether_wart, wart_block)
                    entry("Grass, Foliage",     new MaterialData(null, 0.240, 0.240)),  // Grass, Foliage     (grass, crop, bamboo_sapling, sweet_berry_bush)
                    entry("Metal",              new MaterialData(null, 0.950, 0.400)),  // Metal              (metal, copper, anvil)
                    entry("Aquatic Foliage",    new MaterialData(null, 0.550, 0.650)),  // Aquatic Foliage    (wet_grass, lily_pad)
                    entry("Glass, Ice",         new MaterialData(null, 0.900, 0.320)),  // Glass, Ice         (glass)
                    entry("Nether Foliage",     new MaterialData(null, 0.150, 0.500)),  // Nether Foliage     (roots, nether_sprouts)
                    entry("Shroomlight",        new MaterialData(null, 0.850, 0.300)),  // Shroomlight        (shroomlight)
                    entry("Chain",              new MaterialData(null, 0.800, 0.550)),  // Chain              (chain)
                    entry("Deepslate",          new MaterialData(null, 0.940, 0.600)),  // Deepslate          (deepslate)
                    entry("Wood",               new MaterialData(null, 0.675, 0.400)),  // Wood               (wood, ladder)
                    entry("Deepslate Tiles",    new MaterialData(null, 0.975, 0.525)),  // Deepslate Tiles    (deepslate_tiles)
                    entry("Stone, Blackstone",  new MaterialData(null, 0.900, 0.500)),  // Stone, Blackstone  (stone, calcite, gilded_blackstone)
                    entry("Slime",              new MaterialData(null, 0.880, 0.620)),  // Slime              (slime_block)
                    entry("Polished Deepslate", new MaterialData(null, 0.975, 0.600)),  // Polished Deepslate (polished_deepslate, deepslate_bricks)
                    entry("Snow",               new MaterialData(null, 0.250, 0.420)),  // Snow               (snow)
                    entry("Azalea Leaves",      new MaterialData(null, 0.300, 0.350)),  // Azalea Leaves      (azalea_leaves)
                    entry("Bamboo",             new MaterialData(null, 0.600, 0.300)),  // Bamboo             (bamboo, scaffolding)
                    entry("Mushroom Stems",     new MaterialData(null, 0.600, 0.650)),  // Mushroom Stems     (stem)
                    entry("Wool",               new MaterialData(null, 0.025, 0.950)),  // Wool               (wool)
                    entry("Dry Foliage",        new MaterialData(null, 0.250, 0.150)),  // Dry Foliage        (vine, hanging_roots, glow_lichen)
                    entry("Azalea Bush",        new MaterialData(null, 0.300, 0.450)),  // Azalea Bush        (azalea)
                    entry("Lush Cave Foliage",  new MaterialData(null, 0.350, 0.250)),  // Lush Foliage       (cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
                    entry("Netherite",          new MaterialData(null, 0.995, 0.300)),  // Netherite          (netherite_block, lodestone)
                    entry("Ancient Debris",     new MaterialData(null, 0.450, 0.800)),  // Ancient Debris     (ancient_debris)
                    entry("Nether Fungus Stem", new MaterialData(null, 0.300, 0.650)),  // Nether Fungus Stem (nether_stem)
                    entry("Powder Snow",        new MaterialData(null, 0.180, 0.100)),  // Powder Snow        (powder_snow)
                    entry("Tuff",               new MaterialData(null, 0.750, 0.400)),  // Tuff               (tuff)
                    entry("Moss",               new MaterialData(null, 0.200, 0.400)),  // Moss               (moss, moss_carpet)
                    entry("Nylium",             new MaterialData(null, 0.400, 0.500)),  // Nylium             (nylium)
                    entry("Nether Mushroom",    new MaterialData(null, 0.250, 0.750)),  // Nether Mushroom    (fungus)
                    entry("Lanterns",           new MaterialData(null, 0.750, 0.350)),  // Lanterns           (lantern)
                    entry("Dripstone",          new MaterialData(null, 0.850, 0.320)),  // Dripstone          (dripstone_block, pointed_dripstone)
                    entry("Sculk Sensor",       new MaterialData(null, 0.150, 0.850)),  // Sculk Sensor       (sculk_sensor)
                    entry("DEFAULT",            new MaterialData(null, 0.500, 0.500))   // Default Material   ()
            );//</editor-fold>
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
    public boolean debug;

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

            defaultRefl = c.materials.materialProperties.get("DEFAULT").reflectivity;
            defaultAbs = c.materials.materialProperties.get("DEFAULT").absorption;
            blockWhiteSet = new HashSet<>(c.materials.blockWhiteList);
            Map<String, MaterialData> matProp = new HashMap<>(c.materials.materialProperties);
            c.materials.blockWhiteList.stream()
                    .map(a -> new Pair<>(a, matProp.get(a)))
                    .forEach(e -> {
                        if (e.getRight() != null && Double.isFinite(e.getRight().reflectivity) && Double.isFinite(e.getRight().absorption)) {
                            if (e.getRight().example == null || e.getRight().example.isBlank()) {
                                matProp.put(e.getLeft(), new MaterialData(e.getLeft(), e.getRight().reflectivity, e.getRight().absorption));
                            }
                            return;
                        }
                        Engine.LOGGER.error("Missing material data for {}, Default entry created.", e.getLeft());
                        final MaterialData newData = new MaterialData(e.getLeft(), defaultRefl, defaultAbs);
                        matProp.put(e.getLeft(), newData);
                    });

            reflMap = new HashMap<>();
            absMap = new HashMap<>();
            final List<String> wrong = new java.util.ArrayList<>();
            final List<String> toRemove = new java.util.ArrayList<>();
            matProp.forEach((k, v) -> { //TODO Materials need to be reworked.
                if (nameToGroup.containsKey(k) || blockWhiteSet.contains(k)) {
                    reflMap.put(k, Math.pow(v.reflectivity, globalReflRcp));
                    absMap.put(k, v.absorption);
                } else if (!k.equals("DEFAULT")) {
                    wrong.add(k + " (" + v.example + ")");
                    toRemove.add(k);
                }
            });
            if (!wrong.isEmpty()) {
                Engine.LOGGER.error("Material Data map contains {} extra entries:\n{}\nPatching Material Data...", wrong.size(), Arrays.toString(new List[]{wrong}));
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
            debug = c.debug.raytraceParticles;
        } else {
            soundSimulationDistance = c.server.soundSimulationDistance;
        }
    }

    public void deactivate(){ active = false;}
}
