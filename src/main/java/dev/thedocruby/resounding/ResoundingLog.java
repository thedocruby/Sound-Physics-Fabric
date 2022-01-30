package dev.thedocruby.resounding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.openal.AL10;

// TODO: Remove ResoundingLog.java
public class ResoundingLog {

    private ResoundingLog() {}

    public static void checkErrorLog(final String errorMessage) {
        final int error = AL10.alGetError();
        if (error == AL10.AL_NO_ERROR) {
            return;
        }

        String errorName;

        errorName = switch (error) {
            case AL10.AL_INVALID_NAME -> "INVALID_NAME";
            case AL10.AL_INVALID_ENUM -> "INVALID_ENUM";
            case AL10.AL_INVALID_VALUE -> "INVALID_VALUE";
            case AL10.AL_INVALID_OPERATION -> "INVALID_OPERATION";
            case AL10.AL_OUT_OF_MEMORY -> "OUT_OF_MEMORY";
            default -> Integer.toString(error);
        };

        Resounding.LOGGER.error(errorMessage + " Caused by: OpenAL \"" + errorName + "\" error.");
    }

}
