package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.GunMagazineHelper;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.api.capability.IOnWeaponUsingTick;
import com.greyhat.dark_grey.entity.EntityHeliosBurningField;
import com.greyhat.dark_grey.entity.EntitySunflameBullet;
import com.greyhat.dark_grey.mark.MarkManager;
import com.greyhat.dark_grey.mark.type.ScorchMarkType;

public class ComponentHeliosBlastGun
    implements IRPGComponent, IOnRightClick, IOnWeaponUsingTick, IOnLeftClick, IHasTooltip {

    private int reloadTicks = 60; // 3 seconds
    private int magazineCapacity = 6;
    private float pelletDamage = 200.0f;
    private int pelletCount = 6;
    private float range = 12.0f;
    private float fanAngleDegrees = 90.0f;
    private float fullDamageDistance = 1.0f;
    private float recoilVelocity = 1.35f;

    @Override
    public String getComponentId() {
        return "赫利俄斯爆破枪";
    }

    public int getReloadTicks() {
        return reloadTicks;
    }

    public int getMagazineCapacity() {
        return magazineCapacity;
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("reloadTicks")) this.reloadTicks = params.get("reloadTicks")
            .getAsInt();
        if (params.has("magazineCapacity")) this.magazineCapacity = params.get("magazineCapacity")
            .getAsInt();
        if (params.has("maxDamage")) {
            this.pelletDamage = params.get("maxDamage")
                .getAsFloat() / 6.0f;
        }
        if (params.has("minDamage")) {
            // Unused
        }
        if (params.has("range")) this.range = params.get("range")
            .getAsFloat();
        if (params.has("fanAngleDegrees")) this.fanAngleDegrees = params.get("fanAngleDegrees")
            .getAsFloat();
        if (params.has("fullDamageDistance")) this.fullDamageDistance = params.get("fullDamageDistance")
            .getAsFloat();
        if (params.has("recoilVelocity")) this.recoilVelocity = params.get("recoilVelocity")
            .getAsFloat();
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

        if (ticksInUse == reloadTicks) {
            if (!player.worldObj.isRemote) {
                int needed = magazineCapacity - ammo;
                int inventoryAmmo = consumeInventoryAmmo(player, needed);
                if (inventoryAmmo > 0 || player.capabilities.isCreativeMode) {
                    int toAdd = player.capabilities.isCreativeMode ? needed : inventoryAmmo;
                    GunMagazineHelper.addAmmo(stack, toAdd, magazineCapacity);
                    player.worldObj.playSoundAtEntity(player, "random.click", 1.0F, 1.2F);
                }
                player.stopUsingItem();
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
                }
            } else {
                player.clearItemInUse();
            }
        }
    }

    private int consumeInventoryAmmo(EntityPlayer player, int maxConsume) {
        int consumed = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack slot = player.inventory.getStackInSlot(i);
            if (slot != null && slot.getItem() != null) {
                if (slot.getUnlocalizedName()
                    .contains("sunflame_round")) {
                    int take = Math.min(maxConsume - consumed, slot.stackSize);
                    player.inventory.decrStackSize(i, take);
                    consumed += take;
                    if (consumed >= maxConsume) {
                        break;
                    }
                }
            }
        }
        return consumed;
    }

    @Override
    public boolean onLeftClick(ItemStack weaponStack, EntityPlayer player) {
        World world = player.worldObj;
        int ammo = GunMagazineHelper.getLoadedAmmo(weaponStack);

        com.greyhat.dark_grey.DarkGrey.LOG.info(
            "[HeliosBlastGun] onLeftClick called. ammo=" + ammo
                + " isRemote="
                + world.isRemote
                + " fanAngle="
                + fanAngleDegrees
                + " pelletDmg="
                + pelletDamage
                + " pelletCount="
                + pelletCount);

        if (ammo < 6) {
            return false;
        }

        if (!world.isRemote) {
            GunMagazineHelper.setLoadedAmmo(weaponStack, 0);

            // Raycast in cone
            Vec3 look = player.getLookVec();
            Vec3 playerPos = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);

            float halfAngle = fanAngleDegrees / 2.0f;

            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
                player.posX - range,
                player.posY - range,
                player.posZ - range,
                player.posX + range,
                player.posY + range,
                player.posZ + range);

            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, aabb);
            for (Entity e : list) {
                if (!(e instanceof EntityLivingBase)) continue;
                EntityLivingBase target = (EntityLivingBase) e;
                if (!CombatTargeting.canDamage(player, target, false)) continue;

                Vec3 targetCenter = Vec3.createVectorHelper(
                    target.posX - player.posX,
                    (target.posY + target.height / 2.0) - (player.posY + player.getEyeHeight()),
                    target.posZ - player.posZ);

                double dist = targetCenter.lengthVector();
                Vec3 dirToTarget = targetCenter.normalize();

                double dot = look.xCoord * dirToTarget.xCoord + look.yCoord * dirToTarget.yCoord
                    + look.zCoord * dirToTarget.zCoord;

                double targetHalfAngle = Math.asin(Math.min(1.0, (target.width / 2.0) / Math.max(dist, 0.5)));
                double currentAngle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));

                if (dist <= 1.0 || currentAngle - targetHalfAngle <= Math.toRadians(halfAngle) + 0.001) {
                    // Check walls
                    if (world.rayTraceBlocks(
                        playerPos,
                        Vec3.createVectorHelper(target.posX, target.posY + target.height / 2.0, target.posZ)) != null
                        && world.rayTraceBlocks(
                            playerPos,
                            Vec3.createVectorHelper(
                                target.posX,
                                target.posY + target.height / 2.0,
                                target.posZ)).typeOfHit
                            == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                        continue;
                    }

                    // Calculate damage based on pellets hitting
                    double effectiveDistance = Math.max(0, dist - target.width / 2.0);
                    float hitChance = 1.0f;
                    if (effectiveDistance > fullDamageDistance) {
                        hitChance = 1.0f
                            - 0.85f * (float) ((effectiveDistance - fullDamageDistance) / (range - fullDamageDistance));
                    }
                    int hits = 0;
                    for (int p = 0; p < pelletCount; p++) {
                        if (world.rand.nextFloat() <= hitChance) hits++;
                    }
                    if (hits == 0) hits = 1; // Guarantee at least 1 hit if caught in cone

                    float dmg = hits * pelletDamage;

                    com.greyhat.dark_grey.DarkGrey.LOG.info(
                        "[HeliosBlastGun] Target=" + target.getCommandSenderName()
                            + " dist="
                            + String.format("%.2f", effectiveDistance)
                            + " hits="
                            + hits
                            + "/"
                            + pelletCount
                            + " dmg="
                            + dmg
                            + " angle="
                            + String.format("%.1f", Math.toDegrees(currentAngle))
                            + " halfAngle="
                            + halfAngle
                            + " hurtResist="
                            + target.hurtResistantTime);

                    DamageSource src = DamageSource.causePlayerDamage(player);
                    if (target.attackEntityFrom(src, dmg)) {
                        int scorchStacks = MarkManager.getStacks(target, ScorchMarkType.ID);
                        if (scorchStacks > 0) {
                            // Scorch Detonation logic (base damage is 10 per stack)
                            float detonationDamage = scorchStacks * 10.0f * 1.25f;
                            target.attackEntityFrom(
                                DamageSource.causePlayerDamage(player)
                                    .setExplosion(),
                                detonationDamage);
                        }
                    }
                }
            }

            // Create Field
            EntityHeliosBurningField field = new EntityHeliosBurningField(world, player);
            world.spawnEntityInWorld(field);

            // Recoil
            player.motionX -= look.xCoord * recoilVelocity;
            player.motionZ -= look.zCoord * recoilVelocity;
            player.velocityChanged = true;

            // FX
            world.playSoundAtEntity(player, "random.explode", 1.0F, 0.5F);

            // Visual trajectories
            for (int i = 0; i < 6; i++) {
                EntitySunflameBullet bullet = new EntitySunflameBullet(world, player, 0);
                bullet.getEntityData()
                    .setBoolean("VisualOnly", true);

                float dyaw = (world.rand.nextFloat() - 0.5f) * fanAngleDegrees;
                float dpitch = (world.rand.nextFloat() - 0.5f) * 15.0f; // tighter vertical spread for shotgun feel

                bullet.setLocationAndAngles(
                    player.posX,
                    player.posY + player.getEyeHeight(),
                    player.posZ,
                    player.rotationYaw + dyaw,
                    player.rotationPitch + dpitch);
                float f = 1.5F;
                bullet.motionX = (double) (-net.minecraft.util.MathHelper
                    .sin(bullet.rotationYaw / 180.0F * (float) Math.PI)
                    * net.minecraft.util.MathHelper.cos(bullet.rotationPitch / 180.0F * (float) Math.PI)
                    * f);
                bullet.motionZ = (double) (net.minecraft.util.MathHelper
                    .cos(bullet.rotationYaw / 180.0F * (float) Math.PI)
                    * net.minecraft.util.MathHelper.cos(bullet.rotationPitch / 180.0F * (float) Math.PI)
                    * f);
                bullet.motionY = (double) (-net.minecraft.util.MathHelper
                    .sin((bullet.rotationPitch) / 180.0F * (float) Math.PI) * f);
                bullet.setThrowableHeading(bullet.motionX, bullet.motionY, bullet.motionZ, 2.0f, 0.0f);
                world.spawnEntityInWorld(bullet);
            }

            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).updateHeldItem();
            }
        }
        return true;
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean advanced) {
        int ammo = GunMagazineHelper.getLoadedAmmo(stack);

        tooltipLines.add(EnumChatFormatting.GOLD + "伤害：" + (int) pelletDamage + " x " + pelletCount);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹匣：" + magazineCapacity);
        tooltipLines.add(EnumChatFormatting.AQUA + "弹药：阳炎弹");
        tooltipLines.add(
            EnumChatFormatting.LIGHT_PURPLE + "[引燃] " + EnumChatFormatting.GRAY + "该武器造成的有效直接攻击可以引爆目标身上的灼痕印记 (125%)");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "长按右键：装填 (" + (reloadTicks / 20.0f) + "秒)");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键：发射");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.RED + "焚烧领域：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  前方生成4x6x4火海，持续6秒");
        tooltipLines.add(EnumChatFormatting.GRAY + "  每0.5秒造成60点伤害，有40%概率施加灼痕");
        tooltipLines.add("");
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前弹药："
                + (ammo > 0 ? (EnumChatFormatting.GREEN + String.valueOf(ammo) + " / " + magazineCapacity)
                    : (EnumChatFormatting.RED + "空")));
    }
}
