package com.greyhat.dark_grey.api.impl;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.entity.EntityAuraTorrent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.List;

public class ComponentAuraTorrent implements IRPGComponent, IOnWeaponUsingTick, IOnRightClick, IOnPlayerStoppedUsing, IHasTooltip {

    private int duration = 200; // default 10 seconds
    private float damage = 1.0F; // default 1 true damage
    private int cooldown = 600; // default 30 seconds

    @Override
    public String getComponentId() {
        return "灵气洪流";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("duration")) {
            this.duration = params.get("duration").getAsInt();
        }
        if (params.has("damage")) {
            this.damage = params.get("damage").getAsFloat();
        }
        if (params.has("cooldown")) {
            this.cooldown = params.get("cooldown").getAsInt();
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add("\u00A7b\u2248 \u7075\u6C14\u6D2A\u6D41 \u00A77| \u00A73\u957F\u6309\u53F3\u952E\u91CA\u653E\u6269\u6563\u6CD5\u9635\uFF0C\u9020\u6210\u771F\u5B9E\u4F24\u5BB3\u4E0E\u591A\u91CD\u524A\u5F31 (\u51B7\u5374" + (this.cooldown / 20) + "\u79D2)");
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote) return weaponStack;

        long currentTime = world.getTotalWorldTime();
        long lastUsedTime = 0;
        if (weaponStack.hasTagCompound() && weaponStack.getTagCompound().hasKey("aura_torrent_last_used")) {
            lastUsedTime = weaponStack.getTagCompound().getLong("aura_torrent_last_used");
        }

        if (currentTime - lastUsedTime < this.cooldown) {
            long remainingTicks = this.cooldown - (currentTime - lastUsedTime);
            float remainingSeconds = remainingTicks / 20.0f;
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "【灵气洪流】技能冷却中，还需等待 " + String.format("%.1f", remainingSeconds) + " 秒。"));
        }
        return weaponStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        World world = player.worldObj;
        
        long currentTime = world.getTotalWorldTime();
        long lastUsedTime = 0;
        if (weaponStack.hasTagCompound() && weaponStack.getTagCompound().hasKey("aura_torrent_last_used")) {
            lastUsedTime = weaponStack.getTagCompound().getLong("aura_torrent_last_used");
        }

        // If on cooldown, do not show effects or deal damage
        if (currentTime - lastUsedTime < this.cooldown) {
            return;
        }

        int maxDuration = weaponStack.getItem().getMaxItemUseDuration(weaponStack);
        int chargeTicks = maxDuration - count;

        double currentRadius = Math.min(10.0, chargeTicks * 0.15);
        double baseY = player.boundingBox.minY + 0.5; // Use boundingBox.minY for consistent feet height on both client and server

        if (world.isRemote) {
            int edgeParticles = 12;
            for (int i = 0; i < edgeParticles; i++) {
                double angle = world.rand.nextDouble() * 2 * Math.PI;
                double px = player.posX + currentRadius * Math.cos(angle);
                double pz = player.posZ + currentRadius * Math.sin(angle);
                double py = baseY + (world.rand.nextDouble() - 0.5) * 0.2;
                
                world.spawnParticle("cloud", px, py, pz, 0, 0.01, 0);
            }
            
            int innerParticles = 8;
            for (int i = 0; i < innerParticles; i++) {
                double innerRadius = world.rand.nextDouble() * currentRadius;
                double angle = world.rand.nextDouble() * 2 * Math.PI;
                double px = player.posX + innerRadius * Math.cos(angle);
                double pz = player.posZ + innerRadius * Math.sin(angle);
                double py = baseY + (world.rand.nextDouble() - 0.5) * 1.0;
                
                world.spawnParticle("mobSpell", px, py, pz, 0.5, 0.0, 0.5);
            }
            return;
        }

        int scanInterval = 10;
        if (chargeTicks % scanInterval == 0) {
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                player.posX - currentRadius, baseY - 2.0, player.posZ - currentRadius,
                player.posX + currentRadius, baseY + 2.0, player.posZ + currentRadius
            );

            @SuppressWarnings("unchecked")
            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);

            for (EntityLivingBase target : entities) {
                if (target == player) continue;
                
                double distSq = player.getDistanceSqToEntity(target);
                if (distSq <= currentRadius * currentRadius) {
                    float newHealth = target.getHealth() - this.damage;
                    if (newHealth <= 0.0F) {
                        target.setHealth(0.0F);
                        DamageSource magic = new net.minecraft.util.EntityDamageSource("magic", player).setDamageBypassesArmor().setDamageIsAbsolute();
                        target.onDeath(magic);
                    } else {
                        target.setHealth(newHealth);
                        world.playSoundAtEntity(target, "game.player.hurt", 1.0F, 1.0F);
                        world.setEntityState(target, (byte)2); // Play hurt animation
                    }
                    
                    target.addPotionEffect(new PotionEffect(Potion.confusion.id, 20, 0));
                    target.addPotionEffect(new PotionEffect(Potion.weakness.id, 20, 1));
                    target.addPotionEffect(new PotionEffect(Potion.blindness.id, 20, 0));
                    target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 1));
                    target.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 20, 1));
                }
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int itemInUseCount) {
        long currentTime = world.getTotalWorldTime();
        long lastUsedTime = 0;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("aura_torrent_last_used")) {
            lastUsedTime = stack.getTagCompound().getLong("aura_torrent_last_used");
        }

        if (currentTime - lastUsedTime < this.cooldown) {
            return; // Still on cooldown
        }

        int maxDuration = stack.getItem().getMaxItemUseDuration(stack);
        int chargeTicks = maxDuration - itemInUseCount;
        
        if (chargeTicks < 10) return; // Ignore accidental quick clicks

        if (!world.isRemote) {
            double finalRadius = Math.min(10.0, chargeTicks * 0.15);
            
            EntityAuraTorrent aura = new EntityAuraTorrent(world, player, finalRadius, this.damage, this.duration);
            world.spawnEntityInWorld(aura);
            
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            }
            stack.getTagCompound().setLong("aura_torrent_last_used", currentTime);
        }
    }
}