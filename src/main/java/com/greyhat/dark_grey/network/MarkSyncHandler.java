package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.mark.client.ClientMarkCache;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MarkSyncHandler implements IMessageHandler<MarkSyncMessage, IMessage> {

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MarkSyncMessage message, MessageContext ctx) {
        ClientMarkCache.updateMark(
            message.entityId,
            message.data,
            message.changeReason,
            message.displayedDelta,
            message.immediateTriggered);
        return null;
    }
}
