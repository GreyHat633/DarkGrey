package com.greyhat.dark_grey.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.mark.MarkManager;

public class EntityBoneMeteor extends EntityThrowable {

    private float impactDamage;
    private double impactBoxSize;
    private String fractureMarkId;
    private int fractureStacks;

    private float customGravity = 0.04F;
    private int lifetimeTicks = 200;

    private boolean impacted = false;

    public EntityBoneMeteor(World world) {
        super(world);
    }

    public EntityBoneMeteor(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityBoneMeteor(World world, EntityLivingBase thrower, float fallSpeed, float gravity, int lifetime,
        double impactBox, float damage, String markId, int markStacks) {
        super(world, thrower);
        this.customGravity = gravity;
        this.lifetimeTicks = lifetime;
        this.impactBoxSize = impactBox;
        this.impactDamage = damage;
        this.fractureMarkId = markId;
        this.fractureStacks = markStacks;

        this.motionX = 0;
        this.motionY = -fallSpeed;
        this.motionZ = 0;
    }

    @Override
    protected float getGravityVelocity() {
        return this.customGravity;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.worldObj.isRemote) {
            if (this.ticksExisted > this.lifetimeTicks) {
                this.setDead();
            }
        } else {
            for (int i = 0; i < 6; i++) {
                double ox = (this.rand.nextDouble() - 0.5) * 1.5;
                double oy = (this.rand.nextDouble() - 0.5) * 1.5;
                double oz = (this.rand.nextDouble() - 0.5) * 1.5;
                this.worldObj
                    .spawnParticle("smoke", this.posX + ox, this.posY + 0.5D + oy, this.posZ + oz, 0.0D, 0.0D, 0.0D);
            }
            for (int i = 0; i < 2; i++) {
                double ox = (this.rand.nextDouble() - 0.5) * 1.0;
                double oy = (this.rand.nextDouble() - 0.5) * 1.0;
                double oz = (this.rand.nextDouble() - 0.5) * 1.0;
                this.worldObj
                    .spawnParticle("flame", this.posX + ox, this.posY + 0.5D + oy, this.posZ + oz, 0.0D, 0.0D, 0.0D);
            }
            if (this.worldObj.rand.nextInt(3) == 0) {
                this.worldObj.spawnParticle("magicCrit", this.posX, this.posY + 0.5D, this.posZ, 0.0D, -0.1D, 0.0D);
            }
        }
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (this.worldObj.isRemote) {
            // Client side impact effects
            this.worldObj.spawnParticle("hugeexplosion", this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);

            // Seismic shockwave particles (restricted to 6x6x6 bounds, i.e. radius 3.0)
            for (int i = 0; i < 36; i++) {
                double angle = i * 10 * Math.PI / 180.0;
                double speed = 0.2D;
                double mx = Math.cos(angle) * speed;
                double mz = Math.sin(angle) * speed;
                this.worldObj.spawnParticle(
                    "cloud",
                    this.posX + Math.cos(angle) * 1.5,
                    this.posY + 0.1D,
                    this.posZ + Math.sin(angle) * 1.5,
                    mx,
                    0.0D,
                    mz);
                this.worldObj.spawnParticle(
                    "largeexplode",
                    this.posX + Math.cos(angle) * 2.8,
                    this.posY + 0.2D,
                    this.posZ + Math.sin(angle) * 2.8,
                    0,
                    0,
                    0);
            }

            for (int i = 0; i < 20; i++) {
                double mx = this.worldObj.rand.nextGaussian() * 0.1D;
                double my = this.worldObj.rand.nextGaussian() * 0.1D;
                double mz = this.worldObj.rand.nextGaussian() * 0.1D;
                this.worldObj.spawnParticle("iconcrack_352", this.posX, this.posY, this.posZ, mx, my, mz); // 352 is
                                                                                                           // bone block
                                                                                                           // or just
                                                                                                           // bone (item
                                                                                                           // 352 is
                                                                                                           // bone)
            }
            this.worldObj.playSound(
                this.posX,
                this.posY,
                this.posZ,
                "random.explode",
                0.5F,
                0.8F + this.worldObj.rand.nextFloat() * 0.2F,
                false);
            return;
        }

        if (this.impacted) {
            return;
        }
        this.impacted = true;

        EntityLivingBase caster = this.getThrower();

        double halfSize = this.impactBoxSize / 2.0;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            this.posX - halfSize,
            this.posY - halfSize,
            this.posZ - halfSize,
            this.posX + halfSize,
            this.posY + halfSize,
            this.posZ + halfSize);

        for (Object obj : this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb)) {
            EntityLivingBase target = (EntityLivingBase) obj;
            if (CombatTargeting.canDamage(caster, target, false)) {
                DamageSource ds = RPGDamageSources.causeBoneMeteorDamage(this, caster);
                boolean damaged = RPGDamageSources.dealIndependentProjectileDamage(target, ds, this.impactDamage);
                if (damaged) {
                    MarkManager.apply(target, this.fractureMarkId, this.fractureStacks, caster);
                }
            }
        }

        this.setDead();
    }
}
