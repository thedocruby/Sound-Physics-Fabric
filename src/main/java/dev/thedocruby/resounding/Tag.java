package dev.thedocruby.resounding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

// basic wrapper class for consistency
public record Tag(
        @NotNull String[] blocks // all blocks belonging to this tag
) {
}
