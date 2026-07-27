package com.greyhat.dark_grey.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.greyhat.dark_grey.api.SetBonusManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class SupernovaPlanetRenderer {

    private static class PlanetState {

        double lx, ly, lz;
        double rx, ry, rz;
        double prevLx, prevLy, prevLz;
        double prevRx, prevRy, prevRz;
        boolean initialized = false;

        int phase = 0; // 0: appearing, 1: active, 2: disappearing
        int animTimer = 0;
    }

    private static final Map<EntityPlayer, PlanetState> states = new WeakHashMap<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) return;

            for (Object obj : mc.theWorld.playerEntities) {
                if (obj instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) obj;
                    boolean hasSet = SetBonusManager.getActiveSetCount(player, "supernova_set") >= 4;
                    PlanetState state = states.get(player);

                    if (hasSet && state == null) {
                        state = new PlanetState();
                        state.phase = 0;
                        state.animTimer = 0;
                        states.put(player, state);
                    }

                    if (state != null) {
                        if (hasSet) {
                            if (state.phase == 2) {
                                state.phase = 0;
                                state.animTimer = 20 - state.animTimer;
                                if (state.animTimer < 0) state.animTimer = 0;
                            }
                            if (state.phase == 0) {
                                state.animTimer++;
                                if (state.animTimer > 20) state.phase = 1;
                            }
                        } else {
                            if (state.phase != 2) {
                                state.phase = 2;
                                state.animTimer = 0;
                            }
                            if (state.phase == 2) {
                                state.animTimer++;
                                if (state.animTimer > 20) {
                                    states.remove(player);
                                    continue;
                                }
                            }
                        }

                        double headY = player.boundingBox.minY + (player.isSneaking() ? 1.4 : 1.7);

                        double yaw = Math.toRadians(player.renderYawOffset);

                        // Target positions: orbit around player
                        double distance = 1.0;
                        double orbitSpeed = 0.05;
                        double orbitAngleL = player.ticksExisted * orbitSpeed;
                        double orbitAngleR = orbitAngleL + Math.PI;

                        double targetLx = player.posX + Math.cos(orbitAngleL) * distance;
                        double targetLz = player.posZ + Math.sin(orbitAngleL) * distance;
                        double targetLy = headY + Math.sin(player.ticksExisted * 0.1) * 0.2;

                        double targetRx = player.posX + Math.cos(orbitAngleR) * distance;
                        double targetRz = player.posZ + Math.sin(orbitAngleR) * distance;
                        double targetRy = headY + Math.sin(player.ticksExisted * 0.1 + Math.PI) * 0.2;

                        if (!state.initialized) {
                            state.lx = state.prevLx = targetLx;
                            state.ly = state.prevLy = targetLy;
                            state.lz = state.prevLz = targetLz;

                            state.rx = state.prevRx = targetRx;
                            state.ry = state.prevRy = targetRy;
                            state.rz = state.prevRz = targetRz;
                            state.initialized = true;
                        } else {
                            state.prevLx = state.lx;
                            state.prevLy = state.ly;
                            state.prevLz = state.lz;

                            state.prevRx = state.rx;
                            state.prevRy = state.ry;
                            state.prevRz = state.rz;

                            double lerp = 0.2;
                            state.lx += (targetLx - state.lx) * lerp;
                            state.ly += (targetLy - state.ly) * lerp;
                            state.lz += (targetLz - state.lz) * lerp;

                            state.rx += (targetRx - state.rx) * lerp;
                            state.ry += (targetRy - state.ry) * lerp;
                            state.rz += (targetRz - state.rz) * lerp;
                        }

                        if (state.phase == 0 || state.phase == 2) {
                            if (player.worldObj.rand.nextInt(2) == 0) {
                                double px = state.lx + (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                                double py = state.ly + (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                                double pz = state.lz + (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                                player.worldObj.spawnParticle("fireworksSpark", px, py, pz, 0, 0, 0);

                                px = state.rx + (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                                py = state.ry + (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                                double rz2 = state.rz + (player.worldObj.rand.nextDouble() - 0.5) * 0.5;
                                player.worldObj.spawnParticle("fireworksSpark", px, py, rz2, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        float pt = event.partialTicks;

        for (Object obj : mc.theWorld.playerEntities) {
            if (obj instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) obj;
                PlanetState state = states.get(player);
                if (state != null && state.initialized) {
                    double lx = state.prevLx + (state.lx - state.prevLx) * pt;
                    double ly = state.prevLy + (state.ly - state.prevLy) * pt;
                    double lz = state.prevLz + (state.lz - state.prevLz) * pt;

                    double rx = state.prevRx + (state.rx - state.prevRx) * pt;
                    double ry = state.prevRy + (state.ry - state.prevRy) * pt;
                    double rz = state.prevRz + (state.rz - state.prevRz) * pt;

                    double tx = net.minecraft.client.renderer.entity.RenderManager.renderPosX;
                    double ty = net.minecraft.client.renderer.entity.RenderManager.renderPosY;
                    double tz = net.minecraft.client.renderer.entity.RenderManager.renderPosZ;

                    float drawScale = 1.0f;
                    if (state.phase == 0) {
                        drawScale = state.animTimer / 20.0f;
                    } else if (state.phase == 2) {
                        drawScale = 1.0f - (state.animTimer / 20.0f);
                    }

                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                    GL11.glPushMatrix();
                    GL11.glTranslated(-tx, -ty, -tz);

                    drawStar(lx, ly, lz, player.ticksExisted + pt, drawScale);
                    drawStar(rx, ry, rz, player.ticksExisted + pt, drawScale);

                    GL11.glPopMatrix();
                    GL11.glPopAttrib();
                }
            }
        }
    }

    private static final net.minecraft.util.ResourceLocation SPHERE_TEXTURE = new net.minecraft.util.ResourceLocation(
        "dark_grey",
        "textures/entity/sphere_texture.png");
    private static final net.minecraft.util.ResourceLocation RING_TEXTURE = new net.minecraft.util.ResourceLocation(
        "dark_grey",
        "textures/entity/ring_texture.png");

    private void drawStar(double x, double y, double z, float ticks, float animScale) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);

        // Sphere is mostly opaque, so we ENABLE depth writing and cull face for proper 3D sorting
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);

        // Give the planet a fixed tilt (e.g., 20 degrees on Z axis like Earth/Saturn)
        GL11.glRotatef(20.0f, 0.0f, 0.0f, 1.0f);

        // Slow rotation around its local Y axis
        float rotationSpeed = 2.0f;
        GL11.glRotatef(ticks * rotationSpeed, 0.0f, 1.0f, 0.0f);

        // --- Render Planet Sphere ---
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(SPHERE_TEXTURE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // Normal blending for solid sphere

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Full color from texture
        float sphereRadius = 0.25f * animScale;

        // Setup tiny texture perturbation matrix
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        GL11.glTranslatef((ticks * 0.0005f) % 1.0f, (ticks * 0.0002f) % 1.0f, 0.0f); // Extremely slow scroll
        GL11.glRotatef((float) Math.sin(ticks * 0.01) * 1.5f, 0.0f, 0.0f, 1.0f); // Tiny swirling
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // Sphere needs to be rotated 90 degrees on X to align texture poles properly (GLU Sphere poles are on Z axis)
        GL11.glPushMatrix();
        GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        SphereDisplayList.draw(sphereRadius);
        GL11.glPopMatrix();

        // Restore texture matrix
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        // --- Render Planetary Ring ---
        // Ring is transparent, so disable depth write and cull face
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(RING_TEXTURE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // Additive blending for glowing ring

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        float ringRadius = sphereRadius * 2.5f; // Ring is much larger than the planet
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.instance;
        tessellator.startDrawingQuads();

        // Draw flat quad on XZ plane
        tessellator.addVertexWithUV(-ringRadius, 0, -ringRadius, 0.0, 0.0);
        tessellator.addVertexWithUV(-ringRadius, 0, ringRadius, 0.0, 1.0);
        tessellator.addVertexWithUV(ringRadius, 0, ringRadius, 1.0, 1.0);
        tessellator.addVertexWithUV(ringRadius, 0, -ringRadius, 1.0, 0.0);

        tessellator.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
}
