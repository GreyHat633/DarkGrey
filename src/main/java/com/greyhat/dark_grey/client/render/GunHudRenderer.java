package com.greyhat.dark_grey.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.greyhat.dark_grey.api.GunMagazineHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.component.ComponentBoneCannon;
import com.greyhat.dark_grey.component.ComponentBoneSplasher;
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

        ItemRPGGun gun = (ItemRPGGun) heldStack.getItem();

        NBTTagCompound nbt = heldStack.getTagCompound();
        boolean legacyBoneCannonLoaded = (nbt != null && nbt.getBoolean("DarkGreyBoneCannonLoaded"));
        int loadedAmmo = GunMagazineHelper.getLoadedAmmo(heldStack);

        int capacity = 1;
        int reloadTicks = 50;
        boolean hasMagazine = false;

        for (IRPGComponent comp : gun.getAllComponents()) {
            if (comp instanceof ComponentBoneSplasher) {
                ComponentBoneSplasher splasher = (ComponentBoneSplasher) comp;
                capacity = splasher.getMagazineCapacity();
                reloadTicks = splasher.getReloadTicks();
                hasMagazine = true;
                break;
            } else if (comp instanceof ComponentBoneCannon) {
                ComponentBoneCannon cannon = (ComponentBoneCannon) comp;
                reloadTicks = cannon.getLoadTicksRequired();
                capacity = 1;
                // Legacy loaded state mapping
                if (legacyBoneCannonLoaded) loadedAmmo = 1;
                hasMagazine = true;
                break;
            }
        }

        ItemStack usingItem = player.getItemInUse();
        boolean reloading = (usingItem != null && usingItem.getItem() instanceof ItemRPGGun);

        if (loadedAmmo <= 0 && !reloading) {
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

        if (reloading && loadedAmmo < capacity) {
            int inUse = player.getItemInUseDuration(); // This goes UP from 0
            float progress = Math.min(1.0f, (float) inUse / (float) reloadTicks);

            String text = "\u88C5\u586B\u4E2D... (" + String.format("%.1f", inUse / 20.0f)
                + "/"
                + String.format("%.1f", reloadTicks / 20.0f)
                + "s)"; // 装填中...
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

            yBase += 20; // move down for ammo count
        } else if (loadedAmmo >= capacity) {
            String text = "[\u5DF2\u88C5\u586B]"; // [已装填]
            int textW = fr.getStringWidth(text);
            fr.drawStringWithShadow(text, width / 2 - textW / 2, yBase, 0x00FF00); // Green
            yBase += 12;
        }

        if (hasMagazine) {
            String ammoText = loadedAmmo + " / " + capacity;
            int ammoColor = loadedAmmo > 0 ? 0x00FF00 : 0xFF0000;
            int ammoW = fr.getStringWidth(ammoText);
            fr.drawStringWithShadow(ammoText, width / 2 - ammoW / 2, yBase, ammoColor);
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
