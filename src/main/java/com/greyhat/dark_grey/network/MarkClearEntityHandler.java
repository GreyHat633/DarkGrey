package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MarkClearEntityHandler implements IMessageHandler<MarkClearEntityMessage, IMessage> {

    @Override
    public IMessage onMessage(MarkClearEntityMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleMarkClearEntity(message);
        return null;
    }
}
