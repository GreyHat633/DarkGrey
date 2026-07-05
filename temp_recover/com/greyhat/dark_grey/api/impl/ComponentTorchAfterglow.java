package com.greyhat.dark_grey.api.impl;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComponentTorchAfterglow implements IRPGComponent, IOnHeldTick, IHasTooltip {

    @Override
    public String getComponentId() {
        return "炬火的残光";
    }

    @Override
    public void configure(JsonObject params) {
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add("\u00A76\u263C \u70AC\u706B\u7684\u6B8B\u5149 \u00A77| \u00A7e\u6301\u6709\u65F6\u514D\u75AB\u4E00\u5207\u8D1F\u9762\u72B6\u6001");
    }

    @Override
    public void onHeldTick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }

        // 优化：每 10 tick（0.5秒）执行一次批量清除
        if (world.getTotalWorldTime() % 10 != 0) {
            return;
        }

        @SuppressWarnings("unchecked")
        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
        if (activeEffects.isEmpty()) return;

        List<Integer> toRemove = new ArrayList<>();
        for (PotionEffect effect : activeEffects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion != null && potion.isBadEffect()) {
                toRemove.add(effect.getPotionID());
            }
        }

        for (Integer id : toRemove) {
            player.removePotionEffect(id);
        }
    }
}