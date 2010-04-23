package winterBallz;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

public class Botz {
	
	private static final int verticalRes = 10;
	private static final int horzRes = 7;

	private BufferedImage m_currentImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;
	private boolean m_allowMove = true;
	private Rectangle m_rabbit;
	private Robot m_robot;
	
	private ExecutorService pixelExpanders;

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
		List<Rectangle> rectList = extractFeatures(m_currentImage);
		
		// draw the rectangles on the image and make a move
		drawFeatures(rectList);
		
		// calculate the best move for the rabbit to make
		Point move = getMove(rectList);
		
		// make the move
		if(m_allowMove && move != null)
			movePlayer(move);
		

		// put the filtered image in the panel and draw
		m_drawPanel.setImage(m_currentImage);
		m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(), m_currentImage.getHeight());

	}

	private Point getMove(List<Rectangle> rectList) {
		
		// we can't get a move if we don't know where the rabbit is...
		if (m_rabbit == null)
			return null;
		
		// get current screen to draw prediction lines on
		Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();
		
		Point closestBell = this.findNearest(rectList, m_rabbit);
		
		Point move = null;
		
		if (closestBell != null)
			move = new Point(closestBell.x + m_gameArea.x, closestBell.y + m_gameArea.y);
		
		if (move != null)
		{
			g2d.setColor(Color.magenta);
			g2d.drawLine(closestBell.x, closestBell.y, m_rabbit.x, m_rabbit.y);
		}
		
		
		
		TreeMap<Integer, Rectangle> sortedFeatures = getFeaturesByDistance(rectList, m_rabbit);
		
		Set<Integer> keySet = sortedFeatures.keySet();
		
		
		
		
		return move;
	}
	
	private TreeMap<Integer, Rectangle> getFeaturesByDistance (List<Rectangle> rectList, Rectangle feature)
	{
		TreeMap<Integer, Rectangle> tm = new TreeMap<Integer, Rectangle>();
		
		int distance;
		for (Rectangle r : rectList)
		{
			distance = (int) Point2D.distance(r.getCenterX(), r.getCenterY(), feature.getCenterX(), feature.getCenterY());
			
			tm.put(distance, r);
			//tm.put(-r.y, r);
		}
		
		return tm;
	}
	
	public static int getSquaredDistance (Point p1, Point p2)
	{
		return (p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y);
	}

	private void drawFeatures(List<Rectangle> recs) {

		
		Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();

		for (Rectangle r : recs) {
			if (r.width > 10) {
				g2d.setColor(Color.RED);
				g2d.drawRect(r.x, r.y, r.width, r.height);
			} else {
				g2d.setColor(Color.GREEN);
				g2d.drawRect(r.x, r.y, r.width, r.height);
			}
		}

		if (m_rabbit != null){
			g2d.setColor(Color.yellow);
			g2d.drawRect(m_rabbit.x, m_rabbit.y, m_rabbit.width, m_rabbit.height);
		}

	}

	private Point findNearest(List<Rectangle> list, Rectangle rabbit) {

		Rectangle closestFeature = null;
		if (list.size() > 2) {
			
			int closest = Integer.MAX_VALUE;
			
			for (Rectangle r : list) {
				int distance = (int) (rabbit.getCenterY() - r.getCenterY());// (int) Point2D.distance(r.getCenterX(), r.getCenterY(), rabbit.getCenterX(), rabbit.getCenterY());
				
				if (distance < closest){
					closest = distance;
					closestFeature = r;
				}
			}
		}
		
		if (closestFeature == null)
			return null;
		return closestFeature.getLocation();
	}

	private List<Rectangle> extractFeatures(BufferedImage image) {
		
		// reset rabbit each frame
		m_rabbit = null;
		
		//TODO divide the image into four segments and search each quadrant with a separate thread
		//pixelExpanders = Executors.newFixedThreadPool(4);
		
		List<Rectangle> features = new ArrayList<Rectangle>();

		for (int x = 0; x < image.getWidth() - 1; x += horzRes) {
			nextPixel:
			for (int y = 0; y < image.getHeight() - 1; y += verticalRes) {

				if (isWhite(image.getRGB(x, y))) {
					
					// skip pixel if it is inside a previous rectangle
					for (Rectangle oldRect : features) {
						if (oldRect.contains(x, y)) {
							continue nextPixel;
						}
					}

					//SpatialRectangle newRect = new SpatialRectangle(expandRect(y, x));
					Rectangle newRectangle = expandRectangle(y, x);

					// only add large rectangles
					if (newRectangle.width > 10 && newRectangle.height > 10)
					{
						Cell c = new Cell(m_currentImage.getRGB(newRectangle.x, newRectangle.y, newRectangle.width, newRectangle.height, null, 0, newRectangle.width));

						// update rabbit location
						if (c.getCount() > 350) {
							m_rabbit = newRectangle;
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

		return features;

	}

	private Rectangle expandRectangle(int row, int col) {

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
