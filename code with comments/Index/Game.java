package Index;

import java.awt.event.KeyEvent;
import Input.Controller;
import Input.InputHandler;

/**
 * Клас Game (Гра).
 * Це центральний клас, який керує логікою та правилами гри.
 * Він зв'язує введення гравця, карту рівня, ворогів та умови перемоги/поразки.
 */
public class Game {

    /**
     * Лічильник часу (у тіках).
     * Збільшується на 1 кожен кадр оновлення.
     */
    public int time;

    /**
     * Контролер гравця.
     * Відповідає за координати, поворот камери та фізику руху гравця.
     */
    public Controller controls;

    /**
     * Поточний рівень.
     * Містить карту стін, список ворогів та предметів.
     */
    public Level level;

    /**
     * Прапорець поразки.
     * Якщо true — гра зупиняється і показується екран Game Over.
     */
    public boolean lost = false;

    /**
     * Обробник введення.
     * Посилання на об'єкт, який знає, які клавіші натиснуті.
     */
    public InputHandler input;

    // --- СТАТИСТИКА ГРИ ---

    /**
     * Загальний рахунок.
     * Static означає, що очки зберігаються при переході між рівнями.
     */
    public static int totalScore = 0;

    // --- НАЛАШТУВАННЯ СКЛАДНОСТІ (змінюються з меню) ---
    public static int currentWidth = 20;       // Ширина рівня
    public static int currentHeight = 20;      // Висота рівня
    public static int totalItemsOnLevel = 30;  // Скільки всього предметів генерувати
    public static int requiredOnLevel = 20;    // Скільки треба зібрати для виходу
    public static int enemyCount = 1;          // Кількість ворогів
    public static double enemyHearingRadius = 4.0; // Як далеко чують вороги
    public static double enemySpeed = 0.5;     // Швидкість ворогів

    // --- СТАН ПОТОЧНОГО РІВНЯ ---
    public int collectedOnLevel = 0; // Скільки зібрано на цьому рівні
    public boolean exitOpen = false; // Чи відкрився вихід

    /**
     * Конструктор гри.
     * Приймає обробник введення і запускає перший рівень.
     */
    public Game(InputHandler input) {
        this.input = input;
        startLevel(); // Генерація та запуск рівня
    }

    /**
     * Метод startLevel (Почати рівень).
     * Генерує нову карту, скидає прогрес рівня та ставить гравця на точку спавну.
     */
    private void startLevel() {
        // Створення нового процедурного рівня з заданими параметрами
        level = new Level(currentWidth, currentHeight, totalItemsOnLevel, enemyCount);

        // Створення нового гравця
        controls = new Controller();

        // Телепортація гравця на безпечну точку спавну (далеко від стін)
        controls.x = level.spawnX;
        controls.z = level.spawnZ;

        // Скидання лічильників поточного рівня
        collectedOnLevel = 0;
        exitOpen = false;
    }

    /**
     * Метод tick (Оновлення гри).
     * Виконується 60 разів на секунду. Тут відбувається вся магія.
     *
     * @param key Масив стану клавіш (натиснута чи ні).
     */
    public void tick(boolean[] key) {
        // Якщо програли, зупиняємо оновлення логіки
        if (lost) return;

        time++;

        // 1. ЗЧИТУВАННЯ КЛАВІШ
        boolean forward = key[KeyEvent.VK_W];
        boolean back = key[KeyEvent.VK_S];
        boolean left = key[KeyEvent.VK_A];
        boolean right = key[KeyEvent.VK_D];
        boolean jump = key[KeyEvent.VK_SPACE];
        boolean crouch = key[KeyEvent.VK_CONTROL];
        boolean run = key[KeyEvent.VK_SHIFT];

        // 2. ОНОВЛЕННЯ ГРАВЦЯ
        // Передаємо натиснуті кнопки в контролер, щоб він розрахував рух
        controls.tick(forward, back, left, right, jump, crouch, run, level);

        // 3. ОНОВЛЕННЯ ВОРОГІВ
        for (Enemy enemy : level.enemies) {
            // Кожен ворог "думає" і рухається
            enemy.tick(controls, level);

            // Перевірка зіткнення гравця з ворогом
            // Формула відстані: sqrt((x2-x1)^2 + (z2-z1)^2)
            double dist = Math.sqrt(Math.pow(controls.x - enemy.x, 2) + Math.pow(controls.z - enemy.z, 2));

            // Якщо ворог ближче ніж 0.8 блоку -> GAME OVER
            if (dist < 0.8) {
                lost = true;
                totalScore = 0; // Скидаємо очки при програші
            }
        }

        // 4. ПЕРЕВІРКА ВИХОДУ З РІВНЯ
        if (exitOpen) {
            // Рахуємо відстань до точки виходу
            double distToExit = Math.sqrt(Math.pow(controls.x - level.exitPixelX, 2) + Math.pow(controls.z - level.exitPixelZ, 2));

            // Якщо ми дійшли до виходу (дистанція < 15 пікселів/одиниць) -> Наступний рівень
            if (distToExit < 15.0) {
                startLevel();
            }
        }

        // 5. ВЗАЄМОДІЯ З ПРЕДМЕТАМИ (ПІДБІР)
        // Якщо натиснута кнопка дії (наприклад, 'E' або ЛКМ)
        if (input.interact) {
            input.interact = false; // Одразу скидаємо прапорець, щоб не підібрати 100 разів за одне натискання

            for (int i = 0; i < level.items.size(); i++) {
                Item item = level.items.get(i);

                // Вектор від гравця до предмета
                double vx = item.x - controls.x;
                double vz = item.z - controls.z;

                // Відстань до предмета
                double dist = Math.sqrt(vx * vx + vz * vz);

                // Якщо предмет досить близько (радіус дії 20 одиниць)
                if (dist < 20.0) {
                    // --- МАТЕМАТИКА ПРИЦІЛУ ---
                    // Нам треба перевірити, чи дивиться гравець прямо на предмет.

                    // Вектор погляду гравця (напрямок камери)
                    double dx = Math.sin(controls.rotation);
                    double dz = Math.cos(controls.rotation);

                    // Скалярний добуток (Dot Product).ц
                    // Показує, чи знаходиться предмет "попереду" (значення > 0) чи "позаду" (< 0).
                    double distanceToItem = vx * dx + vz * dz;
                    if (distanceToItem < 0) continue; // Предмет за спиною - ігноруємо

                    // "Псевдо-векторний добуток" (Cross Product у 2D).
                }
            }
        }
    }
}