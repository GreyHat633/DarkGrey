package com.greyhat.dark_grey.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.greyhat.dark_grey.combat.AutomaticFireManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MessageStartAutomaticFireHandler implements IMessageHandler<MessageStartAutomaticFire, IMessage> {

    @Override
    public IMessage onMessage(MessageStartAutomaticFire message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player != null && player.isEntityAlive()) {
            AutomaticFireManager.startFire(player, player.getCurrentEquippedItem());
        }
        return null;
    }
}
