package com.greyhat.dark_grey.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

/** Central factory for DarkGrey damage sources that must retain their owner. */
public final class RPGDamageSources {

    private RPGDamageSources() {}

    /**
     * Creates arrow-compatible indirect damage while retaining the real shooter.
     *
     * <p>
     * CustomNPC+ and several combat plugins distinguish the vanilla {@code arrow}
     * type from the generic {@code thrown} type even when both are projectiles.
     * </p>
     */
    public static DamageSource causeArrowDamage(Entity projectile, EntityLivingBase shooter) {
        return new EntityDamageSourceIndirect("arrow", projectile, shooter).setProjectile();
    }

    /** Creates magic damage attributed to its caster instead of anonymous global magic. */
    public static DamageSource causeCasterMagicDamage(EntityLivingBase caster) {
        DamageSource source = causeCasterDamage(caster);
        return source == null ? null : source.setMagicDamage();
    }

    /** Creates direct living damage with the correct player/mob attribution. */
    public static DamageSource causeCasterDamage(EntityLivingBase caster) {
        if (caster instanceof EntityPlayer) {
            return DamageSource.causePlayerDamage((EntityPlayer) caster);
        }
        if (caster != null) {
            return DamageSource.causeMobDamage(caster);
        }
        return null;
    }

    /**
     * Creates the absolute player-owned damage used by Suspended Clockhand's
     * "Fool's Hanging" skill.
     *
     * <p>
     * Keeping the vanilla {@code player} damage type and the real player as the
     * source preserves loot, experience and NPC death attribution. The dedicated
     * subtype lets DarkGrey exclude this skill from its own weapon/set proc chain.
     * </p>
     */
    public static DamageSource causeFoolsHangingDamage(EntityPlayer player) {
        return new FoolsHangingDamageSource(player);
    }

    public static boolean isFoolsHangingDamage(DamageSource source) {
        return source instanceof FoolsHangingDamageSource;
    }

    /** Creates indirect magic damage for Underground Sun. */
    public static DamageSource causeUndergroundSunDamage(Entity orb, EntityLivingBase shooter) {
        return new EntityDamageSourceIndirect("magic", orb, shooter).setMagicDamage();
    }

    public static DamageSource causeRedSunBurnSwitchDamage(EntityLivingBase shooter) {
        if (shooter != null) {
            return new net.minecraft.util.EntityDamageSource("red_sun_burn_switch", shooter).setDamageBypassesArmor();
        } else {
            return new net.minecraft.util.DamageSource("red_sun_burn_switch").setDamageBypassesArmor();
        }
    }

    public static DamageSource causeRedSunFireballDamage(Entity projectile, EntityLivingBase owner) {
        return new EntityDamageSourceIndirect("red_sun_fireball", projectile, owner).setProjectile()
            .setMagicDamage();
    }

    public static DamageSource causeBeyondStarSatelliteDamage(Entity projectile, EntityLivingBase owner) {
        return new EntityDamageSourceIndirect("beyond_star_satellite", projectile, owner).setProjectile()
            .setMagicDamage();
    }

    public static DamageSource causeBoneMeteorDamage(Entity projectile, EntityLivingBase owner) {
        return new EntityDamageSourceIndirect("bone_meteor", projectile, owner).setProjectile()
            .setMagicDamage();
    }

    public static boolean isSwitchDamage(DamageSource source) {
        return "red_sun_burn_switch".equals(source.damageType);
    }

    public static boolean isMarkDamage(DamageSource source) {
        return source != null && source.damageType != null && source.damageType.startsWith("mark_");
    }

    public static DamageSource causeMarkDamage(String markId, EntityLivingBase shooter) {
        DamageSource source;
        if (shooter != null) {
            source = new net.minecraft.util.EntityDamageSource("mark_" + markId, shooter);
        } else {
            source = new net.minecraft.util.DamageSource("mark_" + markId);
        }

        if ("poison".equals(markId)) {
            if (com.greyhat.dark_grey.common.Config.poisonDamageBypassesArmor) {
                source.setDamageBypassesArmor();
            }
            if (com.greyhat.dark_grey.common.Config.poisonDamageBypassesMagicResistance) {
                source.setMagicDamage();
            }
        }
        return source;
    }

    /**
     * Deals damage to the target without triggering Minecraft's invulnerability frames (i-frames).
     * This ensures that DOT effects (like Poison) do not block the player's normal melee attacks.
     */
    public static boolean dealDamageWithoutInvulnerability(EntityLivingBase target, DamageSource source, float damage) {
        int oldHurtResistantTime = target.hurtResistantTime;
        int oldHurtTime = target.hurtTime;

        try {
            // Temporarily reset resistance so the attack connects.
            target.hurtResistantTime = 0;
            return target.attackEntityFrom(source, damage);
        } finally {
            // Never let an exception or canceled damage corrupt later combat timing.
            target.hurtResistantTime = oldHurtResistantTime;
            target.hurtTime = oldHurtTime;
        }
    }

    /**
     * Deals one independent hit while retaining the longer of the target's old and
     * newly-created invulnerability frames.
     *
     * <p>
     * This is intended for multi-projectile attacks whose separate projectiles must
     * all deal damage. Unlike {@link #dealDamageWithoutInvulnerability}, a successful
     * hit still leaves the target with normal post-hit protection.
     * </p>
     */
    public static boolean dealIndependentProjectileDamage(EntityLivingBase target, DamageSource source, float damage) {
        int oldHurtResistantTime = target.hurtResistantTime;
        int oldHurtTime = target.hurtTime;

        try {
            target.hurtResistantTime = 0;
            return target.attackEntityFrom(source, damage);
        } finally {
            target.hurtResistantTime = Math.max(oldHurtResistantTime, target.hurtResistantTime);
            target.hurtTime = Math.max(oldHurtTime, target.hurtTime);
        }
    }

    private static final class FoolsHangingDamageSource extends EntityDamageSource {

        private FoolsHangingDamageSource(EntityPlayer player) {
            super("player", player);
            this.setDamageBypassesArmor();
            this.setDamageIsAbsolute();
            this.setDamageAllowedInCreativeMode();
        }
    }
}
