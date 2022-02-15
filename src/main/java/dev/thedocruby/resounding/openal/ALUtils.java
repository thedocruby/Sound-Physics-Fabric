package dev.thedocruby.resounding.openal;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;

import static dev.thedocruby.resounding.Resounding.LOGGER;

public class ALUtils{
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

    static boolean checkErrors(String message) {
        int i = AL10.alGetError();
        if (i != AL10.AL_NO_ERROR) {
            LOGGER.error("Caught new OpenAL AL10 error!\n{}\nCaused by: {}", message, getErrorMessage(i));
            return true;
        } return false;
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

    static boolean checkAlcErrors(long deviceHandle, String message) {
        int i = ALC10.alcGetError(deviceHandle);
        if (i != ALC10.ALC_NO_ERROR) {
            LOGGER.error("Caught new OpenAL ALC10 error!\n{}\nCaused by: {}\nDevice: {}", message, getAlcErrorMessage(i), deviceHandle);
            return true;
        } return false;
    }
}
