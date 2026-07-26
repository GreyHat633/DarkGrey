package com.greyhat.dark_grey.mark.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;

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
public class MarkEntityOverlayRenderer {

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post event) {
        if (!com.greyhat.dark_grey.common.Config.enableEntityMarkIcons) return;

        EntityLivingBase entity = event.entity;
        Minecraft mc = Minecraft.getMinecraft();

        if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;
        if (entity.isDead || entity.isInvisible()) return;

        double dSq = mc.renderViewEntity.getDistanceSqToEntity(entity);
        double maxDist = com.greyhat.dark_grey.common.Config.markRenderDistance;
        if (dSq > maxDist * maxDist) return;

        ClientEntityMarks marks = ClientMarkCache.get(entity.getEntityId());
        if (marks == null || marks.isEmpty()) return;

        List<ClientMarkInstance> list = new ArrayList<>();
        for (ClientMarkInstance instance : marks.getAll()) {
            IMarkType type = MarkRegistry.get(instance.markId);
            if (type != null && type.getVisualData() != null && type.getVisualData().showOnEntity) {
                list.add(instance);
            }
        }

        if (list.isEmpty()) return;

        // Sort by priority, etc.
        Collections.sort(list, new Comparator<ClientMarkInstance>() {

            @Override
            public int compare(ClientMarkInstance a, ClientMarkInstance b) {
                if (a.maxed && !b.maxed) return -1;
                if (!a.maxed && b.maxed) return 1;
                if (a.decaying && !b.decaying) return -1;
                if (!a.decaying && b.decaying) return 1;
                return a.markId.compareTo(b.markId);
            }
        });

        int maxIcons = com.greyhat.dark_grey.common.Config.maxEntityMarkIcons;
        boolean hasMore = list.size() > maxIcons;
        if (hasMore) {
            list = list.subList(0, maxIcons);
        }

        double x = event.x;
        double y = event.y + entity.height + 0.6; // Moved down from 0.85
        double z = event.z;

        FontRenderer fontRenderer = mc.fontRenderer;
        float f = 1.6F * 0.75F; // Scaled down to 0.75x
        float f1 = 0.016666668F * f;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(-f1, -f1, f1);

        if (!com.greyhat.dark_grey.common.Config.showMarksThroughWalls) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
        }

        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glDisable(GL11.GL_LIGHTING);

        int totalWidth = 0;
        List<String> texts = new ArrayList<>();
        List<MarkVisualData> visuals = new ArrayList<>();

        for (ClientMarkInstance instance : list) {
            IMarkType type = MarkRegistry.get(instance.markId);
            visuals.add(type.getVisualData());
            String text = String.valueOf(instance.stacks);
            texts.add(text);
        }

        int MAX_PER_ROW = 3;
        int numRows = (list.size() + MAX_PER_ROW - 1) / MAX_PER_ROW;
        int rowHeight = 16;

        Tessellator tessellator = Tessellator.instance;

        for (int r = 0; r < numRows; r++) {
            int startIdx = r * MAX_PER_ROW;
            int endIdx = Math.min(startIdx + MAX_PER_ROW, list.size());

            int rowWidth = 0;
            int[] itemWidths = new int[endIdx - startIdx];
            for (int i = startIdx; i < endIdx; i++) {
                ClientMarkInstance instance = list.get(i);
                int textW = fontRenderer.getStringWidth(texts.get(i));
                if (instance.decaying) {
                    textW += 2 + fontRenderer.getStringWidth("\u2193");
                }
                int w = 2 + 10 + 2 + textW + 2;
                itemWidths[i - startIdx] = w;
                rowWidth += w;
            }
            int itemSpacing = 2;
            rowWidth += (endIdx - startIdx - 1) * itemSpacing;

            int currentX = -rowWidth / 2;
            int currentY = (numRows - 1 - r) * rowHeight;

            for (int i = startIdx; i < endIdx; i++) {
                int itemW = itemWidths[i - startIdx];
                ClientMarkInstance instance = list.get(i);
                MarkVisualData visual = visuals.get(i);
                String text = texts.get(i);

                GL11.glDisable(GL11.GL_TEXTURE_2D);
                int boxLeft = currentX;
                int boxRight = currentX + itemW;
                int boxTop = currentY - 2;
                int boxBottom = currentY + 12;

                int bgColor = 0xFF222222;
                int borderColor = 0xFFAAAAAA;

                net.minecraft.client.gui.Gui.drawRect(boxLeft, boxTop, boxRight, boxBottom, bgColor);
                net.minecraft.client.gui.Gui.drawRect(boxLeft, boxTop, boxRight, boxTop + 1, borderColor);
                net.minecraft.client.gui.Gui.drawRect(boxLeft, boxTop, boxLeft + 1, boxBottom, borderColor);
                net.minecraft.client.gui.Gui.drawRect(boxLeft, boxBottom - 1, boxRight, boxBottom, borderColor);
                net.minecraft.client.gui.Gui.drawRect(boxRight - 1, boxTop, boxRight, boxBottom, borderColor);
                GL11.glEnable(GL11.GL_TEXTURE_2D);

                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.0F, -0.01F);

                boolean drawContent = true;
                if (instance.decaying) {
                    if ((mc.theWorld.getTotalWorldTime() / 10) % 2 == 0) {
                        drawContent = false;
                    }
                } else if (visual.flashWhenNearDecay && !instance.maxed) {
                    long remaining = instance.stableUntilWorldTime - mc.theWorld.getTotalWorldTime();
                    if (remaining > 0 && remaining < 40) {
                        if ((remaining / 4) % 2 == 0) {
                            drawContent = false;
                        }
                    }
                }

                if (drawContent) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.getTextureManager()
                        .bindTexture(visual.icon);
                    tessellator.startDrawingQuads();
                    tessellator.addVertexWithUV(currentX + 2, currentY + 10, 0, 0.0, 1.0);
                    tessellator.addVertexWithUV(currentX + 12, currentY + 10, 0, 1.0, 1.0);
                    tessellator.addVertexWithUV(currentX + 12, currentY, 0, 1.0, 0.0);
                    tessellator.addVertexWithUV(currentX + 2, currentY, 0, 0.0, 0.0);
                    tessellator.draw();

                    int color = visual.primaryColor;
                    fontRenderer.drawString(text, currentX + 14, currentY + 1, color);
                    if (instance.decaying) {
                        int textW = fontRenderer.getStringWidth(text);
                        fontRenderer.drawString("\u2193", currentX + 14 + textW + 2, currentY + 1 + 1, color);
                    }
                }

                GL11.glPopMatrix();

                currentX += itemW + itemSpacing;
            }
        }

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
}
