package com.greyhat.dark_grey.event;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.component.ComponentItanis;
import com.greyhat.dark_grey.component.ItanisMode;
import com.greyhat.dark_grey.component.ItanisNBT;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ItanisServerEventHandler {

    private static final ConcurrentMap<UUID, Boolean> TOGGLE_REQUESTS = new ConcurrentHashMap<>();

    public static void requestToggle(EntityPlayerMP player) {
        if (player != null) TOGGLE_REQUESTS.put(player.getUniqueID(), Boolean.TRUE);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        if (TOGGLE_REQUESTS.remove(event.player.getUniqueID()) == null) return;

        ItemStack stack = event.player.getHeldItem();
        if (!isItanis(stack)) return;

        ItanisNBT.toggleMode(stack);
        ItanisMode newMode = ItanisNBT.getMode(stack);
        String modeName = newMode == ItanisMode.RAPID ? "速射" : "蓄能";
        event.player.inventoryContainer.detectAndSendChanges();
        event.player.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GOLD + "已切换至: " + EnumChatFormatting.YELLOW + modeName + "模式"));
        event.player.worldObj
            .playSoundAtEntity(event.player, "random.click", 0.5F, newMode == ItanisMode.RAPID ? 1.0F : 0.8F);
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        if (event.entityPlayer != null) {
            ItemStack held = event.entityPlayer.getHeldItem();
            if (isItanis(held)) {
                // Cancel melee attack
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            if (event.entityPlayer != null) {
                ItemStack held = event.entityPlayer.getHeldItem();
                if (isItanis(held)) {
                    // Cancel block breaking
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean isItanis(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof IRPGItemContainer) {
            IRPGItemContainer container = (IRPGItemContainer) stack.getItem();
            return container.getAllComponents()
                .stream()
                .anyMatch(c -> c instanceof ComponentItanis);
        }
        return false;
    }
}
