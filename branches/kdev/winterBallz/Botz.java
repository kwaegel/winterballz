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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Botz {
	
	public static final int verticalRes = 10;
	public static final int horzRes = 7;

	private BufferedImage m_currentImage;
	private BufferedImage m_scaledImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;
	private boolean m_allowMove = true;
	private Rectangle m_rabbit;
	private Rectangle m_closestFeature;
	private Point m_target;
	private Robot m_robot;
	
	ExecutorService rectangleExtractors;

	public Botz(Rectangle bounds, DrawPanel panel) {
		m_drawPanel = panel;
		
		m_gameArea = new Rectangle(bounds);
		
		rectangleExtractors = Executors.newCachedThreadPool();
		
		
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
		
		// calculate the best move for the rabbit to make
		Point move = getMove(rectList);
		
		// draw the rectangles on the image and make a move
		drawFeatures(rectList);
		
		// make the move
		if(m_allowMove && move != null)
			movePlayer(move);
		

		// put the filtered image in the panel and draw
		
		// scale the image to fit the draw panel size
		Dimension drawSize = m_drawPanel.getSize();
		int width = m_currentImage.getWidth();
		int height = m_currentImage.getHeight();
		double aspectRatio = (double)width / (double)height;
		int newWidth = drawSize.width;
		int newHeight = (int)(newWidth / aspectRatio);
		
		
		// Create new (blank) image of required (scaled) size
		m_scaledImage = new BufferedImage(drawSize.width, drawSize.height, BufferedImage.TYPE_INT_RGB);

		// Paint scaled version of image to new image
		Graphics2D graphics2D = m_scaledImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(m_currentImage, 0, 0, newWidth, newHeight, null);

		// clean up
		graphics2D.dispose();
		
		m_drawPanel.setImage(m_scaledImage);
		
		// draw the screen immediately
		m_drawPanel.paintImmediately(0, 0, m_scaledImage.getWidth(), m_scaledImage.getHeight());

	}

	/**
	 * Create a point the mouse should move to in order to make the rabbit
	 * hit the (hopefully) best bell.
	 * @param rectList - a list of rectangles representing game features
	 * @return - a point to move the mouse to in screen coordinates
	 */
	private Point getMove(List<Rectangle> rectList) {
		
		// we can't calculate a move if we don't know where the rabbit is...
		if (m_rabbit == null)
			return null;
		
		// check if our target is still on the screen
		if (m_target != null)
		{
			// check to see if target is still inside a rectangle 
			// (Probably the same feature from the last frame)
			boolean targetMissing = true;
			for (Rectangle r : rectList){
				
				if (r.contains(m_target)){
					targetMissing = false;
					
					// if the target point is inside a rectangle, it is assumed to be
					// the former target
					m_target.x = (int) r.getCenterX();
					m_target.y = (int) r.getCenterY();
				}
			}
			
			// if the target was not found, set to null
			if (targetMissing) {
				m_target = null;
			}
		} else {
			// find a new target feature
			
			// find the nearest feature to the rabbit
			m_closestFeature = this.findNearest(rectList, m_rabbit);
			
			if (m_closestFeature != null)
				m_target = getCenter(m_closestFeature);
			else
				m_target = null;
		}
		
		// return a new move if we have a target
		if (m_target != null)
			return new Point(m_target.x + m_gameArea.x, m_target.y + m_gameArea.y);// return position in image + game screen offset
		else
			return null;
	}
	
	public static Point getCenter (Rectangle rect)
	{
		return new Point((int)rect.getCenterX(), (int)rect.getCenterY());
	}
	
	public static int getSquaredDistance (Point p1, Point p2)
	{
		return (p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y);
	}

	/**
	 * Draw:
	 * -> a red box around every game feature
	 * -> a yellow box around the rabbit
	 * -> a magenta line from the rabbit to the nearest game feature
	 * -> a cyan circle on the current target game feature
	 * @param recs
	 */
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

		// if we know where the rabbit is, highlight it.
		if (m_rabbit != null){
			g2d.setColor(Color.yellow);
			g2d.drawRect(m_rabbit.x, m_rabbit.y, m_rabbit.width, m_rabbit.height);
		}
		
		// draw a circle at the current target
		if (m_target != null) {
			g2d.setColor(Color.cyan);
			g2d.fillOval(m_target.x-5, m_target.y-5, 10, 10);
		}
		
		// draw a line from the rabbit to the closest feature
		if (m_closestFeature != null && m_rabbit != null)
		{
			// draw a line to the closest bell
			g2d.setColor(Color.magenta);
			g2d.drawLine((int)m_closestFeature.getCenterX(), (int)m_closestFeature.getCenterY(), m_rabbit.x, m_rabbit.y);
		}

	}

	private Rectangle findNearest(List<Rectangle> list, Rectangle rabbit) {

		Rectangle closestFeature = null;
		if (list.size() > 2) {
			
			int closest = Integer.MAX_VALUE;
			
			for (Rectangle r : list) {
				int distance =(int) Point.distance(r.getCenterX(), r.getCenterY(), rabbit.getCenterX(), rabbit.getCenterY());
				//int distance = (int) (rabbit.getCenterY() - r.getCenterY());
				
//				if (r.y < rabbit.getCenterY())
//					continue;
				
//				if (r.y > m_currentImage.getHeight() - 50)
//					continue;
				
				if (distance < closest){
					closest = distance;
					closestFeature = r;
				}
			}
		}
		
		if (closestFeature == null)
			return null;
		return closestFeature;
	}

	private List<Rectangle> extractFeatures(BufferedImage image) {
		
		// reset rabbit each frame
		m_rabbit = null;
		
		List<Rectangle> features = new ArrayList<Rectangle>();
		
		//rectangleExtractors = Executors.newFixedThreadPool(4);
		
		// divide the image into separate areas with black borders
		List<Rectangle> sections = new ArrayList<Rectangle>(4);// = this.divideImage(image, 1);
		List<Future<Pair<List<Rectangle>, Rectangle>>> asyncResults = new ArrayList<Future<Pair<List<Rectangle>, Rectangle>>>();
		
		// create four tasks to search each area
		sections.add(new Rectangle(0,0,image.getWidth()/2, image.getHeight()));
		sections.add(new Rectangle(image.getWidth()/2,0,image.getWidth()/2, image.getHeight()));
		
		// start threads
		for (Rectangle bounds : sections) {
			FeatureExtractor fxt = new FeatureExtractor(bounds, image);
			asyncResults.add(rectangleExtractors.submit(fxt));
		}
		
		// wait for thread results
		for (Future<Pair<List<Rectangle>, Rectangle>> ar : asyncResults)
		{	
			try {
				Pair<List<Rectangle>, Rectangle> results = ar.get();
				features.addAll( results.first );
				
				// check if this thread found the rabbit
				if (results.second != null)
					m_rabbit = results.second;
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		return features;

	}
	
	// divide up the image vertically
	@SuppressWarnings("unused")
	private List<Rectangle> divideImage(BufferedImage image, int sections) {
		
		List<Rectangle> list = new ArrayList<Rectangle>();
		
		int step = (int)(image.getWidth() / sections)-1;
		boolean collision = false;
		
		int x,y,height,width;
		
		x=y=0;
		int col=step;
		while ( !collision && col < image.getWidth() )
		{
			for (int row = 0; row < image.getHeight(); row++){
				// make sure the column is empty of white pixels
				if (isWhite(image.getRGB(col, row)))
				{
					collision = true;
					break;
				}
			}
			
			if (!collision)
			{
				height = image.getHeight();
				width = col - x;
				list.add(new Rectangle(x,y,height,width));
				col += step;
			}
			else
			{
				col++;
				collision = false;
			}
		}
		
		return list;
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
	
	public static boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

}
