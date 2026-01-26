package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class FishingInteractionListener {
    private static final int MAX_CAST_DISTANCE = 6;
    private static final String MODEL_ASSET_ID = "OriginsFishingBobber";
    private static final String CAST_SOUND_ID = "SFX_Origins_Fishing_Rod_Cast";
    private static final String REEL_SOUND_ID = "SFX_Origins_Fishing_Rod_Reel";

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null || event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        ItemStack held = event.getItemInHand();
        if (!FishingRegistry.isFishingRod(held)) {
            return;
        }

        InteractionType action = event.getActionType();
        if (action != InteractionType.Use
                && action != InteractionType.Primary
                && action != InteractionType.Secondary) {
            return;
        }

        ItemStack heldHotbar = inventory.getActiveHotbarItem();
        FishingMetaData metaData = heldHotbar != null
            ? heldHotbar.getFromMetadataOrNull(FishingMetaData.KEY, FishingMetaData.CODEC)
            : null;
        if (metaData != null && metaData.getBoundBobber() != null) {
            handleReel(playerRef, inventory, metaData);
            return;
        }

        Vector3i target = event.getTargetBlock();
        if (target == null) {
            return;
        }

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }

        BlockType blockType = getBlockType(world, target);
        if (blockType == null || blockType.getId() == null) {
            return;
        }

        String blockId = blockType.getId().toLowerCase(Locale.ROOT);
        if (!blockId.contains("water")) {
            return;
        }

        if (!withinCastDistance(playerRef.getReference(), target)) {
            playerRef.sendMessage(Message.raw("That spot is too far away."));
            return;
        }

        Optional<FishingRegistry.BaitStack> baitOpt = FishingRegistry.findBait(inventory);
        if (baitOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("You need bait to fish."));
            return;
        }

        if (!consumeBait(inventory, baitOpt.get().itemId)) {
            playerRef.sendMessage(Message.raw("You need bait to fish."));
            return;
        }

        applyRodDurabilityLoss(player);

        LevelingService service = LevelingService.get();
        int level = service != null
                ? service.getSkillLevel(playerRef.getUuid(), SkillType.FISHING)
                : 1;

        Store<EntityStore> store = playerRef.getReference() != null ? playerRef.getReference().getStore() : null;
        if (store == null) {
            return;
        }

        UUID bobberId = spawnBobber(store, target, playerRef, baitOpt.get().definition, level);
        if (bobberId != null) {
            updateRodMetadata(inventory, bobberId);
            playCastSound(playerRef);
            playerRef.sendMessage(Message.raw("You cast your line."));
        }
    }

    private void handleReel(PlayerRef playerRef,
                            Inventory inventory,
                            FishingMetaData metaData) {
        Store<EntityStore> store = playerRef.getReference() != null ? playerRef.getReference().getStore() : null;
        if (store == null) {
            return;
        }

        UUID bobberId = metaData.getBoundBobber();
        if (bobberId == null) {
            updateRodMetadata(inventory, null);
            return;
        }

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            updateRodMetadata(inventory, null);
            return;
        }

        Ref<EntityStore> bobberRef = world.getEntityStore().getRefFromUUID(bobberId);
        if (bobberRef == null || !bobberRef.isValid()) {
            updateRodMetadata(inventory, null);
            return;
        }

        FishingBobberComponent bobber = store.getComponent(bobberRef, Origins.getFishingBobberComponentType());
        updateRodMetadata(inventory, null);
        playReelSound(playerRef);

        if (bobber == null || !bobber.canCatchFish()) {
            store.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            playerRef.sendMessage(Message.raw("Nothing is biting yet."));
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            store.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            return;
        }

        FishingRegistry.FishDefinition fish = FishingRegistry.rollFish(bobber.getFishingLevel(), bobber.getBait());
        if (fish == null) {
            store.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            playerRef.sendMessage(Message.raw("The fish got away."));
            return;
        }

        ItemStack caught = new ItemStack(fish.itemId, 1);
        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            var tx = storage.addItemStack(caught);
            ItemStack remainder = tx.getRemainder();
            if (remainder != null && remainder.getQuantity() > 0) {
                playerRef.sendMessage(Message.raw("Inventory full - fish dropped at your feet."));
                if (playerRef.getReference() != null) {
                    com.hypixel.hytale.server.core.entity.ItemUtils.dropItem(
                        playerRef.getReference(),
                        remainder,
                        playerRef.getReference().getStore()
                    );
                }
            }
        }

        service.addSkillXp(playerRef.getUuid(), SkillType.FISHING, fish.xp);
        playerRef.sendMessage(Message.raw("You caught a fish!"));
        store.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
    }

    private static boolean consumeBait(Inventory inventory, String itemId) {
        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return false;
        }

        ItemStack bait = new ItemStack(itemId, 1);
        ItemStackTransaction tx = container.removeItemStack(bait);
        return tx != null && tx.succeeded();
    }

    private static void applyRodDurabilityLoss(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        byte slot = inventory.getActiveHotbarSlot();
        if (slot < 0 || slot > 8) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return;
        }

        ItemStack current = hotbar.getItemStack(slot);
        if (current == null || current.isEmpty()) {
            return;
        }

        ItemStack updated = current.withIncreasedDurability(-1);
        hotbar.setItemStackForSlot((short) slot, updated);
    }

    private static UUID spawnBobber(Store<EntityStore> store,
                                    Vector3i target,
                                    PlayerRef playerRef,
                                    FishingRegistry.BaitDefinition bait,
                                    int fishingLevel) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        EntityStore entityStore = (EntityStore) store.getExternalData();

        Vector3d position = new Vector3d(target.x + 0.5, target.y + 0.2, target.z + 0.5);
        Vector3f rotation = new Vector3f(0f, 0f, 0f);

        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.ensureComponent(PhysicsValues.getComponentType());

        UUID uuid = UUID.randomUUID();
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
        holder.putComponent(NetworkId.getComponentType(), new NetworkId(entityStore.takeNextNetworkId()));

        ModelAsset asset = ModelAsset.getAssetMap().getAsset(MODEL_ASSET_ID);
        if (asset == null) {
            asset = ModelAsset.DEBUG;
        }
        Model model = Model.createRandomScaleModel(asset);

        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));

        if (model.getBoundingBox() != null) {
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
        }

        holder.addComponent(Velocity.getComponentType(), new Velocity());

        FishingBobberComponent bobber = new FishingBobberComponent();
        bobber.init(playerRef.getUuid(), bait, fishingLevel);
        holder.addComponent(Origins.getFishingBobberComponentType(), bobber);

        store.addEntity(holder, AddReason.SPAWN);
        return uuid;
    }

    private static void updateRodMetadata(Inventory inventory, UUID bobberId) {
        if (inventory == null) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return;
        }

        byte slot = inventory.getActiveHotbarSlot();
        if (slot < 0 || slot > 8) {
            return;
        }

        ItemStack current = hotbar.getItemStack(slot);
        if (current == null) {
            return;
        }

        ItemStack updated;
        if (bobberId == null) {
            updated = current.withMetadata(FishingMetaData.KEY, null);
        } else {
            FishingMetaData metaData = current.getFromMetadataOrNull(FishingMetaData.KEY, FishingMetaData.CODEC);
            if (metaData == null) {
                metaData = new FishingMetaData();
            }
            metaData.setBoundBobber(bobberId);
            updated = current.withMetadata(FishingMetaData.KEYED_CODEC, metaData);
        }

        hotbar.replaceItemStackInSlot((short) slot, current, updated);
    }

    private static void playCastSound(PlayerRef playerRef) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(CAST_SOUND_ID);
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.SFX);
    }

    private static void playReelSound(PlayerRef playerRef) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(REEL_SOUND_ID);
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.SFX);
    }

    private static boolean withinCastDistance(Ref<EntityStore> ref, Vector3i target) {
        if (ref == null || !ref.isValid()) {
            return false;
        }

        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return false;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return false;
        }

        Vector3d pos = transform.getPosition();
        double dx = pos.x - (target.x + 0.5);
        double dy = pos.y - (target.y + 0.5);
        double dz = pos.z - (target.z + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= (MAX_CAST_DISTANCE * MAX_CAST_DISTANCE);
    }

    private static BlockType getBlockType(World world, Vector3i target) {
        ChunkStore chunkStore = world.getChunkStore();
        var chunkRef = chunkStore.getChunkReference(com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(target.x, target.z));
        if (chunkRef == null || !chunkRef.isValid()) {
            return null;
        }

        WorldChunk worldChunk = chunkStore.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
            return null;
        }
        return worldChunk.getBlockType(target);
    }
}
