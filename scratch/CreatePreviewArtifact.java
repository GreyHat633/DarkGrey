package scratch;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CreatePreviewArtifact {

    public static void main(String[] args) throws Exception {
        File pngFile = new File("src/main/resources/assets/dark_grey/textures/items/itanis.png");
        BufferedImage bowImg = ImageIO.read(pngFile);

        // Render 256x256 preview image with dark Minecraft inventory background
        BufferedImage preview = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = preview.createGraphics();

        // Dark background
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, 256, 256);

        // Grid lines
        g.setColor(new Color(45, 45, 45));
        for (int i = 0; i < 256; i += 16) {
            g.drawLine(i, 0, i, 256);
            g.drawLine(0, i, 256, i);
        }

        // Draw bow scaled nearest-neighbor
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        g.drawImage(bowImg, 16, 16, 224, 224, null);
        g.dispose();

        File dest = new File("C:/Users/GreyHat/.gemini/antigravity/brain/18b40cc9-989b-4382-9612-5272c93ea91e/itanis_user_style_preview.png");
        ImageIO.write(preview, "png", dest);
        System.out.println("Preview generated at: " + dest.getAbsolutePath());
    }
}
