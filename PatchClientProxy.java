import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class PatchClientProxy {
    public static void main(String[] args) throws Exception {
        String path = "src/main/java/com/greyhat/dark_grey/common/ClientProxy.java";
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);

        String regex = "(cpw\\.mods\\.fml\\.client\\.registry\\.RenderingRegistry\\.registerEntityRenderingHandler\\(\\s*com\\.greyhat\\.dark_grey\\.entity\\.EntityStarBullet\\.class,\\s*new com\\.greyhat\\.dark_grey\\.client\\.render\\.RenderStarBullet\\(\\)\\);)";
        String replacement = "$1\n        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityBoneFlask.class, new com.greyhat.dark_grey.client.render.RenderBoneFlask());\n        cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.greyhat.dark_grey.entity.EntityBoneSpikesField.class, new com.greyhat.dark_grey.client.render.RenderInvisible());";
        
        content = content.replaceAll(regex, replacement);
        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
    }
}
