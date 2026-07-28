package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;

public class EntityStarBullet extends EntityThrowable {

    private float customDamage;
    private boolean isHeavyStrike;
    private EntityLivingBase homingTarget;

    public EntityStarBullet(World world) {
        super(world);
        this.customDamage = 1.0f;
        this.isHeavyStrike = false;
    }

    public EntityStarBullet(World world, EntityLivingBase shooter, EntityLivingBase target, float damage,
        boolean isHeavyStrike) {
        super(world, shooter);
        this.homingTarget = target;
        this.customDamage = damage;
        this.isHeavyStrike = isHeavyStrike;
        this.dataWatcher.updateObject(20, Byte.valueOf((byte) (isHeavyStrike ? 1 : 0)));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(20, Byte.valueOf((byte) 0));
    }

    @Override
    public void onUpdate() {
        if (this.worldObj.isRemote) {
            this.isHeavyStrike = this.dataWatcher.getWatchableObjectByte(20) != 0;
        }
        super.onUpdate();

        if (this.ticksExisted > 100) {
            this.setDead();
            return;
        }

        if (!this.worldObj.isRemote) {
            // Homing logic (fixed target)
            if (this.homingTarget != null && !this.homingTarget.isDead) {
                double targetX = this.homingTarget.posX;
                double targetY = this.homingTarget.boundingBox.minY + (this.homingTarget.height / 2.0F);
                double targetZ = this.homingTarget.posZ;

                double dx = targetX - this.posX;
                double dy = targetY - this.posY;
                double dz = targetZ - this.posZ;

                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0) {
                    double speed = 1.2;
                    this.motionX = (dx / dist) * speed;
                    this.motionY = (dy / dist) * speed;
                    this.motionZ = (dz / dist) * speed;
                }
            }
        } else {
            // Spawn particles
            for (int i = 0; i < 3; i++) {
                double px = this.posX + (this.rand.nextDouble() - 0.5) * 0.3;
                double py = this.posY + (this.rand.nextDouble() - 0.5) * 0.3;
                double pz = this.posZ + (this.rand.nextDouble() - 0.5) * 0.3;
                if (this.isHeavyStrike) {
                    this.worldObj.spawnParticle("reddust", px, py, pz, 1.0, 0.2, 0.2);
                } else {
                    this.worldObj.spawnParticle("magicCrit", px, py, pz, 0.0, 0.0, 0.0);
                    this.worldObj.spawnParticle("fireworksSpark", px, py, pz, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F; // No gravity for stars
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (!this.worldObj.isRemote) {
            if (mop.entityHit != null) {
                EntityLivingBase shooter = this.getThrower();
                if (mop.entityHit instanceof EntityLivingBase && shooter != null
                    && CombatTargeting.canDamage(shooter, (EntityLivingBase) mop.entityHit, false)) {
                    EntityLivingBase target = (EntityLivingBase) mop.entityHit;
                    DamageSource source = RPGDamageSources.causeArrowDamage(this, shooter);
                    double originalMotionX = target.motionX;
                    double originalMotionY = target.motionY;
                    double originalMotionZ = target.motionZ;
                    boolean originalIsAirBorne = target.isAirBorne;
                    boolean originalVelocityChanged = target.velocityChanged;
                    try {
                        RPGDamageSources.dealDamageWithoutInvulnerability(target, source, this.customDamage);
                    } finally {
                        target.motionX = originalMotionX;
                        target.motionY = originalMotionY;
                        target.motionZ = originalMotionZ;
                        target.isAirBorne = originalIsAirBorne;
                        target.velocityChanged = originalVelocityChanged;
                    }
                }
            }

            // Explosion particles
            this.worldObj.setEntityState(this, (byte) 17);
            this.setDead();
        }
    }

    @Override
    public void handleHealthUpdate(byte state) {
        if (state == 17) {
            for (int i = 0; i < 8; ++i) {
                this.worldObj.spawnParticle(
                    "fireworksSpark",
                    this.posX,
                    this.posY,
                    this.posZ,
                    this.rand.nextGaussian() * 0.1,
                    this.rand.nextGaussian() * 0.1,
                    this.rand.nextGaussian() * 0.1);
            }
            return;
        }
        super.handleHealthUpdate(state);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setFloat("CustomDamage", this.customDamage);
        nbt.setBoolean("IsHeavyStrike", this.isHeavyStrike);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.customDamage = nbt.getFloat("CustomDamage");
        this.isHeavyStrike = nbt.getBoolean("IsHeavyStrike");
        this.dataWatcher.updateObject(20, Byte.valueOf((byte) (this.isHeavyStrike ? 1 : 0)));
        if (!this.worldObj.isRemote) {
            // The fixed homing target cannot be reconstructed safely after a chunk reload.
            this.setDead();
        }
    }
}
