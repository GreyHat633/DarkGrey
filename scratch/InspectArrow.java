import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class InspectArrow {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("src/main/resources/assets/dark_grey/textures/entity/itanis_arrow.png"));
        System.out.println("Size: " + img.getWidth() + "x" + img.getHeight());
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >> 24) & 0xff;
                if (a > 10) {
                    System.out.printf("(%d,%d) a=%d r=%d g=%d b=%d\n", x, y, a, (argb>>16)&0xff, (argb>>8)&0xff, argb&0xff);
                }
            }
        }
    }
}
