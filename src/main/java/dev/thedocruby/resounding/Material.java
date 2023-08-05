package dev.thedocruby.resounding;

import org.jetbrains.annotations.NotNull;

public record Material(
        @NotNull Double impedance,   // impedance of material
        @NotNull Double absorption,  // absorption of material
        @NotNull Double state        // state of matter
) {
}
