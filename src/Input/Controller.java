package Input;

import Index.Level;

public class Controller {

    public double x, y, z, rotation, xa, za, rotationa;
    public double rotationPitch = 0;

    public double ya = 0;
    public boolean isJumping = false;

    public static boolean turnLeft = false;
    public static boolean turnRight = false;
    public static boolean walk = false;

    public void tick(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean crouch, boolean run, Level level) {
        double rotationSpeed = 0.0076;
        double walkSpeed = 0.9;
        double groundLevel = 0;

        if (crouch) {
            walkSpeed = 0.45;
            run = false;
            groundLevel = -0.5;
        } else if (run) {
            walkSpeed = 1.4;
        }

        double xMove = 0;
        double zMove = 0;

        if (forward) {
            zMove++;
            walk = true;
        }
        if (back) {
            zMove--;
            walk = true;
        }
        if (left) {
            xMove--;
            walk = true;
        }
        if (right) {
            xMove++;
            walk = true;
        }
        if (turnLeft) {
            rotationa -= rotationSpeed;
        }
        if (turnRight) {
            rotationa += rotationSpeed;
        }

        if (jump && !isJumping && !crouch) {
            ya = 1.2;
            isJumping = true;
        }

        ya -= 0.1;
        y += ya;

        if (y <= groundLevel) {
            y = groundLevel;
            ya = 0;
            isJumping = false;
        }

        if (!forward && !back && !left && !right && !turnLeft && !turnRight && !crouch) {
            walk = false;
        }

        xa += (xMove * Math.cos(rotation) + zMove * Math.sin(rotation)) * walkSpeed;
        za += (zMove * Math.cos(rotation) - xMove * Math.sin(rotation)) * walkSpeed;

        if (isFree(x + xa, z, level)) {
            x += xa;
        } else {
            xa = 0;
        }

        if (isFree(x, z + za, level)) {
            z += za;
        } else {
            za = 0;
        }

        xa *= 0.1;
        za *= 0.1;

        rotation += rotationa;
        rotationa *= 0.8;
    }

    private boolean isFree(double xx, double zz, Level level) {
        double r = 0.2;

        if (level.getTile((int)(xx - r), (int)(zz - r)) > 0) return false;
        if (level.getTile((int)(xx - r), (int)(zz + r)) > 0) return false;
        if (level.getTile((int)(xx + r), (int)(zz - r)) > 0) return false;
        if (level.getTile((int)(xx + r), (int)(zz + r)) > 0) return false;

        return true;
    }
}