package winterBallz;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;



public class FeatureExtractor implements Callable<Pair<List<Rectangle>, Rectangle>> {
	
	Rectangle m_area;
	BufferedImage m_image;
	
	public FeatureExtractor(Rectangle searchArea, BufferedImage image) {
		
		m_area = searchArea;
		m_image = image;
	}
	
	public Pair<List<Rectangle>, Rectangle> call() throws Exception {
		
		List<Rectangle> features = new ArrayList<Rectangle>();
		Rectangle rabbit = null;
		
		//check each pixel in the search area
		for (int x = 0; x < m_area.x + m_area.width; x += Botz.horzRes) {
			nextPixel:
				for (int y = 0; y < m_area.y + m_area.height; y += Botz.verticalRes) {

					if (isWhite(m_image.getRGB(x, y))) {

						// skip pixel if it is inside a previous rectangle
						for (Rectangle oldRect : features) {
							if (oldRect.contains(x, y)) {
								continue nextPixel;
							}
						}

						//SpatialRectangle newRect = new SpatialRectangle(expandRect(y, x));
						Rectangle newRectangle = expandRectangle(y, x, m_image);

						// only add large rectangles
						if (newRectangle.width > 10 && newRectangle.height > 10)
						{
							Cell c = new Cell(m_image.getRGB(newRectangle.x, newRectangle.y, newRectangle.width, newRectangle.height, null, 0, newRectangle.width));

							// update rabbit location
							if (c.getCount() > 350) {
								rabbit = newRectangle;
								continue nextPixel;
							}

							// skip overlapping rectangles
							for (Rectangle otherRect : features) {
								if (newRectangle.intersects(otherRect)) {
									continue nextPixel;
								}
							}

							// add this feature to the list of rectangles
							features.add(newRectangle);
						}
					}
				}
		}
		
		return new Pair<List<Rectangle>, Rectangle>(features, rabbit);

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

		// if there is a white pixel on any side of the rectangle, grow the rectangel in that direction
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
			bA = checkBorders(r, m_image);
		}

		return r;

	}
	
	public static boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

}
