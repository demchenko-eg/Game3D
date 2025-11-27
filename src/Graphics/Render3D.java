package Graphics;

import java.util.Random;

import Index.Game;
import Input.Controller;

public class Render3D extends Render {

	public double[] zBuffer;
	private double renderDistance = 5000;
	private double forwardGlobal;

	public Render3D(int width, int height) {
		super(width, height);
		zBuffer = new double[width * height];
	}

	double time = 0;

	public void floor(Game game) {

		double floorPosition = 8;
		double ceilingPosition = 8;
		double forward = game.controls.z; // game.time % 100 / 20.0;
		forwardGlobal = forward;
		double right = game.controls.x;
		double up = game.controls.y;
		double walking = Math.sin(game.time / 6.0) * 0.7;

		double rotation = game.controls.rotation;// game.time / 100.0;
		double cosine = Math.cos(rotation);
		double sine = Math.sin(rotation);

		for (int y = 0; y < height; y++) {
			double ceiling = (y - height / 2.0) / height;

			double z = (floorPosition + up) / ceiling;
			if (Controller.walk) {
				z = (floorPosition + up + walking) / ceiling;
			}

			if (ceiling < 0) {
				z = (ceilingPosition - up) / -ceiling;
				if (Controller.walk) {
					z = (ceilingPosition - up - walking) / -ceiling;
				}
			}

			for (int x = 0; x < width; x++) {
				double xDepth = (x - height / 2.0) / height;
				xDepth *= z;
				double xx = xDepth * cosine + z * sine;
				double yy = z * cosine - xDepth * sine;
				int xPix = (int) (xx + right);
				int yPix = (int) (yy + forward);
				zBuffer[x + y * width] = z;
				pixels[x + y * width] = Texture.floor.pixels[(xPix & 7) + (yPix & 7) * 8];

				if (z > 500) {
					pixels[x + y * width] = 0;
				}

			}
		}
	}

	public void walls() {

		Random random = new Random(100);
		for (int i = 0; i < 20000; i++) {
			double xx = random.nextDouble();
			double yy = random.nextDouble();
			double zz = 2;

			int xPixel = (int) (xx / zz * height / 2 + width / 2);
			int yPixel = (int) (yy / zz * height / 2 + height / 2);
			if (xPixel >= 0 && yPixel >= 0 && xPixel < width && yPixel < height) {
				pixels[xPixel + yPixel * width] = 0xfffff;
			}
		}

		for (int i = 0; i < 20000; i++) {
			double xx = random.nextDouble() - 1;
			double yy = random.nextDouble();
			double zz = 2;

			int xPixel = (int) (xx / zz * height / 2 + width / 2);
			int yPixel = (int) (yy / zz * height / 2 + height / 2);
			if (xPixel >= 0 && yPixel >= 0 && xPixel < width && yPixel < height) {
				pixels[xPixel + yPixel * width] = 0xfffff;
			}
		}
		for (int i = 0; i < 20000; i++) {
			double xx = random.nextDouble() - 1;
			double yy = random.nextDouble() - 1;
			double zz = 2;

			int xPixel = (int) (xx / zz * height / 2 + width / 2);
			int yPixel = (int) (yy / zz * height / 2 + height / 2);
			if (xPixel >= 0 && yPixel >= 0 && xPixel < width && yPixel < height) {
				pixels[xPixel + yPixel * width] = 0xfffff;
			}
		}
		for (int i = 0; i < 20000; i++) {
			double xx = random.nextDouble();
			double yy = random.nextDouble() - 1;
			double zz = 2;

			int xPixel = (int) (xx / zz * height / 2 + width / 2);
			int yPixel = (int) (yy / zz * height / 2 + height / 2);
			if (xPixel >= 0 && yPixel >= 0 && xPixel < width && yPixel < height) {
				pixels[xPixel + yPixel * width] = 0xfffff;
			}
		}
	}

	public void renderDistanceLimiter() {
		for (int i = 0; i < width * height; i++) {
			int color = pixels[i];
			int brightness = (int) (renderDistance / (zBuffer[i]));

			if (brightness < 0) {
				brightness = 0;
			}

			if (brightness > 255) {

				brightness = 255;
			}

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
