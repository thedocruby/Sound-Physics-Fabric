package dev.thedocruby.resounding;

// imports {
// internal {

import dev.thedocruby.resounding.openal.Context;
import dev.thedocruby.resounding.raycast.Cast;
import dev.thedocruby.resounding.raycast.Ray;
import dev.thedocruby.resounding.raycast.Renderer;
import dev.thedocruby.resounding.toolbox.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.thedocruby.resounding.Cache.adjustSource;
import static dev.thedocruby.resounding.config.PrecomputedConfig.*;
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


	// init vars {
	private static Set<Vec3d> rays;
	private static SoundCategory category;
	private static String tag;
	private static SoundListener lastSoundListener;
	private static Vec3d playerPos;
	private static Vec3d listenerPos;
	private static @NotNull ChunkChain soundChunk;
	private static Vec3d soundPos;
	private static boolean auxOnly;
	private static int sourceID;

	private static boolean hasLoaded = false;
	//private static boolean doDirEval; // TODO: DirEval
	// }
	// }

	public static void setRoot(Context context) {root=context;}

	/* utility function */
	public static <T> double logBase(T x, T b) { return Math.log((Double) x) / Math.log((Double) b); }

	@Environment(EnvType.CLIENT)
	public static void updateRays() {
		final double rate = 2 * Math.PI / 1.618033988 /* phi */;
		final double epsilon =
			pC.nRays >= 600000 ? 214  :
			pC.nRays >= 400000 ? 75   :
			pC.nRays >= 11000  ? 27   :
			pC.nRays >= 890    ? 10   :
			pC.nRays >= 177    ? 3.33 :
			pC.nRays >= 24     ? 1.33 :
								 0.33 ;
		final double phiHelper = pC.nRays - 1 + 2*epsilon;

		// calculate starting vectors
		rays = IntStream.range(0, pC.nRays).parallel().unordered().mapToObj(i -> {
			// trig stuff
			final double theta = rate * i;
			final double phi = Math.acos(1 - 2*(i + epsilon) / phiHelper);
			final double sP = Math.sin(phi);

			return new Vec3d(
					Math.cos(theta) * sP,
					Math.sin(theta) * sP,
					Math.cos(phi)
			);
		}).collect(Collectors.toSet());
	}

	@Environment(EnvType.CLIENT)
	public static void recordLastSound(@NotNull SoundInstance sound, SoundListener listener) {
		category = sound.getCategory();
		tag = sound.getId().getPath();
		lastSoundListener = listener;
	}

	@Contract("_, _, _, _ -> _")
	@Environment(EnvType.CLIENT) // wraps playSound() in order to process SVC audio chunks
	public static void svc_playSound(Context context, Vec3d soundPos, int sourceIDIn, boolean auxOnlyIn) {
		tag = "voice-chat";
		playSound(context, soundPos, sourceIDIn, auxOnlyIn);
	}

	@Environment(EnvType.CLIENT)
	public static void playSound(Context context, Vec3d pos, int sourceIDIn, boolean auxOnlyIn) {
		assert !Engine.isOff;
		soundPos = pos;
		long startTime = 0;
		if (pC.pLog) startTime = System.nanoTime();
		auxOnly = auxOnlyIn;
		sourceID = sourceIDIn;
		//* TODO remove
		if (!hasLoaded) {
			Cache.generate(LOGGER::info);
			hasLoaded = true;
			return;
		}
		// */


		// adjust sound
		soundPos = adjustSource(category, tag, soundPos);
		// quit early if needed
		if (soundPos == null || mc.player == null || mc.world == null) {
			if (pC.dLog) LOGGER.info("skipped tracing sound \"{}\"", tag);
			return;
		}
		// get pose
		playerPos = mc.player.getPos().add(new Vec3d(0, mc.player.getEyeHeight(mc.player.getPose()), 0));
		listenerPos = lastSoundListener.getPos();
		double maxDist = Math.min(
				Math.min(
						Math.min(mc.options.simulationDistance, mc.options.viewDistance),
						pC.soundSimulationDistance
				) * 16, // chunk
				pC.maxTraceDist / 2); // diameter -> radius
		// too far/quiet
		if (Math.max(playerPos.distanceTo(soundPos), listenerPos.distanceTo(soundPos)) > maxDist) {
			if (pC.dLog) LOGGER.info("skipped tracing sound \"{}\"", tag);
			return;
		}

		// get chunk
		soundChunk = (ChunkChain) mc.world.getChunk((int) soundPos.x>>4, (int) soundPos.z>>4);
		boolean isGentle = Cache.gentlePattern.matcher(tag).matches();

		final EnvData env;
		if (pC.dLog) LOGGER.info(
				"Sound {"
					+ "\n  Player:   " + playerPos
					+ "\n  Listener: " + listenerPos
					+ "\n  Source:   " + soundPos
					+ "\n  ID:       " + sourceID
					+ "\n  Name:     " + category + "." + tag
					+ "\n  }"
		);
		env = evalEnv();

		// CORE PIPELINE
		try { setEnv(context, processEnv(env), isGentle); }
		catch (Exception e) { e.printStackTrace(); }

		if (pC.pLog) LOGGER.info("Total calculation time for sound {}: {} milliseconds",
				tag, (System.nanoTime() - startTime) / 10e5D);
	}

    /* TODO: Occlusion
	@Environment(EnvType.CLIENT)
	private static double getBlockOcclusionD(final BlockState blockState) {
		BlockSoundGroup soundType = blockState.getSoundGroup();
		String blockName = blockState.getBlock().getTranslationKey();
		if (pC.blockWhiteSet.contains(blockName)) return pC.blockWhiteMap.get(blockName).permeability();

		double r = pC.absorptionMap.getOrDefault(soundType, Double.NaN);
		return Double.isNaN(r) ? pC.defaultAbsorption : r;
	}
	*/

	@Environment(EnvType.CLIENT)
	private static @NotNull CastResults raycast(@NotNull Vec3d vector) {
		// int bounces = 100; // -> incompatible with present algorithms
		// assert mc.world != null; // should never happen (never should be called uninitialized)
		double amplitude = 128; // TODO fine-tune & pull from sound volume
		CastResults results = new CastResults(0,0,0);
		Cast cast = new Cast(mc.world, null, soundChunk);
		// launch initial ray & always permeate first
		cast.raycast(soundPos, vector, amplitude);
		Ray ray = new Ray(amplitude,cast.permeated.position(),cast.permeated.vector(), cast.permeated.length());

		double length = cast.permeated.length();
		Vec3d prior = soundPos; // used solely for debugging
		byte reflected = 0; // used to stop rays that are trapped between two walls
		// while power & iterate bounces
		while (results.bounces < pC.nRayBounces && ray.power() > 1) {
			// debugging output
			if (pC.dRays) Renderer.addSoundBounceRay(prior, ray.position(), Cache.colors[results.bounces % Cache.colors.length]);
			prior = ray.position();

			// cast ray
			cast.raycast(ray.position(),ray.vector(),ray.power());
			// } */

			//* handle properties {
			// TODO handle splits & replace:
			//  reflect instead of permeate, when logical
			if (cast.reflected.power() > cast.permeated.power()
					// TODO use better method for permeation preference near start
					* (2 - (pC.nRayBounces - results.bounces) / (double) pC.nRayBounces)) {
				// stop rays stuck between two walls (not moving)
				// num, not bool -> (3D) edges & corners
				if (reflected++ > 2) break;
				// record bounce results
				results.add
						/*shared   */( 0 // TODO figure out & populate
						/*distance */, cast.reflected.position().distanceTo(listenerPos)
						/*segment  */, length+cast.reflected.length()
						/*surface  */, cast.reflected.power()/ray.power()
						/*amplitude*/, cast.reflected.power()
						);

				ray = cast.reflected;
				length = 0;
			} else {
				ray = cast.permeated;
				length += ray.length();
				reflected = 0;
			}
			// } */
		}
		results.length += length;
		return results;
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull Set<OccludedRayData> throwOcclRay(@NotNull Vec3d sourcePos, @NotNull Vec3d sinkPos) { //Direct sound occlusion
		assert !Engine.isOff;

		// TODO: This still needs to be rewritten
		// TODO: fix reflection/permeability calc with fresnels
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
		// Throw rays around
		// TODO implement tagging system here
		// TODO? implement lambda function referencing to remove branches
		Consumer<String> logger = pC.log ? (pC.eLog ? LOGGER::info : LOGGER::debug) : (x) -> {};
		Set<CastResults> reflRays;
		logger.accept("Sampling environment with "+pC.nRays+" seed rays...");
		reflRays = rays.stream().parallel().unordered().map(Engine::raycast).collect(Collectors.toSet());
		if (pC.eLog) {
			int rayCount = 0;
			for (CastResults reflRay : reflRays){
				rayCount += reflRay.bounces * 2 + 1;
			}
			logger.accept("Total number of rays casted: "+rayCount);
		}

		// TODO: Occlusion. Also, add occlusion profiles.
		// Step rays from sound to listener
		Set<OccludedRayData> occlRays = throwOcclRay(soundPos, listenerPos);

		// Pass data to post
		EnvData data = new EnvData(reflRays, occlRays);
		logger.accept("Raw Environment data:\n"+data);
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
		for (CastResults reflRay : data.reflRays()) {
			bounceCount += reflRay.bounces;
			missedSum += reflRay.missed;
		}
		missedSum *= pC.rcpNRays;

		// TODO: Does this perform better in parallel? (test using Spark)
		double sharedSum = 0.0D;
		final double[] sendGain = new double[pC.resolution + 1];
		// TODO explain
		for (CastResults reflRay : data.reflRays()) {
			if (reflRay.missed == 1.0D) continue;

			final int size = reflRay.bounces;
			final double[] smoothSharedEnergy = new double[pC.nRayBounces];
			final double[] smoothSharedDistance = new double[pC.nRayBounces];
			for (int i = 0; i < size; i++) {
				if (!pC.fastShared ) {
					if (reflRay.shared[i] == 1) {
						smoothSharedEnergy[i] = 1;
						smoothSharedDistance[i] = reflRay.distance[i];
					} else {
						int up; double traceUpRefl = 1; double traceUpDistance = 0;
						for (up = i + 1; up <= size; up++) {
							traceUpRefl *= up == size ? 0 : reflRay.surfaces[up];
							if (up != size) traceUpDistance += reflRay.segments[up];
							if (up != size && reflRay.shared[up] == 1) { traceUpDistance += reflRay.distance[up]; break; }
						}

						int dn; double traceDownRefl = 1; double traceDownDistance = 0;
						for (dn = i - 1; dn >= -1; dn--) {
							traceDownRefl *= dn == -1 ? 0 : reflRay.surfaces[dn];
							if (dn != -1) traceDownDistance += reflRay.segments[dn + 1];
							if (dn != -1 && reflRay.shared[dn] == 1) { traceDownDistance += reflRay.distance[dn]; break; }
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

				sharedSum += reflRay.shared[i];

				// TODO integrate with fresnels
				final double playerEnergy = MathHelper.clamp(
						reflRay.amplitude[i] * (pC.fastShared ? 1 : smoothSharedEnergy[i])
								* Math.pow(airAbsorptionHF, reflRay.lengths[i] + (pC.fastShared ? reflRay.distance[i] : smoothSharedDistance[i]))
								/ Math.pow(reflRay.lengths[i] + (pC.fastShared ? reflRay.distance[i] : smoothSharedDistance[i]), 2.0D * missedSum),
						0, 1);

				final double bounceEnergy = MathHelper.clamp(
						reflRay.amplitude[i]
								* Math.pow(airAbsorptionHF, reflRay.lengths[i])
								/ Math.pow(reflRay.lengths[i], 2.0D * missedSum),
						Double.MIN_VALUE, 1);

				final double bounceTime = reflRay.lengths[i] / speedOfSound;

				sendGain[MathHelper.clamp((int) (1/logBase(Math.max(Math.pow(bounceEnergy, pC.maxDecayTime / bounceTime * pC.energyFix), Double.MIN_VALUE), minEnergy) * pC.resolution), 0, pC.resolution)] += playerEnergy;
			}
		}
		sharedSum /= bounceCount;
		final double[] sendCutoff = new double[pC.resolution+1];
		for (int i = 0; i <= pC.resolution; i++) {
			sendGain[i] = MathHelper.clamp(sendGain[i] * (inWater ? pC.waterFilt : 1) * (pC.fastShared ? sharedSum : 1) * pC.resolution / bounceCount * pC.globalRvrbGain, 0, 1.0 - Double.MIN_NORMAL);
			sendCutoff[i] = Math.pow(sendGain[i], pC.globalRvrbHFRcp); // TODO: make sure this actually works.
		}

		// TODO: Occlusion calculation here, instead of extra ray

		double occlusion = 1;

		// double occlusion = Patch.fixedRaycast(soundPos, listenerPos, mc.world, soundBlockPos, soundChunk).isMissed() ? 1 : 0; // TODO: occlusion coeff from processing goes here IF fancy or fabulous occl

		directGain *= Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos))
				/ Math.pow(listenerPos.distanceTo(soundPos), 2 * missedSum)
				* MathHelper.lerp(occlusion, sharedSum, 1);
		double directCutoff = Math.pow(directGain, pC.globalAbsHFRcp); // TODO: make sure this actually works.

		SoundProfile profile = new SoundProfile(sourceID, directGain, directCutoff, sendGain, sendCutoff);

		if (pC.log) LOGGER.info("Processed sound profile:\n{}", profile);

		return profile;
	}

	@Environment(EnvType.CLIENT)
	public static void setEnv(Context context, final @NotNull SoundProfile profile, boolean isGentle) {
		if (profile.sendGain().length != pC.resolution + 1 || profile.sendCutoff().length != pC.resolution + 1) {
			throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb resolution!");
		}

		final SlotProfile finalSend = selectSlot(profile.sendGain(), profile.sendCutoff());

		if (pC.eLog || pC.dLog) {
			LOGGER.info("Final reverb settings:\n{}", finalSend);
		}

		context.update(finalSend, profile, isGentle);
	}


	@Contract("_, _ -> new")
	@Environment(EnvType.CLIENT)
	public static @NotNull SlotProfile selectSlot(double[] sendGain, double[] sendCutoff) {
		if (pC.fastPick) { // TODO: find cause of block.lava.ambient NaN
			int slot = 0;
			double max = sendGain[1];
			for (int i = 2; i <= pC.resolution; i++) if (sendGain[i] > max) {
				slot=i;
				max = sendGain[i];
			}

			final int iavg = slot;
			// Different fast selection method, can't decide which one is better.
			// TODO: Do something with this.
            /* if (false) {
				double sum = 0;
				double weightedSum = 0;
				for (int i = 1; i <= pC.resolution; i++) {
					sum += sendGain[i];
					weightedSum += i * sendGain[i];
				}
				iavg = (int) Math.round(MathHelper.clamp(weightedSum / sum, 0, pC.resolution));
			} */

			return iavg > 0 ? new SlotProfile(iavg, sendGain[iavg], sendCutoff[iavg]) : new SlotProfile(0, sendGain[0], sendCutoff[0]);
		}
		// TODO: Slot selection logic will go here. See https://www.desmos.com/calculator/v5bt1gdgki
        /*
		final double mk = m-k;
		double selected = factorial(m)/(factorial(k)-factorial(mk))*Math.pow(1-x,mk)*Math.min(1,Math.max(0,Math.pow(x,k)));
		 */
		return new SlotProfile(0, 0, 0);
	}

}
