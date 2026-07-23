import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CheckRes {
    public static void main(String[] args) throws Exception {
        String base = "src/main/resources/assets/dark_grey/textures/entity/";
        String[] files = {"itanis_arrow.png"};
        for (String f : files) {
            File file = new File(base + f);
            if (file.exists()) {
                BufferedImage img = ImageIO.read(file);
                System.out.println(f + ": " + img.getWidth() + "x" + img.getHeight());
            } else {
                System.out.println(f + " not found.");
            }
        }
    }
}
