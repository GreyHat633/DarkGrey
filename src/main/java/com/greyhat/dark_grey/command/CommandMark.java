package com.greyhat.dark_grey.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.greyhat.dark_grey.mark.MarkContainer;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkRemovalReason;

public class CommandMark {

    public static void processMarkCommand(ICommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED
                        + "Usage: /darkgrey mark <apply|set|remove|clear|list> <target> [markId] [amount] [durationTicks]"));
            return;
        }

        String action = args[1];
        String targetName = args[2];
        EntityLivingBase target = null;

        try {
            target = CommandBase.getPlayer(sender, targetName);
        } catch (PlayerNotFoundException e) {
            // Target is not a player, fallback to sender if it's self test
            if (sender instanceof EntityLivingBase) {
                target = (EntityLivingBase) sender;
            }
        }

        if (target == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Target not found."));
            return;
        }

        if (action.equalsIgnoreCase("clear")) {
            MarkManager.clearAll(target, MarkRemovalReason.COMMAND);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Cleared all marks from target."));
            return;
        }

        if (action.equalsIgnoreCase("list")) {
            MarkContainer container = MarkContainer.get(target);
            if (container == null || container.isEmpty()) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Target has no marks."));
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Marks on target:"));
                for (MarkInstance inst : container.getAllMarks()) {
                    sender.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.AQUA + "- " + inst.getMarkId() + ": " + inst.getStacks() + " stacks"));
                }
            }
            return;
        }

        if (args.length < 4) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Missing markId."));
            return;
        }

        String markId = args[3];

        if (action.equalsIgnoreCase("remove")) {
            MarkManager.remove(target, markId, MarkRemovalReason.COMMAND);
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.GREEN + "Removed mark " + markId + " from target."));
            return;
        }

        if (args.length < 5) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Missing amount."));
            return;
        }

        int amount = 0;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid amount."));
            return;
        }

        int durationTicks = 0;
        if (args.length >= 6) {
            try {
                durationTicks = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Invalid duration."));
                return;
            }
        }

        EntityLivingBase source = sender instanceof EntityLivingBase ? (EntityLivingBase) sender : null;

        if (action.equalsIgnoreCase("apply")) {
            com.greyhat.dark_grey.mark.api.MarkApplyContext.Builder builder = new com.greyhat.dark_grey.mark.api.MarkApplyContext.Builder()
                .source(source)
                .requestedStacks(amount)
                .worldTime(target.worldObj.getTotalWorldTime());
            if (durationTicks > 0) {
                builder.durationTicks(durationTicks);
            }
            MarkManager.apply(target, markId, builder.build());
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "Applied "
                        + amount
                        + " stacks of "
                        + markId
                        + " to target."
                        + (durationTicks > 0 ? (" (" + durationTicks + " ticks)") : "")));
        } else if (action.equalsIgnoreCase("set")) {
            MarkManager.setStacks(target, markId, amount, source);
            if (durationTicks > 0) {
                MarkInstance inst = MarkContainer.get(target)
                    .getMark(markId);
                if (inst != null) {
                    inst.getCustomData()
                        .setBoolean("HasIndependentDuration", true);
                    inst.getCustomData()
                        .setLong("IndependentExpireWorldTime", target.worldObj.getTotalWorldTime() + durationTicks);
                    MarkManager.syncMark(
                        target,
                        inst,
                        com.greyhat.dark_grey.mark.MarkRegistry.get(markId),
                        (byte) 4,
                        0,
                        false);
                }
            }
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "Set "
                        + markId
                        + " to "
                        + amount
                        + " stacks on target."
                        + (durationTicks > 0 ? (" (" + durationTicks + " ticks)") : "")));
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Unknown mark action: " + action));
        }
    }
}
