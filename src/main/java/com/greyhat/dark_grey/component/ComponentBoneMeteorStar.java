package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.combat.BoneMeteorCastManager;
import com.greyhat.dark_grey.combat.BoneMeteorCastState;
import com.greyhat.dark_grey.mark.type.FractureMarkType;

public class ComponentBoneMeteorStar
    implements IRPGComponent, IOnRightClick, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IHasTooltip {

    private String materialItemId = "dark_grey:hardened_bone_marrow";
    private int materialCost = 1;
    private int consumeIntervalTicks = 20;
    private int meteorsPerConsume = 2;
    private double summonHeight = 7.0;
    private double summonRadius = 12.0;
    private float meteorFallSpeed = 0.65F;
    private float meteorGravity = 0.04F;
    private int meteorLifetimeTicks = 200;
    private double impactBoxSize = 3.0;
    private float impactDamage = 10.0F;
    private String fractureMarkId = FractureMarkType.ID;
    private int fractureStacks = 1;
    private double castingMoveSpeedMultiplier = 0.40;
    private boolean consumeInCreative = false;
    private boolean requireMaterialInCreative = false;
    private boolean affectCaster = false;
    private boolean respectWalls = false;

    @Override
    public String getComponentId() {
        return "陨骨星";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("materialItemId")) {
            String configuredMaterial = params.get("materialItemId")
                .getAsString()
                .trim();
            if (!configuredMaterial.isEmpty()) {
                materialItemId = configuredMaterial.contains(":") ? configuredMaterial
                    : "dark_grey:" + configuredMaterial;
            }
        }
        if (params.has("materialCost")) materialCost = Math.max(
            1,
            Math.min(
                64,
                params.get("materialCost")
                    .getAsInt()));
        if (params.has("consumeIntervalTicks")) consumeIntervalTicks = params.get("consumeIntervalTicks")
            .getAsInt();
        if (params.has("meteorsPerConsume")) meteorsPerConsume = params.get("meteorsPerConsume")
            .getAsInt();
        if (params.has("summonHeight")) summonHeight = params.get("summonHeight")
            .getAsDouble();
        if (params.has("summonRadius")) summonRadius = params.get("summonRadius")
            .getAsDouble();
        if (params.has("meteorFallSpeed")) meteorFallSpeed = params.get("meteorFallSpeed")
            .getAsFloat();
        if (params.has("meteorGravity")) meteorGravity = params.get("meteorGravity")
            .getAsFloat();
        if (params.has("meteorLifetimeTicks")) meteorLifetimeTicks = params.get("meteorLifetimeTicks")
            .getAsInt();
        if (params.has("impactBoxSize")) impactBoxSize = params.get("impactBoxSize")
            .getAsDouble();
        if (params.has("impactDamage")) impactDamage = params.get("impactDamage")
            .getAsFloat();
        if (params.has("fractureMarkId")) fractureMarkId = params.get("fractureMarkId")
            .getAsString();
        if (params.has("fractureStacks")) fractureStacks = params.get("fractureStacks")
            .getAsInt();
        if (params.has("castingMoveSpeedMultiplier"))
            castingMoveSpeedMultiplier = params.get("castingMoveSpeedMultiplier")
                .getAsDouble();
        if (params.has("consumeInCreative")) consumeInCreative = params.get("consumeInCreative")
            .getAsBoolean();
        if (params.has("requireMaterialInCreative")) requireMaterialInCreative = params.get("requireMaterialInCreative")
            .getAsBoolean();
        if (params.has("affectCaster")) affectCaster = params.get("affectCaster")
            .getAsBoolean();
        if (params.has("respectWalls")) respectWalls = params.get("respectWalls")
            .getAsBoolean();
    }

    @Override
    public ItemStack onRightClick(ItemStack stack, net.minecraft.world.World world, EntityPlayer player) {
        if (world.isRemote) return stack;

        if (BoneMeteorCastManager.INSTANCE.isCasting(player)) {
            return stack;
        }

        boolean hasMaterial = true;
        boolean creative = player.capabilities.isCreativeMode;

        if (!creative || requireMaterialInCreative) {
            hasMaterial = getMaterialCount(player) >= materialCost;
        } else if (creative && !consumeInCreative) {
            hasMaterial = true;
        }

        if (!hasMaterial) {
            player.clearItemInUse();
            player.addChatComponentMessage(new net.minecraft.util.ChatComponentText("§c缺少施法材料：硬化骨髓"));
            return stack;
        }

        BoneMeteorCastState state = new BoneMeteorCastState(
            player,
            world.getTotalWorldTime(),
            consumeIntervalTicks,
            meteorsPerConsume,
            materialItemId,
            materialCost,
            summonHeight,
            summonRadius,
            meteorFallSpeed,
            meteorGravity,
            meteorLifetimeTicks,
            impactBoxSize,
            impactDamage,
            fractureMarkId,
            fractureStacks,
            castingMoveSpeedMultiplier,
            consumeInCreative,
            requireMaterialInCreative);

        player.setItemInUse(stack, stack.getMaxItemUseDuration());
        BoneMeteorCastManager.INSTANCE.startCast(player, state);

        return stack;
    }

    private void spawnMeteorBatch(EntityPlayer caster, BoneMeteorCastState state) {
        for (int i = 0; i < state.meteorsPerConsume; i++) {
            double theta = caster.worldObj.rand.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(caster.worldObj.rand.nextDouble()) * state.summonRadius;

            double spawnX = caster.posX + Math.cos(theta) * r;
            double spawnZ = caster.posZ + Math.sin(theta) * r;
            double spawnY = caster.posY + state.summonHeight;

            com.greyhat.dark_grey.entity.EntityBoneMeteor meteor = new com.greyhat.dark_grey.entity.EntityBoneMeteor(
                caster.worldObj,
                caster,
                state.meteorFallSpeed,
                state.meteorGravity,
                state.meteorLifetimeTicks,
                state.impactBoxSize,
                state.impactDamage,
                state.fractureMarkId,
                state.fractureStacks);
            meteor.setPosition(spawnX, spawnY, spawnZ);

            caster.worldObj.spawnEntityInWorld(meteor);
        }
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (player.worldObj.isRemote) {
            for (int i = 0; i < 2; i++) {
                double angle = player.worldObj.rand.nextDouble() * 2.0 * Math.PI;
                double r = 1.0 + player.worldObj.rand.nextDouble() * 0.5;
                double px = player.posX + Math.cos(angle) * r;
                double py = player.boundingBox.minY + player.worldObj.rand.nextDouble() * 2.0;
                double pz = player.posZ + Math.sin(angle) * r;

                player.worldObj.spawnParticle(
                    "portal",
                    px,
                    py,
                    pz,
                    (player.posX - px) * 0.1,
                    (player.boundingBox.minY + 1.0 - py) * 0.1,
                    (player.posZ - pz) * 0.1);
                if (player.worldObj.rand.nextBoolean()) {
                    player.worldObj.spawnParticle("enchantmenttable", px, py, pz, 0, 0.05, 0);
                }
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, net.minecraft.world.World world, EntityPlayer player,
        int timeLeft) {
        if (!world.isRemote) {
            BoneMeteorCastManager.INSTANCE.endCast(player);
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add("§b持续施法：召唤骨陨石轰击战场");
        tooltip.add("");
        tooltip.add("§e长按右键：持续召唤骨陨石");
        tooltip.add("");
        tooltip.add("§a陨骨召唤：");
        tooltip.add(String.format("§7  开始施法 %.1f 秒后消耗 %d 枚硬化骨髓", consumeIntervalTicks / 20.0f, materialCost));
        tooltip.add(String.format("§7  此后每 %.1f 秒继续消耗 %d 枚", consumeIntervalTicks / 20.0f, materialCost));
        tooltip.add(String.format("§7  每消耗 %d 枚，召唤 %d 枚骨陨石", materialCost, meteorsPerConsume));
        tooltip.add(String.format("§7  骨陨石在玩家上方 %.1f 格、", summonHeight));
        tooltip.add(String.format("§7  半径 %.1f 格的圆形区域内随机生成", summonRadius));
        tooltip.add("");
        tooltip.add("§d骨陨石：");
        tooltip.add(String.format("§7  落地后影响 %.1f×%.1f×%.1f 区域", impactBoxSize, impactBoxSize, impactBoxSize));
        tooltip.add(String.format("§7  每枚造成固定 %.1f 点伤害", impactDamage));
        tooltip.add(String.format("§7  每次成功命中施加 %d 层【骨折】", fractureStacks));
        tooltip.add("");
        tooltip.add("§c施法代价：");
        tooltip.add(String.format("§7  施法期间移动速度降低 %d%%", Math.round((1.0 - castingMoveSpeedMultiplier) * 100)));
        tooltip.add("§7  硬化骨髓耗尽时自动停止施法");

        int materialCount = getMaterialCount(player);
        tooltip.add("");
        tooltip.add(String.format("§f背包硬化骨髓：§6%d", materialCount));
    }

    private int getMaterialCount(EntityPlayer player) {
        int count = 0;
        IInventory inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() != null) {
                if (Item.itemRegistry.getNameForObject(stack.getItem())
                    .equals(materialItemId)) {
                    count += stack.stackSize;
                }
            }
        }
        return count;
    }
}
