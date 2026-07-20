package com.greyhat.dark_grey.component;

import java.util.Collection;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IModifyMeleeDamage;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;

/** Adds a configurable weapon-attack-scaled burst to an eligible direct melee hit. */
public class HeavyStrikeComponent implements IRPGComponent, IModifyMeleeDamage, IOnHeldTick, IHasTooltip {

    private static final String LAST_TRIGGER_TICK = "darkgrey_heavy_strike_last_tick";
    private static final String READY_NOTIFIED = "darkgrey_heavy_strike_ready_notified";
    private static final float MAX_INTERVAL_SECONDS = 3600.0F;
    private static final float MAX_MULTIPLIER = 1000000.0F;

    private float intervalSeconds = 5.0F;
    private float multiplier = 4.0F;

    @Override
    public String getComponentId() {
        return "重击";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("intervalSeconds")) {
            this.intervalSeconds = clampFinite(
                params.get("intervalSeconds")
                    .getAsFloat(),
                0.0F,
                MAX_INTERVAL_SECONDS,
                5.0F);
        }
        if (params.has("multiplier")) {
            this.multiplier = clampFinite(
                params.get("multiplier")
                    .getAsFloat(),
                0.0F,
                MAX_MULTIPLIER,
                4.0F);
        }
    }

    static float clampFinite(float value, float min, float max, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public float modifyMeleeDamage(ItemStack weaponStack, EntityLivingBase attacker, EntityLivingBase target,
        DamageSource source, float currentDamage) {
        if (attacker.worldObj == null || attacker.worldObj.isRemote
            || target.isDead
            || target.getHealth() <= 0.0F
            || this.multiplier <= 0.0F) {
            return currentDamage;
        }

        float weaponAttackDamage = resolveWeaponAttackDamage(weaponStack);
        if (weaponAttackDamage <= 0.0F) {
            return currentDamage;
        }

        NBTTagCompound tag = weaponStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            weaponStack.setTagCompound(tag);
        }
        return applyScaledBonus(
            tag,
            LAST_TRIGGER_TICK,
            READY_NOTIFIED,
            attacker.worldObj.getTotalWorldTime(),
            this.intervalSeconds,
            weaponAttackDamage,
            this.multiplier,
            currentDamage);
    }

    /**
     * Resolves only the attack-damage contribution supplied by the held item
     * stack. Player base attributes, potion effects, critical hits and the
     * already-modified event damage are intentionally excluded.
     */
    static float resolveWeaponAttackDamage(ItemStack weaponStack) {
        if (weaponStack == null || weaponStack.getItem() == null) {
            return 0.0F;
        }

        Multimap<String, AttributeModifier> modifiers = weaponStack.getAttributeModifiers();
        if (modifiers == null) {
            return 0.0F;
        }
        Collection<AttributeModifier> attackModifiers = modifiers
            .get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());
        if (attackModifiers == null || attackModifiers.isEmpty()) {
            return 0.0F;
        }

        double operationZero = 0.0D;
        for (AttributeModifier modifier : attackModifiers) {
            if (modifier != null && modifier.getOperation() == 0 && isFinite(modifier.getAmount())) {
                operationZero += modifier.getAmount();
            }
        }

        double itemAttack = operationZero;
        for (AttributeModifier modifier : attackModifiers) {
            if (modifier != null && modifier.getOperation() == 1 && isFinite(modifier.getAmount())) {
                itemAttack += operationZero * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : attackModifiers) {
            if (modifier != null && modifier.getOperation() == 2 && isFinite(modifier.getAmount())) {
                itemAttack *= 1.0D + modifier.getAmount();
            }
        }

        if (!isFinite(itemAttack) || itemAttack <= 0.0D) {
            return 0.0F;
        }
        return itemAttack >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) itemAttack;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    static float applyScaledBonus(NBTTagCompound state, String lastTriggerKey, String readyNotifiedKey, long now,
        float intervalSeconds, float weaponAttackDamage, float multiplier, float currentDamage) {
        if (state == null || weaponAttackDamage <= 0.0F || multiplier <= 0.0F) {
            return currentDamage;
        }

        long intervalTicks = Math.max(0L, Math.round(intervalSeconds * 20.0F));
        if (state.hasKey(lastTriggerKey)) {
            long lastTrigger = state.getLong(lastTriggerKey);
            if (now >= lastTrigger && now - lastTrigger < intervalTicks) {
                return currentDamage;
            }
        }

        double bonusDamage = (double) weaponAttackDamage * multiplier;
        double modified = (double) currentDamage + bonusDamage;
        if (!isFinite(bonusDamage) || bonusDamage <= 0.0D || Double.isNaN(modified)) {
            return currentDamage;
        }
        state.setLong(lastTriggerKey, now);
        state.setBoolean(readyNotifiedKey, false);
        return modified >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) modified;
    }

    static void notifyWhenReady(NBTTagCompound state, String lastTriggerKey, String readyNotifiedKey, long now,
        float intervalSeconds, EntityPlayer player, String displayName) {
        if (state == null || player == null || state.getBoolean(readyNotifiedKey) || !state.hasKey(lastTriggerKey)) {
            return;
        }

        long lastTrigger = state.getLong(lastTriggerKey);
        long intervalTicks = Math.max(0L, Math.round(intervalSeconds * 20.0F));
        if (now >= lastTrigger && now - lastTrigger < intervalTicks) {
            return;
        }

        state.setBoolean(readyNotifiedKey, true);
        player.addChatMessage(
            new ChatComponentText(
                "\u00A76[" + displayName
                    + "] \u00A7e\u51B7\u5374\u7ED3\u675F\uFF0C\u4E0B\u4E00\u6B21\u8FD1\u6218\u547D\u4E2D\u5C06\u89E6\u53D1\u91CD\u51FB\u3002"));
    }

    @Override
    public void onHeldTick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote || weaponStack == null || !weaponStack.hasTagCompound()) {
            return;
        }
        notifyWhenReady(
            weaponStack.getTagCompound(),
            LAST_TRIGGER_TICK,
            READY_NOTIFIED,
            world.getTotalWorldTime(),
            this.intervalSeconds,
            player,
            "\u91CD\u51FB");
    }

    @Override
    public boolean bypassesArmor() {
        return false;
    }

    @Override
    public boolean isAbsoluteDamage() {
        return false;
    }

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        tooltip.add(
            "\u00A76\u2694 \u91CD\u51FB \u00A77| \u00A7e\u6BCF " + String.format("%.1f", this.intervalSeconds)
                + " \u79D2\uFF0C\u4E0B\u4E00\u6B21\u8FD1\u6218\u547D\u4E2D\u989D\u5916\u9020\u6210\u5F53\u524D\u624B\u6301\u6B66\u5668\u653B\u51FB\u529B\u7684 "
                + String.format("%.1f", this.multiplier)
                + " \u500D\u4F24\u5BB3");
        tooltip.add("\u00A78   \u51B7\u5374\u7ED3\u675F\u65F6\u4F1A\u5728\u804A\u5929\u680F\u63D0\u793A");
    }
}
