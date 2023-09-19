package dev.thedocruby.resounding;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public record Tag(
        @Nullable Pattern[] patterns, // regex' for blocks in tag
        @Nullable String[] blocks     // exact block names in tag
) {
}
