package com.greyhat.dark_grey.entity;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.greyhat.dark_grey.damage.DamageSourceItanisArrow;

public class EntityItanisArrow extends EntityArrow {

    private ItanisArrowType arrowType = ItanisArrowType.NORMAL;
    private double baseDamage = 300.0;
    private EntityLivingBase trackingTarget = null;
    private int lifeTicks = 0;
    private int maxLifeTicks = 200;
    private Set<Integer> hitEntityIds = new HashSet<>();
    private double turnSpeed = 0.18;
    private double acceleration = 0.08;
    private double trackingRange = 32.0;
    private double orbitAngle = 0.0;

    // Fallback for vanilla rendering logic
    public int arrowShake;

    public EntityItanisArrow(World world) {
        super(world);
    }

    public EntityItanisArrow(World world, EntityLivingBase shooter, EntityLivingBase target, ItanisArrowType type,
        double damage, double turnSpeed, double acceleration, double trackingRange) {
        super(world, shooter, 1.0F);
        this.arrowType = type;
        this.baseDamage = damage;
        this.trackingTarget = target;
        this.turnSpeed = turnSpeed;
        this.acceleration = acceleration;
        this.trackingRange = trackingRange;
        this.setDamage(damage);
        this.canBePickedUp = 0;
        this.orbitAngle = this.getEntityId() * 37.0; // Randomize start angle

        if (target != null) {
            Vec3 dir = Vec3
                .createVectorHelper(
                    target.posX - this.posX,
                    target.posY + target.height / 2.0F - this.posY,
                    target.posZ - this.posZ)
                .normalize();
            this.motionX = dir.xCoord * 1.5;
            this.motionY = dir.yCoord * 1.5;
            this.motionZ = dir.zCoord * 1.5;
        }
    }

    public EntityItanisArrow(World world, EntityLivingBase shooter, float speed, ItanisArrowType type, double damage,
        double turnSpeed, double acceleration, double trackingRange) {
        super(world, shooter, speed);
        this.arrowType = type;
        this.baseDamage = damage;
        this.turnSpeed = turnSpeed;
        this.acceleration = acceleration;
        this.trackingRange = trackingRange;
        this.setDamage(damage);
        this.canBePickedUp = 0;
        this.orbitAngle = this.getEntityId() * 37.0;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(20, (byte) ItanisArrowType.NORMAL.ordinal());
    }

    public ItanisArrowType getArrowType() {
        return ItanisArrowType.fromInt(this.dataWatcher.getWatchableObjectByte(20));
    }

    public void setArrowType(ItanisArrowType type) {
        this.arrowType = type;
        this.dataWatcher.updateObject(20, (byte) type.ordinal());
    }

    public void setTrackingTarget(EntityLivingBase target, double speed) {
        this.trackingTarget = target;
        if (target == null) return;
        Vec3 direction = Vec3
            .createVectorHelper(
                target.posX - this.posX,
                target.posY + target.height / 2.0F - this.posY,
                target.posZ - this.posZ)
            .normalize();
        this.motionX = direction.xCoord * speed;
        this.motionY = direction.yCoord * speed;
        this.motionZ = direction.zCoord * speed;
    }

    @Override
    public void onUpdate() {
        super.onEntityUpdate();
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.worldObj.isRemote) {
            this.arrowType = this.getArrowType();
        } else {
            this.setArrowType(this.arrowType);

            if (this.arrowType == ItanisArrowType.CHARGE_ORBIT
                && (this.shootingEntity == null || this.shootingEntity.isDead
                    || !(this.shootingEntity instanceof net.minecraft.entity.player.EntityPlayer)
                    || !((net.minecraft.entity.player.EntityPlayer) this.shootingEntity).isUsingItem())) {
                this.arrowType = ItanisArrowType.CHARGE_AUTO;
                this.setArrowType(this.arrowType);
            }

            if (this.arrowType == ItanisArrowType.CHARGE_AUTO && this.trackingTarget == null
                && this.ticksExisted % 5 == 0) {
                if (this.shootingEntity != null && !this.shootingEntity.isDead) {
                    this.trackingTarget = ItanisTargetHelper.findClosestTarget(
                        this.shootingEntity,
                        this.shootingEntity.posX,
                        this.shootingEntity.posY,
                        this.shootingEntity.posZ,
                        this.trackingRange,
                        this.getEntityId());
                    if (this.trackingTarget != null) {
                        Vec3 dir = Vec3
                            .createVectorHelper(
                                this.trackingTarget.posX - this.posX,
                                this.trackingTarget.posY + this.trackingTarget.height / 2.0F - this.posY,
                                this.trackingTarget.posZ - this.posZ)
                            .normalize();
                        this.motionX = dir.xCoord * 1.5;
                        this.motionY = dir.yCoord * 1.5;
                        this.motionZ = dir.zCoord * 1.5;
                        this.worldObj
                            .playSoundAtEntity(this, "random.bow", 0.5F, 1.4F / (this.rand.nextFloat() * 0.4F + 0.8F));
                    }
                }
            }
        }

        boolean isHovering = this.arrowType == ItanisArrowType.CHARGE_ORBIT
            || (this.arrowType == ItanisArrowType.CHARGE_AUTO && this.trackingTarget == null
                && !this.worldObj.isRemote);
        if (this.worldObj.isRemote
            && (this.arrowType == ItanisArrowType.CHARGE_ORBIT || this.arrowType == ItanisArrowType.CHARGE_AUTO)) {
            // Client side heuristic for hovering if motion is very small
            if (Math.abs(this.motionX) < 0.1 && Math.abs(this.motionY) < 0.1 && Math.abs(this.motionZ) < 0.1) {
                isHovering = true;
            }
        }

        if (isHovering) {
            if (!this.worldObj.isRemote) {
                this.lifeTicks = 0;
                if (this.shootingEntity != null && !this.shootingEntity.isDead) {
                    this.orbitAngle += 10.0;
                    double rad = Math.toRadians(this.orbitAngle);
                    double radius = 1.8;
                    this.posX = this.shootingEntity.posX + Math.cos(rad) * radius;
                    this.posY = this.shootingEntity.posY + this.shootingEntity.getEyeHeight() - 0.2;
                    this.posZ = this.shootingEntity.posZ + Math.sin(rad) * radius;
                    this.setPosition(this.posX, this.posY, this.posZ);
                    this.motionX = 0;
                    this.motionY = 0;
                    this.motionZ = 0;
                } else {
                    this.setDead();
                }
            } else {
                // Client side: zero out motion to prevent gravity pulling it down before server syncs position
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;
            }
            // Spawn hover particles
            if (this.worldObj.isRemote && this.ticksExisted % 2 == 0) {
                this.worldObj.spawnParticle(
                    "crit",
                    this.posX + (this.rand.nextDouble() - 0.5D),
                    this.posY + this.rand.nextDouble(),
                    this.posZ + (this.rand.nextDouble() - 0.5D),
                    0,
                    0,
                    0);
            }
            return; // Skip super.onUpdate() which applies gravity and collision
        } else if (!this.worldObj.isRemote) {
            this.lifeTicks++;
            if (this.lifeTicks > this.maxLifeTicks) {
                this.setDead();
                return;
            }

            if (this.lifeTicks % 5 == 0) {
                if (!ItanisTargetHelper.isValidTarget(this.shootingEntity, this.trackingTarget)) {
                    if (this.shootingEntity == null || this.shootingEntity.isDead) {
                        this.setDead();
                        return;
                    }
                    this.trackingTarget = ItanisTargetHelper.findClosestTarget(
                        this.shootingEntity,
                        this.shootingEntity.posX,
                        this.shootingEntity.posY,
                        this.shootingEntity.posZ,
                        this.trackingRange,
                        this.getEntityId());
                }
            }
        }

        // Homing logic
        if (this.trackingTarget != null && (this.motionX != 0.0 || this.motionY != 0.0 || this.motionZ != 0.0)) {
            Vec3 currentVel = Vec3.createVectorHelper(this.motionX, this.motionY, this.motionZ);
            double speed = currentVel.lengthVector();

            // Allow acceleration up to a cap
            if (speed < 2.5) {
                speed += this.acceleration;
            }

            if (speed > 0.1) {
                Vec3 dirToTarget = Vec3
                    .createVectorHelper(
                        this.trackingTarget.posX - this.posX,
                        this.trackingTarget.posY + this.trackingTarget.height / 2.0F - this.posY,
                        this.trackingTarget.posZ - this.posZ)
                    .normalize();

                Vec3 newDir = Vec3
                    .createVectorHelper(
                        currentVel.xCoord / speed + (dirToTarget.xCoord - currentVel.xCoord / speed) * this.turnSpeed,
                        currentVel.yCoord / speed + (dirToTarget.yCoord - currentVel.yCoord / speed) * this.turnSpeed,
                        currentVel.zCoord / speed + (dirToTarget.zCoord - currentVel.zCoord / speed) * this.turnSpeed)
                    .normalize();

                this.motionX = newDir.xCoord * speed;
                this.motionY = newDir.yCoord * speed;
                this.motionZ = newDir.zCoord * speed;

                // Reduce gravity effect while homing
                this.motionY += 0.045;
            }
        }

        // Spawn particles
        if (this.worldObj.isRemote && (this.motionX != 0.0 || this.motionY != 0.0 || this.motionZ != 0.0)) {
            String particle = this.getArrowType() == ItanisArrowType.FULL_CHARGE ? "magicCrit" : "crit";
            for (int i = 0; i < 2; i++) {
                this.worldObj.spawnParticle(
                    particle,
                    this.posX + (this.rand.nextDouble() - 0.5D) * this.width,
                    this.posY + this.rand.nextDouble() * this.height,
                    this.posZ + (this.rand.nextDouble() - 0.5D) * this.width,
                    -this.motionX * 0.1,
                    -this.motionY * 0.1,
                    -this.motionZ * 0.1);
            }
        }

        // Custom collision and piercing logic
        if (!this.worldObj.isRemote && (this.motionX != 0.0 || this.motionY != 0.0 || this.motionZ != 0.0)) {
            Vec3 vecPos = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            Vec3 vecNextPos = Vec3
                .createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            MovingObjectPosition mopBlock = this.worldObj.func_147447_a(vecPos, vecNextPos, false, true, false);

            vecPos = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
            vecNextPos = Vec3
                .createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            if (mopBlock != null) {
                vecNextPos = Vec3
                    .createVectorHelper(mopBlock.hitVec.xCoord, mopBlock.hitVec.yCoord, mopBlock.hitVec.zCoord);
            }

            Entity hitEntity = null;
            @SuppressWarnings("unchecked")
            java.util.List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(
                this,
                this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ)
                    .expand(1.0D, 1.0D, 1.0D));
            double minDistance = 0.0D;

            for (Entity e : list) {
                if (e.canBeCollidedWith() && ItanisTargetHelper.isValidTarget(this.shootingEntity, e)) {
                    float f1 = 0.3F;
                    AxisAlignedBB aabb = e.boundingBox.expand(f1, f1, f1);
                    MovingObjectPosition mop = aabb.calculateIntercept(vecPos, vecNextPos);
                    if (mop != null) {
                        double distance = vecPos.distanceTo(mop.hitVec);
                        if (distance < minDistance || minDistance == 0.0D) {
                            hitEntity = e;
                            minDistance = distance;
                        }
                    }
                }
            }

            if (hitEntity != null) {
                if (this.arrowType == ItanisArrowType.FULL_CHARGE) {
                    if (!this.hitEntityIds.contains(hitEntity.getEntityId())) {
                        this.hitEntityIds.add(hitEntity.getEntityId());
                        DamageSourceItanisArrow source = new DamageSourceItanisArrow(
                            "arrow",
                            this,
                            this.shootingEntity);
                        source.setIgnoreHurtResistantTime(true);
                        source.applyPreDamage((EntityLivingBase) hitEntity);
                        hitEntity.attackEntityFrom(source, (float) this.baseDamage);
                        this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
                    }
                    // Piercing arrow does not stop on entity hit!
                } else {
                    DamageSourceItanisArrow source = new DamageSourceItanisArrow("arrow", this, this.shootingEntity);
                    source.setIgnoreHurtResistantTime(true);
                    source.applyPreDamage((EntityLivingBase) hitEntity);
                    if (hitEntity.attackEntityFrom(source, (float) this.baseDamage)) {
                        this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
                        this.setDead();
                    } else {
                        this.setDead();
                    }
                    return; // Stop update to prevent super.onUpdate from hitting again
                }
            }

            if (mopBlock != null && mopBlock.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                this.setDead();
                return;
            }
        }

        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);

        // Face motion direction
        if ((this.motionX != 0.0 || this.motionY != 0.0 || this.motionZ != 0.0)
            && this.motionX * this.motionX + this.motionZ * this.motionZ > 0.001) {
            float f3 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D
                / Math.PI);
            this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(this.motionY, f3) * 180.0D / Math.PI);
        }
    }

    @Override
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        super.setThrowableHeading(x, y, z, velocity, 0.0f); // 0 inaccuracy for Itanis
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("ItanisArrowType", this.arrowType.ordinal());
        nbt.setDouble("BaseDamage", this.baseDamage);
        nbt.setDouble("TrackingRange", this.trackingRange);
        nbt.setDouble("TurnSpeed", this.turnSpeed);
        nbt.setDouble("Acceleration", this.acceleration);
        nbt.setInteger("LifeTicks", this.lifeTicks);

        NBTTagList hitIds = new NBTTagList();
        for (Integer id : this.hitEntityIds) {
            NBTTagCompound idTag = new NBTTagCompound();
            idTag.setInteger("Id", id);
            hitIds.appendTag(idTag);
        }
        nbt.setTag("HitIds", hitIds);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.arrowType = ItanisArrowType.fromInt(nbt.getInteger("ItanisArrowType"));
        this.setArrowType(this.arrowType);
        if (nbt.hasKey("BaseDamage")) {
            this.baseDamage = nbt.getDouble("BaseDamage");
            this.setDamage(this.baseDamage);
        }
        if (nbt.hasKey("TrackingRange")) this.trackingRange = nbt.getDouble("TrackingRange");
        if (nbt.hasKey("TurnSpeed")) this.turnSpeed = nbt.getDouble("TurnSpeed");
        if (nbt.hasKey("Acceleration")) this.acceleration = nbt.getDouble("Acceleration");
        this.lifeTicks = nbt.getInteger("LifeTicks");

        this.hitEntityIds.clear();
        if (nbt.hasKey("HitIds")) {
            NBTTagList hitIds = nbt.getTagList("HitIds", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < hitIds.tagCount(); i++) {
                this.hitEntityIds.add(
                    hitIds.getCompoundTagAt(i)
                        .getInteger("Id"));
            }
        }
    }

    // Override the hit logic in EntityArrow to support piercing and custom damage source
    @Override
    public void onCollideWithPlayer(net.minecraft.entity.player.EntityPlayer player) {
        if (!this.worldObj.isRemote && (this.motionX == 0.0 && this.motionY == 0.0 && this.motionZ == 0.0)
            && this.arrowShake <= 0) {
            // Do nothing, cannot be picked up
        }
    }
}
