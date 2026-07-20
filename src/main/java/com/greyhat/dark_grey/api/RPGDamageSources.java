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
}
