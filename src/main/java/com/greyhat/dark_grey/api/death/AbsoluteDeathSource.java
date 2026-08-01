package com.greyhat.dark_grey.api.death;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.Loader;

public class AbsoluteDeathSource extends EntityDamageSource {

    private final AbsoluteDeathReason reason;

    public AbsoluteDeathSource(AbsoluteDeathReason reason, Entity executor) {
        super(getDamageTypeForCompat(), executor);
        this.reason = reason;
        this.setDamageBypassesArmor();
        this.setDamageAllowedInCreativeMode();
        this.setDamageIsAbsolute();
    }

    private static String getDamageTypeForCompat() {
        if (Loader.isModLoaded("Avaritia")) {
            return "infinity";
        } else {
            return "dark_grey_absolute_death";
        }
    }

    public AbsoluteDeathReason getReason() {
        return this.reason;
    }

    @Override
    public IChatComponent func_151519_b(EntityLivingBase target) { // getDeathMessage
        String translationKey = "death.attack.dark_grey." + reason.name()
            .toLowerCase();

        if (reason == AbsoluteDeathReason.WOLFSBANE_ROULETTE) {
            return new ChatComponentTranslation(translationKey, target.func_145748_c_());
        }

        EntityLivingBase attacker = target.func_94060_bK();
        if (attacker != null && attacker != target) {
            return new ChatComponentTranslation(
                translationKey + ".player",
                target.func_145748_c_(),
                attacker.func_145748_c_());
        } else {
            return new ChatComponentTranslation(translationKey, target.func_145748_c_());
        }
    }
}
