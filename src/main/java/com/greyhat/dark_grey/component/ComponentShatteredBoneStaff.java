package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.combat.ShatteredBoneStaffCastManager;
import com.greyhat.dark_grey.combat.ShatteredBoneStaffCastState;
import com.greyhat.dark_grey.util.WeaponAttackPowerResolver;

public class ComponentShatteredBoneStaff
    implements IRPGComponent, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IHasTooltip {

    private int chargeTicks;
    private int maxCastTicks;
    private float circleRadius;
    private int pulseIntervalTicks;
    private String fractureMarkId;
    private int hitsPerFracture;
    private int fractureStacksPerTrigger;
    private int slownessAmplifier;
    private int slownessRefreshTicks;
    private String materialItemId;
    private int materialCost;
    private boolean consumeInCreative;
    private boolean requireMaterialInCreative;
    private boolean lockCasterPosition;
    private double teleportCancelThresholdSq;

    @Override
    public String getComponentId() {
        return "碎骨权杖";
    }

    @Override
    public void configure(JsonObject json) {
        chargeTicks = json.has("chargeTicks") ? json.get("chargeTicks")
            .getAsInt() : 60;
        maxCastTicks = json.has("maxCastTicks") ? json.get("maxCastTicks")
            .getAsInt() : 160;
        circleRadius = json.has("circleRadius") ? json.get("circleRadius")
            .getAsFloat() : 4.0f;
        pulseIntervalTicks = json.has("pulseIntervalTicks") ? json.get("pulseIntervalTicks")
            .getAsInt() : 10;
        fractureMarkId = json.has("fractureMarkId") ? json.get("fractureMarkId")
            .getAsString() : "fracture";
        hitsPerFracture = json.has("hitsPerFracture") ? json.get("hitsPerFracture")
            .getAsInt() : 2;
        fractureStacksPerTrigger = json.has("fractureStacksPerTrigger") ? json.get("fractureStacksPerTrigger")
            .getAsInt() : 1;
        slownessAmplifier = json.has("slownessAmplifier") ? json.get("slownessAmplifier")
            .getAsInt() : 1;
        slownessRefreshTicks = json.has("slownessRefreshTicks") ? json.get("slownessRefreshTicks")
            .getAsInt() : 15;
        materialItemId = json.has("materialItemId") ? json.get("materialItemId")
            .getAsString() : "dark_grey:hardened_bone_marrow";
        if (!materialItemId.contains(":")) {
            materialItemId = "dark_grey:" + materialItemId;
        }
        materialCost = json.has("materialCost") ? json.get("materialCost")
            .getAsInt() : 1;
        consumeInCreative = json.has("consumeInCreative") ? json.get("consumeInCreative")
            .getAsBoolean() : false;
        requireMaterialInCreative = json.has("requireMaterialInCreative") ? json.get("requireMaterialInCreative")
            .getAsBoolean() : false;
        lockCasterPosition = json.has("lockCasterPosition") ? json.get("lockCasterPosition")
            .getAsBoolean() : true;
        double tpThresh = json.has("teleportCancelThreshold") ? json.get("teleportCancelThreshold")
            .getAsDouble() : 4.0;
        teleportCancelThresholdSq = tpThresh * tpThresh;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        int itemInUseDuration = stack.getMaxItemUseDuration() - count;

        if (player.worldObj.isRemote) {
            // Client side: spawn charging particles
            if (itemInUseDuration < chargeTicks) {
                // Spawn particles gathering towards the player to indicate charging
                double px = player.posX + (player.worldObj.rand.nextDouble() - 0.5) * 2.0;
                double py = player.posY - player.yOffset + player.worldObj.rand.nextDouble() * 2.0;
                double pz = player.posZ + (player.worldObj.rand.nextDouble() - 0.5) * 2.0;
                
                // Use bone particles or magic particles
                String particleName = player.worldObj.rand.nextBoolean() ? "iconcrack_352" : "witchMagic";
                player.worldObj.spawnParticle(particleName, px, py, pz, 0, 0.1, 0);
            }
            return;
        }

        // Server side logic below
        // Start cast at chargeTicks
        if (itemInUseDuration == chargeTicks) {
            boolean isCreative = (player instanceof EntityPlayer)
                && ((EntityPlayer) player).capabilities.isCreativeMode;

            // No onGround check here anymore. We allow casting in the air.
            // (The code below will handle pulling them to the ground)

            if (!isCreative || requireMaterialInCreative) {
                if (!consumeMaterial((EntityPlayer) player)) {
                    if (player instanceof EntityPlayer) {
                        ((EntityPlayer) player).addChatMessage(
                            new ChatComponentTranslation("message.shattered_bone_staff.no_material")
                                .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                        ((EntityPlayer) player).clearItemInUse();
                    }
                    return;
                }
            }

            float damage = (float) WeaponAttackPowerResolver.getBaseAttackPower(player);
            ShatteredBoneStaffCastState state = new ShatteredBoneStaffCastState(
                player,
                circleRadius,
                damage,
                maxCastTicks,
                pulseIntervalTicks,
                fractureMarkId,
                hitsPerFracture,
                fractureStacksPerTrigger,
                slownessAmplifier,
                slownessRefreshTicks,
                teleportCancelThresholdSq,
                lockCasterPosition);

            ShatteredBoneStaffCastManager.INSTANCE.startCast(player, state);
        }

        // Ensure player doesn't hold item longer than cast time + charge time
        if (itemInUseDuration >= chargeTicks + maxCastTicks) {
            if (player instanceof EntityPlayer) {
                ((EntityPlayer) player).clearItemInUse();
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
        if (!world.isRemote) {
            if (ShatteredBoneStaffCastManager.INSTANCE.isCasting(player)) {
                ShatteredBoneStaffCastManager.INSTANCE.endCast(player);
                player.addChatComponentMessage(
                    new ChatComponentTranslation("message.shattered_bone_staff.cast_cancelled")
                        .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)));
            }
        }
    }

    private boolean consumeMaterial(EntityPlayer player) {
        String[] parts = materialItemId.split(":");
        if (parts.length != 2) return false;

        // Search inventory
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack invStack = player.inventory.mainInventory[i];
            if (invStack != null && invStack.getItem() != null) {
                String regName = net.minecraft.item.Item.itemRegistry.getNameForObject(invStack.getItem());
                if (regName != null && regName.equals(materialItemId)) {
                    if (invStack.stackSize >= materialCost) {
                        if (!player.capabilities.isCreativeMode || consumeInCreative) {
                            player.inventory.decrStackSize(i, materialCost);
                            player.inventoryContainer.detectAndSendChanges();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        float damage = 4.0f; // Default damage from config
        tooltipLines.add(
            EnumChatFormatting.GOLD + net.minecraft.util.StatCollector
                .translateToLocalFormatted("tooltip.shattered_bone_staff.damage", (int) damage));
        tooltipLines.add(
            EnumChatFormatting.YELLOW + net.minecraft.util.StatCollector
                .translateToLocalFormatted("tooltip.shattered_bone_staff.charge", chargeTicks / 20.0f));
        tooltipLines.add(
            EnumChatFormatting.YELLOW + net.minecraft.util.StatCollector
                .translateToLocalFormatted("tooltip.shattered_bone_staff.cost", materialCost));
        tooltipLines.add("");
        tooltipLines.add(
            EnumChatFormatting.GREEN + net.minecraft.util.StatCollector
                .translateToLocalFormatted("tooltip.shattered_bone_staff.circle", circleRadius));
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  "
                + net.minecraft.util.StatCollector
                    .translateToLocalFormatted("tooltip.shattered_bone_staff.duration", maxCastTicks / 20.0f));
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  "
                + net.minecraft.util.StatCollector.translateToLocalFormatted("tooltip.shattered_bone_staff.root"));
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  "
                + net.minecraft.util.StatCollector.translateToLocalFormatted("tooltip.shattered_bone_staff.slow"));
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  "
                + net.minecraft.util.StatCollector
                    .translateToLocalFormatted("tooltip.shattered_bone_staff.damage_tick", pulseIntervalTicks / 20.0f));
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  "
                + net.minecraft.util.StatCollector.translateToLocalFormatted(
                    "tooltip.shattered_bone_staff.fracture",
                    hitsPerFracture,
                    fractureStacksPerTrigger));
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  "
                + net.minecraft.util.StatCollector.translateToLocalFormatted("tooltip.shattered_bone_staff.release"));
    }
}
