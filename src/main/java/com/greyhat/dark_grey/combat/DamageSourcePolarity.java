package com.greyhat.dark_grey.combat;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSourceIndirect;

public class DamageSourcePolarity extends EntityDamageSourceIndirect {

    public DamageSourcePolarity(String damageType, Entity source, Entity indirectEntity) {
        super(damageType, source, indirectEntity);
    }
}
