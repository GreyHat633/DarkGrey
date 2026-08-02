package com.greyhat.dark_grey.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.component.ComponentPolarity;
import com.greyhat.dark_grey.mark.client.ClientEntityMarks;
import com.greyhat.dark_grey.mark.client.ClientMarkCache;
import com.greyhat.dark_grey.mark.type.NegativePolarityMarkType;
import com.greyhat.dark_grey.mark.type.PositivePolarityMarkType;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PolarityUIRenderer {

    private static final double PARTICLES_PER_RENDER_AT_60_FPS = 0.5D;
    private static final double PARTICLES_PER_SECOND = PARTICLES_PER_RENDER_AT_60_FPS * 60.0D;
    private static final double MAX_CATCH_UP_SECONDS = 0.1D;
    private static final double MAX_PARTICLE_BUDGET = PARTICLES_PER_SECOND * MAX_CATCH_UP_SECONDS;

    private final Map<Integer, Double> particleBudgets = new HashMap<>();
    private final Map<Integer, Long> lastAttemptFrames = new HashMap<>();
    private World particleBudgetWorld;
    private long lastParticleBudgetNanos = Long.MIN_VALUE;
    private long frameSequence;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        frameSequence++;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        net.minecraft.item.ItemStack held = mc.thePlayer.getCurrentEquippedItem();
        if (held != null && held.getItem() instanceof com.greyhat.dark_grey.api.IRPGItemContainer) {
            com.greyhat.dark_grey.api.IRPGItemContainer container = (com.greyhat.dark_grey.api.IRPGItemContainer) held
                .getItem();
            if ("polarity".equals(container.getRpgItemId())) {
                int mode = ComponentPolarity.MODE_POSITIVE;
                if (held.hasTagCompound() && held.getTagCompound()
                    .hasKey("DarkGreyPolarityMode")) {
                    mode = held.getTagCompound()
                        .getInteger("DarkGreyPolarityMode");
                }

                ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                int width = res.getScaledWidth();
                int height = res.getScaledHeight();

                FontRenderer fr = mc.fontRenderer;
                String text = mode == ComponentPolarity.MODE_POSITIVE ? "\u00a7c+" : "\u00a79-";

                GL11.glPushMatrix();
                GL11.glScalef(2.0f, 2.0f, 2.0f);
                int strWidth = fr.getStringWidth(text);
                fr.drawStringWithShadow(text, (width / 4) - (strWidth / 2), (height / 4) + 10, 0xFFFFFF);
                GL11.glPopMatrix();
            }
        }
    }

    @SubscribeEvent
    public void onRenderLivingSpecials(RenderLivingEvent.Specials.Post event) {
        EntityLivingBase entity = event.entity;
        Minecraft mc = Minecraft.getMinecraft();
        if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;

        if (entity.worldObj == null) return;
        resetForWorldChange(entity.worldObj);
        advanceParticleBudget(entity.worldObj);

        ClientEntityMarks marks = ClientMarkCache.get(entity.getEntityId());
        if (marks != null) {
            boolean hasPositive = marks.getMark(PositivePolarityMarkType.ID) != null;
            boolean hasNegative = marks.getMark(NegativePolarityMarkType.ID) != null;

            if (hasPositive || hasNegative) {
                if (takeParticleBudget(entity.worldObj, entity.getEntityId())) {
                    if (hasPositive) {
                        Minecraft.getMinecraft().effectRenderer.addEffect(
                            new com.greyhat.dark_grey.client.particle.EntityPolaritySymbolFX(
                                entity.worldObj,
                                entity.posX + (entity.worldObj.rand.nextDouble() - 0.5),
                                entity.boundingBox.minY + entity.worldObj.rand.nextDouble() * entity.height,
                                entity.posZ + (entity.worldObj.rand.nextDouble() - 0.5),
                                "+",
                                1.0f,
                                0.0f,
                                0.0f));
                    } else {
                        Minecraft.getMinecraft().effectRenderer.addEffect(
                            new com.greyhat.dark_grey.client.particle.EntityPolaritySymbolFX(
                                entity.worldObj,
                                entity.posX + (entity.worldObj.rand.nextDouble() - 0.5),
                                entity.boundingBox.minY + entity.worldObj.rand.nextDouble() * entity.height,
                                entity.posZ + (entity.worldObj.rand.nextDouble() - 0.5),
                                "-",
                                0.0f,
                                0.0f,
                                1.0f));
                    }
                }
            } else {
                particleBudgets.remove(entity.getEntityId());
                lastAttemptFrames.remove(entity.getEntityId());
            }
        } else {
            particleBudgets.remove(entity.getEntityId());
            lastAttemptFrames.remove(entity.getEntityId());
        }
    }

    private void resetForWorldChange(World world) {
        if (particleBudgetWorld != world) {
            particleBudgets.clear();
            lastAttemptFrames.clear();
            particleBudgetWorld = world;
            lastParticleBudgetNanos = Long.MIN_VALUE;
        }
    }

    private void advanceParticleBudget(World world) {
        resetForWorldChange(world);
        long currentNanos = System.nanoTime();
        if (lastParticleBudgetNanos == Long.MIN_VALUE) {
            lastParticleBudgetNanos = currentNanos;
            return;
        }
        long elapsedNanos = currentNanos - lastParticleBudgetNanos;
        if (elapsedNanos <= 0L) {
            return;
        }

        double elapsedSeconds = Math.min(elapsedNanos / 1_000_000_000.0D, MAX_CATCH_UP_SECONDS);
        double charge = PARTICLES_PER_SECOND * elapsedSeconds;
        for (Map.Entry<Integer, Double> entry : particleBudgets.entrySet()) {
            entry.setValue(Math.min(MAX_PARTICLE_BUDGET, entry.getValue() + charge));
        }
        // Discard time beyond the bounded catch-up window instead of building an unbounded burst.
        lastParticleBudgetNanos = currentNanos;
    }

    private boolean takeParticleBudget(World world, int entityId) {
        Long lastAttemptFrame = lastAttemptFrames.get(entityId);
        if (lastAttemptFrame != null && lastAttemptFrame.longValue() == frameSequence) {
            return false;
        }
        lastAttemptFrames.put(entityId, frameSequence);

        // Keep the original one-chance-per-render burst rhythm and cap it with the token budget.
        if (world.rand.nextInt(2) != 0) {
            return false;
        }

        Double budget = particleBudgets.get(entityId);
        if (budget == null) {
            // A new entity may start with one token, but it still has to pass the random gate above.
            budget = 1.0D;
        }
        if (budget < 1.0D) {
            particleBudgets.put(entityId, budget);
            return false;
        }

        particleBudgets.put(entityId, budget - 1.0D);
        return true;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            particleBudgets.clear();
            lastAttemptFrames.clear();
            particleBudgetWorld = null;
            lastParticleBudgetNanos = Long.MIN_VALUE;
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player.worldObj != null && event.player.worldObj.isRemote) {
            particleBudgets.clear();
            lastAttemptFrames.clear();
            particleBudgetWorld = null;
            lastParticleBudgetNanos = Long.MIN_VALUE;
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.worldObj != null && event.player.worldObj.isRemote) {
            particleBudgets.clear();
            lastAttemptFrames.clear();
            particleBudgetWorld = null;
            lastParticleBudgetNanos = Long.MIN_VALUE;
        }
    }
}
