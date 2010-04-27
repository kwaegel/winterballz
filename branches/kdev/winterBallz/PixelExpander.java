package winterBallz;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Test class to try to multithread the rectangle expansions
 * 
 */
public class PixelExpander implements Runnable {

	int x, y;
	private static BufferedImage m_image;
	private static ConcurrentSkipListSet<Rectangle> m_rectList;

	public PixelExpander(int x, int y, BufferedImage image, ConcurrentSkipListSet<Rectangle> rectList) {
		m_image = image;
		m_rectList = rectList;
	}

	private boolean[] checkBorders(Rectangle r, BufferedImage image) {
		boolean[] bArray = new boolean[4];

		// check top

		int row = r.y - 1;

		if (row > 0) {
			for (int c = r.x; c <= r.x + r.width; c++) {
				if (isWhite(image.getRGB(c, row))) {
					bArray[0] = true;
					break;
				}
			}
		}

		// check right

		int col = r.x + r.width + 1;
		if (col < image.getWidth()) {
			for (row = r.y; row <= r.y + r.height; row++) {
				if (isWhite(image.getRGB(col, row))) {
					bArray[1] = true;
					break;
				}
			}
		}

		// check bottom

		row = r.y + r.height + 1;
		if (row < image.getHeight()) {
			for (int c = r.x; c <= r.x + r.width; c++) {
				if (isWhite(image.getRGB(c, row))) {
					bArray[2] = true;
					break;
				}
			}
		}
		// check left
		col = r.x - 1;
		if (col > 0) {
			for (row = r.y; row <= r.y + r.height; row++) {
				if (isWhite(image.getRGB(col, row))) {
					bArray[3] = true;
					break;
				}
			}
		}
		return bArray;
	}

	private Rectangle expandRectangle(int row, int col, BufferedImage image) {

		Rectangle r = new Rectangle(col, row, 1, 1);

		boolean[] bA = checkBorders(r, image);

		// if there is a white pixel on any side of the rectangle, grow the
		// rectangel in that direction
		while (bA[0] || bA[1] || bA[2] || bA[3]) {
			// grow up
			if (bA[0]) {
				r.y -= 1;
				r.height++;
			}

			// grow right
			if (bA[1]) {
				r.width++;
			}

			// grow down
			if (bA[2]) {
				r.height++;
			}

			// grow left
			if (bA[3]) {
				r.x -= 1;
				r.width++;
			}

			// check around the borders again
			bA = checkBorders(r, image);
		}

		return r;

	}

	@Override
	public void run() {
		Rectangle r = expandRectangle(x, y, m_image);

	}

	private static boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

}
