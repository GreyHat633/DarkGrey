package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

public class EntityCorruptionBomb extends EntityThrowable implements IEntityAdditionalSpawnData {

    public String markId = "poison";
    public int markStacks = 3;
    public float areaWidth = 5.0F;
    public float areaHeight = 5.0F;
    public String areaShape = "square";
    public boolean respectWalls = false;
    public boolean affectThrower = false;

    public float projectileGravity = 0.03F;
    public int projectileLifetime = 200;

    private int ageTicks;
    private boolean impacted;

    public EntityCorruptionBomb(World world) {
        super(world);
    }

    public EntityCorruptionBomb(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    @Override
    protected float getGravityVelocity() {
        return this.projectileGravity;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.ageTicks++;
        if (this.ageTicks >= this.projectileLifetime) {
            this.setDead();
        }
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (this.worldObj.isRemote) {
            return;
        }
        if (this.impacted) {
            return;
        }
        this.impacted = true;

        double impactX = this.posX;
        double impactY = this.posY;
        double impactZ = this.posZ;

        if (mop.entityHit != null) {
            impactX = mop.entityHit.posX;
            // Center the explosion on the entity's body
            impactY = mop.entityHit.posY + (mop.entityHit.height / 2.0F);
            impactZ = mop.entityHit.posZ;
        } else if (mop.hitVec != null) {
            impactX = mop.hitVec.xCoord;
            impactY = mop.hitVec.yCoord;
            impactZ = mop.hitVec.zCoord;
        }

        EntityLivingBase thrower = this.getThrower();

        double halfWidth = this.areaWidth / 2.0;
        double halfHeight = this.areaHeight / 2.0;

        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            impactX - halfWidth,
            impactY - halfHeight,
            impactZ - halfWidth,
            impactX + halfWidth,
            impactY + halfHeight,
            impactZ + halfWidth);

        List<EntityLivingBase> list = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box);

        for (EntityLivingBase target : list) {
            if (target.isDead || target.getHealth() <= 0) continue;
            if (!this.affectThrower && target == thrower) continue;

            if ("circle".equals(this.areaShape)) {
                double dx = target.posX - impactX;
                double dz = target.posZ - impactZ;
                if (dx * dx + dz * dz > halfWidth * halfWidth) {
                    continue;
                }
            }

            if (this.respectWalls) {
                MovingObjectPosition wallMop = this.worldObj.rayTraceBlocks(
                    net.minecraft.util.Vec3.createVectorHelper(impactX, impactY, impactZ),
                    net.minecraft.util.Vec3
                        .createVectorHelper(target.posX, target.posY + target.getEyeHeight(), target.posZ));
                if (wallMop != null && wallMop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    continue;
                }
            }

            if (CombatTargeting.canDamage(thrower, target, false)) {
                MarkApplyContext context = new MarkApplyContext.Builder().source(thrower)
                    .requestedStacks(this.markStacks)
                    .worldTime(this.worldObj.getTotalWorldTime())
                    .applicationId("corruption_bomb")
                    .refreshDuration(true)
                    .triggerImmediate(true)
                    .build();

                MarkManager.apply(target, this.markId, context);
            }
        }

        // 16388 is the metadata for Splash Potion of Poison (green particles)
        this.worldObj.playAuxSFX(
            2002,
            (int) Math.round(this.posX),
            (int) Math.round(this.posY),
            (int) Math.round(this.posZ),
            16388);

        // Add a mix of purple witch magic particles on the server side
        if (this.worldObj instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) this.worldObj;
            // Spawn 50 witchMagic particles, distributed in a 1.25 radius, with slight speed
            ws.func_147487_a("witchMagic", this.posX, this.posY + 0.5D, this.posZ, 50, 1.25D, 0.5D, 1.25D, 0.05D);
        }

        this.setDead();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setString("MarkId", this.markId);
        compound.setInteger("MarkStacks", this.markStacks);
        compound.setFloat("AreaWidth", this.areaWidth);
        compound.setFloat("AreaHeight", this.areaHeight);
        compound.setString("AreaShape", this.areaShape);
        compound.setBoolean("RespectWalls", this.respectWalls);
        compound.setBoolean("AffectThrower", this.affectThrower);
        compound.setFloat("ProjectileGravity", this.projectileGravity);
        compound.setInteger("ProjectileLifetime", this.projectileLifetime);
        compound.setInteger("AgeTicks", this.ageTicks);
        compound.setBoolean("Impacted", this.impacted);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("MarkId")) this.markId = compound.getString("MarkId");
        if (compound.hasKey("MarkStacks")) this.markStacks = compound.getInteger("MarkStacks");
        if (compound.hasKey("AreaWidth")) this.areaWidth = compound.getFloat("AreaWidth");
        if (compound.hasKey("AreaHeight")) this.areaHeight = compound.getFloat("AreaHeight");
        if (compound.hasKey("AreaShape")) this.areaShape = compound.getString("AreaShape");
        if (compound.hasKey("RespectWalls")) this.respectWalls = compound.getBoolean("RespectWalls");
        if (compound.hasKey("AffectThrower")) this.affectThrower = compound.getBoolean("AffectThrower");
        if (compound.hasKey("ProjectileGravity")) this.projectileGravity = compound.getFloat("ProjectileGravity");
        if (compound.hasKey("ProjectileLifetime")) this.projectileLifetime = compound.getInteger("ProjectileLifetime");
        if (compound.hasKey("AgeTicks")) this.ageTicks = compound.getInteger("AgeTicks");
        if (compound.hasKey("Impacted")) this.impacted = compound.getBoolean("Impacted");

        if (this.impacted) {
            this.setDead();
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeFloat(this.areaWidth);
        buffer.writeFloat(this.areaHeight);
        ByteBufUtils.writeUTF8String(buffer, this.areaShape);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        this.areaWidth = buffer.readFloat();
        this.areaHeight = buffer.readFloat();
        this.areaShape = ByteBufUtils.readUTF8String(buffer);
    }
}
