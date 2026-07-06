//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.World;

public class EntityScythe extends Entity {

    private EntityLivingBase owner;

    public EntityScythe(final World worldIn) {
        super(worldIn);
        this.setSize(1.0f, 1.0f);
    }

    public EntityScythe(final World worldIn, final EntityLivingBase owner) {
        super(worldIn);
        this.owner = owner;
        this.setSize(1.0f, 1.0f);
        this.setPosition(owner.posX, owner.posY + owner.getEyeHeight() / 2.0f, owner.posZ);
    }

    protected void entityInit() {}

    public void onUpdate() {
        super.onUpdate();
        if (this.owner != null && !this.owner.isDead) {
            this.setPosition(this.owner.posX, this.owner.posY + this.owner.getEyeHeight() / 2.0f, this.owner.posZ);
        }
        if (this.worldObj.isRemote) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.thePlayer.getDistanceSqToEntity(this) < 4096.0D) {
                final float current_angle = this.ticksExisted / 20.0f * 3.1415927f * 2.0f;
                final double base_radius = 3.5;
                int maxParticles = (int) (60 * com.greyhat.dark_grey.common.Config.particleDensity);
                for (int i = 0; i < maxParticles; ++i) {
                    final float angle_offset = current_angle - i * 0.08f;
                    final double radius = base_radius + (Math.random() - 0.5) * 1.5;
                    final double px = this.posX + Math.sin(angle_offset) * radius;
                    final double pz = this.posZ + Math.cos(angle_offset) * radius;
                    final double py = this.posY + (Math.random() - 0.5) * 1.5;

                    this.worldObj.spawnParticle("reddust", px, py, pz, 0.0, 0.0, 0.0);

                    if (i % 2 == 0) {
                        this.worldObj.spawnParticle("largesmoke", px, py, pz, 0.0, 0.0, 0.0);
                    }

                    if (i % 3 == 0) {
                        this.worldObj.spawnParticle("magicCrit", px, py, pz, 0.0, 0.0, 0.0);
                    }
                }
            }
        }
        if (!this.worldObj.isRemote) {
            if (this.ticksExisted == 10) {
                final double radius2 = 3.5;
                final AxisAlignedBB aabb = this.boundingBox.expand(radius2, radius2, radius2);
                final List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity) this.owner, aabb);
                final DamageSource source = (DamageSource) ((this.owner != null)
                    ? new EntityDamageSource("player", (Entity) this.owner)
                    : DamageSource.magic);
                final float damage = 35.0f;
                for (final Entity entity : list) {
                    if (entity instanceof EntityLivingBase && entity != this.owner
                        && this.getDistanceSqToEntity(entity) <= radius2 * radius2) {
                        entity.attackEntityFrom(source, damage);
                    }
                }
            }
            if (this.ticksExisted >= 20) {
                this.setDead();
            }
        }
    }

    protected void readEntityFromNBT(final NBTTagCompound compound) {
        this.ticksExisted = compound.getInteger("TicksAlive");
        this.setDead();
    }

    protected void writeEntityToNBT(final NBTTagCompound compound) {
        compound.setInteger("TicksAlive", this.ticksExisted);
    }
}
