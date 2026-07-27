package com.greyhat.dark_grey.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.greyhat.dark_grey.event.ServerLeftClickHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ItanisModeSwitchHandler implements IMessageHandler<ItanisModeSwitchMessage, IMessage> {

    @Override
    public IMessage onMessage(ItanisModeSwitchMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null || !player.isEntityAlive()) {
            return null;
        }

        ServerLeftClickHandler.request(player);
        return null;
    }
}
