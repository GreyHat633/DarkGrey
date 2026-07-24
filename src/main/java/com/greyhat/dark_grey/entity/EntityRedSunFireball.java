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

    private UUID ownerUuid;
    private int ownerEntityId = -1;
    private byte state = STATE_CHARGING;
    private long chargeStartWorldTime;
    private int chargeTicks;
    private float chargeProgress;
    private float currentSize = 1.0F;
    private float currentDamage = 100.0F;

    private int maxChargeTicks = 100;
    private float minSize = 1.0F;
    private float maxSize = 5.0F;
    private float minDamage = 100.0F;
    private float maxDamage = 1250.0F;
    private float projectileSpeed = 0.65F;
    private float projectileGravity = 0.03F;
    private float projectileDrag = 0.98F;
    private float projectileUpwardBoost = 0.12F;
    private int projectileLifetime = 200;
    private int burnDurationTicks = 200;

    private int flyingTicks;
    private boolean impacted;

    public EntityRedSunFireball(World world) {
        super(world);
        this.setSize(1.0F, 1.0F);
        this.noClip = true;
    }

    public EntityRedSunFireball(World world, EntityPlayer owner, int maxChargeTicks, float minSize, float maxSize,
        float minDamage, float maxDamage, float projectileSpeed, float projectileGravity, float projectileDrag,
        float projectileUpwardBoost, int projectileLifetime, int burnDurationTicks) {
        this(world);
        this.ownerUuid = owner.getUniqueID();
        this.ownerEntityId = owner.getEntityId();
        this.chargeStartWorldTime = world.getTotalWorldTime();

        this.maxChargeTicks = maxChargeTicks;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.projectileSpeed = projectileSpeed;
        this.projectileGravity = projectileGravity;
        this.projectileDrag = projectileDrag;
        this.projectileUpwardBoost = projectileUpwardBoost;
        this.projectileLifetime = projectileLifetime;
        this.burnDurationTicks = burnDurationTicks;

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

        look = look.normalize();
        this.motionX = look.xCoord * projectileSpeed;
        this.motionY = look.yCoord * projectileSpeed + projectileUpwardBoost;
        this.motionZ = look.zCoord * projectileSpeed;
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
        
        AxisAlignedBB impactBox = this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(0.5D, 0.5D, 0.5D);
        List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, impactBox);
        
        boolean hitEntity = false;
        for (Entity entity : list) {
            if (entity instanceof EntityLivingBase && !entity.isDead) {
                if (this.flyingTicks <= 10 && entity.getEntityId() == this.ownerEntityId) continue;
                if (entity instanceof EntityRedSunFireball) continue;
                
                if (entity.canBeCollidedWith()) {
                    float f = 0.3F;
                    AxisAlignedBB entityBox = entity.boundingBox.expand((double)f, (double)f, (double)f);
                    net.minecraft.util.MovingObjectPosition mopEntity = entityBox.calculateIntercept(oldPos, newPos);
                    if (mopEntity != null || entityBox.intersectsWith(this.boundingBox)) {
                        hitEntity = true;
                        break;
                    }
                }
            }
        }
        
        if (mop != null || hitEntity) {
            if (!worldObj.isRemote) triggerImpact();
            else this.setDead();
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

    private void triggerImpact() {
        if (impacted) return;
        impacted = true;

        if (!worldObj.isRemote) {
            double explosionRadius = currentSize * 5.0D;
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

                // Check actual spherical distance
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
                    (int) (150 * currentSize),
                    explosionRadius * 0.8,
                    explosionRadius * 0.8,
                    explosionRadius * 0.8,
                    0.3);

                ((net.minecraft.world.WorldServer) worldObj).func_147487_a(
                    "lava",
                    this.posX,
                    this.posY,
                    this.posZ,
                    (int) (50 * currentSize),
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
        projectileSpeed = nbt.getFloat("ProjSpeed");
        projectileGravity = nbt.getFloat("ProjGravity");
        projectileDrag = nbt.getFloat("ProjDrag");
        projectileUpwardBoost = nbt.getFloat("ProjUpward");
        projectileLifetime = nbt.getInteger("ProjLife");
        flyingTicks = nbt.getInteger("FlyingTicks");
        burnDurationTicks = nbt.getInteger("BurnDuration");
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
        nbt.setFloat("ProjSpeed", projectileSpeed);
        nbt.setFloat("ProjGravity", projectileGravity);
        nbt.setFloat("ProjDrag", projectileDrag);
        nbt.setFloat("ProjUpward", projectileUpwardBoost);
        nbt.setInteger("ProjLife", projectileLifetime);
        nbt.setInteger("FlyingTicks", flyingTicks);
        nbt.setInteger("BurnDuration", burnDurationTicks);
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
