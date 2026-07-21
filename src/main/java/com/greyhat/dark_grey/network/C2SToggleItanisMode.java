package com.greyhat.dark_grey.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SToggleItanisMode implements IMessage {

    public C2SToggleItanisMode() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<C2SToggleItanisMode, IMessage> {

        @Override
        public IMessage onMessage(C2SToggleItanisMode message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;
            com.greyhat.dark_grey.event.ItanisServerEventHandler.requestToggle(player);
            return null;
        }
    }
}
