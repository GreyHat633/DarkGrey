package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public final class ShatteredBoneParticlesHandler implements IMessageHandler<ShatteredBoneParticlesMessage, IMessage> {

    @Override
    public IMessage onMessage(ShatteredBoneParticlesMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleShatteredBoneParticles(message);
        return null;
    }
}
