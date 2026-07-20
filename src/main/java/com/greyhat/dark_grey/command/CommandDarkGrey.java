package com.greyhat.dark_grey.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.greyhat.dark_grey.component.ComponentSuspendedClockhand;
import com.greyhat.dark_grey.item.ItemRPGWeapon;

public class CommandDarkGrey extends CommandBase {

    @Override
    public String getCommandName() {
        return "darkgrey";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/darkgrey fillsoul - 充满手中倒悬时针的灵魂值";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("fillsoul")) {
            if (sender instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) sender;
                ItemStack heldItem = player.getCurrentEquippedItem();

                if (heldItem != null && heldItem.getItem() instanceof ItemRPGWeapon) {
                    if (!heldItem.hasTagCompound()) {
                        heldItem.setTagCompound(new NBTTagCompound());
                    }
                    // Directly set the soul value
                    heldItem.getTagCompound()
                        .setInteger("SoulValue", ComponentSuspendedClockhand.MAX_SOUL_VALUE);
                    player.addChatComponentMessage(
                        new ChatComponentText(
                            EnumChatFormatting.GREEN + "成功充满灵魂值！当前灵魂值: " + ComponentSuspendedClockhand.MAX_SOUL_VALUE));
                } else {
                    player.addChatComponentMessage(
                        new ChatComponentText(EnumChatFormatting.RED + "你必须手持倒悬时针或其他支持灵魂值的RPG武器！"));
                }
            } // Close if (sender instanceof EntityPlayer)
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "未知子指令！" + getCommandUsage(sender)));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP required
    }
}
