package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.greyhat.dark_grey.api.BeyondStarOrbitMath;
import com.greyhat.dark_grey.api.BeyondStarSatelliteManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BeyondStarOrbitRenderer {

    private static final int RENDER_ATTRIB_MASK = GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
        | GL11.GL_CURRENT_BIT
        | GL11.GL_DEPTH_BUFFER_BIT
        | GL11.GL_TEXTURE_BIT
        | GL11.GL_TRANSFORM_BIT;
    private final Frustrum renderFrustum = new Frustrum();
    private static final net.minecraft.util.ResourceLocation SPHERE_TEXTURE = new net.minecraft.util.ResourceLocation(
        "dark_grey",
        "textures/entity/beyond_star_satellite.png");
    private static final net.minecraft.util.ResourceLocation RING_TEXTURE = new net.minecraft.util.ResourceLocation(
        "dark_grey",
        "textures/entity/beyond_star_satellite_ring.png");

    @SubscribeEvent
    public void onRenderWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        float pt = event.partialTicks;

        double tx = net.minecraft.client.renderer.entity.RenderManager.renderPosX;
        double ty = net.minecraft.client.renderer.entity.RenderManager.renderPosY;
        double tz = net.minecraft.client.renderer.entity.RenderManager.renderPosZ;
        this.renderFrustum.setPosition(tx, ty, tz);

        GL11.glPushAttrib(RENDER_ATTRIB_MASK);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(-tx, -ty, -tz);
            for (Object obj : mc.theWorld.playerEntities) {
                if (obj instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) obj;
                    int count = BeyondStarSatelliteManager.getCount(player);
                    if (count > 0) {
                        for (int i = 0; i < count; i++) {
                            double[] pos = BeyondStarOrbitMath
                                .getOrbitPosition(player, i, count, player.ticksExisted, pt, true);
                            if (isVisible(pos[0], pos[1], pos[2])) {
                                drawStar(pos[0], pos[1], pos[2], player.ticksExisted + pt);

                                // Drop magma-like particles from the satellite (scaled down lava)
                                if (Math.random() < 0.04) {
                                    net.minecraft.client.particle.EntityFX fx = new net.minecraft.client.particle.EntityLavaFX(
                                        mc.theWorld,
                                        pos[0],
                                        pos[1],
                                        pos[2]);
                                    fx.multipleParticleScaleBy(0.3F);
                                    mc.effectRenderer.addEffect(fx);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private boolean isVisible(double x, double y, double z) {
        AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5);
        return this.renderFrustum.isBoundingBoxInFrustum(bounds);
    }

    private void drawStar(double x, double y, double z, float ticks) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);

        GL11.glRotatef(20.0f, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(ticks * 2.0f, 0.0f, 1.0f, 0.0f);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(SPHERE_TEXTURE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        float sphereRadius = 0.25f;

        GL11.glPushMatrix();
        GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        SphereDisplayList.draw(sphereRadius);
        GL11.glPopMatrix();

        GL11.glDepthMask(false);
        // Ring rendering removed based on user feedback

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
}
