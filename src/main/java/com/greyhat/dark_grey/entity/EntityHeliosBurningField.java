package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.type.ScorchMarkType;

public class EntityHeliosBurningField extends Entity {

    private EntityPlayer owner;
    private int remainingTicks = 120;
    private int intervalTicks = 10;
    private float damage = 60.0f;
    private float scorchApplyChance = 0.40f;
    private float width = 4.0f;
    private float length = 6.0f;
    private float height = 4.0f;

    private float forwardX;
    private float forwardZ;
    private float rightX;
    private float rightZ;

    public EntityHeliosBurningField(World world) {
        super(world);
        this.setSize(width, height);
    }

    public EntityHeliosBurningField(World world, EntityPlayer owner) {
        super(world);
        this.owner = owner;
        this.setSize(width, height);

        // Position at owner's feet + front edge
        double yawRad = Math.toRadians(owner.rotationYaw);
        this.forwardX = (float) -Math.sin(yawRad);
        this.forwardZ = (float) Math.cos(yawRad);
        this.rightX = (float) Math.cos(yawRad);
        this.rightZ = (float) Math.sin(yawRad);

        // Center of the field (length is 3, so center is 1.5 blocks forward)
        this.setPosition(
            owner.posX + forwardX * (length / 2.0f),
            owner.boundingBox.minY,
            owner.posZ + forwardZ * (length / 2.0f));
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (worldObj.isRemote) {
            // Spawn particles mostly on the boundaries to show the range, with few inside
            for (int i = 0; i < 10; i++) {
                double px = posX + (worldObj.rand.nextDouble() - 0.5) * width;
                double py = posY + worldObj.rand.nextDouble() * height;
                double pz = posZ + (worldObj.rand.nextDouble() - 0.5) * length;

                // Force to a boundary face
                int face = worldObj.rand.nextInt(3);
                if (face == 0) {
                    px = posX + (worldObj.rand.nextBoolean() ? 0.5 : -0.5) * width;
                } else if (face == 1) {
                    py = posY + (worldObj.rand.nextBoolean() ? height : 0.0);
                } else {
                    pz = posZ + (worldObj.rand.nextBoolean() ? 0.5 : -0.5) * length;
                }

                worldObj.spawnParticle("reddust", px, py, pz, 1.0, 0.4, 0.0);
                if (worldObj.rand.nextInt(4) == 0) {
                    worldObj.spawnParticle("largesmoke", px, py, pz, 0, 0.0, 0);
                }
            }

            // Spawn a few particles inside
            for (int i = 0; i < 2; i++) {
                double px = posX + (worldObj.rand.nextDouble() - 0.5) * width;
                double py = posY + worldObj.rand.nextDouble() * height;
                double pz = posZ + (worldObj.rand.nextDouble() - 0.5) * length;

                worldObj.spawnParticle("reddust", px, py, pz, 1.0, 0.4, 0.0);
                if (worldObj.rand.nextInt(5) == 0) {
                    worldObj.spawnParticle("largesmoke", px, py, pz, 0, 0.0, 0);
                }
            }
        } else {
            remainingTicks--;

            if (remainingTicks < 0) {
                this.setDead();
                return;
            }

            if (remainingTicks % intervalTicks == 0) {
                pulseDamage();
            }
        }
    }

    private void pulseDamage() {
        // Enlarge AABB to definitely cover the rotated box
        float maxRadius = Math.max(width, length);
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            posX - maxRadius,
            posY,
            posZ - maxRadius,
            posX + maxRadius,
            posY + height,
            posZ + maxRadius);

        List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, aabb);
        for (Entity e : list) {
            if (!(e instanceof EntityLivingBase)) continue;
            EntityLivingBase target = (EntityLivingBase) e;
            if (owner != null && !CombatTargeting.canDamage(owner, target, false)) continue;

            // Transform target bounding box relative to field center
            double dx = target.posX - this.posX;
            double dz = target.posZ - this.posZ;

            // Project onto forward and right vectors
            double localZ = dx * forwardX + dz * forwardZ; // along forward axis
            double localX = dx * rightX + dz * rightZ; // along right axis

            // Check if within bounds (+ target width / 2 for forgiveness)
            double margin = target.width / 2.0;
            if (Math.abs(localX) <= (width / 2.0 + margin) && Math.abs(localZ) <= (length / 2.0 + margin)
                && target.posY <= this.posY + height
                && target.posY + target.height >= this.posY) {

                // Raytrace wall check
                if (owner != null) {
                    Vec3 center = Vec3.createVectorHelper(this.posX, this.posY + height / 2.0, this.posZ);
                    Vec3 tCenter = Vec3.createVectorHelper(target.posX, target.posY + target.height / 2.0, target.posZ);
                    if (worldObj.rayTraceBlocks(center, tCenter) != null
                        && worldObj.rayTraceBlocks(center, tCenter).typeOfHit
                            == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                        continue;
                    }
                }

                DamageSource src = owner != null ? DamageSource.causePlayerDamage(owner) : DamageSource.generic;
                src.setFireDamage();

                int previousHurtResistant = target.hurtResistantTime;
                target.hurtResistantTime = 0; // Allow damage even if just hit by bullet

                double mx = target.motionX;
                double my = target.motionY;
                double mz = target.motionZ;

                if (target.attackEntityFrom(src, damage)) {
                    target.motionX = mx;
                    target.motionY = my;
                    target.motionZ = mz;
                    if (worldObj.rand.nextFloat() < scorchApplyChance) {
                        MarkManager.apply(target, ScorchMarkType.ID, 1, owner != null ? owner : target);
                    }
                }

                target.hurtResistantTime = previousHurtResistant; // Restore if we didn't want to clear it completely
                                                                  // for everything
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        remainingTicks = nbt.getInteger("RemainingTicks");
        forwardX = nbt.getFloat("ForwardX");
        forwardZ = nbt.getFloat("ForwardZ");
        rightX = nbt.getFloat("RightX");
        rightZ = nbt.getFloat("RightZ");

        // Don't keep around fields if loaded without owner context reliably, or we can just let them die quickly
        if (remainingTicks <= 0) this.setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("RemainingTicks", remainingTicks);
        nbt.setFloat("ForwardX", forwardX);
        nbt.setFloat("ForwardZ", forwardZ);
        nbt.setFloat("RightX", rightX);
        nbt.setFloat("RightZ", rightZ);
    }
}
