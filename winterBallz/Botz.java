package winterBallz;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Botz {

	public static final int verticalRes = 10;
	public static final int horzRes = 7;
	public static final int maxLookahead = 3;
	public static final int birdSpeedLimit = 3;
	
	private boolean m_allowMove = true;

	private BufferedImage m_currentImage;
	private BufferedImage m_scaledImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;
	private Robot m_robot;

	// special rectangles
	private Rectangle m_rabbitRectangle;
	//private Rectangle m_birdRectangle;
	
	// special feature locations
	private Point m_rabbitLoc;
	private Point m_birdLoc;
	
	
	private List<Point> m_featureLocations;
	private Point m_currentTarget;
	private Rectangle m_targetZone = new Rectangle (0, 0, 0, 240);

	public Botz(Rectangle bounds, DrawPanel panel) {
		m_drawPanel = panel;
		m_gameArea = new Rectangle(bounds);
		m_featureLocations = new ArrayList<Point>();

		try {
			m_robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	// divide up the image vertically. Not used right now.
	@SuppressWarnings("unused")
	private List<Rectangle> divideImage(BufferedImage image, int sections) {

		List<Rectangle> list = new ArrayList<Rectangle>();

		int step = (image.getWidth() / sections) - 1;
		boolean collision = false;

		int x, y, height, width;

		x = y = 0;
		int col = step;
		while (!collision && col < image.getWidth()) {
			for (int row = 0; row < image.getHeight(); row++) {
				// make sure the column is empty of white pixels
				if (isWhite(image.getRGB(col, row))) {
					collision = true;
					break;
				}
			}

			if (!collision) {
				height = image.getHeight();
				width = col - x;
				list.add(new Rectangle(x, y, height, width));
				col += step;
			} else {
				col++;
				collision = false;
			}
		}

		return list;
	}

	/**
	 * Draw these features: -> a red box around every game feature -> a yellow
	 * box around the rabbit -> a magenta line from the rabbit to the nearest
	 * game feature -> a cyan circle on the current target game feature
	 * 
	 * @param recs
	 */
	private void drawFeatures(List<Rectangle> recs) {

		Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();
		
		// draw circles about all the targets
		for (Point p : m_featureLocations)
		{
			if (p == m_rabbitLoc)
				g2d.setColor(Color.yellow);
			else if (p == m_birdLoc)
				g2d.setColor(Color.cyan);
			else
				g2d.setColor(Color.magenta);
			g2d.fillOval(p.x - 8, p.y - 8, 16, 16);
		}
		
		// draw the target zone
		g2d.setColor(Color.white);
		g2d.draw(m_targetZone);
		
		// draw a line to the target
		if (m_currentTarget != null && m_rabbitLoc != null)
		{
			g2d.setColor(Color.green);
			g2d.drawLine(m_currentTarget.x, m_currentTarget.y, m_rabbitLoc.x, m_rabbitLoc.y);
		}
		

		// put the filtered image in the panel and draw it

		// scale the image to fit the draw panel size
		Dimension drawSize = m_drawPanel.getSize();
		int width = m_currentImage.getWidth();
		int height = m_currentImage.getHeight();
		double aspectRatio = (double) width / (double) height;
		int newWidth = drawSize.width;
		int newHeight = (int) (newWidth / aspectRatio);

		// Create new (blank) image of required (scaled) size
		m_scaledImage = new BufferedImage(drawSize.width, drawSize.height, BufferedImage.TYPE_INT_RGB);

		// Paint scaled version of image to new image
		Graphics2D graphics2D = m_scaledImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(m_currentImage, 0, 0, newWidth, newHeight, null);

		// clean up
		graphics2D.dispose();

		m_drawPanel.setImage(m_scaledImage);

		// draw the screen immediately
		m_drawPanel.paintImmediately(0, 0, m_scaledImage.getWidth(), m_scaledImage.getHeight());

	}

	private List<Rectangle> extractFeatures(BufferedImage m_image, Rectangle m_area) {

		// reset rabbit each frame
		m_rabbitRectangle = null;
		
		List<Rectangle> features = new ArrayList<Rectangle>();

		// check each pixel in the search area
		for (int x = 0; x < m_area.x + m_area.width; x += Botz.horzRes) {
			nextPixel: for (int y = 0; y < m_area.y + m_area.height; y += Botz.verticalRes) {

				if (isWhite(m_image.getRGB(x, y))) {

					// skip pixel if it is inside a previous rectangle
					for (Rectangle oldRect : features) {
						if (oldRect.contains(x, y)) {
							continue nextPixel;
						}
					}

					// SpatialRectangle newRect = new
					// SpatialRectangle(expandRect(y, x));
					Rectangle newRectangle = expandRectangle(y, x, m_image);

					// only add large rectangles
					if (newRectangle.width > 10 && newRectangle.height > 10) {
						Cell c = new Cell(m_image.getRGB(newRectangle.x, newRectangle.y, newRectangle.width,
								newRectangle.height, null, 0, newRectangle.width));

						// update rabbit location
						if (c.getCount() > 350) {
							m_rabbitRectangle = newRectangle;
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
		
		// the rabbit is not returned as part of the features list, so add it here
		if (m_rabbitRectangle != null)
		{
			features.add(m_rabbitRectangle);
			
			// set the initial rabbit location if it is null
			if (m_rabbitLoc == null)
				m_rabbitLoc = getCenter(m_rabbitRectangle);
		}
		
		
		
		return features;
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

	/**
	 * Create a point the mouse should move to in order to make the rabbit hit
	 * the (hopefully) best bell.
	 * 
	 * @param rectList
	 *            - a list of rectangles representing game features
	 * @return - a point to move the mouse to in screen coordinates
	 */
	private Point getMove(List<Point> featureLocations) {

		// we can't calculate where to move if we don't know where the rabbit is...?
		if (m_rabbitLoc == null) {
			return null;
		}
		
		
		Rectangle targetZone = new Rectangle (0, m_rabbitLoc.y - 100, m_currentImage.getWidth() - 1, 240);
		
		this.m_targetZone.y = m_rabbitLoc.y - 100;
		this.m_targetZone.width = m_currentImage.getWidth() - 1;
		
		m_currentTarget = getLowestFromZone(featureLocations, targetZone);
		
		Point mouseTarget = null;
		
		if (m_currentTarget != null)
		{
			mouseTarget = new Point(m_currentTarget);

			double xdistance = Math.abs(m_rabbitLoc.x - m_currentTarget.x);

			if (xdistance > 140)
			{
				//System.out.println("MADE IT HERE");
				if (m_currentTarget.x > m_rabbitLoc.x)
				{
					mouseTarget.setLocation(m_currentImage.getWidth() - 4, m_currentTarget.x);
				}
				else
				{
					mouseTarget.setLocation(4, m_currentTarget.y);
				}
			}
			else
			{
				mouseTarget.setLocation(m_currentTarget.x,m_currentTarget.y);
			}
		}
		
		
		
		
		// return a new move if we have a target
		// TODO cange this to overshoot the bell for extra speed
		if (mouseTarget != null) {
			// position in image + game screen offset
			return new Point(mouseTarget.x + m_gameArea.x, mouseTarget.y + m_gameArea.y);
		} else {
			// no move possible
			return null;
		}
	}
	

	private Point getLowestFromZone(List<Point> featureLocations, Rectangle targetZone) {
		
		double highestY = Double.MIN_VALUE;
		Point lowestFeature = null;
		for (Point feature : featureLocations)
		{
			if (feature.y > highestY && feature != m_rabbitLoc && targetZone.contains(feature))
			{
				highestY = feature.y;
				lowestFeature = feature;
			}
		}
		
		
		return lowestFeature;
	}

	private void movePlayer(Point move) {

		if (move != null) {
			// only move the mouse horizontally
			Point mousePos = MouseInfo.getPointerInfo().getLocation();
			m_robot.mouseMove(move.x, mousePos.y);

		}

		return;
	}
	

	public void update() {

		// capture the screen
		m_currentImage = m_robot.createScreenCapture(m_gameArea);

		// filter out extraneous image data
		filterImage(m_currentImage);

		// extract a list of features from the image
		Rectangle bounds = new Rectangle(0,0,m_currentImage.getWidth(), m_currentImage.getHeight());
		List<Rectangle> rectList = extractFeatures(m_currentImage, bounds);

		updateFeatureLocations(rectList, m_featureLocations);

		// calculate the best move for the rabbit to make
		Point move = getMove(m_featureLocations);

		// draw the rectangles on the image and make a move
		drawFeatures(rectList);

		// make the move
		if (m_allowMove && move != null) {
			movePlayer(move);
		}
	}

	/**
	 * Update target positions based on where the rectangles have moved.
	 * 
	 * @param featureList
	 * @param targets
	 */
	private void updateFeatureLocations(List<Rectangle> featureList, List<Point> targets) {

		// update all the target positions and remove targets without a feature behind them
		boolean targetUpdated;
		for (Iterator<Point> itr = targets.iterator(); itr.hasNext();) {
			
			targetUpdated = false;
			Point target = itr.next();
			for (Rectangle rect : featureList) {
				if (rect.contains(target)) {
					targetUpdated = true;

					// if the target point is inside a rectangle, it is assumed
					// to be the former target
					int centerX = (int) rect.getCenterX();
					int centerY = (int) rect.getCenterY();
					
					// check for special targets
					int deltaX = target.x - centerX;
					//int deltaY = target.y - centerY;
					
					if (m_birdLoc == null && deltaX > birdSpeedLimit)
					{
						m_birdLoc = target;
					}
					
					// update target location
					target.x = centerX;
					target.y = centerY;

					// check the next target
					break;
				}
			}

			// if an associated feature was not found, remove the target
			if (!targetUpdated) {
				
				// remove special features if they go missing
				if (target == m_rabbitLoc)
					m_rabbitLoc = null;
				if (target == m_birdLoc)
					m_birdLoc = null;
				
				//m_targetList.remove(target);
				itr.remove();
			}
		}
			

		
		// check for any rectangles without a target point
		boolean targetPointMissing;
		for (Rectangle rect : featureList) {
			
			targetPointMissing = true;
			for (Point t : targets) {
				if (rect.contains(t)) {
					targetPointMissing = false;
					break;
				}
			}

			// if the feature does not have a target point associated with it
			if (targetPointMissing) {
				Point newPoint = getCenter(rect);
				targets.add(newPoint);
				
				if (rect == m_rabbitRectangle)
					m_rabbitLoc = newPoint;
			}
		}

	}

	
	/** Static methods **/
	

	public static Point getCenter(Rectangle rect) {
		return new Point((int) rect.getCenterX(), (int) rect.getCenterY());
	}

	@SuppressWarnings("unused")
	private static int getSquaredDistance(Point p1, Point p2) {
		return (p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y);
	}

	private static boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

	private static int[] rgbToRGBArray(int value) {
		int[] rgb = new int[3];
		value = value >> 8;
		rgb[0] = value & 0xFF;
		value = value >> 8;
		rgb[1] = value & 0xFF;
		value = value >> 8;
		rgb[2] = value & 0xFF;
		return rgb;
	}

	
	private static boolean[] checkBorders(Rectangle r, BufferedImage image) {
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

	
	private static Rectangle expandRectangle(int row, int col, BufferedImage image) {

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
	
}
