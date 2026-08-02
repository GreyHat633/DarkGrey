package com.greyhat.dark_grey.combat;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.network.PolarityExplosionEffectMessage;

public class PolarityDamageResolver {

    public static DamageValues calculateDamageValues(EntityLivingBase entityA, EntityLivingBase entityB,
        boolean isSpecial) {
        return calculateDamageValues(entityA.getTotalArmorValue(), entityB.getTotalArmorValue(), isSpecial);
    }

    public static DamageValues calculateDamageValues(int armorValueA, int armorValueB, boolean isSpecial) {
        double armorA = Math.min(Math.max(0, armorValueA), 1000);
        double armorB = Math.min(Math.max(0, armorValueB), 1000);
        double s = armorA + armorB;
        double l = Math.log1p(s / 20.0) / Math.log(3.0);

        float collisionDamage = (float) Math.round(160 + 300 * l);
        float explosionDamageNormal = (float) Math.round(50 + 150 * l);
        float explosionDamageSpecial = explosionDamageNormal * 4.0f;
        float explosionDamage = isSpecial ? explosionDamageSpecial : explosionDamageNormal;
        return new DamageValues(collisionDamage, explosionDamageNormal, explosionDamage);
    }

    public static void resolveCollision(EntityLivingBase entityA, EntityLivingBase entityB, double radius,
        double knockbackStrength, boolean isSpecial, double ex, double ey, double ez) {

        DamageValues damageValues = calculateDamageValues(entityA, entityB, isSpecial);

        EntityLivingBase primarySource = entityA;
        DamageSource collisionDs = new DamageSourcePolarity("polarity_collision", primarySource, null)
            .setDamageBypassesArmor();
        DamageSource explosionDs = new DamageSourcePolarity("polarity_explosion", primarySource, null)
            .setDamageBypassesArmor();

        dealIndependentDerivedDamage(entityA, collisionDs, damageValues.collisionDamage);
        dealIndependentDerivedDamage(entityB, collisionDs, damageValues.collisionDamage);
        // Core collision participants must always receive the independent
        // explosion stage, even when a very large hitbox puts one center outside
        // the radius query around the contact midpoint.
        dealIndependentDerivedDamage(entityA, explosionDs, damageValues.appliedExplosionDamage);
        dealIndependentDerivedDamage(entityB, explosionDs, damageValues.appliedExplosionDamage);

        List<Entity> list = entityA.worldObj.getEntitiesWithinAABBExcludingEntity(
            null,
            AxisAlignedBB.getBoundingBox(ex - radius, ey - radius, ez - radius, ex + radius, ey + radius, ez + radius));

        for (Entity e : list) {
            if (e instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) e;
                double distSq = target.getDistanceSq(ex, ey, ez);
                if (distSq <= radius * radius) {
                    if (target != entityA && target != entityB
                        && CombatTargeting.canDamage(primarySource, target, false)) {
                        dealIndependentDerivedDamage(target, explosionDs, damageValues.appliedExplosionDamage);
                    }

                    double dist = Math.sqrt(distSq);
                    if (dist > 0.01) {
                        double dx = (target.posX - ex) / dist;
                        double dy = (target.posY - ey) / dist;
                        double dz = (target.posZ - ez) / dist;
                        target.addVelocity(dx * knockbackStrength, dy * knockbackStrength, dz * knockbackStrength);
                    }
                }
            }
        }

        DarkGrey.NETWORK.sendToAllAround(
            new PolarityExplosionEffectMessage(ex, ey, ez, isSpecial),
            new cpw.mods.fml.common.network.NetworkRegistry.TargetPoint(entityA.dimension, ex, ey, ez, 64.0));
    }

    public static boolean dealIndependentDerivedDamage(EntityLivingBase target, DamageSource ds, float amount) {
        int oldResist = target.hurtResistantTime;
        try {
            target.hurtResistantTime = 0;
            return target.attackEntityFrom(ds, amount);
        } finally {
            target.hurtResistantTime = oldResist;
        }
    }

    public static final class DamageValues {

        public final float collisionDamage;
        public final float normalExplosionDamage;
        public final float appliedExplosionDamage;

        private DamageValues(float collisionDamage, float normalExplosionDamage, float appliedExplosionDamage) {
            this.collisionDamage = collisionDamage;
            this.normalExplosionDamage = normalExplosionDamage;
            this.appliedExplosionDamage = appliedExplosionDamage;
        }
    }
}
