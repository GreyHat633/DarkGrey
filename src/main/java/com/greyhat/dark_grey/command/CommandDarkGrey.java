package com.greyhat.dark_grey.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.greyhat.dark_grey.combat.PolarityPhysicsManager;
import com.greyhat.dark_grey.component.ComponentSuspendedClockhand;
import com.greyhat.dark_grey.item.ItemRPGWeapon;

public class CommandDarkGrey extends CommandBase {

    @Override
    public String getCommandName() {
        return "darkgrey";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/darkgrey fillsoul | mark | polaritydebug <on|off|status>";
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
        } else if (args.length > 0 && args[0].equalsIgnoreCase("mark")) {
            com.greyhat.dark_grey.command.CommandMark.processMarkCommand(sender, args);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("polaritydebug")) {
            processPolarityDebug(sender, args);
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "未知子指令！" + getCommandUsage(sender)));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP required
    }

    @Override
    public java.util.List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "fillsoul", "mark", "polaritydebug");
        } else if (args.length > 1 && args[0].equalsIgnoreCase("mark")) {
            return com.greyhat.dark_grey.command.CommandMark.addTabCompletionOptions(sender, args);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("polaritydebug")) {
            return getListOfStringsMatchingLastWord(args, "on", "off", "status");
        }
        return null;
    }

    private void processPolarityDebug(ICommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.YELLOW + "极性调试日志：" + (PolarityPhysicsManager.isDebugEnabled() ? "已开启" : "已关闭")));
            return;
        }
        if (args[1].equalsIgnoreCase("on")) {
            PolarityPhysicsManager.setDebugEnabled(true);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "已开启极性逐Tick调试日志。"));
        } else if (args[1].equalsIgnoreCase("off")) {
            PolarityPhysicsManager.setDebugEnabled(false);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "已关闭极性逐Tick调试日志。"));
        } else {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "用法：/darkgrey polaritydebug <on|off|status>"));
        }
    }
}
