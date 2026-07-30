package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ShatteredBoneStaffCastStartHandler
    implements IMessageHandler<ShatteredBoneStaffCastStartMessage, IMessage> {

    @Override
    public IMessage onMessage(ShatteredBoneStaffCastStartMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleShatteredBoneCastStart(message);
        return null;
    }
}
