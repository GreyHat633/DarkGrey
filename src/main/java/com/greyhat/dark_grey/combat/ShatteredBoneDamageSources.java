package com.greyhat.dark_grey.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

public final class ShatteredBoneDamageSources {

    private ShatteredBoneDamageSources() {}

    public static DamageSource causeMovementDamage() {
        return new DamageSource("shattered_bone_movement").setDamageBypassesArmor()
            .setMagicDamage();
    }

    public static DamageSource causeSplashDamage(EntityLivingBase attacker) {
        if (attacker != null) {
            return new EntityDamageSource("shattered_bone_splash", attacker).setDamageBypassesArmor();
        }
        return new DamageSource("shattered_bone_splash").setDamageBypassesArmor();
    }

    public static boolean isSplashDamage(DamageSource source) {
        return source.damageType.equals("shattered_bone_splash");
    }

    public static DamageSource causeShatteredBoneStaffDamage(EntityLivingBase attacker) {
        if (attacker != null) {
            return new EntityDamageSource("shattered_bone_staff", attacker).setMagicDamage();
        }
        return new DamageSource("shattered_bone_staff").setMagicDamage();
    }
}
