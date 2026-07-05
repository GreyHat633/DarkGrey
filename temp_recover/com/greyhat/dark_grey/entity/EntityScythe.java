package com.greyhat.dark_grey.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.World;

import java.util.List;

public class EntityScythe extends Entity {

    private EntityLivingBase owner;

    public EntityScythe(World worldIn) {
        super(worldIn);
        this.setSize(1.0F, 1.0F);
    }

    public EntityScythe(World worldIn, EntityLivingBase owner) {
        super(worldIn);
        this.owner = owner;
        this.setSize(1.0F, 1.0F);
        this.setPosition(owner.posX, owner.posY + owner.getEyeHeight() / 2.0f, owner.posZ);
        this.dataWatcher.updateObject(20, owner.getEntityId());
    }

    @Override
    protected void entityInit() {
        // DataWatcher index 20 for owner entity ID (-1 means no owner yet)
        this.dataWatcher.addObject(20, Integer.valueOf(-1));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        
        if (this.worldObj.isRemote && this.owner == null) {
            int ownerId = this.dataWatcher.getWatchableObjectInt(20);
            if (ownerId != -1) {
                Entity e = this.worldObj.getEntityByID(ownerId);
                if (e instanceof EntityLivingBase) {
                    this.owner = (EntityLivingBase) e;
                }
            }
        }
        
        if (this.owner != null && !this.owner.isDead) {
            this.setPosition(this.owner.posX, this.owner.posY + this.owner.getEyeHeight() / 2.0f, this.owner.posZ);
        }

        // Add beautiful particle effects during the sweep
        if (this.worldObj.isRemote && this.owner != null) {
            float current_angle = (this.ticksExisted / 20.0f) * (float)Math.PI * 2.0f;
            double radius = 3.0;
            // Spawn multiple particles along the current arc segment
            for (int i = 0; i < 30; i++) {
                float angle_offset = current_angle - (i * 0.05f);
                double px = this.posX + Math.sin(angle_offset) * radius;
                double pz = this.posZ + Math.cos(angle_offset) * radius;
                
                // Dark red/black particles
                this.worldObj.spawnParticle("reddust", px, this.posY + (this.rand.nextDouble() - 0.5), pz, -1.0D, 0.0D, 0.0D);
                
                if (i % 2 == 0) {
                    this.worldObj.spawnParticle("largesmoke", px, this.posY + (this.rand.nextDouble() - 0.5), pz, 0.0D, 0.0D, 0.0D);
                    this.worldObj.spawnParticle("magicCrit", px, this.posY + (this.rand.nextDouble() - 0.5), pz, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        if (!this.worldObj.isRemote) {
            if (this.ticksExisted == 10) {
                // Perform sweep attack at halfway point to match animation
                double radius = 3.0;
                AxisAlignedBB aabb = this.boundingBox.expand(radius, radius, radius);
                List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this.owner, aabb);
                
                DamageSource source = this.owner != null ? new EntityDamageSource("player", this.owner) : DamageSource.generic;
                float damage = 35.0f; // Base damage of Calamity
                
                for (Entity entity : list) {
                    if (entity instanceof EntityLivingBase && entity != this.owner) {
                        if (this.getDistanceSqToEntity(entity) <= radius * radius) {
                            entity.attackEntityFrom(source, damage);
                        }
                    }
                }
            }
            
            if (this.ticksExisted >= 20) {
                this.setDead();
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.ticksExisted = compound.getInteger("TicksAlive");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("TicksAlive", this.ticksExisted);
    }
}