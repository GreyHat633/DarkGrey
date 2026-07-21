package com.greyhat.dark_grey.component;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnBowShoot;
import com.greyhat.dark_grey.api.capability.IOnBowUsingTick;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.entity.EntityItanisArrow;
import com.greyhat.dark_grey.entity.ItanisArrowType;
import com.greyhat.dark_grey.entity.ItanisTargetHelper;

public class ComponentItanis
    implements IRPGComponent, IOnBowShoot, IOnBowUsingTick, IOnRightClick, IOnHeldTick, IHasTooltip {

    private int normalDrawTicks = 20;
    private double bonusChance1 = 0.85;
    private double bonusMultiplier1 = 0.35;
    private double bonusChance2 = 0.65;
    private double bonusMultiplier2 = 0.60;
    private double bonusChance3 = 0.25;
    private double bonusMultiplier3 = 0.90;

    private int chargeIntervalTicks = 20;
    private double chargeAutoArrowDamageMultiplier = 0.40;
    private int maxChargeTicks = 200;
    private double fullChargeDamage = 5000.0;

    private double trackingRange = 32.0;
    private double trackingTurnSpeed = 0.45;
    private double trackingAcceleration = 0.15;

    private Random rand = new Random();

    @Override
    public String getComponentId() {
        return "伊塔尼斯";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("normalDrawTicks")) normalDrawTicks = clamp(
            params.get("normalDrawTicks")
                .getAsInt(),
            1,
            1200);
        if (params.has("bonusChance1")) bonusChance1 = clamp(
            params.get("bonusChance1")
                .getAsDouble(),
            0.0,
            1.0);
        if (params.has("bonusMultiplier1")) bonusMultiplier1 = clamp(
            params.get("bonusMultiplier1")
                .getAsDouble(),
            0.0,
            1000.0);
        if (params.has("bonusChance2")) bonusChance2 = clamp(
            params.get("bonusChance2")
                .getAsDouble(),
            0.0,
            1.0);
        if (params.has("bonusMultiplier2")) bonusMultiplier2 = clamp(
            params.get("bonusMultiplier2")
                .getAsDouble(),
            0.0,
            1000.0);
        if (params.has("bonusChance3")) bonusChance3 = clamp(
            params.get("bonusChance3")
                .getAsDouble(),
            0.0,
            1.0);
        if (params.has("bonusMultiplier3")) bonusMultiplier3 = clamp(
            params.get("bonusMultiplier3")
                .getAsDouble(),
            0.0,
            1000.0);

        if (params.has("chargeIntervalTicks")) chargeIntervalTicks = clamp(
            params.get("chargeIntervalTicks")
                .getAsInt(),
            1,
            1200);
        if (params.has("chargeAutoArrowDamageMultiplier")) chargeAutoArrowDamageMultiplier = clamp(
            params.get("chargeAutoArrowDamageMultiplier")
                .getAsDouble(),
            0.0,
            1000.0);
        if (params.has("maxChargeTicks")) maxChargeTicks = clamp(
            params.get("maxChargeTicks")
                .getAsInt(),
            20,
            1200);
        if (params.has("fullChargeDamage")) fullChargeDamage = clamp(
            params.get("fullChargeDamage")
                .getAsDouble(),
            0.0,
            1000000.0);

        if (params.has("trackingRange")) trackingRange = clamp(
            params.get("trackingRange")
                .getAsDouble(),
            1.0,
            128.0);
        if (params.has("trackingTurnSpeed")) trackingTurnSpeed = clamp(
            params.get("trackingTurnSpeed")
                .getAsDouble(),
            0.01,
            1.0);
        if (params.has("trackingAcceleration")) trackingAcceleration = clamp(
            params.get("trackingAcceleration")
                .getAsDouble(),
            0.0,
            1.0);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    public int getNormalDrawTicks() {
        return normalDrawTicks;
    }

    public int getMaxChargeTicks() {
        return maxChargeTicks;
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, World world, EntityPlayer player) {
        // Player starts using the bow, mode logic is handled in ItanisNBT externally by Left Click
        return stack;
    }

    @Override
    public void onBowUsingTick(ItemStack stack, World world, EntityPlayer player, int count) {
        if (world.isRemote) return;

        ItanisMode mode = ItanisNBT.getMode(stack);
        if (mode == ItanisMode.CHARGE) {
            int chargeDuration = stack.getItem()
                .getMaxItemUseDuration(stack) - count;

            // Resistance II buff
            player.addPotionEffect(new PotionEffect(Potion.resistance.id, 10, 1, true));

            // Stop generating after max charge
            if (chargeDuration <= maxChargeTicks && chargeDuration % chargeIntervalTicks == 0 && chargeDuration > 0) {
                // Generate auto tracking arrow
                double arrowDmg = getWeaponDamage(stack) * chargeAutoArrowDamageMultiplier;
                EntityItanisArrow arrow = new EntityItanisArrow(
                    world,
                    player,
                    null,
                    ItanisArrowType.CHARGE_ORBIT,
                    arrowDmg,
                    trackingTurnSpeed,
                    trackingAcceleration,
                    trackingRange);

                arrow.setPosition(
                    player.posX + (rand.nextDouble() - 0.5) * 2,
                    player.posY + player.getEyeHeight() + 0.5 + rand.nextDouble(),
                    player.posZ + (rand.nextDouble() - 0.5) * 2);

                world.spawnEntityInWorld(arrow);
                world.playSoundAtEntity(player, "random.bow", 0.5F, 1.4F / (rand.nextFloat() * 0.4F + 0.8F));
            }

            if (chargeDuration == maxChargeTicks) {
                fireAllPhalanxArrows(world, player);

                EntityItanisArrow pierceArrow = new EntityItanisArrow(
                    world,
                    player,
                    3.0F,
                    ItanisArrowType.FULL_CHARGE,
                    fullChargeDamage,
                    trackingTurnSpeed,
                    trackingAcceleration,
                    trackingRange);
                pierceArrow.setTrackingTarget(ItanisTargetHelper.findTargetInSight(player, trackingRange, 60.0), 3.0D);
                pierceArrow.setIsCritical(true);
                world.spawnEntityInWorld(pierceArrow);
                world.playSoundAtEntity(player, "mob.wither.shoot", 1.0F, 1.0F / (rand.nextFloat() * 0.4F + 1.2F));

                player.clearItemInUse();
            }
        }
    }

    @Override
    public boolean onBowShoot(ItemStack stack, World world, EntityPlayer player, int charge) {
        if (world.isRemote) return true;

        ItanisMode mode = ItanisNBT.getMode(stack);
        double baseDamage = getWeaponDamage(stack);

        if (mode == ItanisMode.RAPID) {
            float f = (float) charge / (float) normalDrawTicks;
            f = (f * f + f * 2.0F) / 3.0F;
            if (f < 0.1D) return false;
            if (f > 1.0F) f = 1.0F;

            net.minecraft.entity.EntityLivingBase target = ItanisTargetHelper
                .findTargetInSight(player, trackingRange, 60.0);

            EntityItanisArrow mainArrow = new EntityItanisArrow(
                world,
                player,
                target,
                ItanisArrowType.NORMAL,
                baseDamage * f,
                trackingTurnSpeed,
                trackingAcceleration,
                trackingRange);
            mainArrow.setIsCritical(f == 1.0F);
            world.spawnEntityInWorld(mainArrow);

            // Bonus arrows
            trySpawnBonus(world, player, target, f, baseDamage, bonusChance1, bonusMultiplier1);
            trySpawnBonus(world, player, target, f, baseDamage, bonusChance2, bonusMultiplier2);
            trySpawnBonus(world, player, target, f, baseDamage, bonusChance3, bonusMultiplier3);

            world.playSoundAtEntity(player, "random.bow", 1.0F, 1.0F / (rand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

        } else if (mode == ItanisMode.CHARGE) {
            fireAllPhalanxArrows(world, player);
        }

        return true;
    }

    private void fireAllPhalanxArrows(World world, EntityPlayer player) {
        @SuppressWarnings("unchecked")
        List<EntityItanisArrow> arrows = world
            .getEntitiesWithinAABB(EntityItanisArrow.class, player.boundingBox.expand(4, 4, 4));
        for (EntityItanisArrow arrow : arrows) {
            if (arrow.shootingEntity == player && arrow.getArrowType() == ItanisArrowType.CHARGE_ORBIT) {
                arrow.setArrowType(ItanisArrowType.CHARGE_AUTO);
                // The arrow itself will find a target in its onUpdate
            }
        }
    }

    private void trySpawnBonus(World world, EntityPlayer player, net.minecraft.entity.EntityLivingBase target,
        float force, double baseDmg, double chance, double mult) {
        if (rand.nextDouble() < chance) {
            EntityItanisArrow bonus = new EntityItanisArrow(
                world,
                player,
                target,
                ItanisArrowType.BONUS,
                baseDmg * force * mult,
                trackingTurnSpeed,
                trackingAcceleration,
                trackingRange);
            bonus.setIsCritical(force == 1.0F);
            // offset slightly
            bonus.posX += (rand.nextDouble() - 0.5);
            bonus.posY += (rand.nextDouble() - 0.5);
            bonus.posZ += (rand.nextDouble() - 0.5);
            world.spawnEntityInWorld(bonus);
        }
    }

    @Override
    public void onHeldTick(ItemStack stack, World world, EntityPlayer player) {
        // Not strictly needed on server if we only want mode switch effects, but can be used for particles
    }

    private double getWeaponDamage(ItemStack stack) {
        if (stack.getItem() instanceof IRPGItemContainer) {
            com.greyhat.dark_grey.api.RPGItemDataManager.ItemConfig config = com.greyhat.dark_grey.api.RPGItemDataManager
                .getInstance()
                .getConfig(((IRPGItemContainer) stack.getItem()).getRpgItemId());
            if (config != null) return config.damage;
        }
        return 300.0;
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        ItanisMode mode = ItanisNBT.getMode(stack);
        tooltip.add("");
        tooltip.add(
            EnumChatFormatting.GOLD + "✦ " + EnumChatFormatting.YELLOW + "传说之弓机制 " + EnumChatFormatting.GOLD + "✦");

        if (mode == ItanisMode.RAPID) {
            tooltip.add(EnumChatFormatting.AQUA + "当前模式: [速射]");
            tooltip.add(EnumChatFormatting.GRAY + " - 左键点击切换模式");
            tooltip.add(EnumChatFormatting.GRAY + " - 射出追踪箭，并有概率附加额外箭矢");
        } else {
            tooltip.add(EnumChatFormatting.LIGHT_PURPLE + "当前模式: [蓄能]");
            tooltip.add(EnumChatFormatting.GRAY + " - 左键点击切换模式");
            tooltip.add(EnumChatFormatting.GRAY + " - 蓄力期间自动生成追踪箭");
            tooltip.add(EnumChatFormatting.GRAY + " - 满蓄力(10秒)射出高伤贯穿箭");
            tooltip.add(EnumChatFormatting.GRAY + " - 蓄力期间获得抗性提升II");
        }
    }
}
