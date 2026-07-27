package com.greyhat.dark_grey.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.api.RedSunFireballManager;
import com.greyhat.dark_grey.status.RedSunBurnData;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class RedSunBurnHandler {

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityLivingBase) {
            if (RedSunBurnData.get((EntityLivingBase) event.entity) == null) {
                RedSunBurnData.register((EntityLivingBase) event.entity);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;

        EntityLivingBase target = event.entityLiving;
        if (target.worldObj.isRemote) return;

        RedSunBurnData data = RedSunBurnData.get(target);
        if (data != null && data.isActive(target.worldObj)) {
            if (!RPGDamageSources.isSwitchDamage(event.source)) {
                event.ammount *= data.getIncomingDamageMultiplier();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;

        RedSunBurnData data = RedSunBurnData.get(player);
        if (data == null || !data.isActive(player.worldObj)) {
            return;
        }

        long time = player.worldObj.getTotalWorldTime();
        if (time % 10 == 0 && player.worldObj instanceof WorldServer) {
            ((WorldServer) player.worldObj).func_147487_a(
                "flame",
                player.posX,
                player.posY + player.height / 2.0,
                player.posZ,
                2,
                0.3,
                0.5,
                0.3,
                0.01);
        }

        int currentSlot = player.inventory.currentItem;
        if (currentSlot != data.getLastSelectedHotbarSlot()) {
            int previousSlot = data.getLastSelectedHotbarSlot();
            data.setLastSelectedHotbarSlot(currentSlot);

            if (previousSlot != -1 && data.getLastSwitchDamageTick() != time) {
                data.setLastSwitchDamageTick(time);

                EntityPlayer sourcePlayer = null;
                if (data.getSourceUuid() != null) {
                    sourcePlayer = player.worldObj.func_152378_a(data.getSourceUuid());
                }

                DamageSource ds = RPGDamageSources.causeRedSunBurnSwitchDamage(sourcePlayer);
                boolean damaged;
                if (data.isIgnoringSwitchDamageHurtResistance()) {
                    damaged = RPGDamageSources.dealDamageWithoutInvulnerability(player, ds, data.getSwitchDamage());
                } else {
                    damaged = player.attackEntityFrom(ds, data.getSwitchDamage());
                }
                if (!damaged) {
                    return;
                }

                player.worldObj.playSoundAtEntity(player, "random.fizz", 1.0F, 1.0F);
                if (player.worldObj instanceof WorldServer) {
                    ((WorldServer) player.worldObj).func_147487_a(
                        "lava",
                        player.posX,
                        player.posY + player.height / 2.0,
                        player.posZ,
                        5,
                        0.3,
                        0.5,
                        0.3,
                        0.0);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityPlayer) {
            RedSunFireballManager.removeChargingFireball((EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.player.worldObj.isRemote) {
            RedSunFireballManager.removeChargingFireball(event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.player.worldObj.isRemote) {
            RedSunFireballManager.removeChargingFireball(event.player);
        }
    }
}
