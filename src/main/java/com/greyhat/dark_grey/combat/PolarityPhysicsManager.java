package com.greyhat.dark_grey.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.component.ComponentPolarity;
import com.greyhat.dark_grey.mark.MarkContainer;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;
import com.greyhat.dark_grey.mark.type.NegativePolarityMarkType;
import com.greyhat.dark_grey.mark.type.PositivePolarityMarkType;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Server-authoritative, world-batched polarity physics.
 *
 * <p>
 * World tick START calculates every unordered pair once, accumulates equal and
 * opposite contributions, caps only each entity's net magnetic delta, applies
 * it once, and records the pre-movement velocity. World tick END performs
 * actual/swept contact checks and reads those snapshots for closing speed.
 * </p>
 */
public class PolarityPhysicsManager {

    private static final Map<World, Map<Integer, EntityLivingBase>> TRACKED_ENTITIES = new WeakHashMap<>();
    private static final Comparator<EntityLivingBase> ENTITY_ID_COMPARATOR = Comparator
        .comparingInt(EntityLivingBase::getEntityId);
    private static volatile boolean debugEnabled = Boolean.getBoolean("darkgrey.polarity.debug");

    private final Map<World, TickBatch> activeBatches = new WeakHashMap<>();
    private final Map<World, Map<String, PolarityPairState>> pairStatesByWorld = new WeakHashMap<>();
    private ComponentPolarity cachedConfig = new ComponentPolarity();
    private int cachedConfigVersion = Integer.MIN_VALUE;

    public static void track(EntityLivingBase entity) {
        if (entity == null || entity.worldObj == null || entity.worldObj.isRemote || entity.isDead) return;
        synchronized (TRACKED_ENTITIES) {
            Map<Integer, EntityLivingBase> worldEntities = TRACKED_ENTITIES.get(entity.worldObj);
            if (worldEntities == null) {
                worldEntities = new HashMap<>();
                TRACKED_ENTITIES.put(entity.worldObj, worldEntities);
            }
            worldEntities.put(entity.getEntityId(), entity);
        }
    }

    public static void untrackIfUnmarked(EntityLivingBase entity) {
        if (entity == null || entity.worldObj == null || hasPolarity(entity)) return;
        synchronized (TRACKED_ENTITIES) {
            Map<Integer, EntityLivingBase> worldEntities = TRACKED_ENTITIES.get(entity.worldObj);
            if (worldEntities != null) {
                worldEntities.remove(entity.getEntityId());
                if (worldEntities.isEmpty()) TRACKED_ENTITIES.remove(entity.worldObj);
            }
        }
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        DarkGrey.LOG.warn("[PolarityDebug] " + (enabled ? "enabled" : "disabled"));
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity.worldObj.isRemote) return;
        if (hasPolarity(entity)) {
            track(entity);
        } else {
            untrackIfUnmarked(entity);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote) return;
        if (event.phase == TickEvent.Phase.START) {
            beginWorldTick(event.world);
        } else {
            finishWorldTick(event.world);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        synchronized (TRACKED_ENTITIES) {
            TRACKED_ENTITIES.remove(event.world);
        }
        activeBatches.remove(event.world);
        pairStatesByWorld.remove(event.world);
    }

    private void beginWorldTick(World world) {
        ComponentPolarity config = getCurrentConfig();
        List<EntityLivingBase> entities = collectActiveEntities(world);
        if (entities.isEmpty()) {
            activeBatches.remove(world);
            cleanupPairStates(world, world.getTotalWorldTime(), config);
            return;
        }

        Collections.sort(entities, ENTITY_ID_COMPARATOR);
        Map<Integer, MotionSnapshot> snapshots = new HashMap<>();
        Map<Integer, double[]> accumulatedDeltas = new HashMap<>();
        Map<Integer, PolarityData> polarityByEntityId = new HashMap<>();
        Map<SpatialCell, List<EntityLivingBase>> spatialIndex = new HashMap<>();
        for (EntityLivingBase entity : entities) {
            int entityId = entity.getEntityId();
            MotionSnapshot snapshot = new MotionSnapshot(entity);
            snapshots.put(entityId, snapshot);
            accumulatedDeltas.put(entity.getEntityId(), new double[3]);
            polarityByEntityId.put(entityId, getPolarityData(entity));
            addToSpatialIndex(spatialIndex, entity, snapshot, config.magneticRange);
        }

        List<PairSample> pairSamples = new ArrayList<>();
        double[] direction = new double[3];
        for (int i = 0; i < entities.size(); i++) {
            EntityLivingBase entityA = entities.get(i);
            PolarityData polarityA = polarityByEntityId.get(entityA.getEntityId());
            if (polarityA == null) continue;
            MotionSnapshot snapshotA = snapshots.get(entityA.getEntityId());

            List<EntityLivingBase> nearbyEntities = collectNearbyEntities(
                spatialIndex,
                entityA,
                snapshotA,
                config.magneticRange);
            for (EntityLivingBase entityB : nearbyEntities) {
                PolarityData polarityB = polarityByEntityId.get(entityB.getEntityId());
                if (polarityB == null) continue;
                MotionSnapshot snapshotB = snapshots.get(entityB.getEntityId());

                double dx = snapshotB.startCenterX - snapshotA.startCenterX;
                double dy = snapshotB.startCenterY - snapshotA.startCenterY;
                double dz = snapshotB.startCenterZ - snapshotA.startCenterZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;
                double distance = distanceSq <= 0.0D ? 0.0D : Math.sqrt(distanceSq);
                if (distance >= config.magneticRange) continue;

                PolarityPhysicsMath.directionAtoB(dx, dy, dz, entityA.getEntityId(), entityB.getEntityId(), direction);
                double pairAcceleration = PolarityPhysicsMath
                    .pairAcceleration(distance, config.magneticRange, config.maxPairAcceleration);
                boolean samePolarity = polarityA.mode == polarityB.mode;
                double signForA = samePolarity ? -1.0D : 1.0D;
                double pairDeltaX = direction[0] * pairAcceleration * signForA;
                double pairDeltaY = direction[1] * pairAcceleration * signForA;
                double pairDeltaZ = direction[2] * pairAcceleration * signForA;

                double[] deltaA = accumulatedDeltas.get(entityA.getEntityId());
                double[] deltaB = accumulatedDeltas.get(entityB.getEntityId());
                deltaA[0] += pairDeltaX;
                deltaA[1] += pairDeltaY;
                deltaA[2] += pairDeltaZ;
                deltaB[0] -= pairDeltaX;
                deltaB[1] -= pairDeltaY;
                deltaB[2] -= pairDeltaZ;

                pairSamples.add(
                    new PairSample(
                        entityA,
                        entityB,
                        samePolarity,
                        distance,
                        pairAcceleration,
                        direction[0],
                        direction[1],
                        direction[2]));
            }
        }

        for (EntityLivingBase entity : entities) {
            MotionSnapshot snapshot = snapshots.get(entity.getEntityId());
            double[] delta = accumulatedDeltas.get(entity.getEntityId());
            snapshot.uncappedDeltaMagnitude = magnitude(delta[0], delta[1], delta[2]);
            snapshot.cappedDeltaMagnitude = PolarityPhysicsMath
                .limitMagnitude(delta, config.maxNetMagneticAcceleration);
            snapshot.deltaX = delta[0];
            snapshot.deltaY = delta[1];
            snapshot.deltaZ = delta[2];

            entity.addVelocity(delta[0], delta[1], delta[2]);
            if (entity instanceof EntityPlayerMP && snapshot.cappedDeltaMagnitude > 0.0D) {
                entity.velocityChanged = true;
            }
            snapshot.preMoveVelocityX = entity.motionX;
            snapshot.preMoveVelocityY = entity.motionY;
            snapshot.preMoveVelocityZ = entity.motionZ;
        }

        for (PairSample pair : pairSamples) {
            MotionSnapshot snapshotA = snapshots.get(pair.entityA.getEntityId());
            MotionSnapshot snapshotB = snapshots.get(pair.entityB.getEntityId());
            pair.closingSpeed = PolarityPhysicsMath.closingSpeed(
                snapshotA.preMoveVelocityX,
                snapshotA.preMoveVelocityY,
                snapshotA.preMoveVelocityZ,
                snapshotB.preMoveVelocityX,
                snapshotB.preMoveVelocityY,
                snapshotB.preMoveVelocityZ,
                pair.directionX,
                pair.directionY,
                pair.directionZ);
        }

        activeBatches.put(world, new TickBatch(config, snapshots, pairSamples));
    }

    private static void addToSpatialIndex(Map<SpatialCell, List<EntityLivingBase>> spatialIndex,
        EntityLivingBase entity, MotionSnapshot snapshot, double cellSize) {
        SpatialCell cell = SpatialCell
            .from(snapshot.startCenterX, snapshot.startCenterY, snapshot.startCenterZ, cellSize);
        List<EntityLivingBase> cellEntities = spatialIndex.get(cell);
        if (cellEntities == null) {
            cellEntities = new ArrayList<>();
            spatialIndex.put(cell, cellEntities);
        }
        cellEntities.add(entity);
    }

    private static List<EntityLivingBase> collectNearbyEntities(Map<SpatialCell, List<EntityLivingBase>> spatialIndex,
        EntityLivingBase entity, MotionSnapshot snapshot, double cellSize) {
        SpatialCell origin = SpatialCell
            .from(snapshot.startCenterX, snapshot.startCenterY, snapshot.startCenterZ, cellSize);
        List<EntityLivingBase> nearby = new ArrayList<>();
        int entityId = entity.getEntityId();
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    List<EntityLivingBase> cellEntities = spatialIndex.get(origin.offset(offsetX, offsetY, offsetZ));
                    if (cellEntities == null) continue;
                    for (EntityLivingBase candidate : cellEntities) {
                        if (candidate.getEntityId() > entityId) nearby.add(candidate);
                    }
                }
            }
        }
        Collections.sort(nearby, ENTITY_ID_COMPARATOR);
        return nearby;
    }

    private void finishWorldTick(World world) {
        TickBatch batch = activeBatches.remove(world);
        if (batch == null) return;

        long now = world.getTotalWorldTime();
        Map<String, PolarityPairState> pairStates = pairStatesByWorld.get(world);
        if (pairStates == null) {
            pairStates = new HashMap<>();
            pairStatesByWorld.put(world, pairStates);
        }

        for (PairSample pair : batch.pairs) {
            MotionSnapshot snapshotA = batch.snapshots.get(pair.entityA.getEntityId());
            MotionSnapshot snapshotB = batch.snapshots.get(pair.entityB.getEntityId());
            if (!isStillValid(world, pair.entityA) || !isStillValid(world, pair.entityB)) continue;

            double moveAX = centerX(pair.entityA) - snapshotA.startCenterX;
            double moveAY = centerY(pair.entityA) - snapshotA.startCenterY;
            double moveAZ = centerZ(pair.entityA) - snapshotA.startCenterZ;
            double moveBX = centerX(pair.entityB) - snapshotB.startCenterX;
            double moveBY = centerY(pair.entityB) - snapshotB.startCenterY;
            double moveBZ = centerZ(pair.entityB) - snapshotB.startCenterZ;

            boolean actualContact = PolarityPhysicsMath.intersectsWithTolerance(
                pair.entityA.boundingBox,
                pair.entityB.boundingBox,
                batch.config.collisionTolerance);
            double sweptTime = PolarityPhysicsMath.sweptContactTime(
                snapshotA.startBox,
                snapshotB.startBox,
                moveAX,
                moveAY,
                moveAZ,
                moveBX,
                moveBY,
                moveBZ,
                batch.config.collisionTolerance);
            boolean sweptContact = !actualContact && !Double.isNaN(sweptTime);
            boolean contact = actualContact || sweptContact;
            boolean triggered = false;

            PolarityData currentA = getPolarityData(pair.entityA);
            PolarityData currentB = getPolarityData(pair.entityB);
            boolean currentlyOpposite = currentA != null && currentB != null && currentA.mode != currentB.mode;
            if (currentlyOpposite) {
                String pairId = getPairId(pair.entityA, pair.entityB);
                PolarityPairState state = pairStates.get(pairId);
                if (state == null) {
                    state = new PolarityPairState();
                    pairStates.put(pairId, state);
                }
                state.lastSeenWorldTime = now;

                if (!state.armed) {
                    double gap = PolarityPhysicsMath.aabbGap(pair.entityA.boundingBox, pair.entityB.boundingBox);
                    if (gap > batch.config.collisionTolerance + batch.config.collisionRearmExtraDistance) {
                        state.rearm();
                    }
                } else if (contact && pair.closingSpeed >= batch.config.collisionSpeedThreshold
                    && debounceElapsed(state, now, batch.config.pairExplosionDebounceTicks)) {
                        state.armed = false;
                        state.lastExplosionWorldTime = now;
                        triggered = true;

                        boolean special = sourceParticipates(currentA.sourceUuid, pair.entityA, pair.entityB)
                            || sourceParticipates(currentB.sourceUuid, pair.entityA, pair.entityB);
                        double contactTime = actualContact ? 1.0D : clamp01(sweptTime);
                        double explosionX = ((snapshotA.startCenterX + moveAX * contactTime)
                            + (snapshotB.startCenterX + moveBX * contactTime)) * 0.5D;
                        double explosionY = ((snapshotA.startCenterY + moveAY * contactTime)
                            + (snapshotB.startCenterY + moveBY * contactTime)) * 0.5D;
                        double explosionZ = ((snapshotA.startCenterZ + moveAZ * contactTime)
                            + (snapshotB.startCenterZ + moveBZ * contactTime)) * 0.5D;

                        PolarityDamageResolver.resolveCollision(
                            pair.entityA,
                            pair.entityB,
                            batch.config.secondaryExplosionRadius,
                            batch.config.explosionKnockbackStrength,
                            special,
                            explosionX,
                            explosionY,
                            explosionZ);
                        consumePolarityMarks(pair.entityA);
                        consumePolarityMarks(pair.entityB);
                    }
            }

            if (debugEnabled) {
                logPairDebug(now, pair, snapshotA, snapshotB, actualContact, sweptContact, triggered);
            }
        }
        cleanupPairStates(world, now, batch.config);
    }

    private ComponentPolarity getCurrentConfig() {
        RPGItemDataManager manager = RPGItemDataManager.getInstance();
        int version = manager.getDataVersion();
        if (version == cachedConfigVersion) return cachedConfig;

        ComponentPolarity resolved = new ComponentPolarity();
        RPGItemDataManager.ItemConfig itemConfig = manager.getConfig("polarity");
        if (itemConfig != null && itemConfig.componentsJson != null) {
            for (JsonElement element : itemConfig.componentsJson) {
                if (!element.isJsonObject()) continue;
                JsonObject component = element.getAsJsonObject();
                if (!component.has("name") || !"极性".equals(
                    component.get("name")
                        .getAsString())) {
                    continue;
                }
                JsonObject params = component.has("params") ? component.getAsJsonObject("params") : new JsonObject();
                resolved.configure(params);
                break;
            }
        }
        cachedConfig = resolved;
        cachedConfigVersion = version;
        return cachedConfig;
    }

    private List<EntityLivingBase> collectActiveEntities(World world) {
        List<EntityLivingBase> result = new ArrayList<>();
        synchronized (TRACKED_ENTITIES) {
            Map<Integer, EntityLivingBase> worldEntities = TRACKED_ENTITIES.get(world);
            if (worldEntities == null) return result;
            Iterator<Map.Entry<Integer, EntityLivingBase>> iterator = worldEntities.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, EntityLivingBase> entry = iterator.next();
                EntityLivingBase entity = entry.getValue();
                if (!isStillValid(world, entity) || world.getEntityByID(entry.getKey()) != entity
                    || !hasPolarity(entity)) {
                    iterator.remove();
                } else {
                    result.add(entity);
                }
            }
            if (worldEntities.isEmpty()) TRACKED_ENTITIES.remove(world);
        }
        return result;
    }

    private void cleanupPairStates(World world, long now, ComponentPolarity config) {
        Map<String, PolarityPairState> states = pairStatesByWorld.get(world);
        if (states == null) return;
        long retentionTicks = Math.max(200L, config.pairExplosionDebounceTicks * 20L);
        Iterator<PolarityPairState> iterator = states.values()
            .iterator();
        while (iterator.hasNext()) {
            PolarityPairState state = iterator.next();
            if (now < state.lastSeenWorldTime || now - state.lastSeenWorldTime > retentionTicks) {
                iterator.remove();
            }
        }
        if (states.isEmpty()) pairStatesByWorld.remove(world);
    }

    private static boolean debounceElapsed(PolarityPairState state, long now, int debounceTicks) {
        if (state.lastExplosionWorldTime <= 0L || now < state.lastExplosionWorldTime) return true;
        return now - state.lastExplosionWorldTime >= Math.max(0, debounceTicks);
    }

    private static boolean sourceParticipates(UUID sourceUuid, EntityLivingBase a, EntityLivingBase b) {
        return sourceUuid != null && (sourceUuid.equals(a.getUniqueID()) || sourceUuid.equals(b.getUniqueID()));
    }

    private static void consumePolarityMarks(EntityLivingBase entity) {
        MarkManager.remove(entity, PositivePolarityMarkType.ID, MarkRemovalReason.CONSUMED);
        MarkManager.remove(entity, NegativePolarityMarkType.ID, MarkRemovalReason.CONSUMED);
    }

    private static PolarityData getPolarityData(EntityLivingBase entity) {
        MarkContainer container = MarkContainer.get(entity);
        if (container == null) return null;
        MarkInstance positive = container.getMark(PositivePolarityMarkType.ID);
        if (positive != null && positive.getStacks() > 0) {
            return new PolarityData(ComponentPolarity.MODE_POSITIVE, positive.getSourceUuid());
        }
        MarkInstance negative = container.getMark(NegativePolarityMarkType.ID);
        if (negative != null && negative.getStacks() > 0) {
            return new PolarityData(ComponentPolarity.MODE_NEGATIVE, negative.getSourceUuid());
        }
        return null;
    }

    private static boolean hasPolarity(EntityLivingBase entity) {
        return getPolarityData(entity) != null;
    }

    private static boolean isStillValid(World world, EntityLivingBase entity) {
        return entity != null && !entity.isDead && entity.worldObj == world;
    }

    private static String getPairId(EntityLivingBase a, EntityLivingBase b) {
        String first = a.getUniqueID()
            .toString();
        String second = b.getUniqueID()
            .toString();
        return first.compareTo(second) <= 0 ? first + "_" + second : second + "_" + first;
    }

    private static double centerX(EntityLivingBase entity) {
        return (entity.boundingBox.minX + entity.boundingBox.maxX) * 0.5D;
    }

    private static double centerY(EntityLivingBase entity) {
        return (entity.boundingBox.minY + entity.boundingBox.maxY) * 0.5D;
    }

    private static double centerZ(EntityLivingBase entity) {
        return (entity.boundingBox.minZ + entity.boundingBox.maxZ) * 0.5D;
    }

    private static double magnitude(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) return 1.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static void logPairDebug(long tick, PairSample pair, MotionSnapshot a, MotionSnapshot b,
        boolean actualContact, boolean sweptContact, boolean triggered) {
        DarkGrey.LOG.info(
            String.format(
                Locale.ROOT,
                "[PolarityDebug] tick=%d pair=%d/%d same=%s distance=%.6f pairAcceleration=%.6f "
                    + "netDeltaA=(%.6f,%.6f,%.6f; uncapped=%.6f capped=%.6f) "
                    + "netDeltaB=(%.6f,%.6f,%.6f; uncapped=%.6f capped=%.6f) "
                    + "beforeA=(%.6f,%.6f,%.6f) beforeB=(%.6f,%.6f,%.6f) "
                    + "preMoveA=(%.6f,%.6f,%.6f) preMoveB=(%.6f,%.6f,%.6f) "
                    + "postMoveA=(%.6f,%.6f,%.6f) postMoveB=(%.6f,%.6f,%.6f) "
                    + "closingSpeed=%.6f actualContact=%s sweptContact=%s triggered=%s",
                tick,
                pair.entityA.getEntityId(),
                pair.entityB.getEntityId(),
                pair.samePolarity,
                pair.startDistance,
                pair.pairAcceleration,
                a.deltaX,
                a.deltaY,
                a.deltaZ,
                a.uncappedDeltaMagnitude,
                a.cappedDeltaMagnitude,
                b.deltaX,
                b.deltaY,
                b.deltaZ,
                b.uncappedDeltaMagnitude,
                b.cappedDeltaMagnitude,
                a.velocityBeforeX,
                a.velocityBeforeY,
                a.velocityBeforeZ,
                b.velocityBeforeX,
                b.velocityBeforeY,
                b.velocityBeforeZ,
                a.preMoveVelocityX,
                a.preMoveVelocityY,
                a.preMoveVelocityZ,
                b.preMoveVelocityX,
                b.preMoveVelocityY,
                b.preMoveVelocityZ,
                pair.entityA.motionX,
                pair.entityA.motionY,
                pair.entityA.motionZ,
                pair.entityB.motionX,
                pair.entityB.motionY,
                pair.entityB.motionZ,
                pair.closingSpeed,
                actualContact,
                sweptContact,
                triggered));
    }

    private static final class PolarityData {

        private final int mode;
        private final UUID sourceUuid;

        private PolarityData(int mode, UUID sourceUuid) {
            this.mode = mode;
            this.sourceUuid = sourceUuid;
        }
    }

    private static final class MotionSnapshot {

        private final AxisAlignedBB startBox;
        private final double startCenterX;
        private final double startCenterY;
        private final double startCenterZ;
        private final double velocityBeforeX;
        private final double velocityBeforeY;
        private final double velocityBeforeZ;
        private double deltaX;
        private double deltaY;
        private double deltaZ;
        private double uncappedDeltaMagnitude;
        private double cappedDeltaMagnitude;
        private double preMoveVelocityX;
        private double preMoveVelocityY;
        private double preMoveVelocityZ;

        private MotionSnapshot(EntityLivingBase entity) {
            this.startBox = entity.boundingBox.copy();
            this.startCenterX = centerX(entity);
            this.startCenterY = centerY(entity);
            this.startCenterZ = centerZ(entity);
            this.velocityBeforeX = entity.motionX;
            this.velocityBeforeY = entity.motionY;
            this.velocityBeforeZ = entity.motionZ;
        }
    }

    private static final class SpatialCell {

        private final long x;
        private final long y;
        private final long z;

        private SpatialCell(long x, long y, long z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static SpatialCell from(double x, double y, double z, double cellSize) {
            return new SpatialCell(
                (long) Math.floor(x / cellSize),
                (long) Math.floor(y / cellSize),
                (long) Math.floor(z / cellSize));
        }

        private SpatialCell offset(int offsetX, int offsetY, int offsetZ) {
            return new SpatialCell(x + offsetX, y + offsetY, z + offsetZ);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof SpatialCell)) return false;
            SpatialCell cell = (SpatialCell) other;
            return x == cell.x && y == cell.y && z == cell.z;
        }

        @Override
        public int hashCode() {
            int result = (int) (x ^ x >>> 32);
            result = 31 * result + (int) (y ^ y >>> 32);
            result = 31 * result + (int) (z ^ z >>> 32);
            return result;
        }
    }

    private static final class PairSample {

        private final EntityLivingBase entityA;
        private final EntityLivingBase entityB;
        private final boolean samePolarity;
        private final double startDistance;
        private final double pairAcceleration;
        private final double directionX;
        private final double directionY;
        private final double directionZ;
        private double closingSpeed;

        private PairSample(EntityLivingBase entityA, EntityLivingBase entityB, boolean samePolarity,
            double startDistance, double pairAcceleration, double directionX, double directionY, double directionZ) {
            this.entityA = entityA;
            this.entityB = entityB;
            this.samePolarity = samePolarity;
            this.startDistance = startDistance;
            this.pairAcceleration = pairAcceleration;
            this.directionX = directionX;
            this.directionY = directionY;
            this.directionZ = directionZ;
        }
    }

    private static final class TickBatch {

        private final ComponentPolarity config;
        private final Map<Integer, MotionSnapshot> snapshots;
        private final List<PairSample> pairs;

        private TickBatch(ComponentPolarity config, Map<Integer, MotionSnapshot> snapshots, List<PairSample> pairs) {
            this.config = config;
            this.snapshots = snapshots;
            this.pairs = pairs;
        }
    }
}
