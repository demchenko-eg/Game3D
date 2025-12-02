package Index;

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

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import Graphics.Render;
import Graphics.Screen;
import Input.Controller;
import Input.InputHandler;

public class Display extends Canvas implements Runnable {

    private static final long serialVersionUID = 1L;

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final String TITLE = "Akshay's 3D Game";

    public static JFrame frame;

    private Thread thread;
    private Screen screen;
    private Game game;
    private BufferedImage img;
    private boolean running = false;
    private Render render;
    private int[] pixels;
    private InputHandler input;
    private int newX = 0;
    private int oldX = 0;
    private int fps;
    private Robot robot;

    private boolean isFullScreen = false;
    private boolean f11Pressed = false;
    private boolean qDebounce = false;

    public static JPanel mainMenuPanel;
    public static JPanel gameOverPanel;
    public static JPanel settingsPanel;

    public Display() {
        try {
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }

        screen = new Screen(WIDTH, HEIGHT);
        input = new InputHandler();

        game = new Game(input);

        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        addKeyListener(input);
        addFocusListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);
    }

    private void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void run() {
        int frames = 0;
        double unprocessedSeconds = 0;
        long previousTime = System.nanoTime();
        double secondsPerTick = 1 / 60.0;
        int tickCount = 0;
        boolean ticked = false;

        while (running) {
            long currentTime = System.nanoTime();
            long passedTime = currentTime - previousTime;
            previousTime = currentTime;
            unprocessedSeconds += passedTime / 1000000000.0;
            requestFocus();

            while (unprocessedSeconds > secondsPerTick) {
                tick();
                unprocessedSeconds -= secondsPerTick;
                ticked = true;
                tickCount++;
                if (tickCount % 60 == 0) {
                    System.out.println(frames + "fps");
                    fps = frames;
                    previousTime += 1000;
                    frames = 0;
                }
            }
            if (ticked) {
                render();
                frames++;
            }
            render();
            frames++;

            if (hasFocus() && !game.lost) {
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                int mouseX = InputHandler.mouseX;
                int mouseY = InputHandler.mouseY;

                int dx = mouseX - centerX;
                int dy = mouseY - centerY;

                game.controls.rotation += dx * 0.005;

                game.controls.rotationPitch -= dy * 1.0;

                if (game.controls.rotationPitch > 600) game.controls.rotationPitch = 600;
                if (game.controls.rotationPitch < -600) game.controls.rotationPitch = -600;

                if (mouseX != centerX || mouseY != centerY) {
                    try {
                        Point windowPos = this.getLocationOnScreen();
                        robot.mouseMove(windowPos.x + centerX, windowPos.y + centerY);
                        InputHandler.mouseX = centerX;
                        InputHandler.mouseY = centerY;
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void tick() {
        game.tick(input.key);

        if (game.lost) {
            running = false;
            SwingUtilities.invokeLater(() -> {
                frame.getContentPane().setCursor(Cursor.getDefaultCursor());
                frame.setContentPane(gameOverPanel);
                frame.revalidate();
                frame.repaint();
            });
            return;
        }

        if (input.key[KeyEvent.VK_F11]) {
            if (!f11Pressed) {
                toggleFullScreen();
                f11Pressed = true;
            }
        } else {
            f11Pressed = false;
        }

        if (input.key[KeyEvent.VK_Q]) {
            if (!qDebounce) {
                showQuitDialog();
                qDebounce = true;
            }
        } else {
            qDebounce = false;
        }
    }

    private void toggleFullScreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        frame.dispose();

        if (!isFullScreen) {
            frame.setUndecorated(true);
            device.setFullScreenWindow(frame);
        } else {
            device.setFullScreenWindow(null);
            frame.setUndecorated(false);
            frame.setSize(WIDTH, HEIGHT);
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        isFullScreen = !isFullScreen;
    }

    private void showQuitDialog() {
        frame.getContentPane().setCursor(Cursor.getDefaultCursor());

        int result = JOptionPane.showConfirmDialog(frame, "Вийти з гри?", "Підтвердження", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        } else {
            BufferedImage cursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Cursor blank = Toolkit.getDefaultToolkit().createCustomCursor(cursor, new Point(0, 0), "blank");
            frame.getContentPane().setCursor(blank);
            requestFocus();
        }
    }

    private void render() {
        if (game.lost) return;

        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        screen.render(game);

        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            pixels[i] = screen.pixels[i];
        }

        Graphics g = bs.getDrawGraphics();
        g.drawImage(img, 0, 0, getWidth(), getHeight(), null);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Verdana", Font.BOLD, 20));

        String itemsLeftText;
        if (game.collectedOnLevel >= Game.requiredOnLevel) {
            itemsLeftText = "Вихід відкрито! (" + game.collectedOnLevel + "/" + Game.requiredOnLevel + ")";
            g.setColor(Color.GREEN);
        } else {
            itemsLeftText = "Зібрати: " + game.collectedOnLevel + " / " + Game.requiredOnLevel;
        }

        String scoreText = "Рахунок: " + Game.totalScore;

        int uiX = getWidth() - 350;
        g.drawString(itemsLeftText, uiX, 40);
        g.setColor(Color.WHITE);
        g.drawString(scoreText, uiX, 70);

        g.setColor(Color.RED);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int size = 10;
        g.drawLine(cx - size, cy, cx + size, cy);
        g.drawLine(cx, cy - size, cx, cy + size);

        g.setFont(new Font("Verdana", 2, 20));
        g.setColor(Color.YELLOW);
        g.drawString(fps + " FPS", 15, 40);

        g.dispose();
        bs.show();
    }

    public static void main(String[] args) {
        Display gameInstance = new Display();

        frame = new JFrame();
        frame.setTitle(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        mainMenuPanel = new JPanel(new GridBagLayout());
        mainMenuPanel.setBackground(Color.BLACK);

        JPanel difficultyPanel = new JPanel(new GridBagLayout());
        difficultyPanel.setBackground(Color.BLACK);

        gameOverPanel = new JPanel(new GridBagLayout());
        gameOverPanel.setBackground(Color.RED.darker().darker());

        settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(Color.BLACK);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton btnStart = createButton("Почати гру");
        JButton btnExit = createButton("Вийти з гри");

        mainMenuPanel.add(btnStart, gbc);
        gbc.gridy++;
        mainMenuPanel.add(btnExit, gbc);

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

        btnStart.addActionListener(e -> {
            frame.setContentPane(difficultyPanel);
            frame.revalidate();
        });

        btnExit.addActionListener(e -> System.exit(0));

        btnBack.addActionListener(e -> {
            frame.setContentPane(mainMenuPanel);
            frame.revalidate();
        });

        btnSettings.addActionListener(e -> {
            frame.setContentPane(settingsPanel);
            frame.revalidate();
        });

        btnBackFromSettings.addActionListener(e -> {
            frame.setContentPane(difficultyPanel);
            frame.revalidate();
        });

        ActionListener launchGame = e -> {
            JButton source = (JButton)e.getSource();
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
                try {
                    Game.currentWidth = Integer.parseInt(textWidth.getText());
                    Game.currentHeight = Integer.parseInt(textHeight.getText());
                    Game.totalItemsOnLevel = Integer.parseInt(textTotalItems.getText());
                    Game.requiredOnLevel = Integer.parseInt(textReqItems.getText());
                    Game.enemyCount = Integer.parseInt(textEnemies.getText());
                    Game.enemyHearingRadius = Double.parseDouble(textHearingRadius.getText());
                    Game.enemySpeed = Double.parseDouble(textEnemySpeed.getText());
                } catch (NumberFormatException ex) {
                    return;
                }
            }
            startNewGame(gameInstance);
        };

        btnEasy.addActionListener(launchGame);
        btnMedium.addActionListener(launchGame);
        btnHard.addActionListener(launchGame);
        btnStartCustom.addActionListener(launchGame);

        btnRestart.addActionListener(e -> startNewGame(gameInstance));

        btnMenu.addActionListener(e -> {
            frame.setContentPane(mainMenuPanel);
            frame.revalidate();
        });

        btnExitGame.addActionListener(e -> System.exit(0));

        frame.setContentPane(mainMenuPanel);
        frame.setVisible(true);
    }

    private static void startNewGame(Display display) {
        display.stop();

        Display newGame = new Display();
        JPanel gameContainer = new JPanel(new BorderLayout());
        gameContainer.add(newGame);

        frame.setContentPane(gameContainer);
        frame.revalidate();

        BufferedImage cursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blank = Toolkit.getDefaultToolkit().createCustomCursor(cursor, new Point(0, 0), "blank");
        frame.getContentPane().setCursor(blank);

        newGame.requestFocus();
        newGame.start();
    }

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