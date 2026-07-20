package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class SolarFlareImpactHandler implements IMessageHandler<SolarFlareImpactMessage, IMessage> {

    @Override
    public IMessage onMessage(SolarFlareImpactMessage message, MessageContext context) {
        DarkGrey.proxy.scheduleSolarFlareImpact(message.getMotionX(), message.getMotionY(), message.getMotionZ());
        return null;
    }
}
