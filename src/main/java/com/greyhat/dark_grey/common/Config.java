package com.greyhat.dark_grey.common;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";
    public static double particleDensity = 1.0;

    public static boolean enableEntityMarkIcons = true;
    public static boolean enableTargetMarkPanel = true;
    public static boolean enableSelfMarkHud = true;
    public static boolean enableMarkFloatingText = true;
    public static double markRenderDistance = 24.0;
    public static double targetPanelRange = 32.0;
    public static int maxEntityMarkIcons = 4;
    public static int maxSelfHudMarks = 6;
    public static boolean showMarksThroughWalls = false;
    public static boolean showStableTimer = true;
    public static boolean showPeriodicTimer = true;
    public static boolean showFloatingStackChanges = true;
    public static double entityIconScale = 1.0;
    public static double selfHudScale = 1.0;

    public static int poisonMaxStacks = 99;
    public static int poisonPeriodicIntervalTicks = 10;
    public static int poisonStableDurationTicks = 200;
    public static int poisonDecayIntervalTicks = 10;
    public static int poisonDecayAmount = 10;
    public static boolean poisonImmediateDamageOnApply = true;
    public static boolean poisonImmediateDamageAtMaxRefresh = true;
    public static boolean poisonPeriodicConsumesStacks = false;
    public static boolean poisonDamageBypassesArmor = false;
    public static boolean poisonDamageBypassesMagicResistance = false;
    public static boolean poisonIgnoreHurtResistance = true;

    public static int fractureMaxStacks = 5;
    public static double fractureSpeedReductionPerStack = 0.10;
    public static int fractureDecayIntervalTicks = 100;

    // --- Shattered Bone ---
    public static int shatteredBoneDefaultIndependentDurationTicks = 60; // 3 seconds
    public static double shatteredBoneMovementThresholdSq = 0.01; // 0.1 blocks per tick
    public static float shatteredBoneMovementDamage = 5.0f;
    public static int shatteredBoneMovementCooldownTicks = 5;
    public static double shatteredBoneSplashRadius = 5.0;
    public static double shatteredBoneSplashHalfAngleCos = 0.9238; // cos(22.5) -> 45 total degree cone
    public static float shatteredBoneSplashDamageMultiplier = 2.25f; // 225%
    public static int shatteredBoneSplashDurationTicks = 60; // 3 seconds
    public static boolean fractureRefreshDecayOnApply = true;
    public static boolean fractureRefreshDecayAtMax = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");
        particleDensity = configuration
            .get(Configuration.CATEGORY_GENERAL, "particleDensity", 1.0, "Particle density coefficient (0.0 to 1.0)")
            .getDouble(1.0);
        if (particleDensity < 0.0) particleDensity = 0.0;
        if (particleDensity > 1.0) particleDensity = 1.0;

        enableEntityMarkIcons = configuration.getBoolean("enableEntityMarkIcons", "marks", true, "");
        enableTargetMarkPanel = configuration.getBoolean("enableTargetMarkPanel", "marks", true, "");
        enableSelfMarkHud = configuration.getBoolean("enableSelfMarkHud", "marks", true, "");
        enableMarkFloatingText = configuration.getBoolean("enableMarkFloatingText", "marks", true, "");
        markRenderDistance = configuration.getFloat("markRenderDistance", "marks", 24.0f, 1.0f, 128.0f, "");
        targetPanelRange = configuration.getFloat("targetPanelRange", "marks", 32.0f, 1.0f, 128.0f, "");
        maxEntityMarkIcons = configuration.getInt("maxEntityMarkIcons", "marks", 4, 1, 10, "");
        maxSelfHudMarks = configuration.getInt("maxSelfHudMarks", "marks", 6, 1, 20, "");
        showMarksThroughWalls = configuration.getBoolean("showMarksThroughWalls", "marks", false, "");
        showStableTimer = configuration.getBoolean("showStableTimer", "marks", true, "");
        showPeriodicTimer = configuration.getBoolean("showPeriodicTimer", "marks", true, "");
        showFloatingStackChanges = configuration.getBoolean("showFloatingStackChanges", "marks", true, "");
        entityIconScale = configuration.getFloat("entityIconScale", "marks", 1.0f, 0.1f, 5.0f, "");
        selfHudScale = configuration.getFloat("selfHudScale", "marks", 1.0f, 0.1f, 5.0f, "");

        poisonMaxStacks = configuration.getInt("maxStacks", "marks.poison", 99, 1, 100000, "");
        poisonPeriodicIntervalTicks = configuration.getInt("periodicIntervalTicks", "marks.poison", 10, 1, 72000, "");
        poisonStableDurationTicks = configuration.getInt("stableDurationTicks", "marks.poison", 200, 1, 720000, "");
        poisonDecayIntervalTicks = configuration.getInt("decayIntervalTicks", "marks.poison", 10, 1, 72000, "");
        poisonDecayAmount = configuration.getInt("decayAmount", "marks.poison", 10, 1, 100000, "");
        poisonImmediateDamageOnApply = configuration.getBoolean("immediateDamageOnApply", "marks.poison", true, "");
        poisonImmediateDamageAtMaxRefresh = configuration
            .getBoolean("immediateDamageAtMaxRefresh", "marks.poison", true, "");
        poisonPeriodicConsumesStacks = configuration.getBoolean("periodicConsumesStacks", "marks.poison", false, "");
        poisonDamageBypassesArmor = configuration.getBoolean("damageBypassesArmor", "marks.poison", false, "");
        poisonDamageBypassesMagicResistance = configuration
            .getBoolean("damageBypassesMagicResistance", "marks.poison", false, "");
        poisonIgnoreHurtResistance = configuration.getBoolean("ignoreHurtResistance", "marks.poison", true, "");

        fractureMaxStacks = configuration.getInt("maxStacks", "marks.fracture", 5, 1, 10, "");
        fractureSpeedReductionPerStack = configuration
            .get("marks.fracture", "speedReductionPerStack", 0.10, "Speed reduction per stack (0.0 to 0.19)")
            .getDouble(0.10);
        fractureDecayIntervalTicks = configuration.getInt("decayIntervalTicks", "marks.fracture", 100, 1, 72000, "");
        fractureRefreshDecayOnApply = configuration.getBoolean("refreshDecayOnApply", "marks.fracture", true, "");
        fractureRefreshDecayAtMax = configuration.getBoolean("refreshDecayAtMax", "marks.fracture", true, "");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
