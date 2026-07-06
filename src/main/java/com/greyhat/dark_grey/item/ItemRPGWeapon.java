package com.greyhat.dark_grey.item;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.RPGItemStackSync;
import com.greyhat.dark_grey.api.capability.IAttributeModifier;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHit;
import com.greyhat.dark_grey.api.capability.IOnPlayerDeath;
import com.greyhat.dark_grey.api.capability.IOnRightClick;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Universal RPG weapon container.
 */
public class ItemRPGWeapon extends ItemSword implements IRPGItemContainer {

    private List<IRPGComponent> allComponents;
    private List<IOnHit> hitHandlers;
    private List<IOnRightClick> rightClickHandlers;
    private List<IHasTooltip> tooltipHandlers;
    private List<IAttributeModifier> attributeHandlers;
    private List<IOnPlayerDeath> playerDeathHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnHeldTick> heldTickHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick> weaponUsingTickHandlers;
    private List<com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing> playerStoppedUsingHandlers;

    private final String rpgItemId;

    // P1 #7: 将 UUID 提取为静态常量，并引入 Multimap 缓存以避免每 tick 的 GC 开销
    private static final UUID ITEM_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private Multimap cachedModifiers = null;
    private int cachedModifiersVersion = -1;

    public ItemRPGWeapon(String id, ToolMaterial material, List<IRPGComponent> components) {
        super(material);
        this.rpgItemId = id;
        this.allComponents = Collections.unmodifiableList(components);
        this.hitHandlers = IRPGComponent.filterByCapability(components, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(components, IOnRightClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(components, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(components, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(components, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent
            .filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.weaponUsingTickHandlers = IRPGComponent
            .filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick.class);
        this.playerStoppedUsingHandlers = IRPGComponent
            .filterByCapability(components, com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing.class);
    }

    @Override
    public boolean hitEntity(ItemStack weaponStack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(weaponStack, target, attacker);

        if (!attacker.worldObj.isRemote) {
            float rawDamage = 1.0F;
            if (attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                rawDamage = (float) attacker.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                    .getAttributeValue();
            }

            for (IOnHit handler : hitHandlers) {
                handler.onHit(weaponStack, attacker, target, rawDamage);
            }
        }
        return result;
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
    public String getRpgItemId() {
        return rpgItemId;
    }

    @Override
    public void rebuildComponents() {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config == null || config.componentsJson == null) return;

        List<IRPGComponent> newComponents = new java.util.ArrayList<>();
        for (com.google.gson.JsonElement compElem : config.componentsJson) {
            com.google.gson.JsonObject compObj = compElem.getAsJsonObject();
            String compName = compObj.get("name")
                .getAsString();
            com.google.gson.JsonObject params = compObj.has("params") ? compObj.getAsJsonObject("params")
                : new com.google.gson.JsonObject();
            try {
                newComponents.add(com.greyhat.dark_grey.api.ComponentRegistry.create(compName, params));
            } catch (Exception e) {}
        }

        this.allComponents = Collections.unmodifiableList(newComponents);
        this.hitHandlers = IRPGComponent.filterByCapability(newComponents, IOnHit.class);
        this.rightClickHandlers = IRPGComponent.filterByCapability(newComponents, IOnRightClick.class);
        this.tooltipHandlers = IRPGComponent.filterByCapability(newComponents, IHasTooltip.class);
        this.attributeHandlers = IRPGComponent.filterByCapability(newComponents, IAttributeModifier.class);
        this.playerDeathHandlers = IRPGComponent.filterByCapability(newComponents, IOnPlayerDeath.class);
        this.heldTickHandlers = IRPGComponent
            .filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnHeldTick.class);
        this.weaponUsingTickHandlers = IRPGComponent
            .filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick.class);
        this.playerStoppedUsingHandlers = IRPGComponent
            .filterByCapability(newComponents, com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing.class);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (weaponUsingTickHandlers != null) {
            for (com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick handler : weaponUsingTickHandlers) {
                handler.onUsingTick(stack, player, count);
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        if (playerStoppedUsingHandlers != null) {
            for (com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing handler : playerStoppedUsingHandlers) {
                handler.onPlayerStoppedUsing(stack, world, player, timeLeft);
            }
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        player.setItemInUse(weaponStack, this.getMaxItemUseDuration(weaponStack));

        ItemStack resultStack = weaponStack;
        for (IOnRightClick handler : rightClickHandlers) {
            resultStack = handler.onRightClick(resultStack, world, player);
        }
        return resultStack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack weaponStack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        super.addInformation(weaponStack, player, tooltipLines, showAdvanced);
        for (IHasTooltip handler : tooltipHandlers) {
            handler.addTooltipLines(weaponStack, player, tooltipLines, showAdvanced);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Multimap getItemAttributeModifiers() {
        int currentVersion = RPGItemDataManager.getInstance()
            .getDataVersion();
        if (this.cachedModifiers == null || this.cachedModifiersVersion != currentVersion) {
            Multimap attributeMap = HashMultimap.create();
            RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
                .getConfig(rpgItemId);
            float damage = 0.0f;
            if (config != null) {
                damage = config.damage;
            }
            attributeMap.put(
                SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
                new AttributeModifier(ITEM_DAMAGE_UUID, "Weapon modifier", (double) damage, 0));
            for (IAttributeModifier handler : attributeHandlers) {
                handler.modifyAttributes(attributeMap);
            }
            this.cachedModifiers = attributeMap;
            this.cachedModifiersVersion = currentVersion;
        }
        return this.cachedModifiers;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig(rpgItemId);
        if (config != null && config.durability > 0) {
            return config.durability;
        }
        return super.getMaxDamage(stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int itemSlot,
        boolean isSelected) {
        if (isSelected && entity instanceof EntityPlayer) {
            for (com.greyhat.dark_grey.api.capability.IOnHeldTick handler : heldTickHandlers) {
                handler.onHeldTick(stack, world, (EntityPlayer) entity);
            }
        }

        // P1 #6 / P2 #13: 委托给同步工具类，内部重排了快路径避免每次 getConfig 同步锁竞争
        RPGItemStackSync.syncIfVersionChanged(stack, rpgItemId, world);
    }

    public List<IRPGComponent> getAllComponents() {
        return allComponents;
    }

    public List<IOnHit> getHitHandlers() {
        return hitHandlers;
    }

    @Override
    public List<IOnPlayerDeath> getPlayerDeathHandlers() {
        return playerDeathHandlers;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(net.minecraft.item.Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item, 1, 0);
        RPGItemStackSync.forceSync(stack, rpgItemId);
        list.add(stack);
    }
}
