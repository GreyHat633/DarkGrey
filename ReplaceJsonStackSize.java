import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class ReplaceJsonStackSize {
    public static void main(String[] args) throws Exception {
        String[] paths = {
            "src/main/resources/assets/dark_grey/data/rpg_items.json",
            "E:/Java/MinecraftMod/RPGItem/rpg_items.json",
            "run/config/dark_grey/rpg_items.json"
        };
        
        for (String jsonPath : paths) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8);
                
                // We need to replace maxStackSize for bone_flask.
                // We find bone_flask block and change "maxStackSize": 16 to "maxStackSize": 4
                
                // Using regex to specifically target bone_flask's stack size
                String regex = "(?s)(\"id\"\\s*:\\s*\"bone_flask\"[\\s\\S]*?\"maxStackSize\"\\s*:\\s*)\\d+";
                content = content.replaceAll(regex, "$1" + "4");

                byte[] output = content.getBytes(StandardCharsets.UTF_8);
                Files.write(Paths.get(jsonPath), output);
                System.out.println("Updated " + jsonPath);
            } catch (Exception e) {
                System.out.println("Error processing " + jsonPath + ": " + e.getMessage());
            }
        }
    }
}
