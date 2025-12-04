package Graphics;

import Index.Game;
import Index.Level;
import Index.Enemy;
import Index.Item;
import Input.Controller;

/**
 * Клас Render3D.
 * Відповідає за створення псевдо-3D зображення з 2D карти за допомогою методу Raycasting (Кидання променів).
 * Успадковує базовий клас Render, додаючи роботу з глибиною (Z-координатою).
 */
public class Render3D extends Render {

    /**
     * Z-Buffer (Буфер глибини).
     * Зберігає відстань до об'єкта для кожного пікселя на екрані.
     * Використовується для:
     * 1. Ефекту затемнення (туману) - знаємо, як далеко піксель.
     * 2. Коректного перекриття - щоб стіна не малювалася поверх ворога, якщо ворог ближче.
     */
    public double[] zBuffer;

    /**
     * Дистанція рендерингу (максимальна дальність видимості).
     * Використовується для розрахунку яскравості (чим далі, тим темніше).
     */
    private double renderDistance = 15000;

    /**
     * Конструктор Render3D.
     *
     * @param width  Ширина екрана.
     * @param height Висота екрана.
     */
    public Render3D(int width, int height) {
        super(width, height); // Ініціалізація батьківського класу
        zBuffer = new double[width * height]; // Ініціалізація масиву глибини
    }

    /**
     * Метод floor (Підлога та Стеля).
     * Малює горизонтальні поверхні (підлогу і стелю) методом трасування променів.
     * Тут використовується техніка "Horizontal Raycasting".
     *
     * @param game Об'єкт гри для доступу до керування та камери.
     */
    public void floor(Game game) {
        // Розміри текстури підлоги (64x64)
        int floorSize = 64;
        int floorMask = 63; // Маска для бітових операцій (аналог % 64, але швидше)

        // Якщо текстура підлоги завантажена, беремо її реальні розміри
        if (Texture.floor != null) {
            floorSize = Texture.floor.width;
            floorMask = floorSize - 1;
        }

        // Базова висота камери над підлогою
        double floorPosition = 8;

        // Отримуємо координати та поворот гравця з контролера
        double forward = game.controls.z; // Рух вперед/назад (Z)
        double right = game.controls.x;   // Рух вліво/вправо (X)
        double up = game.controls.y;      // Стрибки/присідання (Y)

        double rotation = game.controls.rotation; // Кут повороту камери

        // Проходимо по кожному вертикальному стовпчику екрана
        for (int x = 0; x < width; x++) {
            // Нормалізована координата камери (від -1 зліва до +1 справа)
            // Це визначає, яку частину променя ми зараз малюємо
            double cameraX = 2 * x / (double) width - 1;

            // Напрямок променя для поточного стовпчика (з урахуванням повороту)
            // Використовується матриця повороту
            double rayDirX = Math.sin(rotation) + Math.cos(rotation) * cameraX;
            double rayDirZ = Math.cos(rotation) - Math.sin(rotation) * cameraX;

            // Проходимо по пікселях зверху вниз (для підлоги та стелі)
            for (int y = 0; y < height; y++) {
                // Розрахунок "глибини" (відстані) до точки на підлозі для поточного пікселя Y.
                // Формула враховує перспективу: чим нижче піксель на екрані, тим ближче він до гравця.
                // rotationPitch дозволяє дивитися вгору/вниз (зсуває горизонт).
                double ceiling = (y - (height / 2.0 + game.controls.rotationPitch)) / height;

                // Розрахунок реальної Z-дистанції у світі
                double z = (floorPosition + up) / ceiling;

                // Якщо ceiling < 0, значить ми вище горизонту -> малюємо стелю
                if (ceiling < 0) {
                    z = (8.0 - up) / -ceiling;
                }

                // Розраховуємо реальні координати текстури на карті (World Coordinates)
                // Поточна позиція гравця + (Напрямок променя * Дистанцію)
                double xx = right + rayDirX * z;
                double yy = forward + rayDirZ * z;

                // Переводимо в цілі числа для вибірки пікселя з текстури
                int xPix = (int) (xx);
                int yPix = (int) (yy);

                // Записуємо глибину в Z-buffer, щоб потім правильно малювати стіни поверх підлоги
                zBuffer[x + y * width] = z;

                // Малюємо піксель. Беремо колір з текстури підлоги.
                // (xPix & floorMask) забезпечує повторення текстури (тайлінг) по всій карті.
                pixels[x + y * width] = Texture.floor.pixels[(xPix & floorMask) + (yPix & floorMask) * floorSize];

                // Обмежуємо дальність промальовування (заливаємо чорним, якщо дуже далеко)
                if (z > renderDistance) {
                    pixels[x + y * width] = 0;
                }
            }
        }
    }

    /**
     * Метод walls (Стіни та об'єкти).
     * Основний метод Raycasting. Він випускає промені для кожного стовпчика екрана.
     * Використовує алгоритм DDA (Digital Differential Analyzer) для швидкого пошуку перетину зі стіною.
     *
     * @param game  Об'єкт гри.
     * @param level Об'єкт рівня (карта).
     */
    public void walls(Game game, Level level) {
        // Позиція гравця
        double xPos = game.controls.x;
        double zPos = game.controls.z;
        double rot = game.controls.rotation;

        // Цикл по ширині екрана (кожен x - це один вертикальний стовпчик зображення)
        for (int x = 0; x < width; x++) {
            double fov = 1.0; // Field of View (Поле зору), коефіцієнт ширини лінзи
            double cameraX = 2 * x / (double) width - 1; // Позиція променя на площині камери (-1 ... 1)

            // Вектор напрямку променя для поточного стовпчика X
            double rayDirX = Math.sin(rot) + Math.cos(rot) * cameraX * fov;
            double rayDirZ = Math.cos(rot) - Math.sin(rot) * cameraX * fov;

            // Поточна клітинка карти (Grid), в якій ми знаходимось
            int mapX = (int) xPos;
            int mapZ = (int) zPos;

            // --- ЗМІННІ ДЛЯ АЛГОРИТМУ DDA ---
            double sideDistX; // Відстань від старту до першої границі клітинки по X
            double sideDistZ; // Відстань від старту до першої границі клітинки по Z

            // DeltaDist: відстань, яку треба пройти променю, щоб перетнути 1 одиницю по X або Z
            // Формула спрощена з Піфагора: sqrt(1 + (rayDirY * rayDirY) / (rayDirX * rayDirX))
            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistZ = Math.abs(1 / rayDirZ);
            double perpWallDist; // Перпендикулярна відстань до стіни (щоб уникнути ефекту "риб'ячого ока")

            int stepX; // Напрямок кроку по X (+1 або -1)
            int stepZ; // Напрямок кроку по Z (+1 або -1)

            boolean hit = false; // Чи вдарився промінь у стіну?
            int side = 0; // В яку сторону вдарився? (0 - Північ/Південь, 1 - Захід/Схід)
            int wallType = 0; // Тип блоку, в який влучили

            // --- ІНІЦІАЛІЗАЦІЯ DDA ---
            // Визначаємо, в який бік ми крокуємо і початкову відстань до першої лінії сітки
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (xPos - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - xPos) * deltaDistX;
            }
            if (rayDirZ < 0) {
                stepZ = -1;
                sideDistZ = (zPos - mapZ) * deltaDistZ;
            } else {
                stepZ = 1;
                sideDistZ = (mapZ + 1.0 - zPos) * deltaDistZ;
            }

            // --- ЗАПУСК ЦИКЛУ DDA ---
            // "Летимо" променем по клітинках карти, поки не вріжемось у стіну
            int maxDepth = 300; // Максимальна кількість кроків (дальність видимості)
            int depthCount = 0;

            while (!hit && depthCount < maxDepth) {
                // Переходимо до наступного квадрата карти в тому напрямку, де відстань менша
                if (sideDistX < sideDistZ) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistZ += deltaDistZ;
                    mapZ += stepZ;
                    side = 1;
                }
                // Перевіряємо, чи є в цій клітинці стіна (значення > 0)
                wallType = level.getTile(mapX, mapZ);
                if (wallType > 0) hit = true;
                depthCount++;
            }

            // ВИПРАВЛЕННЯ: Якщо промінь полетів у безодню (нічого не знайшов), пропускаємо малювання стовпчика
            if (!hit) continue;

            // --- РОЗРАХУНОК ПРОЕКЦІЇ ---
            // Розрахунок перпендикулярної відстані до стіни.
            // Ми не використовуємо звичайну відстань (Euclidean), бо це створить ефект "риб'ячого ока"
            // (стіни будуть круглими). Ми беремо відстань, спроектовану на вектор камери.
            if (side == 0) perpWallDist = (mapX - xPos + (1 - stepX) / 2) / rayDirX;
            else           perpWallDist = (mapZ - zPos + (1 - stepZ) / 2) / rayDirZ;

            // Розрахунок висоти лінії стіни на екрані (чим далі, тим менша висота).
            // (height * 16) - магічне число масштабування.
            int lineHeight = (int) ((height * 16) / perpWallDist);

            // Ефекти камери: нахил голови (pitch) та стрибок (jumpOffset)
            int pitch = (int) game.controls.rotationPitch;
            int jumpOffset = (int) (game.controls.y * 20);

            // Визначаємо, де починати і закінчувати малювати стіну по вертикалі (Y)
            int drawStart = -lineHeight / 2 + height / 2 + pitch + jumpOffset;
            if (drawStart < 0) drawStart = 0;
            int drawEnd = lineHeight / 2 + height / 2 + pitch + jumpOffset;
            if (drawEnd >= height) drawEnd = height - 1;

            // --- ТЕКСТУРУВАННЯ ---
            // Розрахунок точної координати X на стіні (де саме вдарився промінь).
            // Це потрібно, щоб знати, який стовпчик текстури малювати.
            double wallX;
            if (side == 0) wallX = zPos + perpWallDist * rayDirZ;
            else           wallX = xPos + perpWallDist * rayDirX;
            wallX -= Math.floor(wallX); // Залишаємо тільки дробову частину (від 0.0 до 1.0)

            // Вибір текстури залежно від типу блоку
            Render textureToUse = Texture.wall;
            if (wallType == 2) textureToUse = Texture.grate;

            // Переводимо координату 0.0-1.0 у піксельну координату текстури
            int texX = (int)(wallX * textureToUse.width);

            // Відображаємо текстуру дзеркально, якщо дивимось з певних сторін
            // (щоб текстура не була перевернутою)
            if(side == 0 && rayDirX > 0) texX = textureToUse.width - texX - 1;
            if(side == 1 && rayDirZ < 0) texX = textureToUse.width - texX - 1;

            // Забезпечуємо циклічність текстури (Bitwise AND як заміна %)
            int texWidth = textureToUse.width;
            texX = texX & (texWidth - 1);

            // --- МАЛЮВАННЯ СТІНИ ---
            for (int y = drawStart; y < drawEnd; y++) {
                if (y >= 0 && y < height) {
                    // Математика для вибору Y-координати текселя (пікселя текстури).
                    // Ми розтягуємо або стискаємо текстуру залежно від висоти стіни (lineHeight).
                    // Використовуємо цілочисельну математику (* 256) для збереження точності.
                    long d = (long)(y - pitch - jumpOffset) * 256 - height * 128 + lineHeight * 128;
                    int texY = (int)((d * texWidth) / lineHeight) / 256;

                    // Обмежуємо координати текстури (Safety Check)
                    if (texY < 0) texY = 0;
                    if (texY >= texWidth) texY = texWidth - 1;

                    // Беремо колір пікселя з текстури
                    int color = textureToUse.pixels[texX + texY * texWidth];
                    pixels[x + y * width] = color;
                    zBuffer[x + y * width] = perpWallDist; // Записуємо глибину стіни для туману і спрайтів
                }
            }

            // Зберігаємо відстань до найближчого об'єкта (стіни)
            double closestEntityDist = perpWallDist;

            // --- ОБРОБКА ВОРОГІВ (ENEMIES) ---
            // У цьому движку спрайти малюються як "вертикальні смужки" всередині циклу стін.
            // Ми перевіряємо, чи перетинає поточний промінь "хітбокс" кожного ворога.
            double enemyDist = Double.MAX_VALUE;
            double enemyHitX = 0;
            int enemySide = 0;
            boolean hitEnemy = false;

            for (Enemy e : level.enemies) {
                // Спрощена перевірка перетину променя з квадратом ворога (AABB intersection)
                double r = 0.4; // Радіус ворога
                double minX = e.x - r; double maxX = e.x + r;
                double minZ = e.z - r; double maxZ = e.z + r;

                // Розрахунок точок входу і виходу променя в квадрат ворога
                double t1 = (minX - xPos) / rayDirX;
                double t2 = (maxX - xPos) / rayDirX;
                double t3 = (minZ - zPos) / rayDirZ;
                double t4 = (maxZ - zPos) / rayDirZ;

                double tmin = Math.max(Math.min(t1, t2), Math.min(t3, t4));
                double tmax = Math.min(Math.max(t1, t2), Math.max(t3, t4));

                // Якщо перетин є і він перед нами (tmin > 0)
                if (tmax >= tmin && tmin > 0) {
                    if (tmin < enemyDist) {
                        enemyDist = tmin;
                        hitEnemy = true;

                        // Розрахунок точки удару по текстурі ворога
                        double intersectX = xPos + rayDirX * tmin;
                        double intersectZ = zPos + rayDirZ * tmin;

                        // Визначаємо, з якого боку ми бачимо ворога (для вибору текстури/затінення)
                        if (Math.abs(intersectX - minX) < 0.01) { enemyHitX = intersectZ - minZ; enemySide = 0; }
                        else if (Math.abs(intersectX - maxX) < 0.01) { enemyHitX = intersectZ - minZ; enemySide = 0; }
                        else if (Math.abs(intersectZ - minZ) < 0.01) { enemyHitX = intersectX - minX; enemySide = 1; }
                        else { enemyHitX = intersectX - minX; enemySide = 1; }
                    }
                }
            }

            // Якщо ми влучили у ворога і він ближче за стіну (не за стіною)
            if (hitEnemy && enemyDist < closestEntityDist) {
                closestEntityDist = enemyDist; // Оновлюємо найближчу перешкоду

                // Розрахунок висоти спрайта ворога (аналогічно стінам)
                int eHeight = (int) ((height * 16) / enemyDist);
                int eStart = -eHeight / 2 + height / 2 + pitch + jumpOffset;
                int eEnd = eHeight / 2 + height / 2 + pitch + jumpOffset;

                if (eStart < 0) eStart = 0;
                if (eEnd >= height) eEnd = height - 1;

                Render eTex = Texture.enemy;
                // Розрахунок координати X на текстурі ворога
                int eTexX = (int)(enemyHitX * eTex.width * 1.25) & 63;

                // Малювання вертикальної смуги ворога
                for (int y = eStart; y < eEnd; y++) {
                    if (y >= 0 && y < height) {
                        long d = (long)(y - pitch - jumpOffset) * 256 - height * 128 + eHeight * 128;
                        int texY = (int)((d * eTex.width) / eHeight) / 256;
                        if (texY < 0) texY = 0; if (texY >= 64) texY = 63;

                        int col = eTex.pixels[eTexX + texY * 64];
                        // Просте затінення однієї сторони ворога для об'єму (бітовий зсув робить колір темнішим)
                        if (enemySide == 1) col = (col & 0xFEFEFE) >> 1;

                        pixels[x + y * width] = col;
                        zBuffer[x + y * width] = enemyDist;
                    }
                }
            }

            // --- ОБРОБКА ПРЕДМЕТІВ (ITEMS) ---
            // Логіка повністю аналогічна до ворогів.
            // Перевіряємо перетин променя з "кубом" предмета.
            double itemDist = Double.MAX_VALUE;
            double itemHitX = 0;
            int itemSide = 0;
            boolean hitItem = false;

            for (Item item : level.items) {
                double r = 0.3; // Радіус предмета трохи менший за ворога
                double minX = item.x - r; double maxX = item.x + r;
                double minZ = item.z - r; double maxZ = item.z + r;

                // Математика перетину променя з AABB (Axis-Aligned Bounding Box)
                double t1 = (minX - xPos) / rayDirX;
                double t2 = (maxX - xPos) / rayDirX;
                double t3 = (minZ - zPos) / rayDirZ;
                double t4 = (maxZ - zPos) / rayDirZ;

                double tmin = Math.max(Math.min(t1, t2), Math.min(t3, t4));
                double tmax = Math.min(Math.max(t1, t2), Math.max(t3, t4));

                if (tmax >= tmin && tmin > 0) {
                    if (tmin < itemDist) {
                        itemDist = tmin;
                        hitItem = true;

                        double intersectX = xPos + rayDirX * tmin;
                        double intersectZ = zPos + rayDirZ * tmin;

                        if (Math.abs(intersectX - minX) < 0.01) { itemHitX = intersectZ - minZ; itemSide = 0; }
                        else if (Math.abs(intersectX - maxX) < 0.01) { itemHitX = intersectZ - minZ; itemSide = 0; }
                        else if (Math.abs(intersectZ - minZ) < 0.01) { itemHitX = intersectX - minX; itemSide = 1; }
                        else { itemHitX = intersectX - minX; itemSide = 1; }
                    }
                }
            }

            // Малювання предмета, якщо він є найближчим об'єктом (ближче за стіну і ворога)
            if (hitItem && itemDist < closestEntityDist) {
                int iHeight = (int) ((height * 16) / itemDist);
                int iEnd = iHeight / 2 + height / 2 + pitch + jumpOffset;

                // Робимо предмет маленьким (як скриня на підлозі)
                // Висота 1/32 від повної висоти блоку
                int cubeHeight = Math.max(1, iHeight / 32);
                int iStart = iEnd - cubeHeight;

                if (iStart < 0) iStart = 0;
                if (iEnd >= height) iEnd = height - 1;

                Render iTex = Texture.yellowSquare;
                int iTexX = (int)(itemHitX * iTex.width * 1.6) & 63;

                for (int y = iStart; y < iEnd; y++) {
                    if (y >= 0 && y < height) {
                        // Текстурування
                        long d = (long)(y - pitch - jumpOffset) * 256 - height * 128 + iHeight * 128;
                        int texY = (int)((d * iTex.width) / iHeight) / 256;
                        if (texY < 0) texY = 0; if (texY >= 64) texY = 63;

                        int col = iTex.pixels[iTexX + texY * 64];

                        // Додаємо жовту "кришку" зверху предмета для 3D-ефекту
                        if (y <= iStart + 1) {
                            col = 0xFFE6B800;
                        } else if (itemSide == 1) {
                            // Затінення бокової сторони
                            col = (col & 0xFEFEFE) >> 1;
                        }

                        pixels[x + y * width] = col;
                        zBuffer[x + y * width] = itemDist;
                    }
                }
            }
        }
    }

    /**
     * Метод renderDistanceLimiter.
     * Застосовує ефект затемнення (туману) в залежності від відстані.
     * Чим далі об'єкт (значення в zBuffer), тим темнішим він стає.
     * Це приховує артефакти на горизонті і додає атмосферності.
     */
    public void renderDistanceLimiter() {
        for (int i = 0; i < width * height; i++) {
            int color = pixels[i];

            // Розрахунок яскравості: чим більший zBuffer[i] (дистанція), тим менша яскравість.
            // Якщо zBuffer = 0 (впритул), яскравість максимальна.
            int brightness = (int) (renderDistance / (zBuffer[i]));

            // Обмеження значень яскравості від 0 до 255
            if (brightness < 0) brightness = 0;
            if (brightness > 255) brightness = 255;

            // Розбиваємо колір на компоненти R, G, B за допомогою бітових зсувів
            int r = (color >> 16) & 0xff; // Отримуємо червоний
            int g = (color >> 8) & 0xff;  // Отримуємо зелений
            int b = (color) & 0xff;       // Отримуємо синій

            // Застосовуємо коефіцієнт яскравості до кожного каналу
            r = r * brightness / 255;
            g = g * brightness / 255;
            b = b * brightness / 255;

            // Збираємо колір назад в одне число
            pixels[i] = r << 16 | g << 8 | b;
        }
    }
}