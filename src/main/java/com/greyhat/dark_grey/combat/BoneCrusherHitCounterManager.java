package com.greyhat.dark_grey.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;

/**
 * Manages hit counts for the Bone Crusher component.
 * Hit counts are tracked per target, and within each target, per attacker UUID.
 * Uses WeakHashMap to prevent memory leaks when target entities are unloaded or killed.
 */
public class BoneCrusherHitCounterManager {

    private static final WeakHashMap<EntityLivingBase, Map<UUID, Integer>> hitCounters = new WeakHashMap<>();

    /**
     * Adds a hit to the target by the given attacker and returns the new count.
     */
    public static int addHit(UUID attackerUuid, EntityLivingBase target) {
        if (attackerUuid == null || target == null || target.isDead) {
            return 0;
        }

        Map<UUID, Integer> targetMap = hitCounters.computeIfAbsent(target, k -> new HashMap<>());
        int currentCount = targetMap.getOrDefault(attackerUuid, 0);
        int newCount = currentCount + 1;
        targetMap.put(attackerUuid, newCount);

        return newCount;
    }

    /**
     * Clears the hit count for a specific attacker against a specific target.
     */
    public static void clearCount(UUID attackerUuid, EntityLivingBase target) {
        if (target == null) return;

        Map<UUID, Integer> targetMap = hitCounters.get(target);
        if (targetMap != null) {
            targetMap.remove(attackerUuid);
            if (targetMap.isEmpty()) {
                hitCounters.remove(target);
            }
        }
    }

    /**
     * Clears all counts for a specific target.
     */
    public static void clearTarget(EntityLivingBase target) {
        if (target != null) {
            hitCounters.remove(target);
        }
    }

    /**
     * Clears all counts for a specific attacker across all targets.
     */
    public static void clearAttacker(UUID attackerUuid) {
        if (attackerUuid == null) return;

        hitCounters.values()
            .forEach(map -> map.remove(attackerUuid));
        hitCounters.entrySet()
            .removeIf(
                entry -> entry.getValue()
                    .isEmpty());
    }

    /**
     * Clears all tracking completely (e.g., world unload).
     */
    public static void clearAll() {
        hitCounters.clear();
    }
}
