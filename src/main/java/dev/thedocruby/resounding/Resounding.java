package dev.thedocruby.resounding;

import dev.thedocruby.resounding.openal.ResoundingEFX;
import dev.thedocruby.resounding.effects.AirEffects;
import dev.thedocruby.resounding.performance.RaycastFix;
import dev.thedocruby.resounding.performance.SPHitResult;
import dev.thedocruby.resounding.config.PrecomputedConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
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

import static java.util.Map.entry;

@SuppressWarnings({"CommentedOutCode"})
public class Resounding {

	private Resounding() { }

	public static EnvType env = null;
	public static final Logger LOGGER = LogManager.getLogger("Resounding");
	private static final Pattern rainPattern = Pattern.compile(".*rain.*");
	public static final Pattern stepPattern = Pattern.compile(".*step.*");
	private static final Pattern blockPattern = Pattern.compile(".*block..*");
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
	public static final Map<String, String> groupMap = //<editor-fold desc="Map.ofEntries()">
			Map.ofEntries(
					entry("field_11528", "Coral"					),		// Coral              		(coral_block)
					entry("field_11529", "Gravel, Dirt"			),    	// Gravel, Dirt       		(gravel, rooted_dirt)
					entry("field_27197", "Amethyst"				),    	// Amethyst           		(amethyst_block, small_amethyst_bud, medium_amethyst_bud, large_amethyst_bud, amethyst_cluster)
					entry("field_11526", "Sand"					),    	// Sand               		(sand)
					entry("field_27196", "Candle Wax"				),    	// Candle Wax         		(candle)
					entry("field_22140", "Weeping Vines"			),    	// Weeping Vines      		(weeping_vines, weeping_vines_low_pitch)
					entry("field_22141", "Soul Sand"				),    	// Soul Sand          		(soul_sand)
					entry("field_22142", "Soul Soil"				),    	// Soul Soil          		(soul_soil)
					entry("field_22143", "Basalt"					),    	// Basalt             		(basalt)
					entry("field_22145", "Netherrack"				),    	// Netherrack         		(netherrack, nether_ore, nether_gold_ore)
					entry("field_22146", "Nether Brick"			),    	// Nether Brick       		(nether_bricks)
					entry("field_21214", "Honey"					),    	// Honey              		(honey_block)
					entry("field_22149", "Bone"					),    	// Bone               		(bone_block)
					entry("field_17581", "Nether Wart"			),    	// Nether Wart        		(nether_wart, wart_block)
					entry("field_11535", "Grass, Crops, Foliage"	),    	// Grass, Crops, Foliage  	(grass, crop, bamboo_sapling, sweet_berry_bush)
					entry("field_11533", "Metal"					),    	// Metal              		(metal, copper, anvil)
					entry("field_11534", "Aquatic Foliage"		),    	// Aquatic Foliage    		(wet_grass, lily_pad)
					entry("field_11537", "Glass, Ice"				),    	// Glass, Ice         		(glass)
					entry("field_28116", "Sculk Sensor"			),    	// Sculk Sensor       		(sculk_sensor)
					entry("field_22138", "Nether Foliage"			),    	// Nether Foliage     		(roots, nether_sprouts)
					entry("field_22139", "Shroomlight"			),    	// Shroomlight        		(shroomlight)
					entry("field_24119", "Chain"					),    	// Chain              		(chain)
					entry("field_29033", "Deepslate"				),    	// Deepslate          		(deepslate)
					entry("field_11547", "Wood"					),    	// Wood               		(wood, ladder)
					entry("field_29035", "Deepslate Tiles"		),    	// Deepslate Tiles    		(deepslate_tiles)
					entry("field_11544", "Stone, Blackstone"		),    	// Stone, Blackstone  		(stone, calcite, gilded_blackstone)
					entry("field_11545", "Slime"					),    	// Slime              		(slime_block)
					entry("field_29036", "Polished Deepslate"		),    	// Polished Deepslate 		(polished_deepslate, deepslate_bricks)
					entry("field_11548", "Snow"					),    	// Snow               		(snow)
					entry("field_28702", "Azalea Leaves"			),    	// Azalea Leaves      		(azalea_leaves)
					entry("field_11542", "Bamboo"					),    	// Bamboo             		(bamboo, scaffolding)
					entry("field_18852", "Mushroom Stems"			),    	// Mushroom Stems     		(stem)
					entry("field_11543", "Wool"					),    	// Wool               		(wool)
					entry("field_23083", "Dry Foliage"			),    	// Dry Foliage        		(vine, hanging_roots, glow_lichen)
					entry("field_28694", "Azalea Bush"			),    	// Azalea Bush        		(azalea)
					entry("field_28692", "Lush Cave Foliage"		),    	// Lush Cave Foliage       	(cave_vines, spore_blossom, small_dripleaf, big_dripleaf)
					entry("field_22150", "Netherite"				),    	// Netherite          		(netherite_block, lodestone)
					entry("field_22151", "Ancient Debris"			),    	// Ancient Debris     		(ancient_debris)
					entry("field_22152", "Nether Fungus Stem"		),    	// Nether Fungus Stem 		(nether_stem)
					entry("field_27884", "Powder Snow"			),    	// Powder Snow        		(powder_snow)
					entry("field_27202", "Tuff"					),    	// Tuff               		(tuff)
					entry("field_28697", "Moss"					),    	// Moss               		(moss, moss_carpet)
					entry("field_22153", "Nylium"					),    	// Nylium             		(nylium)
					entry("field_22154", "Nether Mushroom"		),    	// Nether Mushroom      	(fungus)
					entry("field_17734", "Lanterns"				),    	// Lanterns           		(lantern)
					entry("field_28060", "Dripstone"				),    	// Dripstone          		(dripstone_block, pointed_dripstone)
					entry("DEFAULT"    , "Default Material"		)     	// Default Material   		()
			);/*</editor-fold>*/
	public static Map<BlockSoundGroup, Pair<String, String>> blockSoundGroups;
	public static Map<String, BlockSoundGroup> groupSoundBlocks;
	public static Set<Vec3d> rays;

	public static MinecraftClient mc;
	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;
	private static Vec3d playerPos;
	private static WorldChunk soundChunk;
	private static Vec3d soundPos;
	private static BlockPos soundBlockPos;
	// private static boolean doDirEval; // TODO: DirEval

	@Environment(EnvType.CLIENT)
	public static void init() {
		LOGGER.info("Initializing Resounding...");
		ResoundingEFX.setupEFX();
		LOGGER.info("OpenAL EFX successfully primed for Resounding effects");
		mc = MinecraftClient.getInstance();
		updateRays();
	}

	public static <T> double logBase(T x, T b) {
		return (Math.log((Double) x) / Math.log((Double) b));
	}

	@Environment(EnvType.CLIENT)
	public static void updateRays() {
		final double gRatio = 1.618033988;
		final double epsilon;

		if (PrecomputedConfig.pC.nRays >= 600000) { epsilon = 214d; }
		else if (PrecomputedConfig.pC.nRays >= 400000) { epsilon = 75d; }
		else if (PrecomputedConfig.pC.nRays >= 11000) { epsilon = 27d; }
		else if (PrecomputedConfig.pC.nRays >= 890) { epsilon = 10d; }
		else if (PrecomputedConfig.pC.nRays >= 177) { epsilon = 3.33d; }
		else if (PrecomputedConfig.pC.nRays >= 24) { epsilon = 1.33d; }
		else { epsilon = 0.33d; }

		rays = IntStream.range(0, PrecomputedConfig.pC.nRays).parallel().unordered().mapToObj(i -> {
			final double theta = 2d * Math.PI * i / gRatio;
			final double phi = Math.acos(1d - 2d * (i + epsilon) / (PrecomputedConfig.pC.nRays - 1d + 2d * epsilon));

			return new Vec3d(
					Math.cos(theta) * Math.sin(phi),
					Math.sin(theta) * Math.sin(phi),
					Math.cos(phi)
			);
		}).collect(Collectors.toSet());
	}

	@Environment(EnvType.CLIENT)
	public static void setLastSoundCategoryAndName(SoundCategory sc, String name) { lastSoundCategory = sc; lastSoundName = name; }

	@Environment(EnvType.CLIENT)
	@SuppressWarnings("unused") @Deprecated
	public static void onPlaySound(double posX, double posY, double posZ, int sourceID){playSound(posX, posY, posZ, sourceID, false);}

	@Environment(EnvType.CLIENT)
	@SuppressWarnings("unused") @Deprecated
	public static void onPlayReverb(double posX, double posY, double posZ, int sourceID){playSound(posX, posY, posZ, sourceID, true);}

	@Environment(EnvType.CLIENT)
	public static void playSound(double posX, double posY, double posZ, int sourceID, boolean auxOnly) {
		if (PrecomputedConfig.pC.off) return;

		if (PrecomputedConfig.pC.dLog) {
			LOGGER.info("Playing sound!\n      Source ID:      {}\n Source Pos:     {}\n      Sound category: {}\n      Sound name:     {}", sourceID, new double[] {posX, posY, posZ}, lastSoundCategory, lastSoundName);
		} else {
			LOGGER.debug("Playing sound!\n      Source ID:      {}\n Source Pos:     {}\n      Sound category: {}\n      Sound name:     {}", sourceID, new double[] {posX, posY, posZ}, lastSoundCategory, lastSoundName);
		}

		long startTime = 0;
		long endTime;
		if (PrecomputedConfig.pC.pLog) startTime = System.nanoTime();

		evalEnv(sourceID, posX, posY, posZ, auxOnly);

		if (PrecomputedConfig.pC.pLog) { endTime = System.nanoTime();
			LOGGER.info("Total calculation time for sound {}: {} milliseconds", lastSoundName, (double)(endTime - startTime)/(double)1000000);
		}

	}

	@Environment(EnvType.CLIENT)
	private static double getBlockReflectivity(final @NotNull BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (PrecomputedConfig.pC.blockWhiteSet.contains(blockName)) return PrecomputedConfig.pC.blockWhiteMap.get(blockName).reflectivity;

		double r = PrecomputedConfig.pC.reflectivityMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? PrecomputedConfig.pC.defaultReflectivity : r;
	}
/*
	@Environment(EnvType.CLIENT)
	private static double getBlockOcclusionD(final BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).absorption;

		double r = pC.absorptionMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultAbsorption : r;
	}
*/

	@Contract("_, _ -> new")
	@Environment(EnvType.CLIENT)
	private static @NotNull Vec3d pseudoReflect(Vec3d dir, @NotNull Vec3i normal)
	{return new Vec3d(normal.getX() == 0 ? dir.x : -dir.x, normal.getY() == 0 ? dir.y : -dir.y, normal.getZ() == 0 ? dir.z : -dir.z);}

	@Environment(EnvType.CLIENT)
	private static @NotNull RayResult throwEnvRay(@NotNull Vec3d dir){
		RayResult result = new RayResult();

		SPHitResult rayHit = RaycastFix.fixedRaycast(
				soundPos,
				soundPos.add(dir.multiply(PrecomputedConfig.pC.maxDistance)),
				mc.world,
				soundBlockPos,
				soundChunk
		);

		if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(soundPos, rayHit.getPos(), Formatting.GREEN.getColorValue());

		if (rayHit.isMissed()) {
			result.totalReflectivity = 1;
			result.missed = 1; return result;
		}

		BlockPos lastHitBlock = rayHit.getBlockPos();
		Vec3d lastHitPos = rayHit.getPos();
		Vec3i lastHitNormal = rayHit.getSide().getVector();
		Vec3d lastRayDir = dir;
		double lastBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());

		result.totalReflectivity = lastBlockReflectivity;
		result.bounceReflectivity[0] = lastBlockReflectivity;
		result.totalBounceReflectivity[0] = lastBlockReflectivity;

		result.totalDistance = soundPos.distanceTo(rayHit.getPos());
		// result.bounceDistance[0] = result.totalDistance;
		result.totalBounceDistance[0] = result.totalDistance + lastHitPos.distanceTo(playerPos);

		// Cast (one) final ray towards the player. If it's
		// unobstructed, then the sound source and the player
		// share airspace.

		SPHitResult finalRayHit = RaycastFix.fixedRaycast(lastHitPos, playerPos, mc.world, lastHitBlock, rayHit.chunk);

		int color = Formatting.GRAY.getColorValue();
		if (finalRayHit.isMissed()) {
			color = Formatting.WHITE.getColorValue();
			result.shared[0] = 1;
		}
		if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);

		// Secondary ray bounces
		for (int i = 1; i < PrecomputedConfig.pC.nRayBounces; i++) {

			final Vec3d newRayDir = pseudoReflect(lastRayDir, lastHitNormal);
			rayHit = RaycastFix.fixedRaycast(lastHitPos, lastHitPos.add(newRayDir.multiply(PrecomputedConfig.pC.maxDistance - result.totalDistance)), mc.world, lastHitBlock, rayHit.chunk);
			// log("New ray dir: " + newRayDir.xCoord + ", " + newRayDir.yCoord + ", " + newRayDir.zCoord);

			if (rayHit.isMissed()) {
				if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, rayHit.getPos(), Formatting.DARK_RED.getColorValue());
				result.missed = Math.pow(result.totalReflectivity, PrecomputedConfig.pC.globalReflRcp);
				break;
			}

			final Vec3d newRayHitPos = rayHit.getPos();
			final double newRayLength = lastHitPos.distanceTo(newRayHitPos);
			result.totalDistance += newRayLength;
			if (PrecomputedConfig.pC.maxDistance - result.totalDistance < newRayHitPos.distanceTo(playerPos))
			{
				if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				result.missed = Math.pow(result.totalReflectivity, PrecomputedConfig.pC.globalReflRcp);
				break;
			}

			final double newBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());
			result.totalReflectivity *= newBlockReflectivity;
			if (Math.pow(result.totalReflectivity, PrecomputedConfig.pC.globalReflRcp) < PrecomputedConfig.pC.minEnergy) { if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_BLUE.getColorValue()); break; }

			if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.BLUE.getColorValue());

			lastBlockReflectivity = newBlockReflectivity;
			lastHitPos = newRayHitPos;
			lastHitNormal = rayHit.getSide().getVector();
			lastRayDir = newRayDir;
			lastHitBlock = rayHit.getBlockPos();

			// result.bounceDistance[result.bounce] = newRayLength;
			result.totalBounceDistance[i] = result.totalDistance + lastHitPos.distanceTo(playerPos);
			result.bounceReflectivity[i] = lastBlockReflectivity;
			result.totalBounceReflectivity[i] = result.totalReflectivity;
			result.lastBounce = i;

			// Cast (one) final ray towards the player. If it's
			// unobstructed, then the sound source and the player
			// share airspace.

			finalRayHit = RaycastFix.fixedRaycast(lastHitPos, playerPos, mc.world, lastHitBlock, rayHit.chunk);

			color = Formatting.GRAY.getColorValue();
			if (finalRayHit.isMissed()) {
				color = Formatting.WHITE.getColorValue();
				result.shared[i] = 1;
			}
			if (PrecomputedConfig.pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);
		}
		if (result.missed == 1) return result;
		for(int i = 0; i <= result.lastBounce; i++){
			if (result.shared[i] > 0) continue;
			double accumulator = 1;
			for(int j = i; result.shared[j] == 0 && j < PrecomputedConfig.pC.nRayBounces - 1; j++){
				accumulator *= result.bounceReflectivity[j+1];
			}
			result.shared[i] = accumulator;
		}
		return result;
	}

	@Environment(EnvType.CLIENT)
	private static void evalEnv(final int sourceID, double posX, double posY, double posZ, boolean auxOnly) {
		if (PrecomputedConfig.pC.off) return;

		if (mc.player == null || mc.world == null || posY <= mc.world.getBottomY() || (PrecomputedConfig.pC.recordsDisable && lastSoundCategory == SoundCategory.RECORDS) || uiPattern.matcher(lastSoundName).matches() || (posX == 0.0 && posY == 0.0 && posZ == 0.0))
		{
			//logDetailed("Menu sound!");
			try {
				ResoundingEFX.setEnv(sourceID, new double[]{0f, 0f, 0f, 0f}, new double[]{1f, 1f, 1f, 1f}, auxOnly ? 0f : 1f, 1f);
			} catch (IllegalArgumentException e) { e.printStackTrace(); }
			return;
		}
		final long timeT = mc.world.getTime();

		final boolean isRain = rainPattern.matcher(lastSoundName).matches();
		boolean block = blockPattern.matcher(lastSoundName).matches() && !stepPattern.matcher(lastSoundName).matches();
		if (lastSoundCategory == SoundCategory.RECORDS){posX+=0.5;posY+=0.5;posZ+=0.5;block = true;}

		if (PrecomputedConfig.pC.skipRainOcclusionTracing && isRain)
		{
			try {
				ResoundingEFX.setEnv(sourceID, new double[]{0f, 0f, 0f, 0f}, new double[]{1f, 1f, 1f, 1f}, auxOnly ? 0f : 1f, 1f);
			} catch (IllegalArgumentException e) { e.printStackTrace(); }
			return;
		}

		// Clear the block shape cache every tick, just in case the local block grid has changed
		// TODO: Do this more efficiently.
		//  In 1.18 there should be something I can mix into to clear only in ticks when the block grid changes
		if (RaycastFix.lastUpd != timeT) {
			RaycastFix.shapeCache.clear();
			RaycastFix.lastUpd = timeT;
		}

		Vec3d playerPosOld = mc.player.getPos();
		playerPos = new Vec3d(playerPosOld.x, playerPosOld.y + mc.player.getEyeHeight(mc.player.getPose()), playerPosOld.z);

		RaycastFix.maxY = mc.world.getTopY();
		RaycastFix.minY = mc.world.getBottomY();
		int dist = mc.options.viewDistance * 16;
		RaycastFix.maxX = (int) (playerPos.getX() + dist);
		RaycastFix.minX = (int) (playerPos.getX() - dist);
		RaycastFix.maxZ = (int) (playerPos.getZ() + dist);
		RaycastFix.minZ = (int) (playerPos.getZ() - dist);

		soundChunk = mc.world.getChunk(((int)Math.floor(posX))>>4,((int)Math.floor(posZ))>>4);
		soundPos = new Vec3d(posX, posY, posZ);
		soundBlockPos = new BlockPos(soundPos.x, soundPos.y,soundPos.z);

		// TODO: This still needs to be rewritten
		// TODO: fix reflection/absorption calc with an exponential
		//Direct sound occlusion

		/*
		Vec3d normalToPlayer = playerPos.subtract(soundPos).normalize();
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

		if (isRain) {
			processEnv(sourceID, auxOnly, null); return;}

		// Throw rays around

		//doDirEval = pC.soundDirectionEvaluation && (occlusionAccumulation > 0 || pC.notOccludedRedirect); // TODO: DirEval

		Set<RayResult> results = rays.stream().parallel().unordered().map(Resounding::throwEnvRay).collect(Collectors.toSet());

		// pass data to post
		// TODO: `directCutoff` should be calculated with `directGain` in `processEnvironment()`, using an occlusionBrightness factor.
		processEnv(sourceID, auxOnly, results);
	}

	@Environment(EnvType.CLIENT)
	private static void processEnv(int sourceID, boolean auxOnly, @Nullable Set<RayResult> results) {
		// Calculate reverb parameters for this sound
		double directGain = auxOnly ? 0 : 1; // TODO: fix occlusion so i don't have to override this.

		// TODO: DirEval is on hold while I rewrite, will be re-added later
		// Take weighted (on squared distance) average of the directions sound reflection came from
		dirEval:  // TODO: this block can be converted to another multithreaded iterator, which will be useful once I add more environment processing
		{/*
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
		*/}
		boolean inWater = false;
		double airAbsorptionHF = AirEffects.getAbsorptionHF();

		assert mc.player != null;
		if (mc.player.isSubmergedInWater()) { inWater = true; }

		if (results == null) {
			try { ResoundingEFX.setEnv(sourceID, new double[PrecomputedConfig.pC.resolution], new double[PrecomputedConfig.pC.resolution], directGain, directGain * PrecomputedConfig.pC.globalAbsorptionBrightness);
			} catch (IllegalArgumentException e) { e.printStackTrace(); }
			return;
		}

		// TODO: Does this perform better in parallel?
		double missedSum = 0.0D;
		double sharedAirspaceSum = 0.0D;
		double bounceCount = 0.0D;
		for (RayResult result : results) {
			bounceCount += result.lastBounce + 1;
			missedSum += result.missed;
			if (result.missed == 1.0D) continue;
			for (int j = 0; j <= result.lastBounce; j++)
				sharedAirspaceSum += result.shared[j];
		}
		missedSum *= PrecomputedConfig.pC.rcpNRays;
		sharedAirspaceSum /= bounceCount;

		// TODO: Does this perform better in parallel?
		// TODO: this is not really done correctly but its the best I can do without dynamic effects
		double[] sendGain = new double[PrecomputedConfig.pC.resolution];
		for (RayResult result : results) {
			if (result.missed == 1.0D) continue;
			for (int j = 0; j <= result.lastBounce; j++) {
				double energy = result.totalBounceReflectivity[j] / Math.pow(result.totalBounceDistance[j], (2.0D * missedSum) + 1 - airAbsorptionHF);
				int t = (int) (Math.pow(MathHelper.clamp(logBase(PrecomputedConfig.pC.minEnergy, 1.0D / energy) * -1.0D * result.totalBounceDistance[j] / PrecomputedConfig.speedOfSound * PrecomputedConfig.pC.maxDecayTime, 0.0D, 1.0D), PrecomputedConfig.pC.warpFactor) * (PrecomputedConfig.pC.resolution - 1));
				sendGain[t] += energy;
			}
		}
		// TODO: tailor shared to each effect slot, like it used to be
		directGain = inWater ? sharedAirspaceSum * PrecomputedConfig.pC.underwaterFilter : sharedAirspaceSum; //TODO: Replace this with occlusion calculation. Maybe add an occlusion mode toggle?
		double directCutoff = Math.pow(directGain, PrecomputedConfig.pC.globalAbsorptionBrightness);
		double[] sendCutoff = new double[PrecomputedConfig.pC.resolution];
		for (int i = 0; i < PrecomputedConfig.pC.resolution; i++) {
			sendGain[i] = Math.pow(sendGain[i] * (inWater ? sharedAirspaceSum * PrecomputedConfig.pC.underwaterFilter : sharedAirspaceSum)/ bounceCount, PrecomputedConfig.pC.globalReverbGain); // TODO: should I use `rcpTotalRays` here? would make reverb quieter
			sendCutoff[i] = Math.pow(sendGain[i], PrecomputedConfig.pC.globalReverbBrightness);
		}

		//logDetailed("HitRatio0: " + hitRatioBounce1 + " HitRatio1: " + hitRatioBounce2 + " HitRatio2: " + hitRatioBounce3 + " HitRatio3: " + hitRatioBounce4);
		//logEnvironment("Bounce reflectivity 0: " + bounceReflectivityRatio[0] + " bounce reflectivity 1: " + bounceReflectivityRatio[1] + " bounce reflectivity 2: " + bounceReflectivityRatio[2] + " bounce reflectivity 3: " + bounceReflectivityRatio[3]);

		if (PrecomputedConfig.pC.eLog) {
			LOGGER.info("Final environment settings:\n      Source Gain:    {}\n      Source Gain HF: {}\n      Reverb Gain:    {}\n      Reverb Gain HF: {}", directGain, directCutoff, sendGain, sendCutoff);
		} else {
			LOGGER.debug("Final environment settings:\n      Source Gain:    {}\n      Source Gain HF: {}\n      Reverb Gain:    {}\n      Reverb Gain HF: {}", directGain, directCutoff, sendGain, sendCutoff);
		}

		try {
			ResoundingEFX.setEnv(sourceID, sendGain, sendCutoff, directGain, directCutoff);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
