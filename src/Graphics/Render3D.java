package Graphics;

import Index.Game;
import Index.Level;
import Input.Controller;

public class Render3D extends Render {

    public double[] zBuffer;
    private double renderDistance = 10000;

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
        double walking = 0;

        double rotation = game.controls.rotation;

        for (int x = 0; x < width; x++) {
            double cameraX = 2 * x / (double) width - 1;
            double rayDirX = Math.sin(rotation) + Math.cos(rotation) * cameraX;
            double rayDirZ = Math.cos(rotation) - Math.sin(rotation) * cameraX;

            for (int y = 0; y < height; y++) {
                double ceiling = (y - (height / 2.0 + game.controls.rotationPitch)) / height;

                double z = (floorPosition + up) / ceiling;
                if (Controller.walk) {
                    z = (floorPosition + up + walking) / ceiling;
                }

                if (ceiling < 0) {
                    z = (8.0 - up) / -ceiling;
                    if (Controller.walk) {
                        z = (8.0 - up - walking) / -ceiling;
                    }
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

            int maxDepth = 100;
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

            if (hit) {
                if (side == 0) perpWallDist = (mapX - xPos + (1 - stepX) / 2) / rayDirX;
                else           perpWallDist = (mapZ - zPos + (1 - stepZ) / 2) / rayDirZ;

                int lineHeight = (int) ((height * 16) / perpWallDist);

                int pitch = (int) game.controls.rotationPitch;
                int jumpOffset = (int) (game.controls.y * 20);

                int drawStart = -lineHeight / 2 + height / 2 + pitch + jumpOffset;
                if (drawStart < 0) drawStart = 0;

                int drawEnd = lineHeight / 2 + height / 2 + pitch + jumpOffset;
                if (drawEnd >= height) drawEnd = height - 1;

                double wallX;
                if (side == 0) {
                    wallX = zPos + perpWallDist * rayDirZ;
                } else {
                    wallX = xPos + perpWallDist * rayDirX;
                }
                wallX -= Math.floor(wallX);

                Render textureToUse = Texture.wall;
                if (wallType == 2) {
                    textureToUse = Texture.grate;
                }
                int texWidth = textureToUse.width;

                int texX = (int)(wallX * texWidth);
                if(side == 0 && rayDirX > 0) texX = texWidth - texX - 1;
                if(side == 1 && rayDirZ < 0) texX = texWidth - texX - 1;

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