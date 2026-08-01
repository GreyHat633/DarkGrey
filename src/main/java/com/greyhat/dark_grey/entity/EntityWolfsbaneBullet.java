package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;

public class EntityWolfsbaneBullet extends EntityThrowable {

    private float customDamage;
    private boolean isHeavyStrike;

    public EntityWolfsbaneBullet(World world) {
        super(world);
        this.customDamage = 1.0f;
        this.isHeavyStrike = false;
    }

    public boolean isHeavyStrike() {
        return this.isHeavyStrike;
    }

    public EntityWolfsbaneBullet(World world, EntityLivingBase shooter, float damage, boolean isHeavyStrike) {
        super(world, shooter);
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

        if (this.worldObj.isRemote) {
            // Spawn particles
            for (int i = 0; i < 3; i++) {
                double px = this.posX + (this.rand.nextDouble() - 0.5) * 0.3;
                double py = this.posY + (this.rand.nextDouble() - 0.5) * 0.3;
                double pz = this.posZ + (this.rand.nextDouble() - 0.5) * 0.3;
                if (this.isHeavyStrike) {
                    this.worldObj.spawnParticle("reddust", px, py, pz, 1.0, 0.1, 0.1);
                    this.worldObj.spawnParticle("largesmoke", px, py, pz, 0.0, 0.0, 0.0);
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
            EntityLivingBase shooter = this.getThrower();
            if (this.isHeavyStrike) {
                double radius = 2.5;
                net.minecraft.util.AxisAlignedBB aabb = net.minecraft.util.AxisAlignedBB.getBoundingBox(
                    this.posX - radius,
                    this.posY - radius,
                    this.posZ - radius,
                    this.posX + radius,
                    this.posY + radius,
                    this.posZ + radius);

                java.util.List<EntityLivingBase> targets = this.worldObj
                    .getEntitiesWithinAABB(EntityLivingBase.class, aabb);
                DamageSource source = RPGDamageSources.causeArrowDamage(this, shooter);

                for (EntityLivingBase target : targets) {
                    if (target == shooter && shooter != null) continue;
                    if (shooter != null && !CombatTargeting.canDamage(shooter, target, false)) continue;
                    if (shooter == null && target instanceof net.minecraft.entity.player.EntityPlayer) continue;

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
            } else {
                if (mop.entityHit != null) {
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
            }

            // Explosion particles
            if (this.isHeavyStrike) {
                this.worldObj.setEntityState(this, (byte) 18);
            } else {
                this.worldObj.setEntityState(this, (byte) 17);
            }
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
        } else if (state == 18) {
            this.worldObj.spawnParticle("hugeexplosion", this.posX, this.posY, this.posZ, 1.0, 1.0, 1.0);
            for (int i = 0; i < 40; ++i) {
                this.worldObj.spawnParticle(
                    "flame",
                    this.posX + (this.rand.nextDouble() - 0.5) * 5.0,
                    this.posY + (this.rand.nextDouble() - 0.5) * 5.0,
                    this.posZ + (this.rand.nextDouble() - 0.5) * 5.0,
                    this.rand.nextGaussian() * 0.05,
                    this.rand.nextGaussian() * 0.05,
                    this.rand.nextGaussian() * 0.05);
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
    }
}
