//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.MadokaVolleyDamageManager;
import com.greyhat.dark_grey.api.RPGDamageSources;

public class EntityMadokaArrow extends EntityThrowable {

    private static final int MAX_LIFETIME_TICKS = 200;

    private int chargeLevel;
    private float customDamage;
    private int volleyId;
    private boolean hasSpawnedHexagrams;

    public EntityMadokaArrow(final World world) {
        super(world);
        this.customDamage = 10.0f;
        this.hasSpawnedHexagrams = false;
    }

    public EntityMadokaArrow(final World world, final EntityLivingBase shooter, final int chargeLevel,
        final float damage) {
        super(world, shooter);
        this.customDamage = 10.0f;
        this.hasSpawnedHexagrams = false;
        this.chargeLevel = chargeLevel;
        this.customDamage = damage;
        this.dataWatcher.updateObject(20, (byte) chargeLevel);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(20, (byte) 0);
        this.dataWatcher.addObject(21, (byte) 0);
        this.dataWatcher.addObject(22, (byte) 0);
    }

    public void setVolleyArrow(final boolean volleyArrow) {
        this.dataWatcher.updateObject(21, (byte) (volleyArrow ? 1 : 0));
    }

    public boolean isVolleyArrow() {
        return this.dataWatcher.getWatchableObjectByte(21) != 0;
    }

    public void setVolleyVisualLeader(final boolean visualLeader) {
        this.dataWatcher.updateObject(22, (byte) (visualLeader ? 1 : 0));
    }

    public boolean isVolleyVisualLeader() {
        return this.dataWatcher.getWatchableObjectByte(22) != 0;
    }

    public void setVolleyId(final int volleyId) {
        this.volleyId = volleyId;
    }

    public int getChargeLevel() {
        if (!this.worldObj.isRemote) {
            return this.chargeLevel;
        }
        return this.dataWatcher.getWatchableObjectByte(20);
    }

    @Override
    public void onUpdate() {
        if (!this.worldObj.isRemote && this.ticksExisted == 0
            && this.isVolleyArrow()
            && this.resolveFormationSpawnGap()) {
            return;
        }
        super.onUpdate();
        if (!this.worldObj.isRemote && this.ticksExisted > MAX_LIFETIME_TICKS) {
            this.setDead();
            return;
        }
        final int level = this.getChargeLevel();
        final boolean volleyArrow = this.isVolleyArrow();
        if (this.worldObj.isRemote) {
            if (level >= 1) {
                final int count = this.getTrailParticleCount(volleyArrow ? 1 : 5);
                for (int i = 0; i < count; ++i) {
                    final double pX = this.lastTickPosX + (this.posX - this.lastTickPosX) * (i / (double) count);
                    final double pY = this.lastTickPosY + (this.posY - this.lastTickPosY) * (i / (double) count);
                    final double pZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * (i / (double) count);
                    this.worldObj.spawnParticle("reddust", pX, pY, pZ, 1.0, 1.0, 1.0);
                }
            }
            if (level >= 2) {
                final int count = this.getTrailParticleCount(volleyArrow ? 2 : 10);
                for (int i = 0; i < count; ++i) {
                    final double pX = this.lastTickPosX + (this.posX - this.lastTickPosX) * (i / (double) count);
                    final double pY = this.lastTickPosY + (this.posY - this.lastTickPosY) * (i / (double) count);
                    final double pZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * (i / (double) count);
                    this.worldObj.spawnParticle("reddust", pX, pY, pZ, 1.0, 0.7, 0.9);
                }
            }
            if (level >= 3) {
                final int count = this.getTrailParticleCount(volleyArrow ? 3 : 30);
                for (int i = 0; i < count; ++i) {
                    final double interp = i / (double) count;
                    final double centerX = this.lastTickPosX + (this.posX - this.lastTickPosX) * interp;
                    final double centerY = this.lastTickPosY + (this.posY - this.lastTickPosY) * interp;
                    final double centerZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * interp;
                    final double offsetX = (this.rand.nextDouble() - 0.5) * 1.0;
                    final double offsetY = (this.rand.nextDouble() - 0.5) * 1.0;
                    final double offsetZ = (this.rand.nextDouble() - 0.5) * 1.0;
                    this.worldObj.spawnParticle(
                        "reddust",
                        centerX + offsetX,
                        centerY + offsetY,
                        centerZ + offsetZ,
                        1.0,
                        0.9,
                        1.0);
                }
            }
        }
        if (this.worldObj.isRemote && level > 0
            && !this.hasSpawnedHexagrams
            && (!volleyArrow || this.isVolleyVisualLeader())) {
            this.hasSpawnedHexagrams = true;
            this.spawnHexagramLayers();
        }
    }

    /**
     * The horizontal formation is created several blocks in front of the player.
     * Resolve that one-time spawn gap so nearby entities and walls cannot be skipped.
     */
    private boolean resolveFormationSpawnGap() {
        EntityLivingBase shooter = this.getThrower();
        if (shooter == null) return false;

        Vec3 start = Vec3.createVectorHelper(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);
        Vec3 end = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
        MovingObjectPosition nearest = this.worldObj.rayTraceBlocks(start, end);
        double nearestDistance = nearest == null ? Double.MAX_VALUE : start.distanceTo(nearest.hitVec);

        AxisAlignedBB pathBounds = AxisAlignedBB
            .getBoundingBox(
                Math.min(start.xCoord, end.xCoord),
                Math.min(start.yCoord, end.yCoord),
                Math.min(start.zCoord, end.zCoord),
                Math.max(start.xCoord, end.xCoord),
                Math.max(start.yCoord, end.yCoord),
                Math.max(start.zCoord, end.zCoord))
            .expand(0.3D, 0.3D, 0.3D);
        @SuppressWarnings("unchecked")
        List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, pathBounds);
        for (Entity entity : entities) {
            if (entity == shooter || !entity.canBeCollidedWith()) continue;
            MovingObjectPosition intercept = entity.boundingBox.expand(0.3D, 0.3D, 0.3D)
                .calculateIntercept(start, end);
            if (intercept == null) continue;
            double distance = start.distanceTo(intercept.hitVec);
            if (distance < nearestDistance) {
                nearest = new MovingObjectPosition(entity);
                nearestDistance = distance;
            }
        }

        if (nearest == null) return false;
        this.onImpact(nearest);
        return this.isDead;
    }

    private int getTrailParticleCount(final int baseCount) {
        final double density = com.greyhat.dark_grey.common.Config.particleDensity;
        if (density <= 0.0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(baseCount * density));
    }

    private void spawnHexagramLayers() {
        EntityLivingBase shooter = null;
        if (this.getThrower() != null) {
            shooter = this.getThrower();
        }
        if (shooter == null) {
            shooter = (EntityLivingBase) this.worldObj.getClosestPlayerToEntity((Entity) this, 30.0);
        }
        Vec3 forward;
        Vec3 origin;
        if (shooter != null) {
            forward = shooter.getLook(1.0f)
                .normalize();
            origin = Vec3.createVectorHelper(
                shooter.posX + forward.xCoord * 1.5,
                shooter.posY + shooter.getEyeHeight() + forward.yCoord * 1.5,
                shooter.posZ + forward.zCoord * 1.5);
        } else {
            forward = Vec3.createVectorHelper(this.motionX, this.motionY, this.motionZ)
                .normalize();
            origin = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
        }
        Vec3 up = Vec3.createVectorHelper(0.0, 1.0, 0.0);
        Vec3 right = forward.crossProduct(up);
        if (right.lengthVector() < 0.1) {
            right = forward.crossProduct(Vec3.createVectorHelper(1.0, 0.0, 0.0));
        }
        right = right.normalize();
        up = right.crossProduct(forward)
            .normalize();
        int layers = Math.min(3, this.getChargeLevel());
        if (layers < 1) {
            layers = 1;
        }
        for (int layer = 0; layer < layers; ++layer) {
            final double layerOffset = layer * 1.5;
            final double radius = 2.0 + layer * 1.5;
            final Vec3 center = Vec3.createVectorHelper(
                origin.xCoord + forward.xCoord * layerOffset,
                origin.yCoord + forward.yCoord * layerOffset,
                origin.zCoord + forward.zCoord * layerOffset);
            this.drawHexagram(center, right, up, radius);
        }
    }

    private void drawHexagram(final Vec3 center, final Vec3 right, final Vec3 up, final double radius) {
        final Vec3[] p = new Vec3[6];
        for (int i = 0; i < 6; ++i) {
            final double angle = 6.283185307179586 * i / 6.0;
            final double dx = Math.cos(angle) * radius;
            final double dy = Math.sin(angle) * radius;
            p[i] = Vec3.createVectorHelper(
                center.xCoord + right.xCoord * dx + up.xCoord * dy,
                center.yCoord + right.yCoord * dx + up.yCoord * dy,
                center.zCoord + right.zCoord * dx + up.zCoord * dy);
        }
        this.drawParticleLine(p[0], p[2]);
        this.drawParticleLine(p[2], p[4]);
        this.drawParticleLine(p[4], p[0]);
        this.drawParticleLine(p[1], p[3]);
        this.drawParticleLine(p[3], p[5]);
        this.drawParticleLine(p[5], p[1]);
        final int circleParticles = (int) Math
            .ceil(radius * 12.0 * com.greyhat.dark_grey.common.Config.particleDensity);
        for (int j = 0; j < circleParticles; ++j) {
            final double angle2 = 6.283185307179586 * j / circleParticles;
            final double dx2 = Math.cos(angle2) * radius;
            final double dy2 = Math.sin(angle2) * radius;
            final double px = center.xCoord + right.xCoord * dx2 + up.xCoord * dy2;
            final double py = center.yCoord + right.yCoord * dx2 + up.yCoord * dy2;
            final double pz = center.zCoord + right.zCoord * dx2 + up.zCoord * dy2;
            this.spawnMagicParticle(px, py, pz);
        }
    }

    private void drawParticleLine(final Vec3 start, final Vec3 end) {
        final double dist = start.distanceTo(end);
        final int particles = (int) Math.ceil(dist * 8.0 * com.greyhat.dark_grey.common.Config.particleDensity);
        if (particles <= 0) {
            return;
        }
        for (int i = 0; i <= particles; ++i) {
            final double fraction = i / (double) particles;
            final double px = start.xCoord + (end.xCoord - start.xCoord) * fraction;
            final double py = start.yCoord + (end.yCoord - start.yCoord) * fraction;
            final double pz = start.zCoord + (end.zCoord - start.zCoord) * fraction;
            this.spawnMagicParticle(px, py, pz);
        }
    }

    private void spawnMagicParticle(final double x, final double y, final double z) {
        final double colorG = 0.8 + this.rand.nextDouble() * 0.2;
        final double colorB = 0.9 + this.rand.nextDouble() * 0.1;
        this.worldObj.spawnParticle("reddust", x, y, z, 1.0, colorG, colorB);
    }

    @Override
    protected float getGravityVelocity() {
        return (this.getChargeLevel() >= 1) ? 0.0f : 0.03f;
    }

    @Override
    protected void onImpact(final MovingObjectPosition mop) {
        if (!this.worldObj.isRemote) {
            final int level = this.getChargeLevel();
            if (mop.entityHit != null) {
                final EntityLivingBase shooter = this.getThrower();
                if (mop.entityHit instanceof EntityLivingBase && shooter != null
                    && CombatTargeting.canDamage(shooter, (EntityLivingBase) mop.entityHit, false)) {
                    final EntityLivingBase target = (EntityLivingBase) mop.entityHit;
                    if (this.isVolleyArrow()) {
                        MadokaVolleyDamageManager.queueHit(this, shooter, target, this.volleyId, this.customDamage);
                    } else {
                        target.attackEntityFrom(RPGDamageSources.causeArrowDamage(this, shooter), this.customDamage);
                    }
                }
            }
            if (this.isVolleyArrow()) {
                this.worldObj.setEntityState(this, (byte) 17);
                this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.explode", 1.0f, 1.2f);
                this.setDead();
                return;
            }

            float explosionSize = 0.0f;
            if (level == 1) {
                explosionSize = 2.5f;
            } else if (level == 2) {
                explosionSize = 5.0f;
            } else if (level >= 3) {
                explosionSize = 8.0f;
            }
            if (explosionSize > 0.0f) {
                this.worldObj.createExplosion(this.getThrower(), this.posX, this.posY, this.posZ, explosionSize, false);
                if (level >= 2) {
                    this.worldObj.createExplosion(
                        this.getThrower(),
                        this.posX,
                        this.posY - 1.5,
                        this.posZ,
                        explosionSize * 0.8f,
                        false);
                }
            }
            this.setDead();
        }
    }

    @Override
    public void handleHealthUpdate(final byte state) {
        if (state == 17) {
            for (int i = 0; i < 18; ++i) {
                this.worldObj.spawnParticle(
                    i % 3 == 0 ? "hugeexplosion" : "fireworksSpark",
                    this.posX + (this.rand.nextDouble() - 0.5) * 1.5,
                    this.posY + (this.rand.nextDouble() - 0.5) * 1.5,
                    this.posZ + (this.rand.nextDouble() - 0.5) * 1.5,
                    0.0,
                    0.0,
                    0.0);
            }
            return;
        }
        super.handleHealthUpdate(state);
    }

    @Override
    public void writeEntityToNBT(final NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("ChargeLevel", this.chargeLevel);
        nbt.setFloat("CustomDamage", this.customDamage);
        nbt.setBoolean("VolleyArrow", this.isVolleyArrow());
        nbt.setBoolean("VolleyVisualLeader", this.isVolleyVisualLeader());
        nbt.setInteger("VolleyId", this.volleyId);
    }

    @Override
    public void readEntityFromNBT(final NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.chargeLevel = nbt.getInteger("ChargeLevel");
        this.customDamage = nbt.getFloat("CustomDamage");
        this.setVolleyArrow(nbt.getBoolean("VolleyArrow"));
        this.setVolleyVisualLeader(nbt.getBoolean("VolleyVisualLeader"));
        this.volleyId = nbt.getInteger("VolleyId");
        this.dataWatcher.updateObject(20, (byte) this.chargeLevel);
    }
}
