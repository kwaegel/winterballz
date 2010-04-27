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
	public static final int birdSpeedLimit = 2;
	public static final float rabbitHorzontalSpeedMultiplier = 1.25f;
	public static final float rabbitVerticalSpeedMultiplier = 0.0005f;
	public static final int zoneLowerOffset = 100;
	public static final int zoneHeight = 240;
	
	private boolean m_allowMove = true;

	private BufferedImage m_currentImage;
	private BufferedImage m_scaledImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;
	private Robot m_robot;

	// special rectangles
	private Rectangle m_rabbitRectangle;
	private Rectangle m_birdRectangle;
	
	// special feature locations
	private Point m_rabbitLocation;
	private Point m_birdLocation;
	private int m_birdDeltaX;
	
	
	private List<Point> m_featureLocations;
	private Point m_currentTarget;
	private Rectangle m_targetZone = new Rectangle (0, 0, 0, zoneHeight);

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
		
		for (Rectangle r : recs)
		{
			g2d.setColor(Color.red);
			g2d.draw(r);
		}
		
		// draw circles on all the targets
		for (Point p : m_featureLocations)
		{
			if (p == m_rabbitLocation)
				g2d.setColor(Color.yellow);
			else if (p == m_birdLocation)
				g2d.setColor(Color.cyan);
			else
				g2d.setColor(Color.magenta);
			g2d.fillOval(p.x - 8, p.y - 8, 16, 16);
		}
		
		// draw the target zone
		g2d.setColor(Color.white);
		g2d.draw(m_targetZone);
		
		// draw a line to the target
		if (m_currentTarget != null && m_rabbitLocation != null)
		{
			g2d.setColor(Color.green);
			g2d.drawLine(m_currentTarget.x, m_currentTarget.y, m_rabbitLocation.x, m_rabbitLocation.y);
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

	private List<Rectangle> extractFeatures(BufferedImage image, Rectangle bounds) {
		
		// Approximate sizes:
		// bird:	26x14
		// bell:	27x29
		// rabbit:	46x25

		// reset rabbit each frame
		m_rabbitRectangle = null;
		m_birdRectangle = null;
		
		List<Rectangle> features = new ArrayList<Rectangle>();
		
		
		// find our maximum bounds
		int minX = Math.max(0, bounds.x);
		int minY = Math.max(0, bounds.y);
		int maxX = Math.min(bounds.x + bounds.width, image.getWidth());
		int maxY = Math.min(bounds.y + bounds.height, image.getHeight());

		// check each pixel in the search area
		for (int x = minX; x < maxX; x += Botz.horzRes) {
			nextPixel: for (int y = minY; y < maxY; y += Botz.verticalRes) {

				if (x > image.getWidth() || y > image.getHeight())
					continue;
				
				if (isWhite(image.getRGB(x, y))) {

					// skip pixel if it is inside a previous rectangle
					for (Rectangle oldRect : features) {
						if (oldRect.contains(x, y)) {
							continue nextPixel;
						}
					}

					// SpatialRectangle newRect = new
					// SpatialRectangle(expandRect(y, x));
					Rectangle newRectangle = expandRectangle(y, x, image);

					// only add large rectangles
					if (newRectangle.width > 10 && newRectangle.height > 10) {
						Cell c = new Cell(image.getRGB(newRectangle.x, newRectangle.y, newRectangle.width, newRectangle.height, null, 0, newRectangle.width));

						int blackPixelCount = c.getCount();
						
						// update rabbit location
						if (blackPixelCount > 350) {
							m_rabbitRectangle = newRectangle;
							features.add(m_rabbitRectangle);
							continue nextPixel;
						}
						else if (newRectangle.height < 20 && newRectangle.width > 25)
						{
							m_birdRectangle = newRectangle;
							features.add(m_birdRectangle);
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
			
			// update the rabbit location
			if (m_rabbitRectangle == null)
				m_rabbitLocation = getCenter(m_rabbitRectangle);
		}
		
		if (m_birdRectangle != null)
		{
			features.add(m_birdRectangle);
			
			// update the rabbit location
			if (m_birdLocation == null)
				m_birdLocation = getCenter(m_birdRectangle);
		}
		
		
		
		return features;
	}


	private void filterImage(BufferedImage image) {
		
		//int minX = Math.max(0, bounds.x);
		//int minY = Math.max(0, bounds.y);
		//int maxX = Math.min(bounds.x + bounds.width, image.getWidth());
		//int maxY = Math.min(bounds.y + bounds.height, image.getHeight());
		int minX = 0;
		int minY = 0;
		int maxX = image.getWidth();
		int maxY = image.getHeight();

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
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
		if (m_rabbitLocation == null) {
			return null;
		}
		
		
		Rectangle targetZone = new Rectangle (0, m_rabbitLocation.y - 100, m_currentImage.getWidth() - 1, 240);
		
		this.m_targetZone.y = m_rabbitLocation.y - zoneLowerOffset;
		this.m_targetZone.width = m_currentImage.getWidth() - 1;
		
		m_currentTarget = getLowestFromZone(featureLocations, targetZone);
		
		Point mouseTarget = null;
		
		if (m_currentTarget != null)
		{
			mouseTarget = new Point(m_currentTarget);
			
			int xDistance = m_currentTarget.x - m_rabbitLocation.x;	// positive if target is to the right of the rabbit, negative if target is to the left
			//int yDistance = m_currentTarget.y - m_rabbitLocation.y;	// positive if target is below the rabbit, negative if target is above
			
			// calculate the distance to overshoot
			int minMouseX = 4;
			int maxMouseX = m_currentImage.getWidth() - 4; 

			// set the horizontal movement to be a multiple of the distance to the target
			int desiredDeltaX = (int) (xDistance * rabbitHorzontalSpeedMultiplier);// + (int) (yDistance * rabbitVerticalSpeedMultiplier);
			
			// clamp the overshoot distance to the screen size
			int desiredMouseX = desiredDeltaX + m_rabbitLocation.x;
			
			if (m_currentTarget == m_birdLocation)
			{
				desiredMouseX += m_birdDeltaX;
			}
			
			int actualMouseX = clamp(desiredMouseX, minMouseX, maxMouseX);
			
			mouseTarget.setLocation(actualMouseX, m_currentTarget.y);
		}
		
		
		// return a new move if we have a target
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
			if (feature.y > highestY && feature != m_rabbitLocation && targetZone.contains(feature))
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
		
		m_targetZone.width = m_currentImage.getWidth()-1;

		// filter out extraneous image data
		filterImage(m_currentImage);

		// extract a list of features from the image
		List<Rectangle> rectList = extractFeatures(m_currentImage, m_targetZone);

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
					int deltaX = Math.abs(target.x - centerX);
					
					// store bird speed
					if (target == m_birdLocation)
					{
						m_birdDeltaX = target.x - m_birdLocation.x;
					}
					
					if (m_birdLocation == null && deltaX > birdSpeedLimit)
					{
						m_birdLocation = target;
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
				if (target == m_rabbitLocation)
					m_rabbitLocation = null;
				if (target == m_birdLocation)
					m_birdLocation = null;
				
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
					m_rabbitLocation = newPoint;
				if (rect == m_birdRectangle)
					m_birdLocation = newPoint;
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
		
		int maxX = image.getWidth();
		int maxY = image.getHeight();

		// check top
		int row = r.y - 1;

		if (row > 0) {
			for (int col = r.x; col <= r.x + r.width; col++) {
				if (col < 0 || col > maxX)
					break;
				if ( isWhite(image.getRGB(col, row))) {
					bArray[0] = true;
					break;
				}
			}
		}

		// check right
		int col = r.x + r.width + 1;
		if (col < maxX) {
			for (row = r.y; row <= r.y + r.height; row++) {
				if (row < 0 || row > maxY)
					break;
				if (isWhite(image.getRGB(col, row))) {
					bArray[1] = true;
					break;
				}
			}
		}

		// check bottom
		row = r.y + r.height + 1;
		if (row < maxY) {
			for (int c = r.x; c <= r.x + r.width; c++) {
				if (col < 0 || col > maxX)
					break;
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
				if (row < 0 || row > maxY)
					break;
				if (isWhite(image.getRGB(col, row))) {
					bArray[3] = true;
					break;
				}
			}
		}
		return bArray;
	}

	/**
	 * Clamps a value to within a specific range
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	private static int clamp(int value, int min, int max)
	{
		if (value < min)
			return min;
		else if (value > max)
			return max;
		else
			return value;
	}
	
	private static Rectangle expandRectangle(int row, int col, BufferedImage image) {

		Rectangle r = new Rectangle(col, row, 1, 1);

		boolean[] bA = checkBorders(r, image);
		
		int maxX = image.getWidth();
		int maxY = image.getHeight();

		// if there is a white pixel on any side of the rectangle, grow the
		// rectangle in that direction
		while (bA[0] || bA[1] || bA[2] || bA[3]) {
			// grow up
			if (bA[0] && r.y > 0) {
				r.y -= 1;
				r.height++;
			}

			// grow right
			if (bA[1] && r.x + r.width < maxX) {
				r.width++;
			}

			// grow down
			if (bA[2] && r.y + r.height < maxY) {
				r.height++;
			}

			// grow left
			if (bA[3] && r.x > 0){
				r.x -= 1;
				r.width++;
			}

			// check around the borders again
			bA = checkBorders(r, image);
		}

		return r;

	}
	
}
