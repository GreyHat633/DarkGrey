package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.mark.client.ClientMarkCache;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MarkClearEntityHandler implements IMessageHandler<MarkClearEntityMessage, IMessage> {

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MarkClearEntityMessage message, MessageContext ctx) {
        ClientMarkCache.clear(message.entityId);
        return null;
    }
}
