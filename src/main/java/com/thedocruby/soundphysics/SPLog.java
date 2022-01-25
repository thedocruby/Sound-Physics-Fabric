package com.thedocruby.soundphysics;

import org.lwjgl.openal.AL10;

public class SPLog {

    private static final String logPrefix = "[SOUND PHYSICS]";
    public static void log(String message)
    {
        System.out.println(logPrefix + ": " + message);
    }

    protected static void logOcclusion(String message) {System.out.println(logPrefix + " [OCCLUSION] " + ": " + message);}

    protected static void logEnvironment(String message) {System.out.println(logPrefix + " [ENVIRONMENT] " + ": " + message);}

    public static void logGeneral(String message) {System.out.println(logPrefix + ": " + message);}

    public static void logError(String errorMessage)
    {
        System.out.println(logPrefix + " [ERROR]: " + errorMessage);
    }

    public static void checkErrorLog(final String errorMessage)
    {
        final int error = AL10.alGetError();
        if (error == AL10.AL_NO_ERROR) {
            return;
        }

        String errorName;

        errorName = switch (error) {
            case AL10.AL_INVALID_NAME -> "AL_INVALID_NAME";
            case AL10.AL_INVALID_ENUM -> "AL_INVALID_ENUM";
            case AL10.AL_INVALID_VALUE -> "AL_INVALID_VALUE";
            case AL10.AL_INVALID_OPERATION -> "AL_INVALID_OPERATION";
            case AL10.AL_OUT_OF_MEMORY -> "AL_OUT_OF_MEMORY";
            default -> Integer.toString(error);
        };

        logError(errorMessage + " OpenAL error " + errorName);
    }

}
