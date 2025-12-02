package Graphics;

import Index.Game;

public class Screen extends Render {

    private Render3D render;

    public Screen(int width, int height) {
        super(width, height);
        render = new Render3D(width, height);
    }

    public void render(Game game) {
        for (int i = 0; i < width * height; i++) {
            pixels[i] = 0;
        }

        render.floor(game);
        render.walls(game, game.level);
        render.renderDistanceLimiter();

        draw(render, 0, 0);
    }
}