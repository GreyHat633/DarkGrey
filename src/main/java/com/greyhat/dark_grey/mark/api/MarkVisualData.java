package com.greyhat.dark_grey.mark.api;

import net.minecraft.util.ResourceLocation;

public final class MarkVisualData {

    public final ResourceLocation icon;

    public final String displayNameKey;
    public final String descriptionKey;

    public final int primaryColor;
    public final int decayColor;
    public final int maxColor;

    public final int displayPriority;

    public final boolean showStacks;
    public final boolean showStableTimer;
    public final boolean showPeriodicTimer;
    public final boolean showDecayTimer;

    public final boolean showOnEntity;
    public final boolean showOnTargetPanel;
    public final boolean showOnSelfHud;

    public final boolean flashWhenNearDecay;
    public final boolean glowWhenMaxed;

    public MarkVisualData(ResourceLocation icon, String displayNameKey, String descriptionKey, int primaryColor,
        int decayColor, int maxColor, int displayPriority, boolean showStacks, boolean showStableTimer,
        boolean showPeriodicTimer, boolean showDecayTimer, boolean showOnEntity, boolean showOnTargetPanel,
        boolean showOnSelfHud, boolean flashWhenNearDecay, boolean glowWhenMaxed) {
        this.icon = icon;
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.primaryColor = primaryColor;
        this.decayColor = decayColor;
        this.maxColor = maxColor;
        this.displayPriority = displayPriority;
        this.showStacks = showStacks;
        this.showStableTimer = showStableTimer;
        this.showPeriodicTimer = showPeriodicTimer;
        this.showDecayTimer = showDecayTimer;
        this.showOnEntity = showOnEntity;
        this.showOnTargetPanel = showOnTargetPanel;
        this.showOnSelfHud = showOnSelfHud;
        this.flashWhenNearDecay = flashWhenNearDecay;
        this.glowWhenMaxed = glowWhenMaxed;
    }
}
