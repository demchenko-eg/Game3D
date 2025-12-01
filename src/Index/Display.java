package Index;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JFrame;
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

    public Display() {
        try {
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }

        screen = new Screen(WIDTH, HEIGHT);
        game = new Game();
        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        input = new InputHandler();
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

            if (hasFocus()) {
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;

                int mouseX = InputHandler.mouseX;
                int mouseY = InputHandler.mouseY;

                if (mouseX > centerX) {
                    Controller.turnRight = true;
                } else if (mouseX < centerX) {
                    Controller.turnLeft = true;
                } else {
                    Controller.turnRight = false;
                    Controller.turnLeft = false;
                }

                int dy = mouseY - centerY;
                game.controls.rotationPitch -= dy * 0.5;

                if (game.controls.rotationPitch > 200) game.controls.rotationPitch = 200;
                if (game.controls.rotationPitch < -200) game.controls.rotationPitch = -200;

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

        if (input.key[KeyEvent.VK_F11]) {
            if (!f11Pressed) {
                toggleFullScreen();
                f11Pressed = true;
            }
        } else {
            f11Pressed = false;
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

    private void render() {
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

        g.setColor(Color.RED);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int size = 10;
        g.drawLine(cx - size, cy, cx + size, cy);
        g.drawLine(cx, cy - size, cx, cy + size);

        g.setFont(new Font("Verdana", 2, 35));
        g.setColor(Color.YELLOW);
        g.drawString(fps + " FPS", 15, 40);
        g.dispose();
        bs.show();
    }

    public static void main(String[] args) {
        BufferedImage cursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blank = Toolkit.getDefaultToolkit().createCustomCursor(cursor, new Point(0, 0), "blank");

        Display game = new Display();

        frame = new JFrame();
        frame.add(game);
        frame.pack();
        frame.getContentPane().setCursor(blank);
        frame.setTitle(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        game.start();
    }
}