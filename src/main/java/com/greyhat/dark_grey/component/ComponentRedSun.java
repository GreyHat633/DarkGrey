package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.CooldownHelper;
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

    private static final String LEGACY_COOLDOWN_END_TICK_KEY = "DarkGreyRedSunCooldownEnd";
    private static final String COOLDOWN_END_MILLIS_KEY = "DarkGreyRedSunCooldownEndMillis";

    private int maxChargeTicks = 180;
    private float minFireballSize = 1.0F;
    private float maxFireballSize = 12.0F;
    private float minFireballDamage = 100.0F;
    private float maxFireballDamage = 1250.0F;
    private int cooldownTicks = 200;

    private float minProjectileSpeed = 0.5F;
    private float maxProjectileSpeed = 2.0F;

    private float projectileGravity = 0.03F;
    private float projectileDrag = 0.98F;
    private float projectileUpwardBoost = 0.12F;
    private int projectileLifetime = 200;
    private int burnDurationTicks = 200;
    private float burnSwitchDamage = 10.0F;
    private float burnIncomingDamageMultiplier = 1.20F;
    private boolean ignoreSwitchDamageHurtResistance = true;

    private float volumeShrinkRate = 0.05F;
    private float maxExplosionRadius = 25.0F;

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

        if (params.has("minProjectileSpeed")) minProjectileSpeed = Math.max(
            0.1F,
            Math.min(
                5.0F,
                params.get("minProjectileSpeed")
                    .getAsFloat()));
        if (params.has("maxProjectileSpeed")) maxProjectileSpeed = Math.max(
            minProjectileSpeed,
            Math.min(
                10.0F,
                params.get("maxProjectileSpeed")
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

        if (params.has("volumeShrinkRate")) volumeShrinkRate = Math.max(
            0.001F,
            Math.min(
                1.0F,
                params.get("volumeShrinkRate")
                    .getAsFloat()));
        if (params.has("maxExplosionRadius")) maxExplosionRadius = Math.max(
            1.0F,
            Math.min(
                100.0F,
                params.get("maxExplosionRadius")
                    .getAsFloat()));
    }

    @Override
    public void onHit(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target, float actualDamage) {
        if (!attacker.worldObj.isRemote && actualDamage > 0) {
            RedSunBurnData.apply(
                target,
                attacker,
                burnDurationTicks,
                burnSwitchDamage,
                burnIncomingDamageMultiplier,
                ignoreSwitchDamageHurtResistance);
        }
    }

    private long getRemainingCooldownMillis(EntityPlayer player) {
        return CooldownHelper.getRemainingMillis(
            player.getEntityData(),
            COOLDOWN_END_MILLIS_KEY,
            CooldownHelper.ticksToMillis(cooldownTicks),
            LEGACY_COOLDOWN_END_TICK_KEY);
    }

    private void startCooldown(EntityPlayer player) {
        CooldownHelper.start(
            player.getEntityData(),
            COOLDOWN_END_MILLIS_KEY,
            CooldownHelper.ticksToMillis(cooldownTicks),
            LEGACY_COOLDOWN_END_TICK_KEY);
    }

    @Override
    public ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            long remainingCooldownMillis = getRemainingCooldownMillis(player);
            if (remainingCooldownMillis > 0L) {
                double secs = remainingCooldownMillis / 1000.0D;
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        EnumChatFormatting.RED + String.format("技能冷却中：%.1f 秒", secs)));
                return itemStack;
            }
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
                minProjectileSpeed,
                maxProjectileSpeed,
                projectileGravity,
                projectileDrag,
                projectileUpwardBoost,
                projectileLifetime,
                burnDurationTicks,
                burnSwitchDamage,
                burnIncomingDamageMultiplier,
                ignoreSwitchDamageHurtResistance,
                volumeShrinkRate,
                maxExplosionRadius);
            world.spawnEntityInWorld(fireball);
        }
        return itemStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {}

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
        if (!world.isRemote) {
            EntityRedSunFireball fireball = RedSunFireballManager.findChargingFireball(player);
            if (fireball != null) {
                fireball.launch(player.getLookVec());
                startCooldown(player);
                world.playSoundAtEntity(player, "mob.ghast.fireball", 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GOLD + "范围伤害：" + (int) minFireballDamage + " ~ " + (int) maxFireballDamage);
        tooltip.add(EnumChatFormatting.AQUA + "所有攻击附带【烧伤】异常状态");
        tooltip.add("");
        tooltip.add(EnumChatFormatting.YELLOW + "长按右键：蓄力烈阳");
        tooltip.add(EnumChatFormatting.YELLOW + "松开右键：发射烈阳");
        tooltip.add("");
        tooltip.add(EnumChatFormatting.GREEN + "蓄力烈阳：");
        tooltip.add(EnumChatFormatting.GRAY + String.format("  按住右键蓄力，最多 %.1f 秒", maxChargeTicks / 20.0F));
        tooltip.add(
            EnumChatFormatting.GRAY + String.format(
                "  火球体积从 %.0fx%.0fx%.0f 逐渐膨胀到最大 %.0fx%.0fx%.0f",
                minFireballSize,
                minFireballSize,
                minFireballSize,
                maxFireballSize,
                maxFireballSize,
                maxFireballSize));
        tooltip.add("");
        tooltip.add(EnumChatFormatting.LIGHT_PURPLE + "发射烈阳：");
        tooltip.add(EnumChatFormatting.GRAY + "  松开右键将火球抛出，触地后将沿原方向无情碾压");
        tooltip.add(EnumChatFormatting.GRAY + "  发射初速度与火球体积呈反比（越小越快）");
        tooltip.add(EnumChatFormatting.GRAY + "  火球无视实体障碍物，对接触的实体造成碾压伤害并击飞");
        tooltip.add(EnumChatFormatting.GRAY + "  直到撞击墙壁或动能耗尽停滞时才会发生大规模爆炸");
        tooltip.add(EnumChatFormatting.GRAY + String.format("  技能冷却时间：%.1f 秒", cooldownTicks / 20.0F));
        tooltip.add("");
        tooltip.add(EnumChatFormatting.RED + "【烧伤】异常：");
        tooltip.add(EnumChatFormatting.GRAY + "  玩家每次切换物品时扣除 10 点生命值，并减少 20% 防御力");

        long remainingCooldownMillis = getRemainingCooldownMillis(player);
        if (remainingCooldownMillis > 0L) {
            double secs = remainingCooldownMillis / 1000.0D;
            tooltip.add(EnumChatFormatting.RED + String.format("冷却中：%.1f 秒", secs));
        }
    }
}
