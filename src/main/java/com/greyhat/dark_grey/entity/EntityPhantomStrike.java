package com.greyhat.dark_grey.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

public class EntityPhantomStrike extends Entity implements IEntityAdditionalSpawnData {

    private int ticksAlive = 0;
    private EntityLivingBase thrower;
    private float damage;

    // P1 #9: 去重 Set，防止每 tick 重复对同一敌人施加高额伤害
    private final Set<Integer> hitEntityIds = new HashSet<>();
    public double startX, startY, startZ;

    public EntityPhantomStrike(World world) {
        super(world);
        this.setSize(1.5F, 1.5F);
    }

    public EntityPhantomStrike(World world, EntityLivingBase thrower, float damage, Vec3 dashDir) {
        super(world);
        this.thrower = thrower;
        this.damage = damage;
        this.setSize(1.5F, 1.5F);

        this.setLocationAndAngles(
            thrower.posX,
            thrower.posY + thrower.getEyeHeight() - 0.5,
            thrower.posZ,
            thrower.rotationYaw,
            thrower.rotationPitch);

        this.startX = this.posX;
        this.startY = this.posY;
        this.startZ = this.posZ;

        double speed = 2.0;
        this.motionX = dashDir.xCoord * speed;
        this.motionY = dashDir.yCoord * speed;
        this.motionZ = dashDir.zCoord * speed;
    }

    public void excludeInitialTarget(EntityLivingBase target) {
        if (target != null) {
            this.hitEntityIds.add(target.getEntityId());
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeFloat(this.rotationYaw);
        buffer.writeFloat(this.rotationPitch);
        buffer.writeDouble(this.motionX);
        buffer.writeDouble(this.motionY);
        buffer.writeDouble(this.motionZ);
        buffer.writeDouble(this.startX);
        buffer.writeDouble(this.startY);
        buffer.writeDouble(this.startZ);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        this.rotationYaw = additionalData.readFloat();
        this.prevRotationYaw = this.rotationYaw;
        this.rotationPitch = additionalData.readFloat();
        this.prevRotationPitch = this.rotationPitch;
        this.motionX = additionalData.readDouble();
        this.motionY = additionalData.readDouble();
        this.motionZ = additionalData.readDouble();
        this.startX = additionalData.readDouble();
        this.startY = additionalData.readDouble();
        this.startZ = additionalData.readDouble();
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.ticksAlive++;

        // Linger duration: 15 ticks total, thrust for first 5 ticks
        if (this.ticksAlive > 15) {
            this.setDead();
            return;
        }

        if (this.ticksAlive <= 5) {
            // Use moveEntity to handle block collisions automatically
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
        }

        // --- Client Side Particle Explosion ---
        if (this.worldObj.isRemote && this.ticksAlive == 1) {
            // Generate a burst of particles thrusting forward in a cone shape
            Vec3 dir = Vec3.createVectorHelper(this.motionX, this.motionY, this.motionZ)
                .normalize();
            for (int i = 0; i < 150; i++) {
                double spread = 0.8;
                double vx = dir.xCoord * 1.8 + (this.rand.nextDouble() - 0.5) * spread;
                double vy = dir.yCoord * 1.8 + (this.rand.nextDouble() - 0.5) * spread;
                double vz = dir.zCoord * 1.8 + (this.rand.nextDouble() - 0.5) * spread;

                this.worldObj.spawnParticle("flame", this.startX, this.startY, this.startZ, vx, vy, vz);

                // Add some critical hit sparks and lava pops
                if (this.rand.nextInt(3) == 0) {
                    this.worldObj
                        .spawnParticle("crit", this.startX, this.startY, this.startZ, vx * 1.2, vy * 1.2, vz * 1.2);
                }
                if (this.rand.nextInt(5) == 0) {
                    this.worldObj
                        .spawnParticle("lava", this.startX, this.startY, this.startZ, vx * 0.5, vy * 0.5, vz * 0.5);
                }

                // Add reddust (red/orange)
                if (this.rand.nextInt(2) == 0) {
                    this.worldObj.spawnParticle("reddust", this.startX, this.startY, this.startZ, 0.0, 0.0, 0.0);
                }
            }
        }
        // --------------------------------------

        // P3: Remove isCollidedHorizontally check so it pierces through walls!

        if (!this.worldObj.isRemote && this.ticksAlive <= 5) {
            // Apply damage and mark to entities in a wide AABB (cone simulation)
            AxisAlignedBB aabb = this.boundingBox.expand(1.5, 1.5, 1.5);
            @SuppressWarnings("unchecked")
            List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, aabb);

            for (Entity e : list) {
                if (e instanceof EntityLivingBase
                    && CombatTargeting.canDamage(this.thrower, (EntityLivingBase) e, false)) {
                    EntityLivingBase target = (EntityLivingBase) e;

                    // 去重检查
                    if (hitEntityIds.contains(target.getEntityId())) {
                        continue;
                    }
                    hitEntityIds.add(target.getEntityId());

                    DamageSource source = RPGDamageSources.causeCasterMagicDamage(this.thrower);
                    if (source == null || !target.attackEntityFrom(source, this.damage)) {
                        continue;
                    }

                    // Apply Scorched Mark to tracking system
                    com.greyhat.dark_grey.event.ScorchedMarkTracker.mark(target, 100);
                }
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.ticksAlive = nbt.getInteger("TicksAlive");
        this.damage = nbt.getFloat("Damage");
        // P2 #17: 跨存档加载时由于 thrower 无法保存，直接销毁，防空指针和机制错乱
        this.setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("TicksAlive", this.ticksAlive);
        nbt.setFloat("Damage", this.damage);
    }
}
