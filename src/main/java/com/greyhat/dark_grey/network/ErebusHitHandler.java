package com.greyhat.dark_grey.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ErebusHitHandler implements IMessageHandler<ErebusHitMessage, IMessage> {

    @Override
    public IMessage onMessage(ErebusHitMessage message, MessageContext ctx) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world != null) {
            for (int id : message.entityIds) {
                Entity entity = world.getEntityByID(id);
                if (entity != null) {
                    spawnCurseParticle(world, entity.posX, entity.posY + entity.height / 2.0, entity.posZ, 5);
                }
            }
        }
        return null;
    }

    private void spawnCurseParticle(World world, double x, double y, double z, int count) {
        for (int i = 0; i < count * 3; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5);
            double offsetY = (world.rand.nextDouble() - 0.5);
            double offsetZ = (world.rand.nextDouble() - 0.5);
            world.spawnParticle(
                "mobSpell",
                x + offsetX,
                y + offsetY,
                z + offsetZ,
                0.5 + world.rand.nextDouble() * 0.4,
                0.0,
                0.7 + world.rand.nextDouble() * 0.3);
            world.spawnParticle("witchMagic", x + offsetX, y + offsetY, z + offsetZ, 0, 0, 0);
        }
        world.spawnParticle("largeexplode", x, y, z, 0, 0, 0);
    }
}
