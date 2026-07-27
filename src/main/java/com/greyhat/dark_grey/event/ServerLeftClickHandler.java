package com.greyhat.dark_grey.event;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Moves client-requested left-click component actions from the Netty thread to
 * the authoritative server player tick.
 */
public final class ServerLeftClickHandler {

    private static final ConcurrentMap<UUID, Boolean> REQUESTS = new ConcurrentHashMap<>();

    public static void request(EntityPlayerMP player) {
        if (player != null) {
            REQUESTS.put(player.getUniqueID(), Boolean.TRUE);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) {
            return;
        }
        if (REQUESTS.remove(event.player.getUniqueID()) == null || !event.player.isEntityAlive()) {
            return;
        }

        ItemStack heldStack = event.player.getCurrentEquippedItem();
        if (heldStack == null || !(heldStack.getItem() instanceof IRPGItemContainer)) {
            return;
        }

        IRPGItemContainer container = (IRPGItemContainer) heldStack.getItem();
        for (IRPGComponent component : container.getAllComponents()) {
            if (component instanceof IOnLeftClick) {
                ((IOnLeftClick) component).onLeftClick(heldStack, event.player);
            }
        }
    }
}
