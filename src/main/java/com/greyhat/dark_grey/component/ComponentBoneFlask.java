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

    private float directDamage = 12.0F;
    private float lingeringDamage = 2.0F;
    private int fieldDuration = 1200; // 60s
    private float projectileVelocity = 0.75F;
    private float projectileInaccuracy = 1.0F;
    private float projectileGravity = 0.03F;

    @Override
    public String getComponentId() {
        return "碎骨瓶";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("directDamage")) this.directDamage = params.get("directDamage")
            .getAsFloat();
        if (params.has("lingeringDamage")) this.lingeringDamage = params.get("lingeringDamage")
            .getAsFloat();
        if (params.has("fieldDuration")) this.fieldDuration = params.get("fieldDuration")
            .getAsInt();
        if (params.has("projectileVelocity")) this.projectileVelocity = params.get("projectileVelocity")
            .getAsFloat();
        if (params.has("projectileInaccuracy")) this.projectileInaccuracy = params.get("projectileInaccuracy")
            .getAsFloat();
        if (params.has("projectileGravity")) this.projectileGravity = params.get("projectileGravity")
            .getAsFloat();
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player == null || player.isDead || stack.stackSize <= 0) return stack;

        if (!world.isRemote) {
            EntityBoneFlask flask = new EntityBoneFlask(world, player);
            flask.directDamage = this.directDamage;
            flask.lingeringDamage = this.lingeringDamage;
            flask.fieldDuration = this.fieldDuration;

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
