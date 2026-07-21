package com.greyhat.dark_grey.event;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.component.ComponentItanis;
import com.greyhat.dark_grey.network.C2SToggleItanisMode;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ItanisClientEventHandler {

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (event.button == 0 && event.buttonstate) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                ItemStack held = mc.thePlayer.getHeldItem();
                if (held != null && held.getItem() instanceof IRPGItemContainer) {
                    IRPGItemContainer container = (IRPGItemContainer) held.getItem();
                    if (container.getAllComponents()
                        .stream()
                        .anyMatch(c -> c instanceof ComponentItanis)) {
                        DarkGrey.NETWORK.sendToServer(new C2SToggleItanisMode());
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
