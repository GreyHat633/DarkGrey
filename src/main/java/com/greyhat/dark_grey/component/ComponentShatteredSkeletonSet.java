package com.greyhat.dark_grey.component;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;

import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.ISetComponent;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;

public class ComponentShatteredSkeletonSet implements ISetComponent, IHasTooltip {

    public static final String ID = "shattered_skeleton";
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("6b6e41b2-132d-4567-9d7e-90f70a483e54");
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("c0410f94-9b5a-4f51-b8f2-8c83dc3cbab4");

    @Override
    public String getSetId() {
        return ID;
    }

    @Override
    public void onSetPieceCountChanged(EntityPlayer player, int oldPieceCount, int newPieceCount) {
        if (player.worldObj.isRemote) return;

        // 2-piece set bonus: +25% Move Speed
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (speedAttr != null) {
            AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_UUID);
            if (newPieceCount >= 2 && existing == null) {
                speedAttr.applyModifier(
                    new AttributeModifier(SPEED_MODIFIER_UUID, "Shattered Skeleton Speed Bonus", 0.25, 2)
                        .setSaved(false));
            } else if (newPieceCount < 2 && existing != null) {
                speedAttr.removeModifier(existing);
            }
        }

        // 4-piece set bonus: +128 Max Health
        IAttributeInstance healthAttr = player.getEntityAttribute(SharedMonsterAttributes.maxHealth);
        if (healthAttr != null) {
            AttributeModifier existing = healthAttr.getModifier(HEALTH_MODIFIER_UUID);
            if (newPieceCount >= 4 && existing == null) {
                healthAttr.applyModifier(
                    new AttributeModifier(HEALTH_MODIFIER_UUID, "Shattered Skeleton Health Bonus", 128.0, 0)
                        .setSaved(false));
            } else if (newPieceCount < 4 && existing != null) {
                healthAttr.removeModifier(existing);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    @Override
    public int modifyMarkRequestedStacks(EntityPlayer applier, EntityLivingBase target, String markId,
        MarkApplyContext context, int requestedStacks, int pieceCount) {
        if (pieceCount >= 2 && com.greyhat.dark_grey.mark.type.FractureMarkType.ID.equals(markId)) {
            return requestedStacks + 1;
        }
        return requestedStacks;
    }

    @Override
    public void addTooltipLines(net.minecraft.item.ItemStack stack, EntityPlayer player, List<String> tooltip,
        boolean advanced) {
        int count = 0;
        if (player != null) {
            count = com.greyhat.dark_grey.api.SetBonusManager.getActiveSetCount(player, ID);
        }

        tooltip.add("");
        tooltip.add(
            "\u00A76\u2726 \u00A7e\u00A7l\u5957\u88c5\u5c5e\u6027: \u00A76\u00A7l\u7c89\u788e\u9ab8\u9aa8 \u00A76\u2726");

        final String color2 = (count >= 2) ? "\u00A7a" : "\u00A78";
        final String prefix2 = (count >= 2) ? "\u00A7a\u2714 " : "\u00A78\u2716 ";
        tooltip.add(
            prefix2 + color2
                + "\u4e24\u4ef6\u5957: \u00A77\u79fb\u901f\u589e\u52a0 25%\uff0c\u9aa8\u6298\u65bd\u52a0\u5c42\u6570\u989d\u5916 +1");

        final String color4 = (count >= 4) ? "\u00A7a" : "\u00A78";
        final String prefix4 = (count >= 4) ? "\u00A7a\u2714 " : "\u00A78\u2716 ";
        tooltip
            .add(prefix4 + color4 + "\u56db\u4ef6\u5957: \u00A77\u6700\u5927\u751f\u547d\u503c\u589e\u52a0 128\u3002");
        tooltip.add(
            "    " + color4
                + "\u4e3b\u76ee\u6807\u62e5\u6709 5 \u5c42\u9aa8\u6298\u65f6\u627f\u53d7\u989d\u5916\u788e\u9aa8\u6e85\u5c04\u4f24\u5bb3");

        final String countColor = (count > 0) ? ((count >= 4) ? "\u00A7a" : "\u00A7e") : "\u00A77";
        tooltip.add("    " + countColor + "\u5f53\u524d\u5df2\u88c5\u5907: (" + count + "/4) \u4ef6");
        tooltip.add("\u00A76\u2727 ---------------------- \u2727");
    }
}
