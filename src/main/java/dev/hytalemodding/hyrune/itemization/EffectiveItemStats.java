package dev.hytalemodding.hyrune.itemization;

/**
 * Resolved stat payload for one item instance.
 */
public final class EffectiveItemStats {
    private final double damage;
    private final double defence;
    private final double healingPower;
    private final double utilityPower;

    public EffectiveItemStats(double damage, double defence, double healingPower, double utilityPower) {
        this.damage = damage;
        this.defence = defence;
        this.healingPower = healingPower;
        this.utilityPower = utilityPower;
    }

    public double getDamage() {
        return damage;
    }

    public double getDefence() {
        return defence;
    }

    public double getHealingPower() {
        return healingPower;
    }

    public double getUtilityPower() {
        return utilityPower;
    }
}
