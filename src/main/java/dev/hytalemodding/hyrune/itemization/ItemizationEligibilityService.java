package dev.hytalemodding.hyrune.itemization;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import dev.hytalemodding.hyrune.config.HyruneConfig;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;

import java.util.List;
import java.util.Locale;

/**
 * Single source of truth for itemization inclusion/exclusion checks.
 */
public final class ItemizationEligibilityService {
    private ItemizationEligibilityService() {
    }

    public static boolean isEligible(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isEligibleItemId(stack.getItemId());
    }

    public static boolean isEligibleItemId(String itemId) {
        return isEligibleItemId(itemId, HyruneConfigManager.getConfig());
    }

    static boolean isEligibleItemId(String itemId, HyruneConfig config) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        if (config == null) {
            return false;
        }

        String normalizedId = itemId.toLowerCase(Locale.ROOT);
        List<String> excludedIds = config.itemizationExcludedIds == null ? List.of() : config.itemizationExcludedIds;
        for (String excludedId : excludedIds) {
            if (excludedId != null && normalizedId.equals(excludedId.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        List<String> excludedPrefixes = config.itemizationExcludedPrefixes == null ? List.of() : config.itemizationExcludedPrefixes;
        for (String excludedPrefix : excludedPrefixes) {
            if (excludedPrefix != null && normalizedId.startsWith(excludedPrefix.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        List<String> eligiblePrefixes = config.itemizationEligiblePrefixes == null ? List.of() : config.itemizationEligiblePrefixes;
        for (String eligiblePrefix : eligiblePrefixes) {
            if (eligiblePrefix != null && normalizedId.startsWith(eligiblePrefix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }
}
