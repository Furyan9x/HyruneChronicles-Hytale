package dev.hytalemodding.origins.slayer;

/**
 * Result payload for a Slayer task turn-in.
 */
public record SlayerTurnInResult(int pointsAwarded, long slayerXpAwarded, boolean itemRewarded) {
}
