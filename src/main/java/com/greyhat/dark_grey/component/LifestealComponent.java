package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;

/**
 * Lifesteal Component
 * Heals the attacker for a percentage of the damage dealt.
 */
public class LifestealComponent implements IRPGComponent, IOnHit, IHasTooltip {

    private float lifestealPercent = 0.0F;

    @Override
    public String getComponentId() {
        return "吸血";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("percent")) {
            lifestealPercent = params.get("percent")
                .getAsFloat();
        }
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float actualDamage) {
        if (attacker.worldObj.isRemote) {
            return;
        }

        float healAmount = actualDamage * lifestealPercent;
        if (healAmount > 0) {
            attacker.heal(healAmount);
        }
    }

    @Override
    public void addTooltipLines(ItemStack itemStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        int displayPercent = Math.round(lifestealPercent * 100);
        tooltipLines.add(
            "\u00A74\u25C6 \u5438\u8840 \u00A77| \u00A7c+" + displayPercent
                + "% \u653B\u51FB\u4F24\u5BB3\u8F6C\u5316\u4E3A\u751F\u547D\u503C");
    }
}
