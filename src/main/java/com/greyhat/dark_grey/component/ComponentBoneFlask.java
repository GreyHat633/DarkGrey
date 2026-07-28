package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.entity.EntityBoneFlask;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ComponentBoneFlask implements IRPGComponent, IOnRightClick, IHasTooltip {

    private float directDamage = 8.0F;
    private float lingeringDamage = 2.0F;
    private int fieldDuration = 1200; // 60s
    private int fractureStableDurationTicks = 100;
    private float projectileVelocity = 0.75F;
    private float projectileInaccuracy = 1.0F;
    private float projectileGravity = 0.03F;

    @Override
    public String getComponentId() {
        return "碎骨瓶";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("directDamage")) {
            this.directDamage = clampFinite(
                params.get("directDamage")
                    .getAsFloat(),
                0.0F,
                1000000.0F,
                8.0F);
        }
        if (params.has("lingeringDamage")) {
            this.lingeringDamage = clampFinite(
                params.get("lingeringDamage")
                    .getAsFloat(),
                0.0F,
                1000000.0F,
                2.0F);
        }
        if (params.has("fieldDuration")) {
            this.fieldDuration = Math.max(
                1,
                Math.min(
                    72000,
                    params.get("fieldDuration")
                        .getAsInt()));
        }
        if (params.has("fractureStableDurationTicks")) {
            this.fractureStableDurationTicks = Math.max(
                1,
                Math.min(
                    720000,
                    params.get("fractureStableDurationTicks")
                        .getAsInt()));
        }
        if (params.has("projectileVelocity")) {
            this.projectileVelocity = clampFinite(
                params.get("projectileVelocity")
                    .getAsFloat(),
                0.01F,
                10.0F,
                0.75F);
        }
        if (params.has("projectileInaccuracy")) {
            this.projectileInaccuracy = clampFinite(
                params.get("projectileInaccuracy")
                    .getAsFloat(),
                0.0F,
                180.0F,
                1.0F);
        }
        if (params.has("projectileGravity")) {
            this.projectileGravity = clampFinite(
                params.get("projectileGravity")
                    .getAsFloat(),
                0.0F,
                1.0F,
                0.03F);
        }
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player == null || player.isDead || stack.stackSize <= 0) return stack;

        if (!world.isRemote) {
            EntityBoneFlask flask = new EntityBoneFlask(world, player);
            flask.directDamage = this.directDamage;
            flask.lingeringDamage = this.lingeringDamage;
            flask.fieldDuration = this.fieldDuration;
            flask.fractureStableDurationTicks = this.fractureStableDurationTicks;
            flask.projectileGravity = this.projectileGravity;

            flask.setThrowableHeading(
                player.getLookVec().xCoord,
                player.getLookVec().yCoord,
                player.getLookVec().zCoord,
                this.projectileVelocity,
                this.projectileInaccuracy);

            boolean success = world.spawnEntityInWorld(flask);
            if (success) {
                world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
                if (!player.capabilities.isCreativeMode) {
                    stack.stackSize--;
                }
            } else {
                player.addChatComponentMessage(
                    new net.minecraft.util.ChatComponentTranslation("message.bone_flask.throw_failed"));
            }
        }
        return stack;
    }

    private static float clampFinite(float value, float minimum, float maximum, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean showAdvanced) {
        tooltipLines.add(net.minecraft.util.EnumChatFormatting.YELLOW + "右键：向前投掷一枚碎骨瓶");
        tooltipLines.add("");
        tooltipLines.add(net.minecraft.util.EnumChatFormatting.GREEN + "碎骨效果：");
        tooltipLines.add(
            net.minecraft.util.EnumChatFormatting.GRAY + "  掷出后，命中造成" + (int) this.directDamage + "点伤害并留下3x3的骨刺场。");
        tooltipLines.add(
            net.minecraft.util.EnumChatFormatting.GRAY + "  踩中骨刺的敌人受到"
                + (int) this.lingeringDamage
                + "点伤害并叠加1层骨折印记，随后骨刺消失。");
    }
}
