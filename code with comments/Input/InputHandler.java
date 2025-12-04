package Input;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Клас InputHandler (Обробник введення).
 * Цей клас відповідає за перехоплення всіх дій користувача:
 * натискання клавіш, рух мишкою, кліки.
 * * Він імплементує (реалізує) чотири інтерфейси Java AWT, кожен з яких
 * відповідає за свій тип подій.
 */
public class InputHandler implements KeyListener, FocusListener, MouseListener, MouseMotionListener {

    /**
     * Масив станів клавіш.
     * Індекс масиву відповідає коду клавіші (KeyEvent.VK_...),
     * а значення true/false означає, чи натиснута кнопка зараз.
     * Розмір 68836 взято з запасом, щоб вмістити всі можливі коди клавіш.
     */
    public boolean[] key = new boolean[68836];

    /**
     * Координати миші.
     * Static дозволяє отримати доступ до позиції миші з будь-якого місця програми (наприклад, з Display).
     */
    public static int mouseX;
    public static int mouseY;

    /**
     * Прапорець взаємодії.
     * Стає true, коли гравець натискає Праву Кнопку Миші (ПКМ).
     * Використовується для підбору предметів.
     */
    public boolean interact = false;

    // --- ОБРОБКА РУХУ МИШІ ---

    /**
     * Метод mouseDragged.
     * Викликається, коли миша рухається із затиснутою кнопкою.
     * Нам це не потрібно, тому залишаємо порожнім.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * Метод mouseMoved.
     * Викликається постійно, коли курсор рухається по вікну.
     * Ми записуємо нові координати, щоб повертати камеру в грі.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    // --- ОБРОБКА КЛІКІВ МИШІ ---

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Метод mousePressed.
     * Викликається в момент натискання кнопки миші.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // BUTTON3 — це права кнопка миші.
        if (e.getButton() == MouseEvent.BUTTON3) {
            interact = true; // Активуємо режим підбору предметів
        }
    }

    /**
     * Метод mouseReleased.
     * Викликається, коли кнопку миші відпустили.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            interact = false; // Вимикаємо режим підбору
        }
    }

    // --- ОБРОБКА ФОКУСУ ВІКНА ---

    @Override
    public void focusGained(FocusEvent e) {
    }

    /**
     * Метод focusLost (Втрата фокусу).
     * Дуже важливий метод! Якщо гравець зверне гру (Alt+Tab) або клікне повз вікно,
     * гра може подумати, що клавіша (наприклад, 'W') все ще затиснута, і гравець буде бігти вічно.
     * Цей метод "відпускає" всі клавіші, коли вікно втрачає активність.
     */
    @Override
    public void focusLost(FocusEvent e) {
        for (int i = 0; i < key.length; i++) {
            key[i] = false;
        }
    }

    // --- ОБРОБКА КЛАВІАТУРИ ---

    /**
     * Метод keyPressed.
     * Викликається, коли клавіша фізично натиснута вниз.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode(); // Отримуємо числовий код клавіші
        // Перевіряємо, чи код входить у межі нашого масиву, щоб уникнути помилок
        if(keyCode > 0 && keyCode < key.length) {
            key[keyCode] = true; // Позначаємо клавішу як натиснуту
        }
    }

    /**
     * Метод keyReleased.
     * Викликається, коли клавішу відпустили (вона піднялася вгору).
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode > 0 && keyCode < key.length) {
            key[keyCode] = false; // Позначаємо клавішу як відпущену
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}