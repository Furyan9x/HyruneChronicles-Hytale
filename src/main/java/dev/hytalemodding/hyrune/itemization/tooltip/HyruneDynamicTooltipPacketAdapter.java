package dev.hytalemodding.hyrune.itemization.tooltip;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.EquipmentUpdate;
import com.hypixel.hytale.protocol.InventorySection;
import com.hypixel.hytale.protocol.ItemBase;
import com.hypixel.hytale.protocol.ItemUpdate;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateItems;
import com.hypixel.hytale.protocol.packets.assets.UpdateTranslations;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.protocol.packets.inventory.UpdatePlayerInventory;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.protocol.packets.player.MouseInteraction;
import com.hypixel.hytale.protocol.packets.player.SetClientId;
import com.hypixel.hytale.protocol.packets.window.OpenWindow;
import com.hypixel.hytale.protocol.packets.window.UpdateWindow;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.hytalemodding.hyrune.config.HyruneConfigManager;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.BsonValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Packet-level adapter for dynamic metadata-driven tooltips.
 */
public final class HyruneDynamicTooltipPacketAdapter {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int POST_TRANSITION_REFRESH_DELAY_SECS = 2;

    private final HyruneVirtualItemRegistry virtualItemRegistry;
    private final HyruneDynamicTooltipComposer tooltipComposer;

    private PacketFilter outboundFilter;
    private PacketFilter inboundFilter;

    private final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);
    private final ConcurrentHashMap<UUID, Map<String, String>> lastSentTranslations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UpdatePlayerInventory> lastRawInventory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PlayerRef> knownPlayerRefs = new ConcurrentHashMap<>();
    private final Set<UUID> worldTransitioning = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Integer> playerEntityIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> playerActiveHotbarSlots = new ConcurrentHashMap<>();

    public HyruneDynamicTooltipPacketAdapter(HyruneVirtualItemRegistry virtualItemRegistry,
                                              HyruneDynamicTooltipComposer tooltipComposer) {
        this.virtualItemRegistry = virtualItemRegistry;
        this.tooltipComposer = tooltipComposer;
    }

    public void register() {
        this.outboundFilter = PacketAdapters.registerOutbound(this::onOutboundPacket);
        this.inboundFilter = PacketAdapters.registerInbound(this::onInboundPacket);
        logMapping("packet-adapter-registered");
    }

    public void deregister() {
        if (this.outboundFilter != null) {
            try {
                PacketAdapters.deregisterOutbound(this.outboundFilter);
            } catch (Exception ex) {
                LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed deregister outbound filter: " + ex.getMessage());
            }
            this.outboundFilter = null;
        }
        if (this.inboundFilter != null) {
            try {
                PacketAdapters.deregisterInbound(this.inboundFilter);
            } catch (Exception ex) {
                LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed deregister inbound filter: " + ex.getMessage());
            }
            this.inboundFilter = null;
        }
    }

    public void onPlayerLeave(UUID playerUuid) {
        worldTransitioning.remove(playerUuid);
        lastSentTranslations.remove(playerUuid);
        lastRawInventory.remove(playerUuid);
        knownPlayerRefs.remove(playerUuid);
        playerEntityIds.remove(playerUuid);
        playerActiveHotbarSlots.remove(playerUuid);
        virtualItemRegistry.onPlayerLeave(playerUuid);
    }

    public void invalidatePlayer(UUID playerUuid) {
        lastSentTranslations.remove(playerUuid);
        virtualItemRegistry.invalidatePlayer(playerUuid);
        logCache("invalidate-player=" + playerUuid);
    }

    public void invalidateAllPlayers() {
        lastSentTranslations.clear();
        for (UUID playerUuid : knownPlayerRefs.keySet()) {
            virtualItemRegistry.invalidatePlayer(playerUuid);
        }
        logCache("invalidate-all-players");
    }

    public boolean refreshPlayer(UUID playerUuid) {
        PlayerRef playerRef = knownPlayerRefs.get(playerUuid);
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        UpdatePlayerInventory raw = lastRawInventory.get(playerUuid);
        if (raw == null) {
            return false;
        }

        try {
            UpdatePlayerInventory clone = deepCloneInventory(raw);
            playerRef.getPacketHandler().writeNoCache(clone);
            logCache("refresh-player=" + playerUuid);
            return true;
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed refresh player=" + playerUuid + ": " + ex.getMessage());
            return false;
        }
    }

    public int refreshAllPlayers() {
        int refreshed = 0;
        for (UUID playerUuid : knownPlayerRefs.keySet()) {
            if (refreshPlayer(playerUuid)) {
                refreshed++;
            }
        }
        return refreshed;
    }

    private void schedulePostTransitionRefresh(UUID playerUuid) {
        try {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> refreshPlayer(playerUuid),
                POST_TRANSITION_REFRESH_DELAY_SECS,
                TimeUnit.SECONDS
            );
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed post-transition refresh for " + playerUuid + ": " + ex.getMessage());
        }
    }

    private boolean onInboundPacket(PlayerRef playerRef, Packet packet) {
        try {
            if (packet instanceof MouseInteraction mouseInteraction) {
                translateMouseInteraction(mouseInteraction);
            } else if (packet instanceof SyncInteractionChains chains) {
                translateInboundSyncInteractionChains(playerRef, chains);
            } else if (packet instanceof SetActiveSlot setActiveSlot) {
                playerActiveHotbarSlots.put(playerRef.getUuid(), setActiveSlot.activeSlot);
            }
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).log("[DynamicTooltip] Inbound translation failed for "
                + playerRef.getUuid() + ": " + ex.getMessage());
        }
        return false;
    }

    private void translateMouseInteraction(MouseInteraction interaction) {
        if (interaction.itemInHandId == null || !HyruneVirtualItemRegistry.isVirtualId(interaction.itemInHandId)) {
            return;
        }
        String baseId = HyruneVirtualItemRegistry.getBaseItemId(interaction.itemInHandId);
        if (baseId != null) {
            interaction.itemInHandId = baseId;
        }
    }

    private void translateInboundSyncInteractionChains(PlayerRef playerRef, SyncInteractionChains chains) {
        if (chains.updates == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        for (SyncInteractionChain chain : chains.updates) {
            if (chain == null) {
                continue;
            }
            if (chain.activeHotbarSlot >= 0) {
                playerActiveHotbarSlots.put(playerUuid, chain.activeHotbarSlot);
            }
            if (chain.data != null && chain.data.targetSlot >= 0) {
                playerActiveHotbarSlots.put(playerUuid, chain.data.targetSlot);
            }
            translateInboundChainItemIds(chain);
        }
    }

    private void translateInboundChainItemIds(SyncInteractionChain chain) {
        chain.itemInHandId = translateInboundId(chain.itemInHandId);
        chain.utilityItemId = translateInboundId(chain.utilityItemId);
        chain.toolsItemId = translateInboundId(chain.toolsItemId);
        if (chain.newForks == null) {
            return;
        }
        for (SyncInteractionChain fork : chain.newForks) {
            if (fork != null) {
                translateInboundChainItemIds(fork);
            }
        }
    }

    private String translateInboundId(String itemId) {
        if (itemId == null || !HyruneVirtualItemRegistry.isVirtualId(itemId)) {
            return itemId;
        }
        String baseId = HyruneVirtualItemRegistry.getBaseItemId(itemId);
        return baseId != null ? baseId : itemId;
    }

    private boolean onOutboundPacket(PlayerRef playerRef, Packet packet) {
        if (isProcessing.get()) {
            return false;
        }

        isProcessing.set(true);
        try {
            UUID playerUuid = playerRef.getUuid();
            knownPlayerRefs.put(playerUuid, playerRef);

            if (packet instanceof JoinWorld) {
                worldTransitioning.add(playerUuid);
            } else if (packet instanceof SetClientId setClientId) {
                playerEntityIds.put(playerUuid, setClientId.clientId);
            } else if (packet instanceof EntityUpdates entityUpdates) {
                processEntityUpdates(playerRef, entityUpdates);
            } else if (packet instanceof UpdatePlayerInventory inventoryPacket) {
                lastRawInventory.put(playerUuid, deepCloneInventory(inventoryPacket));
                if (worldTransitioning.remove(playerUuid)) {
                    schedulePostTransitionRefresh(playerUuid);
                } else {
                    processPlayerInventory(playerRef, inventoryPacket);
                }
            } else if (packet instanceof OpenWindow openWindow) {
                processWindowInventory(playerRef, openWindow.inventory);
            } else if (packet instanceof UpdateWindow updateWindow) {
                processWindowInventory(playerRef, updateWindow.inventory);
            } else if (packet instanceof CustomPage customPage) {
                processCustomPage(playerRef, customPage);
            }
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).log("[DynamicTooltip] Outbound processing failed for "
                + playerRef.getUuid() + ": " + ex.getMessage());
        } finally {
            isProcessing.set(false);
        }
        return false;
    }

    private void processPlayerInventory(PlayerRef playerRef, UpdatePlayerInventory inventoryPacket) {
        UUID playerUuid = playerRef.getUuid();
        String language = playerRef.getLanguage();

        Map<String, ItemBase> virtualItems = new LinkedHashMap<>();
        Map<String, String> translations = new LinkedHashMap<>();

        processSection(playerUuid, "hotbar", inventoryPacket.hotbar, language, virtualItems, translations);
        processSection(playerUuid, "utility", inventoryPacket.utility, language, virtualItems, translations);
        processSection(playerUuid, "tools", inventoryPacket.tools, language, virtualItems, translations);
        processSection(playerUuid, "armor", inventoryPacket.armor, language, virtualItems, translations);
        processSection(playerUuid, "storage", inventoryPacket.storage, language, virtualItems, translations);
        processSection(playerUuid, "backpack", inventoryPacket.backpack, language, virtualItems, translations);
        processSection(playerUuid, "builderMaterial", inventoryPacket.builderMaterial, language, virtualItems, translations);

        sendAuxiliaryPackets(playerRef, virtualItems, translations);
    }

    private void processWindowInventory(PlayerRef playerRef, InventorySection inventorySection) {
        if (inventorySection == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        String language = playerRef.getLanguage();

        Map<String, ItemBase> virtualItems = new LinkedHashMap<>();
        Map<String, String> translations = new LinkedHashMap<>();

        processSection(playerUuid, null, inventorySection, language, virtualItems, translations);
        sendAuxiliaryPackets(playerRef, virtualItems, translations);
    }

    private void processCustomPage(PlayerRef playerRef, CustomPage customPage) {
        if (customPage.commands == null || customPage.commands.length == 0) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        String language = playerRef.getLanguage();
        Map<String, ItemBase> virtualItems = new LinkedHashMap<>();
        Map<String, String> translations = new LinkedHashMap<>();

        for (CustomUICommand command : customPage.commands) {
            if (command == null || command.data == null || command.data.isEmpty()) {
                continue;
            }
            String updated = processCustomUICommandData(playerUuid, language, command.data, virtualItems, translations);
            if (updated != null) {
                command.data = updated;
            }
        }

        sendAuxiliaryPackets(playerRef, virtualItems, translations);
    }

    @Nullable
    private String processCustomUICommandData(UUID playerUuid,
                                              String language,
                                              String rawData,
                                              Map<String, ItemBase> virtualItems,
                                              Map<String, String> translations) {
        try {
            BsonDocument document = BsonDocument.parse(rawData);
            boolean[] changed = new boolean[]{false};
            rewriteBsonDocument(playerUuid, language, document, virtualItems, translations, changed);
            if (!changed[0]) {
                return null;
            }
            return document.toJson();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void rewriteBsonDocument(UUID playerUuid,
                                     String language,
                                     BsonDocument document,
                                     Map<String, ItemBase> virtualItems,
                                     Map<String, String> translations,
                                     boolean[] changed) {
        ArrayList<String> keys = new ArrayList<>(document.keySet());
        for (String key : keys) {
            BsonValue value = document.get(key);
            BsonValue rewritten = rewriteBsonValue(playerUuid, language, value, virtualItems, translations, changed);
            if (rewritten != value) {
                document.put(key, rewritten);
            }
        }
    }

    private void rewriteBsonArray(UUID playerUuid,
                                  String language,
                                  BsonArray array,
                                  Map<String, ItemBase> virtualItems,
                                  Map<String, String> translations,
                                  boolean[] changed) {
        for (int i = 0; i < array.size(); i++) {
            BsonValue value = array.get(i);
            BsonValue rewritten = rewriteBsonValue(playerUuid, language, value, virtualItems, translations, changed);
            if (rewritten != value) {
                array.set(i, rewritten);
            }
        }
    }

    private BsonValue rewriteBsonValue(UUID playerUuid,
                                       String language,
                                       BsonValue value,
                                       Map<String, ItemBase> virtualItems,
                                       Map<String, String> translations,
                                       boolean[] changed) {
        if (value == null) {
            return value;
        }

        BsonType type = value.getBsonType();
        if (type == BsonType.DOCUMENT) {
            rewriteBsonDocument(playerUuid, language, value.asDocument(), virtualItems, translations, changed);
            return value;
        }
        if (type == BsonType.ARRAY) {
            rewriteBsonArray(playerUuid, language, value.asArray(), virtualItems, translations, changed);
            return value;
        }
        if (type != BsonType.STRING) {
            return value;
        }

        String raw = value.asString().getValue();
        if (raw == null || raw.isBlank() || HyruneVirtualItemRegistry.isVirtualId(raw)) {
            return value;
        }

        String virtualId = findVirtualIdForItem(playerUuid, raw, language, virtualItems, translations);
        if (virtualId == null) {
            return value;
        }
        changed[0] = true;
        return new BsonString(virtualId);
    }

    @Nullable
    private String findVirtualIdForItem(UUID playerUuid,
                                        String baseItemId,
                                        String language,
                                        Map<String, ItemBase> virtualItems,
                                        Map<String, String> translations) {
        if (baseItemId == null || baseItemId.isBlank() || HyruneVirtualItemRegistry.isVirtualId(baseItemId)) {
            return null;
        }

        String mapped = virtualItemRegistry.findVirtualIdForBaseItem(playerUuid, baseItemId);
        if (mapped == null) {
            return null;
        }

        if (virtualItems.containsKey(mapped)) {
            return mapped;
        }

        ItemBase virtualBase = virtualItemRegistry.getOrCreateVirtualItemBase(baseItemId, mapped, null);
        if (virtualBase != null) {
            virtualItems.put(mapped, virtualBase);
            String baseDescription = virtualItemRegistry.getOriginalDescription(baseItemId, language);
            translations.put(HyruneVirtualItemRegistry.getVirtualDescriptionKey(mapped), baseDescription);
            String baseName = virtualItemRegistry.getOriginalName(baseItemId, language);
            translations.put(HyruneVirtualItemRegistry.getVirtualNameKey(mapped), baseName);
        }
        return mapped;
    }

    private void processEntityUpdates(PlayerRef playerRef, EntityUpdates entityUpdates) {
        if (entityUpdates.updates == null || entityUpdates.updates.length == 0) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        String language = playerRef.getLanguage();
        Map<String, ItemBase> virtualItems = new LinkedHashMap<>();
        Map<String, String> translations = new LinkedHashMap<>();
        Integer localEntityId = playerEntityIds.get(playerRef.getUuid());

        for (EntityUpdate entityUpdate : entityUpdates.updates) {
            if (entityUpdate == null || entityUpdate.updates == null) {
                continue;
            }

            boolean isLocalPlayer = localEntityId != null && entityUpdate.networkId == localEntityId;
            for (ComponentUpdate update : entityUpdate.updates) {
                if (isLocalPlayer && update instanceof EquipmentUpdate equipmentUpdate) {
                    processEquipmentUpdate(playerRef, equipmentUpdate);
                    continue;
                }
                if (update instanceof ItemUpdate itemUpdate) {
                    processEntityItemUpdate(playerUuid, language, itemUpdate, virtualItems, translations);
                }
            }
        }

        sendAuxiliaryPackets(playerRef, virtualItems, translations);
    }

    private void processEntityItemUpdate(UUID playerUuid,
                                         String language,
                                         ItemUpdate itemUpdate,
                                         Map<String, ItemBase> virtualItems,
                                         Map<String, String> translations) {
        if (itemUpdate.item == null || itemUpdate.item.itemId == null || itemUpdate.item.itemId.isBlank()) {
            return;
        }
        if (HyruneVirtualItemRegistry.isVirtualId(itemUpdate.item.itemId)) {
            return;
        }

        HyruneDynamicTooltipComposer.ComposedTooltip composed =
            tooltipComposer.compose(playerUuid, itemUpdate.item.itemId, itemUpdate.item.metadata);
        if (composed == null) {
            return;
        }

        String virtualId = virtualItemRegistry.generateVirtualId(itemUpdate.item.itemId, composed.getCombinedHash());
        ItemBase virtualBase = virtualItemRegistry.getOrCreateVirtualItemBase(itemUpdate.item.itemId, virtualId, composed.getRarity());
        if (virtualBase == null) {
            return;
        }

        virtualItems.put(virtualId, virtualBase);

        String baseDescription = virtualItemRegistry.getOriginalDescription(itemUpdate.item.itemId, language);
        String fullDescription = composed.buildDescription(baseDescription);
        translations.put(HyruneVirtualItemRegistry.getVirtualDescriptionKey(virtualId), fullDescription);
        String baseName = virtualItemRegistry.getOriginalName(itemUpdate.item.itemId, language);
        String resolvedName = composed.getDisplayNameOverride() == null ? baseName : composed.getDisplayNameOverride();
        translations.put(HyruneVirtualItemRegistry.getVirtualNameKey(virtualId), resolvedName);

        ItemWithAllMetadata clone = itemUpdate.item.clone();
        clone.itemId = virtualId;
        itemUpdate.item = clone;
    }

    private void processEquipmentUpdate(PlayerRef playerRef, EquipmentUpdate equipment) {
        UUID playerUuid = playerRef.getUuid();

        if (equipment.rightHandItemId != null && !HyruneVirtualItemRegistry.isVirtualId(equipment.rightHandItemId)) {
            int activeSlot = playerActiveHotbarSlots.getOrDefault(playerUuid, 0);
            String virtualId = virtualItemRegistry.getSlotVirtualId(playerUuid, "hotbar:" + activeSlot);
            if (virtualId != null) {
                String baseId = HyruneVirtualItemRegistry.getBaseItemId(virtualId);
                if (baseId != null && baseId.equals(equipment.rightHandItemId)) {
                    equipment.rightHandItemId = virtualId;
                }
            }
        }

        if (equipment.leftHandItemId != null && !HyruneVirtualItemRegistry.isVirtualId(equipment.leftHandItemId)) {
            String virtualId = virtualItemRegistry.getSlotVirtualId(playerUuid, "utility:0");
            if (virtualId != null) {
                String baseId = HyruneVirtualItemRegistry.getBaseItemId(virtualId);
                if (baseId != null && baseId.equals(equipment.leftHandItemId)) {
                    equipment.leftHandItemId = virtualId;
                }
            }
        }

        if (equipment.armorIds != null) {
            for (int i = 0; i < equipment.armorIds.length; i++) {
                String armorId = equipment.armorIds[i];
                if (armorId == null || HyruneVirtualItemRegistry.isVirtualId(armorId)) {
                    continue;
                }
                String virtualId = virtualItemRegistry.getSlotVirtualId(playerUuid, "armor:" + i);
                if (virtualId == null) {
                    continue;
                }
                String baseId = HyruneVirtualItemRegistry.getBaseItemId(virtualId);
                if (baseId != null && baseId.equals(armorId)) {
                    equipment.armorIds[i] = virtualId;
                }
            }
        }
    }

    private void processSection(UUID playerUuid,
                                @Nullable String sectionName,
                                InventorySection inventorySection,
                                String language,
                                Map<String, ItemBase> virtualItems,
                                Map<String, String> translations) {
        if (inventorySection == null || inventorySection.items == null || inventorySection.items.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, ItemWithAllMetadata> entry : inventorySection.items.entrySet()) {
            Integer slot = entry.getKey();
            ItemWithAllMetadata item = entry.getValue();
            String slotKey = sectionName != null ? sectionName + ":" + slot : null;

            if (item == null || item.itemId == null || item.itemId.isEmpty()) {
                if (slotKey != null) {
                    virtualItemRegistry.trackSlotVirtualId(playerUuid, slotKey, null);
                }
                continue;
            }

            if (HyruneVirtualItemRegistry.isVirtualId(item.itemId)) {
                if (slotKey != null) {
                    virtualItemRegistry.trackSlotVirtualId(playerUuid, slotKey, item.itemId);
                }
                continue;
            }

            HyruneDynamicTooltipComposer.ComposedTooltip composed = tooltipComposer.compose(playerUuid, item.itemId, item.metadata);
            if (composed == null) {
                if (slotKey != null) {
                    virtualItemRegistry.trackSlotVirtualId(playerUuid, slotKey, null);
                }
                continue;
            }

            String virtualId = virtualItemRegistry.generateVirtualId(item.itemId, composed.getCombinedHash());
            ItemBase virtualBase = virtualItemRegistry.getOrCreateVirtualItemBase(item.itemId, virtualId, composed.getRarity());
            if (virtualBase == null) {
                if (slotKey != null) {
                    virtualItemRegistry.trackSlotVirtualId(playerUuid, slotKey, null);
                }
                continue;
            }

            virtualItems.put(virtualId, virtualBase);

            String baseDescription = virtualItemRegistry.getOriginalDescription(item.itemId, language);
            String fullDescription = composed.buildDescription(baseDescription);
            translations.put(HyruneVirtualItemRegistry.getVirtualDescriptionKey(virtualId), fullDescription);
            String baseName = virtualItemRegistry.getOriginalName(item.itemId, language);
            String resolvedName = composed.getDisplayNameOverride() == null ? baseName : composed.getDisplayNameOverride();
            translations.put(HyruneVirtualItemRegistry.getVirtualNameKey(virtualId), resolvedName);

            ItemWithAllMetadata clone = item.clone();
            clone.itemId = virtualId;
            inventorySection.items.put(slot, clone);

            if (slotKey != null) {
                virtualItemRegistry.trackSlotVirtualId(playerUuid, slotKey, virtualId);
            }
            //logMapping("mapped " + item.itemId + " -> " + virtualId + " slot=" + slotKey);
        }
    }

    private void sendAuxiliaryPackets(PlayerRef playerRef,
                                      Map<String, ItemBase> virtualItems,
                                      Map<String, String> translations) {
        if (virtualItems.isEmpty() && translations.isEmpty()) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        Set<String> unsentVirtualIds = virtualItemRegistry.markAndGetUnsent(playerUuid, virtualItems.keySet());
        if (!unsentVirtualIds.isEmpty()) {
            Map<String, ItemBase> unsentItems = new LinkedHashMap<>();
            for (String virtualId : unsentVirtualIds) {
                ItemBase base = virtualItems.get(virtualId);
                if (base != null) {
                    unsentItems.put(virtualId, base);
                }
            }
            if (!unsentItems.isEmpty()) {
                sendUpdateItems(playerRef, unsentItems);
            }
        }

        if (!translations.isEmpty()) {
            Map<String, String> previous = lastSentTranslations.get(playerUuid);
            Map<String, String> delta = computeTranslationDelta(previous, translations);
            if (!delta.isEmpty()) {
                sendTranslations(playerRef, delta);
                if (previous == null) {
                    lastSentTranslations.put(playerUuid, new ConcurrentHashMap<>(delta));
                } else {
                    previous.putAll(delta);
                }
            }
        }
    }

    private void sendUpdateItems(PlayerRef playerRef, Map<String, ItemBase> items) {
        try {
            UpdateItems updateItems = new UpdateItems();
            updateItems.type = UpdateType.AddOrUpdate;
            updateItems.items = items;
            updateItems.removedItems = new String[0];
            updateItems.updateModels = false;
            updateItems.updateIcons = false;
            playerRef.getPacketHandler().writeNoCache(updateItems);
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed send UpdateItems: " + ex.getMessage());
        }
    }

    private void sendTranslations(PlayerRef playerRef, Map<String, String> translations) {
        try {
            UpdateTranslations updateTranslations = new UpdateTranslations(UpdateType.AddOrUpdate, translations);
            playerRef.getPacketHandler().writeNoCache(updateTranslations);
        } catch (Exception ex) {
            LOGGER.at(Level.WARNING).log("[DynamicTooltip] Failed send UpdateTranslations: " + ex.getMessage());
        }
    }

    private Map<String, String> computeTranslationDelta(@Nullable Map<String, String> previous,
                                                        Map<String, String> current) {
        if (previous == null || previous.isEmpty()) {
            return current;
        }

        Map<String, String> delta = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : current.entrySet()) {
            String oldValue = previous.get(entry.getKey());
            if (!entry.getValue().equals(oldValue)) {
                delta.put(entry.getKey(), entry.getValue());
            }
        }
        return delta;
    }

    private static UpdatePlayerInventory deepCloneInventory(UpdatePlayerInventory inventory) {
        UpdatePlayerInventory clone = new UpdatePlayerInventory();
        clone.hotbar = cloneSection(inventory.hotbar);
        clone.utility = cloneSection(inventory.utility);
        clone.tools = cloneSection(inventory.tools);
        clone.armor = cloneSection(inventory.armor);
        clone.storage = cloneSection(inventory.storage);
        clone.backpack = cloneSection(inventory.backpack);
        clone.builderMaterial = cloneSection(inventory.builderMaterial);
        clone.sortType = inventory.sortType;
        return clone;
    }

    private static InventorySection cloneSection(InventorySection source) {
        if (source == null) {
            return null;
        }

        InventorySection clone = new InventorySection();
        clone.capacity = source.capacity;
        if (source.items != null) {
            clone.items = new HashMap<>();
            for (Map.Entry<Integer, ItemWithAllMetadata> entry : source.items.entrySet()) {
                clone.items.put(entry.getKey(), entry.getValue() != null ? entry.getValue().clone() : null);
            }
        }
        return clone;
    }

    private static void logMapping(String message) {
        if (HyruneConfigManager.getConfig().dynamicTooltipMappingDebug) {
            LOGGER.at(Level.INFO).log("[DynamicTooltip] " + message);
        }
    }

    private static void logCache(String message) {
        if (HyruneConfigManager.getConfig().dynamicTooltipCacheDebug) {
            LOGGER.at(Level.INFO).log("[DynamicTooltip] " + message);
        }
    }
}
