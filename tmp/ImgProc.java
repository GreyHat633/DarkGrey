import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ImgProc {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("C:\\Users\\GreyHat\\.gemini\\antigravity\\brain\\0a8bff84-6a80-43b1-8fa4-16fad0f95134\\.user_uploaded\\media__1785337002155.png"));
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                int rgb = img.getRGB(x,y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (r+g+b)/3;
                int alpha = 255 - brightness;
                int finalCol = (alpha << 24) | 0xFFFFFF;
                out.setRGB(x,y,finalCol);
            }
        }
        File outF = new File("src/main/resources/assets/dark_grey/textures/particles/shattered_bone_circle.png");
        outF.getParentFile().mkdirs();
        ImageIO.write(out, "png", outF);
        System.out.println("Done!");
    }
}
