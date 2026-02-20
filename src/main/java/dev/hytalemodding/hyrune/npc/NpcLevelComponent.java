package dev.hytalemodding.hyrune.npc;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * ECS component for npc level.
 */
public class NpcLevelComponent implements Component<EntityStore> {
    private int level;
    private String groupId;
    private CombatStyle weakness;
    private String archetypeId;
    private String rankId = NpcRank.NORMAL.name();
    private boolean elite;
    private String baseName;
    private String lastDisplayKey;

    public NpcLevelComponent() {
    }

    public NpcLevelComponent(int level,
                             String groupId,
                             CombatStyle weakness,
                             String archetypeId,
                             String rankId,
                             boolean elite,
                             String baseName) {
        this.level = level;
        this.groupId = groupId;
        this.weakness = weakness;
        this.archetypeId = archetypeId;
        this.rankId = rankId;
        this.elite = elite;
        this.baseName = baseName;
    }

    public NpcLevelComponent(int level,
                             String groupId,
                             CombatStyle weakness,
                             String archetypeId,
                             boolean elite,
                             String baseName) {
        this(level, groupId, weakness, archetypeId, NpcRank.NORMAL.name(), elite, baseName);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public CombatStyle getWeakness() {
        return weakness;
    }

    public void setWeakness(CombatStyle weakness) {
        this.weakness = weakness;
    }

    public String getArchetypeId() {
        return archetypeId;
    }

    public void setArchetypeId(String archetypeId) {
        this.archetypeId = archetypeId;
    }

    public boolean isElite() {
        return elite;
    }

    public void setElite(boolean elite) {
        this.elite = elite;
    }

    public String getRankId() {
        return rankId;
    }

    public void setRankId(String rankId) {
        this.rankId = rankId;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getLastDisplayKey() {
        return lastDisplayKey;
    }

    public void setLastDisplayKey(String lastDisplayKey) {
        this.lastDisplayKey = lastDisplayKey;
    }


    @Override
    public NpcLevelComponent clone() {
        NpcLevelComponent cloned = new NpcLevelComponent(level, groupId, weakness, archetypeId, rankId, elite, baseName);
        cloned.setLastDisplayKey(lastDisplayKey);
        return cloned;
    }
}
