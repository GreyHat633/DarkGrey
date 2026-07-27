package com.greyhat.dark_grey.component;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;

public class ComponentBoneCrusher implements IRPGComponent, IHasTooltip {

    private int requiredHits = 3;
    private String fractureMarkId = "fracture";
    private int fractureStacksPerTrigger = 1;
    private int fractureStableDurationTicks = 100;
    private boolean showThirdHitFeedback = true;

    @Override
    public String getComponentId() {
        return "粉碎之骨";
    }

    @Override
    public void configure(JsonObject params) {
        if (params.has("requiredHits")) {
            this.requiredHits = params.get("requiredHits")
                .getAsInt();
        }
        if (params.has("fractureMarkId")) {
            this.fractureMarkId = params.get("fractureMarkId")
                .getAsString();
        }
        if (params.has("fractureStacksPerTrigger")) {
            this.fractureStacksPerTrigger = params.get("fractureStacksPerTrigger")
                .getAsInt();
        }
        if (params.has("showThirdHitFeedback")) {
            this.showThirdHitFeedback = params.get("showThirdHitFeedback")
                .getAsBoolean();
        }
        if (params.has("fractureStableDurationTicks")) {
            this.fractureStableDurationTicks = Math.max(
                1,
                Math.min(
                    720000,
                    params.get("fractureStableDurationTicks")
                        .getAsInt()));
        } else if (params.has("fractureDurationTicks")) {
            this.fractureStableDurationTicks = Math.max(
                1,
                Math.min(
                    720000,
                    params.get("fractureDurationTicks")
                        .getAsInt()));
        }
    }

    public int getRequiredHits() {
        return requiredHits;
    }

    public String getFractureMarkId() {
        return fractureMarkId;
    }

    public int getFractureStacksPerTrigger() {
        return fractureStacksPerTrigger;
    }

    public int getFractureStableDurationTicks() {
        return fractureStableDurationTicks;
    }

    public boolean isShowThirdHitFeedback() {
        return showThirdHitFeedback;
    }

    @Override
    public void addTooltipLines(ItemStack itemStack, EntityPlayer player, List<String> tooltipLines,
        boolean showAdvanced) {
        tooltipLines.add(
            "\u00A74\u25C6 " + StatCollector.translateToLocal("component.dark_grey.bone_crusher.name")
                + " \u00A77| \u00A7c"
                + StatCollector.translateToLocalFormatted(
                    "component.dark_grey.bone_crusher.desc",
                    requiredHits,
                    fractureStacksPerTrigger));
    }
}
