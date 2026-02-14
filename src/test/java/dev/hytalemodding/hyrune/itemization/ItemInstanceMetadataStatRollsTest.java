package dev.hytalemodding.hyrune.itemization;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemInstanceMetadataStatRollsTest {
    @Test
    void setAndGetSpecializedRollsRoundTrip() {
        ItemInstanceMetadata metadata = new ItemInstanceMetadata();
        Map<String, Double> flatRolls = new LinkedHashMap<>();
        flatRolls.put(ItemizedStat.PHYSICAL_DAMAGE.getId(), 50.0);
        flatRolls.put(ItemizedStat.ATTACK_SPEED.getId(), 0.0200);
        Map<String, Double> percentRolls = new LinkedHashMap<>();
        percentRolls.put(ItemizedStat.PHYSICAL_DAMAGE.getId(), 0.1000);
        percentRolls.put(ItemizedStat.ATTACK_SPEED.getId(), 0.0400);

        metadata.setStatFlatRollsRaw(flatRolls);
        metadata.setStatPercentRollsRaw(percentRolls);

        assertEquals(50.0, metadata.getFlatStatRoll(ItemizedStat.PHYSICAL_DAMAGE), 1e-9);
        assertEquals(0.0200, metadata.getFlatStatRoll(ItemizedStat.ATTACK_SPEED), 1e-9);
        assertEquals(0.1000, metadata.getPercentStatRoll(ItemizedStat.PHYSICAL_DAMAGE), 1e-9);
        assertEquals(0.0400, metadata.getPercentStatRoll(ItemizedStat.ATTACK_SPEED), 1e-9);
        assertTrue(metadata.getStatFlatRollsJson().contains("physical_damage"));
        assertTrue(metadata.getStatFlatRollsJson().contains("attack_speed"));
        assertTrue(metadata.getStatPercentRollsJson().contains("physical_damage"));
        assertTrue(metadata.getStatPercentRollsJson().contains("attack_speed"));
    }

    @Test
    void invalidRollJsonFallsBackToEmptyMap() {
        ItemInstanceMetadata metadata = new ItemInstanceMetadata();
        metadata.setStatFlatRollsJson("{bad json");
        metadata.setStatPercentRollsJson("{bad json");

        assertTrue(metadata.getStatFlatRollsRaw().isEmpty());
        assertTrue(metadata.getStatPercentRollsRaw().isEmpty());
        assertEquals(0.0, metadata.getFlatStatRoll(ItemizedStat.MAGICAL_DAMAGE), 1e-9);
        assertEquals(0.0, metadata.getPercentStatRoll(ItemizedStat.MAGICAL_DAMAGE), 1e-9);
    }
}
