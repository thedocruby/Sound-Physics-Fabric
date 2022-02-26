package dev.thedocruby.resounding;

import dev.thedocruby.resounding.effects.AirEffects;
import dev.thedocruby.resounding.openal.ResoundingEFX;
import dev.thedocruby.resounding.raycast.RaycastFix;
import dev.thedocruby.resounding.raycast.RaycastRenderer;
import dev.thedocruby.resounding.raycast.SPHitResult;
import dev.thedocruby.resounding.toolbox.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.thedocruby.resounding.config.PrecomputedConfig.*;
import static java.util.Map.entry;

@SuppressWarnings({"CommentedOutCode"})
// TODO: do more Javadoc
public class ResoundingEngine {

	private ResoundingEngine() { }

	public static EnvType env = null;
	public static MinecraftClient mc;
	public static boolean isOff = true;
	public static final Logger LOGGER = LogManager.getLogger("Resounding");

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
			);/*</editor-fold>*/
	public static final Map<String, BlockSoundGroup> nameToGroup = groupToName.keySet().stream().collect(Collectors.toMap(groupToName::get, k -> k));

	public static final Pattern rainPattern = Pattern.compile(".*rain.*");
	public static final Pattern stepPattern = Pattern.compile(".*step.*"); // TODO: step sounds
	public static final Pattern stepPatternPF = Pattern.compile(".*pf_presence.*"); // TODO: step sounds
	// public static final Pattern blockPattern = Pattern.compile(".*block..*");TODO: Occlusion
	public static final Pattern uiPattern = Pattern.compile("ui..*");

	private static Set<Vec3d> rays;
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
	//private static boolean isBlock; // TODO: Occlusion
	//private static boolean doNineRay; // TODO: Occlusion
	private static long timeT;
	private static int sourceID;
	//private static boolean doDirEval; // TODO: DirEval
	
	public static <T> double logBase(T x, T b) {
		return (Math.log((Double) x) / Math.log((Double) b));
	}

	@Contract("_, _ -> new")
	private static @NotNull Vec3d pseudoReflect(Vec3d dir, @NotNull Vec3i normal) // TODO: I think this breaks with faces not aligned to the grid
	{return new Vec3d(normal.getX() == 0 ? dir.x : -dir.x, normal.getY() == 0 ? dir.y : -dir.y, normal.getZ() == 0 ? dir.z : -dir.z);}

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
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");
		lastSoundInstance = sound;
		lastSoundCategory = lastSoundInstance.getCategory();
		lastSoundName = lastSoundInstance.getId().getPath();
		lastSoundListener = listener;
	}

	@Environment(EnvType.CLIENT)
	public static void playSound(double posX, double posY, double posZ, int sourceIDIn, boolean auxOnlyIn) { // The heart of the Resounding audio pipeline
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");
		long startTime = 0;
		if (pC.pLog) startTime = System.nanoTime();
		long endTime;
		auxOnly = auxOnlyIn;
		sourceID = sourceIDIn;

		if (mc.player == null || mc.world == null || uiPattern.matcher(lastSoundName).matches()) {
			if (pC.dLog) {
				LOGGER.info("Skipped playing sound \"{}\": Not a world sound.", lastSoundName);
			} else {
				LOGGER.debug("Skipped playing sound \"{}\": Not a world sound.", lastSoundName);
			}
			return;
		}

		// isBlock = blockPattern.matcher(lastSoundName).matches(); // && !stepPattern.matcher(lastSoundName).matches(); //  TODO: Occlusion, step sounds
		if (lastSoundCategory == SoundCategory.RECORDS){posX+=0.5;posY+=0.5;posZ+=0.5;/*isBlock = true;*/} // TODO: Occlusion
		if (stepPattern.matcher(lastSoundName).matches() || stepPatternPF.matcher(lastSoundName).matches()) {posY+=0.2;} // TODO: step sounds
		//doNineRay = pC.nineRay && (lastSoundCategory == SoundCategory.BLOCKS || isBlock); // TODO: Occlusion
		Vec3d playerPosOld = mc.player.getPos();
		playerPos = new Vec3d(playerPosOld.x, playerPosOld.y + mc.player.getEyeHeight(mc.player.getPose()), playerPosOld.z);
		listenerPos = lastSoundListener.getPos();
		final int bottom = mc.world.getBottomY();
		final int top = mc.world.getTopY();
		isRain = rainPattern.matcher(lastSoundName).matches();
		soundPos = new Vec3d(posX, posY, posZ);
		viewDist = mc.options.getViewDistance();
		double maxDist = Math.min(Math.min(Math.min(mc.options.simulationDistance, viewDist), pC.soundSimulationDistance) * 16, pC.maxTraceDist / 2);
		soundChunk = mc.world.getChunk(((int)Math.floor(soundPos.x))>>4,((int)Math.floor(soundPos.z))>>4);
		soundBlockPos = new BlockPos(soundPos.x, soundPos.y,soundPos.z);
		timeT = mc.world.getTime();

		if ( /* <editor-fold desc="Outside Block Grid"> */
				posY          <= bottom || posY          >= top ||
						playerPos.y   <= bottom || playerPos.y   >= top ||
						listenerPos.y <= bottom || listenerPos.y >= top
			/* </editor-fold> */ ) {
			if (pC.dLog) {
				LOGGER.info("Skipped playing sound \"{}\": Cannot trace sounds outside the block grid.", lastSoundName);
			} else {
				LOGGER.info("Skipped playing sound \"{}\": Cannot trace sounds outside the block grid.", lastSoundName);
			}
			try { setEnv(processEnv(new EnvData(Collections.emptySet(), Collections.emptySet())));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
		if (Math.max(playerPos.distanceTo(soundPos), listenerPos.distanceTo(soundPos)) > maxDist) {
			if (pC.dLog) {
				LOGGER.info("Skipped environment sampling for sound \"{}\": Sound is outside the maximum traceable distance with the current settings.", lastSoundName);
			} else {
				LOGGER.debug("Skipped environment sampling for sound \"{}\": Sound is outside the maximum traceable distance with the current settings.", lastSoundName);
			}
			try { setEnv(processEnv(new EnvData(Collections.emptySet(), Collections.emptySet())));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
		if (pC.recordsDisable && lastSoundCategory == SoundCategory.RECORDS){
			if (pC.dLog) {
				LOGGER.info("Skipped environment sampling for sound \"{}\": Disabled sound.", lastSoundName);
			} else {
				LOGGER.debug("Skipped environment sampling for sound \"{}\": Disabled sound.", lastSoundName);
			}
			try { setEnv(processEnv(new EnvData(Collections.emptySet(), Collections.emptySet())));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
		if (/*pC.skipRainOcclusionTracing && */isRain) { // TODO: Occlusion
			if (pC.dLog) {
				LOGGER.info("Skipped environment sampling for sound \"{}\": Rain sound", lastSoundName);
			} else {
				LOGGER.debug("Skipped environment sampling for sound \"{}\": Rain sound", lastSoundName);
			}
			try { setEnv(processEnv(new EnvData(Collections.emptySet(), Collections.emptySet())));
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
		if (pC.dLog) {
			LOGGER.info("Playing sound!\n      Player Pos:    {}\n      Listener Pos:    {}\n      Source ID:    {}\n      Source Pos:    {}\n      Sound category:    {}\n      Sound name:    {}", playerPos, listenerPos, sourceID, soundPos, lastSoundCategory, lastSoundName);
		} else {
			LOGGER.debug("Playing sound!\n      Player Pos:    {}\n      Listener Pos:    {}\n      Source ID:    {}\n      Source Pos:    {}\n      Sound category:    {}\n      Sound name:    {}", playerPos, listenerPos, sourceID, soundPos, lastSoundCategory, lastSoundName);
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
		return pC.reflMap.getOrDefault(blockState.getBlock().getTranslationKey(), pC.reflMap.getOrDefault(groupToName.getOrDefault(redirectMap.getOrDefault(blockState.getSoundGroup(), blockState.getSoundGroup()), "DEFAULT"), pC.defaultRefl));
	}

    /*  // TODO: Occlusion
	@Environment(EnvType.CLIENT)
	private static double getBlockOcclusionD(final BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).absorption;

		double r = pC.absorptionMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultAbsorption : r;
	}
    */

	@Environment(EnvType.CLIENT)
	private static @NotNull ReflectedRayData throwReflRay(@NotNull Vec3d dir) {
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");

		SPHitResult rayHit = RaycastFix.fixedRaycast(
				soundPos,
				soundPos.add(dir.multiply(pC.maxTraceDist)),
				mc.world,
				soundBlockPos,
				soundChunk
		);

		if (pC.dRays) RaycastRenderer.addSoundBounceRay(soundPos, rayHit.getPos(), Formatting.GREEN.getColorValue());

		if (rayHit.isMissed()) {
			return new ReflectedRayData(
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

		int size = 0;
		double missed = 0;
		double totalDistance = soundPos.distanceTo(lastHitPos);
		double totalReflectivity = lastBlockReflectivity;
		double[] shared = new double[pC.nRayBounces];
		double[] distToPlayer = new double[pC.nRayBounces];
		double[] bounceDistance = new double[pC.nRayBounces];
		double[] totalBounceDistance = new double[pC.nRayBounces];
		double[] bounceReflectivity = new double[pC.nRayBounces];
		double[] totalBounceReflectivity = new double[pC.nRayBounces];

		bounceReflectivity[0] = lastBlockReflectivity;
		totalBounceReflectivity[0] = lastBlockReflectivity;

		bounceDistance[0] = totalDistance;
		totalBounceDistance[0] = totalDistance;
		distToPlayer[0] = lastHitPos.distanceTo(listenerPos);

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
			rayHit = RaycastFix.fixedRaycast(lastHitPos, lastHitPos.add(newRayDir.multiply(pC.maxTraceDist - totalDistance)), mc.world, lastHitBlock, rayHit.chunk);

			if (rayHit.isMissed()) {
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, rayHit.getPos(), Formatting.DARK_RED.getColorValue());
				missed = Math.pow(totalReflectivity, pC.globalReflRcp);
				break;
			}

			final Vec3d newRayHitPos = rayHit.getPos();
			bounceDistance[i] = lastHitPos.distanceTo(newRayHitPos);
			totalDistance += bounceDistance[i];
			if (pC.maxTraceDist - totalDistance < newRayHitPos.distanceTo(listenerPos)) {
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				missed = Math.pow(totalReflectivity, pC.globalReflRcp);
				break;
			}

			final double newBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());
			totalReflectivity *= newBlockReflectivity;
			if (totalReflectivity < minEnergy){
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				break;
			}

			if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.BLUE.getColorValue());

			lastBlockReflectivity = newBlockReflectivity;
			lastHitPos = newRayHitPos;
			lastHitNormal = rayHit.getSide().getVector();
			lastRayDir = newRayDir;
			lastHitBlock = rayHit.getBlockPos();

			size = i;
			totalBounceDistance[i] = totalDistance;
			distToPlayer[i] = lastHitPos.distanceTo(listenerPos);
			bounceReflectivity[i] = lastBlockReflectivity;
			totalBounceReflectivity[i] = totalReflectivity;

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
		return new ReflectedRayData(++size, missed, totalDistance, totalReflectivity, shared, distToPlayer, bounceDistance, totalBounceDistance, bounceReflectivity, totalBounceReflectivity);
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull Set<OccludedRayData> throwOcclRay(@NotNull Vec3d sourcePos, @NotNull Vec3d sinkPos) { //Direct sound occlusion
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");

		// TODO: This still needs to be rewritten
		// TODO: fix reflection/absorption calc with an exponential

		/* Old code
		Vec3d normalToPlayer = playerPos.subtract(soundPos).normalize(); TODO: change to `listenerPos`
		double occlusionAccumulation = 0;
		//Cast a ray from the source towards the player
		Vec3d rayOrigin = soundPos;
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
					if (pC.soundDirectionEvaluation) directions.add(Map.entry(rayOrigin.subtract(playerPos), // TODO: DirEval
							(_9ray?9:1) * Math.pow(soundPos.distanceTo(playerPos), 2.0)* pC.rcpTotRays
									/
							(Math.exp(-occlusionAccumulation * pC.globalBlockAbsorption)* pC.directRaysDirEvalMultiplier)
					));
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
		if (isRain) { return null; }
		*/

		return Collections.emptySet();
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull EnvData evalEnv() {
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");

		// Clear the block shape cache every tick, just in case the local block grid has changed
		// TODO: Do this more efficiently.
		//  In 1.18 there should be something I can mix into to clear only in ticks when the block grid changes
		//  I think i remember a technical chang relating to this;
		//  It may be faster to skim through the snapshot changelog instead of digging through the code.
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

		// Throw rays around
		if (pC.dLog || pC.eLog) {
			if (isRain) {
				LOGGER.info("Skipped reverb ray tracing for rain sound.");
			} else {
				LOGGER.info("Sampling environment with {} seed rays...", pC.nRays);
			}
		} else {
			if (isRain) {
				LOGGER.debug("Skipped reverb ray tracing for rain sound.");
			} else {
				LOGGER.debug("Sampling environment with {} seed rays...", pC.nRays);
			}
		}
		Set<ReflectedRayData> reflRays = isRain ? Collections.emptySet() :
				rays.stream().parallel().unordered().map(ResoundingEngine::throwReflRay).collect(Collectors.toSet());
		if(!isRain) {
			if (pC.eLog) {
				int rayCount = 0;
				for (ReflectedRayData reflRay : reflRays){
					rayCount += reflRay.size() * 2 + 1;
				}
				LOGGER.info("Environment sampled! Total number of rays casted: {}", rayCount); // TODO: This is not precise
			} else if (pC.dLog) {
				LOGGER.info("Environment sampled!");
			} else {
				LOGGER.debug("Environment sampled!");
			}
		}

		// TODO: Occlusion. Also, add occlusion profiles.
		// Step rays from sound to listener
		Set<OccludedRayData> occlRays = throwOcclRay(soundPos, listenerPos);

		//Pass data to post
		EnvData data = new EnvData(reflRays, occlRays);
		if (pC.eLog) LOGGER.info("Raw Environment data:\n{}", data);
		return data;
	}

	@Contract("_ -> new")
	@Environment(EnvType.CLIENT)
	private static @NotNull SoundProfile processEnv(final EnvData data) {
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");
		// TODO: DirEval is on hold while I rewrite, will be re-added later
		// Take weighted (on squared distance) average of the directions sound reflection came from
		//doDirEval = pC.soundDirectionEvaluation && (occlusionAccumulation > 0 || pC.notOccludedRedirect); // TODO: DirEval
		// dirEval:  TODO: this block can be converted to another multithreaded iterator
		/* {
			if (directions.isEmpty()) break dirEval;

			if (pC.pLog) log("Evaluating direction from " + sendSharedSum + " entries...");
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

		boolean inWater = mc.player != null && mc.player.isSubmergedInWater();
		final double airAbsorptionHF = AirEffects.getAbsorptionHF();
		double directGain = (auxOnly ? 0 : inWater ? pC.waterFilt : 1) * Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos)) ;

		if (data.reflRays().isEmpty()) {
			return new SoundProfile(sourceID, directGain, Math.pow(directGain, pC.globalAbsHFRcp), new double[pC.resolution + 1], new double[pC.resolution + 1]);
		}

		double bounceCount = 0.0D;
		double missedSum = 0.0D;
		for (ReflectedRayData reflRay : data.reflRays()) {
			bounceCount += reflRay.size();
			missedSum += reflRay.missed();
		}
		missedSum *= pC.rcpNRays;

		// TODO: Does this perform better in parallel?
		double sharedSum = 0.0D;
		final double[] sendGain = new double[pC.resolution + 1];
		for (ReflectedRayData reflRay : data.reflRays()) {
			if (reflRay.missed() == 1.0D) continue;

			final int size = reflRay.size();
			final double[] smoothSharedEnergy = new double[pC.nRayBounces];
			final double[] smoothSharedDistance = new double[pC.nRayBounces];
			for (int i = 0; i < size; i++) {
				if (!pC.fastShared ) {
					if (reflRay.shared()[i] == 1) {
						smoothSharedEnergy[i] = 1;
						smoothSharedDistance[i] = reflRay.distToPlayer()[i];
					} else {
						int up; double traceUpRefl = 1; double traceUpDistance = 0;
						for (up = i + 1; up <= size; up++) {
							traceUpRefl *= up == size ? 0 : reflRay.bounceReflectivity()[up];
							if (up != size) traceUpDistance += reflRay.bounceDistance()[up];
							if (up != size && reflRay.shared()[up] == 1) { traceUpDistance += reflRay.distToPlayer()[up]; break; }
						}

						int dn; double traceDownRefl = 1; double traceDownDistance = 0;
						for (dn = i - 1; dn >= -1; dn--) {
							traceDownRefl *= dn == -1 ? 0 : reflRay.bounceReflectivity()[dn];
							if (dn != -1) traceDownDistance += reflRay.bounceDistance()[dn + 1];
							if (dn != -1 && reflRay.shared()[dn] == 1) { traceDownDistance += reflRay.distToPlayer()[dn]; break; }
						}

						if (Math.max(traceDownRefl, traceUpRefl) == traceUpRefl){
							smoothSharedEnergy[i] = traceUpRefl;
							smoothSharedDistance[i] = traceUpDistance;
						} else {
							smoothSharedEnergy[i] = traceDownRefl;
							smoothSharedDistance[i] = traceDownDistance;
						}
					}
				}

				sharedSum += reflRay.shared()[i];

				final double playerEnergy = MathHelper.clamp(
								reflRay.totalBounceEnergy()[i] * (pC.fastShared ? 1 : smoothSharedEnergy[i])
								* Math.pow(airAbsorptionHF, reflRay.totalBounceDistance()[i] + (pC.fastShared ? reflRay.distToPlayer()[i] : smoothSharedDistance[i]))
								/ Math.pow(reflRay.totalBounceDistance()[i] + (pC.fastShared ? reflRay.distToPlayer()[i] : smoothSharedDistance[i]), 2.0D * missedSum),
						0, 1);

				final double bounceEnergy = MathHelper.clamp(
								reflRay.totalBounceEnergy()[i]
								* Math.pow(airAbsorptionHF, reflRay.totalBounceDistance()[i])
								/ Math.pow(reflRay.totalBounceDistance()[i], 2.0D * missedSum),
						Double.MIN_VALUE, 1);

				final double bounceTime = reflRay.totalBounceDistance()[i] / speedOfSound;

				sendGain[MathHelper.clamp((int) (1/logBase(Math.max(Math.pow(bounceEnergy, pC.maxDecayTime / bounceTime * pC.energyFix), Double.MIN_VALUE), minEnergy) * pC.resolution), 0, pC.resolution)] += playerEnergy;
			}
		}
		sharedSum /= bounceCount;
		final double[] sendCutoff = new double[pC.resolution+1];
		for (int i = 0; i <= pC.resolution; i++) {
			sendGain[i] = MathHelper.clamp(sendGain[i] * (inWater ? pC.waterFilt : 1) * (pC.fastShared ? sharedSum : 1) * pC.resolution / bounceCount * pC.globalRvrbGain, 0, 1.0 - Double.MIN_NORMAL);
			sendCutoff[i] = Math.pow(sendGain[i], pC.globalRvrbHFRcp); // TODO: make sure this actually works.
		}

			//TODO: Occlusion calculation here

		directGain *= Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos))
				/ Math.pow(listenerPos.distanceTo(soundPos), 2.0 * missedSum)
				* MathHelper.lerp(sharedSum, 0d/*TODO: occlusion coeff from processing goes here IF fancy or fabulous occl*/, 1d);
		double directCutoff = Math.pow(directGain, pC.globalAbsHFRcp); // TODO: make sure this actually works.

		SoundProfile profile = new SoundProfile(sourceID, directGain, directCutoff, sendGain, sendCutoff);

		if (pC.eLog || pC.dLog) {
			LOGGER.info("Processed sound profile:\n{}", profile);
		} else {
			LOGGER.debug("Processed sound profile:\n{}", profile);
		}

		return profile;
	}

	@Environment(EnvType.CLIENT)
	public static void setEnv(final @NotNull SoundProfile profile) {
		if (ResoundingEngine.isOff) throw new IllegalStateException("ResoundingEngine must be started first! ");

		if (profile.sendGain().length != pC.resolution + 1 || profile.sendCutoff().length != pC.resolution + 1) {
			throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb resolution!");
		}

		SlotProfile finalSend = selectSlot(profile.sendGain(), profile.sendCutoff());

		if (pC.eLog || pC.dLog) {
			LOGGER.info("Final reverb settings:\n{}", finalSend);
		} else {
			LOGGER.debug("Final reverb settings:\n{}", finalSend);
		}

		// Set reverb send filter values and set source to send to all reverb fx slots
		ResoundingEFX.setFilter(finalSend.slot(), profile.sourceID(), (float) finalSend.gain(), (float) finalSend.cutoff());
		// Set direct filter values
		ResoundingEFX.setDirectFilter(profile.sourceID(), (float) profile.directGain(), (float) profile.directCutoff());
	}


	@Contract("_, _ -> new")
	@Environment(EnvType.CLIENT)
	private static @NotNull SlotProfile selectSlot(double[] sendGain, double[] sendCutoff) {
		if(pC.fastPick) { // TODO: find cause of block.lava.ambient NaN
			final double max = Arrays.stream(ArrayUtils.remove(sendGain, 0)).max().orElse(Double.NaN);
			int imax = 0; for (int i = 1; i <= pC.resolution; i++) { if (sendGain[i] == max){ imax=i; break; } }

			final int iavg;
			if (false) { // Different fast selection method, can't decide which one is better. TODO: Do something with this.
				double sum = 0;
				double weightedSum = 0;
				for (int i = 1; i <= pC.resolution; i++) {
					sum += sendGain[i];
					weightedSum += i * sendGain[i];
				}
				iavg = (int) Math.round(MathHelper.clamp(weightedSum / sum, 0, pC.resolution));
			} else { iavg = imax; }

			if (iavg > 0){ return new SlotProfile(iavg-1, sendGain[iavg], sendCutoff[iavg]); }
			return new SlotProfile(0, 0, 0);
		}
		// TODO: Slot selection logic will go here. See https://www.desmos.com/calculator/v5bt1gdgki
		return new SlotProfile(0, 0, 0);
	}

}
