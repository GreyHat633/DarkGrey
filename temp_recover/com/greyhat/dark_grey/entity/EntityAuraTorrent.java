package com.greyhat.dark_grey.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;

public class EntityAuraTorrent extends Entity {
    
    private double radius = 0.0;
    private float dotDamage = 1.0F;
    private int durationTicks = 200; // 10 seconds
    private EntityPlayer owner;

    public EntityAuraTorrent(World world) {
        super(world);
        this.setSize(1.0F, 1.0F);
        this.noClip = true;
    }

    public EntityAuraTorrent(World world, EntityPlayer owner, double radius, float dotDamage, int durationTicks) {
        this(world);
        this.owner = owner;
        this.radius = radius;
        this.dotDamage = dotDamage;
        this.durationTicks = durationTicks;
        this.setPosition(owner.posX, owner.posY + 1.0, owner.posZ);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.worldObj.isRemote) {
            this.durationTicks--;
            if (this.durationTicks <= 0) {
                this.setDead();
                return;
            }

            // Deal damage and debuff every 10 ticks
            if (this.ticksExisted % 10 == 0) {
                AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                    this.posX - radius, this.posY - 2.0, this.posZ - radius,
                    this.posX + radius, this.posY + 2.0, this.posZ + radius
                );

                @SuppressWarnings("unchecked")
                List<EntityLivingBase> entities = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb);

                for (EntityLivingBase target : entities) {
                    if (target == owner) continue;
                    
                    double distSq = this.getDistanceSqToEntity(target);
                    if (distSq <= radius * radius) {
                        // Deal true damage without knockback
                        float newHealth = target.getHealth() - this.dotDamage;
                        if (newHealth <= 0.0F) {
                            target.setHealth(0.0F);
                            DamageSource magic = new net.minecraft.util.EntityDamageSource("magic", owner).setDamageBypassesArmor().setDamageIsAbsolute();
                            target.onDeath(magic);
                        } else {
                            target.setHealth(newHealth);
                            worldObj.playSoundAtEntity(target, "game.player.hurt", 1.0F, 1.0F);
                        }
                        
                        // Apply debuffs (1 second = 20 ticks)
                        target.addPotionEffect(new PotionEffect(Potion.confusion.id, 20, 0));
                        target.addPotionEffect(new PotionEffect(Potion.weakness.id, 20, 1));
                        target.addPotionEffect(new PotionEffect(Potion.blindness.id, 20, 0));
                        target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 1));
                        target.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 20, 1));
                    }
                }
            }
        } else {
            // Client-side rendering
            // Edge particles
            int edgeParticles = 12;
            for (int i = 0; i < edgeParticles; i++) {
                double angle = worldObj.rand.nextDouble() * 2 * Math.PI;
                double px = this.posX + radius * Math.cos(angle);
                double pz = this.posZ + radius * Math.sin(angle);
                double py = this.posY + (worldObj.rand.nextDouble() - 0.5) * 0.2;
                
                worldObj.spawnParticle("cloud", px, py, pz, 0, 0.01, 0);
            }
            
            // Dense inner particles using potion particles
            int innerParticles = 8;
            for (int i = 0; i < innerParticles; i++) {
                double innerRadius = worldObj.rand.nextDouble() * radius;
                double angle = worldObj.rand.nextDouble() * 2 * Math.PI;
                double px = this.posX + innerRadius * Math.cos(angle);
                double pz = this.posZ + innerRadius * Math.sin(angle);
                double py = this.posY + (worldObj.rand.nextDouble() - 0.5) * 1.0;
                
                // Purple-ish black color for a curse aura
                worldObj.spawnParticle("mobSpell", px, py, pz, 0.5, 0.0, 0.5);
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tagCompound) {
        this.radius = tagCompound.getDouble("Radius");
        this.dotDamage = tagCompound.getFloat("DotDamage");
        this.durationTicks = tagCompound.getInteger("DurationTicks");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        tagCompound.setDouble("Radius", this.radius);
        tagCompound.setFloat("DotDamage", this.dotDamage);
        tagCompound.setInteger("DurationTicks", this.durationTicks);
    }
}