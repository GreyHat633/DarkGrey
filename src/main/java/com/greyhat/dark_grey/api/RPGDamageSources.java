package com.greyhat.dark_grey.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
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

    public static boolean isSwitchDamage(DamageSource source) {
        return "red_sun_burn_switch".equals(source.damageType);
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
}
