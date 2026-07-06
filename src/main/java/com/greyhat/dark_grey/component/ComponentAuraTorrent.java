package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityAuraTorrent;

public class ComponentAuraTorrent
    implements IRPGComponent, IOnRightClick, IOnHeldTick, IHasTooltip, IOnWeaponUsingTick, IOnPlayerStoppedUsing {

    private float radius = 5.0f;
    private float dotDamage = 20.0f;
    private int cooldownSeconds = 30;
    private static final int MAX_CHARGE_TICKS = 40; // 2 seconds to reach max radius

    @Override
    public String getComponentId() {
        return "灵气洪流";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("radius")) {
            radius = params.get("radius")
                .getAsFloat();
        }
        if (params.has("dotDamage")) {
            dotDamage = params.get("dotDamage")
                .getAsFloat();
        }
        if (params.has("cooldown")) {
            cooldownSeconds = params.get("cooldown")
                .getAsInt();
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(
            "\u00A76\u2726 \u7075\u6C14\u6D2A\u6D41 \u00A77| \u00A7e\u53F3\u952E\u957F\u6309\u84C4\u529b\uFF0C\u677e\u5F00\u91CA\u653E");
        tooltip.add(
            "\u00A77   \u00A78(\u84C4\u529b\u6700\u591A2\u79D2\u8FBE\u5230\u6700\u5927\u534A\u5F84 " + radius
                + "\uFF0C\u9635\u5185\u6BCF0.5\u79D2"
                + (int) dotDamage
                + "\u70B9\u4F24\u5BB3\uFF0C\u51B7\u5374"
                + cooldownSeconds
                + "\u79D2)");
    }

    @Override
    public ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        long currentTime = world.getTotalWorldTime();
        long lastTime = 0;

        if (itemStack.hasTagCompound() && itemStack.getTagCompound()
            .hasKey("LastAuraTorrentTime")) {
            lastTime = itemStack.getTagCompound()
                .getLong("LastAuraTorrentTime");
        }

        long cooldownTicks = cooldownSeconds * 20L;
        if (currentTime - lastTime < cooldownTicks) {
            if (!world.isRemote) {
                long remaining_ticks = cooldownTicks - (currentTime - lastTime);
                float remaining_seconds = remaining_ticks / 20.0f;
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.RED + "【灵气洪流】技能冷却中，还需等待 "
                            + String.format("%.1f", remaining_seconds)
                            + " 秒。"));
            }
            // Clear item in use so the player doesn't start charging if it is on cooldown
            player.clearItemInUse();
            return itemStack;
        }

        // Just let them start using the item
        return itemStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        World world = player.worldObj;
        int charge = weaponStack.getItem()
            .getMaxItemUseDuration(weaponStack) - count;
        float currentRadius = Math.min(10.0f, charge * 0.15f);

        if (world.isRemote) {
            if (world.getTotalWorldTime() % 2 == 0) {
                double cy = player.boundingBox.minY + 1.0;
                int numParticles = 10 + (int) (currentRadius * 8.0);
                for (int i = 0; i < numParticles; i++) {
                    double angle = world.rand.nextDouble() * Math.PI * 2.0;
                    double px = player.posX + Math.cos(angle) * currentRadius;
                    double pz = player.posZ + Math.sin(angle) * currentRadius;
                    world.spawnParticle("largesmoke", px, cy, pz, 0, 0, 0);
                }

                // Inner area: random potion particles
                int innerCount = (int) (currentRadius * 1.5);
                for (int i = 0; i < innerCount; i++) {
                    double r = world.rand.nextDouble() * currentRadius;
                    double angle = world.rand.nextDouble() * Math.PI * 2;
                    double px = player.posX + Math.cos(angle) * r;
                    double pz = player.posZ + Math.sin(angle) * r;

                    String[] spells = { "mobSpell", "mobSpellAmbient", "witchMagic" };
                    String spell = spells[world.rand.nextInt(spells.length)];

                    double rColor = world.rand.nextDouble();
                    double gColor = world.rand.nextDouble();
                    double bColor = world.rand.nextDouble();

                    if (spell.equals("mobSpell") || spell.equals("mobSpellAmbient")) {
                        world.spawnParticle(spell, px, cy + world.rand.nextDouble() * 1.5, pz, rColor, gColor, bColor);
                    } else {
                        world.spawnParticle("witchMagic", px, cy + world.rand.nextDouble() * 1.5, pz, 0, 0, 0);
                    }
                }
            }
        }

        // Damage entities every 10 ticks (0.5s) while charging
        if (!world.isRemote && charge % 10 == 0) {
            net.minecraft.util.AxisAlignedBB aabb = player.boundingBox.expand(currentRadius, 2.0, currentRadius);
            @SuppressWarnings("unchecked")
            List<net.minecraft.entity.Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, aabb);
            for (net.minecraft.entity.Entity entity : list) {
                if (entity instanceof net.minecraft.entity.EntityLivingBase) {
                    if (player.getDistanceSqToEntity(entity) <= currentRadius * currentRadius) {
                        net.minecraft.entity.EntityLivingBase target = (net.minecraft.entity.EntityLivingBase) entity;

                        double mx = target.motionX;
                        double my = target.motionY;
                        double mz = target.motionZ;

                        target.attackEntityFrom(net.minecraft.util.DamageSource.magic, dotDamage);

                        target.motionX = mx;
                        target.motionY = my;
                        target.motionZ = mz;
                        target.isAirBorne = false;
                    }
                }
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        int charge = stack.getItem()
            .getMaxItemUseDuration(stack) - timeLeft;

        // Require at least 5 ticks to trigger (prevents accidental right click triggers)
        if (charge < 5) {
            return;
        }

        float finalRadius = Math.min(10.0f, charge * 0.15f);

        if (!world.isRemote) {
            double spawnY = player.boundingBox.minY + 1.0;
            EntityAuraTorrent aura = new EntityAuraTorrent(
                world,
                player,
                player.posX,
                spawnY,
                player.posZ,
                finalRadius,
                dotDamage);
            world.spawnEntityInWorld(aura);

            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound()
                .setLong("LastAuraTorrentTime", world.getTotalWorldTime());
        }
    }

    @Override
    public void onHeldTick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            // Idle visual effect at waist height
            if (world.rand.nextInt(5) == 0) {
                double angle = world.rand.nextDouble() * Math.PI * 2;
                double r = 0.5 + world.rand.nextDouble() * 0.5;
                double px = player.posX + Math.cos(angle) * r;
                double pz = player.posZ + Math.sin(angle) * r;
                world.spawnParticle("mobSpell", px, player.boundingBox.minY + 1.0, pz, 0.2, 0.0, 0.8);
            }
        }
    }
}
