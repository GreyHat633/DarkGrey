package com.greyhat.dark_grey.common;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String greeting = "Hello World";
    public static double particleDensity = 1.0;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");
        particleDensity = configuration
            .get(Configuration.CATEGORY_GENERAL, "particleDensity", 1.0, "Particle density coefficient (0.0 to 1.0)")
            .getDouble(1.0);
        if (particleDensity < 0.0) particleDensity = 0.0;
        if (particleDensity > 1.0) particleDensity = 1.0;

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
