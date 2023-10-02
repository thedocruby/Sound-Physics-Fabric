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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static dev.thedocruby.resounding.Engine.mc;
import static dev.thedocruby.resounding.Utils.LOGGER;

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
    private static HashMap<String, LinkedList<String>> blocks;

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





    // TODO integrate
    // Recalls baked materials from config
    // gets the config dir, opens the save file, parses it
    public static boolean recall() {
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
        // TODO figure out in-menu-cache generation
        if (mc.world == null) return false;
        // TODO save/recall cacheTable
//        boolean result = recallMaterials();
        HashMap<String, RawMaterial> materials = new HashMap<>();
        HashMap<String, RawTag> tags = new HashMap<>();
        // get & loop through enabled packs
        Collection<ResourcePackProfile> list = mc.getResourcePackManager().getEnabledProfiles();
        for (ResourcePackProfile profile : list) {
            ResourcePack pack = profile.createResourcePack();
            materials.putAll(Utils.resource(pack, "resounding.materials.json", Utils.token(materials), Cache::deserializeMaterials));
            tags.putAll(Utils.resource(pack, "resounding.tags.json", Utils.token(tags), Cache::deserializeTags));
        }


        // read tags & blocks from game registry
        // TODO: make static
        Registry.BLOCK.forEach((Block block) -> {
            String name = block.getTranslationKey();
            block.getDefaultState().streamTags()
                    .forEach((tag) -> {
                        String id = tag.id().toString();
                        // extrapolate old tags & append
                        tags.put(id,
                                new RawTag(null,
                                        tags.getOrDefault(
                                                id, new RawTag(null, new String[]{}, null, null)
                                        ).blocks(),
                                null, null)
                        );
                        Utils.update(blocks, name, id);
                    });
        });

        Cache.materials = finalizeMaterials(materials);
        Cache.tags = finalizeTags(tags);

        save(); // TODO remove?
        return true;
    }

    private static Material bake(HashMap<String, RawMaterial> in, HashMap<String, Material> out, String key) {
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




    private static RawTag deserializeTags(LinkedTreeMap value) {
        final Function<String[],Pattern[]> toPatterns = patterns -> (Pattern[]) Arrays.stream(patterns).map(Pattern::compile).toArray();
        return new RawTag(
                toPatterns.apply(((String[]) value.getOrDefault("patterns",  new String[] {}))),
                (String[]) value.getOrDefault("blocks", new String[] {}),

                toPatterns.apply(((String[]) value.getOrDefault("tagPatterns",  new String[] {}))),
                (String[]) value.getOrDefault("tags", new String[] {})
        );
    }
    public static HashMap<String, Tag> flattenTags(HashMap<String, RawTag> tags, HashMap<String, LinkedList<String>> blocks) {
        HashMap<String, Tag> output = new HashMap<>();
        Set<String> tagNames = tags.keySet();
        // loop through all tags and check all blocks against them
        for (String name : tags.keySet()) {
            // TODO: could this be done with annotations?
            // NOTE: don't access any non-static closure variables other than (getter, raw) inside the calculation phase
            //       This will change how the compiler sees the lambda, (see: lambda closures)
            //       and will recreate it on every use (resulting in a large performance hit)
            // in: [RawTag], out: [Tag], getter returns Tags (hint: .blocks())
            // Cache.tags must be pre-populated with game's default tags
            Utils.memoize(tags, output, name, (getter, raw) -> {
                // get already populated info
                LinkedList<String> self = (LinkedList<String>) Arrays.stream(tags.getOrDefault(name,
                        new RawTag(null, new String[]{}, null, null)
                ).blocks()).toList();

                Utils.granularFilter(tagNames, raw.tagPatterns(), raw.tags()).map(getter).forEach(tag -> {
                    self.addAll(
                            Arrays.stream(tag.blocks()).toList()
                    );
                });
                Utils.granularFilter(blocks.keySet(), raw.patterns(), raw.blocks()).forEach(block -> {
                    self.add(block);
                    // update reverse mapping, too
                    Utils.update(blocks, block, name);
                });

                return new Tag(self.toArray(new String[]{}));
            });
        }
        return output;
        // TODO attach materials to tagging system
        // TODO cache tags instead of dynamic determination
    }
    private static HashMap<String, Tag> finalizeTags(HashMap<String, RawTag> tags) {
        return flattenTags(tags, Cache.blocks);
    }

    private static RawMaterial deserializeMaterials(LinkedTreeMap value) {
        final Double[] compo = (Double[]) value.getOrDefault("composition", new Double[]{});
        // adjust for %
        final Stream<Double> composition = Arrays.stream(compo).map(c -> Utils.when(c, .01));

        return new RawMaterial(
                (Double) value.getOrDefault("weight", null),
                // default solvent is air
                (String) value.getOrDefault("solvent", null),
                (String[]) value.getOrDefault("solute", new String[]{}),
                (Double[]) composition.toArray(),
                (Boolean) value.getOrDefault("ratio", false),
                (Double) value.getOrDefault("granularity", null),
                (Double) value.getOrDefault("melt", null),
                (Double) value.getOrDefault("boil", null),
                (Double) value.getOrDefault("temperature", null),
                (Double) value.getOrDefault("density", null),
                (Double) value.getOrDefault("swave", null),
                (Double) value.getOrDefault("lwave", null)
        );
    }
    private static HashMap<String, RawMaterial> flattenMaterials(HashMap<String, RawMaterial> in) {
        HashMap<String, RawMaterial> flat = new HashMap<>();
        for (String key : in.keySet()) {
            // TODO: could this be done with annotations?
            // NOTE: don't access any non-static closure variables other than (getter, raw) inside the calculation phase
            //       This will change how the compiler sees the lambda, (see: lambda closures)
            //       and will recreate it on every use (resulting in a large performance hit)
            // fully calculates a raw material, and recursively flattens all dependencies
            Utils.memoize(in, flat, key, (getter, raw) -> {
                // calculation logic
                String[] solute = raw.solute() == null ? new String[]{} : raw.solute();
                Double[] composition = raw.composition() == null ? new Double[]{} : raw.composition();
                int length = solute.length;
                boolean ratio = raw.ratio() != null && raw.ratio();
                // only calculate if necessary
                if (length > 0) {
                    Property weight = new Property(ratio);
                    Property granularity = new Property(ratio);
                    Property melt = new Property(ratio);
                    Property boil = new Property(ratio);
                    Property temperature = new Property(ratio);
                    Property density = new Property(ratio);
                    Property swave = new Property(ratio);
                    Property lwave = new Property(ratio);
                    LinkedList<String> errors = new LinkedList<>();
                    for (int i = 0; i < length; i++) {
                        String name = solute[i];
                        Double count = composition[i];
                        // get values
                        RawMaterial material = getter.apply(name);
                        if (material == null) {
                            errors.add(name);
                            continue;
                        }
                        // save values
                        weight.add(material.weight(), 1D, count);
                        granularity.add(material.granularity(), material.weight(), count);
                        melt.add(material.melt(), material.weight(), count);
                        boil.add(material.boil(), material.weight(), count);
                        temperature.add(material.temperature(), material.weight(), count);
                        density.add(material.density(), material.weight(), count);
                        swave.add(material.swave(), material.weight(), count);
                        lwave.add(material.lwave(), material.weight(), count);
                    }
                    // spew errors
                    if (!errors.isEmpty()) {
                        for (String error : errors)
                            // TODO identify reasonable solution to prevent suboptimal non-static reference here
                            LOGGER.error("{} in {} is invalid or cyclical", error, key);
                        return null;
                    }
                    raw = new RawMaterial(
                            weight.get(),
                            raw.solvent(),     // for posterity/debug, not needed in runtime
                            raw.solute(),      // for posterity/debug, not needed in runtime
                            raw.composition(), // for posterity/debug, not needed in runtime
                            ratio,
                            granularity.get(),
                            melt.get(),
                            boil.get(),
                            temperature.get(),
                            density.get(),
                            swave.get(),
                            lwave.get()
                    );
                }
                return raw;
            }, false);
        }
        in = flat;
        return in;
    }
    // TODO: AIR as base solvent
    public static HashMap<String, Material> refineMaterials(HashMap<String, RawMaterial> rawMaterials) {
        final double temperature = 287.15D;
        // global average for 20th century 14°C/57°F/287°K
        rawMaterials.forEach((String key, RawMaterial raw) -> {
            rawMaterials.put(key,
                    new RawMaterial(raw.weight(),
                            raw.solvent(), raw.solute(), raw.composition(),
                            raw.ratio(),
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
            bake(rawMaterials, refined, key);
        }
        return refined;
    }
    private static HashMap<String, Material> finalizeMaterials(HashMap<String, RawMaterial> materials) {
        // flatten & refine materials
        return refineMaterials(
                flattenMaterials(
                        materials
                )
        );
    }

}
