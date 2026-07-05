package com.greyhat.dark_grey.api.impl;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class ComponentBloodSacrifice implements IRPGComponent, IOnHit, IHasTooltip {

    private final Random rand = new Random();
    
    private float minCritMultiplier = 3.0f;
    private float maxCritMultiplier = 6.0f;
    private float backlashPercentage = 0.5f; // 50% of dealt damage

    @Override
    public String getComponentId() {
        return "血祭";
    }
    
    @Override
    public void configure(JsonObject params) {
        if (params.has("minCritMultiplier")) {
            minCritMultiplier = params.get("minCritMultiplier").getAsFloat();
        }
        if (params.has("maxCritMultiplier")) {
            maxCritMultiplier = params.get("maxCritMultiplier").getAsFloat();
        }
        if (params.has("backlashPercentage")) {
            backlashPercentage = params.get("backlashPercentage").getAsFloat();
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        int minMulti = (int) minCritMultiplier;
        int maxMulti = (int) maxCritMultiplier;
        int backlashPercent = (int) (backlashPercentage * 100);
        
        tooltip.add("\u00A74\u2726 \u8840\u796D \u00A77| \u00A7c\u6BCF\u6B21\u653B\u51FB\u670915%\u6982\u7387\u66B4\u51FB\uFF0C\u9020\u6210" + minMulti + "-" + maxMulti + "\u500D\u7269\u7406\u4F24\u5BB3");
        tooltip.add("\u00A77   \u00A78(\u66B4\u51FB\u4F1A\u53CD\u566C\u6700\u5927\u751F\u547D\u503C" + backlashPercent + "%\u7684\u8840\u91CF\uFF0C\u53EF\u80FD\u81F4\u6B7B)");
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float actualDamage) {
        World worldObj = attacker.worldObj;
        
        if (!worldObj.isRemote && attacker instanceof EntityPlayer && rand.nextFloat() < 0.15f) {
            EntityPlayer player = (EntityPlayer) attacker;
            
            float critMult = minCritMultiplier + worldObj.rand.nextFloat() * (maxCritMultiplier - minCritMultiplier);
            float extraDamage = actualDamage * (critMult - 1.0f);
            
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), extraDamage);
            
            float backlash = (actualDamage + extraDamage) * backlashPercentage;
            player.attackEntityFrom(DamageSource.magic, backlash);
            
            worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "mob.wither.spawn", 0.5f, 1.5f);
            
            for (int i = 0; i < 15; i++) {
                worldObj.spawnParticle("reddust", 
                        target.posX + (worldObj.rand.nextFloat() - 0.5D) * target.width, 
                        target.posY + worldObj.rand.nextFloat() * target.height, 
                        target.posZ + (worldObj.rand.nextFloat() - 0.5D) * target.width, 
                        0.0D, 0.0D, 0.0D);
            }
        }
    }
}
