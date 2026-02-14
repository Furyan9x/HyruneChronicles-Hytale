package dev.hytalemodding.hyrune.social;

/**
 * Result returned by social actions.
 */
public record SocialActionResult(boolean success, String message) {
    public static SocialActionResult success(String message) {
        return new SocialActionResult(true, message);
    }

    public static SocialActionResult failure(String message) {
        return new SocialActionResult(false, message);
    }
}

