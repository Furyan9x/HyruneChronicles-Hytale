package dev.hytalemodding.hyrune.itemization;

/**
 * Non-gameplay context for generation diagnostics.
 */
public record ItemGenerationContext(
    String reason,
    String actorId,
    String triggerId,
    String professionSkill,
    Integer professionLevel,
    Integer benchTier
) {
    public static ItemGenerationContext of(String reason) {
        return new ItemGenerationContext(reason, null, null, null, null, null);
    }

    public static ItemGenerationContext crafting(String reason,
                                                 String actorId,
                                                 String triggerId,
                                                 String professionSkill,
                                                 Integer professionLevel,
                                                 Integer benchTier) {
        return new ItemGenerationContext(reason, actorId, triggerId, professionSkill, professionLevel, benchTier);
    }
}
