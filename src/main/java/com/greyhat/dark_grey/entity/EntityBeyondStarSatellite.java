package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.RPGDamageSources;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

public class EntityBeyondStarSatellite extends EntityThrowable implements IEntityAdditionalSpawnData {

    private float customDamage;
    private EntityLivingBase homingTarget;
    private int lifetimeTicks = 160;

    public EntityBeyondStarSatellite(World world) {
        super(world);
        this.customDamage = 1.0f;
    }

    public EntityBeyondStarSatellite(World world, EntityLivingBase shooter, EntityLivingBase target, float damage) {
        this(world, shooter, target, damage, 160);
    }

    public EntityBeyondStarSatellite(World world, EntityLivingBase shooter, EntityLivingBase target, float damage,
        int lifetimeTicks) {
        super(world, shooter);
        this.homingTarget = target;
        this.customDamage = damage;
        this.lifetimeTicks = Math.max(1, Math.min(12000, lifetimeTicks));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.ticksExisted > this.lifetimeTicks) {
            this.setDead();
            return;
        }

        if (!this.worldObj.isRemote) {
            if (this.homingTarget != null && !this.homingTarget.isDead) {
                double targetX = this.homingTarget.posX;
                double targetY = this.homingTarget.boundingBox.minY + (this.homingTarget.height / 2.0F);
                double targetZ = this.homingTarget.posZ;

                double dx = targetX - this.posX;
                double dy = targetY - this.posY;
                double dz = targetZ - this.posZ;

                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // Detonate when close
                if (dist < this.homingTarget.width + 0.8) {
                    EntityLivingBase shooter = this.getThrower();
                    if (shooter != null) {
                        DamageSource source = RPGDamageSources.causeBeyondStarSatelliteDamage(this, shooter);
                        double originalMotionX = this.homingTarget.motionX;
                        double originalMotionY = this.homingTarget.motionY;
                        double originalMotionZ = this.homingTarget.motionZ;
                        boolean originalIsAirBorne = this.homingTarget.isAirBorne;
                        boolean originalVelocityChanged = this.homingTarget.velocityChanged;
                        try {
                            RPGDamageSources
                                .dealIndependentProjectileDamage(this.homingTarget, source, this.customDamage);
                        } finally {
                            this.homingTarget.motionX = originalMotionX;
                            this.homingTarget.motionY = originalMotionY;
                            this.homingTarget.motionZ = originalMotionZ;
                            this.homingTarget.isAirBorne = originalIsAirBorne;
                            this.homingTarget.velocityChanged = originalVelocityChanged;
                        }
                    }
                    this.worldObj.setEntityState(this, (byte) 17);
                    this.setDead();
                    return;
                }

                if (dist > 0) {
                    // Slower direct homing so it's visible in melee range
                    double speed = 0.15;
                    double turnRate = 0.4; // Smooth but snappy turn rate

                    double idealMx = (dx / dist) * speed;
                    double idealMy = (dy / dist) * speed;
                    double idealMz = (dz / dist) * speed;

                    this.motionX += (idealMx - this.motionX) * turnRate;
                    this.motionY += (idealMy - this.motionY) * turnRate;
                    this.motionZ += (idealMz - this.motionZ) * turnRate;
                }
            } else {
                // If the target is dead, just continue flying straight until despawn (160 ticks).
                // Or if we want it to die gracefully without exploding, we just let it fly.
                // Do not call setDead() immediately to prevent mid-air spontaneous explosions.
            }
        } else {
            // Spawn particles
            for (int i = 0; i < 3; i++) {
                double px = this.posX + (this.rand.nextDouble() - 0.5) * 0.3;
                double py = this.posY + (this.rand.nextDouble() - 0.5) * 0.3;
                double pz = this.posZ + (this.rand.nextDouble() - 0.5) * 0.3;
                this.worldObj.spawnParticle("flame", px, py, pz, 0.0, 0.0, 0.0);
                this.worldObj.spawnParticle("largesmoke", px, py, pz, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F; // No gravity
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        // Do nothing! Let the satellite pass through all blocks and entities.
        // It will only detonate when it gets close to the homing target in onUpdate().
    }

    @Override
    public void handleHealthUpdate(byte state) {
        if (state == 17) {
            for (int i = 0; i < 15; ++i) {
                this.worldObj.spawnParticle(
                    "largeexplode",
                    this.posX + this.rand.nextGaussian() * 0.3,
                    this.posY + this.rand.nextGaussian() * 0.3,
                    this.posZ + this.rand.nextGaussian() * 0.3,
                    0,
                    0,
                    0);
            }
            return;
        }
        super.handleHealthUpdate(state);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setFloat("CustomDamage", this.customDamage);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.customDamage = nbt.getFloat("CustomDamage");
        if (!this.worldObj.isRemote) {
            this.setDead();
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(this.lifetimeTicks);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        this.lifetimeTicks = Math.max(1, Math.min(12000, buffer.readInt()));
    }
}
