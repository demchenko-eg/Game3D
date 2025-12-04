package Index;

// Імпорти стандартних бібліотек Java для графіки, подій та інтерфейсу
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// Імпорти наших власних класів
import Graphics.Render;
import Graphics.Screen;
import Input.Controller;
import Input.InputHandler;

/**
 * Клас Display (Головне вікно гри).
 * Цей клас є точкою входу в програму. Він відповідає за:
 * 1. Створення вікна (JFrame) та полотна для малювання (Canvas).
 * 2. Запуск головного ігрового циклу (Game Loop).
 * 3. Обробку введення (миша, клавіатура).
 * 4. Відображення графічного інтерфейсу (меню, HUD).
 */
public class Display extends Canvas implements Runnable {

    private static final long serialVersionUID = 1L;

    // Константи розміру вікна
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final String TITLE = "Akshay's 3D Game";

    // Головне вікно програми
    public static JFrame frame;

    // Потік, у якому працює гра
    private Thread thread;

    // Об'єкти для логіки та відображення гри
    private Screen screen;
    private Game game;
    private BufferedImage img;
    private boolean running = false; // Прапорець, чи запущена гра
    private Render render;
    private int[] pixels; // Масив пікселів, який ми будемо змінювати напряму
    private InputHandler input; // Обробник клавіатури та миші

    // Змінні для статистики FPS
    private int newX = 0;
    private int oldX = 0;
    private int fps;

    // Robot використовується для захоплення миші (щоб вона не вилітала з вікна)
    private Robot robot;

    // Змінні для повноекранного режиму
    private boolean isFullScreen = false;
    private boolean f11Pressed = false;

    // Панелі інтерфейсу (Меню, Game Over, Налаштування)
    public static JPanel mainMenuPanel;
    public static JPanel gameOverPanel;
    public static JPanel settingsPanel;

    /**
     * Конструктор Display.
     * Ініціалізує основні компоненти гри.
     */
    public Display() {
        try {
            // Ініціалізація робота для керування мишею
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Створення екрана та обробника введення
        screen = new Screen(WIDTH, HEIGHT);
        input = new InputHandler();

        // Створення об'єкта гри
        game = new Game(input);

        // Створення буфера зображення, в який ми будемо малювати пікселі
        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        // Отримуємо доступ до масиву пікселів цього зображення для швидкого малювання
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        // Підключаємо слухачі подій (клавіатура, миша, фокус вікна)
        addKeyListener(input);
        addFocusListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);
    }

    /**
     * Метод start.
     * Запускає новий потік для гри.
     */
    private void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this);
        thread.start(); // Викликає метод run()
    }

    /**
     * Метод stop.
     * Зупиняє ігровий потік.
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            thread.join(); // Чекаємо завершення потоку
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Метод run (Головний ігровий цикл).
     * Це "серце" гри. Воно працює постійно, поки гра запущена.
     * Відповідає за те, щоб гра працювала з стабільною швидкістю (60 тіків на секунду)
     * незалежно від потужності комп'ютера.
     */
    public void run() {
        int frames = 0; // Лічильник кадрів (для FPS)
        double unprocessedSeconds = 0; // Накопичений час, який треба обробити
        long previousTime = System.nanoTime();
        double secondsPerTick = 1 / 60.0; // Скільки часу має займати один ігровий такт (тік)
        int tickCount = 0;
        boolean ticked = false;

        while (running) {
            long currentTime = System.nanoTime();
            long passedTime = currentTime - previousTime;
            previousTime = currentTime;
            unprocessedSeconds += passedTime / 1000000000.0;

            // Завжди тримаємо фокус на грі, щоб працювала клавіатура
            requestFocus();

            // Якщо накопичився час, виконуємо ігрову логіку (tick)
            // Це може виконатися кілька разів підряд, якщо комп'ютер "гальмував"
            while (unprocessedSeconds > secondsPerTick) {
                tick();
                unprocessedSeconds -= secondsPerTick;
                ticked = true;
                tickCount++;

                // Кожну секунду (60 тіків) оновлюємо лічильник FPS в консолі
                if (tickCount % 60 == 0) {
                    System.out.println(frames + "fps");
                    fps = frames;
                    previousTime += 1000;
                    frames = 0;
                }
            }

            // Малюємо кадр, якщо була зміна стану гри (або просто малюємо)
            if (ticked) {
                render();
                frames++;
            }
            render();
            frames++;

            // --- КЕРУВАННЯ МИШЕЮ ---
            // Якщо вікно активне і гра не програна
            if (hasFocus() && !game.lost) {
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                int mouseX = InputHandler.mouseX;
                int mouseY = InputHandler.mouseY;

                // Розрахунок зміщення миші від центру екрана
                int dx = mouseX - centerX;
                int dy = mouseY - centerY;

                // Поворот камери вліво/вправо
                game.controls.rotation += dx * 0.005;

                // Нахил камери вгору/вниз (з обмеженням)
                game.controls.rotationPitch -= dy * 1.0;
                if (game.controls.rotationPitch > 600) game.controls.rotationPitch = 600;
                if (game.controls.rotationPitch < -600) game.controls.rotationPitch = -600;

                // Повертаємо курсор миші назад у центр екрана (щоб можна було крутитися нескінченно)
                if (mouseX != centerX || mouseY != centerY) {
                    try {
                        Point windowPos = this.getLocationOnScreen();
                        robot.mouseMove(windowPos.x + centerX, windowPos.y + centerY);
                        // Оновлюємо координати в InputHandler, щоб не було ривка камери
                        InputHandler.mouseX = centerX;
                        InputHandler.mouseY = centerY;
                    } catch (Exception e) {
                        // Ігноруємо помилки, якщо вікно згорнуте
                    }
                }
            }
        }
    }

    /**
     * Метод tick (Ігрова логіка).
     * Оновлює стан гри: переміщення гравця, перевірка зіткнень, стан клавіш.
     */
    private void tick() {
        // Передаємо натиснуті клавіші в гру
        game.tick(input.key);

        // Якщо гра програна, зупиняємо цикл і показуємо екран Game Over
        if (game.lost) {
            running = false;
            SwingUtilities.invokeLater(() -> {
                frame.getContentPane().setCursor(Cursor.getDefaultCursor()); // Повертаємо курсор
                frame.setContentPane(gameOverPanel);
                frame.revalidate();
                frame.repaint();
            });
            return;
        }

        // Обробка клавіші F11 для повноекранного режиму
        if (input.key[KeyEvent.VK_F11]) {
            if (!f11Pressed) {
                toggleFullScreen();
                f11Pressed = true;
            }
        } else {
            f11Pressed = false;
        }
    }

    /**
     * Метод toggleFullScreen.
     * Перемикає між віконним і повноекранним режимами.
     */
    private void toggleFullScreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        frame.dispose(); // Тимчасово знищуємо вікно для зміни властивостей

        if (!isFullScreen) {
            frame.setUndecorated(true); // Прибираємо рамки вікна
            device.setFullScreenWindow(frame); // Вмикаємо повний екран
        } else {
            device.setFullScreenWindow(null); // Вимикаємо повний екран
            frame.setUndecorated(false); // Повертаємо рамки
            frame.setSize(WIDTH, HEIGHT);
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        isFullScreen = !isFullScreen;
    }

    /**
     * Метод render (Відображення).
     * Малює гру на екрані.
     */
    private void render() {
        if (game.lost) return;

        // Отримуємо стратегію буферизації (потрійна буферизація для плавності)
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        // Рендеримо 3D-сцену у масив пікселів
        screen.render(game);

        // Копіюємо пікселі з об'єкта Screen у пікселі BufferedImage
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            pixels[i] = screen.pixels[i];
        }

        // Отримуємо графічний контекст для малювання на екрані
        Graphics g = bs.getDrawGraphics();

        // Малюємо готове зображення гри
        g.drawImage(img, 0, 0, getWidth(), getHeight(), null);

        // --- МАЛЮВАННЯ ІНТЕРФЕЙСУ (HUD) ---
        g.setColor(Color.WHITE);
        g.setFont(new Font("Verdana", Font.BOLD, 20));

        // Текст про зібрані предмети
        String itemsLeftText;
        if (game.collectedOnLevel >= Game.requiredOnLevel) {
            itemsLeftText = "Вихід відкрито! (" + game.collectedOnLevel + "/" + Game.requiredOnLevel + ")";
            g.setColor(Color.GREEN);
        } else {
            itemsLeftText = "Зібрати: " + game.collectedOnLevel + " / " + Game.requiredOnLevel;
        }

        // Текст рахунку
        String scoreText = "Рахунок: " + Game.totalScore;

        int uiX = getWidth() - 350;
        g.drawString(itemsLeftText, uiX, 40);
        g.setColor(Color.WHITE);
        g.drawString(scoreText, uiX, 70);

        // Малювання прицілу (перехрестя)
        g.setColor(Color.RED);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int size = 10;
        g.drawLine(cx - size, cy, cx + size, cy);
        g.drawLine(cx, cy - size, cx, cy + size);

        // Вивід FPS
        g.setFont(new Font("Verdana", 2, 20));
        g.setColor(Color.YELLOW);
        g.drawString(fps + " FPS", 15, 40);

        // Завершення малювання та показ кадру
        g.dispose();
        bs.show();
    }

    /**
     * Точка входу в програму (main).
     * Створює всі вікна, панелі та кнопки меню.
     */
    public static void main(String[] args) {
        Display gameInstance = new Display();

        // Налаштування головного вікна
        frame = new JFrame();
        frame.setTitle(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null); // По центру екрана
        frame.setResizable(false);

        // Створення панелей інтерфейсу
        mainMenuPanel = new JPanel(new GridBagLayout());
        mainMenuPanel.setBackground(Color.BLACK);

        JPanel difficultyPanel = new JPanel(new GridBagLayout());
        difficultyPanel.setBackground(Color.BLACK);

        gameOverPanel = new JPanel(new GridBagLayout());
        gameOverPanel.setBackground(Color.RED.darker().darker());

        settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(Color.BLACK);

        // Налаштування GridBagLayout для розміщення кнопок
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0); // Відступи
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- ГОЛОВНЕ МЕНЮ ---
        JButton btnStart = createButton("Почати гру");
        JButton btnExit = createButton("Вийти з гри");
        mainMenuPanel.add(btnStart, gbc);
        gbc.gridy++;
        mainMenuPanel.add(btnExit, gbc);

        // --- ВИБІР СКЛАДНОСТІ ---
        gbc.gridy = 0;
        JButton btnEasy = createButton("Просто (20x20)");
        JButton btnMedium = createButton("Середньо (50x50)");
        JButton btnHard = createButton("Складно (100x100)");
        JButton btnSettings = createButton("Налаштувати");
        JButton btnBack = createButton("Повернутись");

        difficultyPanel.add(btnEasy, gbc); gbc.gridy++;
        difficultyPanel.add(btnMedium, gbc); gbc.gridy++;
        difficultyPanel.add(btnHard, gbc); gbc.gridy++;
        difficultyPanel.add(btnSettings, gbc); gbc.gridy++;
        difficultyPanel.add(btnBack, gbc);

        // --- НАЛАШТУВАННЯ (CUSTOM) ---
        gbc.gridy = 0;
        JTextField textWidth = createTextField("20");
        JTextField textHeight = createTextField("20");
        JTextField textTotalItems = createTextField("30");
        JTextField textReqItems = createTextField("20");
        JTextField textEnemies = createTextField("1");
        JTextField textHearingRadius = createTextField("4");
        JTextField textEnemySpeed = createTextField("0.5");

        addSettingRow(settingsPanel, "Ширина карти:", textWidth, gbc);
        addSettingRow(settingsPanel, "Висота карти:", textHeight, gbc);
        addSettingRow(settingsPanel, "Всього точок:", textTotalItems, gbc);
        addSettingRow(settingsPanel, "Зібрати точок:", textReqItems, gbc);
        addSettingRow(settingsPanel, "Кількість ворогів:", textEnemies, gbc);
        addSettingRow(settingsPanel, "Радіус слуху:", textHearingRadius, gbc);
        addSettingRow(settingsPanel, "Швидкість ворогів:", textEnemySpeed, gbc);

        JButton btnStartCustom = createButton("Грати");
        JButton btnBackFromSettings = createButton("Повернутись");

        gbc.gridwidth = 2;
        settingsPanel.add(btnStartCustom, gbc); gbc.gridy++;
        settingsPanel.add(btnBackFromSettings, gbc);

        // --- ЕКРАН ПРОГРАШУ (GAME OVER) ---
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel lblGameOver = new JLabel("GAME OVER");
        lblGameOver.setFont(new Font("Arial", Font.BOLD, 48));
        lblGameOver.setForeground(Color.WHITE);
        lblGameOver.setHorizontalAlignment(JLabel.CENTER);
        gameOverPanel.add(lblGameOver, gbc);

        gbc.gridy++;
        JButton btnRestart = createButton("Заново");
        JButton btnMenu = createButton("Головне меню");
        JButton btnExitGame = createButton("Вихід");
        gameOverPanel.add(btnRestart, gbc);
        gbc.gridy++;
        gameOverPanel.add(btnMenu, gbc);
        gbc.gridy++;
        gameOverPanel.add(btnExitGame, gbc);

        // --- ЛОГІКА КНОПОК ---

        // Перехід до вибору складності
        btnStart.addActionListener(e -> {
            frame.setContentPane(difficultyPanel);
            frame.revalidate();
        });

        // Вихід з гри
        btnExit.addActionListener(e -> System.exit(0));

        // Повернення в меню
        btnBack.addActionListener(e -> {
            frame.setContentPane(mainMenuPanel);
            frame.revalidate();
        });

        // Перехід до налаштувань
        btnSettings.addActionListener(e -> {
            frame.setContentPane(settingsPanel);
            frame.revalidate();
        });

        // Повернення з налаштувань
        btnBackFromSettings.addActionListener(e -> {
            frame.setContentPane(difficultyPanel);
            frame.revalidate();
        });

        // Обробник запуску гри (для всіх кнопок складності)
        ActionListener launchGame = e -> {
            JButton source = (JButton)e.getSource();
            // Встановлення параметрів гри залежно від обраної кнопки
            if (source == btnEasy) {
                Game.currentWidth = 20; Game.currentHeight = 20;
                Game.totalItemsOnLevel = 30; Game.requiredOnLevel = 20;
                Game.enemyCount = 1;
                Game.enemyHearingRadius = 4.0;
                Game.enemySpeed = 0.5;
            } else if (source == btnMedium) {
                Game.currentWidth = 50; Game.currentHeight = 50;
                Game.totalItemsOnLevel = 55; Game.requiredOnLevel = 50;
                Game.enemyCount = 3;
                Game.enemyHearingRadius = 6.0;
                Game.enemySpeed = 0.6;
            } else if (source == btnHard) {
                Game.currentWidth = 100; Game.currentHeight = 100;
                Game.totalItemsOnLevel = 100; Game.requiredOnLevel = 100;
                Game.enemyCount = 5;
                Game.enemyHearingRadius = 9.0;
                Game.enemySpeed = 0.75;
            } else if (source == btnStartCustom) {
                // Зчитування користувацьких налаштувань
                try {
                    Game.currentWidth = Integer.parseInt(textWidth.getText());
                    Game.currentHeight = Integer.parseInt(textHeight.getText());
                    Game.totalItemsOnLevel = Integer.parseInt(textTotalItems.getText());
                    Game.requiredOnLevel = Integer.parseInt(textReqItems.getText());
                    Game.enemyCount = Integer.parseInt(textEnemies.getText());
                    Game.enemyHearingRadius = Double.parseDouble(textHearingRadius.getText());
                    Game.enemySpeed = Double.parseDouble(textEnemySpeed.getText());
                } catch (NumberFormatException ex) {
                    return; // Ігноруємо некоректні дані
                }
            }
            startNewGame(gameInstance);
        };

        btnEasy.addActionListener(launchGame);
        btnMedium.addActionListener(launchGame);
        btnHard.addActionListener(launchGame);
        btnStartCustom.addActionListener(launchGame);

        // Кнопки Game Over меню
        btnRestart.addActionListener(e -> startNewGame(gameInstance));

        btnMenu.addActionListener(e -> {
            frame.setContentPane(mainMenuPanel);
            frame.revalidate();
        });

        btnExitGame.addActionListener(e -> System.exit(0));

        // Показуємо головне меню при старті
        frame.setContentPane(mainMenuPanel);
        frame.setVisible(true);
    }

    /**
     * Метод startNewGame.
     * Запускає нову гру: зупиняє стару, створює новий екземпляр Display,
     * ховає курсор миші і запускає потік.
     */
    private static void startNewGame(Display display) {
        display.stop(); // Зупиняємо попередній екземпляр

        Display newGame = new Display();
        JPanel gameContainer = new JPanel(new BorderLayout());
        gameContainer.add(newGame);

        frame.setContentPane(gameContainer);
        frame.revalidate();

        // Створення прозорого курсору (щоб його не було видно під час гри)
        BufferedImage cursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blank = Toolkit.getDefaultToolkit().createCustomCursor(cursor, new Point(0, 0), "blank");
        frame.getContentPane().setCursor(blank);

        newGame.requestFocus();
        newGame.start();
    }

    // --- ДОПОМІЖНІ МЕТОДИ ДЛЯ СТВОРЕННЯ UI ---

    private static JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(400, 50));
        return btn;
    }

    private static JTextField createTextField(String defaultText) {
        JTextField tf = new JTextField(defaultText);
        tf.setFont(new Font("Arial", Font.PLAIN, 18));
        tf.setPreferredSize(new Dimension(200, 30));
        return tf;
    }

    private static void addSettingRow(JPanel panel, String labelText, JTextField textField, GridBagConstraints gbc) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(Color.WHITE);

        gbc.gridwidth = 1;
        gbc.weightx = 0.3;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(textField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
    }
}