package Index;

import java.awt.event.KeyEvent;
import Input.Controller;
import Input.InputHandler;

public class Game {
    public int time;
    public Controller controls;
    public Level level;
    public boolean lost = false;
    public InputHandler input;

    public static int totalScore = 0;

    public static int currentWidth = 20;
    public static int currentHeight = 20;
    public static int totalItemsOnLevel = 30;
    public static int requiredOnLevel = 20;
    public static int enemyCount = 1;
    public static double enemyHearingRadius = 4.0;
    public static double enemySpeed = 0.5;

    public int collectedOnLevel = 0;
    public boolean exitOpen = false;

    public Game(InputHandler input) {
        this.input = input;
        startLevel();
    }

    private void startLevel() {
        level = new Level(currentWidth, currentHeight, totalItemsOnLevel, enemyCount);
        controls = new Controller();
        controls.x = level.spawnX;
        controls.z = level.spawnZ;
        collectedOnLevel = 0;
        exitOpen = false;
    }

    public void tick(boolean[] key) {
        if (lost) return;

        time++;
        boolean forward = key[KeyEvent.VK_W];
        boolean back = key[KeyEvent.VK_S];
        boolean left = key[KeyEvent.VK_A];
        boolean right = key[KeyEvent.VK_D];
        boolean jump = key[KeyEvent.VK_SPACE];
        boolean crouch = key[KeyEvent.VK_CONTROL];
        boolean run = key[KeyEvent.VK_SHIFT];

        controls.tick(forward, back, left, right, jump, crouch, run, level);

        for (Enemy enemy : level.enemies) {
            enemy.tick(controls, level);
            double dist = Math.sqrt(Math.pow(controls.x - enemy.x, 2) + Math.pow(controls.z - enemy.z, 2));
            if (dist < 0.8) {
                lost = true;
                totalScore = 0;
            }
        }

        if (exitOpen) {
            double distToExit = Math.sqrt(Math.pow(controls.x - level.exitPixelX, 2) + Math.pow(controls.z - level.exitPixelZ, 2));
            if (distToExit < 15.0) {
                startLevel();
            }
        }

        if (input.interact) {
            input.interact = false;

            for (int i = 0; i < level.items.size(); i++) {
                Item item = level.items.get(i);

                double vx = item.x - controls.x;
                double vz = item.z - controls.z;
                double dist = Math.sqrt(vx*vx + vz*vz);

                if (dist < 20.0) {
                    double dx = Math.sin(controls.rotation);
                    double dz = Math.cos(controls.rotation);
                    double distanceToItem = vx * dx + vz * dz;
                    if (distanceToItem < 0) continue;

                    double distanceFromCrosshair = Math.abs(vx * dz - vz * dx);

                    if (distanceFromCrosshair < 0.4) {
                        level.items.remove(i);
                        totalScore++;
                        collectedOnLevel++;

                        if (collectedOnLevel >= requiredOnLevel && !exitOpen) {
                            level.openExit();
                            exitOpen = true;
                        }
                        break;
                    }
                }
            }
        }
    }
}