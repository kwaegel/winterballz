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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Botz {

	public static final int verticalRes = 10;
	public static final int horzRes = 7;
	public static final int maxLookahead = 20;
	
	private boolean m_allowMove = false;

	private BufferedImage m_currentImage;
	private BufferedImage m_scaledImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;

	private Rectangle m_rabbit;
	private Point m_rabbitLoc;
	private Rectangle m_closestFeature;
	//private Point m_currentTarget;
	private Robot m_robot;
	private List<Point> m_targets;
	
	private ExecutorService rectangleExtractors;

	public Botz(Rectangle bounds, DrawPanel panel) {
		m_drawPanel = panel;
		m_gameArea = new Rectangle(bounds);
		m_targets = new ArrayList<Point>();

		rectangleExtractors = Executors.newCachedThreadPool();

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

//		for (Rectangle r : recs) {
//			if (r.width > 10) {
//				g2d.setColor(Color.RED);
//				g2d.drawRect(r.x, r.y, r.width, r.height);
//			} else {
//				g2d.setColor(Color.GREEN);
//				g2d.drawRect(r.x, r.y, r.width, r.height);
//			}
//		}

		// if we know where the rabbit is, highlight it.
		if (m_rabbit != null) {
			g2d.setColor(Color.yellow);
			g2d.drawRect(m_rabbit.x, m_rabbit.y, m_rabbit.width, m_rabbit.height);
		}
		
		// draw circles about all the targets
		for (Point p : m_targets)
		{
			if (p == m_rabbitLoc)
				g2d.setColor(Color.yellow);
			else
				g2d.setColor(Color.magenta);
			g2d.fillOval(p.x - 8, p.y - 8, 16, 16);
		}
		
		// draw lines between all the features
		g2d.setColor(Color.magenta);
		Point p1,p2;
		int rangeMax = Math.min(maxLookahead, m_targets.size()-1);
		for (int i=0; i < rangeMax;i++)
		{
			p1 = m_targets.get(i);
			p2 = m_targets.get(i+1);
			g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
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

	private List<Rectangle> extractFeatures(BufferedImage image) {

		// reset rabbit each frame
		m_rabbit = null;

		List<Rectangle> features = new ArrayList<Rectangle>();

		// divide the image into separate areas with black borders
		List<Rectangle> sections = new ArrayList<Rectangle>(4);// =
		// this.divideImage(image, 1);
		List<Future<Pair<List<Rectangle>, Rectangle>>> asyncResults = new ArrayList<Future<Pair<List<Rectangle>, Rectangle>>>();

		// create four tasks to search each area
		sections.add(new Rectangle(0, 0, image.getWidth() / 2, image.getHeight()));
		sections.add(new Rectangle(image.getWidth() / 2, 0, image.getWidth() / 2, image.getHeight()));

		// start threads
		for (Rectangle bounds : sections) {
			FeatureExtractor fxt = new FeatureExtractor(bounds, image);
			asyncResults.add(rectangleExtractors.submit(fxt));
		}

		// wait for thread results
		for (Future<Pair<List<Rectangle>, Rectangle>> ar : asyncResults) {
			try {
				Pair<List<Rectangle>, Rectangle> results = ar.get();
				features.addAll(results.first);

				// check if this thread found the rabbit
				if (results.second != null) {
					m_rabbit = results.second;
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		// the rabbit is not returned as part of the features list
		if (m_rabbit != null)
			features.add(m_rabbit);

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
	private Point getMove(List<Rectangle> rectList, List<Point> targetPoints) {

		// we can't calculate where to move if we don't know where the rabbit is...
		if (m_rabbitLoc == null) {
			return null;
		}
		
		// sort the targets list in the order they should be hit
		// for now, that is which ones are closest to each other.
		
		// make sure the rabbit is the first in the list.
		Collections.swap(targetPoints, targetPoints.indexOf(m_rabbitLoc), 0);
		
		Point nearest = null;
		int nearestIndex;
		int rangeMax = Math.min(maxLookahead, targetPoints.size()-1);
		for (int i=2; i < rangeMax; i++)
		{
			// find the nearest next point
			nearest = getNearestPoint(targetPoints, targetPoints.get(i));
			
			// swap the nearest with the next in the list
			nearestIndex = targetPoints.indexOf(nearest);
			if (nearestIndex >= 0)
				Collections.swap(targetPoints, nearestIndex, i+1);
			else
			{
				System.err.println("Index was negative");
			}
		}
		
		Point firstTarget;
		try{
			firstTarget = m_targets.get(1);
		}
		catch (IndexOutOfBoundsException e)
		{
			firstTarget = null;
		}
		
		// return a new move if we have a target
		if (firstTarget != null) {
			// position in image + game screen offset
			return new Point(firstTarget.x + m_gameArea.x, firstTarget.y + m_gameArea.y);// return
		} else {
			return null;
		}
	}

	private void movePlayer(Point move) {

		if (move != null) {
			// System.out.println(move);

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
		List<Rectangle> rectList = extractFeatures(m_currentImage);

		updateTargets(rectList, m_targets);

		// calculate the best move for the rabbit to make
		Point move = getMove(rectList, m_targets);

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
	private void updateTargets(List<Rectangle> featureList, List<Point> targets) {

		// update all the target positions and remove targets without a feature behind them
		boolean targetUpdated;
		for (Iterator<Point> itr = targets.iterator(); itr.hasNext();) {
			
			targetUpdated = false;
			Point target = itr.next();
			for (Rectangle feature : featureList) {
				if (feature.contains(target)) {
					targetUpdated = true;

					// if the target point is inside a rectangle, it is assumed
					// to be the former target
					target.x = (int) feature.getCenterX();
					target.y = (int) feature.getCenterY();

					// check the next target
					break;
				}
			}

			// if an associated feature was not found, remove the target
			if (!targetUpdated) {
				// if rabbitLoc is about to be removed from the list, set it to null
				if (target == m_rabbitLoc)
					m_rabbitLoc = null;
				itr.remove();
			}
		}
			

		
		// check for any features without a target point
		boolean targetPointMissing;
		for (Rectangle feature : featureList) {
			
			targetPointMissing = true;
			for (Point t : targets) {
				if (feature.contains(t)) {
					targetPointMissing = false;
					break;
				}
			}

			// if the feature does not have a target point associated with it
			if (targetPointMissing) {
				Point newPoint = getCenter(feature);
				targets.add(newPoint);
				
				if (feature == m_rabbit)
					m_rabbitLoc = newPoint;
			}
		}

	}

	
	/** Static methods **/
	
	
	/**
	 * Find the item in the list that is closest to the point
	 * 
	 * @param list
	 * @param point
	 * @return
	 */
	@SuppressWarnings("unused")
	private static Rectangle findNearest(List<Rectangle> list, Rectangle point) {

		Rectangle closestFeature = null;
		if (list.size() > 2) {

			int closest = Integer.MAX_VALUE;

			for (Rectangle r : list) {
				int distance = (int) Point2D.distance(r.getCenterX(), r.getCenterY(), point.getCenterX(), point
						.getCenterY());

				if (distance < closest) {
					closest = distance;
					closestFeature = r;
				}
			}
		}

		if (closestFeature == null) {
			return null;
		}
		return closestFeature;
	}

	// return the point in list that is above and closest to the basePoint
	private static Point getNearestPoint(List<Point> pointList, Point basePoint)
	{
		Point closestPoint = null;
		int closest = Integer.MAX_VALUE;

		for (Point p : pointList) {
			if (p != basePoint)
			{
				int distance = (int) Point2D.distance(p.x, p.y, basePoint.x, basePoint.y);

				if (distance < closest) {
					closest = distance;
					closestPoint = p;
				}
			}
		}
		
		return closestPoint;
	}
	
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

}
