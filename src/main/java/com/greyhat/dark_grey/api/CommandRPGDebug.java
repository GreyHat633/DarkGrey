package com.greyhat.dark_grey.api;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandRPGDebug extends CommandBase {

    @Override
    public String getCommandName() {
        return "rpg_debug";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rpg_debug";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            RPGItemDataManager.getInstance()
                .reload();
            sender.addChatMessage(new ChatComponentText("§aForce reloaded RPG items from JSON!"));
        }

        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig("vampire_blade");
        if (config == null) {
            sender.addChatMessage(new ChatComponentText("§cvampire_blade config is null!"));
            return;
        }
        sender.addChatMessage(new ChatComponentText("§eVampire Blade Config:"));
        sender.addChatMessage(new ChatComponentText("§e- Damage: " + config.damage));
        sender.addChatMessage(new ChatComponentText("§e- Durability: " + config.durability));
        sender.addChatMessage(new ChatComponentText("§e- Enchants: " + config.enchantments));
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
