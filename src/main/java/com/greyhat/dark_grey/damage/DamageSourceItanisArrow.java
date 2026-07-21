package com.greyhat.dark_grey.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EntityDamageSourceIndirect;

public class DamageSourceItanisArrow extends EntityDamageSourceIndirect {

    private boolean ignoreHurtResistantTime = true;
    private boolean bypassArmor = false;

    public DamageSourceItanisArrow(String type, Entity source, Entity indirectEntity) {
        super(type, source, indirectEntity);
        this.setProjectile();
    }

    public DamageSourceItanisArrow setIgnoreHurtResistantTime(boolean ignore) {
        this.ignoreHurtResistantTime = ignore;
        return this;
    }

    public DamageSourceItanisArrow setBypassArmor(boolean bypass) {
        this.bypassArmor = bypass;
        if (bypass) {
            this.setDamageBypassesArmor();
        }
        return this;
    }

    public void applyPreDamage(EntityLivingBase target) {
        if (this.ignoreHurtResistantTime && target != null) {
            target.hurtResistantTime = 0;
        }
    }
}
