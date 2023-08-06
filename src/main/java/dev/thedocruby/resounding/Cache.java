package dev.thedocruby.resounding;

import dev.thedocruby.resounding.raycast.Branch;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import dev.thedocruby.resounding.toolbox.MaterialData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.thedocruby.resounding.Engine.LOGGER;
import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;
import static java.util.Map.entry;
import static org.apache.commons.lang3.math.NumberUtils.max;

public class Cache {
    // do these really belong here?
    public final static VoxelShape EMPTY = VoxelShapes.empty();
    public final static VoxelShape CUBE = VoxelShapes.fullCube();

    public final static ExecutorService octreePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
        root.material = getProperties(state);
        boolean valid = true;

        if (scale > 1) {
            boolean any = false;
            for (BlockPos block : blockSequence) {
                final BlockPos position = start.add(block.multiply(scale));
                // use recursion here
                Branch leaf = growOctree(chunk, new Branch(position,scale, (MaterialData) null));
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
                @NotNull MaterialData next = getProperties(chunk.getBlockState(position));
                // break if next block isn't similar enough
                if (!root.material.equals(next)) {
                    // root.material = null;
                    valid = false;
                    break;
                }
            }
        }
        root.set(valid ? root.material : (MaterialData) null);
        //*/
        return root;
    }

    public static long counter = 0;

    public final static Map<Block, Pair<Double,Double>> blockMap = new HashMap<>() {{
        put(null        , pair(0.00, 0.98));
        put(Blocks.STONE, pair(0.90, 0.40));
        put(Blocks.AIR  , pair(0.00, 0.98));
    }};
    // Map.ofEntries() {
    @Environment(EnvType.CLIENT) // TODO: Make sure this is used everywhere, add example text
    public static final Map<String, MaterialData> materialDefaults =
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
            ); // }
    // Map.ofEntries() {
    public static final Map<BlockSoundGroup, BlockSoundGroup> redirectMap =
            Map.ofEntries(  // first becomes second
                    entry(BlockSoundGroup.MOSS_CARPET, BlockSoundGroup.MOSS_BLOCK),
                    entry(BlockSoundGroup.AMETHYST_CLUSTER, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.SMALL_AMETHYST_BUD, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.MEDIUM_AMETHYST_BUD, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.LARGE_AMETHYST_BUD, BlockSoundGroup.AMETHYST_BLOCK),
                    entry(BlockSoundGroup.POINTED_DRIPSTONE, BlockSoundGroup.DRIPSTONE_BLOCK),
                    entry(BlockSoundGroup.FLOWERING_AZALEA, BlockSoundGroup.AZALEA),
                    entry(BlockSoundGroup.DEEPSLATE_BRICKS, BlockSoundGroup.POLISHED_DEEPSLATE),
                    entry(BlockSoundGroup.COPPER, BlockSoundGroup.METAL),
                    entry(BlockSoundGroup.ANVIL, BlockSoundGroup.METAL),
                    entry(BlockSoundGroup.NETHER_SPROUTS, BlockSoundGroup.ROOTS),
                    entry(BlockSoundGroup.WEEPING_VINES_LOW_PITCH, BlockSoundGroup.WEEPING_VINES),
                    entry(BlockSoundGroup.LILY_PAD, BlockSoundGroup.WET_GRASS),
                    entry(BlockSoundGroup.NETHER_GOLD_ORE, BlockSoundGroup.NETHERRACK),
                    entry(BlockSoundGroup.NETHER_ORE, BlockSoundGroup.NETHERRACK),
                    entry(BlockSoundGroup.CALCITE, BlockSoundGroup.STONE),
                    entry(BlockSoundGroup.GILDED_BLACKSTONE, BlockSoundGroup.STONE),
                    entry(BlockSoundGroup.SMALL_DRIPLEAF, BlockSoundGroup.CAVE_VINES),
                    entry(BlockSoundGroup.BIG_DRIPLEAF, BlockSoundGroup.CAVE_VINES),
                    entry(BlockSoundGroup.SPORE_BLOSSOM, BlockSoundGroup.CAVE_VINES),
                    entry(BlockSoundGroup.GLOW_LICHEN, BlockSoundGroup.VINE),
                    entry(BlockSoundGroup.HANGING_ROOTS, BlockSoundGroup.VINE),
                    entry(BlockSoundGroup.ROOTED_DIRT, BlockSoundGroup.GRAVEL),
                    entry(BlockSoundGroup.WART_BLOCK, BlockSoundGroup.NETHER_WART),
                    entry(BlockSoundGroup.CROP, BlockSoundGroup.GRASS),
                    entry(BlockSoundGroup.BAMBOO_SAPLING, BlockSoundGroup.GRASS),
                    entry(BlockSoundGroup.SWEET_BERRY_BUSH, BlockSoundGroup.GRASS),
                    entry(BlockSoundGroup.SCAFFOLDING, BlockSoundGroup.BAMBOO),
                    entry(BlockSoundGroup.LODESTONE, BlockSoundGroup.NETHERITE),
                    entry(BlockSoundGroup.LADDER, BlockSoundGroup.WOOD)
            ); // }
    // Map.ofEntries() {
    public static final Map<BlockSoundGroup, String> groupToName =
            Map.ofEntries(
                    entry(BlockSoundGroup.CORAL			, "Coral"			),	// Coral			(coral_block)
                    entry(BlockSoundGroup.GRAVEL		, "Gravel, Dirt"		),	// Gravel, Dirt		(gravel, rooted_dirt)
                    entry(BlockSoundGroup.AMETHYST_BLOCK, "Amethyst"			),	// Amethyst			(amethyst_block, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, amethyst_cluster)
                    entry(BlockSoundGroup.SAND			, "Sand"				),	// Sand				(sand)
                    entry(BlockSoundGroup.CANDLE		, "Candle Wax"		),	// Candle Wax		(candle)
                    entry(BlockSoundGroup.WEEPING_VINES	, "Weeping Vines"	),	// Weeping Vines	(weeping_vines, weeping_vines_low_pitch)
                    entry(BlockSoundGroup.SOUL_SAND		, "Soul Sand"		),	// Soul Sand		(soul_sand)
                    entry(BlockSoundGroup.SOUL_SOIL		, "Soul Soil"		),	// Soul Soil		(soul_soil)
                    entry(BlockSoundGroup.BASALT		, "Basalt"			),	// Basalt			(basalt)
                    entry(BlockSoundGroup.NETHERRACK	, "Netherrack"		),	// Netherrack		(netherrack, nether_ore, nether_gold_ore)
                    entry(BlockSoundGroup.NETHER_BRICKS	, "Nether Brick"		),	// Nether Brick		(nether_bricks)
                    entry(BlockSoundGroup.HONEY			, "Honey"			),	// Honey			(honey_block)
                    entry(BlockSoundGroup.BONE			, "Bone"				),	// Bone				(bone_block)
                    entry(BlockSoundGroup.NETHER_WART	, "Nether Wart"		),	// Nether Wart		(nether_wart, wart_block)
                    entry(BlockSoundGroup.GRASS			, "Grass, Foliage"	),	// Grass, Foliage	(grass, crop, bamboo_sapling, sweet_berry_bush)
                    entry(BlockSoundGroup.METAL			, "Metal"			),	// Metal			(metal, copper, anvil)
                    entry(BlockSoundGroup.WET_GRASS		, "Aquatic Foliage"	),	// Aquatic Foliage	(wet_grass, lily_pad)
                    entry(BlockSoundGroup.GLASS			, "Glass, Ice"		),	// Glass, Ice		(glass)
                    entry(BlockSoundGroup.ROOTS			, "Nether Foliage"	),	// Nether Foliage	(roots, nether_sprouts)
                    entry(BlockSoundGroup.SHROOMLIGHT	, "Shroomlight"		),	// Shroomlight		(shroomlight)
                    entry(BlockSoundGroup.CHAIN			, "Chain"			),	// Chain			(chain)
                    entry(BlockSoundGroup.DEEPSLATE		, "Deepslate"		),	// Deepslate		(deepslate)
                    entry(BlockSoundGroup.WOOD			, "Wood"				),	// Wood				(wood, ladder)
                    entry(BlockSoundGroup.DEEPSLATE_TILES,"Deepslate Tiles"	),	// Deepslate Tiles	(deepslate_tiles)
                    entry(BlockSoundGroup.STONE			, "Stone, Blackstone"),	// Stone, Blackstone(stone, calcite, gilded_blackstone)
                    entry(BlockSoundGroup.SLIME			, "Slime"			),	// Slime			(slime_block)
                    entry(BlockSoundGroup.POLISHED_DEEPSLATE,"Polished Deepslate"),// Polished Deepslate(polished_deepslate, deepslate_bricks)
                    entry(BlockSoundGroup.SNOW			, "Snow"				),	// Snow				(snow)
                    entry(BlockSoundGroup.AZALEA_LEAVES	, "Azalea Leaves"	),	// Azalea Leaves	(azalea_leaves)
                    entry(BlockSoundGroup.BAMBOO		, "Bamboo"			),	// Bamboo			(bamboo, scaffolding)
                    entry(BlockSoundGroup.STEM			, "Mushroom Stems"	),	// Mushroom Stems	(stem)
                    entry(BlockSoundGroup.WOOL			, "Wool"				),	// Wool				(wool)
                    entry(BlockSoundGroup.VINE			, "Dry Foliage"		),	// Dry Foliage		(vine, hanging_roots, glow_lichen)
                    entry(BlockSoundGroup.AZALEA		, "Azalea Bush"		),	// Azalea Bush		(azalea)
                    entry(BlockSoundGroup.CAVE_VINES	, "Lush Cave Foliage"),	// Lush Cave Foliage(cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
                    entry(BlockSoundGroup.NETHERITE		, "Netherite"		),	// Netherite		(netherite_block, lodestone)
                    entry(BlockSoundGroup.ANCIENT_DEBRIS, "Ancient Debris"	),	// Ancient Debris	(ancient_debris)
                    entry(BlockSoundGroup.NETHER_STEM	,"Nether Fungus Stem"),	//Nether Fungus Stem(nether_stem)
                    entry(BlockSoundGroup.POWDER_SNOW	, "Powder Snow"		),	// Powder Snow		(powder_snow)
                    entry(BlockSoundGroup.TUFF			, "Tuff"				),	// Tuff				(tuff)
                    entry(BlockSoundGroup.MOSS_BLOCK	, "Moss"				),	// Moss				(moss_block, moss_carpet)
                    entry(BlockSoundGroup.NYLIUM		, "Nylium"			),	// Nylium			(nylium)
                    entry(BlockSoundGroup.FUNGUS		, "Nether Mushroom"	),	// Nether Mushroom	(fungus)
                    entry(BlockSoundGroup.LANTERN		, "Lanterns"			),	// Lanterns			(lantern)
                    entry(BlockSoundGroup.DRIPSTONE_BLOCK,"Dripstone"		),	// Dripstone		(dripstone_block, pointed_dripstone)
                    entry(BlockSoundGroup.SCULK_SENSOR	, "Sculk Sensor"		)	// Sculk Sensor		(sculk_sensor)
            ); // }
    public static final Map<String, BlockSoundGroup> nameToGroup = groupToName.keySet().stream().collect(Collectors.toMap(groupToName::get, k -> k));
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

    public static boolean generate(Consumer<String> logger) {
        // FabricTagProvider.BlockTagProvider x = null;
        logger.accept(Registry.BLOCK.getKey(Blocks.AIR).toString());
        // Registry.BLOCK.forEach(
        Registry.REGISTRIES.streamTags().forEach(
                (tagKey) -> {
                    logger.accept(
                        tagKey.registry().getValue().getPath()
                    );
                }
        );
        return false;
    }

    public static @NotNull MaterialData getProperties(@Nullable BlockState state) {
        MaterialData material;
        @Nullable Pair<Double,Double> attributes;// = blockMap.get(branch.state.getBlock());
        //*/
        /*
        final double reflec =   pC.reflMap.get(branch.state.getBlock().getTranslationKey());
        final double perm   = 1-pC.absMap.get(branch.state.getBlock().getTranslationKey());
        //*/
        /* TODO remove
        final double reflec = Math.random();
        final double perm = Math.random();
        return new MaterialData("random", reflec, perm);
        //*/
        // attributes = new Pair<>(reflec,perm);
        // in the event of a modded block
        /*if (attributes == null) {
            final BlockPos blockPos = new BlockPos(position);
            final double hardness = (double) Math.min(5,world.getBlockState(blockPos).getHardness(world, blockPos)) / 5 / 4;
            attributes = new Pair<>(hardness * 3,1-hardness);
        }*/
        // state.getFluidState().getFluid(); // Fluids.WATER/EMPTY/etc
        //* TODO remove
        if (state == null || state.getBlock() == Blocks.STONE)
            material = new MaterialData("stone",1.0,0.0);
        else
            material = new MaterialData("air",  0.0,transmission);
        //*/
        // for hashmap usage
        return material != null ? material : new MaterialData("stone", 1.0, 0.0);
    }

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

    @Environment(EnvType.CLIENT)
    private static double getBlockReflectivity(final @NotNull BlockState blockState) {
        return pC.reflMap.getOrDefault(
                blockState.getBlock().getTranslationKey(),
                pC.reflMap.getOrDefault(
                        groupToName.getOrDefault(
                                redirectMap.getOrDefault(
                                        blockState.getSoundGroup(),
                                        blockState.getSoundGroup()),
                                "DEFAULT"),
                        pC.defaultRefl)
        );
    }

    @Environment(EnvType.CLIENT)
    public static void generate() {
        // prepopulated & loaded from disk
        HashMap<String, RawMaterial> config = new HashMap<>();

        // read from game registry
        HashMap<String, LinkedList<String>> tags = new HashMap<>();
        HashMap<String, LinkedList<String>> blocks = new HashMap<>();
        Registry.BLOCK.forEach((Block block) -> {
            String name = block.getTranslationKey();
            block.getDefaultState().streamTags()
                .forEach((tag) -> {
                    String id = tag.id().toString();
                    update(tags, id, name);
                    update(blocks, name, id);
                });
        });
        LOGGER.info(tags.size() + "");
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
        // NOTE: don't access any non-static variables other than (getter, raw) inside the calculation phase
        //       This will change how the compiler sees the lambda, (see: lambda closures)
        //       and will not reuse it (resulting in a large performance hit)
        memoize(in, out, key, (getter, raw) -> {
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
                    totalWeight += c - w; // adjust weight for next item
                    weight[i] = c;

                    // apply composition
                    updWeight(granularity, i, component.granularity(), c);
                    updWeight(melt, i, component.melt(), c);
                    updWeight(boil, i, component.boil(), c);
                    updWeight(density, i, component.density(), c);
                    updWeight(swave, i, component.swave(), c);
                    updWeight(lwave, i, component.lwave(), c);
                }

                // apply weights to values and update value
                raw = new RawMaterial(
                        totalWeight,
                        raw.solvent(),
                        null, //raw.solute(),      // for posterity/debug, not needed in runtime
                        null, //raw.composition(), // for posterity/debug, not needed in runtime
                        unWeight(totalWeight, granularity, raw.granularity()),
                        unWeight(totalWeight, melt, raw.melt()),
                        unWeight(totalWeight, boil, raw.boil()),
                        unWeight(totalWeight, density, raw.density()),
                        unWeight(totalWeight, swave, raw.swave()),
                        unWeight(totalWeight, lwave, raw.lwave())
                );
            }
            return raw;
        });
    }

    // specialized memoization for tag/material cache functionality
    private static <T> T memoize(HashMap<String, T> in, HashMap<String, T> out, String key, BiFunction<Function<String,T>,T,T> calculate) {
        // return cached values
        if (out.containsKey(key))
            return out.get(key);
        // mark as in-progress
        // getter == null; should be scanned for in calculate to prevent cyclic references
        out.put(key, null);
        T value = calculate.apply((String x) -> memoize(in, out, x, calculate), in.remove(key));
        out.put(key, value);
        if (value == null)
            LOGGER.error("{} is invalid or cyclical", key);
        return value;
    }

    // ba-d-ad jokes will never get old!
    // update + weight
    private static void updWeight(Double[] list, int index, @Nullable Double value, double coefficient) {
        if (list != null)
            list[index] = value == null ? 0 : value * coefficient;
    }

    // average values using weights
    private static Double unWeight(Double weight, Double[] values, Double fallback) {
        // density == 0 is for single-value modifications (shapes, lone values)
        return smartSum(values, fallback) / (weight == 0 ? values.length : weight);
    }

    // sum with specialized fallback
    private static Double smartSum(Double[] values, Double fallback) {
        if (values == null) {
            return fallback;
        }
        double output = 0;
        for (Double value : values)
            output += value == null ? 0 : value;
        return output;
    }

    // update a value in a particular type of hashmap (see signature)
    private static void update(HashMap<String, LinkedList<String>> map, String key, String value) {
        final LinkedList<String> list = map.getOrDefault(key, new LinkedList<>());
        list.add(value);
        map.put(key, list);
    }
}
