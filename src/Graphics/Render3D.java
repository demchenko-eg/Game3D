package Graphics;

import Index.Game;
import Index.Level;
import Index.Enemy;
import Index.Item;
import Input.Controller;

public class Render3D extends Render {

    public double[] zBuffer;
    private double renderDistance = 15000;

    public Render3D(int width, int height) {
        super(width, height);
        zBuffer = new double[width * height];
    }

    public void floor(Game game) {
        int floorSize = 64;
        int floorMask = 63;

        if (Texture.floor != null) {
            floorSize = Texture.floor.width;
            floorMask = floorSize - 1;
        }

        double floorPosition = 8;
        double forward = game.controls.z;
        double right = game.controls.x;
        double up = game.controls.y;

        double rotation = game.controls.rotation;

        for (int x = 0; x < width; x++) {
            double cameraX = 2 * x / (double) width - 1;
            double rayDirX = Math.sin(rotation) + Math.cos(rotation) * cameraX;
            double rayDirZ = Math.cos(rotation) - Math.sin(rotation) * cameraX;

            for (int y = 0; y < height; y++) {
                double ceiling = (y - (height / 2.0 + game.controls.rotationPitch)) / height;

                double z = (floorPosition + up) / ceiling;
                if (ceiling < 0) {
                    z = (8.0 - up) / -ceiling;
                }

                double xx = right + rayDirX * z;
                double yy = forward + rayDirZ * z;

                int xPix = (int) (xx);
                int yPix = (int) (yy);

                zBuffer[x + y * width] = z;

                pixels[x + y * width] = Texture.floor.pixels[(xPix & floorMask) + (yPix & floorMask) * floorSize];

                if (z > renderDistance) {
                    pixels[x + y * width] = 0;
                }
            }
        }
    }

    public void walls(Game game, Level level) {
        double xPos = game.controls.x;
        double zPos = game.controls.z;
        double rot = game.controls.rotation;

        for (int x = 0; x < width; x++) {
            double fov = 1.0;
            double cameraX = 2 * x / (double) width - 1;

            double rayDirX = Math.sin(rot) + Math.cos(rot) * cameraX * fov;
            double rayDirZ = Math.cos(rot) - Math.sin(rot) * cameraX * fov;

            int mapX = (int) xPos;
            int mapZ = (int) zPos;

            double sideDistX;
            double sideDistZ;

            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistZ = Math.abs(1 / rayDirZ);
            double perpWallDist;

            int stepX;
            int stepZ;

            boolean hit = false;
            int side = 0;
            int wallType = 0;

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (xPos - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - xPos) * deltaDistX;
            }
            if (rayDirZ < 0) {
                stepZ = -1;
                sideDistZ = (zPos - mapZ) * deltaDistZ;
            } else {
                stepZ = 1;
                sideDistZ = (mapZ + 1.0 - zPos) * deltaDistZ;
            }

            int maxDepth = 300;
            int depthCount = 0;

            while (!hit && depthCount < maxDepth) {
                if (sideDistX < sideDistZ) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistZ += deltaDistZ;
                    mapZ += stepZ;
                    side = 1;
                }
                wallType = level.getTile(mapX, mapZ);
                if (wallType > 0) hit = true;
                depthCount++;
            }

            if (!hit) continue;


            if (side == 0) perpWallDist = (mapX - xPos + (1 - stepX) / 2) / rayDirX;
            else           perpWallDist = (mapZ - zPos + (1 - stepZ) / 2) / rayDirZ;

            if (perpWallDist > 200) continue;

            int lineHeight = (int) ((height * 16) / perpWallDist);
            int pitch = (int) game.controls.rotationPitch;
            int jumpOffset = (int) (game.controls.y * 20);

            int drawStart = -lineHeight / 2 + height / 2 + pitch + jumpOffset;
            if (drawStart < 0) drawStart = 0;
            int drawEnd = lineHeight / 2 + height / 2 + pitch + jumpOffset;
            if (drawEnd >= height) drawEnd = height - 1;

            double wallX;
            if (side == 0) wallX = zPos + perpWallDist * rayDirZ;
            else           wallX = xPos + perpWallDist * rayDirX;
            wallX -= Math.floor(wallX);


            Render textureToUse = Texture.wall;
            if (wallType == 2) textureToUse = Texture.grate;

            int texX = (int)(wallX * textureToUse.width);
            if(side == 0 && rayDirX > 0) texX = textureToUse.width - texX - 1;
            if(side == 1 && rayDirZ < 0) texX = textureToUse.width - texX - 1;

            int texWidth = textureToUse.width;
            texX = texX & (texWidth - 1);

            for (int y = drawStart; y < drawEnd; y++) {
                if (y >= 0 && y < height) {
                    long d = (long)(y - pitch - jumpOffset) * 256 - height * 128 + lineHeight * 128;
                    int texY = (int)((d * texWidth) / lineHeight) / 256;

                    if (texY < 0) texY = 0;
                    if (texY >= texWidth) texY = texWidth - 1;

                    int color = textureToUse.pixels[texX + texY * texWidth];
                    pixels[x + y * width] = color;
                    zBuffer[x + y * width] = perpWallDist;
                }
            }

            double closestEntityDist = perpWallDist;

            double enemyDist = Double.MAX_VALUE;
            double enemyHitX = 0;

            int enemyFace = 0;
            boolean hitEnemy = false;

            double enemyRadius = 0.7;

            for (Enemy e : level.enemies) {
                double minX = e.x - enemyRadius; double maxX = e.x + enemyRadius;
                double minZ = e.z - enemyRadius; double maxZ = e.z + enemyRadius;


                double t1 = (minX - xPos) / rayDirX;
                double t2 = (maxX - xPos) / rayDirX;
                double t3 = (minZ - zPos) / rayDirZ;
                double t4 = (maxZ - zPos) / rayDirZ;

                double tmin = Math.max(Math.min(t1, t2), Math.min(t3, t4));
                double tmax = Math.min(Math.max(t1, t2), Math.max(t3, t4));

                if (tmax >= tmin && tmin > 0) {
                    if (tmin < enemyDist) {
                        enemyDist = tmin;
                        hitEnemy = true;

                        double intersectX = xPos + rayDirX * tmin;
                        double intersectZ = zPos + rayDirZ * tmin;

                        if (Math.abs(intersectX - minX) < 0.01) {
                            enemyHitX = intersectZ - minZ;
                            enemyFace = 2;
                        }
                        else if (Math.abs(intersectX - maxX) < 0.01) {
                            enemyHitX = intersectZ - minZ;
                            enemyFace = 3;
                        }
                        else if (Math.abs(intersectZ - minZ) < 0.01) {
                            enemyHitX = intersectX - minX;
                            enemyFace = 0;
                        }
                        else {
                            enemyHitX = intersectX - minX;
                            enemyFace = 1;
                        }
                    }
                }
            }

            if (hitEnemy && enemyDist < closestEntityDist) {
                closestEntityDist = enemyDist;

                int standardWallHeight = (int) ((height * 16) / enemyDist);
                int cubeHeight = Math.max(1, standardWallHeight / 4);

                int screenCenterY = height / 2 + pitch + jumpOffset;

                int eStart = screenCenterY - (cubeHeight / 2);
                int eEnd = screenCenterY + (cubeHeight / 2);

                int drawStartClamped = Math.max(0, eStart);
                int drawEndClamped = Math.min(height - 1, eEnd);

                Render eTex = Texture.enemyFront;
                if (enemyFace == 1) eTex = Texture.enemyBack;
                if (enemyFace == 2) eTex = Texture.enemyLeft;
                if (enemyFace == 3) eTex = Texture.enemyRight;

                int eTexX = (int)((enemyHitX / (2.0 * enemyRadius)) * eTex.width) & (eTex.width - 1);

                for (int y = drawStartClamped; y < drawEndClamped; y++) {
                    int texY = ((y - eStart) * eTex.height) / cubeHeight;
                    if (texY < 0) texY = 0; if (texY >= eTex.height) texY = eTex.height - 1;

                    int col = eTex.pixels[eTexX + texY * eTex.width];

                    if ((col & 0xFF000000) != 0) {
                        pixels[x + y * width] = col;
                        zBuffer[x + y * width] = enemyDist;
                    }
                }
            }

            double itemDist = Double.MAX_VALUE;
            double itemHitX = 0;
            int itemSide = 0;
            boolean hitItem = false;

            double itemRadius = 0.3;

            for (Item item : level.items) {
                double minX = item.x - itemRadius; double maxX = item.x + itemRadius;
                double minZ = item.z - itemRadius; double maxZ = item.z + itemRadius;

                double t1 = (minX - xPos) / rayDirX;
                double t2 = (maxX - xPos) / rayDirX;
                double t3 = (minZ - zPos) / rayDirZ;
                double t4 = (maxZ - zPos) / rayDirZ;

                double tmin = Math.max(Math.min(t1, t2), Math.min(t3, t4));
                double tmax = Math.min(Math.max(t1, t2), Math.max(t3, t4));

                if (tmax >= tmin && tmin > 0) {
                    if (tmin < itemDist) {
                        itemDist = tmin;
                        hitItem = true;

                        double intersectX = xPos + rayDirX * tmin;
                        double intersectZ = zPos + rayDirZ * tmin;

                        if (Math.abs(intersectX - minX) < 0.01) { itemHitX = intersectZ - minZ; itemSide = 0; }
                        else if (Math.abs(intersectX - maxX) < 0.01) { itemHitX = intersectZ - minZ; itemSide = 0; }
                        else if (Math.abs(intersectZ - minZ) < 0.01) { itemHitX = intersectX - minX; itemSide = 1; }
                        else { itemHitX = intersectX - minX; itemSide = 1; }
                    }
                }
            }

            if (hitItem && itemDist < closestEntityDist) {
                int iHeight = (int) ((height * 16) / itemDist);
                int iEnd = iHeight / 2 + height / 2 + pitch + jumpOffset;

                int cubeHeight = Math.max(1, iHeight / 32);
                int iStart = iEnd - cubeHeight;

                if (iStart < 0) iStart = 0;
                if (iEnd >= height) iEnd = height - 1;

                Render iTex = Texture.yellowSquare;

                int iTexX = (int)((itemHitX / (2.0 * itemRadius)) * iTex.width) & (iTex.width - 1);

                for (int y = iStart; y < iEnd; y++) {
                    if (y >= 0 && y < height) {
                        long d = (long)(y - pitch - jumpOffset) * 256 - height * 128 + iHeight * 128;
                        int texY = (int)((d * iTex.width) / iHeight) / 256;
                        if (texY < 0) texY = 0; if (texY >= 64) texY = 63;

                        int col = iTex.pixels[iTexX + texY * 64];

                        if (y <= iStart + 1) {
                            col = 0xFFE6B800;
                        } else if (itemSide == 1) {
                            col = (col & 0xFEFEFE) >> 1;
                        }

                        pixels[x + y * width] = col;
                        zBuffer[x + y * width] = itemDist;
                    }
                }
            }
        }
    }

    public void renderDistanceLimiter() {
        for (int i = 0; i < width * height; i++) {
            int color = pixels[i];
            int brightness = (int) (renderDistance / (zBuffer[i]));

            if (brightness < 0) brightness = 0;
            if (brightness > 255) brightness = 255;

            int r = (color >> 16) & 0xff;
            int g = (color >> 8) & 0xff;
            int b = (color) & 0xff;

            r = r * brightness / 255;
            g = g * brightness / 255;
            b = b * brightness / 255;

            pixels[i] = r << 16 | g << 8 | b;
        }
    }
}