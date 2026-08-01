package com.greyhat.dark_grey.client.render;

import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.entity.EntityWolfsbaneBullet;

public class RenderWolfsbaneBullet extends RenderSnowball {

    public RenderWolfsbaneBullet(Item item) {
        super(item);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        boolean isHeavy = (entity instanceof EntityWolfsbaneBullet) && ((EntityWolfsbaneBullet) entity).isHeavyStrike();

        if (isHeavy) {
            // Tint the bullet red for death bullets
            GL11.glColor4f(1.0F, 0.15F, 0.15F, 1.0F);
        }

        super.doRender(entity, x, y, z, yaw, partialTicks);

        if (isHeavy) {
            // Reset color to avoid bleeding into other renders
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
