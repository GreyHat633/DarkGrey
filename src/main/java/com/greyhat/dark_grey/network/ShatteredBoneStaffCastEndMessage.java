package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ShatteredBoneStaffCastEndMessage implements IMessage {

    public int casterEntityId;

    public ShatteredBoneStaffCastEndMessage() {}

    public ShatteredBoneStaffCastEndMessage(int casterEntityId) {
        this.casterEntityId = casterEntityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.casterEntityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.casterEntityId);
    }
}
