package com.greyhat.dark_grey.command;

import com.greyhat.dark_grey.api.RPGItemDataManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandRPGReload extends CommandBase {

    @Override
    public String getCommandName() {
        return "rpgreload";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rpgreload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // OP required
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        RPGItemDataManager.getInstance().reload();
        sender.addChatMessage(new ChatComponentText("§aRPG Item data reloaded successfully! Version: " + RPGItemDataManager.getInstance().getDataVersion()));
    }
}