package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;

public class EntityBoneFlask extends EntityThrowable {

    public float directDamage = 12.0F;
    public float lingeringDamage = 2.0F;
    public int fieldDuration = 1200;
    public int fractureStableDurationTicks = 100;
    public float projectileGravity = 0.05F;

    public EntityBoneFlask(World world) {
        super(world);
    }

    public EntityBoneFlask(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityBoneFlask(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (!this.worldObj.isRemote) {
            if (mop.entityHit != null && mop.entityHit instanceof EntityLivingBase) {
                EntityLivingBase thrower = this.getThrower();
                // If the thrower is not null, attribute damage to them
                if (thrower != null && CombatTargeting.canDamage(thrower, (EntityLivingBase) mop.entityHit, false)) {
                    DamageSource source = DamageSource.causeThrownDamage(this, thrower);
                    mop.entityHit.attackEntityFrom(source, this.directDamage);
                }
            }

            double spawnX = mop.hitVec.xCoord;
            double spawnY = mop.hitVec.yCoord;
            double spawnZ = mop.hitVec.zCoord;

            if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                spawnY = mop.blockY + 1.0;
            }

            EntityBoneSpikesField field = new EntityBoneSpikesField(this.worldObj);
            field.setPosition(spawnX, spawnY, spawnZ);
            field.setThrower(this.getThrower());
            field.lingeringDamage = this.lingeringDamage;
            field.fieldDuration = this.fieldDuration;
            field.fractureStableDurationTicks = this.fractureStableDurationTicks;

            this.worldObj.spawnEntityInWorld(field);

            this.setDead();
        } else {
            // Spawn some glass break particles on client side
            for (int i = 0; i < 8; ++i) {
                this.worldObj.spawnParticle(
                    "iconcrack_373",
                    this.posX,
                    this.posY,
                    this.posZ,
                    this.rand.nextGaussian() * 0.15,
                    this.rand.nextDouble() * 0.2,
                    this.rand.nextGaussian() * 0.15);
            }
        }
    }

    @Override
    protected float getGravityVelocity() {
        return this.projectileGravity;
    }

    @Override
    public void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setFloat("DirectDamage", this.directDamage);
        nbt.setFloat("LingeringDamage", this.lingeringDamage);
        nbt.setInteger("FieldDuration", this.fieldDuration);
        nbt.setInteger("FractureStableDurationTicks", this.fractureStableDurationTicks);
        nbt.setFloat("ProjectileGravity", this.projectileGravity);
    }

    @Override
    public void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("DirectDamage")) this.directDamage = nbt.getFloat("DirectDamage");
        if (nbt.hasKey("LingeringDamage")) this.lingeringDamage = nbt.getFloat("LingeringDamage");
        if (nbt.hasKey("FieldDuration")) this.fieldDuration = nbt.getInteger("FieldDuration");
        if (nbt.hasKey("FractureStableDurationTicks")) {
            this.fractureStableDurationTicks = nbt.getInteger("FractureStableDurationTicks");
        }
        if (nbt.hasKey("ProjectileGravity")) this.projectileGravity = nbt.getFloat("ProjectileGravity");
    }
}
