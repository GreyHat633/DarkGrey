// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey.component;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import com.greyhat.dark_grey.entity.EntityMadokaArrow;
import net.minecraft.entity.Entity;
import com.greyhat.dark_grey.api.IRPGItemContainer;
import net.minecraft.util.Vec3;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnBowShoot;
import com.greyhat.dark_grey.api.capability.IOnBowUsingTick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.IRPGComponent;

public class ComponentTrueLawOfCycles implements IRPGComponent, IOnRightClick, IOnBowUsingTick, IOnBowShoot, IHasTooltip
{
    private float baseDamage;
    private int requiredCharge;
    private int arrowCount;
    
    public ComponentTrueLawOfCycles() {
        this.baseDamage = 15.0f;
        this.requiredCharge = 100;
        this.arrowCount = 30;
    }
    
    @Override
    public String getComponentId() {
        return "\u5706\u73af\u4e4b\u7406";
    }
    
    @Override
    public void configure(final JsonObject params) {
        if (params.has("damage")) {
            this.baseDamage = params.get("damage").getAsFloat();
        }
        if (params.has("charge")) {
            this.requiredCharge = params.get("charge").getAsInt();
        }
        if (params.has("arrowCount")) {
            this.arrowCount = params.get("arrowCount").getAsInt();
        }
    }
    
    @Override
    public ItemStack onRightClick(final ItemStack stack, final World world, final EntityPlayer player) {
        if (player.onGround) {
            if (!world.isRemote) {
                player.addChatMessage((IChatComponent)new ChatComponentText("\u00A7c\u5c55\u7fc5\u5427\uff0c\u4eba\u7c7b\u3002"));
            }
            return null;
        }
        boolean hasLightArrow = player.capabilities.isCreativeMode;
        if (!hasLightArrow) {
            for (int i = 0; i < player.inventory.mainInventory.length; ++i) {
                final ItemStack invStack = player.inventory.mainInventory[i];
                if (invStack != null && "item.dark_grey:arrow_of_light".equals(invStack.getItem().getUnlocalizedName())) {
                    hasLightArrow = true;
                    break;
                }
            }
        }
        if (!hasLightArrow) {
            if (!world.isRemote) {
                player.addChatMessage((IChatComponent)new ChatComponentText("\u00A7c\u9700\u8981 [\u5149\u4e4b\u77e2] \u624d\u80fd\u91ca\u653e\u5706\u73af\u4e4b\u7406\uff01 \uff08\u672a\u68c0\u6d4b\u5230ID\u4e3a arrow_of_light \u7684\u7269\u54c1\uff09"));
            }
            return null;
        }
        return stack;
    }
    
    @Override
    public void onBowUsingTick(final ItemStack stack, final World world, final EntityPlayer player, final int timeLeft) {
        final int charge = stack.getItem().getMaxItemUseDuration(stack) - timeLeft;
        if (player.onGround) {
            player.clearItemInUse();
            if (!world.isRemote && charge == 0) {
                player.addChatMessage((IChatComponent)new ChatComponentText("\u00A7c\u5c55\u7fc5\u5427\uff0c\u4eba\u7c7b\u3002"));
            }
            return;
        }
        if (world.isRemote) {
            this.renderCrestParticles(player, charge);
        }
    }
    
    private void renderCrestParticles(final EntityPlayer player, final int charge) {
        if (charge <= 0) {
            return;
        }
        final double progress = Math.min(1.0, charge / (double)this.requiredCharge);
        final Vec3 look = player.getLookVec();
        final World world = player.worldObj;
        final double cx = player.posX + look.xCoord * 3.0;
        final double cy = player.posY + player.getEyeHeight() + look.yCoord * 3.0 + 0.5;
        final double cz = player.posZ + look.zCoord * 3.0;
        final double maxRadius = 3.0;
        final double currentScale = maxRadius * (0.2 + 0.8 * progress);
        for (int outerPoints = (int)(60.0 * progress), i = 0; i < outerPoints; ++i) {
            final double angle = 6.283185307179586 * i / outerPoints;
            final Vec3 right = look.crossProduct(Vec3.createVectorHelper(0.0, 1.0, 0.0)).normalize();
            final Vec3 up = right.crossProduct(look).normalize();
            final double px = cx + right.xCoord * Math.cos(angle) * currentScale + up.xCoord * Math.sin(angle) * currentScale;
            final double py = cy + right.yCoord * Math.cos(angle) * currentScale + up.yCoord * Math.sin(angle) * currentScale;
            final double pz = cz + right.zCoord * Math.cos(angle) * currentScale + up.zCoord * Math.sin(angle) * currentScale;
            world.spawnParticle("fireworksSpark", px, py, pz, 0.0, 0.0, 0.0);
        }
        for (int innerPoints = (int)(30.0 * progress), j = 0; j < innerPoints; ++j) {
            final double angle2 = 6.283185307179586 * j / innerPoints;
            final Vec3 right2 = look.crossProduct(Vec3.createVectorHelper(0.0, 1.0, 0.0)).normalize();
            final Vec3 up2 = right2.crossProduct(look).normalize();
            final double px2 = cx + right2.xCoord * Math.cos(angle2) * (currentScale * 0.3) + up2.xCoord * Math.sin(angle2) * (currentScale * 0.3);
            final double py2 = cy + right2.yCoord * Math.cos(angle2) * (currentScale * 0.3) + up2.yCoord * Math.sin(angle2) * (currentScale * 0.3);
            final double pz2 = cz + right2.zCoord * Math.cos(angle2) * (currentScale * 0.3) + up2.zCoord * Math.sin(angle2) * (currentScale * 0.3);
            world.spawnParticle("magicCrit", px2, py2, pz2, 0.0, 0.0, 0.0);
        }
        if (progress > 0.5) {
            final double subProgress = (progress - 0.5) * 2.0;
            final int[] array;
            final int[] nodeAngles = array = new int[] { 30, 90, 150, 210, 270, 330 };
            for (final int deg : array) {
                final double rad = Math.toRadians(deg + charge * 0.5);
                final Vec3 right3 = look.crossProduct(Vec3.createVectorHelper(0.0, 1.0, 0.0)).normalize();
                final Vec3 up3 = right3.crossProduct(look).normalize();
                for (double lineDist = currentScale * 0.7 * subProgress, d = 0.0; d <= lineDist; d += 0.2) {
                    final double px3 = cx + right3.xCoord * Math.cos(rad) * d + up3.xCoord * Math.sin(rad) * d;
                    final double py3 = cy + right3.yCoord * Math.cos(rad) * d + up3.yCoord * Math.sin(rad) * d;
                    final double pz3 = cz + right3.zCoord * Math.cos(rad) * d + up3.zCoord * Math.sin(rad) * d;
                    world.spawnParticle("reddust", px3, py3, pz3, 1.0, 0.6, 0.8);
                }
                final double nx = cx + right3.xCoord * Math.cos(rad) * (currentScale * 0.7) + up3.xCoord * Math.sin(rad) * (currentScale * 0.7);
                final double ny = cy + right3.yCoord * Math.cos(rad) * (currentScale * 0.7) + up3.yCoord * Math.sin(rad) * (currentScale * 0.7);
                final double nz = cz + right3.zCoord * Math.cos(rad) * (currentScale * 0.7) + up3.zCoord * Math.sin(rad) * (currentScale * 0.7);
                for (int k = 0; k < 15; ++k) {
                    final double angle3 = 6.283185307179586 * k / 15.0;
                    final double px4 = nx + right3.xCoord * Math.cos(angle3) * 0.5 + up3.xCoord * Math.sin(angle3) * 0.5;
                    final double py4 = ny + right3.yCoord * Math.cos(angle3) * 0.5 + up3.yCoord * Math.sin(angle3) * 0.5;
                    final double pz4 = nz + right3.zCoord * Math.cos(angle3) * 0.5 + up3.zCoord * Math.sin(angle3) * 0.5;
                    world.spawnParticle("fireworksSpark", px4, py4, pz4, 0.0, 0.0, 0.0);
                }
            }
        }
        if (charge == this.requiredCharge) {
            for (int j = 0; j < 100; ++j) {
                world.spawnParticle("enchantmenttable", cx + (world.rand.nextDouble() - 0.5) * 4.0, cy + (world.rand.nextDouble() - 0.5) * 4.0, cz + (world.rand.nextDouble() - 0.5) * 4.0, 0.0, 1.0, 0.0);
            }
            player.playSound("random.levelup", 1.0f, 0.5f);
        }
    }
    
    @Override
    public boolean onBowShoot(final ItemStack stack, final World world, final EntityPlayer player, final int charge) {
        if (charge < this.requiredCharge) {
            if (!world.isRemote && charge >= 20) {
                player.addChatMessage((IChatComponent)new ChatComponentText("\u00A7c\u84c4\u529b\u672a\u6ee1\uff0c\u795e\u529b\u6d88\u6563\u4e86..."));
            }
            return true;
        }
        boolean hasLightArrow = player.capabilities.isCreativeMode;
        if (!hasLightArrow) {
            for (int i = 0; i < player.inventory.mainInventory.length; ++i) {
                final ItemStack invStack = player.inventory.mainInventory[i];
                if (invStack != null && invStack.getItem() instanceof IRPGItemContainer) {
                    final String itemId = ((IRPGItemContainer)invStack.getItem()).getRpgItemId();
                    if ("arrow_of_light".equals(itemId)) {
                        hasLightArrow = true;
                        player.inventory.decrStackSize(i, 1);
                        break;
                    }
                }
            }
        }
        if (!hasLightArrow) {
            if (!world.isRemote) {
                player.addChatMessage((IChatComponent)new ChatComponentText("\u00A7c\u9700\u8981 [\u5149\u4e4b\u77e2] \u624d\u80fd\u91ca\u653e\u5706\u73af\u4e4b\u7406\uff01"));
            }
            return true;
        }
        if (!world.isRemote) {
            player.worldObj.playSoundAtEntity((Entity)player, "random.bow", 1.0f, 1.0f / (world.rand.nextFloat() * 0.4f + 1.2f) + 0.5f);
            for (int i = 0; i < this.arrowCount; ++i) {
                final EntityMadokaArrow entityMadokaArrow;
                final EntityMadokaArrow arrow = entityMadokaArrow = new EntityMadokaArrow(world, (EntityLivingBase)player, 3, this.baseDamage);
                entityMadokaArrow.motionX += world.rand.nextGaussian() * 0.4;
                final EntityMadokaArrow entityMadokaArrow2 = arrow;
                entityMadokaArrow2.motionY += world.rand.nextGaussian() * 0.4;
                final EntityMadokaArrow entityMadokaArrow3 = arrow;
                entityMadokaArrow3.motionZ += world.rand.nextGaussian() * 0.4;
                world.spawnEntityInWorld((Entity)arrow);
            }
        }
        final Vec3 look = player.getLookVec();
        player.motionX -= look.xCoord * 1.5;
        player.motionY += 0.5;
        player.motionZ -= look.zCoord * 1.5;
        return true;
    }
    
    @Override
    public void addTooltipLines(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
        tooltip.add("\u00A7d\u2728 \u5706\u73af\u4e4b\u7406 \u00A77| \u00A7d\u795e\u529b\u964d\u4e34");
        tooltip.add("  \u00A77\u25b6 \u00A7e\u6761\u4ef6\u00A77: \u5fc5\u987b\u5728\u6ede\u7a7a\u65f6\u624d\u80fd\u5c55\u5f00\u795e\u529b");
        tooltip.add(String.format("  \u00A77\u25b6 \u00A7c\u84c4\u529b\u00A77: %.1f\u79d2 \u81f3\u795e\u529b\u5b8c\u6574\uff08\u843d\u5730\u6253\u65ad\uff09", this.requiredCharge / 20.0f));
        tooltip.add(String.format("  \u00A77\u25b6 \u00A7a\u5a01\u529b\u00A77: \u6d88\u8017[\u5149\u4e4b\u77e2]\u5c04\u51fa %d \u652f\u5149\u77e2\u4e4b\u96e8", this.arrowCount));
        tooltip.add(String.format("  \u00A77\u25b6 \u00A74\u4f24\u5bb3\u00A77: \u6bcf\u652f\u5149\u77e2 %.1f \u70b9\u771f\u5b9e\u4f24\u5bb3", this.baseDamage));
    }
}
