
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Botz {

	BufferedImage m_currentImage;
	Dimension m_gameDimension;
	DrawPanel m_drawPanel;
	Point m_gameLocation;
	Point m_previousMove = new Point ();
	Point m_mouseLoc;
	Point rabbitLoc;
	Point prevRabbit;
	Robot m_robot;
	int deltax = 0;
	int deltay = 0;
	int rabx = 0;
	int raby = 0;
	SpatialRect rabbit = new SpatialRect (new Rectangle ());
	ArrayList<Rectangle> bells = new ArrayList<Rectangle>();
	double oldLoc;

	public Botz(Dimension d, Point p) {
		m_gameDimension = d;
		m_gameLocation = p;
		prevRabbit = new Point ();
		oldLoc = Double.MAX_VALUE;

	

		try {
			m_robot = new Robot();

		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BufferedImage getCapture() {
		int x, y, width, height;
		x = m_gameLocation.x;
		y = m_gameLocation.y;
		width = m_gameDimension.width;
		height = m_gameDimension.height;

		Rectangle r = new Rectangle(x, y, width, height);

		return (m_robot.createScreenCapture(r));
	}

	public void update() {

		m_currentImage = getCapture();

		filter();

		findAndExecuteMove();
		

		m_drawPanel.setImage(m_currentImage);

		m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(),
				m_currentImage.getHeight());
	}
	
	

	private void findAndExecuteMove() {

		List<SpatialRect> recs = extractFeatures();
		Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();

		for (SpatialRect r : recs) {

			Cell c = new Cell(m_currentImage.getRGB(r.x, r.y, r.width,
					r.height, null, 0, r.width));

			// check for rabbit

			// update rabbit location
			if (c.getCount() > 350) {
				rabbitLoc = new Point((int) r.getCenterX(), (int) r
						.getCenterY());
				deltax = rabbitLoc.x - prevRabbit.x;
				deltay = rabbitLoc.y - prevRabbit.y;
				prevRabbit = rabbitLoc;

			}

			if (r.getWidth() > 21 && r.getWidth() < 25 && r.getHeight() > 17 && r.getHeight() < 22)
			{
				r.setType(SpatialRect.Type.BIRD);

				r.setColor(Color.GREEN);
			}

			// switch color based on estimated object type
			if (rabbitLoc != null && (r.contains(rabbitLoc)) || r.contains(prevRabbit)) {
				r.setType(SpatialRect.Type.RABBIT);
				r.setColor(Color.YELLOW);
				rabbit = r;

			} 

			g2d.setColor(Color.WHITE);
		}



		Point move = findBestMove(recs, rabbitLoc);

		if (move.x + move.y != 0) {

			move.x += m_gameLocation.x;
			move.y += m_gameLocation.y;
			//System.out.println("Moving " + move);
			movePlayer(move);

		}

		for (SpatialRect s: recs)
		{
			s.draw(g2d);
		}

	}


	private SpatialRect findLowest (List<SpatialRect> list)
	{

		Double greatest = Double.MIN_VALUE;
		SpatialRect lowest = null;

		for (SpatialRect s: list)
		{

			if (s.getCenterY() > greatest)
			{
				greatest = s.getCenterY();
				lowest = s;
			}

		}

		return lowest;

	}



	private Point findBestMove(List<SpatialRect> list, Point rabbitLocation) {
		Point p = new Point();
		Graphics2D g2d = (Graphics2D)m_currentImage.getGraphics();

		if (rabbitLocation != null) {

			

			Rectangle zone = new Rectangle (0, rabbitLocation.y - 100, m_currentImage.getWidth() - 1, 240);


			g2d.draw(zone);

			List<SpatialRect> myList = new ArrayList<SpatialRect> ();

			for (SpatialRect s : list)
			{
				if (s.getType() != SpatialRect.Type.RABBIT && (s.intersects(zone) || zone.contains(s)))
				{
					if (s.getType() != SpatialRect.Type.BIRD)
					{
						s.setColor(Color.CYAN);
					}
					myList.add(s);
				}
			}

			SpatialRect lowest = findLowest(myList);

			if (lowest == null)
			{
				g2d.fillRect(0, 0, 100, 100);
			}

			if (lowest != null)
			{
				lowest.setColor(Color.ORANGE);

				double xdistance = Math.abs(rabbitLocation.x - lowest.getCenterX());

				if (xdistance > 140)
				{
					//System.out.println("MADE IT HERE");
					if (lowest.getCenterX() > rabbitLocation.x)
					{
						p.setLocation(m_currentImage.getWidth() - 4, lowest.getCenterY());
					}
					else
					{
						p.setLocation(4, lowest.getCenterY());
					}
				}
				else
				{
					p.setLocation(lowest.getCenterX(),lowest.getCenterY());
				}

				g2d.setColor(Color.MAGENTA);
				g2d.drawLine(p.x, p.y, rabbitLocation.x, rabbitLocation.y);

			}

		}



		return p;


	}


	private List<SpatialRect> extractFeatures() {

		List<SpatialRect> rectList = new ArrayList<SpatialRect>();

		for (int x = 0; x < m_currentImage.getWidth() - 1; x+= 9) {
			for (int y = 0; y < m_currentImage.getHeight() - 1; y+= 9) {

				if (isWhite(m_currentImage.getRGB(x, y))) {
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
							if (newRect.intersects(otherRect))
								skip = true;
						}

						if (!skip && newRect.width > 10 && newRect.height > 10)
							rectList.add(newRect);
					}

				}
			}
		}

		return rectList;

	}

	private Rectangle expandRect(int row, int col) {

		Rectangle r = new Rectangle(col, row, 1, 1);

		boolean[] bA = checkBorders(r);

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

	private void filter() {
		for (int x = 0; x < m_currentImage.getWidth(); x++) {
			for (int y = 0; y < m_currentImage.getHeight(); y++) {
				int rgb = m_currentImage.getRGB(x, y);
				int[] RGB = rgbToRGBArray(rgb);
				if (m_currentImage.getHeight() - y < 30 || RGB[0] < 200
						|| RGB[1] < 200 || RGB[2] < 200)
					m_currentImage.setRGB(x, y, 0);
				else
					m_currentImage.setRGB(x, y, Color.WHITE.getRGB());
			}
		}
	}



	public void delay(int ms) {
		m_robot.delay(ms);
	}

	private Point determineMove(BufferedImage image) {

		int zone_start = image.getHeight() - 1;
		int zone_end = 0;

		Graphics2D g2d = (Graphics2D) image.getGraphics();

		// g2d.drawRect(0, zone_end, image.getWidth(), zone_start - zone_end);

		for (int x = 0; x < image.getWidth(); x++) {

			for (int y = zone_start; y > zone_end; y--) {

				if (image.getRGB(x, y) == Color.WHITE.getRGB()) {

					return (new Point(x, y));
				}
			}
		}

		return null;

	}


	private boolean discardPixel(int c) {
		return (c != Color.WHITE.getRGB());
	}

	private boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

	private Boolean movePlayer(Point move) {

		if (move != null) {
			//System.out.println(move);
			m_robot.mouseMove(move.x, move.y);
			m_previousMove = move;
			// m_robot.mousePress(InputEvent.BUTTON1_MASK);

		} else {
			//System.out.println("NO MOVE");
		}

		return false;
	}

	public void setPanel(DrawPanel panel) {
		m_drawPanel = panel;

	}

}
