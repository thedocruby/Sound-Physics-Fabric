package com.sonicether.soundphysics;

import com.sonicether.soundphysics.performance.RaycastFix;
import com.sonicether.soundphysics.performance.SPHitResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.sonicether.soundphysics.ALstuff.SPEfx.setEnvironment;
import static com.sonicether.soundphysics.ALstuff.SPEfx.setupEFX;
import static com.sonicether.soundphysics.SPLog.*;
import static com.sonicether.soundphysics.config.PrecomputedConfig.pC;
import static com.sonicether.soundphysics.performance.RaycastFix.fixedRaycast;

@SuppressWarnings({"CommentedOutCode"})
@Environment(EnvType.CLIENT) //IDK why
public class SoundPhysics
{

	private static final Pattern rainPattern = Pattern.compile(".*rain.*");
	public static final Pattern stepPattern = Pattern.compile(".*step.*");
	private static final Pattern blockPattern = Pattern.compile(".*block..*");
	private static final Pattern uiPattern = Pattern.compile("ui..*");
	public static Set<Vec3d> rays;

	public static MinecraftClient mc;
	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;
	private static Vec3d playerPos;
	private static WorldChunk soundChunk;
	private static Vec3d soundPos;
	private static BlockPos soundBlockPos;
	// private static boolean doDirEval; // TODO: DirEval


	public static void init() {
		log("Initializing Sound Physics...");
		setupEFX();
		log("EFX ready...");
		mc = MinecraftClient.getInstance();
		updateRays();
	}

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

		rays = IntStream.range(0, pC.nRays).parallel().unordered().mapToObj((i) -> {
			final double theta = 2d * Math.PI * i / gRatio;
			final double phi = Math.acos(1d - 2d * (i + epsilon) / (pC.nRays - 1d + 2d * epsilon));

			return new Vec3d(
					Math.cos(theta) * Math.sin(phi),
					Math.sin(theta) * Math.sin(phi),
					Math.cos(phi)
			);
		}).collect(Collectors.toSet());
	}

	public static void setLastSoundCategoryAndName(SoundCategory sc, String name) { lastSoundCategory = sc; lastSoundName = name; }

	@SuppressWarnings("unused") @Deprecated
	public static void onPlaySound(double posX, double posY, double posZ, int sourceID){onPlaySoundReverb(posX, posY, posZ, sourceID, false);}

	@SuppressWarnings("unused") @Deprecated
	public static void onPlayReverb(double posX, double posY, double posZ, int sourceID){onPlaySoundReverb(posX, posY, posZ, sourceID, true);}

	public static void onPlaySoundReverb(double posX, double posY, double posZ, int sourceID, boolean auxOnly) {
		if (pC.dLog) logGeneral("On play sound... Source ID: " + sourceID + " " + posX + ", " + posY + ", " + posZ + "    Sound category: " + lastSoundCategory.toString() + "    Sound name: " + lastSoundName);

		long startTime = 0, endTime;
		if (pC.pLog) startTime = System.nanoTime();
		{
			evaluateEnvironment(sourceID, posX, posY, posZ, auxOnly);
		}
		if (pC.pLog) { endTime = System.nanoTime();
			log("Total calculation time for sound " + lastSoundName + ": " + (double)(endTime - startTime)/(double)1000000 + " milliseconds");
		}

	}
	
	private static double getBlockReflectivity(final BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).reflectivity;

		double r = pC.reflectivityMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultReflectivity : r;
	}

	private static double getBlockOcclusionD(final BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).absorption;

		double r = pC.absorptionMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultAbsorption : r;
	}

	private static Vec3d pseudoReflect(Vec3d dir, Vec3i normal)
	{return new Vec3d(normal.getX() == 0 ? dir.x : -dir.x, normal.getY() == 0 ? dir.y : -dir.y, normal.getZ() == 0 ? dir.z : -dir.z);}

	private static RayResult throwEnvironmentRay(Vec3d dir){
		RayResult result = new RayResult();

		SPHitResult rayHit = fixedRaycast(
				soundPos,
				soundPos.add(dir.multiply(pC.maxDistance)),
				mc.world,
				soundBlockPos,
				soundChunk
		);

		if (pC.dRays) RaycastRenderer.addSoundBounceRay(soundPos, rayHit.getPos(), Formatting.GREEN.getColorValue());

		if (rayHit.isMissed()) { result.missed = 1; return result; }

		BlockPos lastHitBlock = rayHit.getBlockPos();
		Vec3d lastHitPos = rayHit.getPos();
		Vec3i lastHitNormal = rayHit.getSide().getVector();
		Vec3d lastRayDir = dir;
		double lastBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());

		result.totalReflectivity = lastBlockReflectivity;
		result.bounceReflectivity[0] = lastBlockReflectivity;
		result.totalBounceReflectivity[0] = lastBlockReflectivity;

		result.totalDistance = soundPos.distanceTo(rayHit.getPos());
		result.bounceDistance[0] = result.totalDistance;
		result.totalBounceDistance[0] = result.totalDistance;

		result.shared[0] = 1;

		// Secondary ray bounces
		for (result.bounce = 1; result.bounce < pC.nRayBounces; result.bounce++) {

			final Vec3d newRayDir = pseudoReflect(lastRayDir, lastHitNormal);
			rayHit = fixedRaycast(lastHitPos, lastHitPos.add(newRayDir.multiply(pC.maxDistance - result.totalDistance)), mc.world, lastHitBlock, rayHit.chunk);
			// log("New ray dir: " + newRayDir.xCoord + ", " + newRayDir.yCoord + ", " + newRayDir.zCoord);

			if (rayHit.isMissed()) {
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, rayHit.getPos(), Formatting.DARK_RED.getColorValue());
				result.missed = Math.pow(result.totalReflectivity, 1 / pC.globalBlockReflectance);
				break;
			}

			final Vec3d newRayHitPos = rayHit.getPos();
			final double newRayLength = lastHitPos.distanceTo(newRayHitPos);
			result.totalDistance += newRayLength;
			if (pC.maxDistance - result.totalDistance < newRayHitPos.distanceTo(playerPos))
			{
				if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_PURPLE.getColorValue());
				result.missed = Math.pow(result.totalReflectivity, 1 / pC.globalBlockReflectance);
				break;
			}

			final double newBlockReflectivity = getBlockReflectivity(rayHit.getBlockState());
			result.totalReflectivity *= newBlockReflectivity;
			if (Math.pow(result.totalReflectivity, 1 / pC.globalBlockReflectance) < pC.minEnergy) { if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.DARK_BLUE.getColorValue()); break; }

			if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, newRayHitPos, Formatting.BLUE.getColorValue());

			lastBlockReflectivity = newBlockReflectivity;
			lastHitPos = newRayHitPos;
			lastHitNormal = rayHit.getSide().getVector();
			lastRayDir = newRayDir;
			lastHitBlock = rayHit.getBlockPos();

			result.playerDistance[result.bounce] = lastHitPos.distanceTo(playerPos);
			result.bounceDistance[result.bounce] = newRayLength;
			result.totalBounceDistance[result.bounce] = result.totalDistance;
			result.bounceReflectivity[result.bounce] = lastBlockReflectivity;
			result.totalBounceReflectivity[result.bounce] = result.totalReflectivity;

			// Cast (one) final ray towards the player. If it's
			// unobstructed, then the sound source and the player
			// share airspace.
			// TODO: do we even need shared airspace?

			final SPHitResult finalRayHit = fixedRaycast(lastHitPos, playerPos, mc.world, lastHitBlock, rayHit.chunk);

			int color = Formatting.GRAY.getColorValue();
			result.shared[result.bounce] = 0;
			if (finalRayHit.isMissed()) {
				color = Formatting.WHITE.getColorValue();
				result.shared[result.bounce] = 1;
			}
			if (pC.dRays) RaycastRenderer.addSoundBounceRay(lastHitPos, finalRayHit.getPos(), color);
		}
		for(int i = 1; i <= result.bounce; i++){
			if (result.shared[i] > 0) continue;
			double accumulator = 1;
			for(int j = i; result.shared[j] == 0; j++){
				accumulator *= result.bounceReflectivity[j+1];
			}
			result.shared[i] = accumulator;
		}
		return result;
	}

	private static void evaluateEnvironment(final int sourceID, double posX, double posY, double posZ, boolean auxOnly) {
		if (pC.off) return;

		if (mc.player == null || mc.world == null || posY <= mc.world.getBottomY() || (pC.recordsDisable && lastSoundCategory == SoundCategory.RECORDS) || uiPattern.matcher(lastSoundName).matches() || (posX == 0.0 && posY == 0.0 && posZ == 0.0))
		{
			//logDetailed("Menu sound!");
			try {
				setEnvironment(sourceID, new double[]{0f, 0f, 0f, 0f}, new double[]{1f, 1f, 1f, 1f}, auxOnly ? 0f : 1f, 1f);
			} catch (IllegalArgumentException e) { e.printStackTrace(); }
			return;
		}
		final long timeT = mc.world.getTime();

		final boolean isRain = rainPattern.matcher(lastSoundName).matches();
		boolean block = blockPattern.matcher(lastSoundName).matches() && !stepPattern.matcher(lastSoundName).matches();
		if (lastSoundCategory == SoundCategory.RECORDS){posX+=0.5;posY+=0.5;posZ+=0.5;block = true;}

		if (pC.skipRainOcclusionTracing && isRain)
		{
			try {
				setEnvironment(sourceID, new double[]{0f, 0f, 0f, 0f}, new double[]{1f, 1f, 1f, 1f}, auxOnly ? 0f : 1f, 1f);
			} catch (IllegalArgumentException e) { e.printStackTrace(); }
			return;
		}

		// Clear the block shape cache every tick, just in case the local block grid has changed
		// TODO: Do this more efficiently. in 1.18 there should be something in the client that i can mixin to and know which ticks the block grid changes
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

		// TODO: This still needs to be rewritten
		// TODO: fix reflection/absorption calc with an exponential
		//Direct sound occlusion

		soundPos = new Vec3d(posX, posY, posZ);
		Vec3d normalToPlayer = playerPos.subtract(soundPos).normalize();

		soundBlockPos = new BlockPos(soundPos.x, soundPos.y,soundPos.z);

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
					)); */ // TODO: DirEval
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


		if (isRain) {
			processEnvironment(true, sourceID, directCutoff, 0, directGain, auxOnly, null, new double[]{0d, 0d, 0d, 0d}); return;}

		// Throw rays around

		//doDirEval = pC.soundDirectionEvaluation && (occlusionAccumulation > 0 || pC.notOccludedRedirect); // TODO: DirEval

		Set<RayResult> results = rays.stream().parallel().unordered().map(SoundPhysics::throwEnvironmentRay).collect(Collectors.toSet());

		// pass data to post
		// TODO: `directCutoff` should be calculated with `directGain` in `processEnvironment()`, using an occlusionBrightness factor.
		processEnvironment(auxOnly, false, sourceID, directGain, directCutoff, results);
	}

	// TODO: This still needs to be rewritten
	// TODO: Fix questionable math in `processEnvironment()`
	// TODO: Then, make effect slot count a variable instead of hardcoding to 4
	private static void processEnvironment(boolean isRain, int sourceID, double directCutoff, double sharedAirspace, double directGain, boolean auxOnly, double[] bounceReflectivityRatio, double @NotNull [] sendGain) {
		// Calculate reverb parameters for this sound

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

			// ψ this shows a star at perceived sound pos ψ
			// Vec3d pos = sum.normalize().multiply(soundPos.distanceTo(playerPos)).add(playerPos);
			// mc.world.addParticle(ParticleTypes.END_ROD, false, pos.getX(), pos.getY(), pos.getZ(), 0,0,0);
		*/}

		assert mc.player != null;
		if (mc.player.isSubmergedInWater()) { directCutoff *= pC.underwaterFilter; }

		if (isRain) {
			try {
				setEnvironment(sourceID, sendGain, new double[]{1d, 1d, 1d, 1d}, directGain, directCutoff);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			return;
		}

		sharedAirspace *= 64d;

		if (pC.simplerSharedAirspaceSimulation) sharedAirspace /= pC.nRays; else sharedAirspace /= pC.nRays * pC.nRayBounces;

		final double[] sharedAirspaceWeight = new double[]{
				MathHelper.clamp(sharedAirspace * 0.05, 0d, 1d),
				MathHelper.clamp(sharedAirspace * 0.06666666666666667, 0d, 1d),
				MathHelper.clamp(sharedAirspace * 0.1, 0d, 1d),
				MathHelper.clamp(sharedAirspace * 0.1, 0d, 1d)
		};

		double[] sendCutoff = new double[]{
				directCutoff * (1d - sharedAirspaceWeight[0]) + sharedAirspaceWeight[0],
				directCutoff * (1d - sharedAirspaceWeight[1]) + sharedAirspaceWeight[1],
				directCutoff * (1d - sharedAirspaceWeight[2]) + sharedAirspaceWeight[2],
				directCutoff * (1d - sharedAirspaceWeight[3]) + sharedAirspaceWeight[3]
		};

		// attempt to preserve directionality when airspace is shared by allowing some dry signal through but filtered
		final double averageSharedAirspace = (sharedAirspaceWeight[0] + sharedAirspaceWeight[1] + sharedAirspaceWeight[2] + sharedAirspaceWeight[3]) * 0.25;
		directCutoff = Math.max(Math.pow(averageSharedAirspace, 0.5) * 0.2, directCutoff);

		directGain = auxOnly ? 0d : Math.pow(directCutoff, 0.1); // TODO: why is the previous value overridden? Fix `directGain` and `directCutoff`

		//logDetailed("HitRatio0: " + hitRatioBounce1 + " HitRatio1: " + hitRatioBounce2 + " HitRatio2: " + hitRatioBounce3 + " HitRatio3: " + hitRatioBounce4);

		if (pC.eLog) logEnvironment("Bounce reflectivity 0: " + bounceReflectivityRatio[0] + " bounce reflectivity 1: " + bounceReflectivityRatio[1] + " bounce reflectivity 2: " + bounceReflectivityRatio[2] + " bounce reflectivity 3: " + bounceReflectivityRatio[3]);


		sendGain[0] *= Math.pow(bounceReflectivityRatio[0], 1);
		sendGain[1] *= Math.pow(bounceReflectivityRatio[1], 2);
		sendGain[2] *= Math.pow(bounceReflectivityRatio[2], 3);
		sendGain[3] *= Math.pow(bounceReflectivityRatio[3], 4);

		sendGain[0] = MathHelper.clamp(sendGain[0], 0d, 1d);
		sendGain[1] = MathHelper.clamp(sendGain[1], 0d, 1d);
		sendGain[2] = MathHelper.clamp(sendGain[2], 0d, 1d);
		sendGain[3] = MathHelper.clamp(sendGain[3], 0d, 1d);

		sendGain[0] *= Math.pow(sendCutoff[0], 0.1);
		sendGain[1] *= Math.pow(sendCutoff[1], 0.1);
		sendGain[2] *= Math.pow(sendCutoff[2], 0.1);
		sendGain[3] *= Math.pow(sendCutoff[3], 0.1);

		if (pC.eLog) logEnvironment("Final environment settings:   " + Arrays.toString(sendGain));

		try {
			setEnvironment(sourceID, sendGain, sendCutoff, directGain, directCutoff);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
