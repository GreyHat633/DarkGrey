package com.greyhat.dark_grey.client.render;

import java.util.Random;

import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PolarityMushroomCloudEffect {

    public static void spawnEffect(World world, double x, double y, double z, boolean special) {
        Random rand = world.rand;
        int count = special ? 200 : 100;
        float radius = 8.0f;

        // Center flash
        world.spawnParticle("hugeexplosion", x, y, z, 1.0D, 0.0D, 0.0D);

        // Ground shockwave (ring)
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double r = rand.nextDouble() * radius;
            double dx = Math.cos(angle) * r;
            double dz = Math.sin(angle) * r;

            // Ground level outward dust
            world.spawnParticle("explode", x + dx, y + 0.5, z + dz, dx * 0.1, 0.0, dz * 0.1);
        }

        // Rising pillar and mushroom cap
        int height = special ? 15 : 10;
        int cloudWidth = special ? 8 : 5;
        for (int i = 0; i < count * 2; i++) {
            // Pillar
            if (rand.nextBoolean()) {
                double px = x + (rand.nextDouble() - 0.5) * 2.0;
                double py = y + rand.nextDouble() * height;
                double pz = z + (rand.nextDouble() - 0.5) * 2.0;
                world.spawnParticle("largesmoke", px, py, pz, 0, 0.1, 0);
                if (special) {
                    world.spawnParticle("flame", px, py, pz, 0, 0.1, 0);
                }
            } else {
                // Cap
                double angle = rand.nextDouble() * Math.PI * 2;
                double r = rand.nextDouble() * cloudWidth;
                double cx = x + Math.cos(angle) * r;
                double cy = y + height - 2.0 + (rand.nextDouble() - 0.5) * 3.0;
                double cz = z + Math.sin(angle) * r;
                world.spawnParticle("largesmoke", cx, cy, cz, Math.cos(angle) * 0.05, 0.05, Math.sin(angle) * 0.05);
            }
        }
    }
}
