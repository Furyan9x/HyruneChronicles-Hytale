package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.repair.ItemRarity;

/**
 * Migration shim for item instance metadata schema changes.
 */
public final class ItemInstanceMetadataMigration {
    private ItemInstanceMetadataMigration() {
    }

    public static ItemInstanceMetadata migrateToCurrent(ItemInstanceMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        if (metadata.getVersion() > ItemInstanceMetadata.CURRENT_SCHEMA_VERSION) {
            // Preserve unknown future versions as-is.
            return metadata;
        }
        metadata.setVersion(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION);

        if (metadata.getRarity() == null) {
            metadata.setRarity(ItemRarity.COMMON);
        }
        if (metadata.getPrefixRaw() == null || metadata.getPrefixRaw().isBlank()) {
            metadata.setPrefixRaw("");
        }
        if (metadata.getSource() == null) {
            metadata.setSource(ItemRollSource.CRAFTED);
        }
        if (metadata.getSocketCapacity() <= 0) {
            metadata.setSocketCapacity(GemSocketConfigHelper.socketsForRarity(metadata.getRarity()));
        }
        metadata.setSocketedGems(metadata.getSocketedGems());
        metadata.setStatFlatRollsRaw(metadata.getStatFlatRollsRaw());
        metadata.setStatPercentRollsRaw(metadata.getStatPercentRollsRaw());

        return metadata;
    }
}

