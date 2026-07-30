package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.item.ItemRPGGun;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class GunHudRenderer {

    public static final GunHudRenderer INSTANCE = new GunHudRenderer();

    private GunHudRenderer() {}

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        ItemStack heldStack = player.getCurrentEquippedItem();
        if (heldStack == null || !(heldStack.getItem() instanceof ItemRPGGun)) {
            return;
        }

        NBTTagCompound nbt = heldStack.getTagCompound();
        boolean loaded = (nbt != null && nbt.getBoolean("DarkGreyBoneCannonLoaded"));

        ItemStack usingItem = player.getItemInUse();
        boolean reloading = (usingItem != null && usingItem.getItem() instanceof ItemRPGGun);

        if (!loaded && !reloading) {
            return; // Nothing to show
        }

        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        FontRenderer fr = mc.fontRenderer;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int yBase = height / 2 + 15;

        if (loaded) {
            String text = "[\u5DF2\u88C5\u586B]"; // [已装填]
            int textW = fr.getStringWidth(text);
            fr.drawStringWithShadow(text, width / 2 - textW / 2, yBase, 0x00FF00); // Green
        } else if (reloading) {
            int maxUse = usingItem.getMaxItemUseDuration();
            int inUse = player.getItemInUseDuration(); // This goes UP from 0

            // Assume 50 ticks to reload (since ComponentBoneCannon has 50)
            // It would be better to dynamically fetch, but we'll approximate.
            float progress = Math.min(1.0f, (float) inUse / 50.0f);

            String text = "\u88C5\u586B\u4E2D..."; // 装填中...
            int textW = fr.getStringWidth(text);
            fr.drawStringWithShadow(text, width / 2 - textW / 2, yBase, 0xFFCC00); // Orange/Yellow

            // Draw progress bar
            int barWidth = 40;
            int barHeight = 4;
            int barX = width / 2 - barWidth / 2;
            int barY = yBase + 10;

            net.minecraft.client.gui.Gui
                .drawRect(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0x88000000);
            net.minecraft.client.gui.Gui
                .drawRect(barX, barY, barX + (int) (barWidth * progress), barY + barHeight, 0xFFFFFF00);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
