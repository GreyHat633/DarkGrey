import java.io.*;
import java.nio.file.*;
import java.nio.charset.Charset;

public class AppendItem {
    public static void main(String[] args) throws Exception {
        String path = "E:/Java/MinecraftMod/DarkGrey/src/main/resources/assets/dark_grey/data/rpg_items.json";
        Charset gbk = Charset.forName("GBK");
        String content = new String(Files.readAllBytes(Paths.get(path)), gbk);

        // Find the last "}" + "]" + "}" pattern
        // We need to insert before the final "]" in the items array
        // The structure is: ...}, \n  ]\n}
        // Find last occurrence of "]\n}"
        int lastBracket = content.lastIndexOf("]");
        // Find the "}" before it (end of last item)
        int lastItemEnd = content.lastIndexOf("}", lastBracket - 1);

        String before = content.substring(0, lastItemEnd + 1);
        String after = content.substring(lastItemEnd + 1);

        String newItem =
            ",\n" +
            "    {\n" +
            "      \"id\": \"underground_sun\",\n" +
            "      \"type\": \"\u9053\u5177\",\n" +
            "      \"displayName\": {\n" +
            "        \"zh_CN\": \"\u5730\u5E95\u592A\u9633\",\n" +
            "        \"en_US\": \"Underground Sun\"\n" +
            "      },\n" +
            "      \"texture\": \"dark_grey:underground_sun\",\n" +
            "      \"durability\": 0,\n" +
            "      \"damage\": 90,\n" +
            "      \"components\": [\n" +
            "        {\n" +
            "          \"name\": \"\u5730\u5E95\u592A\u9633\",\n" +
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
            "    }";

        String result = before + newItem + after;
        Files.write(Paths.get(path), result.getBytes(gbk));

        // Verify
        String verify = new String(Files.readAllBytes(Paths.get(path)), gbk);
        int count = 0;
        int idx = 0;
        while ((idx = verify.indexOf("\"id\"", idx)) != -1) { count++; idx++; }
        System.out.println("Total items after append: " + count);
        System.out.println("Contains underground_sun: " + verify.contains("underground_sun"));
        System.out.println("Contains \u5730\u5E95\u592A\u9633: " + verify.contains("\u5730\u5E95\u592A\u9633"));
        System.out.println("Contains \u4F0A\u5854\u5C3C\u65AF: " + verify.contains("\u4F0A\u5854\u5C3C\u65AF"));
        System.out.println("Contains \u91CD\u51FB\u5DE8\u5251: " + verify.contains("\u91CD\u51FB\u5DE8\u5251"));
    }
}
