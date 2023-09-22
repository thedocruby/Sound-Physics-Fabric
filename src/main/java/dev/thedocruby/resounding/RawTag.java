package dev.thedocruby.resounding;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public record RawTag(
        @Nullable Pattern[] patterns,    // regex' for blocks in tag
        @Nullable String[]  blocks,      // exact block names in tag
        @Nullable Pattern[] tagPatterns, // regex' for tags to include contents of
        @Nullable String[]  tags         // exact tags to include contents of
) {
}
