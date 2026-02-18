package dev.hytalemodding.hyrune.itemization;

import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;

import java.util.Locale;

/**
 * Presentation-only formatter that maps raw stat decimals to readable UI values.
 * This does not affect simulation math or metadata values.
 */
public final class ItemStatDisplayFormatter {
    private static final double EPSILON = 1e-9;

    private ItemStatDisplayFormatter() {
    }

    public static String formatRoll(ItemizedStat stat, double flatRaw, double percentRaw) {
        double adjustedFlatRaw = flatRaw;
        double adjustedPercentRaw = percentRaw;
        if (stat == ItemizedStat.MOVEMENT_SPEED) {
            adjustedFlatRaw = SkillStatBonusApplier.applyItemMovementSpeedSoftCap(flatRaw);
            adjustedPercentRaw = SkillStatBonusApplier.applyItemMovementSpeedSoftCap(percentRaw);
        }

        ItemizationSpecializedStatConfigHelper.RollConstraint constraint =
            ItemizationSpecializedStatConfigHelper.rollConstraintForStat(stat);
        if (constraint == ItemizationSpecializedStatConfigHelper.RollConstraint.PERCENT_ONLY) {
            return formatPercent(adjustedFlatRaw + adjustedPercentRaw);
        }
        if (constraint == ItemizationSpecializedStatConfigHelper.RollConstraint.FLAT_ONLY) {
            return formatFlat(stat, adjustedFlatRaw + adjustedPercentRaw);
        }

        boolean hasFlat = Math.abs(adjustedFlatRaw) > EPSILON;
        boolean hasPercent = Math.abs(adjustedPercentRaw) > EPSILON;
        if (hasFlat && hasPercent) {
            if (stat != null && stat.isPercentPrimary()) {
                return formatPercent(adjustedPercentRaw) + " (" + formatFlat(stat, adjustedFlatRaw) + ")";
            }
            return formatFlat(stat, adjustedFlatRaw) + " (" + formatPercent(adjustedPercentRaw) + ")";
        }
        if (hasPercent) {
            return formatPercent(adjustedPercentRaw);
        }
        return formatFlat(stat, adjustedFlatRaw);
    }

    public static String formatFlat(ItemizedStat stat, double rawValue) {
        int decimals = ItemizationSpecializedStatConfigHelper.uiDisplayFlatDecimals();
        return signedNumber(rawValue, decimals, true);
    }

    public static String formatPercent(double rawValue) {
        double scaledPercent = rawValue * 100.0;
        int decimals = ItemizationSpecializedStatConfigHelper.uiDisplayPercentDecimals();
        return signedNumber(scaledPercent, decimals, false) + "%";
    }

    private static String signedNumber(double value, int decimals, boolean enforceMinOneAtIntegerPrecision) {
        if (decimals <= 0) {
            long rounded = Math.round(value);
            if (enforceMinOneAtIntegerPrecision && rounded == 0L && Math.abs(value) > EPSILON) {
                rounded = value > 0.0 ? 1L : -1L;
            }
            return String.format(Locale.US, "%+d", rounded);
        }
        String out = String.format(Locale.US, "%+." + decimals + "f", value);
        return trimTrailingZeros(out);
    }

    private static String trimTrailingZeros(String signedNumber) {
        if (signedNumber == null || signedNumber.isBlank() || !signedNumber.contains(".")) {
            return signedNumber;
        }
        int end = signedNumber.length();
        while (end > 0 && signedNumber.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && signedNumber.charAt(end - 1) == '.') {
            end--;
        }
        return signedNumber.substring(0, end);
    }
}
