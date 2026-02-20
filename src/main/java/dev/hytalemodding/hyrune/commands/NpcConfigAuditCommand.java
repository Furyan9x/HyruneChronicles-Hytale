package dev.hytalemodding.hyrune.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.npc.NpcFamiliesConfig;
import dev.hytalemodding.hyrune.npc.NpcFamilyMatcher;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Audits npc_families mappings against local Hytale role ids.
 */
public class NpcConfigAuditCommand extends AbstractPlayerCommand {
    private static final String NPC_ROLES_ROOT = "./lib/Server/NPC/Roles";
    private static final Set<String> FILE_PREFIX_EXCLUDES = Set.of(
        "component_", "template_", "blanktemplate", "empty_role"
    );
    private static final Set<String> ROLE_SEGMENT_EXCLUDES = Set.of(
        "_core", "components", "templates", "tests", "tests_development"
    );
    private static final int PREVIEW_LIMIT = 10;

    public NpcConfigAuditCommand() {
        super("npcaudit", "Audit npc family mappings: orphans, conflicts, and profile distribution.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        NpcFamiliesConfig cfg = Hyrune.getNpcFamiliesConfig();
        if (cfg == null) {
            ctx.sendMessage(Message.raw("[NpcAudit] npc_families config is not loaded."));
            return;
        }

        List<String> roleIds = scanValidRoleIds();
        if (roleIds.isEmpty()) {
            File root = new File(NPC_ROLES_ROOT);
            ctx.sendMessage(Message.raw("[NpcAudit] No valid role ids found under "
                + root.getAbsolutePath() + " (scan skipped; families config can still be valid)."));
            ctx.sendMessage(Message.raw("[NpcAudit] families=" + safeSize(cfg.families)));
            return;
        }

        List<String> orphans = new ArrayList<>();
        List<Conflict> conflicts = new ArrayList<>();
        Map<String, Integer> assignmentArchetypes = familyArchetypeDistribution(cfg);

        for (String roleId : roleIds) {
            List<String> matchedFamilies = matchedFamilies(cfg, roleId);
            if (matchedFamilies.isEmpty()) {
                orphans.add(roleId);
            } else if (matchedFamilies.size() > 1) {
                conflicts.add(new Conflict(roleId, matchedFamilies));
            }
        }

        ctx.sendMessage(Message.raw("[NpcAudit] roles=" + roleIds.size()
            + ", families=" + safeSize(cfg.families)));
        ctx.sendMessage(Message.raw("[NpcAudit] orphans=" + orphans.size()
            + ", conflicts=" + conflicts.size()));
        ctx.sendMessage(Message.raw("[NpcAudit] profileDistribution="
            + "{TANK=" + assignmentArchetypes.getOrDefault("TANK", 0)
            + ", DPS=" + assignmentArchetypes.getOrDefault("DPS", 0)
            + ", MAGE=" + assignmentArchetypes.getOrDefault("MAGE", 0)
            + ", BOSS=" + assignmentArchetypes.getOrDefault("BOSS", 0)
            + ", OTHER=" + assignmentArchetypes.getOrDefault("OTHER", 0) + "}"));

        if (!orphans.isEmpty()) {
            orphans.sort(String::compareToIgnoreCase);
            ctx.sendMessage(Message.raw("[NpcAudit] orphanPreview="
                + String.join(", ", orphans.subList(0, Math.min(PREVIEW_LIMIT, orphans.size())))));
        }

        if (!conflicts.isEmpty()) {
            conflicts.sort(Comparator.comparing(c -> c.roleId.toLowerCase(Locale.ROOT)));
            List<String> preview = new ArrayList<>();
            int limit = Math.min(PREVIEW_LIMIT, conflicts.size());
            for (int i = 0; i < limit; i++) {
                Conflict c = conflicts.get(i);
                preview.add(c.roleId + "->" + c.familyIds);
            }
            ctx.sendMessage(Message.raw("[NpcAudit] conflictPreview=" + String.join(" | ", preview)));
        }
    }

    private static Map<String, Integer> familyArchetypeDistribution(NpcFamiliesConfig cfg) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (cfg.families == null) {
            return out;
        }
        for (NpcFamiliesConfig.NpcFamilyDefinition family : cfg.families) {
            String key = normalizeArchetype(family == null ? null : family.archetype);
            out.put(key, out.getOrDefault(key, 0) + 1);
        }
        return out;
    }

    private static String normalizeArchetype(String archetype) {
        if (archetype == null || archetype.isBlank()) {
            return "OTHER";
        }
        String normalized = archetype.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TANK", "DPS", "MAGE", "BOSS" -> normalized;
            default -> "OTHER";
        };
    }

    private static List<String> matchedFamilies(NpcFamiliesConfig cfg, String roleId) {
        List<String> out = new ArrayList<>();
        if (cfg.families == null || roleId == null) {
            return out;
        }
        String lower = roleId.toLowerCase(Locale.ROOT);
        for (NpcFamiliesConfig.NpcFamilyDefinition family : cfg.families) {
            if (family == null || family.id == null) {
                continue;
            }
            if (NpcFamilyMatcher.matchesFamily(family, lower)) {
                out.add(family.id);
            }
        }
        return out;
    }

    private static List<String> scanValidRoleIds() {
        File root = new File(NPC_ROLES_ROOT);
        List<String> out = new ArrayList<>();
        if (!root.exists() || !root.isDirectory()) {
            return out;
        }
        scanRecursive(root, root, out);
        return out;
    }

    private static void scanRecursive(File root, File current, List<String> out) {
        File[] files = current.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanRecursive(root, file, out);
                continue;
            }
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                continue;
            }

            String relative = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
            String[] segments = relative.split("/");
            boolean excludedSegment = false;
            for (String segment : segments) {
                if (segment != null && ROLE_SEGMENT_EXCLUDES.contains(segment.toLowerCase(Locale.ROOT))) {
                    excludedSegment = true;
                    break;
                }
            }
            if (excludedSegment) {
                continue;
            }

            String baseName = stripJson(file.getName());
            String lower = baseName.toLowerCase(Locale.ROOT);
            boolean excludedPrefix = false;
            for (String prefix : FILE_PREFIX_EXCLUDES) {
                if (lower.startsWith(prefix)) {
                    excludedPrefix = true;
                    break;
                }
            }
            if (excludedPrefix) {
                continue;
            }
            out.add(baseName);
        }
    }

    private static String stripJson(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private record Conflict(String roleId, List<String> familyIds) {
    }
}
