package com.greyhat.dark_grey.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public final class WeaponAttackPowerResolver {

    private WeaponAttackPowerResolver() {}

    public static double getBaseAttackPower(EntityLivingBase entity) {
        if (entity == null) return 1.0;

        IAttributeInstance attribute = entity.getEntityAttribute(SharedMonsterAttributes.attackDamage);
        if (attribute != null) {
            return attribute.getAttributeValue();
        }

        return 1.0;
    }
}
