//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.SetBonusManager;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.ISetComponent;

public class ComponentSupernovaSet implements ISetComponent, IHasTooltip {

    private static final String LAST_TRIGGER_TICK = "darkgrey_supernova_heavy_strike_last_tick";
    private static final String READY_NOTIFIED = "darkgrey_supernova_heavy_strike_ready_notified";

    private float intervalSeconds;
    private float multiplier;
    private int buffDuration;
    private int buffId;
    private int buffAmplifier;

    public ComponentSupernovaSet() {
        this.intervalSeconds = 5.0f;
        this.multiplier = 4.0f;
        this.buffDuration = 200;
        this.buffId = 5;
        this.buffAmplifier = 1;
    }

    @Override
    public void configure(final JsonObject params) {
        if (params.has("intervalSeconds")) {
            this.intervalSeconds = HeavyStrikeComponent.clampFinite(
                params.get("intervalSeconds")
                    .getAsFloat(),
                0.0F,
                3600.0F,
                5.0F);
        }
        if (params.has("multiplier")) {
            this.multiplier = HeavyStrikeComponent.clampFinite(
                params.get("multiplier")
                    .getAsFloat(),
                0.0F,
                1000000.0F,
                4.0F);
        }
        if (params.has("buffDuration")) {
            this.buffDuration = params.get("buffDuration")
                .getAsInt();
        }
        if (params.has("buffId")) {
            this.buffId = params.get("buffId")
                .getAsInt();
        }
        if (params.has("buffAmplifier")) {
            this.buffAmplifier = params.get("buffAmplifier")
                .getAsInt();
        }
    }

    @Override
    public String getSetId() {
        return "supernova_set";
    }

    @Override
    public float onSetHit(final EntityPlayer attacker, final EntityLivingBase target, final float rawDamage,
        final int pieceCount) {
        if (pieceCount >= 2) {
            ItemStack heldStack = attacker.getCurrentEquippedItem();
            float weaponAttackDamage = HeavyStrikeComponent.resolveWeaponAttackDamage(heldStack);
            if (weaponAttackDamage <= 0.0F || this.multiplier <= 0.0F) {
                return rawDamage;
            }
            NBTTagCompound state = attacker.getEntityData();
            return HeavyStrikeComponent.applyScaledBonus(
                state,
                LAST_TRIGGER_TICK,
                READY_NOTIFIED,
                attacker.worldObj.getTotalWorldTime(),
                this.intervalSeconds,
                weaponAttackDamage,
                this.multiplier,
                rawDamage);
        }
        return rawDamage;
    }

    @Override
    public void onSetTick(final EntityPlayer player, final int pieceCount) {
        if (pieceCount < 2 || player.worldObj == null || player.worldObj.isRemote) {
            return;
        }
        HeavyStrikeComponent.notifyWhenReady(
            player.getEntityData(),
            LAST_TRIGGER_TICK,
            READY_NOTIFIED,
            player.worldObj.getTotalWorldTime(),
            this.intervalSeconds,
            player,
            "\u8D85\u65B0\u661F\u91CD\u51FB");
    }

    @Override
    public void onSetKill(final EntityPlayer killer, final EntityLivingBase victim, final int pieceCount) {
        if (pieceCount >= 4) {
            killer.setHealth(killer.getMaxHealth());
            killer.addPotionEffect(new PotionEffect(this.buffId, this.buffDuration, this.buffAmplifier));
        }
    }

    @Override
    public void addTooltipLines(final ItemStack stack, final EntityPlayer player, final List tooltipLines,
        final boolean showAdvanced) {
        int activeCount = 0;
        if (player != null) {
            activeCount = SetBonusManager.getActiveSetCount(player, this.getSetId());
        }
        tooltipLines.add("");
        tooltipLines.add(
            "\u00A76\u2726 \u00A7e\u00A7l\u5957\u88c5\u5c5e\u6027: \u00A76\u00A7l\u8d85\u65b0\u661f \u00A76\u2726");
        final String color2 = (activeCount >= 2) ? "\u00A7a" : "\u00A78";
        final String prefix2 = (activeCount >= 2) ? "\u00A7a\u2714 " : "\u00A78\u2716 ";
        tooltipLines.add(
            prefix2 + color2
                + "\u4e24\u4ef6\u5957: \u00A77\u6bcf "
                + String.format("%.1f", this.intervalSeconds)
                + " \u79d2\uff0c\u4e0b\u4e00\u6b21\u8fd1\u6218\u547d\u4e2d\u989d\u5916\u9020\u6210\u5f53\u524d\u624b\u6301\u6b66\u5668\u653b\u51fb\u529b\u7684 \u00A7c"
                + String.format("%.1f", this.multiplier)
                + " \u00A77\u500d\u4f24\u5bb3");
        final String color3 = (activeCount >= 4) ? "\u00A7a" : "\u00A78";
        final String prefix3 = (activeCount >= 4) ? "\u00A7a\u2714 " : "\u00A78\u2716 ";
        tooltipLines.add(
            prefix3 + color3
                + "\u56db\u4ef6\u5957: \u00A77\u51fb\u6740\u654c\u4eba\u540e \u00A7a\u77ac\u95f4\u56de\u6ee1\u751f\u547d\u503c \u00A77\u5e76\u83b7\u5f97 \u00A7e\u529b\u91cf II \u00A77(\u6301\u7eed"
                + this.buffDuration / 20
                + "\u79d2)");
        final String countColor = (activeCount > 0) ? ((activeCount >= 4) ? "\u00A7a" : "\u00A7e") : "\u00A77";
        tooltipLines.add("    " + countColor + "\u5f53\u524d\u5df2\u88c5\u5907: (" + activeCount + "/4) \u4ef6");
        tooltipLines.add("\u00A76\u2727 ---------------------- \u2727");
    }
}
