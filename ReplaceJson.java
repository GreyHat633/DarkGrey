import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class ReplaceJson {
    public static void main(String[] args) throws Exception {
        String jsonPath = "src/main/resources/assets/dark_grey/data/rpg_items.json";
        String content = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8);
        
        // Ensure "腐败炸弹" is changed to "腐败瓶" everywhere
        content = content.replace("腐败炸弹", "腐败瓶");

        // Add bone_flask if not exists
        if (!content.contains("\"id\": \"bone_flask\"")) {
            String jsonAdd = "    {\n" +
                "      \"id\": \"bone_flask\",\n" +
                "      \"type\": \"usable_item\",\n" +
                "      \"displayName\": {\n" +
                "        \"zh_CN\": \"碎骨瓶\",\n" +
                "        \"en_US\": \"Bone Flask\"\n" +
                "      },\n" +
                "      \"texture\": \"dark_grey:bone_flask\",\n" +
                "      \"maxStackSize\": 16,\n" +
                "      \"components\": [\n" +
                "        {\n" +
                "          \"name\": \"碎骨瓶\",\n" +
                "          \"params\": {\n" +
                "            \"directDamage\": 12.0,\n" +
                "            \"lingeringDamage\": 2.0,\n" +
                "            \"fieldDuration\": 1200,\n" +
                "            \"projectileVelocity\": 0.75,\n" +
                "            \"projectileInaccuracy\": 1.0,\n" +
                "            \"projectileGravity\": 0.03\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    },";
            content = content.replaceFirst("(\"items\"\\s*:\\s*\\[)", "$1\n" + jsonAdd);
        }

        byte[] output = content.getBytes(StandardCharsets.UTF_8);
        Files.write(Paths.get(jsonPath), output);
        Files.write(Paths.get("E:/Java/MinecraftMod/RPGItem/rpg_items.json"), output);
        Files.write(Paths.get("run/config/dark_grey/rpg_items.json"), output);
    }
}
