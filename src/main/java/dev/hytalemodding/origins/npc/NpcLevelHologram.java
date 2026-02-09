package dev.hytalemodding.origins.npc;

/**
 * 
 */
public final class NpcLevelHologram {
    private NpcLevelHologram() {
    }

    public static String buildLabel(String name, int npcLevel, boolean elite) {
        String safeName = name == null || name.isBlank() ? "NPC" : name;
        String star = elite ? "* " : "";
        return star + "[Lvl " + npcLevel + "] " + safeName;
    }

}
