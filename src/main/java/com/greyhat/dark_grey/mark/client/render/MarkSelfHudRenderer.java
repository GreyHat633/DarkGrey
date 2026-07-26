package com.greyhat.dark_grey.mark.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.mark.MarkRegistry;
import com.greyhat.dark_grey.mark.api.IMarkType;
import com.greyhat.dark_grey.mark.api.MarkVisualData;
import com.greyhat.dark_grey.mark.client.ClientEntityMarks;
import com.greyhat.dark_grey.mark.client.ClientMarkCache;
import com.greyhat.dark_grey.mark.client.ClientMarkInstance;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MarkSelfHudRenderer {

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!com.greyhat.dark_grey.common.Config.enableSelfMarkHud) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.isDead) return;

        ClientEntityMarks marks = ClientMarkCache.get(mc.thePlayer.getEntityId());
        if (marks == null || marks.isEmpty()) return;

        List<ClientMarkInstance> list = new ArrayList<>();
        for (ClientMarkInstance instance : marks.getAll()) {
            IMarkType type = MarkRegistry.get(instance.markId);
            if (type != null && type.getVisualData() != null && type.getVisualData().showOnSelfHud) {
                list.add(instance);
            }
        }

        if (list.isEmpty()) return;

        Collections.sort(list, new Comparator<ClientMarkInstance>() {

            @Override
            public int compare(ClientMarkInstance a, ClientMarkInstance b) {
                return Long.compare(a.localCreationTime, b.localCreationTime);
            }
        });

        int maxSelf = com.greyhat.dark_grey.common.Config.maxSelfHudMarks;
        boolean hasMore = list.size() > maxSelf;
        if (hasMore) {
            list = list.subList(0, maxSelf);
        }

        ScaledResolution res = event.resolution;
        int maxPerColumn = 4;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.instance;
        FontRenderer fr = mc.fontRenderer;

        for (int i = 0; i < list.size(); i++) {
            ClientMarkInstance instance = list.get(i);

            int col = i / maxPerColumn;
            int row = i % maxPerColumn;

            int x = 10 + col * 94; // 120 * 0.75 = 90 + 4 spacing
            int y = 10 + row * 26; // 32 * 0.75 = 24 + 2 spacing

            IMarkType type = MarkRegistry.get(instance.markId);
            MarkVisualData visual = type.getVisualData();

            int width = 120;
            int height = 32;

            GL11.glPushMatrix();
            GL11.glScalef(0.75F, 0.75F, 1.0F);

            // Adjust coordinates to maintain roughly the same screen position
            int renderX = (int) (x / 0.75F);
            int renderY = (int) (y / 0.75F);

            // Draw Background and Borders (Solid background)
            int bgColor = 0xFF222222; // Completely opaque
            int borderColor = 0xFFAAAAAA;

            net.minecraft.client.gui.Gui.drawRect(renderX, renderY, renderX + width, renderY + height, bgColor); // Bg
            net.minecraft.client.gui.Gui.drawRect(renderX, renderY, renderX + width, renderY + 1, borderColor); // Top
            net.minecraft.client.gui.Gui.drawRect(renderX, renderY, renderX + 1, renderY + height, borderColor); // Left
            net.minecraft.client.gui.Gui
                .drawRect(renderX, renderY + height - 1, renderX + width, renderY + height, borderColor); // Bottom
            net.minecraft.client.gui.Gui
                .drawRect(renderX + width - 1, renderY, renderX + width, renderY + height, borderColor); // Right

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager()
                .bindTexture(visual.icon);
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(renderX + 6, renderY + 6 + 18, 0, 0.0, 1.0);
            tessellator.addVertexWithUV(renderX + 6 + 18, renderY + 6 + 18, 0, 1.0, 1.0);
            tessellator.addVertexWithUV(renderX + 6 + 18, renderY + 6, 0, 1.0, 0.0);
            tessellator.addVertexWithUV(renderX + 6, renderY + 6, 0, 0.0, 0.0);
            tessellator.draw();

            String name = StatCollector.translateToLocal(visual.displayNameKey);
            String titleText = name + " " + instance.stacks;
            int color = visual.primaryColor; // Always keep the primary color

            fr.drawStringWithShadow(titleText, renderX + 28, renderY + 6, color);

            if (instance.decaying) {
                int textW = fr.getStringWidth(titleText);
                fr.drawStringWithShadow("\u2193", renderX + 28 + textW + 2, renderY + 6 + 1, color);
            }

            String timeStr = "0:00";
            int timeColor = 0xAAAAAA;
            boolean drawTime = true;

            if (instance.markId.equals("shattered_bone")) {
                if (instance.customData != null) {
                    boolean maintainedByFracture = instance.customData.getBoolean("MaintainedByFracture");
                    boolean hasIndependentDuration = instance.customData.getBoolean("HasIndependentDuration");
                    if (maintainedByFracture) {
                        timeStr = "\u221E"; // Infinity symbol
                        timeColor = 0x55FF55; // Green
                    } else if (hasIndependentDuration) {
                        long expire = instance.customData.getLong("IndependentExpireWorldTime");
                        long remainingTicks = expire - mc.theWorld.getTotalWorldTime();
                        if (remainingTicks < 0) remainingTicks = 0;
                        long totalSeconds = remainingTicks / 20;
                        long m = totalSeconds / 60;
                        long s = totalSeconds % 60;
                        timeStr = String.format("%d:%02d", m, s);
                        if (remainingTicks <= 60) timeColor = 0xFF5555;
                    }
                }
            } else if (!instance.decaying && instance.stableUntilWorldTime > 0) {
                long remainingTicks = instance.stableUntilWorldTime - mc.theWorld.getTotalWorldTime();
                if (remainingTicks < 0) remainingTicks = 0;
                long totalSeconds = remainingTicks / 20;
                long m = totalSeconds / 60;
                long s = totalSeconds % 60;
                timeStr = String.format("%d:%02d", m, s);
            } else if (instance.decaying) {
                long remainingTicks = instance.nextDecayTriggerWorldTime - mc.theWorld.getTotalWorldTime();
                if (remainingTicks < 0) remainingTicks = 0;
                long totalSeconds = remainingTicks / 20;
                long m = totalSeconds / 60;
                long s = totalSeconds % 60;
                timeStr = String.format("%d:%02d", m, s);
                timeColor = 0xFF5555; // Light Red
                if (remainingTicks <= 20) {
                    if ((mc.theWorld.getTotalWorldTime() / 5) % 2 == 0) {
                        drawTime = false;
                    }
                }
            }

            if (drawTime) {
                fr.drawStringWithShadow(timeStr, renderX + 28, renderY + 18, timeColor);
            }

            GL11.glPopMatrix();
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
