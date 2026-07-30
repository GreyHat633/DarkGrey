package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ShatteredBoneStaffCastStartMessage implements IMessage {

    public int casterEntityId;
    public double anchorX;
    public double anchorY;
    public double anchorZ;
    public float radius;
    public long castStartWorldTime;
    public long castEndWorldTime;

    public ShatteredBoneStaffCastStartMessage() {}

    public ShatteredBoneStaffCastStartMessage(int casterEntityId, double anchorX, double anchorY, double anchorZ,
        float radius, long castStartWorldTime, long castEndWorldTime) {
        this.casterEntityId = casterEntityId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.radius = radius;
        this.castStartWorldTime = castStartWorldTime;
        this.castEndWorldTime = castEndWorldTime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.casterEntityId = buf.readInt();
        this.anchorX = buf.readDouble();
        this.anchorY = buf.readDouble();
        this.anchorZ = buf.readDouble();
        this.radius = buf.readFloat();
        this.castStartWorldTime = buf.readLong();
        this.castEndWorldTime = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.casterEntityId);
        buf.writeDouble(this.anchorX);
        buf.writeDouble(this.anchorY);
        buf.writeDouble(this.anchorZ);
        buf.writeFloat(this.radius);
        buf.writeLong(this.castStartWorldTime);
        buf.writeLong(this.castEndWorldTime);
    }
}
