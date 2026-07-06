package com.greyhat.dark_grey.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class EntityPhantomStrike extends Entity {
    private int ticksAlive = 0;
    private EntityLivingBase thrower;
    private float damage;

    public EntityPhantomStrike(World world) {
        super(world);
        this.setSize(1.5F, 1.5F);
    }

    public EntityPhantomStrike(World world, EntityLivingBase thrower, float damage) {
        super(world);
        this.thrower = thrower;
        this.damage = damage;
        this.setSize(1.5F, 1.5F);
        
        this.setLocationAndAngles(thrower.posX, thrower.posY + thrower.getEyeHeight() - 0.5, thrower.posZ, thrower.rotationYaw, thrower.rotationPitch);
        
        Vec3 look = thrower.getLookVec();
        double speed = 2.0; 
        this.motionX = look.xCoord * speed;
        this.motionY = look.yCoord * speed;
        this.motionZ = look.zCoord * speed;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.ticksAlive++;

        if (!this.worldObj.isRemote && this.ticksAlive > 50) {
            this.setDead();
            return;
        }

        // Use moveEntity to handle block collisions automatically
        this.moveEntity(this.motionX, this.motionY, this.motionZ);

        if (this.isCollidedHorizontally) {
            this.setDead();
            return;
        }

        if (!this.worldObj.isRemote) {
            // Apply damage and mark to entities in a wide AABB (cone simulation)
            AxisAlignedBB aabb = this.boundingBox.expand(1.5, 1.5, 1.5);
            @SuppressWarnings("unchecked")
            List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, aabb);
            
            for (Entity e : list) {
                if (e instanceof EntityLivingBase && e != this.thrower) {
                    EntityLivingBase target = (EntityLivingBase) e;
                    
                    DamageSource source = DamageSource.magic;
                    if (this.thrower instanceof EntityPlayer) {
                        // Use magic damage so it doesn't trigger the explosion logic which looks for standard player attack
                        source = DamageSource.causePlayerDamage((EntityPlayer) this.thrower).setMagicDamage();
                    }
                    
                    target.attackEntityFrom(source, this.damage);
                    
                    // Apply Scorched Mark
                    target.getEntityData().setInteger("ScorchedMarkTimer", 100);
                }
            }
        } else {
            // Client-side particles to simulate massive cone
            for (int i = 0; i < 15; i++) {
                double rx = this.posX + (this.rand.nextDouble() - 0.5) * 3.0;
                double ry = this.posY + (this.rand.nextDouble() - 0.5) * 3.0;
                double rz = this.posZ + (this.rand.nextDouble() - 0.5) * 3.0;
                this.worldObj.spawnParticle("flame", rx, ry, rz, this.motionX * 0.1, this.motionY * 0.1, this.motionZ * 0.1);
                this.worldObj.spawnParticle("reddust", rx, ry, rz, 0, 0, 0);
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.ticksAlive = nbt.getInteger("TicksAlive");
        this.damage = nbt.getFloat("Damage");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("TicksAlive", this.ticksAlive);
        nbt.setFloat("Damage", this.damage);
    }
}
