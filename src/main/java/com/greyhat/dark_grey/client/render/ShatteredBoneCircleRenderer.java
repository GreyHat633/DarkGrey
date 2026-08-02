package com.greyhat.dark_grey.client.render;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ShatteredBoneCircleRenderer {

    private static final ResourceLocation CIRCLE_TEXTURE = new ResourceLocation(
        "dark_grey",
        "textures/particles/shattered_bone_circle.png"); // Using default or a blank for now, or generating one
    private static final int PARTICLES_PER_RENDER = 6;
    private static final double RENDER_PASSES_PER_CLIENT_TICK_AT_60_FPS = 60.0D / 20.0D;
    private static final double PARTICLES_PER_CLIENT_TICK = PARTICLES_PER_RENDER
        * RENDER_PASSES_PER_CLIENT_TICK_AT_60_FPS;
    private static final double MAX_PARTICLE_BUDGET = PARTICLES_PER_CLIENT_TICK * 2.0D;
    private static final long MAX_CATCH_UP_TICKS = 2L;

    public static final ShatteredBoneCircleRenderer INSTANCE = new ShatteredBoneCircleRenderer();

    private final Map<Integer, CastData> activeCasts = new ConcurrentHashMap<>();
    private final Map<Integer, Double> particleBudgets = new ConcurrentHashMap<>();
    private World particleBudgetWorld;
    private double lastParticleBudgetTime = Double.NaN;
    private int particlePassLimit;

    private ShatteredBoneCircleRenderer() {}

    public void addCast(int entityId, double x, double y, double z, float radius, long endTime) {
        World currentWorld = Minecraft.getMinecraft().theWorld;
        if (currentWorld != null && particleBudgetWorld != null && particleBudgetWorld != currentWorld) {
            clearAllCasts();
        }
        if (currentWorld != null && particleBudgetWorld == null) {
            particleBudgetWorld = currentWorld;
            lastParticleBudgetTime = Double.NaN;
        }
        activeCasts.put(entityId, new CastData(x, y, z, radius, endTime));
        particleBudgets.put(entityId, (double) PARTICLES_PER_RENDER);
    }

    public void removeCast(int entityId) {
        activeCasts.remove(entityId);
        particleBudgets.remove(entityId);
    }

    public boolean hasActiveCast(int entityId) {
        return activeCasts.containsKey(entityId);
    }

    private void clearAllCasts() {
        activeCasts.clear();
        particleBudgets.clear();
        particleBudgetWorld = null;
        lastParticleBudgetTime = Double.NaN;
        particlePassLimit = 0;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        resetForWorldChange(mc.theWorld);
        if (activeCasts.isEmpty()) {
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
                particleBudgets.remove(entry.getKey());
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
        try {
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
                    new net.minecraft.util.ResourceLocation(
                        "dark_grey",
                        "textures/particles/shattered_bone_circle.png"));

            Tessellator tessellator = Tessellator.instance;

            for (CastData data : activeCasts.values()) {
                float pulse = (float) Math.sin((currentTime + event.partialTicks) * 0.2) * 0.2f + 0.5f;
                double rotationTimer = (currentTime + event.partialTicks) * 1.5;

                GL11.glPushMatrix();
                try {
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
                } finally {
                    GL11.glPopMatrix();
                }
            }
        } finally {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // Just in case, reset color before popping
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }

        // --- Spawn Minecraft Particles ---
        // Keep the old six-particle/60 FPS rate while charging and consuming a bounded tick budget.
        if (!mc.isGamePaused()) {
            advanceParticleBudget(mc.theWorld, event.partialTicks);
            for (Map.Entry<Integer, CastData> entry : activeCasts.entrySet()) {
                CastData data = entry.getValue();
                int particlesToSpawn = takeParticleBudget(entry.getKey(), particlePassLimit);
                int innerParticles = (int) Math.round(particlesToSpawn * (4.0D / PARTICLES_PER_RENDER));
                innerParticles = Math.min(particlesToSpawn, innerParticles);
                int edgeParticles = particlesToSpawn - innerParticles;

                for (int i = 0; i < innerParticles; i++) {
                    double r = data.radius * Math.sqrt(mc.theWorld.rand.nextDouble()); // Random point inside circle
                    double theta = mc.theWorld.rand.nextDouble() * 2 * Math.PI;
                    double px = data.x + r * Math.cos(theta);
                    double pz = data.z + r * Math.sin(theta);
                    double py = data.y + 0.1 + mc.theWorld.rand.nextDouble() * 0.2;

                    // Bone fragments
                    mc.theWorld.spawnParticle("iconcrack_352", px, py, pz, 0, 0.1, 0);
                }

                // Edge particles
                for (int i = 0; i < edgeParticles; i++) {
                    double theta = mc.theWorld.rand.nextDouble() * 2 * Math.PI;
                    double px = data.x + data.radius * Math.cos(theta);
                    double pz = data.z + data.radius * Math.sin(theta);
                    mc.theWorld.spawnParticle("iconcrack_352", px, data.y + 0.1, pz, 0, 0.1, 0);
                }
            }
        }
    }

    private void resetForWorldChange(World world) {
        if (particleBudgetWorld != null && particleBudgetWorld != world) {
            activeCasts.clear();
            particleBudgets.clear();
        }
        if (particleBudgetWorld != world) {
            particleBudgetWorld = world;
            lastParticleBudgetTime = Double.NaN;
            particlePassLimit = 0;
        }
    }

    private void advanceParticleBudget(World world, float partialTicks) {
        resetForWorldChange(world);
        double currentTime = world.getTotalWorldTime() + partialTicks;
        if (Double.isNaN(lastParticleBudgetTime)) {
            lastParticleBudgetTime = currentTime - (1.0D / RENDER_PASSES_PER_CLIENT_TICK_AT_60_FPS);
        }

        double elapsedTime = currentTime - lastParticleBudgetTime;
        if (elapsedTime <= 0.0D) {
            particlePassLimit = 0;
            return;
        }

        double chargedTime = Math.min(elapsedTime, (double) MAX_CATCH_UP_TICKS);
        double charge = PARTICLES_PER_CLIENT_TICK * chargedTime;
        particlePassLimit = Math.max(1, (int) Math.ceil(charge));
        for (Integer entityId : activeCasts.keySet()) {
            Double budget = particleBudgets.get(entityId);
            if (budget == null) {
                budget = 0.0D;
            }
            particleBudgets.put(entityId, Math.min(MAX_PARTICLE_BUDGET, budget + charge));
        }
        lastParticleBudgetTime = currentTime;
    }

    private int takeParticleBudget(int entityId, int requested) {
        Double budget = particleBudgets.get(entityId);
        if (budget == null || budget < 1.0D) {
            return 0;
        }

        int available = (int) Math.floor(budget);
        int emitted = Math.min(requested, available);
        particleBudgets.put(entityId, budget - emitted);
        return emitted;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            clearAllCasts();
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player.worldObj != null && event.player.worldObj.isRemote) {
            clearAllCasts();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.worldObj != null && event.player.worldObj.isRemote) {
            clearAllCasts();
        }
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
