package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.entity.EntityCorruptionBomb;
import com.greyhat.dark_grey.mark.MarkRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ComponentCorruptionBomb implements IRPGComponent, IOnRightClick, IHasTooltip {

    private String markId = "poison";
    private int markStacks = 3;
    private int markStableDurationTicks = 200;
    private float areaWidth = 5.0F;
    private float areaHeight = 5.0F;
    private String areaShape = "square";
    private float projectileVelocity = 0.75F;
    private float projectileInaccuracy = 1.0F;
    private float projectileGravity = 0.03F;
    private int projectileLifetime = 200;
    private boolean respectWalls = false;
    private boolean affectThrower = false;
    private boolean consumeInCreative = false;

    @Override
    public String getComponentId() {
        return "腐败瓶";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("markId")) this.markId = params.get("markId")
            .getAsString();
        if (params.has("markStacks")) this.markStacks = Math.max(
            1,
            Math.min(
                100000,
                params.get("markStacks")
                    .getAsInt()));
        if (params.has("markStableDurationTicks")) this.markStableDurationTicks = Math.max(
            1,
            Math.min(
                720000,
                params.get("markStableDurationTicks")
                    .getAsInt()));
        if (params.has("areaWidth")) this.areaWidth = Math.max(
            1.0F,
            Math.min(
                64.0F,
                params.get("areaWidth")
                    .getAsFloat()));
        if (params.has("areaHeight")) this.areaHeight = Math.max(
            1.0F,
            Math.min(
                64.0F,
                params.get("areaHeight")
                    .getAsFloat()));
        if (params.has("areaShape")) {
            String shape = params.get("areaShape")
                .getAsString();
            if ("square".equals(shape) || "circle".equals(shape)) {
                this.areaShape = shape;
            }
        }
        if (params.has("projectileVelocity")) this.projectileVelocity = Math.max(
            0.1F,
            Math.min(
                5.0F,
                params.get("projectileVelocity")
                    .getAsFloat()));
        if (params.has("projectileInaccuracy")) this.projectileInaccuracy = Math.max(
            0.0F,
            Math.min(
                20.0F,
                params.get("projectileInaccuracy")
                    .getAsFloat()));
        if (params.has("projectileGravity")) this.projectileGravity = Math.max(
            0.0F,
            Math.min(
                1.0F,
                params.get("projectileGravity")
                    .getAsFloat()));
        if (params.has("projectileLifetime")) this.projectileLifetime = Math.max(
            20,
            Math.min(
                1200,
                params.get("projectileLifetime")
                    .getAsInt()));
        if (params.has("respectWalls")) this.respectWalls = params.get("respectWalls")
            .getAsBoolean();
        if (params.has("affectThrower")) this.affectThrower = params.get("affectThrower")
            .getAsBoolean();
        if (params.has("consumeInCreative")) this.consumeInCreative = params.get("consumeInCreative")
            .getAsBoolean();
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player == null || player.isDead || stack.stackSize <= 0) return stack;
        if (MarkRegistry.get(this.markId) == null) {
            if (!world.isRemote) {
                player.addChatComponentMessage(
                    new net.minecraft.util.ChatComponentTranslation(
                        "message.corruption_bomb.mark_unavailable",
                        this.markId));
            }
            return stack;
        }

        if (!world.isRemote) {
            EntityCorruptionBomb bomb = new EntityCorruptionBomb(world, player);
            bomb.markId = this.markId;
            bomb.markStacks = this.markStacks;
            bomb.markStableDurationTicks = this.markStableDurationTicks;
            bomb.areaWidth = this.areaWidth;
            bomb.areaHeight = this.areaHeight;
            bomb.areaShape = this.areaShape;
            bomb.projectileGravity = this.projectileGravity;
            bomb.projectileLifetime = this.projectileLifetime;
            bomb.respectWalls = this.respectWalls;
            bomb.affectThrower = this.affectThrower;

            bomb.setThrowableHeading(
                player.getLookVec().xCoord,
                player.getLookVec().yCoord,
                player.getLookVec().zCoord,
                this.projectileVelocity,
                this.projectileInaccuracy);

            boolean success = world.spawnEntityInWorld(bomb);
            if (success) {
                world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
                if (!player.capabilities.isCreativeMode || this.consumeInCreative) {
                    stack.stackSize--;
                }
            } else {
                player.addChatComponentMessage(
                    new net.minecraft.util.ChatComponentTranslation("message.corruption_bomb.throw_failed"));
            }
        }

        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean showAdvanced) {
        tooltipLines.add(net.minecraft.util.EnumChatFormatting.YELLOW + "右键：向前投掷一枚腐败瓶");
        tooltipLines.add("");
        tooltipLines.add(net.minecraft.util.EnumChatFormatting.GREEN + "炸弹效果：");

        String markName = this.markId.equals("poison") ? "剧毒" : this.markId;
        tooltipLines.add(
            net.minecraft.util.EnumChatFormatting.GRAY + "  掷出后，范围内的所有实体获得" + this.markStacks + "层" + markName + "印记。");
    }
}
