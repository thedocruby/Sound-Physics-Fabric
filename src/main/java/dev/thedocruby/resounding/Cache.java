package dev.thedocruby.resounding;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import dev.thedocruby.resounding.raycast.Branch;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static dev.thedocruby.resounding.Engine.mc;
import static dev.thedocruby.resounding.Utils.LOGGER;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Cache {
    // do these really belong here?
    public final static VoxelShape EMPTY = VoxelShapes.empty();
    public final static VoxelShape CUBE = VoxelShapes.fullCube();

    public final static ExecutorService octreePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static HashMap<String, Material> materials = new HashMap<>();

    public static HashMap<String, Tag> tags = new HashMap<>();

    public static final BlockPos[] branchSequence = {
            new BlockPos(1, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(1, 1, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 1),
            new BlockPos(0, 1, 1),
            new BlockPos(1, 1, 1)
    };
    public static final BlockPos[] blockSequence = ArrayUtils.addFirst(branchSequence, new BlockPos(0, 0, 0));

    static Vec3d playerPos;

    // code for queue
    public static void plantOctree(ChunkChain chunk, int index, Branch root) {
        if (chunk == null) return; // handle unloaded chunks

        growOctree((WorldChunk) chunk, root); // mutates root
        chunk.set(index, root); // "plant" the "grown" octree
        // LOGGER.info("[" + --counter + "] Planted octree at " + ((WorldChunk) chunk).getPos() + "." + index);
    }

    public static Branch growOctree(WorldChunk chunk, Branch root) {
        /*
        if (root.start.getX() > 0 || root.start.getZ() > 0) {
            root.material = null;
            return root;
        }
        //*/
        //root.material = null; /*/
        // determine scale to play with
        final int scale = root.size >> 1;
        final BlockPos start = root.start;
        // get first state at root position
        BlockState state = chunk.getBlockState(start);
        root.material = material(state);
        boolean valid = true;

        if (scale > 1) {
            boolean any = false;
            for (BlockPos block : blockSequence) {
                final BlockPos position = start.add(block.multiply(scale));
                // use recursion here
                Branch leaf = growOctree(chunk, new Branch(position,scale, (Material) null));
                // if (leaf.material != null) {
                if (leaf.material == null) any = any || !leaf.isEmpty();
                else {
                    // any = true;
                    if (!root.material.equals(leaf.material)) {
                        // root.material = null;
                        any = true;
                        valid = false;
                        // any = true;
                    }
                }
                // don't break here, as understanding adjacent sections is important
                root.put(position.asLong(),leaf);
            }
            if (!any) root.empty();
            // for single-blocks
        } else {
            for (BlockPos block : branchSequence) {
                final BlockPos position = start.add(block);
                @NotNull Material next = material(chunk.getBlockState(position));
                // break if next block isn't similar enough
                if (!root.material.equals(next)) {
                    // root.material = null;
                    valid = false;
                    break;
                }
            }
        }
        root.set(valid ? root.material : (Material) null);
        //*/
        return root;
    }

    public static long counter = 0;

    // pattern vars {
    // TODO tagging system
    public static final Pattern spamPattern   = Pattern.compile(".*(rain|lava).*"); // spammy sounds
    public static final Pattern stepPattern   = Pattern.compile(".*(step|pf_).*");  // includes presence_footseps
    public static final Pattern gentlePattern = Pattern.compile(".*(ambient|splash|swim|note|compounded).*");
    public static final Pattern ignorePattern = Pattern.compile(".*(music|voice).*");
    // TODO Occlusion
    //ublic static final Pattern blockPattern = Pattern.compile(".*block..*");
    public static final Pattern uiPattern     = Pattern.compile("ui\\..*");
    static Integer[] colors = new Integer[]
            {
                    Formatting.GREEN.getColorValue(),
                    Formatting.AQUA .getColorValue(), Formatting.LIGHT_PURPLE.getColorValue(), Formatting.DARK_PURPLE.getColorValue(),
                    Formatting.RED  .getColorValue(), Formatting.GOLD        .getColorValue(), Formatting.YELLOW     .getColorValue()
            };

    // coefficient for reflection & coefficient for block permeability (inverse of absorption)
    private static Pair<Double,Double> pair(Double ref, Double perm) { return new Pair<>(ref, perm); }

    // TODO calculate wth atmospherics effect
    // determined by temperature & humidity (global transmission coefficient -> alters permeability)
    public static double transmission = 1;

    // TODO integrate tagging system here
    public static Vec3d adjustSource(SoundCategory category, String tag, Vec3d soundPos) {
        Vec3d offset = new Vec3d(0,0,0);
        if (category == SoundCategory.RECORDS)  offset = offset.add(0.5,0.5,0.5);
        else
        if (stepPattern.matcher(tag).matches()) offset = offset.add(0.0,0.2,0.0);
        // doNineRay = pC.nineRay && (lastSoundCategory == SoundCategory.BLOCKS || isBlock); // TODO: Occlusion
        return uiPattern    .matcher(tag).matches()
                || ignorePattern.matcher(tag).matches()
                || spamPattern  .matcher(tag).matches()
                ? null : soundPos.add(offset);
    }

    @Environment(EnvType.CLIENT) // TODO: is this method needed on server side?
    public static @NotNull Material material(@Nullable BlockState state) {
        // TODO: separate map for fluids? (performance consideration)
        // state.getFluidState().getFluid(); // Fluids.WATER/EMPTY/etc
        // TODO: cascading effect material controllers
        return materials.getOrDefault(state.getBlock().getName(), materials.get("air")); // TODO remove / use block tagging/regex system
//        return materials.getOrDefault(state.getBlock().getName(), null);
    }

    public static void load() {
        // read tags & blocks from game registry
        HashMap<String, LinkedList<String>> tags = new HashMap<>();
        HashMap<String, LinkedList<String>> blocks = new HashMap<>();
        Registry.BLOCK.forEach((Block block) -> {
            String name = block.getTranslationKey();
            block.getDefaultState().streamTags()
                    .forEach((tag) -> {
                        String id = tag.id().toString();
                        Utils.update(tags, id, name);
                        Utils.update(blocks, name, id);
                    });
        });
//        Pattern.compile();
        // TODO create custom tagging system w/ regex here
        // TODO attach materials to tagging system
        // TODO cache tags instead of dynamic determination
    }

    // Recalls tags from config
    // gets the config dir, opens the save file, parses it
    public static boolean recallTags() {
        HashMap<String, Tag> temp = Utils.recall("resounding.tags", Utils.token(Cache.tags), (LinkedTreeMap value) -> new Tag(
                // defaults to air's values
                (String[]) value.getOrDefault("regexes",  new String[] {}),
                (String[]) value.getOrDefault("blocks", new String[] {})
        ));
        if (temp.isEmpty()) return false;
        else Cache.tags = temp;
        return true;
    }

    // Recalls baked materials from config
    // gets the config dir, opens the save file, parses it
    public static boolean recallMaterials() {
        HashMap<String, Material> temp = Utils.recall("resounding.cache", Utils.token(Cache.materials), (LinkedTreeMap value) -> new Material(
                // defaults to air's values
                (double) value.getOrDefault("impedance",  350),
                (double) value.getOrDefault("permeation", 1),
                (double) value.getOrDefault("state",      1)
        ));
        if (temp.isEmpty()) return false;
        else Cache.materials = temp;
        return true;
    }

    // saves baked materials for recalling later
    // serializes materials, gets the config dir, writes to the file
    public static boolean save() {
        if (Cache.materials.isEmpty()) {
            // TODO perhaps generate here?
            LOGGER.error("Cannot save, materials undefined");
            return false;
        }
        String json = new Gson().toJson(Cache.materials, Utils.token(Cache.materials));
        String name = FabricLoader.getInstance().getConfigDir().toAbsolutePath() + FileSystems.getDefault().getSeparator() + "resounding.cache";
        try {
            FileWriter writer = new FileWriter(name);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Failed saving materials", e);
            return false;
        }
        return true;
    }

    // generate / bake materials from resource packs
    // gets resource pack list, filters for relevant sub-file, reads json values, refines the materials to relevant inputs
    @Environment(EnvType.CLIENT)
    public static boolean generate() {
        if (mc.world == null) return false;
        boolean result = recallMaterials();
        if (result) { // TODO extract into GUI handler
            load();
            LOGGER.info("Recalled materials!");
            return true;
        }
        HashMap<String, RawMaterial> config = new HashMap<>();
        // get & loop through enabled packs
        String filename = "resounding.materials.json";
        Collection<ResourcePackProfile> list = mc.getResourcePackManager().getEnabledProfiles();
        for (ResourcePackProfile profile : list) {
            ResourcePack pack = profile.createResourcePack();
            InputStream input;
            // if not available, move on
            try { input = pack.openRoot(filename); }
            catch (IOException e) { continue; }

            LinkedTreeMap<String, LinkedTreeMap> raw = new Gson().fromJson(
                new InputStreamReader(input, UTF_8),
                Utils.token(config)
            );
            // place deserialized values into record.
            // this issue is fixed in GSON 2.10, but not in 2.8.9 (what 1.18.2 uses)
            raw.forEach((String key, LinkedTreeMap value) -> {
                ArrayList<String> solute = (ArrayList<String>) value.getOrDefault("solute", new ArrayList<String>());
                ArrayList<Double> compo = (ArrayList<Double>) value.getOrDefault("composition", new ArrayList<Double>());
                ArrayList<Double> composition = new ArrayList<Double>();
                // adjust for %
                compo.forEach(c -> composition.add(Utils.when(c, .01)));

                config.put(key, new RawMaterial(
                        (Double) value.getOrDefault("weight", null),
                        // default solvent is air
                        (String) value.getOrDefault("solvent", null),
                        solute.toArray(new String[solute.size()]),
                        composition.toArray(new Double[composition.size()]),
                        (Double) value.getOrDefault("granularity", null),
                        (Double) value.getOrDefault("melt", null),
                        (Double) value.getOrDefault("boil", null),
                        (Double) value.getOrDefault("temperature", null),
                        (Double) value.getOrDefault("density", null),
                        (Double) value.getOrDefault("swave", null),
                        (Double) value.getOrDefault("lwave", null)
                ));
            });
        }

        // flatten & refine materials
        HashMap<String, RawMaterial> flat = flattenMaterials(config);
        Cache.materials = refineMaterials(flat);
        // TODO: split this section into assign()?
        load();

        save(); // TODO remove?
        return true;
    }

    // TODO: AIR as base solvent
    public static HashMap<String, Material> refineMaterials(HashMap<String, RawMaterial> rawMaterials) {
        final double temperature = 287.15D;
        // global average for 20th century 14°C/57°F/287°K
        rawMaterials.forEach((String key, RawMaterial raw) -> {
            rawMaterials.put(key,
                    new RawMaterial(raw.weight(),
                        raw.solvent(), raw.solute(), raw.composition(),
                        raw.granularity(),
                        raw.melt(), raw.boil(),
                        temperature,
                        raw.density(),
                        raw.swave(), raw.lwave()
                    )
            );
        });

        HashMap<String, Material> refined = new HashMap<>();
        for (String key : rawMaterials.keySet()) {
            RawMaterial raw = rawMaterials.get(key);
            // if material isn't worthy of a tag, skip it
            if
            (  raw.granularity() == null
            || raw.melt() == null || raw.boil() == null
            || raw.density() == null
            || raw.temperature() == null
            || raw.swave() == null || raw.lwave() == null
            )  continue;
            refine(rawMaterials, refined, key);
        }
        return refined;
    }


    private static Material refine(HashMap<String, RawMaterial> in, HashMap<String, Material> out, String key) {
        return Utils.memoize(in, out, key, (getter, raw) -> {
            // TODO: consider using isFluid() in runtime (affects absorption)
            //     : (isFluid * .25 + 2state)/2
            double state = MathHelper.getLerpProgress(raw.temperature(), raw.melt(), raw.boil());
            double velocity = MathHelper.lerp(state, raw.lwave(), raw.swave());
            double impedance = velocity * raw.density();

            // solvent material is i.8 when undefined.
            // if material supplied doesn't exist, error
            double solvent;
            if (raw.solvent() == null)
                solvent = impedance * 0.8;
            else {
                Material solve = getter.apply(raw.solvent());
                if (solve == null) return null;
                solvent = solve.impedance();
            }

            double permeation;
            // java doesn't handle x.pow(infinity) when x.range(0, < 1) correctly! Fix this.
            if (raw.granularity() == Double.POSITIVE_INFINITY)
                permeation = 0;
            else
                permeation = Math.pow(1-Physics.reflection(impedance, solvent), raw.granularity() * (1 + state));

            return new Material(
                    impedance,
                    permeation,
                    state);
        }, false);
    }

    // flatten all materials
    private static HashMap<String, RawMaterial> flattenMaterials(HashMap<String, RawMaterial> raw) {
        HashMap<String, RawMaterial> flat = new HashMap<>();
        for (String key : raw.keySet()) {
            flatten(raw, flat, key);
        }
        raw = flat;
        return raw;
    }

    // fully calculates a raw material, and recursively flattens all dependencies
    private static void flatten(HashMap<String, RawMaterial> in, HashMap<String, RawMaterial> out, String key) {
        // TODO: could this be done with annotations?
        // NOTE: don't access any non-static closure variables other than (getter, raw) inside the calculation phase
        //       This will change how the compiler sees the lambda, (see: lambda closures)
        //       and will recreate it on every use (resulting in a large performance hit)
        Utils.memoize(in, out, key, (getter, raw) -> {
            // calculation logic
            String[] solute = raw.solute() == null ? new String[]{} : raw.solute();
            int length = solute.length;
            // only calculate if necessary
            if (length > 0) {
                // coefficient initialization
                Double totalWeight = 0D;
                Double[] composition = raw.composition() == null ? new Double[]{} : raw.composition();

                // raw.value.overridden? -> null
                Double[] weight = raw.weight() == null ? new Double[length] : null;
                String   solvent = raw.solvent();
                Double[] granularity = raw.granularity() == null ? new Double[length] : null;
                Double[] melt = raw.melt() == null ? new Double[length] : null;
                Double[] boil = raw.boil() == null ? new Double[length] : null;
                Double[] density = raw.density() == null ? new Double[length] : null;
                Double[] swave = raw.swave() == null ? new Double[length] : null;
                Double[] lwave = raw.lwave() == null ? new Double[length] : null;

                // loop through solute, collect values for weighted calculations
                RawMaterial[] components = new RawMaterial[length];
                for (int i = 0; i < length; i++) {
                    // get values
                    components[i] = getter.apply(solute[i]);
                    if (components[i] == null) {
                        LOGGER.error("{} is invalid or cyclical", solute[i]);
                        return null;
                    }
                    totalWeight += components[i].weight();
                }
                // calculate composition
                for (int i = 0; i < length; i++) {
                    RawMaterial component = components[i];
                    Double percent = composition[i];
                    Double w = component.weight();
                    // calculate composition
                    Double c = totalWeight + totalWeight * percent - w;
                    totalWeight = c; // adjust weight for next item
                    weight[i] = c * percent;

                    // apply composition
                    // 1st component dictates solvent when not present
                    if (solvent == null) solvent = component.solvent();
                    Utils.updWeight(granularity, i, component.granularity(), c * percent);
                    Utils.updWeight(melt, i, component.melt(), c * percent);
                    Utils.updWeight(boil, i, component.boil(), c * percent);
                    Utils.updWeight(density, i, component.density(), c * percent);
                    Utils.updWeight(swave, i, component.swave(), c * percent);
                    Utils.updWeight(lwave, i, component.lwave(), c * percent);
                }

                // apply weights to values and update value
                raw = new RawMaterial(
                        totalWeight,
                        raw.solvent(),
                        null, //raw.solute(),      // for posterity/debug, not needed in runtime
                        raw.composition(), // for posterity/debug, not needed in runtime
                        Utils.unWeight(totalWeight, granularity, raw.granularity()),
                        Utils.unWeight(totalWeight, melt, raw.melt()),
                        Utils.unWeight(totalWeight, boil, raw.boil()),
                        raw.temperature(),
                        Utils.unWeight(totalWeight, density, raw.density()),
                        Utils.unWeight(totalWeight, swave, raw.swave()),
                        Utils.unWeight(totalWeight, lwave, raw.lwave())
                );
            }
            return raw;
        }, false);
    }

}
