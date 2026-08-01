package com.greyhat.dark_grey.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.greyhat.dark_grey.api.RPGDamageSources;
import com.greyhat.dark_grey.common.Config;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class RedSunBurnTracker {

    private static final WeakHashMap<EntityLivingBase, BurnData> BURNING_ENTITIES = new WeakHashMap<>();
    private static final WeakHashMap<EntityPlayer, Integer> LAST_SLOTS = new WeakHashMap<>();
    private static final WeakHashMap<EntityPlayer, Long> LAST_SWITCH_TICKS = new WeakHashMap<>();

    private static class BurnData {

        int timer;
        EntityLivingBase source;

        BurnData(int timer, EntityLivingBase source) {
            this.timer = timer;
            this.source = source;
        }
    }

    public static synchronized void mark(EntityLivingBase entity, EntityLivingBase source, int duration) {
        BURNING_ENTITIES.put(entity, new BurnData(duration, source));
    }

    public static synchronized boolean isBurning(EntityLivingBase entity) {
        BurnData data = BURNING_ENTITIES.get(entity);
        return data != null && data.timer > 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;

        EntityLivingBase target = event.entityLiving;
        if (target.worldObj.isRemote) return;

        if (isBurning(target)) {
            if (!RPGDamageSources.isSwitchDamage(event.source)) {
                event.ammount *= Config.scorchIncomingDamageMultiplier;
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        synchronized (RedSunBurnTracker.class) {
            if (BURNING_ENTITIES.isEmpty()) return;

            List<EntityLivingBase> toRemove = new ArrayList<>();
            for (Map.Entry<EntityLivingBase, BurnData> entry : BURNING_ENTITIES.entrySet()) {
                EntityLivingBase entity = entry.getKey();
                if (entity == null || entity.isDead) {
                    toRemove.add(entity);
                    continue;
                }

                BurnData data = entry.getValue();
                if (data.timer > 0) {
                    data.timer--;

                    // Periodic flame particles
                    if (data.timer % 10 == 0 && entity.worldObj instanceof WorldServer) {
                        ((WorldServer) entity.worldObj).func_147487_a(
                            "flame",
                            entity.posX,
                            entity.posY + entity.height / 2.0,
                            entity.posZ,
                            2,
                            0.3,
                            0.5,
                            0.3,
                            0.01);
                    }
                } else {
                    toRemove.add(entity);
                }
            }

            for (EntityLivingBase entity : toRemove) {
                BURNING_ENTITIES.remove(entity);
                if (entity instanceof EntityPlayer) {
                    LAST_SLOTS.remove(entity);
                    LAST_SWITCH_TICKS.remove(entity);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;

        if (!isBurning(player)) return;

        BurnData data = BURNING_ENTITIES.get(player);
        if (data == null || data.timer <= 0) return;

        int currentSlot = player.inventory.currentItem;
        long time = player.worldObj.getTotalWorldTime();

        if (!LAST_SLOTS.containsKey(player)) {
            LAST_SLOTS.put(player, currentSlot);
        } else {
            int previousSlot = LAST_SLOTS.get(player);
            if (currentSlot != previousSlot) {
                LAST_SLOTS.put(player, currentSlot);

                long lastSwitchTime = LAST_SWITCH_TICKS.getOrDefault(player, 0L);
                if (lastSwitchTime != time) {
                    LAST_SWITCH_TICKS.put(player, time);

                    EntityPlayer sourcePlayer = data.source instanceof EntityPlayer ? (EntityPlayer) data.source : null;
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
