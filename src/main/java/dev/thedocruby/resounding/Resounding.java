package dev.thedocruby.resounding;

import dev.thedocruby.resounding.openal.ResoundingEFX;
import dev.thedocruby.resounding.effects.AirEffects;
import dev.thedocruby.resounding.performance.RaycastFix;
import dev.thedocruby.resounding.performance.SPHitResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundEngine;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;
import static dev.thedocruby.resounding.config.PrecomputedConfig.speedOfSound;
import static dev.thedocruby.resounding.openal.ResoundingEFX.efxEnabled;
import static java.util.Map.entry;

@SuppressWarnings({"CommentedOutCode"})
// TODO: do more Javadoc
public class Resounding {

	private Resounding() { }

	public static EnvType env = null;
	public static final Logger LOGGER = LogManager.getLogger("Resounding");
	private static final Pattern rainPattern = Pattern.compile(".*rain.*");
	// public static final Pattern stepPattern = Pattern.compile(".*step.*"); // TODO: step sounds
	// private static final Pattern blockPattern = Pattern.compile(".*block..*");TODO: Occlusion
	private static final Pattern uiPattern = Pattern.compile("ui..*");

	public static final Map<BlockSoundGroup, BlockSoundGroup> redirectMap = //<editor-fold desc="Map.ofEntries()">
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
			);//</editor-fold>
	public static final Map<BlockSoundGroup, String> groupToName = //<editor-fold desc="Map.ofEntries()">
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
					entry(BlockSoundGroup.SCULK_SENSOR	, "Sculk Sensor"		),	// Sculk Sensor		(sculk_sensor)
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
					entry(BlockSoundGroup.DRIPSTONE_BLOCK,"Dripstone"		)	// Dripstone		(dripstone_block, pointed_dripstone)
			);/*</editor-fold>*/
	public static final Map<String, BlockSoundGroup> nameToGroup = //<editor-fold desc="Map.ofEntries()">
			Map.ofEntries(
					entry("Coral"			, BlockSoundGroup.CORAL			),	// Coral			(coral_block)
					entry("Gravel, Dirt"		, BlockSoundGroup.GRAVEL		),	// Gravel, Dirt		(gravel, rooted_dirt)
					entry("Amethyst"			, BlockSoundGroup.AMETHYST_BLOCK),	// Amethyst			(amethyst_block, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, amethyst_cluster)
					entry("Sand"				, BlockSoundGroup.SAND			),	// Sand				(sand)
					entry("Candle Wax"		, BlockSoundGroup.CANDLE		),	// Candle Wax		(candle)
					entry("Weeping Vines"	, BlockSoundGroup.WEEPING_VINES	),	// Weeping Vines	(weeping_vines, weeping_vines_low_pitch)
					entry("Soul Sand"		, BlockSoundGroup.SOUL_SAND		),	// Soul Sand		(soul_sand)
					entry("Soul Soil"		, BlockSoundGroup.SOUL_SOIL		),	// Soul Soil		(soul_soil)
					entry("Basalt"			, BlockSoundGroup.BASALT		),	// Basalt			(basalt)
					entry("Netherrack"		, BlockSoundGroup.NETHERRACK	),	// Netherrack		(netherrack, nether_ore, nether_gold_ore)
					entry("Nether Brick"		, BlockSoundGroup.NETHER_BRICKS	),	// Nether Brick		(nether_bricks)
					entry("Honey"			, BlockSoundGroup.HONEY			),	// Honey			(honey_block)
					entry("Bone"				, BlockSoundGroup.BONE			),	// Bone				(bone_block)
					entry("Nether Wart"		, BlockSoundGroup.NETHER_WART	),	// Nether Wart		(nether_wart, wart_block)
					entry("Grass, Foliage"	, BlockSoundGroup.GRASS			),	// Grass, Foliage	(grass, crop, bamboo_sapling, sweet_berry_bush)
					entry("Metal"			, BlockSoundGroup.METAL			),	// Metal			(metal, copper, anvil)
					entry("Aquatic Foliage"	, BlockSoundGroup.WET_GRASS		),	// Aquatic Foliage	(wet_grass, lily_pad)
					entry("Glass, Ice"		, BlockSoundGroup.GLASS			),	// Glass, Ice		(glass)
					entry("Sculk Sensor"		, BlockSoundGroup.SCULK_SENSOR	),	// Sculk Sensor		(sculk_sensor)
					entry("Nether Foliage"	, BlockSoundGroup.ROOTS			),	// Nether Foliage	(roots, nether_sprouts)
					entry("Shroomlight"		, BlockSoundGroup.SHROOMLIGHT	),	// Shroomlight		(shroomlight)
					entry("Chain"			, BlockSoundGroup.CHAIN			),	// Chain			(chain)
					entry("Deepslate"		, BlockSoundGroup.DEEPSLATE		),	// Deepslate		(deepslate)
					entry("Wood"				, BlockSoundGroup.WOOD			),	// Wood				(wood, ladder)
					entry("Deepslate Tiles"	,BlockSoundGroup.DEEPSLATE_TILES),	// Deepslate Tiles	(deepslate_tiles)
					entry("Stone, Blackstone", BlockSoundGroup.STONE			),	// Stone, Blackstone(stone, calcite, gilded_blackstone)
					entry("Slime"			, BlockSoundGroup.SLIME			),	// Slime			(slime_block)
					entry("Polished Deepslate",BlockSoundGroup.POLISHED_DEEPSLATE),// Polished Deepslate(polished_deepslate, deepslate_bricks)
					entry("Snow"				, BlockSoundGroup.SNOW			),	// Snow				(snow)
					entry("Azalea Leaves"	, BlockSoundGroup.AZALEA_LEAVES	),	// Azalea Leaves	(azalea_leaves)
					entry("Bamboo"			, BlockSoundGroup.BAMBOO		),	// Bamboo			(bamboo, scaffolding)
					entry("Mushroom Stems"	, BlockSoundGroup.STEM			),	// Mushroom Stems	(stem)
					entry("Wool"				, BlockSoundGroup.WOOL			),	// Wool				(wool)
					entry("Dry Foliage"		, BlockSoundGroup.VINE			),	// Dry Foliage		(vine, hanging_roots, glow_lichen)
					entry("Azalea Bush"		, BlockSoundGroup.AZALEA		),	// Azalea Bush		(azalea)
					entry("Lush Cave Foliage", BlockSoundGroup.CAVE_VINES	),	// Lush Cave Foliage(cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
					entry("Netherite"		, BlockSoundGroup.NETHERITE		),	// Netherite		(netherite_block, lodestone)
					entry("Ancient Debris"	, BlockSoundGroup.ANCIENT_DEBRIS),	// Ancient Debris	(ancient_debris)
					entry("Nether Fungus Stem",BlockSoundGroup.NETHER_STEM	),	//Nether Fungus Stem(nether_stem)
					entry("Powder Snow"		, BlockSoundGroup.POWDER_SNOW	),	// Powder Snow		(powder_snow)
					entry("Tuff"				, BlockSoundGroup.TUFF			),	// Tuff				(tuff)
					entry("Moss"				, BlockSoundGroup.MOSS_BLOCK	),	// Moss				(moss_block, moss_carpet)
					entry("Nylium"			, BlockSoundGroup.NYLIUM		),	// Nylium			(nylium)
					entry("Nether Mushroom"	, BlockSoundGroup.FUNGUS		),	// Nether Mushroom	(fungus)
					entry("Lanterns"			, BlockSoundGroup.LANTERN		),	// Lanterns			(lantern)
					entry("Dripstone"		,BlockSoundGroup.DRIPSTONE_BLOCK)	// Dripstone		(dripstone_block, pointed_dripstone)
			);//</editor-fold>
	private static Set<Vec3d> rays;

	public static MinecraftClient mc;
	private static int viewDist;
	private static SoundInstance lastSoundInstance;
	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;
	private static SoundListener lastSoundListener;
	private static Vec3d playerPos;
	private static Vec3d listenerPos;
	private static WorldChunk soundChunk;
	private static Vec3d soundPos;
	private static BlockPos soundBlockPos;
	private static boolean auxOnly;
	private static boolean isRain;
	// private static boolean isBlock; TODO: Occlusion
	private static long timeT;
	private static int sourceID;
	// private static boolean doDirEval; // TODO: DirEval

	@Environment(EnvType.CLIENT)
	public static void start() {
		LOGGER.info("Starting Resounding engine...");
		ResoundingEFX.setupEXTEfx();
		LOGGER.info("OpenAL EFX successfully primed for Resounding effects");
		mc = MinecraftClient.getInstance();
		updateRays();
	}

	@Environment(EnvType.CLIENT)
	public static void stop() {
		LOGGER.info("Stopping Resounding engine...");
		ResoundingEFX.cleanUpEXTEfx();
	}

	public static <T> double logBase(T x, T b) {
		return (Math.log((Double) x) / Math.log((Double) b));
	}

	@Environment(EnvType.CLIENT)
	public static void updateRays() {
		final double gRatio = 1.618033988;
		final double epsilon;

		if (pC.nRays >= 600000) { epsilon = 214d; }
		else if (pC.nRays >= 400000) { epsilon = 75d; }
		else if (pC.nRays >= 11000) { epsilon = 27d; }
		else if (pC.nRays >= 890) { epsilon = 10d; }
		else if (pC.nRays >= 177) { epsilon = 3.33d; }
		else if (pC.nRays >= 24) { epsilon = 1.33d; }
		else { epsilon = 0.33d; }

		rays = IntStream.range(0, pC.nRays).parallel().unordered().mapToObj(i -> {
			final double theta = 2d * Math.PI * i / gRatio;
			final double phi = Math.acos(1d - 2d * (i + epsilon) / (pC.nRays - 1d + 2d * epsilon));

			return new Vec3d(
					Math.cos(theta) * Math.sin(phi),
					Math.sin(theta) * Math.sin(phi),
					Math.cos(phi)
			);
		}).collect(Collectors.toSet());
	}

	@Environment(EnvType.CLIENT)
	public static void updateYeetedSoundInfo(SoundInstance sound, SoundListener listener) {
		lastSoundInstance = sound;
		lastSoundCategory = lastSoundInstance.getCategory();
		lastSoundName = lastSoundInstance.getId().getPath();
		lastSoundListener = listener;
	}

	@Environment(EnvType.CLIENT)
	public static void playSound(double posX, double posY, double posZ, int sourceIDIn, boolean auxOnlyIn) {
		if (!efxEnabled || pC.off) return;
		long startTime = 0;
		if (pC.pLog) startTime = System.nanoTime();
		long endTime;
		if (mc.player == null || mc.world == null || uiPattern.matcher(lastSoundName).matches()) {
			return; // Menu sound!
		}
		if (posY <= mc.world.getBottomY() || posY >= mc.world.getTopY() || (pC.recordsDisable && lastSoundCategory == SoundCategory.RECORDS))  {
			try { setEnv(new SoundProfile(sourceID, auxOnly ? 0f : 1f, 1f, new double[pC.resolution], new double[pC.resolution]));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}

		auxOnly = auxOnlyIn;
		sourceID = sourceIDIn;
		Vec3d playerPosOld = mc.player.getPos();
		playerPos = new Vec3d(playerPosOld.x, playerPosOld.y + mc.player.getEyeHeight(mc.player.getPose()), playerPosOld.z);
		listenerPos = lastSoundListener.getPos();
		// isBlock = blockPattern.matcher(lastSoundName).matches(); // && !stepPattern.matcher(lastSoundName).matches(); //  TODO: Occlusion, step sounds
		if (lastSoundCategory == SoundCategory.RECORDS){posX+=0.5;posY+=0.5;posZ+=0.5;/*isBlock = true;*/} // TODO: Occlusion
		isRain = rainPattern.matcher(lastSoundName).matches();
		soundPos = new Vec3d(posX, posY, posZ);
		viewDist = mc.options.getViewDistance();
		double maxDist = Math.min(Math.min(Math.min(mc.options.simulationDistance, viewDist), pC.soundSimulationDistance) * 16, pC.maxDistance / 2);
		soundChunk = mc.world.getChunk(((int)Math.floor(soundPos.x))>>4,((int)Math.floor(soundPos.z))>>4);
		soundBlockPos = new BlockPos(soundPos.x, soundPos.y,soundPos.z);
		timeT = mc.world.getTime();

		if (Math.max(playerPos.distanceTo(soundPos), listenerPos.distanceTo(soundPos)) > maxDist) {
			try { setEnv(new SoundProfile(sourceID, 0d , 1d, new double[pC.resolution], new double[pC.resolution]));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
		if (/*pC.skipRainOcclusionTracing && */isRain) { // TODO: Occlusion
			try { setEnv(new SoundProfile(sourceID, auxOnly ? 0d : 1d, 1d, new double[pC.resolution], new double[pC.resolution]));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
		if (pC.dLog) {
			LOGGER.info("Playing sound!\n      Source ID:      {}\n      Source Pos:     {}\n      Sound category: {}\n      Sound name:     {}", sourceID, new double[] {posX, posY, posZ}, lastSoundCategory, lastSoundName);
		} else {
			LOGGER.debug("Playing sound!\n      Source ID:      {}\n      Source Pos:     {}\n      Sound category: {}\n      Sound name:     {}", sourceID, new double[] {posX, posY, posZ}, lastSoundCategory, lastSoundName);
		}
		try {  ////////  CORE SOUND PIPELINE  ////////

			setEnv(processEnv(evalEnv()));

		} catch (Exception e) { e.printStackTrace(); }
		if (pC.pLog) { endTime = System.nanoTime();
			LOGGER.info("Total calculation time for sound {}: {} milliseconds", lastSoundName, (double)(endTime - startTime)/(double)1000000);
		}
	}

	@Environment(EnvType.CLIENT)
	private static double getBlockReflectivity(final @NotNull BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).reflectivity;
		if (redirectMap.containsKey(soundType)) soundType = redirectMap.get(soundType);
		double r = pC.reflectivityMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultReflectivity : r;
	}
/*
	@Environment(EnvType.CLIENT)
	private static double getBlockOcclusionD(final BlockState blockState) { // TODO: Occlusion
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).absorption;

		double r = pC.absorptionMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultAbsorption : r;
	}
*/

	@Contract("_, _ -> new")
	@Environment(EnvType.CLIENT)
	private static @NotNull Vec3d pseudoReflect(Vec3d dir, @NotNull Vec3i normal) // TODO: I think this breaks with faces not aligned to the grid
	{return new Vec3d(normal.getX() == 0 ? dir.x : -dir.x, normal.getY() == 0 ? dir.y : -dir.y, normal.getZ() == 0 ? dir.z : -dir.z);}

	@Environment(EnvType.CLIENT)
	private static @NotNull RayResult throwEnvRay(@NotNull Vec3d dir) {

		SPHitResult rayHit = RaycastFix.fixedRaycast(
				soundPos,
				soundPos.add(dir.multiply(pC.maxDistance)),
				mc.world,
				soundBlockPos,
				soundChunk
		);

		if (pC.dRays) RaycastRenderer.addSoundBounceRay(soundPos, rayHit.getPos(), Formatting.GREEN.getColorValue());

		if (rayHit.isMissed()) {
			return new RayResult(
					0,
					1,
					0,
					1,
					new double[pC.nRayBounces],
					new double[pC.nRayBounces],
					new double[pC.nRayBounces],
					new double[pC.nRayBounces],
					new double[pC.nRayBounces],
					new double[pC.nRayBounces]
					);
		}

		BlockPos lastHitBlock = rayHit.getBlockPos();
		Vec3d lastHitPos = rayHit.getPos();
		Vec3i lastHitNormal = rayHit.getSide().getVector();
		Vec3d lastRayDir = dir;
		double lastBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());

		int lastBounce = 0;
		double missed = 0;
		double totalDistance = soundPos.distanceTo(rayHit.getPos());
		double totalReflectivity = lastBlockReflectivity;
		double[] shared = new double[pC.nRayBounces];
		double[] energyToPlayer = new double[pC.nRayBounces];
		double[] bounceDistance = new double[pC.nRayBounces];
		double[] totalBounceDistance = new double[pC.nRayBounces];
		double[] bounceReflectivity = new double[pC.nRayBounces];
		double[] totalBounceReflectivity = new double[pC.nRayBounces];

		bounceReflectivity[0] = lastBlockReflectivity;
		totalBounceReflectivity[0] = lastBlockReflectivity;

		bounceDistance[0] = totalDistance;
		totalBounceDistance[0] = totalDistance + lastHitPos.distanceTo(listenerPos);

		// Cast (one) final ray towards the player. If it's
		// unobstructed, then the sound source and the player
		// share airspace.

		SPHitResult finalRayHit = RaycastFix.fixedRaycast(lastHitPos, listenerPos, mc.world, lastHitBlock, rayHit.chunk);

		int color = Formatting.GRAY.getColorValue();
		if (finalRayHit.isMissed()) {
			color = Formatting.WHITE.getColorValue();
			shared[0] = 1;
		}
		if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);

		// Secondary ray bounces
		for (int i = 1; i < pC.nRayBounces; i++) {

			final Vec3d newRayDir = pseudoReflect(lastRayDir, lastHitNormal);
			rayHit = RaycastFix.fixedRaycast(lastHitPos, lastHitPos.add(newRayDir.multiply(pC.maxDistance - totalDistance)), mc.world, lastHitBlock, rayHit.chunk);
			// log("New ray dir: " + newRayDir.xCoord + ", " + newRayDir.yCoord + ", " + newRayDir.zCoord);

			if (rayHit.isMissed()) {
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, rayHit.getPos(), Formatting.DARK_RED.getColorValue());
				missed = Math.pow(totalReflectivity, pC.globalReflRcp);
				break;
			}

			final Vec3d newRayHitPos = rayHit.getPos();
			final double newRayLength = lastHitPos.distanceTo(newRayHitPos);
			totalDistance += newRayLength;
			if (pC.maxDistance - totalDistance < newRayHitPos.distanceTo(listenerPos)) {
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				missed = Math.pow(totalReflectivity, pC.globalReflRcp);
				break;
			}

			final double newBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());
			totalReflectivity *= newBlockReflectivity;
			if (Math.pow(totalReflectivity, pC.globalReflRcp) < pC.minEnergy) {
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_BLUE.getColorValue());
				break;
			}

			if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.BLUE.getColorValue());

			lastBlockReflectivity = newBlockReflectivity;
			lastHitPos = newRayHitPos;
			lastHitNormal = rayHit.getSide().getVector();
			lastRayDir = newRayDir;
			lastHitBlock = rayHit.getBlockPos();

			bounceDistance[i] = newRayLength;
			totalBounceDistance[i] = totalDistance + lastHitPos.distanceTo(listenerPos);
			bounceReflectivity[i] = lastBlockReflectivity;
			totalBounceReflectivity[i] = totalReflectivity;
			lastBounce = i;

			// Cast (one) final ray towards the player. If it's
			// unobstructed, then the sound source and the player
			// share airspace.

			finalRayHit = RaycastFix.fixedRaycast(lastHitPos, listenerPos, mc.world, lastHitBlock, rayHit.chunk);

			color = Formatting.GRAY.getColorValue();
			if (finalRayHit.isMissed()) {
				color = Formatting.WHITE.getColorValue();
				shared[i] = 1;
			}
			if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);
		}
		if (missed == 1) return new RayResult(lastBounce, missed, totalDistance, totalReflectivity, shared, energyToPlayer, bounceDistance, totalBounceDistance, bounceReflectivity, totalBounceReflectivity);
		for(int i = 0; i <= lastBounce; i++) {
			if (shared[i] > 0) continue;
			double accumulator = 1;
			for(int j = i; shared[j] == 0 && j < pC.nRayBounces - 1; j++){
				accumulator *= bounceReflectivity[j+1];
			}
			shared[i] = accumulator;
		}
		return new RayResult(lastBounce, missed, totalDistance, totalReflectivity, shared, energyToPlayer, bounceDistance, totalBounceDistance, bounceReflectivity, totalBounceReflectivity);
	}

	@Environment(EnvType.CLIENT)
	private static Set<RayResult> evalEnv() {

		// Clear the block shape cache every tick, just in case the local block grid has changed
		// TODO: Do this more efficiently.
		//  In 1.18 there should be something I can mix into to clear only in ticks when the block grid changes
		if (RaycastFix.lastUpd != timeT) {
			RaycastFix.shapeCache.clear();
			RaycastFix.lastUpd = timeT;
		}

		RaycastFix.maxY = mc.world.getTopY();
		RaycastFix.minY = mc.world.getBottomY();
		RaycastFix.maxX = (int) (playerPos.getX() + (viewDist * 16));
		RaycastFix.minX = (int) (playerPos.getX() - (viewDist * 16));
		RaycastFix.maxZ = (int) (playerPos.getZ() + (viewDist * 16));
		RaycastFix.minZ = (int) (playerPos.getZ() - (viewDist * 16));

		// TODO: This still needs to be rewritten
		// TODO: fix reflection/absorption calc with an exponential
		//Direct sound occlusion

		/*
		Vec3d normalToPlayer = playerPos.subtract(soundPos).normalize(); TODO: change to `listenerPos`
		if (pC.dLog) logGeneral("Player pos: " + playerPos.x + ", " + playerPos.y + ", " + playerPos.z + "      Sound Pos: " + soundPos.x + ", " + soundPos.y + ", " + soundPos.z + "       To player vector: " + normalToPlayer.x + ", " + normalToPlayer.y + ", " + normalToPlayer.z);
		double occlusionAccumulation = 0;
		//Cast a ray from the source towards the player
		Vec3d rayOrigin = soundPos;
		//System.out.println(rayOrigin.toString());
		BlockPos lastBlockPos = soundBlockPos;
		final boolean _9ray = pC._9Ray && (lastSoundCategory == SoundCategory.BLOCKS || block);
		final int nOccRays = _9ray ? 9 : 1;
		// final List<Map.Entry<Vec3d, Double>> directions = new ArrayList<>(pC.nRays * pC.nRayBounces + nOccRays + 1); // TODO: DirEval
		double occlusionAccMin = Double.MAX_VALUE;
		// TODO: this can probably be spliteratored too
		for (int j = 0; j < nOccRays; j++) {
			if(j > 0){
				final int jj = j - 1;
				rayOrigin = new Vec3d(soundBlockPos.getX() + 0.001 + 0.998 * (jj % 2), soundBlockPos.getY() + 0.001 + 0.998 * ((jj >> 1) % 2), soundBlockPos.getZ() + 0.001 + 0.998 * ((jj >> 2) % 2));
				lastBlockPos = soundBlockPos;
				occlusionAccumulation = 0;

			}
			boolean oAValid = false;
			SPHitResult rayHit = fixedRaycast(rayOrigin, playerPos, mc.world, lastBlockPos, soundChunk);

			for (int i = 0; i < 10; i++) {

				lastBlockPos = rayHit.getBlockPos();
				//If we hit a block

				if (pC.dRays) RaycastRenderer.addOcclusionRay(rayOrigin, rayHit.getPos(), Color.getHSBColor((float) (1F / 3F * (1F - Math.min(1F, occlusionAccumulation / 12F))), 1F, 1F).getRGB());
				if (rayHit.isMissed()) {
					/* if (pC.soundDirectionEvaluation) directions.add(Map.entry(rayOrigin.subtract(playerPos),
							(_9ray?9:1) * Math.pow(soundPos.distanceTo(playerPos), 2.0)* pC.rcpTotRays
									/
							(Math.exp(-occlusionAccumulation * pC.globalBlockAbsorption)* pC.directRaysDirEvalMultiplier)
					)); *//* // TODO: DirEval
					oAValid = true;
					break;
				}

				final Vec3d rayHitPos = rayHit.getPos();
				final BlockState blockHit = rayHit.getBlockState();
				double blockOcclusion = getBlockOcclusionD(blockHit);

				// Regardless to whether we hit from inside or outside

				if (pC.oLog) logOcclusion(blockHit.getBlock().getTranslationKey() + "    " + rayHitPos.x + ", " + rayHitPos.y + ", " + rayHitPos.z);

				rayOrigin = rayHitPos; //new Vec3d(rayHit.getPos().x + normalToPlayer.x * 0.1, rayHit.getPos().y + normalToPlayer.y * 0.1, rayHit.getPos().z + normalToPlayer.z * 0.1);

				rayHit = fixedRaycast(rayOrigin, playerPos, mc.world, lastBlockPos, rayHit.chunk);

				SPHitResult rayBack = fixedRaycast(rayHit.getPos(), rayOrigin, mc.world, rayHit.getBlockPos(), rayHit.chunk);

				if (rayBack.getBlockPos().equals(lastBlockPos)) {
					//Accumulate density
					occlusionAccumulation += blockOcclusion * (rayOrigin.distanceTo(rayBack.getPos()));
					if (occlusionAccumulation > pC.maxDirectOcclusionFromBlocks) break;
				}

				if (pC.oLog) logOcclusion("New trace position: " + rayOrigin.x + ", " + rayOrigin.y + ", " + rayOrigin.z);
			}
			if (oAValid) occlusionAccMin = Math.min(occlusionAccMin, occlusionAccumulation);
		}
		occlusionAccumulation = Math.min(occlusionAccMin, pC.maxDirectOcclusionFromBlocks);
		double directCutoff = Math.exp(-occlusionAccumulation * pC.globalBlockAbsorption);
		double directGain = auxOnly ? 0 : Math.pow(directCutoff, 0.01);

		if (pC.oLog) logOcclusion("direct cutoff: " + directCutoff + "  direct gain:" + directGain);
		*/

		// if (isRain) { return null; }  // TODO: Occlusion

		// Throw rays around and pass data to post
		return rays.stream().parallel().unordered().map(Resounding::throwEnvRay).collect(Collectors.toSet());
	}

	@Contract("_ -> new")
	@Environment(EnvType.CLIENT)
	private static @NotNull SoundProfile processEnv(final @Nullable Set<RayResult> results) {
		// Calculate reverb parameters for this sound
		double directGain = auxOnly ? 0 : 1; // TODO: fix occlusion so i don't have to override this.

		// TODO: DirEval is on hold while I rewrite, will be re-added later
		// Take weighted (on squared distance) average of the directions sound reflection came from
		//doDirEval = pC.soundDirectionEvaluation && (occlusionAccumulation > 0 || pC.notOccludedRedirect); // TODO: DirEval
		// dirEval:  TODO: this block can be converted to another multithreaded iterator
		/* {
			if (directions.isEmpty()) break dirEval;

			if (pC.pLog) log("Evaluating direction from " + sharedAirspace + " entries...");
			Vec3d sum = new Vec3d(0, 0, 0);
			double weight = 0;

			for (Map.Entry<Vec3d, Double> direction : directions) {
				final double w = direction.getValue();
				weight += w;
				sum = sum.add(direction.getKey().normalize().multiply(w));
			}
			sum = sum.multiply(1 / weight);
			setSoundPos(sourceID, sum.normalize().multiply(soundPos.distanceTo(playerPos)).add(playerPos));

			// Vec3d pos = sum.normalize().multiply(soundPos.distanceTo(playerPos)).add(playerPos);
			// mc.world.addParticle(ParticleTypes.END_ROD, false, pos.getX(), pos.getY(), pos.getZ(), 0,0,0);
		}*/
		boolean inWater = false;
		double airAbsorptionHF = AirEffects.getAbsorptionHF();

		assert mc.player != null;
		if (mc.player.isSubmergedInWater()) { inWater = true; }

		if (results == null) {
			return new SoundProfile(sourceID, directGain, directGain * pC.globalAbsorptionBrightness, new double[pC.resolution], new double[pC.resolution]);
		}

		// TODO: Does this perform better in parallel?
		double missedSum = 0.0D;
		double sharedAirspaceSum = 0.0D;
		double bounceCount = 0.0D;
		for (RayResult result : results) {
			bounceCount += result.lastBounce() + 1;
			missedSum += result.missed();
			if (result.missed() == 1.0D) continue;
			for (int j = 0; j <= result.lastBounce(); j++)
				sharedAirspaceSum += result.shared()[j];
		}
		missedSum *= pC.rcpNRays;
		sharedAirspaceSum /= bounceCount;

		// TODO: Does this perform better in parallel?
		// TODO: this is not really done correctly but its the best I can do without dynamic effects
		double[] sendGain = new double[pC.resolution];
		for (RayResult result : results) {
			if (result.missed() == 1.0D) continue;
			for (int j = 0; j <= result.lastBounce(); j++) {
				double energy = result.totalBounceReflectivity()[j] / Math.pow(result.totalBounceDistance()[j], (2.0D * missedSum) + 1 - airAbsorptionHF);
				int t = (int) (Math.pow(MathHelper.clamp(logBase(pC.minEnergy, 1.0D / energy) * -1.0D * result.totalBounceDistance()[j] / speedOfSound, 0.0D, 1.0D), pC.warpFactor) * (pC.resolution - 1));
				sendGain[t] += energy;
			}
		}
		// TODO: tailor shared to each effect slot, like it used to be
		directGain = inWater ? sharedAirspaceSum * pC.underwaterFilter : sharedAirspaceSum; //TODO: Replace this with occlusion calculation, and add an occlusion mode toggle?
		double directCutoff = directGain * pC.globalAbsorptionBrightness;
		double[] sendCutoff = new double[pC.resolution];
		for (int i = 0; i < pC.resolution; i++) {
			sendGain[i] = sendGain[i] * (inWater ? sharedAirspaceSum * pC.underwaterFilter : sharedAirspaceSum)/ bounceCount * pC.globalReverbGain; // TODO: should I use `rcpTotalRays` here? would make reverb quieter
			sendCutoff[i] = sendGain[i] * pC.globalReverbBrightness;
		}

		//logDetailed("HitRatio0: " + hitRatioBounce1 + " HitRatio1: " + hitRatioBounce2 + " HitRatio2: " + hitRatioBounce3 + " HitRatio3: " + hitRatioBounce4);
		//logEnvironment("Bounce reflectivity 0: " + bounceReflectivityRatio[0] + " bounce reflectivity 1: " + bounceReflectivityRatio[1] + " bounce reflectivity 2: " + bounceReflectivityRatio[2] + " bounce reflectivity 3: " + bounceReflectivityRatio[3]);

		if (pC.eLog || pC.dLog) {
			LOGGER.info("Final sound profile:\n      Source Gain:    {}\n      Source Gain HF: {}\n      Reverb Gain:    {}\n      Reverb Gain HF: {}", directGain, directCutoff, sendGain, sendCutoff);
		} else {
			LOGGER.debug("Final sound profile:\n      Source Gain:    {}\n      Source Gain HF: {}\n      Reverb Gain:    {}\n      Reverb Gain HF: {}", directGain, directCutoff, sendGain, sendCutoff);
		}

		return new SoundProfile(sourceID, directGain, directCutoff, sendGain, sendCutoff);
	}

	@Environment(EnvType.CLIENT)
	public static void setEnv(final @NotNull SoundProfile profile) {
		if (profile.sendGain().length != pC.resolution || profile.sendCutoff().length != pC.resolution) {
			throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb resolution!");
		}

		// Set reverb send filter values and set source to send to all reverb fx slots
		for(int i = 0; i < pC.resolution; i++){ ResoundingEFX.setFilter(i, profile.sourceID(), (float) profile.sendGain()[i], (float) profile.sendCutoff()[i]); }

		// Set direct filter values
		ResoundingEFX.setDirectFilter(profile.sourceID(), (float) profile.directGain(), (float) profile.directCutoff());
	}

}
