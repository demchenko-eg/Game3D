package Input;

import Index.Level;

/**
 * Клас Controller (Контролер/Гравець).
 * Цей клас керує переміщенням гравця у світі. Він обробляє введення з клавіатури,
 * розраховує фізику руху (швидкість, прискорення, стрибки) та перевіряє зіткнення зі стінами.
 */
public class Controller {

    // --- КООРДИНАТИ ТА ПОЗИЦІЯ ---
    public double x, z; // Позиція гравця на карті (площина підлоги)
    public double y;    // Висота гравця (вгору/вниз). 0 - це рівень підлоги.

    // --- ПОВОРОТ ---
    public double rotation;      // Кут повороту голови вліво/вправо (у радіанах)
    public double rotationPitch = 0; // Нахил голови вгору/вниз

    // --- ФІЗИКА РУХУ (Velocity/Acceleration) ---
    // Суфікс 'a' означає "acceleration" (прискорення) або поточну швидкість зміни координати.
    public double xa, za; // Швидкість руху по осях X та Z
    public double rotationa; // Швидкість повороту
    public double ya = 0; // Вертикальна швидкість (для стрибків та гравітації)

    // --- СТАНИ ГРАВЦЯ ---
    public boolean isJumping = false;   // Чи знаходиться гравець у повітрі?
    public boolean isMoving = false;    // Чи натиснуті кнопки руху?
    public boolean isCrouching = false; // Чи присів гравець?

    // Статичні змінні для керування поворотом (можуть змінюватися з інших класів)
    public static boolean turnLeft = false;
    public static boolean turnRight = false;
    public static boolean walk = false;

    /**
     * Метод tick (Оновлення фізики).
     * Викликається кожен кадр гри. Розраховує нову позицію гравця.
     *
     * @param forward Натиснуто W (вперед)
     * @param back    Натиснуто S (назад)
     * @param left    Натиснуто A (вліво)
     * @param right   Натиснуто D (вправо)
     * @param jump    Натиснуто SPACE (стрибок)
     * @param crouch  Натиснуто CTRL (присісти)
     * @param run     Натиснуто SHIFT (біг)
     * @param level   Посилання на рівень для перевірки зіткнень
     */
    public void tick(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean crouch, boolean run, Level level) {
        // Базові налаштування швидкості
        double rotationSpeed = 0.0076; // Як швидко повертається камера
        double walkSpeed = 0.5;        // Звичайна швидкість ходьби
        double groundLevel = 0;        // Рівень землі (куди ми приземляємось)

        // Оновлюємо прапорці станів
        isCrouching = crouch;
        isMoving = forward || back || left || right;

        // --- ЛОГІКА ПРИСІДАННЯ ТА БІГУ ---
        if (crouch) {
            walkSpeed = 0.2;     // Уповільнюємось
            run = false;         // Не можна бігати навприсядки
            groundLevel = -0.5;  // "Опускаємо" камеру вниз (змінюємо рівень підлоги)
        } else if (run) {
            walkSpeed = 0.8;     // Прискорюємось
        }

        // Вектори руху (локальні: 1 = вперед/вправо, -1 = назад/вліво)
        double xMove = 0;
        double zMove = 0;

        if (forward) {
            zMove++; // Вперед
            walk = true;
        }
        if (back) {
            zMove--; // Назад
            walk = true;
        }
        if (left) {
            xMove--; // Вліво (стрейф)
            walk = true;
        }
        if (right) {
            xMove++; // Вправо (стрейф)
            walk = true;
        }

        // Поворот клавішами (якщо не використовується миша)
        if (turnLeft) {
            rotationa -= rotationSpeed;
        }
        if (turnRight) {
            rotationa += rotationSpeed;
        }

        // --- ФІЗИКА СТРИБКА ---
        // Стрибаємо тільки якщо натиснуто Space, ми не в повітрі і не присіли
        if (jump && !isJumping && !crouch) {
            ya = 1.2; // Надаємо імпульс вгору
            isJumping = true;
        }

        // Гравітація: зменшуємо вертикальну швидкість кожен кадр
        ya -= 0.1;
        y += ya; // Змінюємо висоту

        // Приземлення
        if (y <= groundLevel) {
            y = groundLevel; // Фіксуємо на рівні землі
            ya = 0;          // Скидаємо вертикальну швидкість
            isJumping = false;
        }

        // Якщо нічого не натиснуто, вимикаємо анімацію ходьби
        if (!forward && !back && !left && !right && !turnLeft && !turnRight && !crouch) {
            walk = false;
        }

        // --- МАТЕМАТИКА РУХУ (ПЕРЕТВОРЕННЯ КООРДИНАТ) ---
        // Нам потрібно перетворити локальний рух (W/A/S/D) у глобальні координати світу,
        // враховуючи, куди дивиться гравець (rotation).
        // Формула повороту вектора:
        // X_new = X * cos(angle) + Z * sin(angle)
        // Z_new = Z * cos(angle) - X * sin(angle)

        // Додаємо розрахований рух до прискорення (xa, za)
        xa += (xMove * Math.cos(rotation) + zMove * Math.sin(rotation)) * walkSpeed;
        za += (zMove * Math.cos(rotation) - xMove * Math.sin(rotation)) * walkSpeed;

        // --- КОЛІЗІЇ (ЗІТКНЕННЯ) ---
        // Ми перевіряємо осі X та Z окремо. Це дозволяє "ковзати" вздовж стін.

        // Перевірка руху по X
        if (isFree(x + xa, z, level)) {
            x += xa; // Якщо вільно - рухаємось
        } else {
            xa = 0; // Якщо стіна - зупиняємось (обнуляємо швидкість)
        }

        // Перевірка руху по Z
        if (isFree(x, z + za, level)) {
            z += za; // Якщо вільно - рухаємось
        } else {
            za = 0; // Якщо стіна - зупиняємось
        }

        // --- ТЕРТЯ (FRICTION) ---
        // Ми множимо швидкість на 0.1 кожен кадр.
        // Це змушує гравця швидко зупинятися, якщо він відпустить кнопки.
        // Без цього гравець ковзав би як на льоду нескінченно.
        xa *= 0.1;
        za *= 0.1;

        // Застосування повороту та його гальмування
        rotation += rotationa;
        rotationa *= 0.8;
    }

    /**
     * Метод isFree (Перевірка вільного місця).
     * Перевіряє, чи не застрягне гравець у стіні в заданих координатах.
     *
     * @param xx    Координата X для перевірки.
     * @param zz    Координата Z для перевірки.
     * @param level Посилання на рівень.
     * @return true, якщо шлях вільний. false, якщо там стіна.
     */
    private boolean isFree(double xx, double zz, Level level) {
        // Радіус гравця (Collision Box / Hitbox).
        // Гравець - це квадрат розміром 0.7x0.7 (0.35 в кожну сторону від центру).
        double r = 0.35;

        // Перевіряємо 4 кути навколо майбутньої позиції гравця.
        // Якщо хоч один кут потрапляє в блок стіни (> 0), рух заборонено.
        if (level.getTile((int)(xx - r), (int)(zz - r)) > 0) return false; // Верхній лівий
        if (level.getTile((int)(xx - r), (int)(zz + r)) > 0) return false; // Нижній лівий
        if (level.getTile((int)(xx + r), (int)(zz - r)) > 0) return false; // Верхній правий
        if (level.getTile((int)(xx + r), (int)(zz + r)) > 0) return false; // Нижній правий

        return true;
    }
}