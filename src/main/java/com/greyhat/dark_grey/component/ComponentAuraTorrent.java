package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.CooldownHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityAuraTorrent;

public class ComponentAuraTorrent
    implements IRPGComponent, IOnRightClick, IOnHeldTick, IHasTooltip, IOnWeaponUsingTick, IOnPlayerStoppedUsing {

    private static final String LEGACY_COOLDOWN_KEY = "LastAuraTorrentTime";
    private static final String COOLDOWN_END_MILLIS_KEY = "AuraTorrentCooldownEndMillis";

    private float radius = 5.0f;
    private float dotDamage = 250.0f;
    private int durationTicks = 200;
    private int cooldownTicks = 600;
    private static final int MAX_CHARGE_TICKS = 40; // 2 seconds to reach max radius

    @Override
    public String getComponentId() {
        return "灵气洪流";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("radius")) {
            radius = clamp(
                params.get("radius")
                    .getAsFloat(),
                1.0f,
                16.0f);
        }
        if (params.has("dotDamage")) {
            dotDamage = clamp(
                params.get("dotDamage")
                    .getAsFloat(),
                0.0f,
                10000.0f);
        } else if (params.has("damage")) {
            dotDamage = clamp(
                params.get("damage")
                    .getAsFloat(),
                0.0f,
                10000.0f);
        }
        if (params.has("duration")) {
            durationTicks = Math.max(
                20,
                Math.min(
                    20 * 60,
                    params.get("duration")
                        .getAsInt()));
        }
        if (params.has("cooldown")) {
            cooldownTicks = Math.max(
                0,
                Math.min(
                    20 * 60 * 60,
                    params.get("cooldown")
                        .getAsInt()));
        }
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
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
                + String.format("%.1f", cooldownTicks / 20.0f)
                + "\u79D2)");
    }

    @Override
    public ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            if (!itemStack.hasTagCompound()) {
                itemStack.setTagCompound(new NBTTagCompound());
            }
            long cooldownMillis = CooldownHelper.ticksToMillis(this.cooldownTicks);
            long remainingMillis = CooldownHelper.getRemainingMillis(
                itemStack.getTagCompound(),
                COOLDOWN_END_MILLIS_KEY,
                cooldownMillis,
                LEGACY_COOLDOWN_KEY);
            if (remainingMillis > 0L) {
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        net.minecraft.util.EnumChatFormatting.RED + "【灵气洪流】技能冷却中，还需等待 "
                            + String.format("%.1f", remainingMillis / 1000.0D)
                            + " 秒。"));
                player.clearItemInUse();
                return itemStack;
            }
        }

        // Just let them start using the item
        return itemStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        World world = player.worldObj;
        int charge = weaponStack.getItem()
            .getMaxItemUseDuration(weaponStack) - count;
        float currentRadius = Math.min(radius, radius * charge / MAX_CHARGE_TICKS);

        if (world.isRemote) {
            if (world.getTotalWorldTime() % 2 == 0) {
                double cy = player.boundingBox.minY + 1.0;
                int numParticles = 10 + (int) (currentRadius * 8.0);
                for (int i = 0; i < numParticles; i++) {
                    double angle = world.rand.nextDouble() * Math.PI * 2.0;
                    double px = player.posX + Math.cos(angle) * currentRadius;
                    double pz = player.posZ + Math.sin(angle) * currentRadius;
                    com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(world, "largesmoke", px, cy, pz, 0, 0, 0);
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
                        com.greyhat.dark_grey.DarkGrey.proxy.spawnParticle(
                            world,
                            spell,
                            px,
                            cy + world.rand.nextDouble() * 1.5,
                            pz,
                            rColor,
                            gColor,
                            bColor);
                    } else {
                        com.greyhat.dark_grey.DarkGrey.proxy
                            .spawnParticle(world, "witchMagic", px, cy + world.rand.nextDouble() * 1.5, pz, 0, 0, 0);
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
                        EntityAuraTorrent.applyAuraEffect(player, target, dotDamage);
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

        float finalRadius = Math.min(radius, radius * charge / MAX_CHARGE_TICKS);

        if (!world.isRemote) {
            double spawnY = player.boundingBox.minY + 1.0;
            EntityAuraTorrent aura = new EntityAuraTorrent(
                world,
                player,
                player.posX,
                spawnY,
                player.posZ,
                finalRadius,
                dotDamage,
                durationTicks);
            world.spawnEntityInWorld(aura);

            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            CooldownHelper.start(
                stack.getTagCompound(),
                COOLDOWN_END_MILLIS_KEY,
                CooldownHelper.ticksToMillis(this.cooldownTicks),
                LEGACY_COOLDOWN_KEY);
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
                com.greyhat.dark_grey.DarkGrey.proxy
                    .spawnParticle(world, "mobSpell", px, player.boundingBox.minY + 1.0, pz, 0.2, 0.0, 0.8);
            }
        }
    }
}
