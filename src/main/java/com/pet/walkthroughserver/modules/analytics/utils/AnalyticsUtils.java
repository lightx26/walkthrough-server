package com.pet.walkthroughserver.modules.analytics.utils;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnalyticsUtils {

    private static final int SEC_PER_FILE = 5;
    private static final int SEC_PER_PATCH_LINE = 1;
    public static final int MIN_EXPECTED_READ_SEC = 20;
    public static final double SKIM_THRESHOLD_RATIO = 0.3;

    public static int expectedReadSec(int fileCount, int patchLineCount) {
        return Math.max(MIN_EXPECTED_READ_SEC,
                fileCount * SEC_PER_FILE + patchLineCount * SEC_PER_PATCH_LINE);
    }

    public static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    public static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

    public static boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        return Boolean.parseBoolean(o.toString());
    }

    public static Instant toInstant(Object o) {
        return switch (o) {
            case Instant i -> i;
            case java.sql.Timestamp ts -> ts.toInstant();
            case java.time.OffsetDateTime odt -> odt.toInstant();
            case null, default -> null;
        };
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
