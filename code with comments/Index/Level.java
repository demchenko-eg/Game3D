package Index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Клас Level (Рівень).
 * Цей клас відповідає за генерацію та зберігання карти рівня.
 * Він створює випадковий лабіринт, розміщує гравця, ворогів, предмети та вихід.
 */
public class Level {

    // --- ПАРАМЕТРИ КАРТИ ---
    // Реальна ширина та висота рівня у пікселях (масштабовані)
    public int width;
    public int height;

    // Масив пікселів стін (використовується для Raycasting).
    // Якщо tiles[i] > 0, там є стіна.
    public int[] tiles;

    // Логічна карта лабіринту (сітка).
    // map[x][y] = 1 (стіна), 0 (пусто), 2 (вихід).
    private int[][] map;

    // --- ВАЖЛИВІ КООРДИНАТИ ---
    public double spawnX; // Де з'явиться гравець (X)
    public double spawnZ; // Де з'явиться гравець (Z)

    public double exitPixelX; // Де знаходиться вихід (для перевірки перемоги)
    public double exitPixelZ;

    // --- СПИСКИ ОБ'ЄКТІВ ---
    // Зберігають всіх ворогів та всі предмети на поточному рівні
    public List<Enemy> enemies = new ArrayList<>();
    public List<Item> items = new ArrayList<>();

    /**
     * Конструктор Level.
     * Генерує новий випадковий рівень.
     *
     * @param w          Ширина лабіринту (в клітинках).
     * @param h          Висота лабіринту (в клітинках).
     * @param itemCount  Кількість предметів для генерації.
     * @param enemyCount Кількість ворогів для генерації.
     */
    public Level(int w, int h, int itemCount, int enemyCount) {
        // Алгоритм лабіринту (Recursive Backtracking) вимагає непарних розмірів карти,
        // щоб коректно створювати стіни та проходи.
        if (w % 2 == 0) w++;
        if (h % 2 == 0) h++;

        int mapWidth = w;
        int mapHeight = h;

        // Ініціалізація карти заповненою стінами (1)
        map = new int[mapWidth][mapHeight];
        for(int x = 0; x < mapWidth; x++) {
            for(int y = 0; y < mapHeight; y++) {
                map[x][y] = 1;
            }
        }

        // 1. ГЕНЕРАЦІЯ ЛАБІРИНТУ
        // Починаємо з точки (1,1) і "прогризаємо" проходи
        generateMaze(1, 1);

        Random random = new Random();

        // 2. СТВОРЕННЯ ЦИКЛІВ (LOOPS)
        // Лабіринт, створений Backtracking-ом, є "ідеальним" (деревом) — без циклів.
        // Це нудно, бо є лише один шлях. Ми випадково руйнуємо деякі стіни,
        // щоб можна було бігати кругами.
        for (int x = 1; x < mapWidth - 1; x++) {
            for (int y = 1; y < mapHeight - 1; y++) {
                if (map[x][y] == 1) {
                    // Перевіряємо, чи стіна розділяє два проходи
                    boolean horizontal = map[x-1][y] == 0 && map[x+1][y] == 0;
                    boolean vertical = map[x][y-1] == 0 && map[x][y+1] == 0;

                    // З ймовірністю 20% видаляємо таку стіну
                    if ((horizontal || vertical) && random.nextInt(100) < 20) {
                        map[x][y] = 0;
                    }
                }
            }
        }

        // Масштаб: 1 клітинка лабіринту = 10 ігрових одиниць (пікселів)
        int scale = 10;

        // 3. ГЕНЕРАЦІЯ ВИХОДУ
        // Вихід завжди знаходиться на краю карти.
        int side = random.nextInt(4); // 0=Top, 1=Right, 2=Bottom, 3=Left
        int exitPos = 1 + random.nextInt(mapWidth - 3); // Випадкова позиція на стіні

        // Встановлюємо тип блоку 2 (решітка) для виходу і записуємо координати
        if (side == 0) { // Верхня стіна
            map[exitPos][0] = 2; map[exitPos+1][0]=2; map[exitPos][1]=0; map[exitPos+1][1]=0;
            exitPixelX = (exitPos + 0.5) * scale; exitPixelZ = 0;
        } else if (side == 1) { // Права стіна
            map[mapWidth-1][exitPos]=2; map[mapWidth-1][exitPos+1]=2; map[mapWidth-2][exitPos]=0; map[mapWidth-2][exitPos+1]=0;
            exitPixelX = (mapWidth - 1) * scale; exitPixelZ = (exitPos + 0.5) * scale;
        } else if (side == 2) { // Нижня стіна
            map[exitPos][mapHeight-1]=2; map[exitPos+1][mapHeight-1]=2; map[exitPos][mapHeight-2]=0; map[exitPos+1][mapHeight-2]=0;
            exitPixelX = (exitPos + 0.5) * scale; exitPixelZ = (mapHeight - 1) * scale;
        } else { // Ліва стіна
            map[0][exitPos]=2; map[0][exitPos+1]=2; map[1][exitPos]=0; map[1][exitPos+1]=0;
            exitPixelX = 0; exitPixelZ = (exitPos + 0.5) * scale;
        }

        // 4. СПАВН ГРАВЦЯ
        // Шукаємо випадкову порожню клітинку (0)
        while (true) {
            int rx = random.nextInt(mapWidth);
            int ry = random.nextInt(mapHeight);
            if (map[rx][ry] == 0) {
                // Встановлюємо координати в центр клітинки
                spawnX = rx * scale + scale / 2.0;
                spawnZ = ry * scale + scale / 2.0;
                break;
            }
        }

        // 5. СПАВН ВОРОГІВ
        for (int i = 0; i < enemyCount; i++) {
            int enemyGx, enemyGz;
            while (true) {
                enemyGx = random.nextInt(mapWidth);
                enemyGz = random.nextInt(mapHeight);

                // Перевірка дистанції: ворог не повинен з'явитися прямо перед носом гравця.
                // Мінімум 10 блоків відстані.
                double dist = Math.sqrt(Math.pow(enemyGx - (spawnX/scale), 2) + Math.pow(enemyGz - (spawnZ/scale), 2));

                if (map[enemyGx][enemyGz] == 0 && dist > 10) {
                    break;
                }
            }
            enemies.add(new Enemy(enemyGx * scale + scale/2.0, enemyGz * scale + scale/2.0));
        }

        // 6. СПАВН ПРЕДМЕТІВ
        for (int i = 0; i < itemCount; i++) {
            int itemGx, itemGz;
            while (true) {
                itemGx = random.nextInt(mapWidth);
                itemGz = random.nextInt(mapHeight);
                // Предмет не може з'явитися в стіні або в точці спавну гравця
                if (map[itemGx][itemGz] == 0 && (itemGx != (int)(spawnX/scale) || itemGz != (int)(spawnZ/scale))) {
                    break;
                }
            }

            // Випадковий зсув всередині клітинки, щоб предмети не лежали строго по центру
            double padding = 2.5;
            double availableSpace = scale - (padding * 2);
            double offsetX = padding + random.nextDouble() * availableSpace;
            double offsetZ = padding + random.nextDouble() * availableSpace;

            items.add(new Item(itemGx * scale + offsetX, itemGz * scale + offsetZ));
        }

        // 7. КОНВЕРТАЦІЯ КАРТИ
        // Перетворюємо логічну карту (маленьку сітку) у велику карту пікселів для рендерингу.
        this.width = mapWidth * scale;
        this.height = mapHeight * scale;
        tiles = new int[this.width * this.height];

        refreshTiles(scale);
    }

    /**
     * Метод refreshTiles (Оновити карту пікселів).
     * Заповнює масив tiles на основі масиву map.
     * Це потрібно, бо Raycasting працює з великим масивом пікселів, а логіка — з клітинками.
     * * @param scale Масштаб (1 клітинка = scale пікселів).
     */
    public void refreshTiles(int scale) {
        for (int y = 0; y < map[0].length; y++) {
            for (int x = 0; x < map.length; x++) {
                int type = map[x][y];

                // Якщо це стіна (1) або вихід (2)
                if (type == 1 || type == 2) {
                    int pixelX = x * scale;
                    int pixelY = y * scale;

                    // Записуємо тип блоку у відповідний піксель
                    tiles[pixelX + pixelY * this.width] = type;

                    // Оптимізація: "розтягуємо" значення на сусідні пікселі, щоб не заповнювати все в циклі.
                    // Це заповнює простір між точками сітки.
                    if (x + 1 < map.length && (map[x+1][y] == 1 || map[x+1][y] == 2)) {
                        int fill = (type == 2 && map[x+1][y] == 2) ? 2 : 1;
                        for (int i=1; i<scale; i++) tiles[(pixelX+i)+pixelY*this.width] = fill;
                    }
                    if (y + 1 < map[0].length && (map[x][y+1] == 1 || map[x][y+1] == 2)) {
                        int fill = (type == 2 && map[x][y+1] == 2) ? 2 : 1;
                        for (int i=1; i<scale; i++) tiles[pixelX+(pixelY+i)*this.width] = fill;
                    }
                } else {
                    // Якщо пусто (0), зачищаємо область
                    int pixelX = x * scale;
                    int pixelY = y * scale;
                    for(int xx=0; xx<scale; xx++) {
                        for(int yy=0; yy<scale; yy++) {
                            tiles[(pixelX+xx) + (pixelY+yy)*this.width] = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * Метод openExit (Відкрити вихід).
     * Замінює блоки виходу (тип 2) на повітря (тип 0), фактично прибираючи решітку.
     */
    public void openExit() {
        boolean changed = false;
        for(int x = 0; x < map.length; x++) {
            for(int y = 0; y < map[0].length; y++) {
                if(map[x][y] == 2) {
                    map[x][y] = 0; // Перетворюємо стіну на повітря
                    changed = true;
                }
            }
        }
        if (changed) {
            refreshTiles(10); // Оновлюємо фізичну карту
        }
    }

    /**
     * Метод generateMaze (Рекурсивний генератор лабіринту).
     * Використовує алгоритм Recursive Backtracking (DFS).
     * 1. Обираємо випадковий напрямок.
     * 2. Якщо через одну клітинку стіна — робимо прохід і йдемо туди.
     * 3. Якщо шляху немає — повертаємося назад (backtrack).
     *
     * @param x Поточна координата X.
     * @param y Поточна координата Y.
     */
    private void generateMaze(int x, int y) {
        map[x][y] = 0; // Робимо поточну клітинку порожньою

        // Список напрямків: 0-Вгору, 1-Вниз, 2-Вліво, 3-Вправо
        ArrayList<Integer> dirs = new ArrayList<>();
        dirs.add(0); dirs.add(1); dirs.add(2); dirs.add(3);
        Collections.shuffle(dirs, new Random()); // Перемішуємо для випадковості

        for (int dir : dirs) {
            int dx = 0, dy = 0;
            // Крок через одну клітинку (щоб залишити стіну між проходами)
            if (dir==0) dy=-2;
            if(dir==1) dy=2;
            if(dir==2) dx=-2;
            if(dir==3) dx=2;

            int nx = x + dx;
            int ny = y + dy;

            // Перевірка меж карти та чи відвідували ми цю клітинку (чи вона ще стіна)
            if (nx>0 && nx<map.length-1 && ny>0 && ny<map[0].length-1 && map[nx][ny]==1) {
                // Пробиваємо стіну між поточною і новою клітинкою
                map[x+dx/2][y+dy/2] = 0;
                // Рекурсивно йдемо далі
                generateMaze(nx, ny);
            }
        }
    }

    /**
     * Метод getTile (Отримати блок).
     * Повертає тип блоку за координатами пікселів.
     * Використовується для колізій та Raycasting.
     */
    public int getTile(int x, int z) {
        if (x < 0 || z < 0 || x >= width || z >= height) return 0;
        return tiles[x + z * width];
    }

    /**
     * Метод getTileMap (Отримати блок з логічної карти).
     * Повертає тип блоку за координатами клітинок.
     */
    public int getTileMap(int x, int z) {
        if (x < 0 || z < 0 || x >= map.length || z >= map[0].length) return 1;
        return map[x][z];
    }
}