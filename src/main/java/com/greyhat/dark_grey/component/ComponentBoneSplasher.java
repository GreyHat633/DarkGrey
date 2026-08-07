package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.GunMagazineHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnPlayerStoppedUsing;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;

import cpw.mods.fml.common.registry.GameRegistry;

public class ComponentBoneSplasher
    implements IRPGComponent, IOnRightClick, IOnWeaponUsingTick, IOnPlayerStoppedUsing, IOnLeftClick, IHasTooltip {

    private int reloadTicks = 40;
    private String ammoItemId = "hardened_bone_marrow";
    private int ammoPerReload = 1;
    private int magazineCapacity = 1;

    private float fanAngleDegrees = 45.0f;
    private float range = 4.0f;
    private float verticalHalfHeight = 3.0f;

    private float maxDamage = 30.0f;
    private float minDamage = 10.0f;
    private float fullDamageDistance = 1.0f;

    private String fractureMarkId = "fracture";
    private int fractureStacks = 1;
    private boolean respectWalls = true;
    private boolean consumeAmmoInCreative = false;

    public int getReloadTicks() {
        return this.reloadTicks;
    }

    public int getMagazineCapacity() {
        return this.magazineCapacity;
    }

    @Override
    public String getComponentId() {
        return "碎骨喷溅者";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("reloadTicks")) this.reloadTicks = params.get("reloadTicks")
            .getAsInt();
        if (params.has("ammoItemId")) this.ammoItemId = params.get("ammoItemId")
            .getAsString();
        if (params.has("ammoPerReload")) this.ammoPerReload = params.get("ammoPerReload")
            .getAsInt();
        if (params.has("magazineCapacity")) this.magazineCapacity = params.get("magazineCapacity")
            .getAsInt();

        if (params.has("fanAngleDegrees")) this.fanAngleDegrees = params.get("fanAngleDegrees")
            .getAsFloat();
        if (params.has("range")) this.range = params.get("range")
            .getAsFloat();
        if (params.has("verticalHalfHeight")) this.verticalHalfHeight = params.get("verticalHalfHeight")
            .getAsFloat();

        if (params.has("maxDamage")) this.maxDamage = params.get("maxDamage")
            .getAsFloat();
        if (params.has("minDamage")) this.minDamage = params.get("minDamage")
            .getAsFloat();
        if (params.has("fullDamageDistance")) this.fullDamageDistance = params.get("fullDamageDistance")
            .getAsFloat();

        if (params.has("fractureMarkId")) this.fractureMarkId = params.get("fractureMarkId")
            .getAsString();
        if (params.has("fractureStacks")) this.fractureStacks = params.get("fractureStacks")
            .getAsInt();

        if (params.has("respectWalls")) this.respectWalls = params.get("respectWalls")
            .getAsBoolean();
        if (params.has("consumeAmmoInCreative")) this.consumeAmmoInCreative = params.get("consumeAmmoInCreative")
            .getAsBoolean();

        // Boundaries safety
        this.reloadTicks = Math.max(1, Math.min(72000, this.reloadTicks));
        this.ammoPerReload = Math.max(1, Math.min(64, this.ammoPerReload));
        this.magazineCapacity = Math.max(1, Math.min(64, this.magazineCapacity));
        this.fanAngleDegrees = Math.max(1.0f, Math.min(360.0f, this.fanAngleDegrees));
        this.range = Math.max(0.5f, Math.min(64.0f, this.range));
        this.verticalHalfHeight = Math.max(0.5f, Math.min(64.0f, this.verticalHalfHeight));
        this.maxDamage = Math.max(0.0f, this.maxDamage);
        this.minDamage = Math.max(0.0f, this.minDamage);
        if (this.minDamage > this.maxDamage) this.minDamage = this.maxDamage;
        this.fullDamageDistance = Math.max(0.0f, Math.min(this.range, this.fullDamageDistance));
        this.fractureStacks = Math.max(0, Math.min(100, this.fractureStacks));
    }

    @Override
    public ItemStack onRightClick(ItemStack weaponStack, World world, EntityPlayer player) {
        // Hot-reload clamp
        GunMagazineHelper.clampAmmo(weaponStack, this.magazineCapacity);
        int loaded = GunMagazineHelper.getLoadedAmmo(weaponStack);

        if (loaded >= this.magazineCapacity) {
            // Full, deny reload
            if (!world.isRemote) {
                // Play a generic tick/deny sound if desired, but for now we just do nothing
            }
        } else {
            // Not full, enter reloading state
            player.setItemInUse(
                weaponStack,
                weaponStack.getItem()
                    .getMaxItemUseDuration(weaponStack));
        }

        // We return the stack, but we did NOT fire, so we let the gun enter Use state.
        return weaponStack;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        int maxDuration = stack.getItem()
            .getMaxItemUseDuration(stack);
        int ticksInUse = maxDuration - count;

        if (ticksInUse == this.reloadTicks) {
            boolean hasAmmo = false;
            Item ammoItem = GameRegistry.findItem("dark_grey", this.ammoItemId);

            if (player.capabilities.isCreativeMode && !this.consumeAmmoInCreative) {
                hasAmmo = true;
            } else if (ammoItem != null && player.inventory.consumeInventoryItem(ammoItem)) {
                hasAmmo = true;
            }

            if (hasAmmo) {
                if (!player.worldObj.isRemote) {
                    GunMagazineHelper.addAmmo(stack, this.ammoPerReload, this.magazineCapacity);
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
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int timeLeft) {
        // If interrupted before reloadTicks is reached, do nothing. Ammo is not consumed.
    }

    @Override
    public boolean onLeftClick(ItemStack weaponStack, EntityPlayer player) {
        World world = player.worldObj;
        GunMagazineHelper.clampAmmo(weaponStack, this.magazineCapacity);

        if (GunMagazineHelper.getLoadedAmmo(weaponStack) <= 0) {
            return false;
        }

        if (!world.isRemote) {
            // Consume 1 ammo
            GunMagazineHelper.consumeAmmo(weaponStack, 1);
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
            }

            // Fire logic
            Vec3 look = player.getLookVec();
            Vec3 lookH = Vec3.createVectorHelper(look.xCoord, 0, look.zCoord)
                .normalize();

            double halfAngleRad = Math.toRadians(this.fanAngleDegrees / 2.0);

            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                player.posX - this.range,
                player.posY - this.verticalHalfHeight,
                player.posZ - this.range,
                player.posX + this.range,
                player.posY + this.verticalHalfHeight,
                player.posZ + this.range);

            List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
            DamageSource ds = DamageSource.causePlayerDamage(player);

            for (EntityLivingBase target : targets) {
                if (target == player) continue;
                if (target.isDead || target.getHealth() <= 0) continue;
                if (!CombatTargeting.canDamage(player, target, false)) continue;

                if (this.respectWalls && !player.canEntityBeSeen(target)) continue;

                double dx = target.posX - player.posX;
                double dz = target.posZ - player.posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);

                // SKILL.md rule: distance broadness for large mobs
                double effectiveDist = Math.max(0, dist - target.width / 2.0);
                if (effectiveDist > this.range) continue;

                boolean hit = false;
                if (dist < 0.001) {
                    hit = true;
                } else {
                    Vec3 targetDir = Vec3.createVectorHelper(dx / dist, 0, dz / dist);
                    double dot = lookH.dotProduct(targetDir);

                    // SKILL.md rule: angle broadness for large mobs
                    double targetHalfAngle = Math.asin(Math.min(1.0, (target.width / 2.0) / Math.max(dist, 0.5)));
                    double currentAngle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));

                    if (currentAngle - targetHalfAngle <= halfAngleRad + 0.001) {
                        hit = true;
                    }
                }

                if (hit) {
                    float damage = calculateDamage(effectiveDist);
                    // Do actual damage
                    if (target.attackEntityFrom(ds, damage)) {
                        // Apply fracture
                        if (this.fractureStacks > 0) {
                            MarkApplyContext ctx = new MarkApplyContext.Builder().source(player)
                                .requestedStacks(this.fractureStacks)
                                .applicationId("bone_splasher")
                                .stableDurationTicks(100)
                                .worldTime(target.worldObj.getTotalWorldTime())
                                .refreshDuration(true)
                                .build();
                            MarkManager.apply(target, this.fractureMarkId, ctx);
                        }
                    }
                }
            }

            world.playSoundAtEntity(player, "random.explode", 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);

            // Visuals
            spawnParticles((WorldServer) world, player, lookH);
        }

        return true; // cancel sword melee
    }

    private float calculateDamage(double distance) {
        if (distance <= this.fullDamageDistance) return this.maxDamage;
        if (distance >= this.range) return this.minDamage;

        double t = (distance - this.fullDamageDistance) / (this.range - this.fullDamageDistance);
        return (float) (this.maxDamage + (this.minDamage - this.maxDamage) * t);
    }

    private void spawnParticles(WorldServer world, EntityPlayer player, Vec3 lookH) {
        int count = 30;

        Item ammoItem = GameRegistry.findItem("dark_grey", this.ammoItemId);
        String pName = "iconcrack_352"; // default bone
        if (ammoItem != null) {
            pName = "iconcrack_" + Item.getIdFromItem(ammoItem);
        }

        Vec3 look = player.getLookVec();

        for (int i = 0; i < count; i++) {
            // Randomize angle within fanAngleDegrees (yaw) and pitch spread
            double rYaw = Math.toRadians(player.rotationYaw + (world.rand.nextDouble() - 0.5) * this.fanAngleDegrees);
            double rPitch = Math.toRadians(player.rotationPitch + (world.rand.nextDouble() - 0.5) * 12.0);

            double dirX = -Math.sin(rYaw) * Math.cos(rPitch);
            double dirY = -Math.sin(rPitch);
            double dirZ = Math.cos(rYaw) * Math.cos(rPitch);

            // Muzzle spawn position slightly in front of player
            double px = player.posX + look.xCoord * 0.6 + (world.rand.nextDouble() - 0.5) * 0.1;
            double py = player.posY + player.getEyeHeight()
                - 0.15
                + look.yCoord * 0.6
                + (world.rand.nextDouble() - 0.5) * 0.1;
            double pz = player.posZ + look.zCoord * 0.6 + (world.rand.nextDouble() - 0.5) * 0.1;

            // Speed of shrapnel shooting forward
            double speed = 0.6 + world.rand.nextDouble() * 0.6; // 0.6 to 1.2 blocks/tick

            // Note: count = 0 forces Minecraft client to interpret (dirX, dirY, dirZ) * speed as exact velocity vector!
            world.func_147487_a(pName, px, py, pz, 0, dirX, dirY, dirZ, speed);
        }

        // Spawn smoke particles in the AOE cone
        int smokeCount = 30;
        for (int i = 0; i < smokeCount; i++) {
            double angleOffset = (world.rand.nextDouble() - 0.5) * Math.toRadians(this.fanAngleDegrees);
            double dist = world.rand.nextDouble() * this.range;
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);
            double dirX = lookH.xCoord * cos - lookH.zCoord * sin;
            double dirZ = lookH.xCoord * sin + lookH.zCoord * cos;

            double px = player.posX + dirX * dist;
            double py = player.posY + player.getEyeHeight()
                - 0.2
                + (world.rand.nextDouble() - 0.5) * this.verticalHalfHeight;
            double pz = player.posZ + dirZ * dist;

            world.func_147487_a("largesmoke", px, py, pz, 1, 0, 0.05, 0, 0);
        }
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        tooltipLines.add(EnumChatFormatting.GOLD + "伤害：" + (int) minDamage + "-" + (int) maxDamage);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹匣：" + magazineCapacity);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹药：硬化骨髓");
        tooltipLines.add("");
        tooltipLines
            .add(EnumChatFormatting.YELLOW + "长按右键：装填 (" + String.format("%.1f", this.reloadTicks / 20.0f) + "秒)");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键：发射散弹");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "碎骨倾泻：");
        tooltipLines.add(
            EnumChatFormatting.GRAY + "  向正前方 "
                + (int) this.fanAngleDegrees
                + "度的扇形区域倾泻碎骨，最远可达 "
                + (int) this.range
                + " 格");
        tooltipLines.add(EnumChatFormatting.GRAY + "  被命中的敌人将被施加 " + this.fractureStacks + " 层【骨折】印记");
        tooltipLines.add("");

        int loaded = GunMagazineHelper.getLoadedAmmo(stack);
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前弹药："
                + (loaded > 0 ? (EnumChatFormatting.GREEN + String.valueOf(loaded) + " / " + this.magazineCapacity)
                    : (EnumChatFormatting.RED + "空")));
    }
}
