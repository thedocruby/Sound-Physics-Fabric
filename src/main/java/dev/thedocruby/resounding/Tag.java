package dev.thedocruby.resounding;

import org.jetbrains.annotations.Nullable;

public record Tag(
    @Nullable String[] regexes, // regex for blocks in tag
    @Nullable String[] blocks   // exact block names for tag
) {
}
