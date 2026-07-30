package com.greyhat.dark_grey.client.event;

import net.minecraft.client.entity.EntityClientPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientInputEventHandler {

    public static final ClientInputEventHandler INSTANCE = new ClientInputEventHandler();

    private ClientInputEventHandler() {}

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.side == Side.CLIENT) {
            if (event.player instanceof EntityClientPlayerMP) {
                EntityClientPlayerMP clientPlayer = (EntityClientPlayerMP) event.player;
                if (com.greyhat.dark_grey.client.render.ShatteredBoneCircleRenderer.INSTANCE
                    .hasActiveCast(clientPlayer.getEntityId())) {
                    if (clientPlayer.movementInput != null) {
                        clientPlayer.movementInput.moveForward = 0.0f;
                        clientPlayer.movementInput.moveStrafe = 0.0f;
                        clientPlayer.movementInput.jump = false;
                        clientPlayer.movementInput.sneak = false;
                    }
                }
            }
        }
    }
}
