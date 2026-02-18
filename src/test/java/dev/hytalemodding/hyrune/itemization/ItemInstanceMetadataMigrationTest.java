package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.repair.ItemRarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemInstanceMetadataMigrationTest {
    @Test
    void legacyVersionIsUpgradedToCurrentSchema() {
        ItemInstanceMetadata metadata = new ItemInstanceMetadata();
        metadata.setVersion(0);
        metadata.setRarity(ItemRarity.RARE);
        metadata.setSource(ItemRollSource.CRAFTED);

        ItemInstanceMetadata migrated = ItemInstanceMetadataMigration.migrateToCurrent(metadata);

        assertSame(metadata, migrated);
        assertEquals(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION, migrated.getVersion());
        assertEquals(ItemRarity.RARE, migrated.getRarity());
        assertEquals(ItemRollSource.CRAFTED, migrated.getSource());
    }

    @Test
    void futureVersionIsPreserved() {
        ItemInstanceMetadata metadata = new ItemInstanceMetadata();
        metadata.setVersion(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION + 10L);

        ItemInstanceMetadata migrated = ItemInstanceMetadataMigration.migrateToCurrent(metadata);

        assertSame(metadata, migrated);
        assertEquals(ItemInstanceMetadata.CURRENT_SCHEMA_VERSION + 10L, migrated.getVersion());
    }
}
