package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class SolarFlareImpactMessage implements IMessage {

    private double motionX;
    private double motionY;
    private double motionZ;

    public SolarFlareImpactMessage() {}

    public SolarFlareImpactMessage(double motionX, double motionY, double motionZ) {
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
    }

    public double getMotionX() {
        return this.motionX;
    }

    public double getMotionY() {
        return this.motionY;
    }

    public double getMotionZ() {
        return this.motionZ;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.motionX = buffer.readDouble();
        this.motionY = buffer.readDouble();
        this.motionZ = buffer.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeDouble(this.motionX);
        buffer.writeDouble(this.motionY);
        buffer.writeDouble(this.motionZ);
    }
}
