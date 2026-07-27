package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.type.FractureMarkType;

public class EntityBoneSpikesField extends Entity {

    private EntityLivingBase thrower;
    public float lingeringDamage = 2.0F;
    public int fieldDuration = 1200;

    public EntityBoneSpikesField(World world) {
        super(world);
        this.setSize(3.0F, 1.0F); // 3x3 horizontal, 1 tall
        this.ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        // DataWatcher index 16 for bitmask of active spikes
        this.dataWatcher.addObject(16, Integer.valueOf(511)); // 511 = 9 bits all set to 1
    }

    public void setThrower(EntityLivingBase thrower) {
        this.thrower = thrower;
    }

    public EntityLivingBase getThrower() {
        return this.thrower;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.ticksExisted > this.fieldDuration) {
            if (!this.worldObj.isRemote) {
                this.setDead();
            }
            return;
        }

        if (this.worldObj.isRemote) {
            spawnParticles();
            return;
        }

        // Server side logic
        List<EntityLivingBase> entities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox);
        int bitmask = this.dataWatcher.getWatchableObjectInt(16);
        boolean changed = false;

        for (EntityLivingBase entity : entities) {
            if (entity == this.thrower) continue;

            // Calculate relative grid position
            int relX = (int) Math.round(entity.posX - this.posX);
            int relZ = (int) Math.round(entity.posZ - this.posZ);

            if (relX >= -1 && relX <= 1 && relZ >= -1 && relZ <= 1) {
                int index = (relX + 1) + (relZ + 1) * 3;
                if ((bitmask & (1 << index)) != 0) {
                    // Apply damage and mark
                    DamageSource source = this.thrower != null
                        ? DamageSource.causeIndirectMagicDamage(this, this.thrower)
                        : DamageSource.magic;

                    entity.attackEntityFrom(source, this.lingeringDamage);
                    MarkManager.apply(entity, FractureMarkType.ID, 1, this.thrower);

                    // Consume spike
                    bitmask &= ~(1 << index);
                    changed = true;

                    // Spawn extra particles to indicate consumption
                    this.worldObj
                        .playSoundEffect(entity.posX, entity.posY, entity.posZ, "mob.zombie.woodbreak", 0.5F, 1.5F);
                }
            }
        }

        if (changed) {
            this.dataWatcher.updateObject(16, Integer.valueOf(bitmask));
        }
    }

    private void spawnParticles() {
        int bitmask = this.dataWatcher.getWatchableObjectInt(16);
        for (int i = 0; i < 9; i++) {
            if ((bitmask & (1 << i)) != 0) {
                int count = 1 + this.rand.nextInt(2); // spawn 1 to 2 particles per cell every tick
                for (int p = 0; p < count; p++) {
                    int relX = (i % 3) - 1;
                    int relZ = (i / 3) - 1;
                    double px = this.posX + relX + (this.rand.nextDouble() - 0.5) * 0.8;
                    double pz = this.posZ + relZ + (this.rand.nextDouble() - 0.5) * 0.8;
                    double py = this.posY + 0.1; // slightly above ground

                    double vx = (this.rand.nextDouble() - 0.5) * 0.08;
                    double vy = 0.05 + this.rand.nextDouble() * 0.1;
                    double vz = (this.rand.nextDouble() - 0.5) * 0.08;

                    this.worldObj.spawnParticle("iconcrack_352", px, py, pz, vx, vy, vz);
                }
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.lingeringDamage = nbt.getFloat("LingeringDamage");
        this.fieldDuration = nbt.getInteger("FieldDuration");
        this.dataWatcher.updateObject(16, Integer.valueOf(nbt.getInteger("ActiveSpikesBitmask")));
        this.ticksExisted = nbt.getInteger("TicksExisted");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setFloat("LingeringDamage", this.lingeringDamage);
        nbt.setInteger("FieldDuration", this.fieldDuration);
        nbt.setInteger("ActiveSpikesBitmask", this.dataWatcher.getWatchableObjectInt(16));
        nbt.setInteger("TicksExisted", this.ticksExisted);
    }
}
