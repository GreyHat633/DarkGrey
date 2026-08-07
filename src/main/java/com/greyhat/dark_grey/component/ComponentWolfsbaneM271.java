package com.greyhat.dark_grey.component;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.GunMagazineHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.api.death.AbsoluteDeathReason;
import com.greyhat.dark_grey.api.death.AbsoluteDeathService;
import com.greyhat.dark_grey.entity.EntityWolfsbaneBullet;

import cpw.mods.fml.common.registry.GameRegistry;

public class ComponentWolfsbaneM271
    implements IRPGComponent, IOnRightClick, IOnWeaponUsingTick, IOnLeftClick, IHasTooltip {

    private int loadTicksRequired = 50; // 2.5s
    private float baseDamage = 100.0f;
    private int magazineCapacity = 6;

    public int getLoadTicksRequired() {
        return this.loadTicksRequired;
    }

    public int getMagazineCapacity() {
        return this.magazineCapacity;
    }

    @Override
    public String getComponentId() {
        return "狼毒M271";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("loadTicksRequired")) {
            this.loadTicksRequired = params.get("loadTicksRequired")
                .getAsInt();
        }
        if (params.has("baseDamage")) {
            this.baseDamage = params.get("baseDamage")
                .getAsFloat();
        }
        if (params.has("magazineCapacity")) {
            this.magazineCapacity = params.get("magazineCapacity")
                .getAsInt();
        }
    }

    private int getRouletteChance(NBTTagCompound nbt) {
        if (nbt != null && nbt.hasKey("DarkGreyWolfsbaneRouletteChance")) {
            return nbt.getInteger("DarkGreyWolfsbaneRouletteChance");
        }
        return 10;
    }

    private void setRouletteChance(ItemStack stack, int chance) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setInteger("DarkGreyWolfsbaneRouletteChance", Math.min(100, Math.max(10, chance)));
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        int ammo = GunMagazineHelper.getLoadedAmmo(weaponStack);

        if (ammo < magazineCapacity) {
            player.setItemInUse(
                weaponStack,
                weaponStack.getItem()
                    .getMaxItemUseDuration(weaponStack));
        }
        return weaponStack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        int maxDuration = stack.getItem()
            .getMaxItemUseDuration(stack);
        int ticksInUse = maxDuration - count;

        int ammo = GunMagazineHelper.getLoadedAmmo(stack);
        if (ammo >= magazineCapacity) {
            player.clearItemInUse();
            return;
        }

        if (ticksInUse == loadTicksRequired) {
            int ammoToAdd = 0;
            int neededAmmo = magazineCapacity - ammo;
            Item diceItem = GameRegistry.findItem("dark_grey", "judgment_dice");

            if (player.capabilities.isCreativeMode) {
                ammoToAdd = neededAmmo;
            } else if (diceItem != null) {
                for (int i = 0; i < neededAmmo; i++) {
                    if (player.inventory.consumeInventoryItem(diceItem)) {
                        ammoToAdd++;
                    } else {
                        break;
                    }
                }
            }

            if (ammoToAdd > 0) {
                if (!player.worldObj.isRemote) {
                    GunMagazineHelper.setLoadedAmmo(stack, ammo + ammoToAdd);
                    player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.2F);
                    player.stopUsingItem();

                    if (player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
                    }
                }
            } else {
                if (!player.worldObj.isRemote) {
                    player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.5F);
                }
                player.clearItemInUse();
            }
        }

        // Stunning visual effects during reloading
        if (player.worldObj.isRemote && ticksInUse % 5 == 0) {
            double px = player.posX + (player.worldObj.rand.nextDouble() - 0.5);
            double py = player.posY + player.worldObj.rand.nextDouble() * 2.0;
            double pz = player.posZ + (player.worldObj.rand.nextDouble() - 0.5);
            player.worldObj.spawnParticle("reddust", px, py, pz, 1.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean onLeftClick(ItemStack weaponStack, EntityPlayer player) {
        World world = player.worldObj;
        int ammo = GunMagazineHelper.getLoadedAmmo(weaponStack);
        NBTTagCompound nbt = weaponStack.getTagCompound();

        if (ammo > 0) {
            if (!world.isRemote) {
                int chance = getRouletteChance(nbt);
                Random rand = world.rand;

                int roll = rand.nextInt(100) + 1; // 1 to 100
                boolean rouletteTriggered = roll <= chance;

                if (rouletteTriggered) {
                    setRouletteChance(weaponStack, 10); // Reset to 10%

                    boolean lucky = rand.nextBoolean(); // 50/50
                    if (lucky) {
                        // 幸运分支
                        player.addChatMessage(
                            new net.minecraft.util.ChatComponentText(
                                EnumChatFormatting.GOLD + "【狼毒M271】极运触发！发射 6000% 伤害死神弹！"));
                        float dmg = baseDamage * 60.0f;
                        shootProjectile(world, player, dmg, true);
                        world.playSoundAtEntity(player, "random.explode", 2.0F, 0.5F);
                    } else {
                        // 死亡分支
                        player.addChatMessage(
                            new net.minecraft.util.ChatComponentText(
                                EnumChatFormatting.DARK_RED + "【狼毒M271】死神降临！厄运之枪对准了你..."));
                        world.playSoundAtEntity(player, "ambient.weather.thunder", 2.0F, 0.5F);
                        if (world instanceof WorldServer) {
                            ((WorldServer) world).func_147487_a(
                                "largeexplode",
                                player.posX,
                                player.posY + 1,
                                player.posZ,
                                20,
                                1.0,
                                2.0,
                                1.0,
                                0.0);
                            ((WorldServer) world).func_147487_a(
                                "flame",
                                player.posX,
                                player.posY + 1,
                                player.posZ,
                                50,
                                0.5,
                                2.0,
                                0.5,
                                0.1);
                        }

                        if (player instanceof EntityPlayerMP) {
                            AbsoluteDeathService.requestAbsoluteDeath(
                                (EntityPlayerMP) player,
                                player,
                                AbsoluteDeathReason.WOLFSBANE_ROULETTE);
                        }
                    }
                } else {
                    // 轮盘未触发
                    setRouletteChance(weaponStack, chance + 10);
                    player.addChatMessage(
                        new net.minecraft.util.ChatComponentText(
                            EnumChatFormatting.GRAY + "【狼毒M271】轮盘未触发，死神降临概率上升至 " + (chance + 10) + "%。"));
                    shootProjectile(world, player, baseDamage, false);
                    world.playSoundAtEntity(player, "random.explode", 1.0F, 1.5F);
                }

                GunMagazineHelper.setLoadedAmmo(weaponStack, ammo - 1);

                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).updateHeldItem();
                }
            }
            return true;
        }

        return false;
    }

    private void shootProjectile(World world, EntityPlayer player, float damage, boolean isHeavyStrike) {
        EntityWolfsbaneBullet bullet = new EntityWolfsbaneBullet(world, player, damage, isHeavyStrike);
        // speed up
        bullet.motionX *= 2.0;
        bullet.motionY *= 2.0;
        bullet.motionZ *= 2.0;
        if (isHeavyStrike) {
            world.playSoundAtEntity(
                player,
                "mob.wither.spawn",
                0.5F,
                (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 0.5F);
        }
        world.spawnEntityInWorld(bullet);
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        int ammo = GunMagazineHelper.getLoadedAmmo(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        int chance = getRouletteChance(nbt);

        tooltipLines.add(EnumChatFormatting.GOLD + "伤害：" + (int) baseDamage);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹匣：" + magazineCapacity);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹药：审判骰");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "长按右键：装填 (" + (loadTicksRequired / 20.0f) + "秒)");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键：发射");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "俄罗斯轮盘：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  如果触发轮盘：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  - 50% 概率发射 6000% 伤害死神弹");
        tooltipLines.add(EnumChatFormatting.GRAY + "  - 50% 概率触发【绝对死亡】自尽");
        tooltipLines.add(EnumChatFormatting.GRAY + "  若未触发，每次射击都会增加 10% 概率");
        tooltipLines.add("");
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前弹药："
                + (ammo > 0 ? (EnumChatFormatting.GREEN + String.valueOf(ammo) + " / " + magazineCapacity)
                    : (EnumChatFormatting.RED + "空")));
        tooltipLines.add(EnumChatFormatting.WHITE + "当前审判概率：" + EnumChatFormatting.GOLD + chance + "%");
    }
}
