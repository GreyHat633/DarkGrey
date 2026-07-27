package com.greyhat.dark_grey.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.mark.MarkContainer;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.type.ShatteredBoneMarkType;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ShatteredBoneAttackHandler {

    private static class MovementData {

        double lastX, lastZ;
        long nextDamageTick;

        MovementData(double x, double z) {
            this.lastX = x;
            this.lastZ = z;
            this.nextDamageTick = 0;
        }
    }

    private final Map<UUID, MovementData> tracking = new HashMap<>();

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.entityLiving;
        World world = entity.worldObj;
        if (world.isRemote || entity.isDead) return;

        MarkContainer container = MarkContainer.get(entity);
        if (container == null || container.isEmpty()) {
            this.tracking.remove(entity.getUniqueID());
            return;
        }

        MarkInstance instance = container.getMark(ShatteredBoneMarkType.ID);
        if (instance == null || instance.getStacks() < 1) {
            this.tracking.remove(entity.getUniqueID());
            return;
        }

        UUID uuid = entity.getUniqueID();
        MovementData data = this.tracking.get(uuid);

        if (data == null) {
            this.tracking.put(uuid, new MovementData(entity.posX, entity.posZ));
            return;
        }

        double dx = entity.posX - data.lastX;
        double dz = entity.posZ - data.lastZ;
        double distSq = dx * dx + dz * dz;

        if (distSq > 10.0 * 10.0) {
            data.lastX = entity.posX;
            data.lastZ = entity.posZ;
            return;
        }

        if (distSq >= 0.01) {
            long now = world.getTotalWorldTime();
            if (now >= data.nextDamageTick) {
                com.greyhat.dark_grey.api.RPGDamageSources
                    .dealDamageWithoutInvulnerability(entity, ShatteredBoneDamageSources.causeMovementDamage(), 5.0F);
                data.nextDamageTick = now + 5;
            }
        }

        data.lastX = entity.posX;
        data.lastZ = entity.posZ;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.entityLiving.worldObj.isRemote) return;
        EntityLivingBase target = event.entityLiving;
        if (!com.greyhat.dark_grey.util.DirectAttackClassifier.isDirectAttack(event.source)) return;
        if (!(event.source.getEntity() instanceof EntityLivingBase)) return;
        EntityLivingBase attacker = (EntityLivingBase) event.source.getEntity();

        MarkContainer container = MarkContainer.get(target);
        if (container == null || container.isEmpty()) return;

        MarkInstance instance = container.getMark(ShatteredBoneMarkType.ID);
        if (instance == null || instance.getStacks() < 1) return;

        if (com.greyhat.dark_grey.util.SplashRecursionGuard.isProcessingSplash()) return;

        IAttributeInstance attackDamageAttr = attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage);
        float baseDamage = attackDamageAttr != null ? (float) attackDamageAttr.getAttributeValue() : 1.0F;
        float splashDamage = baseDamage * 2.25F;

        com.greyhat.dark_grey.util.SplashRecursionGuard.setProcessingSplash(true);
        try {
            // Calculate reliable attack direction (from attacker to target)
            double dirX = target.posX - attacker.posX;
            double dirY = (target.posY + target.height / 2.0F) - (attacker.posY + attacker.getEyeHeight());
            double dirZ = target.posZ - attacker.posZ;
            double dirLen = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);

            if (dirLen < 0.01) {
                net.minecraft.util.Vec3 lv = attacker.getLookVec();
                dirX = lv.xCoord;
                dirY = lv.yCoord;
                dirZ = lv.zCoord;
            } else {
                dirX /= dirLen;
                dirY /= dirLen;
                dirZ /= dirLen;
            }

            if (target.worldObj instanceof net.minecraft.world.WorldServer) {
                net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) target.worldObj;
                java.util.Random rand = ws.rand;
                float[] velocities = new float[com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage.PARTICLE_COUNT
                    * 3];

                // Physical Parabolic Spray of Bone Fragments
                for (int i = 0; i < com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage.PARTICLE_COUNT; i++) {
                    // Base horizontal direction
                    double hLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
                    double bx = dirX, bz = dirZ;
                    if (hLen > 0.001) {
                        bx /= hLen;
                        bz /= hLen;
                    }

                    // Random angle spread within +/- 25 degrees (approx 0.8 radians total)
                    double angle = (rand.nextDouble() - 0.5) * 0.8;
                    double cosA = Math.cos(angle);
                    double sinA = Math.sin(angle);

                    double hx = bx * cosA - bz * sinA;
                    double hz = bz * cosA + bx * sinA;

                    // Flatter, faster trajectory like a shotgun blast
                    double targetDist = 1.0 + rand.nextDouble() * 4.0;

                    // Lower upward velocity (0.05 to 0.15) for a flat spray
                    double vy = 0.05 + rand.nextDouble() * 0.1;

                    // Time in air approx = 2 * vy / 0.04 = vy * 50
                    double timeInAir = vy * 50.0;

                    // Required horizontal velocity
                    double vHoriz = (targetDist / timeInAir) * 1.1;

                    double vx = hx * vHoriz;
                    double vz = hz * vHoriz;

                    int velocityOffset = i * 3;
                    velocities[velocityOffset] = (float) vx;
                    velocities[velocityOffset + 1] = (float) vy;
                    velocities[velocityOffset + 2] = (float) vz;
                }
                com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage particleMessage = new com.greyhat.dark_grey.network.ShatteredBoneParticlesMessage(
                    target.posX,
                    target.posY + target.height / 2.0F,
                    target.posZ,
                    velocities);
                for (Object playerObject : ws.playerEntities) {
                    net.minecraft.entity.player.EntityPlayerMP player = (net.minecraft.entity.player.EntityPlayerMP) playerObject;
                    net.minecraft.util.ChunkCoordinates coordinates = player.getPlayerCoordinates();
                    double playerDx = target.posX - coordinates.posX;
                    double playerDy = target.posY + target.height / 2.0F - coordinates.posY;
                    double playerDz = target.posZ - coordinates.posZ;
                    if (playerDx * playerDx + playerDy * playerDy + playerDz * playerDz <= 256.0D) {
                        com.greyhat.dark_grey.DarkGrey.NETWORK.sendTo(particleMessage, player);
                    }
                }
            }

            double radius = 5.0;
            java.util.List<EntityLivingBase> list = target.worldObj
                .getEntitiesWithinAABB(EntityLivingBase.class, target.boundingBox.expand(radius, radius, radius));
            for (EntityLivingBase splashTarget : list) {
                if (splashTarget == target || splashTarget == attacker) continue;
                if (!splashTarget.isEntityAlive()) continue;

                // Calculate distance from TARGET to SPLASH_TARGET
                double dx = splashTarget.posX - target.posX;
                double dz = splashTarget.posZ - target.posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);

                // Allow edge hits: distance to the EDGE of the entity's cylinder must be <= radius
                if (dist - splashTarget.width / 2.0 > radius || dist < 0.01) continue;

                // Accurate Cone-to-Cylinder Intersection:
                // Calculate the angular width of the entity as seen from the target
                double targetHalfAngle = Math.asin(Math.min(1.0, (splashTarget.width / 2.0) / Math.max(dist, 0.5)));

                // Calculate the angle between the attack direction and the entity's center
                double currentAngle = Math.acos(Math.max(-1.0, Math.min(1.0, (dx / dist) * dirX + (dz / dist) * dirZ)));

                // The requested cone has a total angle of 45 degrees, so half-angle is 22.5 degrees (0.3927 radians)
                // If the current angle minus the entity's half-angle is <= 22.5 degrees, they overlap!
                if (currentAngle - targetHalfAngle <= 0.3927) {
                    if (com.greyhat.dark_grey.api.CombatTargeting.canDamage(attacker, splashTarget, false)) {
                        boolean damaged = com.greyhat.dark_grey.api.RPGDamageSources.dealDamageWithoutInvulnerability(
                            splashTarget,
                            ShatteredBoneDamageSources.causeSplashDamage(attacker),
                            splashDamage);
                        if (!damaged) {
                            continue;
                        }

                        if (splashTarget.worldObj instanceof net.minecraft.world.WorldServer) {
                            ((net.minecraft.world.WorldServer) splashTarget.worldObj).func_147487_a(
                                "crit",
                                splashTarget.posX,
                                splashTarget.posY + splashTarget.height / 2.0F,
                                splashTarget.posZ,
                                20,
                                0.4D,
                                0.4D,
                                0.4D,
                                0.1D);
                        }

                        com.greyhat.dark_grey.mark.api.MarkApplyContext context = new com.greyhat.dark_grey.mark.api.MarkApplyContext.Builder()
                            .source(attacker)
                            .requestedStacks(1)
                            .worldTime(splashTarget.worldObj.getTotalWorldTime())
                            .applicationId("shattered_bone_splash")
                            .stableDurationTicks(com.greyhat.dark_grey.common.Config.shatteredBoneSplashDurationTicks)
                            .build();
                        com.greyhat.dark_grey.mark.MarkManager.apply(splashTarget, ShatteredBoneMarkType.ID, context);
                    }
                }
            }
        } finally {
            com.greyhat.dark_grey.util.SplashRecursionGuard.setProcessingSplash(false);
        }
    }
}
