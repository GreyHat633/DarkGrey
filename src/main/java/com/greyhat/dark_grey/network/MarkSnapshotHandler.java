package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.mark.client.ClientMarkCache;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MarkSnapshotHandler implements IMessageHandler<MarkSnapshotMessage, IMessage> {

    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MarkSnapshotMessage message, MessageContext ctx) {
        ClientMarkCache.clear(message.entityId);
        if (message.marks != null) {
            for (MarkSyncMessage.MarkData data : message.marks) {
                ClientMarkCache.updateMark(message.entityId, data, (byte) 0, 0, false);
            }
        }
        return null;
    }
}
