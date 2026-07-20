package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ConfigSyncHandler implements IMessageHandler<ConfigSyncMessage, IMessage> {

    @Override
    public IMessage onMessage(final ConfigSyncMessage message, MessageContext context) {
        DarkGrey.proxy.scheduleConfigApply(message.getJson());
        return null;
    }
}
