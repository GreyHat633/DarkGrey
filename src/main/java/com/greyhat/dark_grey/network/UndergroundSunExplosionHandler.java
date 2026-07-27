package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class UndergroundSunExplosionHandler implements IMessageHandler<UndergroundSunExplosionMessage, IMessage> {

    @Override
    public IMessage onMessage(UndergroundSunExplosionMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleUndergroundSunExplosion(message);
        return null;
    }
}
