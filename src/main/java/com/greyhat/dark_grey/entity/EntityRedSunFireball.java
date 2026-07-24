package com.greyhat.dark_grey.entity;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.status.RedSunBurnData;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

public class EntityRedSunFireball extends Entity implements IEntityAdditionalSpawnData {

    public static final byte STATE_CHARGING = 0;
    public static final byte STATE_FLYING = 1;
    public static final byte STATE_ROLLING = 2;
    public static final byte STATE_STOPPED = 3;

    private UUID ownerUuid;
    private int ownerEntityId = -1;
    private byte state = STATE_CHARGING;
    private long chargeStartWorldTime;
    private int chargeTicks;
    private float chargeProgress;
    private float currentSize = 1.0F;
    private float currentDamage = 100.0F;

    private int maxChargeTicks = 180;
    private float minSize = 1.0F;
    private float maxSize = 12.0F;
    private float minDamage = 100.0F;
    private float maxDamage = 1250.0F;
    
    private float minProjectileSpeed = 0.5F;
    private float maxProjectileSpeed = 2.0F;
    
    private float projectileGravity = 0.03F;
    private float projectileDrag = 0.98F;
    private float projectileUpwardBoost = 0.12F;
    private int projectileLifetime = 200;
    private int burnDurationTicks = 200;
    
    private float volumeShrinkRate = 0.05F;
    private float maxExplosionRadius = 25.0F;

    private int flyingTicks;
    private int rollingTicks;
    private int stoppedTicks;
    private boolean impacted;
    private boolean playedMaxChargeSound = false;

    public EntityRedSunFireball(World world) {
        super(world);
        this.setSize(1.0F, 1.0F);
        this.noClip = true;
    }

    public EntityRedSunFireball(World world, EntityPlayer owner, int maxChargeTicks, float minSize, float maxSize,
        float minDamage, float maxDamage, float minProjectileSpeed, float maxProjectileSpeed, float projectileGravity, float projectileDrag,
        float projectileUpwardBoost, int projectileLifetime, int burnDurationTicks, float volumeShrinkRate, float maxExplosionRadius) {
        this(world);
        this.ownerUuid = owner.getUniqueID();
        this.ownerEntityId = owner.getEntityId();
        this.chargeStartWorldTime = world.getTotalWorldTime();

        this.maxChargeTicks = maxChargeTicks;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.minProjectileSpeed = minProjectileSpeed;
        this.maxProjectileSpeed = maxProjectileSpeed;
        this.projectileGravity = projectileGravity;
        this.projectileDrag = projectileDrag;
        this.projectileUpwardBoost = projectileUpwardBoost;
        this.projectileLifetime = projectileLifetime;
        this.burnDurationTicks = burnDurationTicks;
        this.volumeShrinkRate = volumeShrinkRate;
        this.maxExplosionRadius = maxExplosionRadius;

        this.currentSize = minSize;
        this.currentDamage = minDamage;
        this.setSize(currentSize, currentSize);

        updatePositionToOwner(owner);
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(16, (byte) STATE_CHARGING);
        this.dataWatcher.addObject(17, -1);
        this.dataWatcher.addObject(18, 0.0F);
        this.dataWatcher.addObject(19, 1.0F);
    }

    public byte getState() {
        return state;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public float getCurrentSize() {
        return currentSize;
    }

    public float getChargeProgress() {
        return chargeProgress;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!worldObj.isRemote) {
            this.dataWatcher.updateObject(16, this.state);
            this.dataWatcher.updateObject(17, this.ownerEntityId);
            this.dataWatcher.updateObject(18, this.chargeProgress);

            float oldSize = this.dataWatcher.getWatchableObjectFloat(19);
            if (Math.abs(oldSize - currentSize) >= 0.05F || this.ticksExisted % 2 == 0) {
                this.dataWatcher.updateObject(19, this.currentSize);
            }
        } else {
            this.state = this.dataWatcher.getWatchableObjectByte(16);
            this.ownerEntityId = this.dataWatcher.getWatchableObjectInt(17);
            this.chargeProgress = this.dataWatcher.getWatchableObjectFloat(18);
            this.currentSize = this.dataWatcher.getWatchableObjectFloat(19);
            this.setSize(this.currentSize, this.currentSize);
        }

        if (state == STATE_CHARGING) {
            onChargingUpdate();
        } else if (state == STATE_FLYING) {
            onFlyingUpdate();
        } else if (state == STATE_ROLLING) {
            onRollingUpdate();
        } else if (state == STATE_STOPPED) {
            onStoppedUpdate();
        }
    }

    private void onChargingUpdate() {
        if (!worldObj.isRemote) {
            Entity owner = worldObj.getEntityByID(ownerEntityId);
            if (owner instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) owner;
                if (!player.isEntityAlive() || player.isDead || !player.isUsingItem()) {
                    this.setDead();
                    return;
                }

                chargeTicks = (int) Math.min(maxChargeTicks, worldObj.getTotalWorldTime() - chargeStartWorldTime);
                chargeProgress = MathHelper.clamp_float((float) chargeTicks / maxChargeTicks, 0.0F, 1.0F);
                currentSize = minSize + (maxSize - minSize) * chargeProgress;
                currentDamage = minDamage + (maxDamage - minDamage) * chargeProgress;
                this.setSize(currentSize, currentSize);
                
                if (chargeProgress >= 1.0F && !playedMaxChargeSound) {
                    playedMaxChargeSound = true;
                    worldObj.playSoundAtEntity(player, "random.orb", 1.0F, 1.2F);
                }

                updatePositionToOwner(player);
            } else {
                if (this.ticksExisted > 20) {
                    this.setDead();
                }
            }
        } else {
            if (this.chargeProgress >= 1.0F) {
                double px = this.posX + (this.rand.nextFloat() - 0.5D) * this.currentSize * 1.5;
                double py = this.posY + (this.rand.nextFloat() - 0.5D) * this.currentSize * 1.5;
                double pz = this.posZ + (this.rand.nextFloat() - 0.5D) * this.currentSize * 1.5;
                this.worldObj.spawnParticle("flame", px, py, pz, 0.0D, 0.05D, 0.0D);
                
                if (this.ticksExisted % 3 == 0) {
                    this.worldObj.spawnParticle("lava", px, py, pz, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private void updatePositionToOwner(EntityPlayer player) {
        Vec3 look = player.getLookVec();
        double distance = 1.0 + currentSize * 0.55;

        double targetX = player.posX + look.xCoord * distance;
        double targetY = player.posY + player.getEyeHeight() + look.yCoord * distance;
        double targetZ = player.posZ + look.zCoord * distance;

        double dX = targetX - this.posX;
        double dY = targetY - this.posY;
        double dZ = targetZ - this.posZ;

        if (dX * dX + dY * dY + dZ * dZ > 144.0D) {
            this.setPosition(targetX, targetY, targetZ);
        } else {
            this.posX += dX * 0.45D;
            this.posY += dY * 0.45D;
            this.posZ += dZ * 0.45D;
            this.setPosition(this.posX, this.posY, this.posZ);
        }
    }

    public void launch(Vec3 look) {
        this.state = STATE_FLYING;
        this.noClip = false;
        this.flyingTicks = 0;
        this.impacted = false;

        float speedRatio = 1.0F;
        if (maxSize > minSize) {
            speedRatio = 1.0F - ((currentSize - minSize) / (maxSize - minSize)); // 1.0 at minSize, 0.0 at maxSize
        }
        float actualSpeed = minProjectileSpeed + (maxProjectileSpeed - minProjectileSpeed) * speedRatio;

        look = look.normalize();
        this.motionX = look.xCoord * actualSpeed;
        this.motionY = look.yCoord * actualSpeed + projectileUpwardBoost;
        this.motionZ = look.zCoord * actualSpeed;
    }

    private void onFlyingUpdate() {
        if (flyingTicks > projectileLifetime) {
            if (!worldObj.isRemote) triggerImpact();
            return;
        }
        
        Vec3 oldPos = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
        Vec3 newPos = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        
        net.minecraft.util.MovingObjectPosition mop = this.worldObj.func_147447_a(oldPos, newPos, false, true, false);
        if (mop != null) {
            newPos = Vec3.createVectorHelper(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
        }
        
        if (!worldObj.isRemote) {
            doRollingDamage();
        }

        if (mop != null) {
            if (!worldObj.isRemote) {
                if (mop.sideHit == 1) { // Hit top of block (floor) -> start rolling
                    this.state = STATE_ROLLING;
                    this.motionY = 0.0;
                    this.posY = mop.hitVec.yCoord + 0.01;
                    this.setPosition(this.posX, this.posY, this.posZ);
                } else { // Hit side wall or ceiling -> explode immediately
                    triggerImpact();
                }
            }
            return;
        }
        
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);
        
        this.motionX *= projectileDrag;
        this.motionY *= projectileDrag;
        this.motionZ *= projectileDrag;
        this.motionY -= projectileGravity;
        
        this.flyingTicks++;

        if (worldObj.isRemote) {
            worldObj.spawnParticle("flame", this.posX, this.posY, this.posZ, 0, 0, 0);
        }
    }

    private void onRollingUpdate() {
        if (!worldObj.isRemote) {
            this.stepHeight = this.currentSize / 2.0F; // Climb blocks shorter than radius!
            
            this.motionY -= projectileGravity * 2.0D; // Stronger gravity for ground stickiness
            
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            
            if (this.isCollidedHorizontally) {
                triggerImpact(); // Explode if hit a wall taller than stepHeight
                return;
            }
            
            this.motionX *= 0.96D; // Rolling drag
            this.motionZ *= 0.96D;
            
            this.rollingTicks++;
            
            this.currentSize -= volumeShrinkRate; 
            
            if (this.currentSize <= 0.1F) {
                this.setDead(); // Vanish silently
                return;
            }
            
            this.setSize(currentSize, currentSize);
            
            double speedSq = this.motionX * this.motionX + this.motionZ * this.motionZ;
            if (speedSq < 0.0001D && this.currentSize > 0.5F) {
                this.state = STATE_STOPPED;
                this.motionX = 0;
                this.motionZ = 0;
            }
            
            doRollingDamage();
        } else {
            // Spawn trail particles at the bottom
            double py = this.boundingBox.minY + 0.1;
            double px = this.posX + (this.rand.nextFloat() - 0.5) * this.currentSize * 0.9;
            double pz = this.posZ + (this.rand.nextFloat() - 0.5) * this.currentSize * 0.9;
            worldObj.spawnParticle("flame", px, py, pz, 0, 0.05, 0);
            worldObj.spawnParticle("largesmoke", px, py, pz, 0, 0.0, 0);
            worldObj.spawnParticle("townaura", px, py, pz, 0, 0.0, 0);
            if (this.ticksExisted % 2 == 0) {
                worldObj.spawnParticle("lava", px, py, pz, 0, 0, 0);
            }
        }
    }

    private void doRollingDamage() {
        AxisAlignedBB aoe = this.boundingBox.expand(0.5D, 0.0D, 0.5D);
        List<EntityLivingBase> targets = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aoe);
        
        Entity owner = worldObj.getEntityByID(ownerEntityId);
        EntityLivingBase ownerLiving = owner instanceof EntityLivingBase ? (EntityLivingBase) owner : null;
        DamageSource ds = RPGDamageSources.causeRedSunFireballDamage(this, ownerLiving);
        
        for (EntityLivingBase target : targets) {
            if (ownerLiving != null && !CombatTargeting.canDamage(ownerLiving, target, false)) {
                continue;
            }
            if (target.getEntityId() == this.ownerEntityId) continue;
            
            boolean damaged = target.attackEntityFrom(ds, currentDamage);
            if (damaged) {
                if (ownerLiving != null) RedSunBurnData.apply(target, ownerLiving, burnDurationTicks);
                // Knockback
                Vec3 dir = Vec3.createVectorHelper(target.posX - this.posX, 0, target.posZ - this.posZ);
                if (dir.lengthVector() > 0) dir = dir.normalize();
                target.addVelocity(dir.xCoord * 1.5D, 0.5D, dir.zCoord * 1.5D);
            }
        }
    }

    private void onStoppedUpdate() {
        if (!worldObj.isRemote) {
            this.stoppedTicks++;
            if (this.stoppedTicks >= 60) {
                triggerImpact();
            }
        } else {
            double py = this.posY + (this.rand.nextFloat() - 0.5) * this.currentSize;
            double px = this.posX + (this.rand.nextFloat() - 0.5) * this.currentSize;
            double pz = this.posZ + (this.rand.nextFloat() - 0.5) * this.currentSize;
            worldObj.spawnParticle("smoke", px, py, pz, 0, 0.1, 0);
            worldObj.spawnParticle("flame", px, py, pz, 0, 0.1, 0);
        }
    }

    private void triggerImpact() {
        if (impacted) return;
        impacted = true;

        if (!worldObj.isRemote) {
            double explosionRadius = currentSize * (maxExplosionRadius / maxSize);
            if (explosionRadius < 0.1) {
                this.setDead();
                return;
            }
            
            AxisAlignedBB aoe = AxisAlignedBB.getBoundingBox(
                this.posX - explosionRadius,
                this.posY - explosionRadius,
                this.posZ - explosionRadius,
                this.posX + explosionRadius,
                this.posY + explosionRadius,
                this.posZ + explosionRadius);

            Entity owner = worldObj.getEntityByID(ownerEntityId);
            EntityLivingBase ownerLiving = owner instanceof EntityLivingBase ? (EntityLivingBase) owner : null;

            List<EntityLivingBase> targets = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aoe);
            DamageSource ds = RPGDamageSources.causeRedSunFireballDamage(this, ownerLiving);

            for (EntityLivingBase target : targets) {
                if (ownerLiving != null && !CombatTargeting.canDamage(ownerLiving, target, false)) {
                    continue;
                }

                double dx = target.posX - this.posX;
                double dy = (target.posY + target.height / 2.0) - this.posY;
                double dz = target.posZ - this.posZ;
                if (dx * dx + dy * dy + dz * dz > explosionRadius * explosionRadius) {
                    continue;
                }

                if (target.attackEntityFrom(ds, currentDamage)) {
                    if (ownerLiving != null) {
                        RedSunBurnData.apply(target, ownerLiving, burnDurationTicks);
                    }
                }
            }

            worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.explode", 4.0F, 1.0F);
            if (worldObj instanceof net.minecraft.world.WorldServer) {
                ((net.minecraft.world.WorldServer) worldObj)
                    .func_147487_a("hugeexplosion", this.posX, this.posY, this.posZ, (int)(3 * currentSize), 1.0, 1.0, 1.0, 0.1);
                
                ((net.minecraft.world.WorldServer) worldObj).func_147487_a(
                    "flame",
                    this.posX,
                    this.posY,
                    this.posZ,
                    (int) (50 * currentSize),
                    explosionRadius * 0.8,
                    explosionRadius * 0.8,
                    explosionRadius * 0.8,
                    0.3);

                ((net.minecraft.world.WorldServer) worldObj).func_147487_a(
                    "lava",
                    this.posX,
                    this.posY,
                    this.posZ,
                    (int) (15 * currentSize),
                    explosionRadius * 0.5,
                    explosionRadius * 0.5,
                    explosionRadius * 0.5,
                    0.5);
            }

            this.setDead();
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        state = nbt.getByte("State");
        chargeStartWorldTime = nbt.getLong("ChargeStart");
        chargeProgress = nbt.getFloat("ChargeProgress");
        currentSize = nbt.getFloat("CurrentSize");
        currentDamage = nbt.getFloat("CurrentDamage");
        ownerEntityId = nbt.getInteger("OwnerEntityId");
        if (nbt.hasKey("OwnerUUIDMost") && nbt.hasKey("OwnerUUIDLeast")) {
            ownerUuid = new UUID(nbt.getLong("OwnerUUIDMost"), nbt.getLong("OwnerUUIDLeast"));
        }
        maxChargeTicks = nbt.getInteger("MaxChargeTicks");
        minSize = nbt.getFloat("MinSize");
        maxSize = nbt.getFloat("MaxSize");
        minDamage = nbt.getFloat("MinDamage");
        maxDamage = nbt.getFloat("MaxDamage");
        minProjectileSpeed = nbt.getFloat("MinProjSpeed");
        maxProjectileSpeed = nbt.getFloat("MaxProjSpeed");
        projectileGravity = nbt.getFloat("ProjGravity");
        projectileDrag = nbt.getFloat("ProjDrag");
        projectileUpwardBoost = nbt.getFloat("ProjUpward");
        projectileLifetime = nbt.getInteger("ProjLife");
        flyingTicks = nbt.getInteger("FlyingTicks");
        rollingTicks = nbt.getInteger("RollingTicks");
        stoppedTicks = nbt.getInteger("StoppedTicks");
        burnDurationTicks = nbt.getInteger("BurnDuration");
        volumeShrinkRate = nbt.getFloat("VolShrink");
        maxExplosionRadius = nbt.getFloat("MaxExpRad");
        playedMaxChargeSound = nbt.getBoolean("PlayedMaxDing");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setByte("State", state);
        nbt.setLong("ChargeStart", chargeStartWorldTime);
        nbt.setFloat("ChargeProgress", chargeProgress);
        nbt.setFloat("CurrentSize", currentSize);
        nbt.setFloat("CurrentDamage", currentDamage);
        nbt.setInteger("OwnerEntityId", ownerEntityId);
        if (ownerUuid != null) {
            nbt.setLong("OwnerUUIDMost", ownerUuid.getMostSignificantBits());
            nbt.setLong("OwnerUUIDLeast", ownerUuid.getLeastSignificantBits());
        }
        nbt.setInteger("MaxChargeTicks", maxChargeTicks);
        nbt.setFloat("MinSize", minSize);
        nbt.setFloat("MaxSize", maxSize);
        nbt.setFloat("MinDamage", minDamage);
        nbt.setFloat("MaxDamage", maxDamage);
        nbt.setFloat("MinProjSpeed", minProjectileSpeed);
        nbt.setFloat("MaxProjSpeed", maxProjectileSpeed);
        nbt.setFloat("ProjGravity", projectileGravity);
        nbt.setFloat("ProjDrag", projectileDrag);
        nbt.setFloat("ProjUpward", projectileUpwardBoost);
        nbt.setInteger("ProjLife", projectileLifetime);
        nbt.setInteger("FlyingTicks", flyingTicks);
        nbt.setInteger("RollingTicks", rollingTicks);
        nbt.setInteger("StoppedTicks", stoppedTicks);
        nbt.setInteger("BurnDuration", burnDurationTicks);
        nbt.setFloat("VolShrink", volumeShrinkRate);
        nbt.setFloat("MaxExpRad", maxExplosionRadius);
        nbt.setBoolean("PlayedMaxDing", playedMaxChargeSound);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(ownerEntityId);
        buffer.writeByte(state);
        buffer.writeFloat(chargeProgress);
        buffer.writeFloat(currentSize);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        ownerEntityId = additionalData.readInt();
        state = additionalData.readByte();
        chargeProgress = additionalData.readFloat();
        currentSize = additionalData.readFloat();
        this.setSize(currentSize, currentSize);
    }
}
