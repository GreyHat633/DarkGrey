package com.greyhat.dark_grey.component;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;

public class ComponentBloodSacrifice implements IRPGComponent, IOnHit, IHasTooltip {

    private final Random rand = new Random();

    private float minCritMultiplier = 3.0f;
    private float maxCritMultiplier = 6.0f;
    private float backlashHealthPercentage = 0.05f;
    private static final ThreadLocal<Boolean> APPLYING_BLOOD_SACRIFICE = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    // P0 #1: 修复 DamageSource.magic 全局单例被永久修改的问题，改用组件局部定义的自定义伤害源
    private static final DamageSource BLOOD_SACRIFICE_DAMAGE = new DamageSource("darkgrey.bloodsacrifice")
        .setDamageBypassesArmor()
        .setDamageIsAbsolute()
        .setMagicDamage();

    @Override
    public String getComponentId() {
        return "血祭";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("min_crit_multiplier")) {
            minCritMultiplier = params.get("min_crit_multiplier")
                .getAsFloat();
        }
        if (params.has("max_crit_multiplier")) {
            maxCritMultiplier = params.get("max_crit_multiplier")
                .getAsFloat();
        }
        if (params.has("backlash_health_percentage")) {
            backlashHealthPercentage = params.get("backlash_health_percentage")
                .getAsFloat();
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        int minMulti = (int) minCritMultiplier;
        int maxMulti = (int) maxCritMultiplier;
        int backlashPercent = (int) (backlashHealthPercentage * 100);

        tooltip.add(
            "\u00A74\u2726 \u8840\u796D \u00A77| \u00A7c\u6BCF\u6B21\u653B\u51FB\u670915%\u6982\u7387\u66B4\u51FB\uFF0C\u9020\u6210"
                + minMulti
                + "-"
                + maxMulti
                + "\u500D\u7269\u7406\u4F24\u5BB3");
        tooltip.add(
            "\u00A77   \u00A78(\u66B4\u51FB\u4F1A\u53CD\u566C\u6700\u5927\u751F\u547D\u503C" + backlashPercent
                + "%\u7684\u8840\u91CF\uFF0C\u53EF\u80FD\u81F4\u6B7B)");
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float baseDamage) {
        if (attacker.worldObj.isRemote || target.isDead
            || target.getHealth() <= 0.0F
            || APPLYING_BLOOD_SACRIFICE.get()) {
            return;
        }

        // 15% crit chance
        if (rand.nextFloat() < 0.15f) {
            // Random multiplier between min and max
            float multiplier = minCritMultiplier + (rand.nextFloat() * (maxCritMultiplier - minCritMultiplier));
            float totalDamage = baseDamage * multiplier;

            // Calculate max health backlash
            float backlash = attacker.getMaxHealth() * backlashHealthPercentage;

            APPLYING_BLOOD_SACRIFICE.set(Boolean.TRUE);
            try {
                // Apply bonus damage as physical damage through the normal event pipeline.
                DamageSource source = RPGDamageSources.causeCasterDamage(attacker);
                if (source != null) {
                    target.attackEntityFrom(source, totalDamage - baseDamage);
                }

                if (attacker.getHealth() <= backlash) {
                    attacker.attackEntityFrom(BLOOD_SACRIFICE_DAMAGE, attacker.getMaxHealth() * 1000f);
                } else {
                    attacker.attackEntityFrom(BLOOD_SACRIFICE_DAMAGE, backlash);
                }
            } finally {
                APPLYING_BLOOD_SACRIFICE.set(Boolean.FALSE);
            }

            // spawn particles
            target.worldObj.playSoundAtEntity(attacker, "mob.enderdragon.hit", 1.0F, 0.5F);
        }
    }
}
