package com.greyhat.dark_grey.client.event;

import net.minecraft.client.entity.EntityClientPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientInputEventHandler {

    public static final ClientInputEventHandler INSTANCE = new ClientInputEventHandler();

    private boolean wasAttackKeyDown = false;

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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == Side.CLIENT) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.currentScreen == null) {
                boolean isKeyDown = mc.gameSettings.keyBindAttack.getIsKeyPressed();
                if (isKeyDown && !wasAttackKeyDown) {
                    net.minecraft.item.ItemStack held = mc.thePlayer.getCurrentEquippedItem();
                    if (held != null && held.getItem() instanceof com.greyhat.dark_grey.api.IRPGItemContainer) {
                        if ("slag_eruptor"
                            .equals(((com.greyhat.dark_grey.api.IRPGItemContainer) held.getItem()).getRpgItemId())) {
                            com.greyhat.dark_grey.DarkGrey.NETWORK
                                .sendToServer(new com.greyhat.dark_grey.network.MessageStartAutomaticFire());
                        }
                    }
                } else if (!isKeyDown && wasAttackKeyDown) {
                    com.greyhat.dark_grey.DarkGrey.NETWORK
                        .sendToServer(new com.greyhat.dark_grey.network.MessageStopAutomaticFire());
                }
                wasAttackKeyDown = isKeyDown;
            }
        }
    }
}
