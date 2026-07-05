// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey.entity;

import net.minecraft.nbt.NBTTagCompound;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;

public class EntityScythe extends Entity
{
    private EntityLivingBase owner;
    
    public EntityScythe(final World worldIn) {
        super(worldIn);
        this.setSize(1.0f, 1.0f);
    }
    
    public EntityScythe(final World worldIn, final EntityLivingBase owner) {
        super(worldIn);
        this.owner = owner;
        this.setSize(1.0f, 1.0f);
        this.setPosition(owner.posX, owner.posY + owner.getEyeHeight() / 2.0f, owner.posZ);
    }
    
    protected void entityInit() {
    }
    
    public void onUpdate() {
        super.onUpdate();
        if (this.owner != null && !this.owner.isDead) {
            this.setPosition(this.owner.posX, this.owner.posY + this.owner.getEyeHeight() / 2.0f, this.owner.posZ);
        }
        if (this.worldObj.isRemote && this.owner != null) {
            final float current_angle = this.ticksExisted / 6.0f * 3.1415927f * 2.0f;
            final double radius = 3.0;
            for (int i = 0; i < 15; ++i) {
                final float angle_offset = current_angle - i * 0.1f;
                final double px = this.posX + Math.sin(angle_offset) * radius;
                final double pz = this.posZ + Math.cos(angle_offset) * radius;
                this.worldObj.spawnParticle("reddust", px, this.posY, pz, -1.0, 0.0, 0.0);
                if (i % 3 == 0) {
                    this.worldObj.spawnParticle("magicCrit", px, this.posY, pz, 0.0, 0.0, 0.0);
                    this.worldObj.spawnParticle("flame", px, this.posY, pz, 0.0, 0.0, 0.0);
                }
            }
        }
        if (!this.worldObj.isRemote) {
            if (this.ticksExisted == 3) {
                final double radius2 = 3.0;
                final AxisAlignedBB aabb = this.boundingBox.expand(radius2, radius2, radius2);
                final List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity)this.owner, aabb);
                final DamageSource source = (DamageSource)((this.owner != null) ? new EntityDamageSource("player", (Entity)this.owner) : DamageSource.magic);
                final float damage = 35.0f;
                for (final Entity entity : list) {
                    if (entity instanceof EntityLivingBase && entity != this.owner && this.getDistanceSqToEntity(entity) <= radius2 * radius2) {
                        entity.attackEntityFrom(source, damage);
                    }
                }
            }
            if (this.ticksExisted >= 6) {
                this.setDead();
            }
        }
    }
    
    protected void readEntityFromNBT(final NBTTagCompound compound) {
        this.ticksExisted = compound.getInteger("TicksAlive");
    }
    
    protected void writeEntityToNBT(final NBTTagCompound compound) {
        compound.setInteger("TicksAlive", this.ticksExisted);
    }
}
