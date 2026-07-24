package com.greyhat.dark_grey.item;

import java.util.Collections;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.ComponentRegistry;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.RPGItemStackSync;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemRPGUsableItem extends Item implements IRPGItemContainer {

    private final String rpgItemId;

    private List<IRPGComponent> allComponents;
    private List<IOnRightClick> rightClickHandlers;
    private List<IOnWeaponUsingTick> usingTickHandlers;
    private List<IOnPlayerStoppedUsing> stoppedUsingHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;

    public ItemRPGUsableItem(String rpgItemId, List<IRPGComponent> components) {
        super();
        this.setMaxStackSize(1);
        this.rpgItemId = rpgItemId;
        this.allComponents = Collections.unmodifiableList(components);

        this.rightClickHandlers = IRPGComponent.filterByCapability(components, IOnRightClick.class);
        this.usingTickHandlers = IRPGComponent.filterByCapability(components, IOnWeaponUsingTick.class);
        this.stoppedUsingHandlers = IRPGComponent.filterByCapability(components, IOnPlayerStoppedUsing.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
    }

    @Override
    public String getRpgItemId() {
        return rpgItemId;
    }

    @Override
    public void rebuildComponents() {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config == null || config.componentsJson == null) return;

        List<IRPGComponent> newComponents = new java.util.ArrayList<>();
        for (JsonElement compElem : config.componentsJson) {
            JsonObject compObj = compElem.getAsJsonObject();
            String compName = compObj.get("name")
                .getAsString();
            JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params") : new JsonObject();
            try {
                newComponents.add(ComponentRegistry.create(compName, params));
            } catch (Exception e) {
                DarkGrey.LOG.error("Failed to rebuild component " + compName + " for item " + rpgItemId, e);
            }
        }

        this.allComponents = Collections.unmodifiableList(newComponents);
        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);
        this.usingTickHandlers = IRPGComponent.filterByCapability(newComponents, IOnWeaponUsingTick.class);
        this.stoppedUsingHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerStoppedUsing.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.none;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config != null && config.displayName != null && !config.displayName.isEmpty()) {
            return config.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        ItemStack resultStack = stack;
        for (IOnRightClick handler : rightClickHandlers) {
            resultStack = handler.onRightClick(resultStack, world, player);
        }
        return resultStack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        for (IOnWeaponUsingTick handler : usingTickHandlers) {
            handler.onUsingTick(stack, player, count);
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        for (IOnPlayerStoppedUsing handler : stoppedUsingHandlers) {
            handler.onPlayerStoppedUsing(stack, world, player, timeLeft);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(stack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(stack, player, tooltipLines, showAdvanced);
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        RPGItemStackSync.syncIfVersionChanged(stack, rpgItemId, world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item, 1, 0);
        RPGItemStackSync.forceSync(stack, rpgItemId);
        list.add(stack);
    }

    @Override
    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }
}
