package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class MarkSnapshotHandler implements IMessageHandler<MarkSnapshotMessage, IMessage> {

    @Override
    public IMessage onMessage(MarkSnapshotMessage message, MessageContext ctx) {
        DarkGrey.proxy.scheduleMarkSnapshot(message);
        return null;
    }
}
