package com.greyhat.dark_grey.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.RPGDamageSources;

public class EntityAuraTorrent extends Entity {

    private float dotDamage = 250.0f;
    private int maxAge = 200; // 10 seconds
    private EntityLivingBase caster;

    public EntityAuraTorrent(World world) {
        super(world);
        this.setSize(0.1F, 0.1F);
    }

    public EntityAuraTorrent(World world, EntityLivingBase caster, double x, double y, double z, float radius,
        float dotDamage, int maxAge) {
        this(world);
        this.caster = caster;
        this.setPosition(x, y, z);
        this.setRadius(radius);
        this.dotDamage = dotDamage;
        this.maxAge = Math.max(20, maxAge);
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

    /** Applies one server-authoritative pulse without creating knockback. */
    public static boolean applyAuraEffect(EntityLivingBase caster, EntityLivingBase target, float damage) {
        if (caster == null || target == null
            || caster.worldObj == null
            || caster.worldObj.isRemote
            || caster.worldObj != target.worldObj
            || !CombatTargeting.canDamage(caster, target, false)) {
            return false;
        }

        double motionX = target.motionX;
        double motionY = target.motionY;
        double motionZ = target.motionZ;
        DamageSource source = RPGDamageSources.causeCasterMagicDamage(caster);
        boolean damaged = source != null && target.attackEntityFrom(source, damage);

        target.motionX = motionX;
        target.motionY = motionY;
        target.motionZ = motionZ;
        target.isAirBorne = false;

        if (!damaged) {
            return false;
        }

        addPotionEffectSafely(target, Potion.confusion, 0);
        addPotionEffectSafely(target, Potion.weakness, 1);
        addPotionEffectSafely(target, Potion.blindness, 0);
        addPotionEffectSafely(target, Potion.moveSlowdown, 1);
        addPotionEffectSafely(target, Potion.digSlowdown, 1);
        return true;
    }

    private static void addPotionEffectSafely(EntityLivingBase target, Potion potion, int amplifier) {
        if (potion == null) {
            return;
        }
        try {
            target.addPotionEffect(new PotionEffect(potion.id, 20, amplifier));
        } catch (RuntimeException ignored) {
            // Some modded living entities reject individual potion effects. One
            // incompatible debuff must not cancel the remaining pulse or kick players.
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.ticksExisted > maxAge) {
            this.setDead();
            return;
        }

        if (!this.worldObj.isRemote
            && (this.caster == null || this.caster.isDead || this.caster.worldObj != this.worldObj)) {
            this.setDead();
            return;
        }

        float currentRadius = this.getRadius();

        // Spawn particles
        if (this.worldObj.isRemote) {
            net.minecraft.entity.player.EntityPlayer viewer = this.worldObj.getClosestPlayerToEntity(this, 64.0D);
            if (viewer != null) {
                if (this.ticksExisted % 2 == 0) {
                    // Outer ring: largesmoke
                    int ringCount = (int) ((10 + (int) (currentRadius * 8.0))
                        * com.greyhat.dark_grey.common.Config.particleDensity);
                    for (int i = 0; i < ringCount; i++) {
                        double angle = this.rand.nextDouble() * Math.PI * 2;
                        double px = this.posX + Math.cos(angle) * currentRadius;
                        double pz = this.posZ + Math.sin(angle) * currentRadius;
                        this.worldObj.spawnParticle("largesmoke", px, this.posY, pz, 0, 0, 0);
                    }

                    // Inner area: random potion particles
                    int innerCount = (int) ((currentRadius * 1.5)
                        * com.greyhat.dark_grey.common.Config.particleDensity);
                    for (int i = 0; i < innerCount; i++) {
                        double r = this.rand.nextDouble() * currentRadius;
                        double angle = this.rand.nextDouble() * Math.PI * 2;
                        double px = this.posX + Math.cos(angle) * r;
                        double pz = this.posZ + Math.sin(angle) * r;

                        String[] spells = { "mobSpell", "mobSpellAmbient", "witchMagic" };
                        String spell = spells[this.rand.nextInt(spells.length)];

                        double rColor = this.rand.nextDouble();
                        double gColor = this.rand.nextDouble();
                        double bColor = this.rand.nextDouble();

                        if (spell.equals("mobSpell") || spell.equals("mobSpellAmbient")) {
                            this.worldObj.spawnParticle(
                                spell,
                                px,
                                this.posY + this.rand.nextDouble() * 1.5,
                                pz,
                                rColor,
                                gColor,
                                bColor);
                        } else {
                            this.worldObj
                                .spawnParticle("witchMagic", px, this.posY + this.rand.nextDouble() * 1.5, pz, 0, 0, 0);
                        }
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
                    if (this.getDistanceSqToEntity(entity) <= currentRadius * currentRadius) {
                        EntityLivingBase target = (EntityLivingBase) entity;
                        applyAuraEffect(this.caster, target, dotDamage);
                    }
                }
            }
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        this.setRadius(tag.getFloat("Radius"));
        this.dotDamage = tag.getFloat("DotDamage");
        this.setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setFloat("Radius", this.getRadius());
        tag.setFloat("DotDamage", this.dotDamage);
        tag.setInteger("MaxAge", this.maxAge);
    }
}
