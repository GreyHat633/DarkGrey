package com.greyhat.dark_grey.api.impl;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

import java.util.List;

/**
 * "切割" (Cleave) Component
 * Deals bonus damage equal to a percentage of the target's maximum health.
 */
public class CleaveComponent implements IRPGComponent, IOnHit, IHasTooltip {

    // Default 10% max HP damage
    private float maxHpPercent = 0.10F;

    @Override
    public String getComponentId() {
        return "切割";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("percent")) {
            this.maxHpPercent = params.get("percent").getAsFloat();
        }
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float actualDamage) {
        if (attacker.worldObj.isRemote) {
            return;
        }

        // Calculate bonus damage based on target's max health
        float bonusDamage = target.getMaxHealth() * maxHpPercent;
        
        // Ensure bonus damage is at least 1, just in case
        if (bonusDamage < 1.0F) {
            bonusDamage = 1.0F;
        }

        // Create a damage source. If attacker is player, use player damage source.
        DamageSource source = null;
        if (attacker instanceof EntityPlayer) {
            source = new net.minecraft.util.EntityDamageSource("player", attacker).setDamageBypassesArmor().setDamageIsAbsolute();
        } else {
            source = new net.minecraft.util.EntityDamageSource("mob", attacker).setDamageBypassesArmor().setDamageIsAbsolute();
        }

        // Bypass attackEntityFrom completely to avoid extreme lag caused by third-party mod loops.
        float newHealth = target.getHealth() - bonusDamage;
        if (newHealth <= 0.0F) {
            target.setHealth(0.0F);
            target.onDeath(source);
        } else {
            target.setHealth(newHealth);
        }
    }

    @Override
    public void addTooltipLines(ItemStack itemStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        int displayPercent = Math.round(maxHpPercent * 100);
        // \u00A7c is red color (§c), \u00A77 is gray (§7)
        tooltipLines.add("\u00A74\u2694 \u5207\u5272 \u00A77| \u00A7c\u9644\u5E26\u76EE\u6807\u6700\u5927\u751F\u547D\u503C " + displayPercent + "% \u7684\u4F24\u5BB3");
        // Output: §4⚔ 切割 §7| §c附带目标最大生命值 X% 的伤害
    }
}