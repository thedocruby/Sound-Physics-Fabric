package dev.thedocruby.resounding;

import org.lwjgl.openal.AL10;

public class ResoundingLog {

    private static final String logPrefix = "[RESOUNDING";
    public static void log(String message) { System.out.println(logPrefix + ": " + message); }

    protected static void logOcclusion(String message) { System.out.println(logPrefix + " | OCCLUSION LOG]: " + message); }

    protected static void logEnvironment(String message) { System.out.println(logPrefix + " | ENVIRONMENT LOG]: " + message); }

    public static void logGeneral(String message) { System.out.println(logPrefix + " LOG]: " + message); }

    public static void logError(String errorMessage) { System.out.println(logPrefix + " | ERROR LOG]: " + errorMessage); }

    public static void checkErrorLog(final String errorMessage)
    {
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

        logError(errorMessage + " Caused by: OpenAL \"" + errorName + "\" error.");
    }

}
