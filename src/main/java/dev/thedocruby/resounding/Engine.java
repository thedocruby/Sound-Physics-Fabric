package dev.thedocruby.resounding;

// imports {
// internal {

import dev.thedocruby.resounding.openal.Context;
import dev.thedocruby.resounding.raycast.Cast;
import dev.thedocruby.resounding.raycast.Hit;
import dev.thedocruby.resounding.raycast.Ray;
import dev.thedocruby.resounding.raycast.Renderer;
import dev.thedocruby.resounding.toolbox.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.BiPredicate;
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
	public static boolean on = false;


	// init vars {
	private static Set<Pair<Vec3d,Integer>> rays; // TODO: move inside sound entity
	private static String tag; // TODO: move inside sound entity
	private static Vec3d listenerPos; // TODO remove?
	private static SoundListener lastSoundListener; // TODO: move inside sound entity
	// TODO: refactor away ^^
	private static int sourceID; // TODO: move inside sound entity
	private static @NotNull ChunkChain soundChunk; // TODO: move inside sound entity
	private static Vec3d soundPos; // TODO: move inside sound entity

	private static SoundCategory category; // TODO: replace with tagging system
	private static boolean auxOnly; // TODO: rename?

	public static boolean hasLoaded = false;
	//private static boolean doDirEval; // TODO: DirEval
	// }
	// }

	public static void setRoot(Context context) {root=context;}

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

			return new Pair<>(new Vec3d(
					Math.cos(theta) * sP,
					Math.sin(theta) * sP,
					Math.cos(phi)
			), i);
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
	public static void play_svc(Context context, Vec3d soundPos, int sourceIDIn, boolean auxOnlyIn) {
		tag = "voice-chat";
		play(context, soundPos, sourceIDIn, auxOnlyIn);
	}

	@Environment(EnvType.CLIENT)
	public static void play(Context context, Vec3d pos, int sourceIDIn, boolean auxOnlyIn) {
		assert Engine.on;
		soundPos = pos;
		long startTime = 0;
		if (pC.pLog) startTime = System.nanoTime();
		auxOnly = auxOnlyIn;
		sourceID = sourceIDIn;
		/* TODO remove
		if (!hasLoaded) {
			hasLoaded = Cache.generate();
			return;
		}
		// */


		// adjust sound
		soundPos = adjustSource(category, tag, soundPos);
		// quit early if needed
		if (soundPos == null || mc.player == null || mc.world == null) {
			if (pC.dLog) Utils.LOGGER.info("skipped tracing sound \"{}\"", tag);
			return;
		}
		// get pose
		Cache.playerPos = mc.player.getPos().add(new Vec3d(0, mc.player.getEyeHeight(mc.player.getPose()), 0));
		listenerPos = lastSoundListener.getTransform().position();
		double maxDist = Math.min(
				Math.min(
						Math.min(
								mc.options.getSimulationDistance().getValue(),
								mc.options.getViewDistance().getValue()),
						pC.soundSimulationDistance
				) * 16, // chunk
				pC.maxTraceDist / 2); // diameter -> radius
		// too far/quiet
		if (Math.max(Cache.playerPos.distanceTo(soundPos), listenerPos.distanceTo(soundPos)) > maxDist) {
			if (pC.dLog) Utils.LOGGER.info("skipped tracing sound \"{}\"", tag);
			return;
		}

		// get chunk
		soundChunk = (ChunkChain) mc.world.getChunk((int) soundPos.x>>4, (int) soundPos.z>>4);
		boolean isGentle = Cache.gentlePattern.matcher(tag).matches();

		final EnvData env;
		if (pC.dLog) Utils.LOGGER.info(
				"Sound {"
					+ "\n  Player:   " + Cache.playerPos
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

		if (pC.pLog) Utils.LOGGER.info("Total calculation time for sound {}: {} milliseconds",
				tag, (System.nanoTime() - startTime) / 10e5D);
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull LinkedList<Hit> airspace(@NotNull Pair<Vec3d,Integer> input, double amplitude, Vec3d targetPosition) {
		return raycast(input, amplitude, input.getLeft().distanceTo(targetPosition), targetPosition,
				// always permeate!
				(Cast c, LinkedList<Hit> r) -> false
		);
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull LinkedList<Hit> raycast(@NotNull Pair<Vec3d,Integer> input, double amplitude) {
		return raycast(input, amplitude,
				(Cast cast, LinkedList<Hit> results) -> cast.reflected.power() > cast.transmitted.power()
						// TODO use better method for permeation preference near start
						* (2 - (pC.nRayBounces - results.size()) / (double) pC.nRayBounces)
		);
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull LinkedList<Hit> raycast(@NotNull Pair<Vec3d,Integer> input, double amplitude, BiPredicate<Cast, LinkedList<Hit>> reflect) {
		return raycast(input, amplitude, Double.POSITIVE_INFINITY, null, reflect);
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull LinkedList<Hit> raycast(@NotNull Pair<Vec3d,Integer> input, double amplitude, double maxLength, Vec3d targetPosition, BiFunction<Cast, LinkedList<Hit>, Boolean> reflect) {
		int id = input.getRight(); // for debug purposes
		Vec3d vector = input.getLeft();
	private static @NotNull LinkedList<Hit> raycast(@NotNull Pair<Vec3d,Integer> input, double amplitude, double maxLength, Vec3d targetPosition, BiPredicate<Cast, LinkedList<Hit>> reflect) {
		// TODO: allow arbitrary bounces per ray & splitting
		// int bounces = 100; // -> incompatible with present algorithms
		// assert mc.world != null; // should never happen (never should be called uninitialized)
//		double amplitude = 128; // TODO fine-tune & pull from sound volume
		LinkedList<Hit> results = new LinkedList<>();
		Cast cast = new Cast(mc.world, null, soundChunk, targetPosition);
		// launch initial ray & always permeate first
		cast.raycast(soundPos, vector, amplitude);
		Ray ray = new Ray(amplitude, cast.transmitted.position(), cast.transmitted.vector(), cast.transmitted.length());

		double length = cast.transmitted.length();
		Vec3d prior = soundPos; // used solely for debugging
		byte reflected = 0; // used to stop rays that are trapped between two walls
		int casts = 0;
		// while power, within max search range & iterate bounces
		while (ray.power() > 1 && maxLength > length && results.size() < pC.nRayBounces) {
			// debugging output
			if (pC.dRays) Renderer.addSoundBounceRay(prior, ray.position(), Cache.colors[(casts++ + id + results.size()) % Cache.colors.length]);
			prior = ray.position();

			// cast ray
			cast.raycast(ray.position(), ray.vector(), ray.power());
			// } */

			//* handle properties {
			// TODO handle splits & replace:
			//  reflect instead of permeate, when logical
			if (reflect.test(cast, results)) {
				// stop rays stuck between two walls (not moving)
				// num, not bool -> (3D) edges & corners
				if (reflected++ > 2) break;
				// record bounce results
				results.add(new Hit
						/*end pos  */( ray.position()
						/*length   */, length
						/*shared   */, 0 // TODO figure out & populate
						/*distance */, cast.reflected.position().distanceTo(listenerPos)
						/*segment  */, length+cast.reflected.length()
						/*surface  */, cast.reflected.power()/ray.power()
						/*amplitude*/, cast.reflected.power()
						));

				ray = cast.reflected;
				length = 0;
				continue;
			}
			ray = cast.transmitted;
			length += ray.length();
			reflected = 0;
			// } */
		}
		return results;
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull Set<OccludedRayData> throwOcclRay(@NotNull Vec3d sourcePos, @NotNull Vec3d sinkPos) { //Direct sound occlusion
		assert Engine.on;
		// TODO replace
		return Collections.emptySet();
	}

	@Environment(EnvType.CLIENT)
	private static @NotNull EnvData evalEnv() {
		// Throw rays around
		// TODO implement tagging system here
		// TODO? implement lambda function referencing to remove branches
		Consumer<String> logger = pC.log ? (pC.eLog ? Utils.LOGGER::info : Utils.LOGGER::debug) : x -> {};
		Set<LinkedList<Hit>> reflRays;
		logger.accept("Sampling environment with "+pC.nRays+" seed rays...");
		reflRays = rays.stream().parallel().unordered().map((ray) -> Engine.raycast(ray, 128)).collect(Collectors.toSet());
		if (pC.eLog) {
			int rayCount = 0;
			for (LinkedList<Hit> reflRay : reflRays) {
				rayCount += reflRay.size() * 2 + 1;
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
		// NOTE, removed pC.waterFilt logic, as it's superseded by new occlusion method
		// TODO properly implement, move to atmospherics
		final double airAbsorptionHF = 1.0; // Air.getAbsorptionHF();
		double directGain = (auxOnly ? 0 : 1) * Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos));

		if (data.reflRays().isEmpty()) {
			return new SoundProfile(sourceID, directGain, Math.pow(directGain, pC.globalAbsHFRcp), new double[pC.resolution + 1], new double[pC.resolution + 1]);
		}

		double bounces = 0.0D;
		double missed = 0.0D;
		for (LinkedList<Hit> ray : data.reflRays()) {
			bounces += ray.size();
			if (ray.getLast().amplitude() < 1) missed++;
		}
		missed /= pC.nRays;

		// TODO: Does this perform better in parallel? (test using Spark)
		double sharedSum = 0.0D;
		final double[] sendGain = new double[pC.resolution + 1];
		// NOTE temporary solution, will be removed during ray / redirection rework
		// TODO fix during ray / redirection rework
		double amplitude = 0.0D;
		// TODO explain
		for (LinkedList<Hit> ray : data.reflRays()) {
//			if (ray.missed == 1.0D) continue;

			final int size = ray.size();
			// TODO determine if array approach is needed here
			double smoothSharedEnergy = 0;
			double smoothSharedDistance = 0;
			int iterations = 0;
			for (Hit hit : ray) {
				if (!pC.fastShared) { // in-depth calculation
					if (hit.shared() == 1) {
						smoothSharedEnergy = 1;
						smoothSharedDistance = hit.distance();
						continue;
					}

					// Should be 0, old algorithm always returned 0 here. Will this behave better with proper value?
					smoothSharedEnergy = hit.reflect();
					smoothSharedDistance = hit.distance();
					// TODO use a better method to identify where occlusion / airspace rays should go
					// halfway through each ray, send occlusion ray
					if (++iterations == size / 2) {
						// TODO account for difference in angle in amplitude
						LinkedList<Hit> occlusion = airspace(new Pair<>(hit.position(), -1), hit.amplitude(), Cache.playerPos);
						double permeation = occlusion.getLast().amplitude();
						amplitude = Math.max(permeation, amplitude);
						// TODO determine accuracy of this method
						if (permeation > 1) sharedSum += size;
					}
				}

				final double playerEnergy =
						MathHelper.clamp(
						hit.amplitude() * (
								pC.fastShared
								? Math.pow(airAbsorptionHF, hit.length() + hit.distance())
								/ Math.pow(hit.length() + hit.distance(), 2.0D * missed)

								: smoothSharedEnergy
								* Math.pow(airAbsorptionHF, hit.length() + smoothSharedDistance)
								/ Math.pow(hit.length() + smoothSharedDistance, 2.0D * missed)
							),
						0, 1);

				final double bounceEnergy = MathHelper.clamp(
						hit.amplitude()
								* Math.pow(airAbsorptionHF, hit.length())
								/ Math.pow(hit.length(), 2.0D * missed),
						java.lang.Double.MIN_VALUE, 1);

				// TODO modify to use individual speed of sound in mediums
				final double bounceTime = hit.length() / speedOfSound;

				// TODO identify purpose..?
				sendGain[
						MathHelper.clamp((int) (1 / Utils.logBase(
								Math.max(
										Math.pow(bounceEnergy, pC.maxDecayTime / bounceTime * pC.energyFix),
										java.lang.Double.MIN_VALUE
								), minEnergy) * pC.resolution),
								0, pC.resolution)
						] += playerEnergy;

			}
		}

		sharedSum /= bounces;
		final double[] sendCutoff = new double[pC.resolution+1];
		for (int i = 0; i <= pC.resolution; i++) {
			// NOTE, removed pC.waterFilt logic, as it's superseded by new occlusion method
			sendGain[i] = MathHelper.clamp(sendGain[i] * (pC.fastShared ? sharedSum : 1) * pC.resolution / bounces * pC.globalRvrbGain, 0, 1.0 - java.lang.Double.MIN_NORMAL);
			sendCutoff[i] = Math.pow(sendGain[i], pC.globalRvrbHFRcp); // TODO: make sure this actually works.
		}

		// inverse of occlusion
		double permeation = amplitude / 128;

		directGain *= Math.pow(airAbsorptionHF, listenerPos.distanceTo(soundPos))
				/ Math.pow(listenerPos.distanceTo(soundPos), 2 * missed)
				* MathHelper.lerp(permeation, 1, sharedSum);
		double directCutoff = Math.pow(directGain, pC.globalAbsHFRcp); // TODO: make sure this actually works.

		SoundProfile profile = new SoundProfile(sourceID, directGain, directCutoff, sendGain, sendCutoff);

		if (pC.log) Utils.LOGGER.info("Processed sound profile:\n{}", profile);

		return profile;
	}

	@Environment(EnvType.CLIENT)
	public static void setEnv(Context context, final @NotNull SoundProfile profile, boolean isGentle) {
		if (profile.sendGain().length != pC.resolution + 1 || profile.sendCutoff().length != pC.resolution + 1) {
			throw new IllegalArgumentException("Error: Reverb parameter count does not match reverb resolution!");
		}

		final SlotProfile finalSend = selectSlot(profile.sendGain(), profile.sendCutoff());

		if (pC.eLog || pC.dLog) {
			Utils.LOGGER.info("Final reverb settings:\n{}", finalSend);
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

			return iavg > 0
				? new SlotProfile(iavg, sendGain[iavg], sendCutoff[iavg])
				: new SlotProfile(0, sendGain[0], sendCutoff[0]);
		}
		// TODO: Slot selection logic will go here. See https://www.desmos.com/calculator/v5bt1gdgki
        /*
		final double mk = m-k;
		double selected = factorial(m)/(factorial(k)-factorial(mk))*Math.pow(1-x,mk)*Math.min(1,Math.max(0,Math.pow(x,k)));
		 */
		return new SlotProfile(0, 0, 0);
	}

}
