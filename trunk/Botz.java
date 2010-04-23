
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
	Point m_previousMove;
	Point m_mouseLoc;
	Point rabbitLoc;
	Robot m_robot;
	int rabx = 0;
	int raby = 0;
	ArrayList<Rectangle> bells = new ArrayList<Rectangle>();

	public Botz(Dimension d, Point p) {
		m_gameDimension = d;
		m_gameLocation = p;

		// System.out.println("Dimension" + m_gameDimension);
		// System.out.println("Location" + m_gameLocation);

		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();

		GraphicsDevice[] screen = ge.getScreenDevices();

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

		Point move;

		m_currentImage = getCapture();

		// processImage (m_currentImage);

		filter();

		// move = determineMove (m_currentImage);
		// move = getMove (m_currentImage);

		test();

		m_drawPanel.setImage(m_currentImage);

		m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(),
				m_currentImage.getHeight());

		// movePlayer (move);

	}

	private void test() {

		List<SpatialRect> recs = extractFeatures();
		Graphics2D g2d = (Graphics2D) m_currentImage.getGraphics();

		for (SpatialRect r : recs) {

			Cell c = new Cell(m_currentImage.getRGB(r.x, r.y, r.width,
					r.height, null, 0, r.width));

			// System.out.println(c.getCount());

			// System.out.println(r.width - r.height);

			// check for rabbit

			// update rabbit location
			if (c.getCount() > 350) {
				rabbitLoc = new Point((int) r.getCenterX(), (int) r
						.getCenterY());

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

		Point move = findNearest(recs, rabbitLoc);

		if (move.x + move.y != 0) {
			move.x += m_gameLocation.x;
			move.y += m_gameLocation.y;
			System.out.println("Moving " + move);
			movePlayer(move);
		}

	}

	private Point findNearest(List<SpatialRect> list, Point rabbitLocation) {
		Point p = new Point();

		if (rabbitLocation != null) {
			Double distancesq = Double.MAX_VALUE;

			for (SpatialRect r : list) {
				if (r.getType() != SpatialRect.Type.RABBIT) {
					
					Double d = Point.distanceSq(rabbitLocation.getX(),
							rabbitLocation.getY(), r.getCenterX(), r
									.getCenterY());
					
					
					if (d < distancesq &&
						r.getCenterY() - rabbitLocation.y < 160 &&
						rabbitLocation.y < r.getCenterY()) 
					{
					
						
						if (r.getCenterY() > p.y)
						{
							distancesq = d;
							p.setLocation(r.getCenterX(), r.getCenterY());
						}

					}
			}
		}
		}
	
		
	/*	if (list.size() > 2)
		{
			Collections.sort(list, new SpatialCompare ());
		
			p.x = (int)list.get(1).getCenterX();
			p.y = (int)list.get(1).getCenterY();
			
			Double d1 = Point.distanceSq(rabbitLocation.getX(),
					rabbitLocation.getY(), p.x,p.y);
			
			
		
		}*/
		
		return p;

		// Point center = new Point (r.)

	}

	private List<SpatialRect> extractFeatures() {

		List<SpatialRect> rectList = new ArrayList<SpatialRect>();

		for (int x = 0; x < m_currentImage.getWidth() - 1; x++) {
			for (int y = 0; y < m_currentImage.getHeight() - 1; y++) {

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

	private boolean inBounds(Rectangle r) {
		Rectangle imageBounds = new Rectangle(1, 1,
				m_currentImage.getWidth() - 1, m_currentImage.getHeight() - 1);

		return (imageBounds.contains(r));
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

	private void processImage(BufferedImage image) {
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {

				if (discardPixel(image.getRGB(x, y))) {
					image.setRGB(x, y, 0);
				}
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

	private Point getMove(BufferedImage image) {
		Graphics2D g2d = (Graphics2D) image.getGraphics();
		g2d.setColor(Color.BLUE);

		int width = image.getWidth();

		int column_width = 80;
		int row_height = 80;
		int col = width / column_width;
		int row = 4;

		int x = 0;
		int y = image.getHeight() - row * row_height;

		int height = image.getHeight() - y;

		BufferedImage region = image.getSubimage(x, y, width, height);

		boolean flag = false;

		int lowest_x = 0;
		int lowest_y = 0;

		for (int r = 0; r < row * row_height; r += row_height) {
			int row_count = 0;
			int rownum = r / row_height;

			for (int c = 0; c < col * column_width; c += column_width) {

				Cell cell = new Cell(region.getRGB(c, r, column_width,
						row_height, null, 0, column_width));

				int wcount = 0;

				int colnum = c / column_width;

				if (cell.isOccupied(200, 550)) {
					row_count++;

					if (row_count > 1 && rownum != 0) {
						break;
					}

					lowest_x = x + c;
					lowest_y = y + r;

					g2d.setColor(Color.RED);
					g2d.drawRect(x + c, y + r, column_width, row_height);

					flag = true;
					g2d.setColor(Color.BLUE);
				} else {
					g2d.drawRect(x + c, y + r, column_width, row_height);
				}

				g2d.drawString("T: " + cell.getCount(), x + c, y + r);

			}

		}

		g2d.setColor(Color.YELLOW);

		m_mouseLoc = MouseInfo.getPointerInfo().getLocation();

		g2d.fillOval(m_mouseLoc.x - m_gameLocation.x, m_mouseLoc.y
				- m_gameLocation.y, 10, 10);

		Point m = null;

		if (flag) {

			m = determineMove(image.getSubimage(lowest_x, lowest_y,
					column_width, row_height));

			if (m != null) {
				g2d.setColor(Color.CYAN);
				g2d.fillOval(m.x + lowest_x, m.y + lowest_y, 10, 10);
				m.x += lowest_x + m_gameLocation.x + 15;
				m.y += lowest_y + m_gameLocation.y;
			}
		}

		return m;

		// return (new Point (m_gameLocation.x + lowest_x,
		// m_gameLocation.y+lowest_y));

		// g2d.drawRect(x, y, width, height);
	}

	private boolean discardPixel(int c) {
		return (c != Color.WHITE.getRGB());
	}

	private boolean isWhite(int c) {
		return (c == Color.WHITE.getRGB());
	}

	private Boolean movePlayer(Point move) {

		if (move != null) {
			System.out.println(move);
			m_robot.mouseMove(move.x, move.y);
			// m_robot.mousePress(InputEvent.BUTTON1_MASK);

		} else {
			System.out.println("NO MOVE");
		}

		return false;
	}

	public void setPanel(DrawPanel panel) {
		m_drawPanel = panel;

	}

}
