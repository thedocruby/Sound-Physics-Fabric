package com.sonicether.soundphysics;

import com.google.common.util.concurrent.AtomicDoubleArray;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.sonicether.soundphysics.ALstuff.SPEfx.*;
import static com.sonicether.soundphysics.SPLog.*;
import static com.sonicether.soundphysics.performance.RaycastFix.fixedRaycast;
import static com.sonicether.soundphysics.config.PrecomputedConfig.pC;

@SuppressWarnings({"NonAsciiCharacters", "CommentedOutCode"})
@Environment(EnvType.CLIENT) //IDK why
public class SoundPhysics
{

	private static final Pattern rainPattern = Pattern.compile(".*rain.*");
	public static final Pattern stepPattern = Pattern.compile(".*step.*");
	private static final Pattern blockPattern = Pattern.compile(".*block..*");
	private static final Pattern uiPattern = Pattern.compile("ui..*");
	//Private fields
	// ψ time ψ
	//public static long tt = 0;
	//private static long ttt;
	//private static double cumtt = 0;
	//private static long navgt = 0;
	//public static void t1() {ttt = System.nanoTime(); }
	//public static void t2() { SoundPhysics.tt+=(System.nanoTime()-ttt);}
	//public static void tavg() { cumtt += tt; navgt++; }
	//public static void tout() { System.out.println((SoundPhysics.tt/1e6d) + "   Avg: " + cumtt/navgt/1e6d); }
	//public static void tres() { SoundPhysics.tt=0; }

	public static MinecraftClient mc;
	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;

	public static void init() {
		log("Initializing Sound Physics...");
		setupEFX();
		log("EFX ready...");
		mc = MinecraftClient.getInstance();
		//rand = new Random(System.currentTimeMillis());
	}

	public static void setLastSoundCategoryAndName(SoundCategory sc, String name) { lastSoundCategory = sc; lastSoundName = name; }

	@SuppressWarnings("unused") @Deprecated
	public static void onPlaySound(double posX, double posY, double posZ, int sourceID){onPlaySoundReverb(posX, posY, posZ, sourceID, false);}

	@SuppressWarnings("unused") @Deprecated
	public static void onPlayReverb(double posX, double posY, double posZ, int sourceID){onPlaySoundReverb(posX, posY, posZ, sourceID, true);}

	public static void onPlaySoundReverb(double posX, double posY, double posZ, int sourceID, boolean auxOnly) {
		if (pC.dLog) logGeneral("On play sound... Source ID: " + sourceID + " " + posX + ", " + posY + ", " + posZ + "    Sound category: " + lastSoundCategory.toString() + "    Sound name: " + lastSoundName);

		long startTime = 0;
		long endTime;
		
		if (pC.pLog) startTime = System.nanoTime();
		//t1();// rm
		evaluateEnvironment(sourceID, posX, posY, posZ, auxOnly); // time = 0.5? OωO
		//t2();
		//tavg();tres();//tout();// ψ time ψ
		if (pC.pLog) { endTime = System.nanoTime();
			log("Total calculation time for sound " + lastSoundName + ": " + (double)(endTime - startTime)/(double)1000000 + " milliseconds"); }

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

	@SuppressWarnings("ConstantConditions")
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

		if (RaycastFix.lastUpd != timeT) {
			if (timeT % 1024 == 0) {
				RaycastFix.shapeCache = new ConcurrentHashMap<>(2048); // just in case something gets corrupted
				//cumtt = 0; navgt = 0; ψ time ψ
			}
			else {
				RaycastFix.shapeCache.clear();
			}
			RaycastFix.lastUpd = timeT;
		}
		final Vec3d playerPosOld = mc.player.getPos();
		final Vec3d playerPos = new Vec3d(playerPosOld.x, playerPosOld.y + mc.player.getEyeHeight(mc.player.getPose()), playerPosOld.z);

		RaycastFix.maxY = mc.world.getTopY();
		RaycastFix.minY = mc.world.getBottomY();
		int dist = mc.options.viewDistance * 16;
		RaycastFix.maxX = (int) (playerPos.getX() + dist);
		RaycastFix.minX = (int) (playerPos.getX() - dist);
		RaycastFix.maxZ = (int) (playerPos.getZ() + dist);
		RaycastFix.minZ = (int) (playerPos.getZ() - dist);
		final WorldChunk soundChunk = mc.world.getChunk(((int)Math.floor(posX))>>4,((int)Math.floor(posZ))>>4);


		//Direct sound occlusion // time = 0.1 // TODO: This can be done better

		final Vec3d soundPos = new Vec3d(posX, posY, posZ);
		Vec3d normalToPlayer = playerPos.subtract(soundPos).normalize();

		final BlockPos soundBlockPos = new BlockPos(soundPos.x, soundPos.y,soundPos.z);

		if (pC.dLog) logGeneral("Player pos: " + playerPos.x + ", " + playerPos.y + ", " + playerPos.z + "      Sound Pos: " + soundPos.x + ", " + soundPos.y + ", " + soundPos.z + "       To player vector: " + normalToPlayer.x + ", " + normalToPlayer.y + ", " + normalToPlayer.z);
		double occlusionAccumulation = 0;
		final List<Map.Entry<Vec3d, Double>> directions = Collections.synchronizedList(new Vector<>(10, 10));
		//Cast a ray from the source towards the player
		Vec3d rayOrigin = soundPos;
		//System.out.println(rayOrigin.toString());
		BlockPos lastBlockPos = soundBlockPos;
		final boolean _9ray = pC._9Ray && (lastSoundCategory == SoundCategory.BLOCKS || block);
		final int nOccRays = _9ray ? 9 : 1;
		double occlusionAccMin = Double.MAX_VALUE;
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
					if (pC.soundDirectionEvaluation) directions.add(Map.entry(rayOrigin.subtract(playerPos),
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


		if (isRain) {finalizeEnvironment(true, sourceID, directCutoff, 0, directGain, auxOnly, null, new double[]{0d, 0d, 0d, 0d}); return;}

		// Shoot rays around sound

		final double maxDistance = 256 * pC.nRayBounces;

		boolean doDirEval = pC.soundDirectionEvaluation && (occlusionAccumulation > 0 || pC.notOccludedRedirect);

		final AtomicDoubleArray bounceReflectivityRatio = new AtomicDoubleArray(pC.nRayBounces);
		final AtomicDoubleArray δsendGain = new AtomicDoubleArray(new double[]{0d, 0d, 0d, 0d});
		final AtomicInteger sharedAirspace = new AtomicInteger();

		final double gRatio = 1.618033988;
		final double epsilon;
		
		if (pC.nRays >= 600000) { epsilon = 214d; }
		else if (pC.nRays >= 400000) { epsilon = 75d; }
		else if (pC.nRays >= 11000) { epsilon = 27d; }
		else if (pC.nRays >= 890) { epsilon = 10d; }
		else if (pC.nRays >= 177) { epsilon = 3.33d; }
		else if (pC.nRays >= 24) { epsilon = 1.33d; }
		else { epsilon = 0.33d; }

		//for (int i = 0; i < pC.nRays; i++)

		IntStream stream = IntStream.range(0, pC.nRays);
		(pC.multiThreading ? stream.parallel() : stream).forEach((i) -> { // time = 3
			final double x = (i + epsilon) / (pC.nRays - 1d + 2d*epsilon);
			final double y = (double) i / gRatio;
			final double theta = 2d * Math.PI * y;
			final double phi = Math.acos(1d - 2d*x);

			final Vec3d rayDir = new Vec3d(Math.cos(theta) * Math.sin(phi),
					Math.sin(theta) * Math.sin(phi), Math.cos(phi));

			final Vec3d rayEnd = new Vec3d(soundPos.x + rayDir.x * maxDistance, soundPos.y + rayDir.y * maxDistance,
					soundPos.z + rayDir.z * maxDistance);

			SPHitResult rayHit = fixedRaycast(soundPos, rayEnd, mc.world, soundBlockPos, soundChunk);

			if (pC.dRays) RaycastRenderer.addSoundBounceRay(soundPos, rayHit.getPos(), Formatting.GREEN.getColorValue());

			// TODO: This can be done better
			if (!rayHit.isMissed()) {

				// Additional bounces
				BlockPos lastHitBlock = rayHit.getBlockPos();
				Vec3d lastHitPos = rayHit.getPos();
				Vec3i lastHitNormal = rayHit.getSide().getVector();
				Vec3d lastRayDir = rayDir;

				double totalRayDistance = soundPos.distanceTo(rayHit.getPos());

				double blockReflectivity = getBlockReflectivity(rayHit.getBlockState());

				double totalReflectivityCoefficient = Math.min(blockReflectivity, 1);

				// Secondary ray bounces
				for (int j = 0; j < pC.nRayBounces; j++) {
					// Cast (one) final ray towards the player. If it's
					// unobstructed, then the sound source and the player
					// share airspace.
					final double energyTowardsPlayer = Math.pow(blockReflectivity, 1 / pC.globalBlockReflectance) * 0.1875 + 0.0625;
					if (!pC.simplerSharedAirspaceSimulation || j == pC.nRayBounces - 1) {
						final Vec3d finalRayStart = new Vec3d(lastHitPos.x + lastHitNormal.getX() * 0.01,
								lastHitPos.y + lastHitNormal.getY() * 0.01, lastHitPos.z + lastHitNormal.getZ() * 0.01);

						final SPHitResult finalRayHit = fixedRaycast(finalRayStart, playerPos, mc.world, null, rayHit.chunk);

						int color = Formatting.GRAY.getColorValue();
						if (finalRayHit.isMissed()) {
							color = Formatting.WHITE.getColorValue();

							double totalFinalRayDistance = totalRayDistance + finalRayStart.distanceTo(playerPos);

							if (doDirEval) directions.add(Map.entry(finalRayStart.subtract(playerPos), totalReflectivityCoefficient/(totalFinalRayDistance*totalFinalRayDistance)));
							//log("Secondary ray hit the player!");

							sharedAirspace.incrementAndGet();

							final double reflectionDelay = Math.max(totalRayDistance, 0.0) * 0.12 * Math.pow(blockReflectivity, 1 / pC.globalBlockReflectance);

							final double cross0 = 1d - MathHelper.clamp(Math.abs(reflectionDelay - 0d), 0d, 1d);
							final double cross1 = 1d - MathHelper.clamp(Math.abs(reflectionDelay - 1d), 0d, 1d);
							final double cross2 = 1d - MathHelper.clamp(Math.abs(reflectionDelay - 2d), 0d, 1d);
							final double cross3 = MathHelper.clamp(reflectionDelay - 2d, 0d, 1d);

							double factor = energyTowardsPlayer * 12.8 * pC.rcpTotRays;
							δsendGain.getAndAdd(0, cross0 * factor * 0.5);
							δsendGain.getAndAdd(1, cross1 * factor);
							δsendGain.getAndAdd(2, cross2 * factor);
							δsendGain.getAndAdd(3, cross3 * factor);

						}
						if (pC.dRays) RaycastRenderer.addSoundBounceRay(finalRayStart, finalRayHit.getPos(), color);
					}

					final Vec3d newRayDir = pseudoReflect(lastRayDir, lastHitNormal);
					final Vec3d newRayStart = lastHitPos;
					final Vec3d newRayEnd = new Vec3d(newRayStart.x + newRayDir.x * (maxDistance - totalRayDistance),
							newRayStart.y + newRayDir.y * (maxDistance - totalRayDistance), newRayStart.z + newRayDir.z * (maxDistance - totalRayDistance));

					//log("New ray dir: " + newRayDir.xCoord + ", " + newRayDir.yCoord + ", " + newRayDir.zCoord);

					rayHit = fixedRaycast(newRayStart, newRayEnd, mc.world, lastHitBlock, rayHit.chunk);

					if (rayHit.isMissed()) {
						if (pC.dRays) RaycastRenderer.addSoundBounceRay(newRayStart, newRayEnd, Formatting.DARK_RED.getColorValue());
						break;
					} else {
						final Vec3d newRayHitPos = rayHit.getPos();
						final double newRayLength = lastHitPos.distanceTo(newRayHitPos);

						if (pC.dRays) RaycastRenderer.addSoundBounceRay(newRayStart, newRayHitPos, Formatting.BLUE.getColorValue());

						bounceReflectivityRatio.getAndAdd(j, Math.pow(blockReflectivity, 1 / pC.globalBlockReflectance));

						totalRayDistance += newRayLength;

						lastHitPos = newRayHitPos;
						lastHitNormal = rayHit.getSide().getVector();
						lastRayDir = newRayDir;
						lastHitBlock = rayHit.getBlockPos();
						blockReflectivity = getBlockReflectivity(rayHit.getBlockState());
						totalReflectivityCoefficient *= Math.min(blockReflectivity, 1);

					}
				}
			}
		});
		for (int i = 0; i < pC.nRayBounces; i++) {
			double prev, next;
			do {
				prev = bounceReflectivityRatio.get(i);
				next = prev / pC.nRays;
			} while (!bounceReflectivityRatio.compareAndSet(i, prev, next));
		}

		// Take weighted (on squared distance) average of the directions sound reflection came from
		dirEval: // time = 0.04 // TODO: this block can be converted to another multithreaded iterator, which will be useful once I add more post processing
		{
			if (directions.isEmpty()) break dirEval;

			if (pC.pLog) log("Evaluating direction from " + sharedAirspace.get() + " entries...");
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
		}

		// convert `AtomicDoubleArray`s to `double[]`s
		double[] sendGainFinal = new double[δsendGain.length()];
		for (int i = 0; i < δsendGain.length(); i++) { sendGainFinal[i] = δsendGain.get(i); }

		double[] bounceReflRatFinal = new double[bounceReflectivityRatio.length()];
		for (int i = 0; i < bounceReflectivityRatio.length(); i++) { bounceReflRatFinal[i] = bounceReflectivityRatio.get(i); }

		// pass data to post
		finalizeEnvironment(false, sourceID, directCutoff, sharedAirspace.get(), directGain, auxOnly, bounceReflRatFinal, sendGainFinal);
	}

	// TODO: Fix questionable math in `finalizeEnvironment()`
	// TODO: Then, make effect slot count a variable instead of hardcoding to 4
	private static void finalizeEnvironment(boolean isRain, int sourceID, double directCutoff, double sharedAirspace, double directGain, boolean auxOnly, double[] bounceReflectivityRatio, double @NotNull [] sendGain) {
		// Calculate reverb parameters for this sound

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

		directGain = auxOnly ? 0d : Math.pow(directCutoff, 0.1); // TODO: why is the previous value overridden?

		//logDetailed("HitRatio0: " + hitRatioBounce1 + " HitRatio1: " + hitRatioBounce2 + " HitRatio2: " + hitRatioBounce3 + " HitRatio3: " + hitRatioBounce4);

		if (pC.eLog) logEnvironment("Bounce reflectivity 0: " + bounceReflectivityRatio[0] + " bounce reflectivity 1: " + bounceReflectivityRatio[1] + " bounce reflectivity 2: " + bounceReflectivityRatio[2] + " bounce reflectivity 3: " + bounceReflectivityRatio[3]);


		sendGain[1] *= bounceReflectivityRatio[1];
		sendGain[2] *= Math.pow(bounceReflectivityRatio[2], 3.0);
		sendGain[3] *= Math.pow(bounceReflectivityRatio[3], 4.0);

		sendGain[0] = MathHelper.clamp(sendGain[0], 0d, 1d);
		sendGain[1] = MathHelper.clamp(sendGain[1], 0d, 1d);
		sendGain[2] = MathHelper.clamp(sendGain[2] * 1.05 - 0.05, 0d, 1d);
		sendGain[3] = MathHelper.clamp(sendGain[3] * 1.05 - 0.05, 0d, 1d);

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
