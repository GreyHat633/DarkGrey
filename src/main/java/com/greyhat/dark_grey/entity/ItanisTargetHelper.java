package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import com.greyhat.dark_grey.api.CombatTargeting;

public class ItanisTargetHelper {

    /**
     * Finds the best target for an arrow based on sight and angle.
     */
    public static EntityLivingBase findTargetInSight(Entity shooter, double range, double fov) {
        if (!(shooter instanceof EntityLivingBase)) return null;

        EntityLivingBase target = null;
        double minDistanceSq = range * range;

        AxisAlignedBB aabb = shooter.boundingBox.expand(range, range, range);
        @SuppressWarnings("unchecked")
        List<Entity> entities = shooter.worldObj.getEntitiesWithinAABBExcludingEntity(shooter, aabb);

        Vec3 lookVec = shooter.getLookVec();
        Vec3 eyePos = Vec3.createVectorHelper(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);

        for (Entity e : entities) {
            if (isValidTarget(shooter, e)) {
                Vec3 dirToTarget = Vec3.createVectorHelper(
                    e.posX - eyePos.xCoord,
                    e.posY + e.height / 2.0F - eyePos.yCoord,
                    e.posZ - eyePos.zCoord);
                double distanceSq = dirToTarget.lengthVector() * dirToTarget.lengthVector();

                if (distanceSq <= range * range) {
                    dirToTarget = dirToTarget.normalize();
                    double dot = lookVec.dotProduct(dirToTarget);

                    if (dot > Math.cos(Math.toRadians(fov))) {
                        if (distanceSq < minDistanceSq) {
                            if (shooter.worldObj.rayTraceBlocks(
                                eyePos,
                                Vec3.createVectorHelper(e.posX, e.posY + e.height / 2.0F, e.posZ)) == null) {
                                minDistanceSq = distanceSq;
                                target = (EntityLivingBase) e;
                            }
                        }
                    }
                }
            }
        }

        return target;
    }

    /**
     * Finds the closest target around a point, optionally cycling targets for multiple arrows.
     */
    public static EntityLivingBase findClosestTarget(Entity shooter, double x, double y, double z, double range,
        int cycleIndex) {
        if (!(shooter instanceof EntityLivingBase)) return null;

        AxisAlignedBB aabb = AxisAlignedBB
            .getBoundingBox(x - range, y - range, z - range, x + range, y + range, z + range);
        @SuppressWarnings("unchecked")
        List<Entity> entities = shooter.worldObj.getEntitiesWithinAABBExcludingEntity(shooter, aabb);

        List<EntityLivingBase> validTargets = new java.util.ArrayList<>();

        for (Entity e : entities) {
            if (isValidTarget(shooter, e)) {
                double distSq = e.getDistanceSq(x, y, z);
                if (distSq <= range * range) {
                    validTargets.add((EntityLivingBase) e);
                }
            }
        }

        if (validTargets.isEmpty()) return null;

        validTargets.sort((e1, e2) -> Double.compare(e1.getDistanceSq(x, y, z), e2.getDistanceSq(x, y, z)));

        return validTargets.get(cycleIndex % validTargets.size());
    }

    public static boolean isValidTarget(Entity shooter, Entity target) {
        if (!(target instanceof EntityLivingBase)) return false;
        if (target.isDead || ((EntityLivingBase) target).getHealth() <= 0) return false;
        if (target == shooter) return false;
        if (shooter instanceof EntityLivingBase) {
            // Respect CombatTargeting for PVP, team damage and other protection rules
            if (!CombatTargeting.canDamage((EntityLivingBase) shooter, (EntityLivingBase) target, false)) {
                return false;
            }
        }
        return true;
    }
}
