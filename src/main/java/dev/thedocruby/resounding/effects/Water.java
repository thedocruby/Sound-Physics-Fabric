package dev.thedocruby.resounding.effects;

// Water absorption filter effect, see logic flow below for a basic description
public class Water extends Effect {
// bidirectional logic
// strength: - weak -- medium --- heavy (same applies for +)
// vol = volume  change -
// lp  = lowpass filter +
// submerge(emitter        ) = vol--  lp++  // only sound in water
// submerge(         player) = vol--- lp+++ // only you   in water
// submerge(               ) = no change    // both       in air
// submerge(emitter, player) = vol-   lp+   // both       in water

// python/math pseudocode
// def submerge(sound,  player):
//   amount = 2(player)-sound
//   -vol(amount)
//   +lp (amount)
}

