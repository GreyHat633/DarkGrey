package com.greyhat.dark_grey.network;

import net.minecraft.client.Minecraft;

import com.greyhat.dark_grey.client.render.PolarityMushroomCloudEffect;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PolarityExplosionEffectHandler implements IMessageHandler<PolarityExplosionEffectMessage, IMessage> {

    @Override
    public IMessage onMessage(PolarityExplosionEffectMessage message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            handleClientSide(message);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private void handleClientSide(PolarityExplosionEffectMessage message) {
        Minecraft.getMinecraft()
            .func_152344_a(new Runnable() {

                @Override
                public void run() {
                    if (Minecraft.getMinecraft().theWorld != null) {
                        PolarityMushroomCloudEffect.spawnEffect(
                            Minecraft.getMinecraft().theWorld,
                            message.x,
                            message.y,
                            message.z,
                            message.special);
                    }
                }
            });
    }
}
