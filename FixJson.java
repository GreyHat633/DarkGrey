import java.io.*;
import java.nio.file.*;
import java.nio.charset.Charset;

public class FixJson {
    public static void main(String[] args) throws Exception {
        String path = "E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/data/rpg_items.json";
        String content = new String(Files.readAllBytes(Paths.get(path)), Charset.forName("GBK"));
        content = content.trim();
        if (content.endsWith("]")) {
            content = content.substring(0, content.length() - 1);
        }
        
        String append = ",\n" +
            "    {\n" +
            "      \"id\": \"underground_sun\",\n" +
            "      \"type\": \"道具\",\n" +
            "      \"displayName\": {\n" +
            "        \"zh_CN\": \"地底太阳\",\n" +
            "        \"en_US\": \"Underground Sun\"\n" +
            "      },\n" +
            "      \"texture\": \"dark_grey:underground_sun\",\n" +
            "      \"durability\": 0,\n" +
            "      \"damage\": 90,\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"地底太阳\",\n" +
            "          \"params\": {\n" +
            "            \"chargeTicks\": 40,\n" +
            "            \"maxStoredOrbs\": 3,\n" +
            "            \"damageMultiplier\": 5.0,\n" +
            "            \"explosionRadius\": 20.0,\n" +
            "            \"explosionHalfHeight\": 10.0,\n" +
            "            \"projectileSpeed\": 1.8,\n" +
            "            \"projectileLifetime\": 100,\n" +
            "            \"launchCooldownTicks\": 5,\n" +
            "            \"ignoreHurtResistance\": true,\n" +
            "            \"respectWalls\": false,\n" +
            "            \"orbitRadius\": 1.25,\n" +
            "            \"orbitHeight\": -0.2,\n" +
            "            \"orbitSpeed\": 2.0\n" +
            "          }\n" +
            "        }\n" +
            "      ],\n" +
            "      \"enchantments\": \"\"\n" +
            "    }\n" +
            "]";
            
        content += append;
        Files.write(Paths.get(path), content.getBytes(Charset.forName("GBK")));
        System.out.println("JSON fixed perfectly in GBK");
    }
}
