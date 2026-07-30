package com.greyhat.dark_grey.client.render;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ShatteredBoneCircleRenderer {

    private static final ResourceLocation CIRCLE_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/particles/shattered_bone_circle.png"); // Using default or a blank for now, or generating one

    public static final ShatteredBoneCircleRenderer INSTANCE = new ShatteredBoneCircleRenderer();

    private final Map<Integer, CastData> activeCasts = new ConcurrentHashMap<>();

    private ShatteredBoneCircleRenderer() {}

    public void addCast(int entityId, double x, double y, double z, float radius, long endTime) {
        activeCasts.put(entityId, new CastData(x, y, z, radius, endTime));
    }

    public void removeCast(int entityId) {
        activeCasts.remove(entityId);
    }

    public boolean hasActiveCast(int entityId) {
        return activeCasts.containsKey(entityId);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (activeCasts.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        long currentTime = mc.theWorld.getTotalWorldTime();

        // Cleanup expired casts
        Iterator<Map.Entry<Integer, CastData>> it = activeCasts.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, CastData> entry = it.next();
            if (currentTime >= entry.getValue().endTime) {
                it.remove();
            }
        }

        if (activeCasts.isEmpty()) {
            return;
        }

        double interpPosX = mc.thePlayer.lastTickPosX
            + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double interpPosY = mc.thePlayer.lastTickPosY
            + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double interpPosZ = mc.thePlayer.lastTickPosZ
            + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;

        // Save ALL OpenGL attributes to prevent state leaks (black inventory bug)
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glPushMatrix();
        GL11.glTranslated(-interpPosX, -interpPosY, -interpPosZ);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive blending
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(
                new net.minecraft.util.ResourceLocation("dark_grey", "textures/particles/shattered_bone_circle.png"));

        Tessellator tessellator = Tessellator.instance;

        for (CastData data : activeCasts.values()) {
            float pulse = (float) Math.sin((currentTime + event.partialTicks) * 0.2) * 0.2f + 0.5f;
            double rotationTimer = (currentTime + event.partialTicks) * 1.5;

            GL11.glPushMatrix();
            GL11.glTranslated(data.x, data.y + 0.05, data.z);
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F); // Lay flat on the ground
            GL11.glRotatef((float) rotationTimer, 0.0F, 0.0F, 1.0F); // Rotate the circle

            float alpha = pulse * 0.8f;
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA_F(1.0f, 1.0f, 1.0f, alpha); // White glowing

            // Draw a quad spanning from -radius to +radius
            float r = data.radius;
            tessellator.addVertexWithUV(-r, -r, 0, 0.0, 0.0);
            tessellator.addVertexWithUV(-r, r, 0, 0.0, 1.0);
            tessellator.addVertexWithUV(r, r, 0, 1.0, 1.0);
            tessellator.addVertexWithUV(r, -r, 0, 1.0, 0.0);

            tessellator.draw();

            GL11.glPopMatrix();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // Just in case, reset color before popping
        GL11.glPopMatrix();

        // Restore all GL attributes safely
        GL11.glPopAttrib();

        // --- Spawn Minecraft Particles ---
        // Spawn 3-5 particles per tick randomly around the circle area
        if (!mc.isGamePaused()) {
            for (CastData data : activeCasts.values()) {
                for (int i = 0; i < 4; i++) {
                    double r = data.radius * Math.sqrt(mc.theWorld.rand.nextDouble()); // Random point inside circle
                    double theta = mc.theWorld.rand.nextDouble() * 2 * Math.PI;
                    double px = data.x + r * Math.cos(theta);
                    double pz = data.z + r * Math.sin(theta);
                    double py = data.y + 0.1 + mc.theWorld.rand.nextDouble() * 0.2;

                    // Bone fragments
                    mc.theWorld.spawnParticle("iconcrack_352", px, py, pz, 0, 0.1, 0);
                }

                // Edge particles
                for (int i = 0; i < 2; i++) {
                    double theta = mc.theWorld.rand.nextDouble() * 2 * Math.PI;
                    double px = data.x + data.radius * Math.cos(theta);
                    double pz = data.z + data.radius * Math.sin(theta);
                    mc.theWorld.spawnParticle("iconcrack_352", px, data.y + 0.1, pz, 0, 0.1, 0);
                }
            }
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // Just in case, reset color before popping
        GL11.glPopMatrix();

        // Restore all GL attributes safely
        GL11.glPopAttrib();
    }

    private static class CastData {

        final double x, y, z;
        final float radius;
        final long endTime;

        CastData(double x, double y, double z, float radius, long endTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.endTime = endTime;
        }
    }
}
