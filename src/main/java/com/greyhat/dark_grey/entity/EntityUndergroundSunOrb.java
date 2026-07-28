package com.greyhat.dark_grey.entity;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.network.UndergroundSunExplosionMessage;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

public class EntityUndergroundSunOrb extends Entity implements IEntityAdditionalSpawnData {

    public static final byte STATE_FOLLOWING = 0;
    public static final byte STATE_FLYING = 1;

    private UUID ownerUuid;
    private int ownerEntityId = -1;
    private byte orbState = STATE_FOLLOWING;
    private int formationSlot;

    private float explosionDamage;
    private float explosionRadius = 20.0F;
    private float explosionHalfHeight = 10.0F;
    private float projectileSpeed = 1.8F;
    private int projectileLifetime = 100;
    private boolean ignoreHurtResistance = true;
    private boolean respectWalls = false;
    private float orbitRadius = 1.25F;
    private float orbitHeight = -4.0F;
    private float orbitSpeed = 2.0F;

    private long spawnWorldTime;
    private int flyingTicks;
    private int ownerMissingTicks;
    private boolean exploded;

    public EntityUndergroundSunOrb(World world) {
        super(world);
        this.setSize(0.6F, 0.6F);
        this.noClip = true;
        this.ignoreFrustumCheck = true;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 4096.0D;
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 15728880; // max brightness
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }

    public EntityUndergroundSunOrb(World world, EntityPlayer owner) {
        this(world);
        this.ownerUuid = owner.getUniqueID();
        this.ownerEntityId = owner.getEntityId();
        this.spawnWorldTime = world.getTotalWorldTime();

        this.dataWatcher.updateObject(10, STATE_FOLLOWING);
        this.dataWatcher.updateObject(11, this.ownerEntityId);
        this.dataWatcher.updateObject(12, 0); // formation slot defaults to 0, updated later
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(10, STATE_FOLLOWING); // state
        this.dataWatcher.addObject(11, -1); // ownerEntityId
        this.dataWatcher.addObject(12, 0); // formationSlot
    }

    public void setFormationSlot(int slot) {
        this.formationSlot = slot;
        this.dataWatcher.updateObject(12, slot);
    }

    public int getFormationSlot() {
        return this.formationSlot;
    }

    public byte getOrbState() {
        return this.orbState;
    }

    public long getSpawnWorldTime() {
        return this.spawnWorldTime;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    // --- Parameter Setters ---
    public void setExplosionDamage(float v) {
        this.explosionDamage = v;
    }

    public void setExplosionRadius(float v) {
        this.explosionRadius = v;
    }

    public void setExplosionHalfHeight(float v) {
        this.explosionHalfHeight = v;
    }

    public void setProjectileSpeed(float v) {
        this.projectileSpeed = v;
    }

    public void setProjectileLifetime(int v) {
        this.projectileLifetime = v;
    }

    public void setIgnoreHurtResistance(boolean v) {
        this.ignoreHurtResistance = v;
    }

    public void setRespectWalls(boolean v) {
        this.respectWalls = v;
    }

    public void setOrbitRadius(float v) {
        this.orbitRadius = v;
    }

    public void setOrbitHeight(float v) {
        this.orbitHeight = v;
    }

    public void setOrbitSpeed(float v) {
        this.orbitSpeed = v;
    }

    public double getOrbitAngleRadians(float partialTicks) {
        double orbitTicks = (double) this.worldObj.getTotalWorldTime() + partialTicks;
        double angleDegrees = orbitTicks * (double) this.orbitSpeed + this.formationSlot * 120.0D;
        return Math.toRadians(angleDegrees % 360.0D);
    }

    public void launch(Vec3 direction) {
        this.orbState = STATE_FLYING;
        this.dataWatcher.updateObject(10, STATE_FLYING);

        this.motionX = direction.xCoord * projectileSpeed;
        this.motionY = direction.yCoord * projectileSpeed;
        this.motionZ = direction.zCoord * projectileSpeed;
        this.flyingTicks = 0;
        this.noClip = false;

        Entity owner = getOwnerEntity();
        if (owner != null) {
            this.setPosition(
                owner.posX + direction.xCoord * 1.0D,
                owner.posY + owner.getEyeHeight() + direction.yCoord * 1.0D,
                owner.posZ + direction.zCoord * 1.0D);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.worldObj.isRemote && this.resolveOwnerLiving() == null) {
            this.setDead();
            return;
        }

        if (this.worldObj.isRemote) {
            this.orbState = this.dataWatcher.getWatchableObjectByte(10);
            this.ownerEntityId = this.dataWatcher.getWatchableObjectInt(11);
            this.formationSlot = this.dataWatcher.getWatchableObjectInt(12);
        }

        if (this.orbState == STATE_FOLLOWING) {
            Entity owner = getOwnerEntity();
            if (owner != null && owner.isEntityAlive()) {
                this.ownerMissingTicks = 0;

                double angle = this.getOrbitAngleRadians(0.0F);

                double targetX = owner.posX + Math.cos(angle) * orbitRadius;
                double targetZ = owner.posZ + Math.sin(angle) * orbitRadius;
                double targetY = owner.posY + orbitHeight;

                double bob = Math.sin((this.ticksExisted + formationSlot * 20) * 0.12D) * 0.12D;
                targetY += bob;

                if (this.getDistanceSqToEntity(owner) > 256.0D) {
                    this.setPosition(targetX, targetY, targetZ);
                } else {
                    this.posX += (targetX - this.posX) * 0.35D;
                    this.posY += (targetY - this.posY) * 0.35D;
                    this.posZ += (targetZ - this.posZ) * 0.35D;
                    this.setPosition(this.posX, this.posY, this.posZ);
                }
            } else {
                if (!this.worldObj.isRemote) {
                    this.ownerMissingTicks++;
                    if (this.ownerMissingTicks > 40) {
                        this.setDead();
                    }
                }
            }
        } else if (this.orbState == STATE_FLYING) {
            this.flyingTicks++;
            if (this.flyingTicks >= projectileLifetime) {
                if (!this.worldObj.isRemote) {
                    explode(this.posX, this.posY, this.posZ);
                }
            } else {
                Vec3 start = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
                Vec3 end = Vec3
                    .createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

                MovingObjectPosition blockHit = this.worldObj.func_147447_a(start, end, false, true, false); // rayTraceBlocks
                if (blockHit != null) {
                    end = Vec3
                        .createVectorHelper(blockHit.hitVec.xCoord, blockHit.hitVec.yCoord, blockHit.hitVec.zCoord);
                }

                Entity hitEntity = null;
                AxisAlignedBB searchBox = this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ)
                    .expand(1.0D, 1.0D, 1.0D);
                @SuppressWarnings("unchecked")
                List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, searchBox);
                double closestDist = 0.0D;
                Entity owner = getOwnerEntity();

                for (Entity e : list) {
                    if (e instanceof EntityLivingBase && e.isEntityAlive()
                        && e != owner
                        && !(e instanceof EntityUndergroundSunOrb)
                        && e.canBeCollidedWith()) {
                        float border = e.getCollisionBorderSize();
                        AxisAlignedBB entityBox = e.boundingBox.expand(border, border, border);
                        MovingObjectPosition eHit = entityBox.calculateIntercept(start, end);
                        if (eHit != null) {
                            double dist = start.distanceTo(eHit.hitVec);
                            if (dist < closestDist || closestDist == 0.0D) {
                                hitEntity = e;
                                closestDist = dist;
                            }
                        }
                    }
                }

                if (hitEntity != null) {
                    if (!this.worldObj.isRemote) {
                        explode(this.posX, this.posY, this.posZ);
                    }
                } else if (blockHit != null) {
                    if (!this.worldObj.isRemote) {
                        explode(blockHit.hitVec.xCoord, blockHit.hitVec.yCoord, blockHit.hitVec.zCoord);
                    }
                }

                this.posX += this.motionX;
                this.posY += this.motionY;
                this.posZ += this.motionZ;
                this.setPosition(this.posX, this.posY, this.posZ);

                if (this.worldObj.isRemote) {
                    this.worldObj.spawnParticle(
                        "flame",
                        this.posX,
                        this.posY,
                        this.posZ,
                        -this.motionX * 0.2,
                        -this.motionY * 0.2,
                        -this.motionZ * 0.2);
                    this.worldObj.spawnParticle(
                        "crit",
                        this.posX,
                        this.posY,
                        this.posZ,
                        -this.motionX * 0.2,
                        -this.motionY * 0.2,
                        -this.motionZ * 0.2);
                }
            }
        }
    }

    public float getOrbitRadius() {
        return this.orbitRadius;
    }

    public float getOrbitHeight() {
        return this.orbitHeight;
    }

    public float getOrbitSpeed() {
        return this.orbitSpeed;
    }

    public Entity getOwnerEntity() {
        Entity byId = this.ownerEntityId == -1 ? null : this.worldObj.getEntityByID(this.ownerEntityId);
        if (byId != null && (this.ownerUuid == null || this.ownerUuid.equals(byId.getUniqueID()))) {
            return byId;
        }
        return this.worldObj.isRemote ? null : this.resolveOwnerLiving();
    }

    private EntityLivingBase resolveOwnerLiving() {
        Entity byId = this.ownerEntityId == -1 ? null : this.worldObj.getEntityByID(this.ownerEntityId);
        if (byId instanceof EntityLivingBase && (this.ownerUuid == null || this.ownerUuid.equals(byId.getUniqueID()))
            && byId.isEntityAlive()) {
            return (EntityLivingBase) byId;
        }
        if (this.ownerUuid != null && this.worldObj instanceof WorldServer) {
            EntityPlayer resolved = ((WorldServer) this.worldObj).func_152378_a(this.ownerUuid);
            if (resolved != null && resolved.isEntityAlive()) {
                this.ownerEntityId = resolved.getEntityId();
                this.dataWatcher.updateObject(11, this.ownerEntityId);
                return resolved;
            }
        }
        return null;
    }

    private void explode(double x, double y, double z) {
        if (this.exploded) return;
        this.exploded = true;

        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            x - explosionRadius,
            y - explosionHalfHeight,
            z - explosionRadius,
            x + explosionRadius,
            y + explosionHalfHeight,
            z + explosionRadius);

        @SuppressWarnings("unchecked")
        List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, box);
        Entity owner = getOwnerEntity();
        EntityLivingBase ownerLiving = (owner instanceof EntityLivingBase) ? (EntityLivingBase) owner : null;

        for (Entity e : list) {
            if (e instanceof EntityLivingBase && e != owner) {
                EntityLivingBase target = (EntityLivingBase) e;

                double dx = target.posX - x;
                double dz = target.posZ - z;
                if (dx * dx + dz * dz > explosionRadius * explosionRadius) {
                    continue;
                }

                double targetCenterY = target.posY + target.height * 0.5D;
                if (Math.abs(targetCenterY - y) > explosionHalfHeight) {
                    continue;
                }

                if (ownerLiving != null && CombatTargeting.canDamage(ownerLiving, target, false)) {
                    if (this.respectWalls) {
                        Vec3 centerPos = Vec3.createVectorHelper(x, y, z);
                        Vec3 targetPos = Vec3.createVectorHelper(target.posX, targetCenterY, target.posZ);
                        MovingObjectPosition hit = this.worldObj
                            .func_147447_a(centerPos, targetPos, false, true, false);
                        if (hit != null) {
                            continue; // Blocked by wall
                        }
                    }

                    DamageSource source = RPGDamageSources.causeUndergroundSunDamage(this, ownerLiving);
                    if (this.ignoreHurtResistance) {
                        RPGDamageSources.dealDamageWithoutInvulnerability(target, source, this.explosionDamage);
                    } else {
                        target.attackEntityFrom(source, this.explosionDamage);
                    }
                }
            }
        }

        this.worldObj.playSoundEffect(
            x,
            y,
            z,
            "random.explode",
            4.0F,
            (1.0F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);

        DarkGrey.NETWORK.sendToAllAround(
            new UndergroundSunExplosionMessage(x, y, z, explosionRadius),
            new TargetPoint(this.dimension, x, y, z, 128.0D));

        this.setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("OwnerUUIDMost") && nbt.hasKey("OwnerUUIDLeast")) {
            this.ownerUuid = new UUID(nbt.getLong("OwnerUUIDMost"), nbt.getLong("OwnerUUIDLeast"));
        }
        this.ownerEntityId = -1;
        this.orbState = nbt.getByte("OrbState");
        this.formationSlot = nbt.getInteger("FormationSlot");
        this.explosionDamage = nbt.getFloat("ExplosionDamage");
        this.explosionRadius = Math.max(1f, Math.min(64f, nbt.getFloat("ExplosionRadius")));
        this.explosionHalfHeight = Math.max(1f, Math.min(64f, nbt.getFloat("ExplosionHalfHeight")));
        this.projectileSpeed = Math.max(0.1f, Math.min(10f, nbt.getFloat("ProjectileSpeed")));
        this.projectileLifetime = Math.max(20, Math.min(1200, nbt.getInteger("ProjectileLifetime")));
        this.ignoreHurtResistance = nbt.getBoolean("IgnoreHurtResistance");
        this.respectWalls = nbt.getBoolean("RespectWalls");
        this.orbitRadius = Math.max(0.3f, Math.min(5f, nbt.getFloat("OrbitRadius")));
        this.orbitHeight = Math.max(-5.0f, Math.min(5f, nbt.getFloat("OrbitHeight")));
        this.orbitSpeed = Math.max(0f, Math.min(20f, nbt.getFloat("OrbitSpeed")));
        this.spawnWorldTime = nbt.getLong("SpawnWorldTime");
        this.flyingTicks = nbt.getInteger("FlyingTicks");
        this.exploded = nbt.getBoolean("Exploded");

        this.dataWatcher.updateObject(10, this.orbState);
        this.dataWatcher.updateObject(11, -1);
        this.dataWatcher.updateObject(12, this.formationSlot);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.setLong("OwnerUUIDMost", this.ownerUuid.getMostSignificantBits());
            nbt.setLong("OwnerUUIDLeast", this.ownerUuid.getLeastSignificantBits());
        }
        nbt.setByte("OrbState", this.orbState);
        nbt.setInteger("FormationSlot", this.formationSlot);
        nbt.setFloat("ExplosionDamage", this.explosionDamage);
        nbt.setFloat("ExplosionRadius", this.explosionRadius);
        nbt.setFloat("ExplosionHalfHeight", this.explosionHalfHeight);
        nbt.setFloat("ProjectileSpeed", this.projectileSpeed);
        nbt.setInteger("ProjectileLifetime", this.projectileLifetime);
        nbt.setBoolean("IgnoreHurtResistance", this.ignoreHurtResistance);
        nbt.setBoolean("RespectWalls", this.respectWalls);
        nbt.setFloat("OrbitRadius", this.orbitRadius);
        nbt.setFloat("OrbitHeight", this.orbitHeight);
        nbt.setFloat("OrbitSpeed", this.orbitSpeed);
        nbt.setLong("SpawnWorldTime", this.spawnWorldTime);
        nbt.setInteger("FlyingTicks", this.flyingTicks);
        nbt.setBoolean("Exploded", this.exploded);
    }

    @Override
    public void writeSpawnData(ByteBuf buf) {
        buf.writeInt(this.ownerEntityId);
        buf.writeByte(this.orbState);
        buf.writeInt(this.formationSlot);
        buf.writeFloat(this.orbitRadius);
        buf.writeFloat(this.orbitHeight);
        buf.writeFloat(this.orbitSpeed);
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        this.ownerEntityId = buf.readInt();
        this.orbState = buf.readByte();
        this.formationSlot = buf.readInt();
        this.orbitRadius = buf.readFloat();
        this.orbitHeight = buf.readFloat();
        this.orbitSpeed = buf.readFloat();

        this.dataWatcher.updateObject(10, this.orbState);
        this.dataWatcher.updateObject(11, this.ownerEntityId);
        this.dataWatcher.updateObject(12, this.formationSlot);
    }
}
