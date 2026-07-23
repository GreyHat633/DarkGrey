package com.greyhat.dark_grey.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ItanisModeSwitchHandler implements IMessageHandler<ItanisModeSwitchMessage, IMessage> {

    @Override
    public IMessage onMessage(ItanisModeSwitchMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null || !player.isEntityAlive()) {
            return null;
        }

        ItemStack heldStack = player.getCurrentEquippedItem();
        if (heldStack == null || !(heldStack.getItem() instanceof IRPGItemContainer)) {
            return null;
        }

        IRPGItemContainer container = (IRPGItemContainer) heldStack.getItem();
        for (IRPGComponent component : container.getAllComponents()) {
            if (component instanceof IOnLeftClick) {
                ((IOnLeftClick) component).onLeftClick(heldStack, player);
            }
        }
        return null;
    }
}
