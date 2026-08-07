package com.greyhat.dark_grey.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.greyhat.dark_grey.api.GunMagazineHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.component.ComponentSlagEruptor;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class AutomaticFireManager {

    private static final Map<UUID, FireState> states = new HashMap<UUID, FireState>();
    private static final AutomaticFireManager INSTANCE = new AutomaticFireManager();

    private static class FireState {

        ItemStack weaponStack;
        int ticksSinceLastFire = 0;

        public FireState(ItemStack stack) {
            this.weaponStack = stack;
            // The first shot can be fired immediately
            this.ticksSinceLastFire = 4;
        }
    }

    public static void startFire(EntityPlayer player, ItemStack stack) {
        if (!player.worldObj.isRemote) {
            FireState state = new FireState(stack);
            states.put(player.getUniqueID(), state);
            tryFire(player, state);
        }
    }

    public static void stopFire(EntityPlayer player) {
        if (!player.worldObj.isRemote) {
            states.remove(player.getUniqueID());
        }
    }

    public static void register() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;

        EntityPlayer player = event.player;
        FireState state = states.get(player.getUniqueID());
        if (state != null) {
            ItemStack held = player.getCurrentEquippedItem();
            if (held == null || held.getItem() != state.weaponStack.getItem() || !player.isEntityAlive()) {
                states.remove(player.getUniqueID());
                return;
            }

            state.ticksSinceLastFire++;

            if (state.ticksSinceLastFire >= 4) {
                if (!tryFire(player, state)) {
                    states.remove(player.getUniqueID());
                }
            }
        }
    }

    private static boolean tryFire(EntityPlayer player, FireState state) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !(held.getItem() instanceof IRPGItemContainer)) return false;

        ComponentSlagEruptor eruptor = null;
        for (IRPGComponent comp : ((IRPGItemContainer) held.getItem()).getAllComponents()) {
            if (comp instanceof ComponentSlagEruptor) {
                eruptor = (ComponentSlagEruptor) comp;
                break;
            }
        }

        if (eruptor == null) return false;

        if (player.getItemInUse() != null && player.getItemInUse()
            .getItem() == held.getItem()) {
            return false;
        }

        int ammo = GunMagazineHelper.getLoadedAmmo(held);
        if (ammo <= 0) {
            // Empty click sound
            player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.5F);
            return false;
        }

        // Fire
        eruptor.fireOneShot(held, player.worldObj, player);
        state.ticksSinceLastFire = 0;
        return true;
    }
}
