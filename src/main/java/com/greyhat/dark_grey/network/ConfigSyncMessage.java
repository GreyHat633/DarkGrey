package com.greyhat.dark_grey.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ConfigSyncMessage implements IMessage {

    private String json;

    public ConfigSyncMessage() {
        this.json = "";
    }

    public ConfigSyncMessage(String json) {
        this.json = json == null ? "" : json;
    }

    public String getJson() {
        return this.json;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.json = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, this.json);
    }
}
