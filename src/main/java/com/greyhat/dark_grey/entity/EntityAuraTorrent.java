package com.greyhat.dark_grey.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;

public class EntityAuraTorrent extends Entity {

    private float dotDamage = 20.0f;
    private int maxAge = 200; // 10 seconds
    private EntityLivingBase caster;

    public EntityAuraTorrent(World world) {
        super(world);
        this.setSize(0.1F, 0.1F);
    }

    public EntityAuraTorrent(World world, EntityLivingBase caster, double x, double y, double z, float radius, float dotDamage) {
        this(world);
        this.caster = caster;
        this.setPosition(x, y, z);
        this.setRadius(radius);
        this.dotDamage = dotDamage;
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(18, 5.0f);
    }

    public float getRadius() {
        return this.dataWatcher.getWatchableObjectFloat(18);
    }

    public void setRadius(float radius) {
        this.dataWatcher.updateObject(18, radius);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.ticksExisted > maxAge) {
            this.setDead();
            return;
        }

        float currentRadius = this.getRadius();

        // Spawn particles
        if (this.worldObj.isRemote) {
            if (this.ticksExisted % 2 == 0) {
                // Outer ring: largesmoke
                int ringCount = 10 + (int) (currentRadius * 8.0);
                for (int i = 0; i < ringCount; i++) {
                    double angle = this.rand.nextDouble() * Math.PI * 2;
                    double px = this.posX + Math.cos(angle) * currentRadius;
                    double pz = this.posZ + Math.sin(angle) * currentRadius;
                    this.worldObj.spawnParticle("largesmoke", px, this.posY, pz, 0, 0, 0);
                }

                // Inner area: random potion particles
                int innerCount = (int) (currentRadius * 1.5);
                for (int i = 0; i < innerCount; i++) {
                    double r = this.rand.nextDouble() * currentRadius;
                    double angle = this.rand.nextDouble() * Math.PI * 2;
                    double px = this.posX + Math.cos(angle) * r;
                    double pz = this.posZ + Math.sin(angle) * r;
                    
                    String[] spells = {"mobSpell", "mobSpellAmbient", "witchMagic"};
                    String spell = spells[this.rand.nextInt(spells.length)];
                    
                    double rColor = this.rand.nextDouble();
                    double gColor = this.rand.nextDouble();
                    double bColor = this.rand.nextDouble();
                    
                    if (spell.equals("mobSpell") || spell.equals("mobSpellAmbient")) {
                        this.worldObj.spawnParticle(spell, px, this.posY + this.rand.nextDouble() * 1.5, pz, rColor, gColor, bColor);
                    } else {
                        this.worldObj.spawnParticle("witchMagic", px, this.posY + this.rand.nextDouble() * 1.5, pz, 0, 0, 0);
                    }
                }
            }
        }

        // Damage entities every 10 ticks (0.5s)
        if (!this.worldObj.isRemote && this.ticksExisted % 10 == 0) {
            AxisAlignedBB aabb = this.boundingBox.expand(currentRadius, 2.0, currentRadius);
            @SuppressWarnings("unchecked")
            List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, aabb);
            for (Entity entity : list) {
                if (entity instanceof EntityLivingBase) {
                    if (entity == this.caster) continue;
                    
                    if (this.getDistanceSqToEntity(entity) <= currentRadius * currentRadius) {
                        EntityLivingBase target = (EntityLivingBase) entity;
                        
                        double mx = target.motionX;
                        double my = target.motionY;
                        double mz = target.motionZ;
                        
                        target.attackEntityFrom(DamageSource.magic, dotDamage);
                        
                        target.motionX = mx;
                        target.motionY = my;
                        target.motionZ = mz;
                        target.isAirBorne = false; 
                    }
                }
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        this.setRadius(tag.getFloat("Radius"));
        this.dotDamage = tag.getFloat("DotDamage");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setFloat("Radius", this.getRadius());
        tag.setFloat("DotDamage", this.dotDamage);
    }
}