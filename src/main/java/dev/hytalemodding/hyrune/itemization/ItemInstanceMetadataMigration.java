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

        if (metadata.getVersion() <= 0) {
            // Legacy instances had no explicit version marker.
            metadata.setVersion(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION);
        } else if (metadata.getVersion() > ItemInstanceMetadata.CURRENT_SCHEMA_VERSION) {
            // Preserve unknown future versions as-is.
            return metadata;
        }

        if (metadata.getRarity() == null) {
            metadata.setRarity(ItemRarity.COMMON);
        }
        if (metadata.getCatalyst() == null) {
            metadata.setCatalyst(CatalystAffinity.NONE);
        }
        if (metadata.getSource() == null) {
            metadata.setSource(ItemRollSource.CRAFTED);
        }

        return metadata;
    }
}
