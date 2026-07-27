package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class EntityBoneFlask extends EntityThrowable {

    public float directDamage = 12.0F;
    public float lingeringDamage = 2.0F;
    public int fieldDuration = 1200;

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
                // If the thrower is not null, attribute damage to them
                DamageSource source = this.getThrower() != null
                    ? DamageSource.causeThrownDamage(this, this.getThrower())
                    : DamageSource.causeThrownDamage(this, this);

                mop.entityHit.attackEntityFrom(source, this.directDamage);
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
        return 0.05F; // similar to splash potion
    }
}
