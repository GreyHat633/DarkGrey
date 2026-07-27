package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ErebusHitHandler implements IMessageHandler<ErebusHitMessage, IMessage> {

    @Override
    public IMessage onMessage(ErebusHitMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleErebusHit(message);
        return null;
    }
}
