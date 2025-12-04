package Graphics;

import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Клас Texture (Текстура).
 * Це статичне сховище для всіх зображень у грі.
 * Він завантажує картинки з файлів або генерує їх програмно.
 */
public class Texture {

    // --- ЗАВАНТАЖЕНІ ТЕКСТУРИ ---
    // Ми робимо їх static, щоб вони були доступні з будь-якого місця програми (Texture.floor, Texture.wall тощо).

    // Текстура підлоги (завантажується з файлу floor.png)
    public static Render floor = loadBitmap("/templates/floor.png");

    // Текстура стіни
    public static Render wall = loadBitmap("/templates/wall.png");

    // Текстура решітки (прозора стіна)
    public static Render grate = loadBitmap("/templates/gr.png");

    // --- ГЕНЕРОВАНІ ТЕКСТУРИ ---
    // Якщо файлу немає, ми створюємо прості кольорові квадрати кодом.

    // Текстура ворога (червоний шум)
    public static Render enemy = genEnemyTexture();

    // Текстура предмета (жовтий шум)
    public static Render yellowSquare = genYellowTexture();

    /**
     * Метод loadBitmap (Завантажити зображення).
     * Зчитує файл картинки (.png, .jpg) з папки ресурсів і перетворює його у формат Render.
     *
     * @param fileName Шлях до файлу (наприклад, "/templates/wall.png").
     * @return Об'єкт Render, що містить пікселі завантаженої картинки.
     */
    public static Render loadBitmap(String fileName) {
        try {
            // Використовуємо Java ImageIO для читання файлу
            BufferedImage image = ImageIO.read(Texture.class.getResource(fileName));

            int width = image.getWidth();
            int height = image.getHeight();

            // Створюємо новий об'єкт Render з розмірами картинки
            Render result = new Render(width, height);

            // Копіюємо пікселі з картинки в масив пікселів нашого рендера.
            // getRGB заповнює масив result.pixels кольорами.
            image.getRGB(0, 0, width, height, result.pixels, 0, width);

            return result;
        } catch(Exception e) {
            // Якщо файл не знайдено або він пошкоджений, програма впаде з помилкою
            System.out.println("CRASH! Could not load texture: " + fileName);
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод genEnemyTexture (Генерувати текстуру ворога).
     * Створює процедурну текстуру (випадковий червоний шум).
     * Використовується для тестування, якщо немає намальованого спрайта ворога.
     */
    private static Render genEnemyTexture() {
        int w = 64; // Ширина
        int h = 64; // Висота
        Render r = new Render(w, h);
        Random random = new Random();

        // Проходимо по кожному пікселю
        for (int i = 0; i < w * h; i++) {
            // Генеруємо випадковий відтінок червоного (від 150 до 255)
            int red = 150 + random.nextInt(105);

            // Формуємо колір у форматі RGB.
            // (red << 16) означає зсув байтів, щоб записати число в канал RED.
            // Формат кольору: 00RRGGBB (де R - червоний, G - зелений, B - синій).
            r.pixels[i] = (red << 16);
        }
        return r;
    }

    /**
     * Метод genYellowTexture (Генерувати жовту текстуру).
     * Створює жовтий шум (схоже на золото або пісок).
     * Використовується для предметів (Items).
     */
    private static Render genYellowTexture() {
        int w = 64;
        int h = 64;
        Render r = new Render(w, h);
        Random random = new Random();

        for (int i = 0; i < w * h; i++) {
            // Випадкова яскравість
            int val = 150 + random.nextInt(105);

            // Жовтий колір утворюється змішуванням Червоного та Зеленого.
            // (val << 16) - це червоний канал.
            // (val << 8)  - це зелений канал.
            // Оператор | (OR) об'єднує їх.
            r.pixels[i] = (val << 16) | (val << 8);
        }
        return r;
    }
}