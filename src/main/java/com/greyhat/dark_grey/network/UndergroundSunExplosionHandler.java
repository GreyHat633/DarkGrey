package com.greyhat.dark_grey.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class UndergroundSunExplosionHandler implements IMessageHandler<UndergroundSunExplosionMessage, IMessage> {

    @Override
    public IMessage onMessage(UndergroundSunExplosionMessage message, MessageContext ctx) {
        if (ctx.side == Side.CLIENT) {
            handleClientSide(message);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    private void handleClientSide(UndergroundSunExplosionMessage message) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;

        double x = message.x;
        double y = message.y;
        double z = message.z;
        float r = message.radius;

        world.spawnParticle("hugeexplosion", x, y, z, 0.0D, 0.0D, 0.0D);
        for (int i = 0; i < 5; i++) {
            double ox = (world.rand.nextDouble() - 0.5D) * r * 0.5;
            double oy = (world.rand.nextDouble() - 0.5D) * r * 0.5;
            double oz = (world.rand.nextDouble() - 0.5D) * r * 0.5;
            world.spawnParticle("largeexplode", x + ox, y + oy, z + oz, 0.0D, 0.0D, 0.0D);
        }

        int particles = 600;
        for (int i = 0; i < particles; i++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double dist = world.rand.nextDouble() * r;

            double px = x + Math.cos(angle) * dist;
            double pz = z + Math.sin(angle) * dist;
            double py = y + (world.rand.nextDouble() - 0.5D) * 4.0D;

            double vx = Math.cos(angle) * 1.5D;
            double vz = Math.sin(angle) * 1.5D;
            double vy = (world.rand.nextDouble() - 0.5D) * 1.5D;

            world.spawnParticle("flame", px, py, pz, vx, vy, vz);

            if (i % 3 == 0) {
                world.spawnParticle("crit", px, py, pz, vx * 1.5, vy * 1.5, vz * 1.5);
            }
        }
    }
}
