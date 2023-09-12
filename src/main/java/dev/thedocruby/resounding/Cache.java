package dev.thedocruby.resounding;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import dev.thedocruby.resounding.raycast.Branch;
import dev.thedocruby.resounding.toolbox.ChunkChain;
import dev.thedocruby.resounding.toolbox.MaterialData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.sound.BlockSoundGroup;
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
import java.util.stream.Collectors;

import static dev.thedocruby.resounding.Engine.mc;
import static dev.thedocruby.resounding.Utils.LOGGER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;

public class Cache {
    // do these really belong here?
    public final static VoxelShape EMPTY = VoxelShapes.empty();
    public final static VoxelShape CUBE = VoxelShapes.fullCube();

    public final static ExecutorService octreePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static HashMap<String, Material> materials = new HashMap<>();

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

    @Environment(EnvType.CLIENT) // TODO: is this method needed on server side?
    public static @NotNull Material material(@Nullable BlockState state) {
        // TODO: separate map for fluids? (performance consideration)
        // state.getFluidState().getFluid(); // Fluids.WATER/EMPTY/etc
        // TODO: cascading effect material controllers
        return materials.getOrDefault(state.getBlock().getName(), null);
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

    public static void load(HashMap<String, RawMaterial> raw) {
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
        // TODO create custom tagging system w/ regex here
        // TODO attach materials to tagging system
    }

    // Recalls baked materials from config
    // gets the config dir, opens the save file, parses it
    @Environment(EnvType.CLIENT)
    public static boolean recall() {
        try {
            String name = FabricLoader.getInstance().getConfigDir().toAbsolutePath().resolve("resounding.cache").toString();
            FileReader reader = new FileReader(name);
            // parse JSON input
            LinkedTreeMap<String, LinkedTreeMap> baked = new Gson().fromJson(reader, Utils.token(Cache.materials));
            HashMap<String, Material> config = new HashMap<>();
            baked.forEach((String key, LinkedTreeMap value) ->
                config.put(key, new Material(
                    // defaults to air's values
                    (double) value.getOrDefault("impedance", 350),
                    (double) value.getOrDefault("permeation", 1),
                    (double) value.getOrDefault("state", 1)
                ))
            );
            // when nothing's present, fix it!
            if (config.isEmpty()) return false;
            // keep atomic, apply last
            Cache.materials = config;
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed recalling baked materials from config", e);
            return false;
        }
    }

    // saves baked materials for recalling later
    // serializes materials, gets the config dir, writes to the file
    @Environment(EnvType.CLIENT)
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
        boolean result = recall();
        if (result) { // TODO extract into GUI handler
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
        // TODO: split this section into assign()?
        load(flat);
        Cache.materials = refineMaterials(flat);

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
