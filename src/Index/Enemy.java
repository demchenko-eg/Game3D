package Index;

import Input.Controller;

public class Enemy {
    public double x, z;

    private double targetX;
    private double targetZ;
    private boolean hasTarget = false;

    public Enemy(double x, double z) {
        this.x = x;
        this.z = z;
        this.targetX = x;
        this.targetZ = z;
    }

    public void tick(Controller player, Level level) {
        double distToPlayer = Math.sqrt(Math.pow(player.x - x, 2) + Math.pow(player.z - z, 2));

        double hearingLimit = Game.enemyHearingRadius * 10.0;

        if (player.isMoving && !player.isCrouching && distToPlayer < hearingLimit) {
            targetX = player.x;
            targetZ = player.z;
            hasTarget = true;
        }

        if (hasTarget) {
            double dx = targetX - x;
            double dz = targetZ - z;
            double distToTarget = Math.sqrt(dx*dx + dz*dz);

            if (distToTarget > 0.5) {
                dx /= distToTarget;
                dz /= distToTarget;

                double moveX = dx * Game.enemySpeed;
                double moveZ = dz * Game.enemySpeed;

                if (isFree(x + moveX, z, level)) {
                    x += moveX;
                }
                if (isFree(x, z + moveZ, level)) {
                    z += moveZ;
                }
            } else {
                hasTarget = false;
            }
        }
    }

    private boolean isFree(double xx, double zz, Level level) {
        double r = 0.35;

        if (level.getTile((int)(xx - r), (int)(zz - r)) > 0) return false;
        if (level.getTile((int)(xx - r), (int)(zz + r)) > 0) return false;
        if (level.getTile((int)(xx + r), (int)(zz - r)) > 0) return false;
        if (level.getTile((int)(xx + r), (int)(zz + r)) > 0) return false;

        return true;
    }
}