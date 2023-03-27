package dev.thedocruby.resounding;

// imports {
// internal {
import dev.thedocruby.resounding.openal.Context;
import dev.thedocruby.resounding.raycast.*;
import dev.thedocruby.resounding.toolbox.*;
// }
// fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
// }
// minecraft {
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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
// }
// logger {
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// }
// utils {
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
// }
// * static * {
import static dev.thedocruby.resounding.config.PrecomputedConfig.*;
import static dev.thedocruby.resounding.raycast.Cache.overlay;
import static java.util.Map.entry;
// }
// }

@SuppressWarnings({"CommentedOutCode"})
// TODO: do more Javadoc
public class Engine {
	// static definitions {
	private Engine() { }

	public static Context root;

	public static EnvType env = null;
	public static MinecraftClient mc;
	public static boolean isOff = true;
	public static final Logger LOGGER = LogManager.getLogger("Resounding");

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
	public static final Pattern uiPattern     = Pattern.compile("ui..*");
	// }

	// init vars {
	private static Set<Vec3d> rays;
	private static int viewDist;
	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;
	private static SoundListener lastSoundListener;
	private static Vec3d playerPos;
	private static Vec3d listenerPos;
	private static WorldChunk soundChunk;
	private static Vec3d soundPos;
	private static BlockPos soundBlockPos;
	private static boolean auxOnly;
	private static boolean isSpam;
	//private static boolean isBlock; // TODO: Occlusion
	//private static boolean doNineRay; // TODO: Occlusion
	private static long timeT;
	private static int sourceID;
	//private static boolean doDirEval; // TODO: DirEval
	// }
	// }
	
	public static void setRoot(Context context) {root=context;}
	/* utility function */
	public static <T> double logBase(T x, T b) {
		return Math.log((Double) x) / Math.log((Double) b);
	}

	@Environment(EnvType.CLIENT)
	public static void updateRays() {
		final double gRatio = 1.618033988;
		final double epsilon;

		{ // calculate rays + mem.context
			final int r = pC.nRays;
			if      (r >= 600000) { epsilon = 214;  }
			else if (r >= 400000) { epsilon = 75;   }
			else if (r >= 11000)  { epsilon = 27;   }
			else if (r >= 890)    { epsilon = 10;   }
			else if (r >= 177)    { epsilon = 3.33; }
			else if (r >= 24)     { epsilon = 1.33; }
			else                  { epsilon = 0.33; }

			// create queue and calculate vector // TODO verify functionality relative to comment
			rays = IntStream.range(0, r).parallel().unordered().mapToObj(i -> {
				// trig stuff
				final double theta = 2 * Math.PI * i / gRatio;
				final double phi = Math.acos(1 - 2*(i + epsilon) / (r - 1 + 2*epsilon));

				{ // calculate once + mem.context
					final double sP = Math.sin(phi);

					return new Vec3d(
							Math.cos(theta) * sP,
							Math.sin(theta) * sP,
							Math.cos(phi)
					);
				}
			}).collect(Collectors.toSet());
		}
	}

	@Environment(EnvType.CLIENT)
	public static void recordLastSound(@NotNull SoundInstance sound, SoundListener listener) {
		lastSoundCategory = sound.getCategory();
		lastSoundName = sound.getId().getPath();
		lastSoundListener = listener;
	}

	// wraps playSound() in order to process SVC audio chunks
	@Contract("_, _, _, _, _, _ -> _")
	@Environment(EnvType.CLIENT)
	public static void svc_playSound(Context context, double posX, double posY, double posZ, int sourceIDIn, boolean auxOnlyIn) {
		lastSoundName = "voice-chat";
		playSound(context, posX, posY, posZ, sourceIDIn, auxOnlyIn);
	}

	@Environment(EnvType.CLIENT)
	public static void playSound(Context context, double posX, double posY, double posZ, int sourceIDIn, boolean auxOnlyIn) { // The heart of the Resounding audio pipeline
		assert !Engine.isOff;
		long startTime = 0;
		if (pC.pLog) startTime = System.nanoTime();
		long endTime;
		auxOnly = auxOnlyIn;
		sourceID = sourceIDIn;

		// TODO integrate with audio tagging
		if (mc.player == null || mc.world == null || uiPattern.matcher(lastSoundName).matches() || ignorePattern.matcher(lastSoundName).matches()) {
			if (pC.dLog) {
				LOGGER.info("Skipped playing sound \"{}\": Not a world sound.", lastSoundName);
			} /* else {
				LOGGER.debug("Skipped playing sound \"{}\": Not a world sound.", lastSoundName);
			} */ // disabled for performance
			return;
		}

		// isBlock = blockPattern.matcher(lastSoundName).matches(); // && !stepPattern.matcher(lastSoundName).matches(); //  TODO: Occlusion, step sounds
		if (lastSoundCategory == SoundCategory.RECORDS){posX+=0.5;posY+=0.5;posZ+=0.5;/*isBlock = true;*/} // TODO: Occlusion
		if (stepPattern.matcher(lastSoundName).matches()) {posY+=0.2;} // TODO: step sounds
		// doNineRay = pC.nineRay && (lastSoundCategory == SoundCategory.BLOCKS || isBlock); // TODO: Occlusion
		{ // get pose - mem.saver
			Vec3d playerPosOld = mc.player.getPos();
			playerPos = new Vec3d(playerPosOld.x, playerPosOld.y + mc.player.getEyeHeight(mc.player.getPose()), playerPosOld.z);
		}
		listenerPos = lastSoundListener.getPos();
		final int bottom = mc.world.getBottomY();
		final int top = mc.world.getTopY();
		isSpam = spamPattern.matcher(lastSoundName).matches();
		soundPos = new Vec3d(posX, posY, posZ);
		viewDist = mc.options.getViewDistance();
		double maxDist = Math.min(
				Math.min(
					Math.min(mc.options.simulationDistance, viewDist),
					pC.soundSimulationDistance
				) * 16, // chunk
				pC.maxTraceDist / 2); // diameter -> radius
		soundChunk = mc.world.getChunk(((int)Math.floor(soundPos.x))>>4,((int)Math.floor(soundPos.z))>>4);
		soundBlockPos = new BlockPos(soundPos.x, soundPos.y, soundPos.z);
		timeT = mc.world.getTime();
		boolean isGentle = gentlePattern.matcher(lastSoundName).matches();

		String message = "";
		// TODO handle void as air, limit rays to 1 to player (if below void) and the rest upward
		// ^ treat outward movement in void as a null chunk (miss)
		if (
				posY          <= bottom || posY          >= top ||
				playerPos.y   <= bottom || playerPos.y   >= top ||
				listenerPos.y <= bottom || listenerPos.y >= top
			) {
			message = String.format("Skipped playing sound \"{}\": Cannot trace sounds outside the block grid.", lastSoundName);
		} else
		// if it's too quiet then simply don't apply effects...
		if (Math.max(playerPos.distanceTo(soundPos), listenerPos.distanceTo(soundPos)) > maxDist) {
			message = String.format("Skipped environment sampling for sound \"{}\": Sound is outside the maximum traceable distance with the current settings.", lastSoundName);
		} else
		if (pC.recordsDisable && lastSoundCategory == SoundCategory.RECORDS){
			message = String.format("Skipped environment sampling for sound \"{}\": Disabled sound.", lastSoundName);
		} else
		if (/*pC.skipRainOcclusionTracing && */isSpam) { // TODO: Occlusion
			message = String.format("Skipped environment sampling for sound \"{}\": Rain sound", lastSoundName);
		}

		if (!message.equals("")) {
			if (pC.dLog) {
				LOGGER.info(message);
			} /* else {
				LOGGER.debug(message);
			} */ // disabled for performance
			try { setEnv(context, processEnv(new EnvData(Collections.emptySet(), Collections.emptySet())), isGentle);
			} catch (IllegalArgumentException e) { e.printStackTrace(); } return;
		}
/*			message = String.format(
"""
Playing sound!
	Player Pos:    {}
	Listener Pos:    {}
	Source Pos:    {}
	Source ID:    {}
	Sound category:    {}
	Sound name:    {}""", playerPos, listenerPos, sourceID, soundPos, lastSoundCategory, lastSoundName); */ // disabled for debug, need better method
		if (pC.dLog) {
			LOGGER.info(message);
		} /* else {
			LOGGER.debug(message);
		} */ // disabled for performance
		try {  ////////  CORE SOUND PIPELINE  ////////

			setEnv(context, processEnv(evalEnv()), isGentle);

		} catch (Exception e) { e.printStackTrace(); }
		if (pC.pLog) {
			endTime = System.nanoTime();
			LOGGER.info("Total calculation time for sound {}: {} milliseconds", lastSoundName, (double)(endTime - startTime)/(double)1000000);
		}
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

	/* TODO: Occlusion
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
		Collision rayHit = Patch.fixedRaycast(
				soundPos,
				soundPos.add(dir.multiply(pC.maxTraceDist)),
				mc.world,
				soundBlockPos,
				soundChunk
		);

		if (pC.debug) Renderer.addSoundBounceRay(soundPos, rayHit.getPos(), Formatting.GREEN.getColorValue());

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
		// TODO integrate with fresnel values, and handle refraction (things
		// like wool)
		double lastBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());

		// TODO integrate with velocity logic, this' no simple task
		int size = 0;
		double missed = 0;
		double totalDistance = soundPos.distanceTo(lastHitPos);
		double totalReflectivity = lastBlockReflectivity;
		// TODO create class and interface, instead of many indexed values?
		double[] shared = new double[pC.nRayBounces];
		double[] distToPlayer = new double[pC.nRayBounces];
		double[] bounceDistance = new double[pC.nRayBounces];
		double[] totalBounceDistance = new double[pC.nRayBounces];
		double[] bounceReflectivity = new double[pC.nRayBounces];
		double[] totalBounceReflectivity = new double[pC.nRayBounces];

		// dummy (incorrect default) <- gets overridden
		Vec3i last = new Vec3i((int) soundPos.x+1, 0, 0); // section coords
		Vec3d position = soundPos;
		Vec3d angle = dir;
		double power = 128; // TODO fine-tune
		int bounces = 100;
		Cast cast = new Cast(null, soundChunk);
		// while power & bounces left
		while (power > 0 && bounces-- > 0) {
			// check if new chunk needed
			int posX = (int) position.x >> 4;
			int posY = (int) position.y >> 4;
			int posZ = (int) position.z >> 4;
			if (posX != last.getX() || posZ != last.getZ()) {
				cast.chunk = mc.world.getChunk(posX, posZ, ChunkStatus.FULL, false);
				cast.tree = overlay.get(new BlockPos(posX, posY, posZ));
			// or if new overlay section reached
			} else if (posY != last.getY()) {
				cast.tree = overlay.get(new BlockPos(posX, posY, posZ));
			}
			// assert angle != null; // should never happen -> power <= 0 -> null, yet breaks above
			Ray ray = cast.raycast(position,angle,power);
			// TODO save values from hit result
			// TODO handle bounces???
			if (pC.debug) Renderer.addSoundBounceRay(position, ray.position, Formatting.GREEN.getColorValue());
			position = ray.position;
			angle = ray.permeated;
			power = ray.permeation;
		}
		/* old code {
		bounceReflectivity[0] = lastBlockReflectivity;
		totalBounceReflectivity[0] = lastBlockReflectivity;

		bounceDistance[0] = totalDistance;
		totalBounceDistance[0] = totalDistance;
		distToPlayer[0] = lastHitPos.distanceTo(listenerPos);

		// TODO blend into sound repositioning code, adds better occlusion too
		// Cast (one) final ray towards the player. If it's
		// unobstructed, then the sound source and the player
		// share airspace.

		// TODO integrate exception into algorithm, must use a refactor, removes
		// duplicated code
		Collision finalRayHit = Patch.fixedRaycast(lastHitPos, listenerPos, mc.world, lastHitBlock, rayHit.chunk);

		int color = Formatting.GRAY.getColorValue();
		if (finalRayHit.isMissed()) {
			color = Formatting.WHITE.getColorValue();
			shared[0] = 1;
		}
		if (pC.debug) Renderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);

		// Secondary ray bounces
		for (int i = 1; i < pC.nRayBounces; i++) {

			// TODO handle velocity here, specifically
			final Vec3d newRayDir = Cast.pseudoReflect(lastRayDir, lastHitNormal);
			rayHit = Patch.fixedRaycast(lastHitPos, lastHitPos.add(newRayDir.multiply(pC.maxTraceDist - totalDistance)), mc.world, lastHitBlock, rayHit.chunk);

			if (rayHit.isMissed()) {
				if (pC.debug)
					Renderer.addSoundBounceRay(lastHitPos, rayHit.getPos(), Formatting.DARK_RED.getColorValue());
				// TODO airspace fresnel?
				missed = Math.pow(totalReflectivity, pC.globalReflRcp);
				break;
			}

			// TODO rework algorithm to eliminate duplicated code and extra
			// branch (the ~12 lines above, and ~9 below)
			final Vec3d newRayHitPos = rayHit.getPos();
			// TODO integrate with (optional) surface abnormality jitter
			bounceDistance[i] = lastHitPos.distanceTo(newRayHitPos);
			totalDistance += bounceDistance[i];
			if (pC.maxTraceDist - totalDistance < newRayHitPos.distanceTo(listenerPos)) {
				if (pC.debug)
					Renderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				missed = Math.pow(totalReflectivity, pC.globalReflRcp);
				break;
			}

			final double newBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());
			totalReflectivity *= newBlockReflectivity;
			// TODO integrate with velocity and fresnels+refraction
			if (totalReflectivity < minEnergy) {
				if (pC.debug)
					Renderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				break;
			}

			// if surface hit, and velocity still high enough, trace bounce
			if (pC.debug) Renderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.BLUE.getColorValue());

			lastBlockReflectivity = newBlockReflectivity;
			lastHitPos = newRayHitPos;
			lastHitNormal = rayHit.getSide().getVector();
			lastRayDir = newRayDir;
			lastHitBlock = rayHit.getBlockPos();

			// TODO see comment above `size` initialization
			size = i;
			totalBounceDistance[i] = totalDistance;
			distToPlayer[i] = lastHitPos.distanceTo(listenerPos);
			bounceReflectivity[i] = lastBlockReflectivity;
			totalBounceReflectivity[i] = totalReflectivity;

			// TODO duplicated code??
			// Cast (one) final ray towards the player. If it's
			// unobstructed, then the sound source and the player
			// share airspace.

			finalRayHit = Patch.fixedRaycast(lastHitPos, listenerPos, mc.world, lastHitBlock, rayHit.chunk);

			color = Formatting.GRAY.getColorValue();
			if (finalRayHit.isMissed()) {
				color = Formatting.WHITE.getColorValue();
				shared[i] = 1;
			}
			if (pC.debug) Renderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);
		}
		} */

		// TODO reorganize class structure for more logical order?
		return new ReflectedRayData(
				++size, missed, totalDistance,
				totalReflectivity, shared,
				distToPlayer, bounceDistance, totalBounceDistance,
				bounceReflectivity, totalBounceReflectivity);
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull Set<OccludedRayData> throwOcclRay(@NotNull Vec3d sourcePos, @NotNull Vec3d sinkPos) { //Direct sound occlusion
		assert !Engine.isOff;

		// TODO: This still needs to be rewritten
		// TODO: fix reflection/absorption calc with fresnels
		// TODO: Allow data theft from other reflection data, and cast occlusion
		// based on conditions then (e.g. rays with the highest distance w/out
		// bouncing)

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

				if (pC.debug) Renderer.addOcclusionRay(rayOrigin, rayHit.getPos(), Color.getHSBColor((float) (1F / 3F * (1F - Math.min(1F, occlusionAccumulation / 12F))), 1F, 1F).getRGB());
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
		if (isSpam) { return null; }
		*/

		return Collections.emptySet();
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull EnvData evalEnv() {
		// Clear the block shape cache every tick, just in case the local block grid has changed
		// TODO: Do this more efficiently.
		//  In 1.18 there should be something I can mix into to clear only in ticks when the block grid changes
		//  I think i remember a technical change relating to this;
		//  It may be faster to skim through the snapshot changelog instead of digging through the code.
		//  TODO split cache into chunks, refresh only relevant chunks,
		//  implement forgetting-system
		if (Patch.lastUpd != timeT) {
			Patch.shapeCache.clear();
			Patch.lastUpd = timeT;
		}

		// TODO make irrelevant with splitting/power/max bounce limits
		Patch.maxY = mc.world.getTopY();
		Patch.minY = mc.world.getBottomY();
		Patch.maxX = (int) (playerPos.getX() + (viewDist * 16));
		Patch.minX = (int) (playerPos.getX() - (viewDist * 16));
		Patch.maxZ = (int) (playerPos.getZ() + (viewDist * 16));
		Patch.minZ = (int) (playerPos.getZ() - (viewDist * 16));

		// Throw rays around
		// TODO implement with tagging system, and di-quadrant optimization
		// idea shared w/ Doc (go read that if you haven't yet)
		// TODO implement lambda function referencing to remove branches
		// Function logger = (pC.dLog) ? LOGGER.info : LOGGER.debug;
		Set<ReflectedRayData> reflRays;
		if (isSpam) {
			if (pC.dLog || pC.eLog) LOGGER.info("Skipped ray tracing for sound: {}", lastSoundName);
			reflRays = Collections.emptySet();
		} else {
			if (pC.dLog || pC.eLog) LOGGER.info("Sampling environment with {} seed rays...", pC.nRays);
			reflRays = rays.stream().parallel().unordered().map(Engine::throwReflRay).collect(Collectors.toSet());
			String message = "";
			if (pC.eLog) {
				int rayCount = 0;
				for (ReflectedRayData reflRay : reflRays){
					rayCount += reflRay.size() * 2 + 1;
				}
				message = " Total number of rays casted: "+rayCount;
				// LOGGER.info("Environment sampled! Total number of rays casted: {}", rayCount); // TODO: This is not precise
			} else if (pC.dLog) {
				LOGGER.info("Environment sampled!"+message);
			} /* else {
				LOGGER.debug("Environment sampled!");
			} */ // disabled for performance
		}

		// TODO: Occlusion. Also, add occlusion profiles.
		// Step rays from sound to listener
		Set<OccludedRayData> occlRays = throwOcclRay(soundPos, listenerPos);

		// Pass data to post
		EnvData data = new EnvData(reflRays, occlRays);
		if (pC.eLog) LOGGER.info("Raw Environment data:\n{}", data);
		return data;
	}

	@Contract("_ -> new")
	@Environment(EnvType.CLIENT)
	private static @NotNull SoundProfile processEnv(final EnvData data) {
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

		// TODO move to separate effect
		boolean inWater = mc.player != null && mc.player.isSubmergedInWater();
		// TODO properly implement, move to atmospherics
		final double airAbsorptionHF = 1.0; // Air.getAbsorptionHF();
		double directGain = (auxOnly ? 0 : inWater ? pC.waterFilt : 1) * Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos));

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

		// TODO: Does this perform better in parallel? (test using Spark)
		double sharedSum = 0.0D;
		final double[] sendGain = new double[pC.resolution + 1];
		// TODO explain
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

				// TODO integrate with fresnels
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

		double occlusion = Patch.fixedRaycast(soundPos, listenerPos, mc.world, soundBlockPos, soundChunk).isMissed() ? 1 : 0; // TODO: occlusion coeff from processing goes here IF fancy or fabulous occl

		directGain *= Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos))
				/ Math.pow(listenerPos.distanceTo(soundPos), 2.0 * missedSum)
				* MathHelper.lerp(occlusion, sharedSum, 1d);
		double directCutoff = Math.pow(directGain, pC.globalAbsHFRcp); // TODO: make sure this actually works.

		SoundProfile profile = new SoundProfile(sourceID, directGain, directCutoff, sendGain, sendCutoff);

		if (pC.eLog || pC.dLog) {
			LOGGER.info("Processed sound profile:\n{}", profile);
		} /* else {
			LOGGER.debug("Processed sound profile:\n{}", profile);
		} */ // disabled for performance

		return profile;
	}

	@Environment(EnvType.CLIENT)
	public static void setEnv(Context context, final @NotNull SoundProfile profile, boolean isGentle) {
		if (profile.sendGain().length != pC.resolution + 1 || profile.sendCutoff().length != pC.resolution + 1) {
			throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb resolution!");
		}

		SlotProfile finalSend = selectSlot(profile.sendGain(), profile.sendCutoff());

		if (pC.eLog || pC.dLog) {
			LOGGER.info("Final reverb settings:\n{}", finalSend);
		} /* else {
			LOGGER.debug("Final reverb settings:\n{}", finalSend);
		} */ // disabled for performance

		context.update(finalSend, profile, isGentle);
	}


	@Contract("_, _ -> new")
	@Environment(EnvType.CLIENT)
	public static @NotNull SlotProfile selectSlot(double[] sendGain, double[] sendCutoff) {
		if (pC.fastPick) { // TODO: find cause of block.lava.ambient NaN
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
