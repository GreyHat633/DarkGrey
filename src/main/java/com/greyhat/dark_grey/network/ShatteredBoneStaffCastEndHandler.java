package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ShatteredBoneStaffCastEndHandler implements IMessageHandler<ShatteredBoneStaffCastEndMessage, IMessage> {

    @Override
    public IMessage onMessage(ShatteredBoneStaffCastEndMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleShatteredBoneCastEnd(message);
        return null;
    }
}
