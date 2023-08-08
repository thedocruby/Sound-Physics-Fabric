package dev.thedocruby.resounding;

import org.jetbrains.annotations.NotNull;

public record Material(
        @NotNull Double impedance,   // impedance of material
        @NotNull Double permeation,  // permeation of material (inverse of absorption)
        @NotNull Double state        // state of matter [0 absent, .25 plasma, .5 gas, .75 liquid, 1 solid]
) {
}
