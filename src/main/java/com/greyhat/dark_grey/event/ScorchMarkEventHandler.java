package com.greyhat.dark_grey.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.common.Config;
import com.greyhat.dark_grey.mark.MarkContainer;
import com.greyhat.dark_grey.mark.MarkInstance;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.type.ScorchMarkType;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ScorchMarkEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;

        EntityLivingBase target = event.entityLiving;
        if (target.worldObj.isRemote) return;

        if (MarkManager.has(target, ScorchMarkType.ID)) {
            if (!RPGDamageSources.isSwitchDamage(event.source)) {
                event.ammount *= Config.scorchIncomingDamageMultiplier;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;

        MarkContainer container = MarkContainer.get(player);
        if (container == null) return;

        MarkInstance scorch = container.getMark(ScorchMarkType.ID);
        if (scorch == null || scorch.getStacks() <= 0) return;

        long time = player.worldObj.getTotalWorldTime();

        // Periodic flame particles
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

        // Switch damage logic
        int currentSlot = player.inventory.currentItem;
        NBTTagCompound customData = scorch.getCustomData();

        if (!customData.hasKey("LastSlot")) {
            customData.setInteger("LastSlot", currentSlot);
        } else {
            int previousSlot = customData.getInteger("LastSlot");
            if (currentSlot != previousSlot) {
                customData.setInteger("LastSlot", currentSlot);

                long lastSwitchTime = customData.getLong("LastSwitchTick");
                if (lastSwitchTime != time) {
                    customData.setLong("LastSwitchTick", time);

                    EntityPlayer sourcePlayer = null;
                    if (scorch.getSourceUuid() != null) {
                        sourcePlayer = player.worldObj.func_152378_a(scorch.getSourceUuid());
                    }

                    DamageSource ds = RPGDamageSources.causeRedSunBurnSwitchDamage(sourcePlayer);
                    boolean damaged = RPGDamageSources
                        .dealDamageWithoutInvulnerability(player, ds, Config.scorchSwitchDamage);

                    if (damaged) {
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
        }
    }
}
