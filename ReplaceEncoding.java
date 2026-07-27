import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class ReplaceEncoding {
    public static void main(String[] args) throws Exception {
        // ComponentCorruptionBomb.java
        String bombPath = "src/main/java/com/greyhat/dark_grey/component/ComponentCorruptionBomb.java";
        String bombContent = new String(Files.readAllBytes(Paths.get(bombPath)), StandardCharsets.UTF_8);
        bombContent = bombContent.replace("腐败炸弹", "腐败瓶");
        Files.write(Paths.get(bombPath), bombContent.getBytes(StandardCharsets.UTF_8));

        // DarkGrey.java
        String darkGreyPath = "src/main/java/com/greyhat/dark_grey/DarkGrey.java";
        String dgContent = new String(Files.readAllBytes(Paths.get(darkGreyPath)), StandardCharsets.UTF_8);
        dgContent = dgContent.replace("腐败炸弹", "腐败瓶");
        
        // Add ComponentBoneFlask
        dgContent = dgContent.replace(
            "ComponentRegistry.register(\"粉碎之骨\"", 
            "ComponentRegistry.register(\"碎骨瓶\", (java.util.function.Supplier<com.greyhat.dark_grey.api.IRPGComponent>) com.greyhat.dark_grey.component.ComponentBoneFlask::new);\n        ComponentRegistry.register(\"粉碎之骨\""
        );
        
        // Add EntityBoneFlask and EntityBoneSpikesField
        String regex = "(cpw\\.mods\\.fml\\.common\\.registry\\.EntityRegistry\\.registerModEntity\\(\\s*\\(Class\\) com\\.greyhat\\.dark_grey\\.entity\\.EntityStarBullet\\.class[\\s\\S]*?true\\);)";
        String replacement = "$1\n        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity((Class) com.greyhat.dark_grey.entity.EntityBoneFlask.class, \"bone_flask\", 11, (Object) DarkGrey.instance, 64, 10, true);\n        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity((Class) com.greyhat.dark_grey.entity.EntityBoneSpikesField.class, \"bone_spikes_field\", 12, (Object) DarkGrey.instance, 64, 10, false);";
        dgContent = dgContent.replaceAll(regex, replacement);
        
        Files.write(Paths.get(darkGreyPath), dgContent.getBytes(StandardCharsets.UTF_8));
    }
}
