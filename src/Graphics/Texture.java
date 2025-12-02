package Graphics;

import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;

public class Texture {
    public static Render floor = loadBitmap("/templates/floor.png");
    public static Render wall = loadBitmap("/templates/wall.png");
    public static Render grate = loadBitmap("/templates/gr.png");

    public static Render enemyFront = loadBitmap("/templates/e_1.png");
    public static Render enemyLeft  = loadBitmap("/templates/e_2.png");
    public static Render enemyRight = loadBitmap("/templates/e_3.png");
    public static Render enemyBack  = loadBitmap("/templates/e_4.png");
    public static Render enemyTop   = loadBitmap("/templates/e_5.png");
    public static Render enemyBot   = loadBitmap("/templates/e_6.png");

    public static Render yellowSquare = genYellowTexture();

    public static Render loadBitmap(String fileName) {
        try {
            BufferedImage image = ImageIO.read(Texture.class.getResource(fileName));
            int width = image.getWidth();
            int height = image.getHeight();

            Render result = new Render(width, height);
            image.getRGB(0, 0, width, height, result.pixels, 0, width);
            return result;
        } catch(Exception e) {

            System.out.println("Error loading: " + fileName);
            Render err = new Render(64, 64);
            for(int i=0; i<64*64; i++) err.pixels[i] = 0xFFFF00FF;
            return err;
        }
    }

    private static Render genYellowTexture() {
        int w = 64;
        int h = 64;
        Render r = new Render(w, h);

        for (int i = 0; i < w * h; i++) {
            int val = 150 + new java.util.Random().nextInt(105);

            r.pixels[i] = (val << 16) | (val << 8);
        }
        return r;
    }
}