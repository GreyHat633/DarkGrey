package com.greyhat.dark_grey.api;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.greyhat.dark_grey.entity.EntityMadokaArrow;

/**
 * Batches same-volley hits into one normal damage transaction per target.
 *
 * <p>
 * This keeps all thirty visible projectiles while preventing vanilla/CNPC+
 * hurt-resistance frames from discarding all but one arrow and avoids dispatching
 * thirty complete Forge/KCauldron combat pipelines in a single tick.
 * </p>
 */
public final class MadokaVolleyDamageManager {

    private static final int QUIET_TICKS_BEFORE_FLUSH = 2;
    private static final int MAX_BATCH_AGE_TICKS = 5;
    private static final AtomicInteger NEXT_VOLLEY_ID = new AtomicInteger(1);
    private static final Map<World, Map<VolleyTargetKey, PendingDamage>> PENDING = new IdentityHashMap<>();

    private MadokaVolleyDamageManager() {}

    public static int nextVolleyId() {
        int id = NEXT_VOLLEY_ID.getAndIncrement();
        if (id == Integer.MAX_VALUE) {
            NEXT_VOLLEY_ID.compareAndSet(Integer.MIN_VALUE, 1);
        }
        return id;
    }

    public static boolean queueHit(EntityMadokaArrow projectile, EntityLivingBase shooter, EntityLivingBase target,
        int volleyId, float damage) {
        if (projectile == null || shooter == null
            || target == null
            || volleyId == 0
            || damage <= 0.0F
            || Float.isNaN(damage)
            || Float.isInfinite(damage)
            || target.worldObj == null
            || target.worldObj.isRemote) {
            return false;
        }

        World world = target.worldObj;
        Map<VolleyTargetKey, PendingDamage> worldPending = PENDING.get(world);
        if (worldPending == null) {
            worldPending = new HashMap<>();
            PENDING.put(world, worldPending);
        }

        VolleyTargetKey key = new VolleyTargetKey(volleyId, shooter.getEntityId(), target.getEntityId());
        PendingDamage pending = worldPending.get(key);
        long now = world.getTotalWorldTime();
        if (pending == null) {
            pending = new PendingDamage(projectile, shooter, target, now);
            worldPending.put(key, pending);
        }
        pending.lastHitTick = now;
        pending.damage = safeAdd(pending.damage, damage);
        return true;
    }

    /** Called on the server tick after projectile updates have queued their hits. */
    public static void flushExpired() {
        Iterator<Map.Entry<World, Map<VolleyTargetKey, PendingDamage>>> worlds = PENDING.entrySet()
            .iterator();
        while (worlds.hasNext()) {
            Map.Entry<World, Map<VolleyTargetKey, PendingDamage>> worldEntry = worlds.next();
            World world = worldEntry.getKey();
            Map<VolleyTargetKey, PendingDamage> worldPending = worldEntry.getValue();
            long now = world.getTotalWorldTime();

            Iterator<PendingDamage> pendingIterator = worldPending.values()
                .iterator();
            while (pendingIterator.hasNext()) {
                PendingDamage pending = pendingIterator.next();
                if (now - pending.lastHitTick < QUIET_TICKS_BEFORE_FLUSH
                    && now - pending.firstHitTick < MAX_BATCH_AGE_TICKS) {
                    continue;
                }
                apply(pending);
                pendingIterator.remove();
            }

            if (worldPending.isEmpty()) {
                worlds.remove();
            }
        }
    }

    public static void clear() {
        PENDING.clear();
    }

    private static void apply(PendingDamage pending) {
        EntityLivingBase shooter = pending.shooter;
        EntityLivingBase target = pending.target;
        if (shooter == null || target == null
            || shooter.isDead
            || target.isDead
            || shooter.worldObj != target.worldObj
            || !CombatTargeting.canDamage(shooter, target, false)) {
            return;
        }
        target.attackEntityFrom(RPGDamageSources.causeArrowDamage(pending.projectile, shooter), pending.damage);
    }

    private static float safeAdd(float left, float right) {
        double sum = (double) left + right;
        if (Double.isNaN(sum) || sum <= 0.0D) return 0.0F;
        return sum >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) sum;
    }

    private static final class PendingDamage {

        private final EntityMadokaArrow projectile;
        private final EntityLivingBase shooter;
        private final EntityLivingBase target;
        private final long firstHitTick;
        private long lastHitTick;
        private float damage;

        private PendingDamage(EntityMadokaArrow projectile, EntityLivingBase shooter, EntityLivingBase target,
            long firstHitTick) {
            this.projectile = projectile;
            this.shooter = shooter;
            this.target = target;
            this.firstHitTick = firstHitTick;
            this.lastHitTick = firstHitTick;
        }
    }

    private static final class VolleyTargetKey {

        private final int volleyId;
        private final int shooterId;
        private final int targetId;

        private VolleyTargetKey(int volleyId, int shooterId, int targetId) {
            this.volleyId = volleyId;
            this.shooterId = shooterId;
            this.targetId = targetId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof VolleyTargetKey)) return false;
            VolleyTargetKey key = (VolleyTargetKey) other;
            return this.volleyId == key.volleyId && this.shooterId == key.shooterId && this.targetId == key.targetId;
        }

        @Override
        public int hashCode() {
            int result = volleyId;
            result = 31 * result + shooterId;
            result = 31 * result + targetId;
            return result;
        }
    }
}
