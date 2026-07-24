package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.RedSunFireballManager;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityRedSunFireball;
import com.greyhat.dark_grey.status.RedSunBurnData;

public class ComponentRedSun
    implements IRPGComponent, IOnHit, IOnRightClick, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IHasTooltip {

    private int maxChargeTicks = 100;
    private float minFireballSize = 1.0F;
    private float maxFireballSize = 5.0F;
    private float minFireballDamage = 100.0F;
    private float maxFireballDamage = 1250.0F;
    private int cooldownTicks = 200;
    private float projectileSpeed = 0.65F;
    private float projectileGravity = 0.03F;
    private float projectileDrag = 0.98F;
    private float projectileUpwardBoost = 0.12F;
    private int projectileLifetime = 200;
    private int burnDurationTicks = 200;
    private float burnSwitchDamage = 10.0F;
    private float burnIncomingDamageMultiplier = 1.20F;
    private boolean ignoreSwitchDamageHurtResistance = true;

    @Override
    public String getComponentId() {
        return "烈阳";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("maxChargeTicks")) maxChargeTicks = Math.max(
            20,
            Math.min(
                1200,
                params.get("maxChargeTicks")
                    .getAsInt()));
        if (params.has("minFireballSize")) minFireballSize = Math.max(
            0.25F,
            Math.min(
                10.0F,
                params.get("minFireballSize")
                    .getAsFloat()));
        if (params.has("maxFireballSize")) maxFireballSize = Math.max(
            minFireballSize,
            Math.min(
                20.0F,
                params.get("maxFireballSize")
                    .getAsFloat()));
        if (params.has("minFireballDamage")) minFireballDamage = Math.max(
            0.0F,
            Math.min(
                100000.0F,
                params.get("minFireballDamage")
                    .getAsFloat()));
        if (params.has("maxFireballDamage")) maxFireballDamage = Math.max(
            minFireballDamage,
            params.get("maxFireballDamage")
                .getAsFloat());
        if (params.has("cooldownTicks")) cooldownTicks = Math.max(
            0,
            Math.min(
                72000,
                params.get("cooldownTicks")
                    .getAsInt()));
        if (params.has("projectileSpeed")) projectileSpeed = Math.max(
            0.1F,
            Math.min(
                5.0F,
                params.get("projectileSpeed")
                    .getAsFloat()));
        if (params.has("projectileGravity")) projectileGravity = Math.max(
            0.0F,
            Math.min(
                1.0F,
                params.get("projectileGravity")
                    .getAsFloat()));
        if (params.has("projectileDrag")) projectileDrag = Math.max(
            0.5F,
            Math.min(
                1.0F,
                params.get("projectileDrag")
                    .getAsFloat()));
        if (params.has("projectileUpwardBoost")) projectileUpwardBoost = Math.max(
            -1.0F,
            Math.min(
                2.0F,
                params.get("projectileUpwardBoost")
                    .getAsFloat()));
        if (params.has("projectileLifetime")) projectileLifetime = Math.max(
            20,
            Math.min(
                1200,
                params.get("projectileLifetime")
                    .getAsInt()));
        if (params.has("burnDurationTicks")) burnDurationTicks = Math.max(
            20,
            Math.min(
                72000,
                params.get("burnDurationTicks")
                    .getAsInt()));
        if (params.has("burnSwitchDamage")) burnSwitchDamage = Math.max(
            0.0F,
            Math.min(
                1000.0F,
                params.get("burnSwitchDamage")
                    .getAsFloat()));
        if (params.has("burnIncomingDamageMultiplier")) burnIncomingDamageMultiplier = Math.max(
            1.0F,
            Math.min(
                10.0F,
                params.get("burnIncomingDamageMultiplier")
                    .getAsFloat()));
        if (params.has("ignoreSwitchDamageHurtResistance"))
            ignoreSwitchDamageHurtResistance = params.get("ignoreSwitchDamageHurtResistance")
                .getAsBoolean();
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float actualDamage) {
        if (!attacker.worldObj.isRemote && actualDamage > 0) {
            RedSunBurnData.apply(target, attacker, burnDurationTicks);
        }
    }

    @Override
    public ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        long cooldownEnd = player.getEntityData()
            .getLong("DarkGreyRedSunCooldownEnd");
        if (world.getTotalWorldTime() < cooldownEnd) {
            if (!world.isRemote) {
                double secs = (cooldownEnd - world.getTotalWorldTime()) / 20.0;
                player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    EnumChatFormatting.RED + String.format("技能冷却中：%.1f 秒", secs)));
            }
            return itemStack;
        }

        EntityRedSunFireball existing = RedSunFireballManager.findChargingFireball(player);
        if (existing != null) {
            return itemStack;
        }

        player.setItemInUse(itemStack, 72000);

        if (!world.isRemote) {
            EntityRedSunFireball fireball = new EntityRedSunFireball(
                world,
                player,
                maxChargeTicks,
                minFireballSize,
                maxFireballSize,
                minFireballDamage,
                maxFireballDamage,
                projectileSpeed,
                projectileGravity,
                projectileDrag,
                projectileUpwardBoost,
                projectileLifetime,
                burnDurationTicks);
            world.spawnEntityInWorld(fireball);
        }
        return itemStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        // Nothing special to do here, fireball manages itself
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
        if (!world.isRemote) {
            EntityRedSunFireball fireball = RedSunFireballManager.findChargingFireball(player);
            if (fireball != null) {
                fireball.launch(player.getLookVec());
                player.getEntityData()
                    .setLong("DarkGreyRedSunCooldownEnd", world.getTotalWorldTime() + cooldownTicks);
                world.playSoundAtEntity(player, "mob.ghast.fireball", 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GOLD + "技能：烈阳");
        tooltip.add(EnumChatFormatting.GRAY + "所有攻击附加烧伤buff。长按右键可以生成一个1x1x1的火球，");
        tooltip.add(EnumChatFormatting.GRAY + "蓄力时间越长火球越大，最高到5x5x5，火球膨胀速度为每秒20%，即五秒到最大值。");
        tooltip.add(EnumChatFormatting.GRAY + "松开后火球以抛物线的形式向前缓慢飞出。");
        tooltip.add(EnumChatFormatting.GRAY + "伤害随时间增长为（100~1250）。冷却时间为10秒。");
        tooltip.add(EnumChatFormatting.GRAY + "烧伤buff:玩家每次切换物品都会扣除10点生命值，防御力减少20%");

        long cooldownEnd = player.getEntityData()
            .getLong("DarkGreyRedSunCooldownEnd");
        long now = player.worldObj.getTotalWorldTime();
        if (cooldownEnd > now) {
            double secs = (cooldownEnd - now) / 20.0;
            tooltip.add(EnumChatFormatting.RED + String.format("冷却中：%.1f 秒", secs));
        }
    }
}
