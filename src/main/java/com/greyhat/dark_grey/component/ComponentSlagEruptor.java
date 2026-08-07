package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.GunMagazineHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntitySunflameBullet;

public class ComponentSlagEruptor implements IRPGComponent, IOnRightClick, IOnWeaponUsingTick, IOnLeftClick,
    IHasTooltip, com.greyhat.dark_grey.api.capability.IScorchIgniter {

    private int loadTicksRequired = 40; // 2s
    private float baseDamage = 50.0f;
    private int magazineCapacity = 30;

    public int getLoadTicksRequired() {
        return loadTicksRequired;
    }

    public int getMagazineCapacity() {
        return magazineCapacity;
    }

    @Override
    public String getComponentId() {
        return "熔渣喷发器";
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
            if (!player.worldObj.isRemote) {
                GunMagazineHelper.setLoadedAmmo(stack, magazineCapacity);
                player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.2F);
                player.stopUsingItem();
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
                }
            } else {
                player.clearItemInUse();
            }
        }

        if (player.worldObj.isRemote && ticksInUse % 4 == 0) {
            double px = player.posX + (player.worldObj.rand.nextDouble() - 0.5);
            double py = player.posY + player.worldObj.rand.nextDouble() * 2.0;
            double pz = player.posZ + (player.worldObj.rand.nextDouble() - 0.5);
            com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(player.worldObj, "flame", px, py, pz, 0.0, 0.05, 0.0);
        }
    }

    @Override
    public boolean onLeftClick(ItemStack weaponStack, EntityPlayer player) {
        int ammo = GunMagazineHelper.getLoadedAmmo(weaponStack);
        if (ammo <= 0) {
            return false;
        } else {
            com.greyhat.dark_grey.combat.AutomaticFireManager.startFire(player, weaponStack);
            return true;
        }
    }

    public void fireOneShot(ItemStack held, World world, EntityPlayer player) {
        GunMagazineHelper.consumeAmmo(held, 1);

        if (!world.isRemote) {
            float spread = 0.8f;
            float dpitch = (world.rand.nextFloat() - 0.5f) * spread;
            float dyaw = (world.rand.nextFloat() - 0.5f) * spread;

            EntitySunflameBullet bullet = new EntitySunflameBullet(world, player, baseDamage);
            bullet.setLocationAndAngles(
                player.posX,
                player.posY + player.getEyeHeight(),
                player.posZ,
                player.rotationYaw + dyaw,
                player.rotationPitch + dpitch);
            // Setting velocity according to look vector
            float f = 0.4F;
            bullet.motionX = (double) (-net.minecraft.util.MathHelper.sin(bullet.rotationYaw / 180.0F * (float) Math.PI)
                * net.minecraft.util.MathHelper.cos(bullet.rotationPitch / 180.0F * (float) Math.PI)
                * f);
            bullet.motionZ = (double) (net.minecraft.util.MathHelper.cos(bullet.rotationYaw / 180.0F * (float) Math.PI)
                * net.minecraft.util.MathHelper.cos(bullet.rotationPitch / 180.0F * (float) Math.PI)
                * f);
            bullet.motionY = (double) (-net.minecraft.util.MathHelper
                .sin((bullet.rotationPitch) / 180.0F * (float) Math.PI) * f);
            bullet.setThrowableHeading(bullet.motionX, bullet.motionY, bullet.motionZ, 4.0f, 0.0f);

            int fireAspect = net.minecraft.enchantment.EnchantmentHelper
                .getEnchantmentLevel(net.minecraft.enchantment.Enchantment.fireAspect.effectId, held);
            if (fireAspect > 0) {
                bullet.setFire(100);
            }

            world.spawnEntityInWorld(bullet);
            world.playSoundAtEntity(player, "random.explode", 0.5F, 2.0F);

            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).updateHeldItem();
            }
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean advanced) {
        int ammo = GunMagazineHelper.getLoadedAmmo(stack);
        tooltipLines.add(EnumChatFormatting.GOLD + "伤害：" + (int) baseDamage);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹匣：" + magazineCapacity);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹药：阳炎弹");
        tooltipLines.add(
            EnumChatFormatting.LIGHT_PURPLE + "[引燃] " + EnumChatFormatting.GRAY + "该武器造成的有效直接攻击可以引爆目标身上的灼痕印记 (115%)");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "长按右键：装填 (" + (loadTicksRequired / 20.0f) + "秒)");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键长按：全自动开火 (5发/秒)");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "阳炎弹：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  自带【火焰附加 I】词缀");
        tooltipLines.add(EnumChatFormatting.GRAY + "  每次命中时有30%概率对目标叠加 1 层【灼痕】");
        tooltipLines.add("");
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前弹药："
                + (ammo > 0 ? (EnumChatFormatting.GREEN + String.valueOf(ammo) + " / " + magazineCapacity)
                    : (EnumChatFormatting.RED + "空")));
    }
}
