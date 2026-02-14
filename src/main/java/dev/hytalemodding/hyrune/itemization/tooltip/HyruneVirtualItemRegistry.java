package dev.hytalemodding.hyrune.itemization.tooltip;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemBase;
import com.hypixel.hytale.protocol.ItemTranslationProperties;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import dev.hytalemodding.hyrune.repair.ItemRarity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Registry and cache for virtual item ids and player-specific sent state.
 */
public final class HyruneVirtualItemRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String VIRTUAL_SEPARATOR = "__hyrunedtt_";
    private static final String DESCRIPTION_KEY_PREFIX = "server.tooltip.dynamic.description.";

    private final Map<String, ItemBase> virtualItemCache =
        Collections.synchronizedMap(new LruCache<>(10_000));

    private final ConcurrentHashMap<UUID, Set<String>> sentToPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> playerSlotVirtualIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> originalDescriptionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> translatedNameCache = new ConcurrentHashMap<>();
    private final EnumMap<ItemRarity, Integer> qualityIndexByRarity = new EnumMap<>(ItemRarity.class);
    private volatile int cachedQualityCount = -1;
    private final Object qualityLock = new Object();

    public String generateVirtualId(String baseItemId, String hash) {
        return baseItemId + VIRTUAL_SEPARATOR + hash;
    }

    public static boolean isVirtualId(String itemId) {
        return itemId != null && itemId.contains(VIRTUAL_SEPARATOR);
    }

    public static String getBaseItemId(String virtualId) {
        if (virtualId == null) {
            return null;
        }
        int index = virtualId.indexOf(VIRTUAL_SEPARATOR);
        if (index <= 0) {
            return null;
        }
        return virtualId.substring(0, index);
    }

    public static String getVirtualDescriptionKey(String virtualId) {
        return DESCRIPTION_KEY_PREFIX + virtualId;
    }

    public ItemBase getOrCreateVirtualItemBase(String baseItemId, String virtualId, ItemRarity rarity) {
        return virtualItemCache.computeIfAbsent(virtualId, key -> buildVirtualItem(baseItemId, virtualId, rarity));
    }

    public Set<String> markAndGetUnsent(UUID playerUuid, Set<String> virtualIds) {
        Set<String> sent = sentToPlayer.computeIfAbsent(playerUuid, ignored -> ConcurrentHashMap.newKeySet());
        Set<String> unsent = new HashSet<>();
        for (String id : virtualIds) {
            if (sent.add(id)) {
                unsent.add(id);
            }
        }
        return unsent;
    }

    public void trackSlotVirtualId(UUID playerUuid, String slotKey, String virtualId) {
        ConcurrentHashMap<String, String> slots =
            playerSlotVirtualIds.computeIfAbsent(playerUuid, ignored -> new ConcurrentHashMap<>());
        if (virtualId == null) {
            slots.remove(slotKey);
            return;
        }
        slots.put(slotKey, virtualId);
    }

    public String getSlotVirtualId(UUID playerUuid, String slotKey) {
        Map<String, String> slots = playerSlotVirtualIds.get(playerUuid);
        return slots != null ? slots.get(slotKey) : null;
    }

    public String findVirtualIdForBaseItem(UUID playerUuid, String baseItemId) {
        Map<String, String> slots = playerSlotVirtualIds.get(playerUuid);
        if (slots == null || slots.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, String> entry : slots.entrySet()) {
            if (!entry.getKey().startsWith("hotbar:")) {
                continue;
            }
            String candidateBase = getBaseItemId(entry.getValue());
            if (baseItemId.equals(candidateBase)) {
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : slots.entrySet()) {
            String candidateBase = getBaseItemId(entry.getValue());
            if (baseItemId.equals(candidateBase)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String getOriginalDescription(String itemId, String language) {
        String safeLanguage = language == null || language.isBlank() ? I18nModule.DEFAULT_LANGUAGE : language;
        String cacheKey = safeLanguage + "|" + itemId;
        return originalDescriptionCache.computeIfAbsent(cacheKey, ignored -> resolveOriginalDescription(itemId, safeLanguage));
    }

    public void onPlayerLeave(UUID playerUuid) {
        sentToPlayer.remove(playerUuid);
        playerSlotVirtualIds.remove(playerUuid);
    }

    public void invalidatePlayer(UUID playerUuid) {
        sentToPlayer.remove(playerUuid);
        playerSlotVirtualIds.remove(playerUuid);
    }

    public void clearCache() {
        virtualItemCache.clear();
        sentToPlayer.clear();
        playerSlotVirtualIds.clear();
        originalDescriptionCache.clear();
        translatedNameCache.clear();
        synchronized (qualityLock) {
            qualityIndexByRarity.clear();
            cachedQualityCount = -1;
        }
    }

    private ItemBase buildVirtualItem(String baseItemId, String virtualId, ItemRarity rarity) {
        try {
            DefaultAssetMap<String, Item> itemAssetMap = Item.getAssetMap();
            Item baseItem = (Item) itemAssetMap.getAsset(baseItemId);
            if (baseItem == null || baseItem == Item.UNKNOWN) {
                return null;
            }

            ItemBase packet = baseItem.toPacket();
            if (packet == null) {
                return null;
            }

            ItemBase out = packet.clone();
            out.id = virtualId;
            out.variant = true;
            out.qualityIndex = resolveQualityIndex(rarity, packet.qualityIndex);

            if (out.translationProperties != null) {
                out.translationProperties = out.translationProperties.clone();
            } else {
                out.translationProperties = new ItemTranslationProperties();
            }
            out.translationProperties.description = getVirtualDescriptionKey(virtualId);
            return out;
        } catch (Exception ex) {
            if (HyruneConfigManager.getConfig().dynamicTooltipMappingDebug) {
                LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed creating virtual base for " + baseItemId + ": " + ex.getMessage());
            }
            return null;
        }
    }

    private int resolveQualityIndex(ItemRarity rarity, int fallbackIndex) {
        ItemRarity target = rarity == null ? ItemRarity.COMMON : rarity;
        Integer mapped = getMappedQualityIndex(target);
        if (mapped != null) {
            return mapped;
        }
        if (target == ItemRarity.VOCATIONAL) {
            Integer epicMapped = getMappedQualityIndex(ItemRarity.EPIC);
            if (epicMapped != null) {
                return epicMapped;
            }
        }
        return fallbackIndex;
    }

    private Integer getMappedQualityIndex(ItemRarity rarity) {
        rebuildQualityIndexMapIfNeeded();
        synchronized (qualityLock) {
            return qualityIndexByRarity.get(rarity);
        }
    }

    private void rebuildQualityIndexMapIfNeeded() {
        IndexedLookupTableAssetMap<String, ItemQuality> qualityMap = ItemQuality.getAssetMap();
        int qualityCount = qualityMap != null ? qualityMap.getAssetCount() : 0;
        if (qualityCount <= 0) {
            return;
        }
        if (qualityCount == cachedQualityCount && !qualityIndexByRarity.isEmpty()) {
            return;
        }

        synchronized (qualityLock) {
            if (qualityCount == cachedQualityCount && !qualityIndexByRarity.isEmpty()) {
                return;
            }

            EnumMap<ItemRarity, Integer> rebuilt = new EnumMap<>(ItemRarity.class);
            Map<String, ItemQuality> all = qualityMap.getAssetMap();
            if (all != null && !all.isEmpty()) {
                for (Map.Entry<String, ItemQuality> entry : all.entrySet()) {
                    String qualityId = entry.getKey();
                    ItemQuality quality = entry.getValue();
                    if (qualityId == null || qualityId.isBlank() || quality == null) {
                        continue;
                    }
                    int idx = qualityMap.getIndex(qualityId);
                    if (idx < 0) {
                        continue;
                    }
                    ItemRarity mappedRarity = matchRarity(qualityId, quality.getLocalizationKey());
                    if (mappedRarity != null) {
                        rebuilt.putIfAbsent(mappedRarity, idx);
                    }
                }
            }

            qualityIndexByRarity.clear();
            qualityIndexByRarity.putAll(rebuilt);
            cachedQualityCount = qualityCount;

            if (HyruneConfigManager.getConfig().dynamicTooltipMappingDebug) {
                LOGGER.at(Level.INFO).log("[DynamicTooltip] quality-map " + qualityIndexByRarity);
            }
        }
    }

    private static ItemRarity matchRarity(String qualityId, String localizationKey) {
        String id = qualityId.toLowerCase(Locale.ROOT);
        String loc = localizationKey == null ? "" : localizationKey.toLowerCase(Locale.ROOT);
        if (containsWord(id, "uncommon") || containsWord(loc, "uncommon")) {
            return ItemRarity.UNCOMMON;
        }
        if (containsWord(id, "common") || containsWord(loc, "common")) {
            return ItemRarity.COMMON;
        }
        if (containsWord(id, "legendary") || containsWord(loc, "legendary")) {
            return ItemRarity.LEGENDARY;
        }
        if (containsWord(id, "mythic") || containsWord(loc, "mythic")) {
            return ItemRarity.MYTHIC;
        }
        if (containsWord(id, "vocational") || containsWord(loc, "vocational")) {
            return ItemRarity.VOCATIONAL;
        }
        if (containsWord(id, "epic") || containsWord(loc, "epic")) {
            return ItemRarity.EPIC;
        }
        if (containsWord(id, "rare") || containsWord(loc, "rare")) {
            return ItemRarity.RARE;
        }
        return null;
    }

    private static boolean containsWord(String value, String word) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.replace('-', '_').replace('.', '_');
        String[] tokens = normalized.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (word.equals(token)) {
                return true;
            }
        }
        if ("common".equals(word)) {
            return normalized.contains("common") && !normalized.contains("uncommon");
        }
        return normalized.contains(word);
    }

    private String resolveOriginalDescription(String itemId, String language) {
        String description = resolveLocalizedDescription(itemId, language);
        if (description != null && !description.isBlank()) {
            return description;
        }

        String displayName = resolveLocalizedName(itemId, language);
        if (displayName == null || displayName.isBlank()) {
            displayName = humanizeItemId(itemId);
        }
        return defaultSentence(displayName);
    }

    private static String resolveLocalizedDescription(String itemId, String language) {
        try {
            Item item = (Item) Item.getAssetMap().getAsset(itemId);
            if (item == null || item == Item.UNKNOWN) {
                return "";
            }

            String descriptionKey = item.getDescriptionTranslationKey();
            if (descriptionKey == null || descriptionKey.isBlank()) {
                return "";
            }

            I18nModule i18n = I18nModule.get();
            if (i18n == null) {
                return "";
            }

            String text = i18n.getMessage(language, descriptionKey);
            if (text == null || text.isBlank() || descriptionKey.equals(text)) {
                return "";
            }
            return text;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String resolveLocalizedName(String itemId, String language) {
        String cacheKey = language + "|" + itemId;
        return translatedNameCache.computeIfAbsent(cacheKey, ignored -> {
            try {
                Item item = (Item) Item.getAssetMap().getAsset(itemId);
                if (item == null || item == Item.UNKNOWN) {
                    return "";
                }

                String nameKey = item.getTranslationKey();
                if (nameKey == null || nameKey.isBlank()) {
                    return "";
                }

                I18nModule i18n = I18nModule.get();
                if (i18n == null) {
                    return "";
                }

                String localized = i18n.getMessage(language, nameKey);
                if (localized == null || localized.isBlank() || nameKey.equals(localized)) {
                    return "";
                }
                return localized;
            } catch (Exception ignoredEx) {
                return "";
            }
        });
    }

    private static String defaultSentence(String displayName) {
        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.isBlank()) {
            return "This is an item.";
        }
        String first = trimmed.substring(0, 1).toLowerCase(Locale.ROOT);
        boolean vowel = first.equals("a") || first.equals("e") || first.equals("i") || first.equals("o") || first.equals("u");
        String article = vowel ? "an" : "a";
        return "This is " + article + " " + trimmed + ".";
    }

    private static String humanizeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "item";
        }

        String[] raw = itemId.split("_");
        if (raw.length == 0) {
            return itemId;
        }

        int start = 0;
        String first = raw[0].toLowerCase(Locale.ROOT);
        if (first.equals("weapon") || first.equals("armor") || first.equals("tool") || first.equals("utility") || first.equals("food") || first.equals("item")) {
            start = 1;
        }

        List<String> words = new java.util.ArrayList<>();
        for (int i = start; i < raw.length; i++) {
            String token = raw[i].trim();
            if (!token.isBlank()) {
                words.add(token.toLowerCase(Locale.ROOT));
            }
        }
        if (words.isEmpty()) {
            words.add(itemId.toLowerCase(Locale.ROOT));
        }

        if (words.size() == 2 && isLikelyAdjective(words.get(1))) {
            Collections.swap(words, 0, 1);
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                out.append(' ');
            }
            String word = words.get(i);
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static boolean isLikelyAdjective(String token) {
        return switch (token) {
            case "crude", "wood", "wooden", "bronze", "iron", "steel", "silversteel", "mythic",
                "legendary", "epic", "rare", "uncommon", "common", "adamantite", "onyxium", "runic",
                "frost", "bone", "tribal", "doomed", "ancient" -> true;
            default -> false;
        };
    }

    private static final class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        private LruCache(int maxSize) {
            super(256, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}
