package Index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Level {
    public int width;
    public int height;
    public int[] tiles;
    private int[][] map;

    public double spawnX;
    public double spawnZ;

    public Level() {
        int mapWidth = 6;
        int mapHeight = 6;

        map = new int[mapWidth][mapHeight];

        for(int x = 0; x < mapWidth; x++) {
            for(int y = 0; y < mapHeight; y++) {
                map[x][y] = 1;
            }
        }

        generateMaze(1, 1);

        Random random = new Random();

        for (int x = 1; x < mapWidth - 1; x++) {
            for (int y = 1; y < mapHeight - 1; y++) {
                if (map[x][y] == 1) {
                    boolean horizontal = map[x-1][y] == 0 && map[x+1][y] == 0;
                    boolean vertical = map[x][y-1] == 0 && map[x][y+1] == 0;

                    if ((horizontal || vertical) && random.nextInt(100) < 20) {
                        map[x][y] = 0;
                    }
                }
            }
        }

        int side = random.nextInt(4);
        int exitPos = 1 + random.nextInt(mapWidth - 3);

        if (side == 0) {
            map[exitPos][0] = 2;
            map[exitPos + 1][0] = 2;
            map[exitPos][1] = 0;
            map[exitPos + 1][1] = 0;
        } else if (side == 1) {
            map[mapWidth - 1][exitPos] = 2;
            map[mapWidth - 1][exitPos + 1] = 2;
            map[mapWidth - 2][exitPos] = 0;
            map[mapWidth - 2][exitPos + 1] = 0;
        } else if (side == 2) {
            map[exitPos][mapHeight - 1] = 2;
            map[exitPos + 1][mapHeight - 1] = 2;
            map[exitPos][mapHeight - 2] = 0;
            map[exitPos + 1][mapHeight - 2] = 0;
        } else {
            map[0][exitPos] = 2;
            map[0][exitPos + 1] = 2;
            map[1][exitPos] = 0;
            map[1][exitPos + 1] = 0;
        }

        int scale = 10;

        while (true) {
            int rx = random.nextInt(mapWidth);
            int ry = random.nextInt(mapHeight);

            if (map[rx][ry] == 0) {
                spawnX = rx * scale + scale / 2.0;
                spawnZ = ry * scale + scale / 2.0;
                break;
            }
        }

        this.width = mapWidth * scale;
        this.height = mapHeight * scale;
        tiles = new int[this.width * this.height];

        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                int type = map[x][y];

                int pixelX = x * scale;
                int pixelY = y * scale;

                if (type == 1 || type == 2) {
                    tiles[pixelX + pixelY * this.width] = type;

                    if (x + 1 < mapWidth && (map[x + 1][y] == 1 || map[x + 1][y] == 2)) {
                        int fillType = (type == 2 && map[x + 1][y] == 2) ? 2 : 1;
                        for (int i = 1; i < scale; i++) {
                            tiles[(pixelX + i) + pixelY * this.width] = fillType;
                        }
                    }

                    if (y + 1 < mapHeight && (map[x][y + 1] == 1 || map[x][y + 1] == 2)) {
                        int fillType = (type == 2 && map[x][y + 1] == 2) ? 2 : 1;
                        for (int i = 1; i < scale; i++) {
                            tiles[pixelX + (pixelY + i) * this.width] = fillType;
                        }
                    }
                }
            }
        }
    }

    private void generateMaze(int x, int y) {
        map[x][y] = 0;

        ArrayList<Integer> dirs = new ArrayList<>();
        dirs.add(0);
        dirs.add(1);
        dirs.add(2);
        dirs.add(3);
        Collections.shuffle(dirs, new Random());

        for (int dir : dirs) {
            int dx = 0;
            int dy = 0;

            if (dir == 0) dy = -2;
            if (dir == 1) dy = 2;
            if (dir == 2) dx = -2;
            if (dir == 3) dx = 2;

            int nx = x + dx;
            int ny = y + dy;

            if (nx > 0 && nx < map.length - 1 && ny > 0 && ny < map[0].length - 1 && map[nx][ny] == 1) {
                map[x + dx / 2][y + dy / 2] = 0;
                generateMaze(nx, ny);
            }
        }
    }

    public int getTile(int x, int z) {
        if (x < 0 || z < 0 || x >= width || z >= height) return 0;
        return tiles[x + z * width];
    }
}