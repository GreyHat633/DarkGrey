package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.item.ItemRPGWeapon;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;
import com.greyhat.dark_grey.mark.type.NegativePolarityMarkType;
import com.greyhat.dark_grey.mark.type.PositivePolarityMarkType;

public class ComponentPolarity implements IRPGComponent, IOnHit, IOnRightClick, IHasTooltip {

    public static final int MODE_POSITIVE = 1;
    public static final int MODE_NEGATIVE = 2;

    public int polarityDurationTicks = 1200;
    public double magneticRange = 20.0;
    public double maxPairAcceleration = 0.20;
    public double maxNetMagneticAcceleration = 0.40;
    public double forceExponent = 2.0;
    public double collisionSpeedThreshold = 0.90;
    public double collisionTolerance = 0.20;
    public double collisionRearmExtraDistance = 0.75;
    public int pairExplosionDebounceTicks = 5;
    public double secondaryExplosionRadius = 8.0;
    public double explosionKnockbackStrength = 1.0;

    @Override
    public String getComponentId() {
        return "极性";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("polarityDurationTicks")) {
            polarityDurationTicks = Math.max(
                0,
                Math.min(
                    72000,
                    params.get("polarityDurationTicks")
                        .getAsInt()));
        }
        if (params.has("magneticRange")) {
            magneticRange = clampFinite(
                params.get("magneticRange")
                    .getAsDouble(),
                0.01D,
                128.0D,
                20.0D);
        }
        if (params.has("maxPairAcceleration")) {
            maxPairAcceleration = clampFinite(
                params.get("maxPairAcceleration")
                    .getAsDouble(),
                0.0D,
                4.0D,
                0.20D);
        }
        if (params.has("maxNetMagneticAcceleration")) maxNetMagneticAcceleration = clampFinite(
            params.get("maxNetMagneticAcceleration")
                .getAsDouble(),
            0.0D,
            8.0D,
            0.40D);
        // The polarity design requires an exact square falloff. Keep the legacy
        // JSON key readable, but never allow it to turn the force linear.
        forceExponent = 2.0D;
        if (params.has("collisionSpeedThreshold")) {
            collisionSpeedThreshold = clampFinite(
                params.get("collisionSpeedThreshold")
                    .getAsDouble(),
                0.0D,
                20.0D,
                0.90D);
        }
        if (params.has("collisionTolerance")) {
            collisionTolerance = clampFinite(
                params.get("collisionTolerance")
                    .getAsDouble(),
                0.0D,
                4.0D,
                0.20D);
        }
        if (params.has("collisionRearmExtraDistance")) collisionRearmExtraDistance = clampFinite(
            params.get("collisionRearmExtraDistance")
                .getAsDouble(),
            0.0D,
            20.0D,
            0.75D);
        if (params.has("pairExplosionDebounceTicks")) pairExplosionDebounceTicks = Math.max(
            0,
            Math.min(
                1200,
                params.get("pairExplosionDebounceTicks")
                    .getAsInt()));
        if (params.has("secondaryExplosionRadius")) {
            secondaryExplosionRadius = clampFinite(
                params.get("secondaryExplosionRadius")
                    .getAsDouble(),
                0.0D,
                64.0D,
                8.0D);
        }
        if (params.has("explosionKnockbackStrength")) explosionKnockbackStrength = clampFinite(
            params.get("explosionKnockbackStrength")
                .getAsDouble(),
            0.0D,
            8.0D,
            1.0D);
    }

    private static double clampFinite(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    private int getMode(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("DarkGreyPolarityMode")) {
            return stack.getTagCompound()
                .getInteger("DarkGreyPolarityMode");
        }
        return MODE_POSITIVE;
    }

    private void setMode(ItemStack stack, int mode) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setInteger("DarkGreyPolarityMode", mode);
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            if (!player.isSneaking()) {
                int mode = getMode(stack);
                mode = (mode == MODE_POSITIVE) ? MODE_NEGATIVE : MODE_POSITIVE;
                setMode(stack, mode);
                world.playSoundAtEntity(player, "random.click", 0.8F, 1.0F);
                if (mode == MODE_POSITIVE) {
                    player.addChatMessage(new ChatComponentText("\u00a7c当前极性：正极"));
                } else {
                    player.addChatMessage(new ChatComponentText("\u00a79当前极性：负极"));
                }
            } else {
                int mode = getMode(stack);
                String markId = (mode == MODE_POSITIVE) ? PositivePolarityMarkType.ID : NegativePolarityMarkType.ID;
                MarkApplyContext context = new MarkApplyContext.Builder().source(player)
                    .sourceUuid(player.getUniqueID())
                    .worldTime(player.worldObj.getTotalWorldTime())
                    .stableDurationTicks(polarityDurationTicks)
                    .build();
                MarkManager.apply(player, markId, context);
            }
        }

        if (stack.getItem() instanceof ItemRPGWeapon) {
            player.clearItemInUse();
        }
        return stack;
    }

    @Override
    public void onHit(ItemStack stack, EntityLivingBase attacker, EntityLivingBase target, float multiplier) {
        if (!attacker.worldObj.isRemote) {
            int mode = getMode(stack);
            String markId = (mode == MODE_POSITIVE) ? PositivePolarityMarkType.ID : NegativePolarityMarkType.ID;

            MarkApplyContext context = new MarkApplyContext.Builder().source(attacker)
                .sourceUuid(attacker.getUniqueID())
                .worldTime(attacker.worldObj.getTotalWorldTime())
                .stableDurationTicks(polarityDurationTicks)
                .build();

            MarkManager.apply(target, markId, context);
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add("\u00a7b操纵正负极性，让敌人在磁力中碰撞毁灭");
        tooltip.add("");
        tooltip.add("\u00a7e右键：切换正极 / 负极");
        tooltip.add("\u00a7eShift + 右键：为自身附加当前极性");
        tooltip.add("\u00a7e攻击：为目标附加当前极性");
        tooltip.add("");
        tooltip.add("\u00a7a极性：");
        tooltip.add("\u00a77  给附有极性的实体附加相反极性可将其抵消清空");
        tooltip.add("\u00a77  正极与负极持续 " + (polarityDurationTicks / 20) + " 秒");
        tooltip.add("\u00a77  相反极性在 " + (int) magneticRange + " 格内互相吸引");
        tooltip.add("\u00a77  相同极性在 " + (int) magneticRange + " 格内互相排斥");
        tooltip.add("\u00a77  距离越近，磁力越强");
        tooltip.add("\u00a77  磁力无视实体与方块遮挡");
        tooltip.add("");
        tooltip.add("\u00a7d极性碰撞：");
        tooltip.add("\u00a77  异极以足够速度相撞时触发爆炸");
        tooltip.add("\u00a77  碰撞双方将基于彼此的护甲值承受无视普通护甲的派生伤害");
        tooltip.add("\u00a77  随后产生半径 " + (int) secondaryExplosionRadius + " 的次生爆炸");
        tooltip.add("\u00a77  若施加者本人参与碰撞，");
        tooltip.add("\u00a77  次生爆炸伤害将提高至 400%");
        tooltip.add("");

        int mode = getMode(stack);
        if (mode == MODE_POSITIVE) {
            tooltip.add("\u00a7f当前模式：\u00a7c正极");
        } else {
            tooltip.add("\u00a7f当前模式：\u00a79负极");
        }
    }
}
