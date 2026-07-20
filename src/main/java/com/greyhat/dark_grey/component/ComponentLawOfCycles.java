//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnBowShoot;
import com.greyhat.dark_grey.api.capability.IOnBowUsingTick;
import com.greyhat.dark_grey.entity.EntityMadokaArrow;

public class ComponentLawOfCycles implements IOnBowShoot, IHasTooltip, IOnBowUsingTick {

    private float damage1;
    private float damage2;
    private float damage3;

    public ComponentLawOfCycles() {
        this.damage1 = 20.0f;
        this.damage2 = 30.0f;
        this.damage3 = 50.0f;
    }

    @Override
    public void configure(final JsonObject params) {
        if (params != null) {
            this.damage1 = (params.has("damage1") ? params.get("damage1")
                .getAsFloat() : 20.0f);
            this.damage2 = (params.has("damage2") ? params.get("damage2")
                .getAsFloat() : 30.0f);
            this.damage3 = (params.has("damage3") ? params.get("damage3")
                .getAsFloat() : 50.0f);
        }
    }

    @Override
    public String getComponentId() {
        return "\u8679\u4e4b\u613f";
    }

    @Override
    public boolean onBowShoot(final ItemStack bowStack, final World world, final EntityPlayer player,
        final int charge) {
        // The first visual/damage stage starts at 20 ticks. Reject tap-release packets so
        // creative/Infinity players cannot flood the server with zero-charge projectiles.
        if (charge < 20) {
            return true;
        }
        boolean hasAmmo = EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, bowStack) > 0;
        if (player.capabilities.isCreativeMode) {
            hasAmmo = true;
        } else if (!hasAmmo) {
            for (int i = 0; i < player.inventory.mainInventory.length; ++i) {
                if (player.inventory.mainInventory[i] != null
                    && player.inventory.mainInventory[i].getItem() instanceof IRPGItemContainer) {
                    final String ammoId = ((IRPGItemContainer) player.inventory.mainInventory[i].getItem())
                        .getRpgItemId();
                    final RPGItemDataManager.ItemConfig ammoConfig = RPGItemDataManager.getInstance()
                        .getConfig(ammoId);
                    if (ammoConfig != null && ("arrow".equals(ammoConfig.type) || "\u7bad".equals(ammoConfig.type))) {
                        hasAmmo = true;
                        break;
                    }
                }
            }
        }
        if (!hasAmmo) {
            return false;
        }
        int level = 0;
        if (charge >= 60) {
            level = 3;
        } else if (charge >= 40) {
            level = 2;
        } else if (charge >= 20) {
            level = 1;
        }
        if (!world.isRemote) {
            float baseDamage = 10.0f;
            float velocity = 3.0f;
            if (level == 1) {
                baseDamage = this.damage1;
                velocity = 5.0f;
            } else if (level == 2) {
                baseDamage = this.damage2;
                velocity = 7.0f;
            } else if (level == 3) {
                baseDamage = this.damage3;
                velocity = 6.0f;
            }
            final EntityMadokaArrow arrow = new EntityMadokaArrow(world, (EntityLivingBase) player, level, baseDamage);
            arrow.setThrowableHeading(arrow.motionX, arrow.motionY, arrow.motionZ, velocity, 1.0f);
            world.spawnEntityInWorld((Entity) arrow);
            world.playSoundAtEntity(
                (Entity) player,
                "random.bow",
                1.0f,
                1.0f / (world.rand.nextFloat() * 0.4f + 1.2f) + velocity * 0.5f);
        }
        if (!world.isRemote && !player.capabilities.isCreativeMode
            && EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, bowStack) == 0) {
            for (int j = 0; j < player.inventory.mainInventory.length; ++j) {
                if (player.inventory.mainInventory[j] != null
                    && player.inventory.mainInventory[j].getItem() instanceof IRPGItemContainer) {
                    final String ammoId2 = ((IRPGItemContainer) player.inventory.mainInventory[j].getItem())
                        .getRpgItemId();
                    final RPGItemDataManager.ItemConfig ammoConfig2 = RPGItemDataManager.getInstance()
                        .getConfig(ammoId2);
                    if (ammoConfig2 != null
                        && ("arrow".equals(ammoConfig2.type) || "\u7bad".equals(ammoConfig2.type))) {
                        player.inventory.decrStackSize(j, 1);
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void onBowUsingTick(final ItemStack bowStack, final World world, final EntityPlayer player,
        final int count) {
        final int maxUse = bowStack.getMaxItemUseDuration();
        final int charge = maxUse - count;
        if (charge == 20) {
            this.spawnChargeParticles(world, player, 1);
            world.playSoundAtEntity((Entity) player, "random.orb", 0.5f, 0.8f);
        } else if (charge == 40) {
            this.spawnChargeParticles(world, player, 2);
            world.playSoundAtEntity((Entity) player, "random.orb", 0.7f, 1.2f);
        } else if (charge == 60) {
            this.spawnChargeParticles(world, player, 3);
            world.playSoundAtEntity((Entity) player, "random.orb", 1.0f, 1.6f);
        }
    }

    private void spawnChargeParticles(final World world, final EntityPlayer player, final int level) {
        if (!world.isRemote) {
            final String color = (level == 1) ? "\u00A7e" : ((level == 2) ? "\u00A76" : "\u00A7c");
            player.addChatMessage(
                (IChatComponent) new ChatComponentText(
                    color + "\u8679\u4e4b\u5f13: \u84c4\u529b\u9636\u5c42 " + level + " !"));
        } else {
            final Vec3 lookVec = player.getLookVec();
            final double spawnX = player.posX + lookVec.xCoord * 1.5;
            final double spawnY = player.posY + player.getEyeHeight() + lookVec.yCoord * 1.5;
            final double spawnZ = player.posZ + lookVec.zCoord * 1.5;
            for (int particleCount = level * 15, i = 0; i < particleCount; ++i) {
                final double offsetX = (world.rand.nextDouble() - 0.5) * 1.0;
                final double offsetY = (world.rand.nextDouble() - 0.5) * 1.0;
                final double offsetZ = (world.rand.nextDouble() - 0.5) * 1.0;
                String particleType = "magicCrit";
                if (level == 3 && world.rand.nextBoolean()) {
                    particleType = "fireworksSpark";
                }
                world.spawnParticle(particleType, spawnX + offsetX, spawnY + offsetY, spawnZ + offsetZ, 0.0, 0.05, 0.0);
            }
        }
    }

    @Override
    public void addTooltipLines(final ItemStack stack, final EntityPlayer player, final List<String> tooltip,
        final boolean advanced) {
        tooltip.add("\u00A7d\u2728 \u8679\u4e4b\u613f \u00A77| \u00A7d\u591a\u6bb5\u84c4\u529b\u9b54\u6cd5\u9635");
        tooltip.add(
            String.format(
                "  \u00A77\u25b6 \u00A7e1\u6bb5\u84c4\u529b \u00A78(1.0s)\u00A77: \u00A7c%.1f \u00A77\u70b9\u4f24\u5bb3",
                this.damage1));
        tooltip.add(
            String.format(
                "  \u00A77\u25b6 \u00A762\u6bb5\u84c4\u529b \u00A78(2.0s)\u00A77: \u00A7c%.1f \u00A77\u70b9\u4f24\u5bb3",
                this.damage2));
        tooltip.add(
            String.format(
                "  \u00A77\u25b6 \u00A7c\u00A7l3\u6bb5\u84c4\u529b \u00A78(3.0s)\u00A77: \u00A7c\u00A7l%.1f \u00A77\u70b9\u4f24\u5bb3",
                this.damage3));
    }
}
