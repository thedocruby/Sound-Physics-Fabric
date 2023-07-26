package dev.thedocruby.resounding.openal;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;

import static dev.thedocruby.resounding.config.PrecomputedConfig.pC;
import static dev.thedocruby.resounding.Engine.LOGGER;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;

public class ALUtils {

	private ALUtils(){}

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

	public static boolean checkErrors(Supplier<String> messageSupplier) {
		return checkErrors(s -> LOGGER.info(() -> s+messageSupplier.get()));
	}

	public static boolean checkErrors(String message) {
		return checkErrors(() -> message);
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
		return ALUtils.checkErrors(() -> "Error while applying "+in+"."+inID+" to "+out+"."+outID);
	}
	public static boolean errorApply(String[] in, int[] inIDs, String out, int outID) {
		if (in.length != inIDs.length) throw new IllegalStateException("differing input lengths");
		return ALUtils.checkErrors(() -> {
			StringJoiner messageJoiner = new StringJoiner(" & ", "Error while applying ", " to "+out+"."+outID);
			for (int i = 0; i<in.length; i++) {
				messageJoiner.add(in[i]+"."+inIDs[i]);
			}
			return messageJoiner.toString();
		});
	}
	public static boolean errorProperty(String type, int id, String property, float value) {
		return ALUtils.checkErrors(() -> "Error while setting "+type+"."+id+"."+property+" to "+value);
	}
	public static boolean errorSet(String type, String subset, int id, float value) {
		return ALUtils.checkErrors(() -> "Error while setting "+type+"."+subset+"."+id+" to "+value);
	}
	// }

}
