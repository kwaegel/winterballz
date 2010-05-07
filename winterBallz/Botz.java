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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Console;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Botz {

	public static final int verticalRes = 10;
	public static final int horzRes = 7;
	public static final int maxLookahead = 3;
	public static final int birdSpeedLimit = 3;
	private static final int hrozValueMult = 10;
	private static final int vertValueMult = 2;
	private static final int maxBirdHeight = 20;
	//private static final int expandIncrement = 3;
	
	private boolean m_allowMove = true;
	private boolean m_scaleImage = false;

	private BufferedImage m_currentImage;
	private BufferedImage m_scaledImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;
	private Robot m_robot;

	// special rectangles
	private Rectangle m_rabbitRectangle;
	//private Rectangle m_birdRectangle;
	
	// special feature locations
	private Rabbit m_rabbit;
	private Bird m_bird;
	private Point m_rabbitLocation;
	
	
	private List<Point> m_featureLocations= new ArrayList<Point>();
	private List<GameFeature> m_persistentFeatures = new ArrayList<GameFeature>();
	private Point m_currentTarget;
	private Rectangle m_targetZone = new Rectangle (0, 0, 0, 240);

	
	
	/**
	 * Constructor.
	 * @param bounds
	 * @param panel
	 */
	public Botz(Rectangle bounds, DrawPanel panel) {
		m_drawPanel = panel;
		m_gameArea = new Rectangle(bounds);

		try {
			m_robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
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
		
//		//m_persistentFeatures.clear();
//		for (Rectangle r : recs)
//		{
//			m_persistentFeatures.add(new Bell(r));
//		}
		
		for (Rectangle r : recs){
			g2d.drawRect(r.x, r.y, r.width, r.height);
		}
		
		
		
		
		for (GameFeature gf : m_persistentFeatures)
		{
			gf.draw(g2d);
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

		if (m_scaleImage)
			{
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
		else {
			m_drawPanel.setImage(m_currentImage);
			m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(), m_currentImage.getHeight());
		}

	}

	private List<Rectangle> extractFeatures(BufferedImage m_image, Rectangle m_area) {
		
		// mark all game features as not updated;
		for (GameFeature gf : m_persistentFeatures)
		{
			gf.updated = false;
		}

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

					Rectangle newRectangle = expandRectangle(y, x, m_image);
					
					// skip overlapping rectangles
					for (Rectangle otherRect : features) {
						if (newRectangle.intersects(otherRect)) {
							continue nextPixel;
						}
					}
					

					// only add large rectangles
					if (newRectangle.width > 10 && newRectangle.height > 10) {
						
						// check if the rectangle intersects with an old feature
						GameFeature gf;
						
						for (Iterator<GameFeature> itr = m_persistentFeatures.iterator(); itr.hasNext(); ) {
							
							gf = itr.next();
							
							// if the new rectangle intersects a game feature, assume that they are the same feature and move gf
							// TODO this null check should not be needed but it is...
							if (newRectangle.intersects(gf)){
								
								// move to newRectangle position
								gf.move(newRectangle);
//								System.out.println("Game feature updated");
								
								break nextPixel;
							}
						}
						
						// if the code reaches this point, the newRectangle did not intersect any of the old game features
						// try to determine what kind of game feature it is and add that to the persistent feature list
						// TODO with filtering at 185, only rabbits and birds have an eye (black hole surrounded by white). Try to find this.
						// Can then distinguish birds from rabbits by size (the rabbit is taller then the bird).
						
//						int whiteCount = countPixels(m_currentImage, newRectangle, Color.white);
//						int blackCount = countPixels(m_currentImage, newRectangle, Color.black);
//						float whiteBlackRatio = (float) whiteCount / blackCount;
						
						
						boolean solid = isSolid(m_currentImage, newRectangle);
						if (solid)
						{
							m_persistentFeatures.add(new Bell(newRectangle));
						}
						else
						{
//							System.out.println("not solid");
							m_rabbit = new Rabbit(newRectangle);
							m_persistentFeatures.add(m_rabbit);
							
//							if (newRectangle.height > maxBirdHeight)
//							{
//								m_rabbit = new Rabbit(newRectangle);
//								m_persistentFeatures.add(m_rabbit);
//							}
//							else
//							{
//								m_bird = new Bird(newRectangle);
//								m_persistentFeatures.add(m_bird);
//							}
						}
						
						
//						System.out.println(whiteCount + " / " + blackCount + " = " + whiteBlackRatio);

						// decide what type of game feature this rectangle is
						//if (blackCount > 330) {
//						if (whiteBlackRatio < 1.33f && whiteCount > 450) {
//							m_rabbit = new Rabbit(newRectangle);
//							m_persistentFeatures.add(m_rabbit);
////							System.out.println(whiteCount + " / " + blackCount + " = " + whiteBlackRatio);
//						}
//						else //if ( blackCount <= 330 && whiteCount < 460)
//						{	
//							// all bells have a black count < 300 and a white count < 460
//							m_persistentFeatures.add(new Bell(newRectangle));
//						}
						
//						if (whiteBlackRatio > 2.0f)
//						{
//							m_persistentFeatures.add(new Bell(newRectangle));
//						}
//						else
//						{
//							m_rabbit = new Rabbit(newRectangle);
//							m_persistentFeatures.add(m_rabbit);
//						}

						

						// add this feature to the list of rectangles
						//features.add(newRectangle);
					}
					else
					{
						// check for game over screen and stop mouse movement if it is found
						// TODO: implement game over checking
						int whiteCount = countPixels(m_currentImage, newRectangle, Color.white);
						int blackCount = countPixels(m_currentImage, newRectangle, Color.black);
						
						// the angle brackets around the "play again" button have 15 white pixels and 21 black pixels with the current filter
						if (whiteCount > 10 && whiteCount < 20 && blackCount > 10 && blackCount < 25)
						{
							//System.out.println("Game over");
							//m_allowMove = false;
						}
						features.add(newRectangle);
						
					}
				}
			} // end inner pixel loop
		} // end outer pixel loop	
		
		
		// remove orphaned gameFeatures from the persistent feature list
		// TODO figure out why removing orphaned game features is not working
		for (Iterator<GameFeature> itr = m_persistentFeatures.iterator(); itr.hasNext(); ) {
			GameFeature gf = itr.next();
			if (gf.updated == false){
				itr.remove();
				//System.out.println("GF removed");
			}
		}
		
		
		return features;
	}
	
	/**
	 * Returns if an image is solid with no inside curves. This is only true of bells
	 * @param image
	 * @param bounds
	 * @return
	 */
	private boolean isSolid(BufferedImage image, Rectangle bounds)
	{
		int maxWidth = bounds.x + bounds.width;
		int maxHeight = bounds.y + bounds.height;
		
		// for each row
		int numberOfTransitions = 0;
		int previousColor, currentColor;
		for (int row = bounds.x; row < maxHeight; row++)
		{
			previousColor = image.getRGB(0, row);
			// for each column in the row
			for (int col = 0; col < maxWidth; col++){
				
				currentColor = image.getRGB(col, row);
				
				// if the new color differs (only need to check one cell)
				if (currentColor != previousColor)
				{
					numberOfTransitions++;
					
					// stop and return as soon as there are more then 2 transitions in a row
					if (numberOfTransitions > 2)
						return false;	
				}
				
				previousColor = currentColor;
			}
//			if (numberOfTransitions > 0)
//				System.out.println(numberOfTransitions);
		}
		
		
		return true;
	}
	
	
	private int countPixels(BufferedImage image, Rectangle bounds, Color color)
	{
		int[] rgbArray = image.getRGB(bounds.x, bounds.y, bounds.width, bounds.height, null, 0, bounds.width);
		
		int count = 0;
		for (int i : rgbArray) {
			if (i == color.getRGB()) {
				count++;
			}
		}
		
		return count;
	}

	private void filterImage(BufferedImage image) {

		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				int rgb = image.getRGB(x, y);
				int[] RGB = rgbToRGBArray(rgb);
				// note: filter out the lower 30 pixels, as the ground confuses the feature extraction
				if (image.getHeight() - y < 30 || RGB[0] < 185 || RGB[1] < 185 || RGB[2] < 185) {
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
		
		this.m_targetZone.y = m_rabbitLocation.y - 100;
		this.m_targetZone.width = m_currentImage.getWidth() - 1;
		
		m_currentTarget = getLowestFromZone(featureLocations, targetZone);
		
		Point mouseTarget = null;
		
		if (m_currentTarget != null)
		{
			mouseTarget = new Point(m_currentTarget);

			double xdistance = Math.abs(m_rabbitLocation.x - m_currentTarget.x);

			// if the rabbit is a certin distance away, overshoot the target to move faster.
			if (xdistance > 140)
			{
				//System.out.println("MADE IT HERE");
				if (m_currentTarget.x > m_rabbitLocation.x)
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
		if (mouseTarget != null) {
			// position in image + game screen offset
			return new Point(mouseTarget.x + m_gameArea.x, mouseTarget.y + m_gameArea.y);
		} else {
			// no move possible
			return null;
		}
	}
	
	private Point getBestValuedFromZone(List<Point> featureLocations, Rectangle targetZone) {
		// find the point with the lowest weighted distance value
		
		double highestY = Double.MIN_VALUE;
		
		int deltaX, deltaY, value;
		double lowestValue = Integer.MAX_VALUE;
		
		
		Point bestFeature = null;
		for (Point feature : featureLocations)
		{	
			
			if (feature.y > highestY && feature != m_rabbitLocation && targetZone.contains(feature))
			{
				deltaX = Math.abs(m_rabbitLocation.x - feature.x);
				deltaY = Math.abs(m_rabbitLocation.y - feature.y);	// positive if feature is below
				
				value = deltaX * Botz.hrozValueMult + deltaY * Botz.vertValueMult;
				
				if (value < lowestValue)
				{
					lowestValue = value;
					bestFeature = feature;
				}
				
			}
		}
		
		
		return bestFeature;
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
					
					
//					if (rect.contains(m_rabbitLocation)){
//						m_rabbitLocation = getCenter(rect);
//					}
					
					// check for special targets
//					int deltaX = target.x - centerX;
					//int deltaY = target.y - centerY;
					
//					if (m_birdLocation == null && deltaX > birdSpeedLimit)
//					{
//						m_birdLocation = target;
//					}
					
					// update target location
					target.x = centerX;
					target.y = centerY;

					// check the next target
					break;
				}
			}

			// if an associated feature was not found, remove the target
			if (!targetUpdated && target != m_rabbitLocation) {
				
				// remove special features if they go missing
//				if (target == m_rabbitLocation)
//					m_rabbitLocation = null;
//				if (target == m_birdLocation)
//					m_birdLocation = null;
				
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
			}
		}

	}

	
	/** Static methods **/
	

	public static Point getCenter(Rectangle rect) {
		return new Point((int) rect.getCenterX(), (int) rect.getCenterY());
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
				if (col < 0 || col > maxX-1)
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
				if (row < 0 || row > maxY-1)
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
				if (col < 0 || col > maxX-1)
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
				if (row < 0 || row > maxY-1)
					break;
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
