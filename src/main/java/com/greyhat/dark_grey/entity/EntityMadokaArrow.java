//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EntityMadokaArrow extends EntityThrowable {

    private int chargeLevel;
    private float customDamage;
    @SideOnly(Side.CLIENT)
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
    }

    public int getChargeLevel() {
        if (!this.worldObj.isRemote) {
            return this.chargeLevel;
        }
        return this.dataWatcher.getWatchableObjectByte(20);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        final int level = this.getChargeLevel();
        if (level >= 1) {
            this.motionY += 0.029999999329447746;
        }
        if (this.worldObj.isRemote) {
            if (level >= 1) {
                for (int i = 0; i < 5; ++i) {
                    final double pX = this.lastTickPosX + (this.posX - this.lastTickPosX) * (i / 5.0);
                    final double pY = this.lastTickPosY + (this.posY - this.lastTickPosY) * (i / 5.0);
                    final double pZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * (i / 5.0);
                    this.worldObj.spawnParticle("reddust", pX, pY, pZ, 1.0, 1.0, 1.0);
                }
            }
            if (level >= 2) {
                for (int i = 0; i < 10; ++i) {
                    final double pX = this.lastTickPosX + (this.posX - this.lastTickPosX) * (i / 10.0);
                    final double pY = this.lastTickPosY + (this.posY - this.lastTickPosY) * (i / 10.0);
                    final double pZ = this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * (i / 10.0);
                    this.worldObj.spawnParticle("reddust", pX, pY, pZ, 1.0, 0.7, 0.9);
                }
            }
            if (level >= 3) {
                for (int i = 0; i < 30; ++i) {
                    final double interp = i / 30.0;
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
        if (this.worldObj.isRemote && level > 0 && !this.hasSpawnedHexagrams) {
            this.hasSpawnedHexagrams = true;
            this.spawnHexagramLayers();
        }
    }

    @SideOnly(Side.CLIENT)
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

    @SideOnly(Side.CLIENT)
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
        for (int circleParticles = (int) (radius * 12.0), j = 0; j < circleParticles; ++j) {
            final double angle2 = 6.283185307179586 * j / circleParticles;
            final double dx2 = Math.cos(angle2) * radius;
            final double dy2 = Math.sin(angle2) * radius;
            final double px = center.xCoord + right.xCoord * dx2 + up.xCoord * dy2;
            final double py = center.yCoord + right.yCoord * dx2 + up.yCoord * dy2;
            final double pz = center.zCoord + right.zCoord * dx2 + up.zCoord * dy2;
            this.spawnMagicParticle(px, py, pz);
        }
    }

    @SideOnly(Side.CLIENT)
    private void drawParticleLine(final Vec3 start, final Vec3 end) {
        final double dist = start.distanceTo(end);
        for (int particles = (int) (dist * 8.0), i = 0; i <= particles; ++i) {
            final double fraction = i / (double) particles;
            final double px = start.xCoord + (end.xCoord - start.xCoord) * fraction;
            final double py = start.yCoord + (end.yCoord - start.yCoord) * fraction;
            final double pz = start.zCoord + (end.zCoord - start.zCoord) * fraction;
            this.spawnMagicParticle(px, py, pz);
        }
    }

    @SideOnly(Side.CLIENT)
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
                final DamageSource src = DamageSource.causeThrownDamage(this, this.getThrower());
                mop.entityHit.attackEntityFrom(src, this.customDamage);
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
                this.worldObj.createExplosion(this.getThrower(), this.posX, this.posY, this.posZ, explosionSize, true);
                if (level >= 2) {
                    this.worldObj.createExplosion(
                        this.getThrower(),
                        this.posX,
                        this.posY - 1.5,
                        this.posZ,
                        explosionSize * 0.8f,
                        true);
                }
            }
            this.setDead();
        }
    }

    @Override
    public void writeEntityToNBT(final NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("ChargeLevel", this.chargeLevel);
        nbt.setFloat("CustomDamage", this.customDamage);
    }

    @Override
    public void readEntityFromNBT(final NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.chargeLevel = nbt.getInteger("ChargeLevel");
        this.customDamage = nbt.getFloat("CustomDamage");
        this.dataWatcher.updateObject(20, (byte) this.chargeLevel);
    }
}
