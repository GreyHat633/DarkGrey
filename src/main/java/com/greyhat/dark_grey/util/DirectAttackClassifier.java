package com.greyhat.dark_grey.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

public final class DirectAttackClassifier {

    private DirectAttackClassifier() {}

    public static boolean isDirectAttack(DamageSource source) {
        if (source == null) return false;

        if (source.isDamageAbsolute() || source.isUnblockable()
            || source.isFireDamage()
            || source.isExplosion()
            || source.isProjectile()
            || source.isMagicDamage()) {
            return false;
        }

        if (!(source.getEntity() instanceof EntityLivingBase)) {
            return false;
        }
        if (source.getSourceOfDamage() != null && source.getSourceOfDamage() != source.getEntity()) {
            return false;
        }

        String type = source.damageType;
        if (type == null) return false;

        if (type.startsWith("mark_") || type.startsWith("shattered_bone_")) {
            return false;
        }

        if (type.equals("magic") || type.equals("indirectMagic") || type.equals("wither") || type.equals("thorns")) {
            return false;
        }

        if (type.equals("inWall") || type.equals("drown")
            || type.equals("starve")
            || type.equals("cactus")
            || type.equals("fall")
            || type.equals("outOfWorld")
            || type.equals("generic")
            || type.equals("onFire")) {
            return false;
        }

        return true;
    }
}
