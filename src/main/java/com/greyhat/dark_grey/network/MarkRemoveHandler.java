package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.mark.client.ClientEntityMarks;
import com.greyhat.dark_grey.mark.client.ClientMarkCache;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MarkRemoveHandler implements IMessageHandler<MarkRemoveMessage, IMessage> {

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MarkRemoveMessage message, MessageContext ctx) {
        ClientEntityMarks marks = ClientMarkCache.get(message.entityId);
        if (marks != null) {
            marks.removeMark(message.markId);

        }
        return null;
    }
}
