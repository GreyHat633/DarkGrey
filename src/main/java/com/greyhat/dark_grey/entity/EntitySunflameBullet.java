package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.type.ScorchMarkType;

public class EntitySunflameBullet extends EntityThrowable {

    private float damage;
    private double startX;
    private double startY;
    private double startZ;
    private static final double MAX_RANGE = 40.0;

    public EntitySunflameBullet(World world) {
        super(world);
    }

    public EntitySunflameBullet(World world, EntityLivingBase thrower, float damage) {
        super(world, thrower);
        this.damage = damage;
        this.startX = thrower.posX;
        this.startY = thrower.posY;
        this.startZ = thrower.posZ;
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0f; // No gravity
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!worldObj.isRemote) {
            double distanceSq = this.getDistanceSq(startX, startY, startZ);
            if (distanceSq > MAX_RANGE * MAX_RANGE) {
                this.setDead();
            }
        } else {
            // Stunning particle trail
            for (int i = 0; i < 3; i++) {
                double ox = (worldObj.rand.nextDouble() - 0.5) * 0.2;
                double oy = (worldObj.rand.nextDouble() - 0.5) * 0.2;
                double oz = (worldObj.rand.nextDouble() - 0.5) * 0.2;
                worldObj.spawnParticle("flame", posX + ox, posY + oy, posZ + oz, 0, 0, 0);
            }
            if (worldObj.rand.nextBoolean()) {
                worldObj.spawnParticle("lava", posX, posY, posZ, 0, 0, 0);
            }
        }
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (!worldObj.isRemote) {
            if (this.getEntityData()
                .getBoolean("VisualOnly")) {
                this.setDead();
                return;
            }

            if (mop.entityHit != null) {
                if (mop.entityHit == getThrower()) {
                    return; // Ignore thrower
                }

                DamageSource source = DamageSource.causeThrownDamage(this, getThrower())
                    .setProjectile();
                if (this.isBurning()) {
                    source.setFireDamage();
                }

                if (mop.entityHit.attackEntityFrom(source, damage)) {
                    if (mop.entityHit instanceof EntityLivingBase) {
                        EntityLivingBase target = (EntityLivingBase) mop.entityHit;

                        // Add Scorch Mark
                        MarkManager.apply(target, ScorchMarkType.ID, 1, getThrower());

                        // If bullet has fire aspect (isBurning), detonate immediately
                        if (this.isBurning()) {
                            com.greyhat.dark_grey.mark.api.IMarkType type = com.greyhat.dark_grey.mark.MarkRegistry
                                .get(ScorchMarkType.ID);
                            if (type instanceof ScorchMarkType) {
                                ((ScorchMarkType) type).detonate(target, getThrower(), 1.0f);
                            }
                        }
                    }
                }
            }

            this.setDead();
        }
    }
}
