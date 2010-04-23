package winterBallz;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Botz {
	
	private static final int gridSize = 9;

	BufferedImage m_currentImage;
	Rectangle m_gameArea;
	DrawPanel m_drawPanel;
	boolean m_allowMove = true;
	Point m_previousMove;
	Point m_mouseLoc;
	Point rabbitLoc;
	Robot m_robot;
	int rabx = 0;
	int raby = 0;
	ArrayList<Rectangle> bells = new ArrayList<Rectangle>();

	public Botz(Rectangle bounds, DrawPanel panel) {
		m_drawPanel = panel;
		
		m_gameArea = new Rectangle(bounds);

		try {
			m_robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	public void update() {

		// capture the screen
		m_currentImage = m_robot.createScreenCapture(m_gameArea);
		
		// filter out extraneous image data
		filterImage(m_currentImage);
		
		// extract a list of features from the image
		List<SpatialRect> rectList = extractFeatures(m_currentImage);
		
		// draw the rectangles on the image and make a move
		drawRectangles(rectList);
		
		// calculate the best move for the rabbit to make
		Point move = getMove(rectList);
		
		// make the move
		if(m_allowMove)
			movePlayer(move);
		

		// put the diagnostic image in the panel and draw
		m_drawPanel.setImage(m_currentImage);
		m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(), m_currentImage.getHeight());

	}

	private Point getMove(List<SpatialRect> rectList) {
		
		Point move = findNearest(rectList, rabbitLoc);

		if (move.x + move.y != 0) {
			move.x += m_gameArea.x;
			move.y += m_gameArea.y;
			movePlayer(move);
		}
		
		return move;
	}

	private void drawRectangles(List<SpatialRect> recs) {

		
		Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();

		for (SpatialRect r : recs) {

			Cell c = new Cell(m_currentImage.getRGB(r.x, r.y, r.width, r.height, null, 0, r.width));

			// update rabbit location
			if (c.getCount() > 350) {
				rabbitLoc = new Point((int) r.getCenterX(), (int) r.getCenterY());

			}

			// switch color based on estimated object type
			if (rabbitLoc != null && r.contains(rabbitLoc)) {
				r.setType(SpatialRect.Type.RABBIT);
				g2d.setColor(Color.YELLOW);
				g2d.drawRect(r.x, r.y, r.width, r.height);

			} else if (r.width > 10) {
				g2d.setColor(Color.RED);
				g2d.drawRect(r.x, r.y, r.width, r.height);
			} else {
				g2d.setColor(Color.GREEN);
				g2d.drawRect(r.x, r.y, r.width, r.height);
			}
			g2d.setColor(Color.WHITE);
		}

		

	}

	private Point findNearest(List<SpatialRect> list, Point rabbitLocation) {
		Point p = new Point();

		if (list.size() > 2) {
			Collections.sort(list, new SpatialCompare());

			p.x = (int) list.get(1).getCenterX();
			p.y = (int) list.get(1).getCenterY();
		}

		return p;
	}

	private List<SpatialRect> extractFeatures(BufferedImage image) {

		List<SpatialRect> rectList = new ArrayList<SpatialRect>();

		for (int x = 0; x < image.getWidth() - 1; x += gridSize) {
			for (int y = 0; y < image.getHeight() - 1; y += gridSize) {

				if (isWhite(image.getRGB(x, y))) {
					// check if pixel is inside a previous rectangle
					boolean skip = false;
					for (Rectangle oldRect : rectList) {
						if (oldRect.contains(x, y)) {
							skip = true;
							break;
						}
					}

					if (!skip) {
						SpatialRect newRect = new SpatialRect(expandRect(y, x));
						// Rectangle newRect = expandRect(y, x);

						skip = false;

						for (Rectangle otherRect : rectList) {
							if (newRect.intersects(otherRect)) {
								skip = true;
							}
						}

						if (!skip && newRect.width > 10 && newRect.height > 10) {
							rectList.add(newRect);
						}
					}

				}
			}
		}

		return rectList;

	}

	private Rectangle expandRect(int row, int col) {

		Rectangle r = new Rectangle(col, row, 1, 1);

		boolean[] bA = checkBorders(r);

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
			bA = checkBorders(r);
		}

		return r;

	}

	private boolean[] checkBorders(Rectangle r) {
		boolean[] bArray = new boolean[4];

		// check top

		int row = r.y - 1;

		if (row > 0) {
			for (int c = r.x; c <= r.x + r.width; c++) {
				if (isWhite(m_currentImage.getRGB(c, row))) {
					bArray[0] = true;
					break;
				}
			}
		}

		// check right

		int col = r.x + r.width + 1;
		if (col < m_currentImage.getWidth()) {
			for (row = r.y; row <= r.y + r.height; row++) {
				if (isWhite(m_currentImage.getRGB(col, row))) {
					bArray[1] = true;
					break;
				}
			}
		}

		// check bottom

		row = r.y + r.height + 1;
		if (row < m_currentImage.getHeight()) {
			for (int c = r.x; c <= r.x + r.width; c++) {
				if (isWhite(m_currentImage.getRGB(c, row))) {
					bArray[2] = true;
					break;
				}
			}
		}
		// check left
		col = r.x - 1;
		if (col > 0) {
			for (row = r.y; row <= r.y + r.height; row++) {
				if (isWhite(m_currentImage.getRGB(col, row))) {
					bArray[3] = true;
					break;
				}
			}
		}
		return bArray;
	}

	private int[] rgbToRGBArray(int value) {
		int[] rgb = new int[3];
		value = value >> 8;
		rgb[0] = value & 0xFF;
		value = value >> 8;
		rgb[1] = value & 0xFF;
		value = value >> 8;
		rgb[2] = value & 0xFF;
		return rgb;
	}

	private void filterImage(BufferedImage image) {
		
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				int rgb = image.getRGB(x, y);
				int[] RGB = rgbToRGBArray(rgb);
				if (image.getHeight() - y < 30 || RGB[0] < 200 || RGB[1] < 200 || RGB[2] < 200) {
					image.setRGB(x, y, 0);
				} else {
					image.setRGB(x, y, Color.WHITE.getRGB());
				}
			}
		}
		
	}

	@SuppressWarnings("unused")
	private boolean discardPixel(int c) {
		return (c != Color.WHITE.getRGB());
	}

	private boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

	private Boolean movePlayer(Point move) {

		if (move != null) {
			//System.out.println(move);
			
			// only move the mouse horizontally
			Point mousePos = MouseInfo.getPointerInfo().getLocation();
			m_robot.mouseMove(move.x, mousePos.y);

		} else {
			//System.out.println("NO MOVE");
		}

		return false;
	}

}
