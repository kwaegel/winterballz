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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Botz {
	
	public static final int verticalRes = 10;
	public static final int horzRes = 7;

	private BufferedImage m_currentImage;
	private Rectangle m_gameArea;
	private DrawPanel m_drawPanel;
	private boolean m_allowMove = true;
	private Rectangle m_rabbit;
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
		
		// TODO change m_target to a rectangle to make sure it intersects the last rectangle position?
		
		// check if our target bell is still on the screen
		Point move = null;
		if (m_target != null)
		{
			// check to see if target is still pointing to a rectangle
			boolean targetMissing = true;
			for (Rectangle r : rectList){
				if (r.contains(m_target)){
					targetMissing = false;
					m_target.x = (int) r.getCenterX();
					m_target.y = (int) r.getCenterY();
				}
			}
			
			if (targetMissing)
				m_target = null;
		}
		else
		{
			// get a new target
			
			// get current screen to draw prediction lines on
			Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();
			
			Point closestBell = this.findNearest(rectList, m_rabbit);
			
			if (closestBell != null)
				move = new Point(closestBell.x + m_gameArea.x, closestBell.y + m_gameArea.y);
			
			if (move != null)
			{
				g2d.setColor(Color.magenta);
				g2d.drawLine(closestBell.x, closestBell.y, m_rabbit.x, m_rabbit.y);
			}
			
			m_target = closestBell;
		}
		
		
		if (m_target != null)
			return new Point(m_target.x + m_gameArea.x, m_target.y + m_gameArea.y);
		else
			return null;
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
		
		if (m_target != null) {
			g2d.setColor(Color.cyan);
			g2d.fillOval(m_target.x, m_target.y, 10, 10);
		}

	}

	private Point findNearest(List<Rectangle> list, Rectangle rabbit) {

		Rectangle closestFeature = null;
		if (list.size() > 2) {
			
			int closest = Integer.MAX_VALUE;
			
			for (Rectangle r : list) {
				//int distance (int) Point2D.distance(r.getCenterX(), r.getCenterY(), rabbit.getCenterX(), rabbit.getCenterY());
				int distance = (int) (rabbit.getCenterY() - r.getCenterY());
				
				
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
		
		
		
		
		
		// ensure all the threads are finished.
		//rectangleExtractors.shutdown();

		return features;

	}
	
	// divide up the image vertically
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
