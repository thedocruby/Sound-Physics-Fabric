package dev.thedocruby.resounding.openal;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;
import static dev.thedocruby.resounding.Engine.LOGGER;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class ALUtils {

	private ALUtils(){}

	public static Optional<String> consumeErrorMessage() {
		int errorCode = AL10.alGetError();

		return Optional.ofNullable(switch (errorCode) {
		case AL10.AL_INVALID_ENUM -> "Illegal enum value. (AL_INVALID_ENUM)";
		case AL10.AL_INVALID_NAME -> "Invalid name parameter. (AL_INVALID_NAME)";
		case AL10.AL_INVALID_OPERATION -> "Invalid operation. (AL_INVALID_OPERATION)";
		case AL10.AL_INVALID_VALUE -> "Invalid input value. (AL_INVALID_VALUE)";
		case AL10.AL_NO_ERROR -> null;
		case AL10.AL_OUT_OF_MEMORY -> "Unable to allocate memory. (AL_OUT_OF_MEMORY)";
		default -> "An unrecognized error. (AL_" + errorCode + ')';
		});
	}

	@Contract(pure = true)
	private static @NotNull String getErrorMessage(int errorCode) {
		switch(errorCode) {
			case AL10.AL_INVALID_NAME:
				return "Invalid name parameter. (AL_INVALID_NAME)";
			case AL10.AL_INVALID_ENUM:
				return "Illegal enum value. (AL_INVALID_ENUM)";
			case AL10.AL_INVALID_VALUE:
				return "Invalid input value. (AL_INVALID_VALUE)";
			case AL10.AL_INVALID_OPERATION:
				return "Invalid operation. (AL_INVALID_OPERATION)";
			case AL10.AL_OUT_OF_MEMORY:
				return "Unable to allocate memory. (AL_OUT_OF_MEMORY)";
			default:
				return "An unrecognized error. (AL_" + errorCode + ')';
		}
	}

	public static boolean checkErrors(Consumer<String> callback) {
		// TODO introduce an error log toggle
		if (!pC.dLog) return false;
		int i = AL10.alGetError();
		if (i != AL10.AL_NO_ERROR) {
			callback.accept("OpenAL AL error "+getErrorMessage(i)+": ");
			return true;
		} return false;
		
	}

	public static boolean checkErrors(String message) {
		return checkErrors(s -> LOGGER.info(s+message));
	}

	@Contract(pure = true)
	private static @NotNull String getAlcErrorMessage(int errorCode) {
		switch(errorCode) {
			case ALC10.ALC_INVALID_DEVICE:
				return "Invalid device. (ALC_INVALID_DEVICE)";
			case ALC10.ALC_INVALID_CONTEXT:
				return "Invalid context. (ALC_INVALID_CONTEXT)";
			case ALC10.ALC_INVALID_ENUM:
				return "Illegal enum value. (ALC_INVALID_ENUM)";
			case ALC10.ALC_INVALID_VALUE:
				return "Invalid input value. (ALC_INVALID_VALUE)";
			case ALC10.ALC_OUT_OF_MEMORY:
				return "Unable to allocate memory. (ALC_OUT_OF_MEMORY)";
			default:
				return "An unrecognized error. (ALC_" + errorCode + ')';
		}
	}

	private static boolean checkAlcErrors(long deviceHandle, String message) {
		int i = ALC10.alcGetError(deviceHandle);
		if (i != ALC10.ALC_NO_ERROR) {
			LOGGER.error("Caught new OpenAL ALC10 error!\n{}\nCaused by: {}\nDevice: {}", message, getAlcErrorMessage(i), deviceHandle);
			return true;
		} return false;
	}

	// wrapped error functions {
	public static boolean errorApply(String   in, int   inID, String out, int outID) {
		return ALUtils.logAndConsumeApplyError(in, inID, out, outID);
	}
	public static boolean errorApply(String[] in, int[] inIDs, String out, int outID) {
		return ALUtils.logAndConsumeMultiApplyError(in, inIDs, out, outID);
	}
	public static boolean errorProperty(String type, int id, String property, float value) {
		return ALUtils.logAndConsumeSetError(type, id, property, value);
	}
	public static boolean errorSet(String type, String subset, int id, float value) {
		return ALUtils.logAndConsumeSetError(type, subset, id, value);
	}
	// }

	public static boolean logAndConsumeApplyError(String in, int inID, String out, int outID) {
		if (!pC.dLog) return false;
		Optional<String> errorMessage = consumeErrorMessage();
		if (errorMessage.isEmpty()) return false;
		LOGGER.info("OpenAL AL error {}: Error while applying {}.{} to {}.{}", errorMessage.get(), in, inID, out, outID);
		return true;
	}

	public static boolean logAndConsumeError(Object consequence) {
		if (!pC.dLog) return false;
		Optional<String> errorMessage = consumeErrorMessage();
		if (errorMessage.isEmpty()) return false;
		LOGGER.info("OpenAL AL error {}: {}", errorMessage.get(), consequence);
		return true;
	}

	public static boolean logAndConsumeMultiApplyError(String[] in, int[] inIDs, String out, int outID) {
		if (in.length != inIDs.length) throw new IllegalStateException("differing input lengths");
		if (!pC.dLog || !LOGGER.isInfoEnabled()) return false;
		Optional<String> errorMessage = consumeErrorMessage();
		if (errorMessage.isEmpty()) return false;
		StringJoiner messageJoiner = new StringJoiner(" & ",
				"OpenAL AL error " + errorMessage.get() + ": Error while applying ",
				" to " + out + "." + outID);
		for (int i = 0; i<in.length; i++) {
			messageJoiner.add(in[i]+"."+inIDs[i]);
		}
		LOGGER.info(messageJoiner);
		return true;
	}

	private static boolean logAndConsumeSetError(String type, String subtype, String subsubtype, float value) {
		if (!pC.dLog) return false;
		Optional<String> errorMessage = consumeErrorMessage();
		if (errorMessage.isEmpty()) return false;
		LOGGER.info("OpenAL AL error {}: Error while setting {}.{}.{} to {}", errorMessage.get(), type, subtype, subsubtype, value);
		return true;
	}

	public static boolean logAndConsumeSetError(String type, String subset, int id, float value) {
		return logAndConsumeSetError(type, subset, Integer.toString(id), value);
	}

	public static boolean logAndConsumeSetError(String type, int id, String property, float value) {
		return logAndConsumeSetError(type, Integer.toString(id), property, value);
	}

}
