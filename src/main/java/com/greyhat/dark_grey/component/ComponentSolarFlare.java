package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityPhantomStrike;

public class ComponentSolarFlare
    implements IRPGComponent, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IOnRightClick, IHasTooltip {

    @Override
    public String getComponentId() {
        return "耀斑";
    }

    @Override
    public void configure(JsonObject params) {}

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean advanced) {
        tooltipLines.add("§6[主动技能: 耀斑冲锋]");
        tooltipLines.add("§7长按右键进行冲锋突进，跨越地形障碍");
        tooltipLines.add("§7撞击敌人时自动触发残影贯穿，造成额外 §c600% §7伤害");
        tooltipLines.add("§7并为目标施加持续5秒的 [焦灼印记]");
        tooltipLines.add("§8(撞击墙壁会中断冲锋)");
        tooltipLines.add("");
        tooltipLines.add("§e[连击: 耀斑引爆]");
        tooltipLines.add("§7普通攻击命中带有印记的敌人时，将引爆印记");
        tooltipLines.add("§7造成周围范围性的毁灭伤害，并引燃目标");
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        player.getEntityData()
            .setBoolean("SolarDashHasHit", false);
        return weaponStack;
    }

    @Override
    public void onUsingTick(ItemStack weaponStack, EntityPlayer player, int count) {
        // If we have already hit something, do not apply dash velocity, just let the player hold the item and rebound.
        if (player.getEntityData()
            .getBoolean("SolarDashHasHit")) {
            return;
        }

        int dashTick = weaponStack.getItem()
            .getMaxItemUseDuration(weaponStack) - count;

        player.stepHeight = 1.0F;
        player.moveForward = 0.0F;
        player.moveStrafing = 0.0F;

        Vec3 look = player.getLookVec();
        double progress = Math.min(1.0, dashTick / 100.0);

        // To prevent speed resetting to 0, we take the MAX of their current speed and the charge speed
        double currentHorizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double chargeSpeed = Math.max(currentHorizontalSpeed, 1.5 * progress);

        player.motionX = look.xCoord * chargeSpeed;
        player.motionZ = look.zCoord * chargeSpeed;

        // Calculate the actual horizontal velocity applied to the player.
        // This accounts for the player's pitch (looking down = low horizontal speed).
        double actualHorizontalSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
        double speedThreshold = 0.6;

        World world = player.worldObj;

        // Spawn particles if speed threshold is met
        if (world.isRemote && actualHorizontalSpeed >= speedThreshold) {
            for (int i = 0; i < 6; i++) {
                // Offset particles FORWARD so they don't just trail behind due to player's high speed.
                // This makes them wrap around the player's vision in 1st person!
                double forwardOffset = world.rand.nextDouble() * 1.5; // [0.0, 1.5] blocks ahead
                double px = player.posX + (look.xCoord * forwardOffset) + (world.rand.nextDouble() - 0.5) * 1.5;
                // True waist/chest level in bounding box coordinates: 0.8 to 1.3
                double py = player.boundingBox.minY + 0.8 + (world.rand.nextDouble() * 0.5);
                double pz = player.posZ + (look.zCoord * forwardOffset) + (world.rand.nextDouble() - 0.5) * 1.5;
                world.spawnParticle("flame", px, py, pz, 0.0, 0.05, 0.0);
                if (world.rand.nextInt(3) == 0) {
                    world.spawnParticle("lava", px, py, pz, 0.0, 0.0, 0.0);
                }
            }
        }

        // Wall collision check.
        // 必须同时要求真实冲刺速度达标：isCollidedHorizontally 在服务端只随移动包更新，
        // 玩家静止/贴墙起手时是陈旧的 true——若低速就置命中标志，服务端会在第 1 tick
        // 被顶部的早退锁死，整场冲锋无伤害（客户端照常反弹+音效）。低速碰撞直接忽略。
        if (player.isCollidedHorizontally && actualHorizontalSpeed >= speedThreshold) {
            player.stepHeight = 0.5F;
            player.getEntityData()
                .setBoolean("SolarDashHasHit", true);

            // Capture exact movement direction BEFORE recoil
            Vec3 dashDir = Vec3.createVectorHelper(player.motionX, player.motionY, player.motionZ)
                .normalize();

            // High speed wall crash: Recoil, Sound, and Phantom
            player.motionX = -look.xCoord * 0.8;
            player.motionY = 0.4;
            player.motionZ = -look.zCoord * 0.8;

            player.playSound("random.anvil_land", 1.0F, 0.8F);
            player.playSound("mob.wither.shoot", 1.0F, 0.5F);

            if (!world.isRemote) {
                float rawDamage = 1.0F;
                if (player.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                    rawDamage = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                        .getAttributeValue();
                }
                float totalDamage = rawDamage * 6.0F;
                EntityPhantomStrike phantom = new EntityPhantomStrike(world, player, totalDamage, dashDir);
                world.spawnEntityInWorld(phantom);
            }
            return;
        }

        // Entity collision check
        if (actualHorizontalSpeed >= speedThreshold && !player.getEntityData()
            .getBoolean("SolarDashHasHit")) {
            AxisAlignedBB aabb = player.boundingBox.expand(1.0, 1.0, 1.0);
            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, aabb);

            EntityLivingBase target = null;
            for (Entity e : list) {
                if (e instanceof EntityLivingBase) {
                    target = (EntityLivingBase) e;
                    break; // Just hit the first one
                }
            }

            if (target != null) {
                // We hit something!
                player.getEntityData()
                    .setBoolean("SolarDashHasHit", true);

                // Capture exact movement direction BEFORE recoil
                Vec3 dashDir = Vec3.createVectorHelper(player.motionX, player.motionY, player.motionZ)
                    .normalize();

                // Recoil (bounce back)
                player.motionX = -look.xCoord * 0.8;
                player.motionY = 0.4;
                player.motionZ = -look.zCoord * 0.8;
                player.stepHeight = 0.5F;

                // Audio and Visuals on Client as well for immediate feedback
                player.playSound("random.explode", 1.0F, 1.0F);
                player.playSound("mob.wither.shoot", 1.0F, 0.5F);

                if (!world.isRemote) {
                    // Get raw damage
                    float rawDamage = 1.0F;
                    if (player.getEntityAttribute(SharedMonsterAttributes.attackDamage) != null) {
                        rawDamage = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                            .getAttributeValue();
                    }

                    // Apply Scorched Mark
                    com.greyhat.dark_grey.event.ScorchedMarkTracker.mark(target, 100);

                    // Deal 600% damage
                    float totalDamage = rawDamage * 6.0F;
                    DamageSource source = DamageSource.causePlayerDamage(player)
                        .setMagicDamage();
                    target.attackEntityFrom(source, totalDamage);

                    // Knockback target
                    target.addVelocity(look.xCoord * 1.5, 0.5, look.zCoord * 1.5);

                    // Spawn Phantom Strike on server to actually deal damage
                    EntityPhantomStrike phantom = new EntityPhantomStrike(world, player, totalDamage, dashDir);
                    world.spawnEntityInWorld(phantom);
                }
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        player.stepHeight = 0.5F;
        player.getEntityData()
            .setBoolean("SolarDashHasHit", false);
    }

}
