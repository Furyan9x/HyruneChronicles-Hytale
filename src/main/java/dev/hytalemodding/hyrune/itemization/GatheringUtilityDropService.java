package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.skills.SkillType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gathering-only utility stat hooks for block break speed, rare drops, and double drops.
 */
public final class GatheringUtilityDropService {
    private static final double MAX_DOUBLE_DROP_CHANCE = 0.60;
    private static final double MAX_RARE_DROP_CHANCE = 0.95;

    private GatheringUtilityDropService() {
    }

    public static List<ItemStack> resolveBlockUtilityDrops(SkillType skill,
                                                           BlockType blockType,
                                                           PlayerItemizationStats stats,
                                                           ThreadLocalRandom random) {
        if (skill == null || blockType == null || stats == null || random == null) {
            return List.of();
        }

        List<ItemStack> out = new ObjectArrayList<>();
        if (shouldDoubleDropForGathering(stats.getItemDoubleDropChanceBonus(), random)) {
            out.addAll(resolveConfiguredGatheringDrops(skill, blockType));
        }
        out.addAll(resolveRareDrops(skill, stats.getItemRareDropChanceBonus(), random));
        return out;
    }

    public static List<ItemStack> resolveRareDrops(SkillType skill,
                                                   PlayerItemizationStats stats,
                                                   ThreadLocalRandom random) {
        if (stats == null) {
            return List.of();
        }
        return resolveRareDrops(skill, stats.getItemRareDropChanceBonus(), random);
    }

    public static List<ItemStack> resolveRareDrops(SkillType skill,
                                                   double rareDropChanceBonus,
                                                   ThreadLocalRandom random) {
        if (skill == null || random == null) {
            return List.of();
        }

        HyruneConfig cfg = HyruneConfigManager.getConfig();
        HyruneConfig.GatheringUtilityDropConfig utilityCfg = cfg == null ? null : cfg.gatheringUtilityDrops;
        if (utilityCfg == null || !utilityCfg.enableRareDrops) {
            return List.of();
        }

        HyruneConfig.GatheringRareDropSkillConfig skillCfg = resolveSkillConfig(utilityCfg.rareDropsBySkill, skill);
        if (skillCfg == null || skillCfg.drops == null || skillCfg.drops.isEmpty()) {
            return List.of();
        }

        double chance = clamp(skillCfg.baseChance + clamp(rareDropChanceBonus, 0.0, 1.0), 0.0, MAX_RARE_DROP_CHANCE);
        if (chance <= 0.0 || random.nextDouble() >= chance) {
            return List.of();
        }

        HyruneConfig.GatheringRareDropEntry picked = weightedPick(skillCfg.drops, random);
        if (picked == null || picked.itemId == null || picked.itemId.isBlank() || !ItemModule.exists(picked.itemId)) {
            return List.of();
        }

        int min = Math.max(1, picked.minQuantity);
        int max = Math.max(min, picked.maxQuantity);
        int quantity = min == max ? min : random.nextInt(min, max + 1);
        return List.of(new ItemStack(picked.itemId, quantity));
    }

    public static boolean shouldDoubleDrop(double chance, ThreadLocalRandom random) {
        return random != null && shouldDoubleDrop(chance, random.nextDouble());
    }

    public static boolean shouldDoubleDropForGathering(double chance, ThreadLocalRandom random) {
        HyruneConfig cfg = HyruneConfigManager.getConfig();
        if (cfg == null || cfg.gatheringUtilityDrops == null || !cfg.gatheringUtilityDrops.enableDoubleDrops) {
            return false;
        }
        return shouldDoubleDrop(chance, random);
    }

    public static boolean shouldDoubleDrop(double chance, double roll) {
        double clampedChance = clamp(chance, 0.0, MAX_DOUBLE_DROP_CHANCE);
        double clampedRoll = clamp(roll, 0.0, 1.0);
        return clampedChance > 0.0 && clampedRoll < clampedChance;
    }

    public static int doubledQuantity(int quantity) {
        long doubled = Math.max(1, quantity) * 2L;
        return (int) Math.min(Integer.MAX_VALUE, doubled);
    }

    private static List<ItemStack> resolveConfiguredGatheringDrops(SkillType skill, BlockType blockType) {
        BlockGathering gathering = blockType == null ? null : blockType.getGathering();
        if (gathering == null) {
            return List.of();
        }

        if (skill == SkillType.FARMING) {
            List<ItemStack> harvest = materializeHarvestDrops(gathering.getHarvest());
            if (!harvest.isEmpty()) {
                return harvest;
            }
        }
        return materializeBreakingDrops(gathering.getBreaking());
    }

    private static List<ItemStack> materializeBreakingDrops(BlockBreakingDropType breaking) {
        if (breaking == null) {
            return List.of();
        }
        List<ItemStack> out = new ObjectArrayList<>();
        if (breaking.getItemId() != null && !breaking.getItemId().isBlank()) {
            int quantity = Math.max(1, breaking.getQuantity());
            out.add(new ItemStack(breaking.getItemId(), quantity));
        }
        if (breaking.getDropListId() != null && !breaking.getDropListId().isBlank()) {
            ItemModule module = ItemModule.get();
            if (module != null && module.isEnabled()) {
                out.addAll(module.getRandomItemDrops(breaking.getDropListId()));
            }
        }
        return out;
    }

    private static List<ItemStack> materializeHarvestDrops(HarvestingDropType harvest) {
        if (harvest == null) {
            return List.of();
        }
        List<ItemStack> out = new ObjectArrayList<>();
        if (harvest.getItemId() != null && !harvest.getItemId().isBlank()) {
            out.add(new ItemStack(harvest.getItemId(), 1));
        }
        if (harvest.getDropListId() != null && !harvest.getDropListId().isBlank()) {
            ItemModule module = ItemModule.get();
            if (module != null && module.isEnabled()) {
                out.addAll(module.getRandomItemDrops(harvest.getDropListId()));
            }
        }
        return out;
    }

    private static HyruneConfig.GatheringRareDropSkillConfig resolveSkillConfig(Map<String, HyruneConfig.GatheringRareDropSkillConfig> bySkill,
                                                                                 SkillType skill) {
        if (bySkill == null || bySkill.isEmpty() || skill == null) {
            return null;
        }
        HyruneConfig.GatheringRareDropSkillConfig direct = bySkill.get(skill.name());
        if (direct != null) {
            return direct;
        }
        return bySkill.get(skill.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static HyruneConfig.GatheringRareDropEntry weightedPick(List<HyruneConfig.GatheringRareDropEntry> entries,
                                                                    ThreadLocalRandom random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        double total = 0.0;
        for (HyruneConfig.GatheringRareDropEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            total += Math.max(0.0, entry.weight);
        }
        if (total <= 0.0) {
            for (HyruneConfig.GatheringRareDropEntry entry : entries) {
                if (entry != null) {
                    return entry;
                }
            }
            return null;
        }

        double roll = random.nextDouble(total);
        double cursor = 0.0;
        for (HyruneConfig.GatheringRareDropEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            cursor += Math.max(0.0, entry.weight);
            if (roll <= cursor) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
