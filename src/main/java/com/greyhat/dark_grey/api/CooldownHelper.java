package com.greyhat.dark_grey.api;

import net.minecraft.nbt.NBTTagCompound;

/** Dimension-independent cooldown storage for persistent item/player NBT. */
public final class CooldownHelper {

    public static final long MILLIS_PER_TICK = 50L;
    private static final long CLOCK_SKEW_TOLERANCE_MILLIS = 1000L;

    private CooldownHelper() {}

    public static long ticksToMillis(long ticks) {
        if (ticks <= 0L) return 0L;
        if (ticks > Long.MAX_VALUE / MILLIS_PER_TICK) return Long.MAX_VALUE;
        return ticks * MILLIS_PER_TICK;
    }

    public static long secondsToMillis(float seconds) {
        if (Float.isNaN(seconds) || Float.isInfinite(seconds) || seconds <= 0.0F) return 0L;
        double millis = seconds * 1000.0D;
        return millis >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(millis);
    }

    public static long getRemainingMillis(NBTTagCompound state, String endMillisKey, long maximumDurationMillis,
        String... legacyKeys) {
        if (state == null || endMillisKey == null) return 0L;
        removeLegacyKeys(state, legacyKeys);
        if (!state.hasKey(endMillisKey)) return 0L;

        long remaining = state.getLong(endMillisKey) - System.currentTimeMillis();
        if (remaining <= 0L) return 0L;
        if (maximumDurationMillis >= 0L && remaining > maximumDurationMillis + CLOCK_SKEW_TOLERANCE_MILLIS) {
            state.removeTag(endMillisKey);
            return 0L;
        }
        return remaining;
    }

    public static boolean isReady(NBTTagCompound state, String endMillisKey, long maximumDurationMillis,
        String... legacyKeys) {
        return getRemainingMillis(state, endMillisKey, maximumDurationMillis, legacyKeys) <= 0L;
    }

    public static void start(NBTTagCompound state, String endMillisKey, long durationMillis, String... legacyKeys) {
        if (state == null || endMillisKey == null) return;
        removeLegacyKeys(state, legacyKeys);
        if (durationMillis <= 0L) {
            state.removeTag(endMillisKey);
            return;
        }
        long now = System.currentTimeMillis();
        long end = durationMillis > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + durationMillis;
        state.setLong(endMillisKey, end);
    }

    public static void clear(NBTTagCompound state, String endMillisKey, String... legacyKeys) {
        if (state == null) return;
        if (endMillisKey != null) state.removeTag(endMillisKey);
        removeLegacyKeys(state, legacyKeys);
    }

    private static void removeLegacyKeys(NBTTagCompound state, String... legacyKeys) {
        if (legacyKeys == null) return;
        for (String key : legacyKeys) {
            if (key != null && !key.isEmpty()) state.removeTag(key);
        }
    }
}
