package com.greyhat.dark_grey.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.CombatTargeting;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.RPGItemDataManager;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnBowShoot;
import com.greyhat.dark_grey.api.capability.IOnBowUsingTick;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;
import com.greyhat.dark_grey.api.capability.IOnLeftClick;
import com.greyhat.dark_grey.api.capability.IOnRightClick;
import com.greyhat.dark_grey.entity.EntityItanisArrow;
import com.greyhat.dark_grey.entity.EntityItanisArrow.ArrowState;

public class ComponentItanis
    implements IRPGComponent, IOnRightClick, IOnBowUsingTick, IOnBowShoot, IHasTooltip, IOnLeftClick, IOnHeldTick {

    private int normalDrawTicks = 20;
    private float bonusChance1 = 0.85F;
    private float bonusMultiplier1 = 0.35F;

    private float bonusChance2 = 0.65F;
    private float bonusMultiplier2 = 0.60F;

    private float bonusChance3 = 0.25F;
    private float bonusMultiplier3 = 0.90F;

    private int formationIntervalTicks = 20;
    private float formationDamageMultiplier = 0.40F;
    private int maxChargeTicks = 200;

    private float fullChargeDamage = 5000.0F;
    private double targetRange = 32.0D;
    private int formationRemainTicks = 100;

    private final Random rand = new Random();

    @Override
    public String getComponentId() {
        return "伊塔尼斯";
    }

    @Override
    public void configure(JsonObject params) {
        if (params == null) return;

        if (params.has("normalDrawTicks")) normalDrawTicks = params.get("normalDrawTicks")
            .getAsInt();
        if (params.has("bonusChance1")) bonusChance1 = params.get("bonusChance1")
            .getAsFloat();
        if (params.has("bonusMultiplier1")) bonusMultiplier1 = params.get("bonusMultiplier1")
            .getAsFloat();

        if (params.has("bonusChance2")) bonusChance2 = params.get("bonusChance2")
            .getAsFloat();
        if (params.has("bonusMultiplier2")) bonusMultiplier2 = params.get("bonusMultiplier2")
            .getAsFloat();

        if (params.has("bonusChance3")) bonusChance3 = params.get("bonusChance3")
            .getAsFloat();
        if (params.has("bonusMultiplier3")) bonusMultiplier3 = params.get("bonusMultiplier3")
            .getAsFloat();

        if (params.has("formationIntervalTicks")) formationIntervalTicks = params.get("formationIntervalTicks")
            .getAsInt();
        if (params.has("formationDamageMultiplier")) formationDamageMultiplier = params.get("formationDamageMultiplier")
            .getAsFloat();
        if (params.has("maxChargeTicks")) maxChargeTicks = params.get("maxChargeTicks")
            .getAsInt();

        if (params.has("fullChargeDamage")) fullChargeDamage = params.get("fullChargeDamage")
            .getAsFloat();
        if (params.has("targetRange")) targetRange = params.get("targetRange")
            .getAsDouble();
        if (params.has("formationRemainTicks")) formationRemainTicks = params.get("formationRemainTicks")
            .getAsInt();
    }

    private float getBaseDamage(ItemStack stack) {
        RPGItemDataManager.ItemConfig config = RPGItemDataManager.getInstance()
            .getConfig("itanis");
        if (config != null && config.damage > 0) {
            return config.damage;
        }
        return 300.0F;
    }

    @Override
    public boolean onLeftClick(ItemStack stack, EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return true;
        }

        NBTTagCompound nbt = getOrCreateNBT(stack);
        long now = player.worldObj.getTotalWorldTime();
        long lastSwitch = nbt.getLong("ItanisModeSwitchCooldown");

        if (now - lastSwitch < 5) {
            return true; // Cooldown anti-spam
        }

        nbt.setLong("ItanisModeSwitchCooldown", now);
        boolean currentMode = nbt.getBoolean("ItanisChargeMode");
        boolean newMode = !currentMode;
        nbt.setBoolean("ItanisChargeMode", newMode);

        player.worldObj
            .playSoundEffect(player.posX, player.posY, player.posZ, "random.click", 0.6F, newMode ? 1.4F : 1.0F);

        String modeKey = newMode ? "msg.dark_grey.itanis.mode_charge" : "msg.dark_grey.itanis.mode_normal";
        ChatComponentTranslation modeText = new ChatComponentTranslation(modeKey);
        modeText.getChatStyle()
            .setColor(newMode ? EnumChatFormatting.GOLD : EnumChatFormatting.AQUA);

        ChatComponentTranslation prefix = new ChatComponentTranslation("msg.dark_grey.itanis.mode_changed");
        prefix.getChatStyle()
            .setColor(EnumChatFormatting.GRAY);
        prefix.appendSibling(modeText);

        player.addChatMessage(prefix);
        return true;
    }

    @Override
    public ItemStack onRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        return itemStack;
    }

    @Override
    public void onBowUsingTick(ItemStack stack, World world, EntityPlayer player, int count) {
        if (world.isRemote) return;

        NBTTagCompound nbt = getOrCreateNBT(stack);
        boolean chargeMode = nbt.getBoolean("ItanisChargeMode");

        if (!chargeMode) return;

        int chargeTicks = stack.getMaxItemUseDuration() - count;

        // Apply Resistance II (amplifier = 1) while charging
        player.addPotionEffect(new PotionEffect(Potion.resistance.id, 10, 1, true));

        // Spawn floating arrow every 20 ticks (1s) up to 10 max
        if (chargeTicks > 0 && chargeTicks <= maxChargeTicks && chargeTicks % formationIntervalTicks == 0) {
            List<EntityItanisArrow> existingHovering = getPlayerHoveringArrows(world, player);
            if (existingHovering.size() < 10) {
                int slot = existingHovering.size();
                float baseDmg = getBaseDamage(stack);
                float floatDmg = baseDmg * formationDamageMultiplier; // 120.0F

                EntityItanisArrow arrow = new EntityItanisArrow(world, player, floatDmg, ArrowState.HOVERING);
                arrow.setFormationSlot(slot);
                arrow.setFormationTotal(slot + 1);
                world.spawnEntityInWorld(arrow);

                // Update total count on existing hovering arrows
                int newTotal = slot + 1;
                for (EntityItanisArrow existing : existingHovering) {
                    existing.setFormationTotal(newTotal);
                }

                world.playSoundEffect(player.posX, player.posY, player.posZ, "random.orb", 0.5F, 1.2F + slot * 0.05F);
            }
        }

        // Chat message every 1 second (20 ticks)
        if (chargeTicks > 0 && chargeTicks <= maxChargeTicks && chargeTicks % 20 == 0) {
            int seconds = chargeTicks / 20;
            List<EntityItanisArrow> existingHovering = getPlayerHoveringArrows(world, player);
            int currentHovering = existingHovering.size();
            int maxHovering = 10;
            
            net.minecraft.util.ChatComponentText msg = new net.minecraft.util.ChatComponentText(
                "已蓄力" + seconds + "秒 浮游箭(" + currentHovering + "/" + maxHovering + ")");
            msg.getChatStyle().setColor(net.minecraft.util.EnumChatFormatting.GOLD);
            player.addChatMessage(msg);
        }
    }

    @Override
    public boolean onBowShoot(ItemStack stack, World world, EntityPlayer player, int charge) {
        if (world.isRemote) return true;

        NBTTagCompound nbt = getOrCreateNBT(stack);
        boolean chargeMode = nbt.getBoolean("ItanisChargeMode");
        float baseDmg = getBaseDamage(stack);

        if (!chargeMode) {
            if (charge < (normalDrawTicks * 2 / 3)) {
                return true; // Prevent rapid-fire spam
            }

            // Normal Mode Shoot
            EntityItanisArrow mainArrow = new EntityItanisArrow(world, player, baseDmg, ArrowState.LAUNCHED);
            Vec3 look = player.getLookVec();
            mainArrow.motionX = look.xCoord * 3.0D;
            mainArrow.motionY = look.yCoord * 3.5D;
            mainArrow.motionZ = look.zCoord * 3.0D;
            world.spawnEntityInWorld(mainArrow);

            // Search available nearby targets to distribute extra arrows
            List<EntityLivingBase> nearbyTargets = searchNearbyTargets(world, player, targetRange);

            // 3 Independent bonus arrow checks
            spawnBonusArrowIfRolled(world, player, baseDmg * bonusMultiplier1, bonusChance1, nearbyTargets, 0);
            spawnBonusArrowIfRolled(world, player, baseDmg * bonusMultiplier2, bonusChance2, nearbyTargets, 1);
            spawnBonusArrowIfRolled(world, player, baseDmg * bonusMultiplier3, bonusChance3, nearbyTargets, 2);

            world.playSoundEffect(player.posX, player.posY, player.posZ, "random.bow", 1.0F, 1.0F);
        } else {
            if (charge >= maxChargeTicks) {
                // Spawn 5000 damage Piercing Arrow
                EntityItanisArrow piercingArrow = new EntityItanisArrow(
                    world,
                    player,
                    fullChargeDamage,
                    ArrowState.PIERCING);
                Vec3 look = player.getLookVec();
                piercingArrow.motionX = look.xCoord * 3.5D;
                piercingArrow.motionY = look.yCoord * 3.5D;
                piercingArrow.motionZ = look.zCoord * 3.5D;
                world.spawnEntityInWorld(piercingArrow);

                world.playSoundEffect(player.posX, player.posY, player.posZ, "random.explode", 1.2F, 0.8F);
            }

            // Notify hovering arrows that bow usage finished so they retain for 5s
            List<EntityItanisArrow> hovering = getPlayerHoveringArrows(world, player);
            for (EntityItanisArrow arrow : hovering) {
                arrow.setOwnerUsingBow(false);
            }
        }
        return true;
    }

    private void spawnBonusArrowIfRolled(World world, EntityPlayer player, float damage, float chance,
        List<EntityLivingBase> targets, int targetIndex) {
        if (rand.nextFloat() < chance) {
            EntityItanisArrow bonusArrow = new EntityItanisArrow(world, player, damage, ArrowState.LAUNCHED);
            Vec3 look = player.getLookVec();

            // Slight spread velocity
            double spreadX = (rand.nextDouble() - 0.5D) * 0.4D;
            double spreadY = (rand.nextDouble() - 0.5D) * 0.4D;
            double spreadZ = (rand.nextDouble() - 0.5D) * 0.4D;

            bonusArrow.motionX = (look.xCoord + spreadX) * 2.8D;
            bonusArrow.motionY = (look.yCoord + spreadY) * 2.8D;
            bonusArrow.motionZ = (look.zCoord + spreadZ) * 2.8D;

            if (targets != null && !targets.isEmpty()) {
                EntityLivingBase assignedTarget = targets.get(targetIndex % targets.size());
                bonusArrow.setTargetEntityId(assignedTarget.getEntityId());
            }

            world.spawnEntityInWorld(bonusArrow);
        }
    }

    private List<EntityLivingBase> searchNearbyTargets(World world, EntityPlayer owner, double range) {
        AxisAlignedBB box = owner.boundingBox.expand(range, range, range);
        @SuppressWarnings("unchecked")
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(owner, box);
        List<EntityLivingBase> validTargets = new ArrayList<>();
        for (Entity e : list) {
            if (e instanceof EntityLivingBase && e.isEntityAlive()) {
                EntityLivingBase living = (EntityLivingBase) e;
                if (CombatTargeting.canDamage(owner, living, false)) {
                    validTargets.add(living);
                }
            }
        }
        return validTargets;
    }

    @SuppressWarnings("unchecked")
    private List<EntityItanisArrow> getPlayerHoveringArrows(World world, EntityPlayer player) {
        AxisAlignedBB box = player.boundingBox.expand(32.0D, 32.0D, 32.0D);
        List rawList = world.getEntitiesWithinAABB(EntityItanisArrow.class, box);
        List<EntityItanisArrow> result = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof EntityItanisArrow) {
                EntityItanisArrow arrow = (EntityItanisArrow) obj;
                if (arrow.getThrower() == player && arrow.getArrowState() == ArrowState.HOVERING) {
                    result.add(arrow);
                }
            }
        }
        return result;
    }

    @Override
    public void onHeldTick(ItemStack stack, World world, EntityPlayer player) {
        // Reserved for held tick state validation
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltipLines, boolean showAdvanced) {
        float baseDmg = getBaseDamage(stack);
        boolean chargeMode = stack.hasTagCompound() && stack.getTagCompound()
            .getBoolean("ItanisChargeMode");

        tooltipLines.add(EnumChatFormatting.GOLD + "基础伤害：" + (int) baseDmg);
        tooltipLines.add(EnumChatFormatting.AQUA + "所有箭矢自动追踪");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.YELLOW + "左键：切换普通/蓄力模式");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.GREEN + "普通模式：");
        tooltipLines.add(EnumChatFormatting.GRAY + "  正常拉弓速度");
        tooltipLines.add(EnumChatFormatting.GRAY + "  85%概率追加一根 " + (int) (baseDmg * bonusMultiplier1) + " 伤害箭");
        tooltipLines.add(EnumChatFormatting.GRAY + "  65%概率追加一根 " + (int) (baseDmg * bonusMultiplier2) + " 伤害箭");
        tooltipLines.add(EnumChatFormatting.GRAY + "  25%概率追加一根 " + (int) (baseDmg * bonusMultiplier3) + " 伤害箭");
        tooltipLines.add(EnumChatFormatting.DARK_GRAY + "  概率独立计算");
        tooltipLines.add("");
        tooltipLines.add(EnumChatFormatting.LIGHT_PURPLE + "蓄力模式：");
        tooltipLines
            .add(EnumChatFormatting.GRAY + "  每1秒生成一根 " + (int) (baseDmg * formationDamageMultiplier) + " 伤害浮游箭");
        tooltipLines.add(EnumChatFormatting.GRAY + "  浮游箭发现目标后自动发射");
        tooltipLines.add(EnumChatFormatting.GRAY + "  蓄力期间获得抗性提升II");
        tooltipLines.add(EnumChatFormatting.GRAY + "  最大蓄力时间：10秒");
        tooltipLines.add(EnumChatFormatting.GRAY + "  满蓄力伤害：" + (int) fullChargeDamage);
        tooltipLines.add(EnumChatFormatting.GRAY + "  满蓄力箭可以贯穿怪物");
        tooltipLines.add("");
        tooltipLines.add(
            EnumChatFormatting.WHITE + "当前模式："
                + (chargeMode ? EnumChatFormatting.GOLD + "蓄力模式" : EnumChatFormatting.AQUA + "普通模式"));
    }

    private NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
