package dev.hytalemodding.hyrune.slayer;

/**
 * Result payload for a Slayer task turn-in.
 */
public record SlayerTurnInResult(int pointsAwarded,
                                 long slayerXpAwarded,
                                 boolean itemRewarded,
                                 int streak,
                                 int streakMilestoneBonusPoints) {
}
