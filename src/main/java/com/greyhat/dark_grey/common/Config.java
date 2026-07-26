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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
