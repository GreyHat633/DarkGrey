package com.greyhat.dark_grey.network;

import com.greyhat.dark_grey.DarkGrey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class BeyondStarSatelliteSyncMessage implements IMessage {

    public int satellites;

    public BeyondStarSatelliteSyncMessage() {}

    public BeyondStarSatelliteSyncMessage(int satellites) {
        this.satellites = satellites;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.satellites = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.satellites);
    }

    public static class Handler implements IMessageHandler<BeyondStarSatelliteSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(BeyondStarSatelliteSyncMessage message, MessageContext ctx) {
            if (ctx.side.isClient()) {
                DarkGrey.proxy.scheduleBeyondStarSatelliteSync(message);
            }
            return null;
        }
    }
}
