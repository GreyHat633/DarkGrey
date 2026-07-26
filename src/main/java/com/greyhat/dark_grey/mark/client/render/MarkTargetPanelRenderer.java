package com.greyhat.dark_grey.mark.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

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
public class MarkTargetPanelRenderer {

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!com.greyhat.dark_grey.common.Config.enableTargetMarkPanel) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.pointedEntity instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) mc.pointedEntity;
            if (target.isDead) return;

            ClientEntityMarks marks = ClientMarkCache.get(target.getEntityId());
            if (marks == null || marks.isEmpty()) return;

            List<ClientMarkInstance> list = new ArrayList<>();
            for (ClientMarkInstance instance : marks.getAll()) {
                IMarkType type = MarkRegistry.get(instance.markId);
                if (type != null && type.getVisualData() != null && type.getVisualData().showOnTargetPanel) {
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

            ScaledResolution res = event.resolution;
            int x = res.getScaledWidth() / 2 + 15;
            int y = res.getScaledHeight() / 2 + 15;

            FontRenderer fr = mc.fontRenderer;
            long now = mc.theWorld.getTotalWorldTime();

            int startY = y;
            for (ClientMarkInstance instance : list) {
                IMarkType type = MarkRegistry.get(instance.markId);
                MarkVisualData visual = type.getVisualData();
                String name = StatCollector.translateToLocal(visual.displayNameKey);

                String stateStr;
                if (instance.markId.equals("shattered_bone")) {
                    boolean maintainedByFracture = instance.customData != null
                        && instance.customData.getBoolean("MaintainedByFracture");
                    boolean hasIndependentDuration = instance.customData != null
                        && instance.customData.getBoolean("HasIndependentDuration");
                    if (maintainedByFracture) {
                        stateStr = "维持: \u221E";
                    } else if (hasIndependentDuration) {
                        long expire = instance.customData.getLong("IndependentExpireWorldTime");
                        double remaining = (expire - now) / 20.0;
                        stateStr = "剩余: " + String.format("%.1f", Math.max(0, remaining)) + "s";
                    } else {
                        stateStr = "消失中";
                    }
                } else if (instance.decaying) {
                    double nextDecay = (instance.nextDecayTriggerWorldTime - now) / 20.0;
                    stateStr = "衰减中 (" + String.format("%.1f", Math.max(0, nextDecay)) + "s)";
                } else {
                    double stable = (instance.stableUntilWorldTime - now) / 20.0;
                    stateStr = "稳定: " + String.format("%.1f", Math.max(0, stable)) + "s";
                }

                String extraStr = "";
                if ("poison".equals(instance.markId)) {
                    extraStr = " | 预计伤害: " + instance.stacks;
                } else if ("fracture".equals(instance.markId)) {
                    extraStr = " | 减速: " + (instance.stacks * 10) + "%";
                }

                String line = name + " " + instance.stacks + "层 \u00A77| " + stateStr + extraStr;

                int color = instance.maxed ? visual.maxColor
                    : (instance.decaying ? visual.decayColor : visual.primaryColor);

                // Draw Icon
                org.lwjgl.opengl.GL11.glPushMatrix();
                org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
                org.lwjgl.opengl.GL11
                    .glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.getTextureManager()
                    .bindTexture(visual.icon);

                net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(x, startY + 12, 0, 0.0, 1.0);
                tessellator.addVertexWithUV(x + 12, startY + 12, 0, 1.0, 1.0);
                tessellator.addVertexWithUV(x + 12, startY, 0, 1.0, 0.0);
                tessellator.addVertexWithUV(x, startY, 0, 0.0, 0.0);
                tessellator.draw();
                org.lwjgl.opengl.GL11.glPopMatrix();

                // Draw Text
                fr.drawStringWithShadow(line, x + 16, startY + 2, color);

                startY += 16;
            }
        }
    }
}
