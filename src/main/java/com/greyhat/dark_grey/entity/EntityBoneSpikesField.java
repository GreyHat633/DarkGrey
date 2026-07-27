package com.greyhat.dark_grey.entity;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.type.FractureMarkType;

public class EntityBoneSpikesField extends Entity {

    private EntityLivingBase thrower;
    private UUID throwerUuid;
    public float lingeringDamage = 2.0F;
    public int fieldDuration = 1200;
    public int fractureStableDurationTicks = 100;

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
        this.throwerUuid = thrower == null ? null : thrower.getUniqueID();
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

        EntityLivingBase owner = this.resolveThrower();
        if (owner == null) {
            this.setDead();
            return;
        }

        // Server side logic
        List<EntityLivingBase> entities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.boundingBox);
        int bitmask = this.dataWatcher.getWatchableObjectInt(16);
        if (bitmask == 0) {
            this.setDead();
            return;
        }
        boolean changed = false;

        for (EntityLivingBase entity : entities) {
            if (!CombatTargeting.canDamage(owner, entity, false)) continue;

            // Calculate relative grid position
            int relX = (int) Math.round(entity.posX - this.posX);
            int relZ = (int) Math.round(entity.posZ - this.posZ);

            if (relX >= -1 && relX <= 1 && relZ >= -1 && relZ <= 1) {
                int index = (relX + 1) + (relZ + 1) * 3;
                if ((bitmask & (1 << index)) != 0) {
                    // Apply damage and mark
                    DamageSource source = DamageSource.causeIndirectMagicDamage(this, owner);
                    boolean damaged = entity.attackEntityFrom(source, this.lingeringDamage);
                    if (!damaged) {
                        continue;
                    }
                    MarkApplyContext context = new MarkApplyContext.Builder().source(owner)
                        .requestedStacks(1)
                        .worldTime(this.worldObj.getTotalWorldTime())
                        .applicationId("bone_flask_spike")
                        .refreshDuration(true)
                        .triggerImmediate(true)
                        .stableDurationTicks(this.fractureStableDurationTicks)
                        .build();
                    MarkManager.apply(entity, FractureMarkType.ID, context);

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
            if (bitmask == 0) {
                this.setDead();
            }
        }
    }

    private EntityLivingBase resolveThrower() {
        if (this.thrower != null && !this.thrower.isDead
            && (this.throwerUuid == null || this.throwerUuid.equals(this.thrower.getUniqueID()))) {
            return this.thrower;
        }
        this.thrower = null;
        if (this.throwerUuid != null && this.worldObj instanceof WorldServer) {
            Entity resolved = ((WorldServer) this.worldObj).func_152378_a(this.throwerUuid);
            if (resolved instanceof EntityLivingBase && !resolved.isDead) {
                this.thrower = (EntityLivingBase) resolved;
            }
        }
        return this.thrower;
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
        if (nbt.hasKey("FractureStableDurationTicks")) {
            this.fractureStableDurationTicks = nbt.getInteger("FractureStableDurationTicks");
        }
        this.dataWatcher.updateObject(16, Integer.valueOf(nbt.getInteger("ActiveSpikesBitmask")));
        this.ticksExisted = nbt.getInteger("TicksExisted");
        if (nbt.hasKey("ThrowerUUID")) {
            try {
                this.throwerUuid = UUID.fromString(nbt.getString("ThrowerUUID"));
            } catch (IllegalArgumentException ignored) {
                this.throwerUuid = null;
            }
        }
        this.thrower = null;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setFloat("LingeringDamage", this.lingeringDamage);
        nbt.setInteger("FieldDuration", this.fieldDuration);
        nbt.setInteger("FractureStableDurationTicks", this.fractureStableDurationTicks);
        nbt.setInteger("ActiveSpikesBitmask", this.dataWatcher.getWatchableObjectInt(16));
        nbt.setInteger("TicksExisted", this.ticksExisted);
        if (this.throwerUuid != null) {
            nbt.setString("ThrowerUUID", this.throwerUuid.toString());
        }
    }
}
