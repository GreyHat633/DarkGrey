package com.greyhat.dark_grey.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.greyhat.dark_grey.combat.AutomaticFireManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MessageStopAutomaticFireHandler implements IMessageHandler<MessageStopAutomaticFire, IMessage> {

    @Override
    public IMessage onMessage(MessageStopAutomaticFire message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player != null) {
            AutomaticFireManager.stopFire(player);
        }
        return null;
    }
}
