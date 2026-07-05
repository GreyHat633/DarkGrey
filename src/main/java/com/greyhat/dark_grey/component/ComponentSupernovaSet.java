// 
// Decompiled by Procyon v0.6.0
// 

package com.greyhat.dark_grey.component;

import com.greyhat.dark_grey.api.SetBonusManager;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.ISetComponent;

public class ComponentSupernovaSet implements ISetComponent, IHasTooltip
{
    private float damagePercent;
    private int buffDuration;
    private int buffId;
    private int buffAmplifier;
    
    public ComponentSupernovaSet() {
        this.damagePercent = 0.1f;
        this.buffDuration = 200;
        this.buffId = 5;
        this.buffAmplifier = 1;
    }
    
    @Override
    public void configure(final JsonObject params) {
        if (params.has("damagePercent")) {
            this.damagePercent = params.get("damagePercent").getAsFloat();
        }
        if (params.has("buffDuration")) {
            this.buffDuration = params.get("buffDuration").getAsInt();
        }
        if (params.has("buffId")) {
            this.buffId = params.get("buffId").getAsInt();
        }
        if (params.has("buffAmplifier")) {
            this.buffAmplifier = params.get("buffAmplifier").getAsInt();
        }
    }
    
    @Override
    public String getSetId() {
        return "supernova_set";
    }
    
    @Override
    public float onSetHit(final EntityPlayer attacker, final EntityLivingBase target, final float rawDamage, final int pieceCount) {
        if (pieceCount >= 2) {
            final float bonusDamage = target.getMaxHealth() * this.damagePercent;
            return rawDamage + bonusDamage;
        }
        return rawDamage;
    }
    
    @Override
    public void onSetKill(final EntityPlayer killer, final EntityLivingBase victim, final int pieceCount) {
        if (pieceCount >= 4) {
            killer.setHealth(killer.getMaxHealth());
            killer.addPotionEffect(new PotionEffect(this.buffId, this.buffDuration, this.buffAmplifier));
        }
    }
    
    @Override
    public void addTooltipLines(final ItemStack stack, final EntityPlayer player, final List tooltipLines, final boolean showAdvanced) {
        int activeCount = 0;
        if (player != null) {
            activeCount = SetBonusManager.getActiveSetCount(player, this.getSetId());
        }
        tooltipLines.add("");
        tooltipLines.add("\u00A76\u2726 \u00A7e\u00A7l\u5957\u88c5\u5c5e\u6027: \u00A76\u00A7l\u8d85\u65b0\u661f \u00A76\u2726");
        final String color2 = (activeCount >= 2) ? "\u00A7a" : "\u00A78";
        final String prefix2 = (activeCount >= 2) ? "\u00A7a\u2714 " : "\u00A78\u2716 ";
        tooltipLines.add(prefix2 + color2 + "\u4e24\u4ef6\u5957: \u00A77\u653b\u51fb\u65f6\u989d\u5916\u9020\u6210 \u00A7c\u76ee\u6807\u6700\u5927\u751f\u547d\u503c " + (int)(this.damagePercent * 100.0f) + "% \u00A77\u7684\u771f\u5b9e\u4f24\u5bb3");
        final String color3 = (activeCount >= 4) ? "\u00A7a" : "\u00A78";
        final String prefix3 = (activeCount >= 4) ? "\u00A7a\u2714 " : "\u00A78\u2716 ";
        tooltipLines.add(prefix3 + color3 + "\u56db\u4ef6\u5957: \u00A77\u51fb\u6740\u654c\u4eba\u540e \u00A7a\u77ac\u95f4\u56de\u6ee1\u751f\u547d\u503c \u00A77\u5e76\u83b7\u5f97 \u00A7e\u529b\u91cf II \u00A77(\u6301\u7eed" + this.buffDuration / 20 + "\u79d2)");
        final String countColor = (activeCount > 0) ? ((activeCount >= 4) ? "\u00A7a" : "\u00A7e") : "\u00A77";
        tooltipLines.add("    " + countColor + "\u5f53\u524d\u5df2\u88c5\u5907: (" + activeCount + "/4) \u4ef6");
        tooltipLines.add("\u00A76\u2727 ---------------------- \u2727");
    }
}
