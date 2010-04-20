

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

import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;



public class Botz  {

	BufferedImage m_currentImage;
	Dimension m_gameDimension;
	DrawPanel m_drawPanel;
	Point m_gameLocation;
	Point m_previousMove;
	Robot m_robot;
	int rabx = 0;
	int raby = 0;
	

	public Botz (Dimension d, Point p)
	{
		m_gameDimension = d;
		m_gameLocation = p;

		System.out.println("Dimension" + m_gameDimension);
		System.out.println("Location" + m_gameLocation);

		GraphicsEnvironment ge = GraphicsEnvironment.
		getLocalGraphicsEnvironment();

		GraphicsDevice [] screen = ge.getScreenDevices();


		try {
			m_robot = new Robot (screen[0]);


		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BufferedImage getCapture ()
	{
		int x,y, width,height;
		x = m_gameLocation.x;
		y = m_gameLocation.y;
		width = m_gameDimension.width;
		height = m_gameDimension.height;

		Rectangle r = new Rectangle (x,y,width,height);

		return (m_robot.createScreenCapture(r));
	}



	public void update ()
	{

		Point move;

		m_currentImage = getCapture ();

		//processImage (m_currentImage);

		filter();



		//move = determineMove (m_currentImage);
		move = getMove (m_currentImage);




		m_drawPanel.setImage(m_currentImage);

		m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(), m_currentImage.getHeight());




		//movePlayer (move);


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
			for (int y = 0; y < m_currentImage.getHeight (); y++)
			{
				int rgb = m_currentImage.getRGB(x, y);
				int[] RGB = rgbToRGBArray(rgb);
				if (m_currentImage.getHeight() - y < 80 || RGB[0] < 200 || RGB[1] < 200 || RGB[2] < 200)
					m_currentImage.setRGB(x, y, 0);
				else
					m_currentImage.setRGB(x, y, Color.WHITE.getRGB());
			}
		} 
	}

	private void processImage (BufferedImage image)
	{
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight (); y++)
			{

				if (discardPixel(image.getRGB(x, y)))
				{
					image.setRGB(x, y, 0);
				}
			}
		}

	}

	public void delay (int ms)
	{
		m_robot.delay(ms);
	}

	private Point determineMove (BufferedImage image)
	{


		int zone_start = image.getHeight() - 1;
		int zone_end  = 0;

		Graphics2D g2d = (Graphics2D) image.getGraphics();
		

		//g2d.drawRect(0, zone_end, image.getWidth(),  zone_start - zone_end);


		for (int x = 0; x < image.getWidth(); x++)
		{

			for (int y = zone_start; y > zone_end; y--)
			{

				if (image.getRGB(x, y) == Color.WHITE.getRGB())
				{

					
					
					return (new Point (x, y));
				}
			}
		}


		return null;


	}

	private Point getMove (BufferedImage image)
	{
		Graphics2D g2d = (Graphics2D)image.getGraphics();
		g2d.setColor(Color.BLUE);

		int width = image.getWidth();


		int column_width = 80;
		int row_height = 80;
		int col = width /column_width;
		int row = 4;

		int x = 0;
		int y = image.getHeight() - row*row_height;

		int height = image.getHeight() - y;

		BufferedImage region = image.getSubimage(x, y, width, height);

		boolean flag = false;

		int lowest_x = 0;
		int lowest_y = 0;


		
	

		for (int r = 0; r < row * row_height; r+= row_height)
		{
			int row_count = 0;
			int rownum = r / row_height;
		
			
			for (int c = 0; c < col * column_width; c += column_width)
			{

				Cell cell = new Cell ( region.getRGB(c, r,  column_width,   row_height, null, 0, column_width ));

				int wcount = 0;
				
				int colnum = c / column_width;

				if (cell.isOccupied(200, 550))
				{
					row_count++;
					
					if (row_count > 1 && rownum != 0 )
					{
						break;
					}
					
					lowest_x = x + c;
					lowest_y = y + r;
				
					
					
					g2d.setColor(Color.RED);
					g2d.drawRect(x + c, y + r, column_width, row_height);
				
					flag = true;
					g2d.setColor(Color.BLUE);
				}
				else
				{
					g2d.drawRect(x + c, y + r, column_width, row_height);
				}
				

				
				g2d.drawString("T: " + wcount , x + c, y + r);

			/*	if (cell.getCount() > 500)
				{
					g2d.setColor(Color.YELLOW);
					g2d.drawRect(x + c, y + r, column_width, row_height);
					rabx = x + c;
					raby = y + c;
					g2d.setColor(Color.BLUE);
				

				}*/
				
				
			
			}
			
		
		}
		
		g2d.setColor(Color.YELLOW);
		Point mouse = MouseInfo.getPointerInfo().getLocation();
		g2d.fillOval(mouse.x, mouse.y, 10, 10);
		
		Point m = null;

		if (flag)
		{
		
			m = determineMove (image.getSubimage(lowest_x, lowest_y, column_width, row_height));

		

			if (m != null)
			{
				g2d.setColor(Color.CYAN);
				g2d.fillOval(m.x + lowest_x, m.y + lowest_y, 10, 10);
				m.x += lowest_x + m_gameLocation.x;
				m.y += lowest_y + m_gameLocation.y - 100 ;
			}
		}	
		
		

		return m;

		//return (new Point (m_gameLocation.x + lowest_x, m_gameLocation.y+lowest_y));


		//g2d.drawRect(x, y, width, height);
	}

	private boolean discardPixel (int c)
	{
		return (c != Color.WHITE.getRGB());
	}

	private Boolean movePlayer (Point move)
	{

		if (move != null)
		{
			System.out.println(move);
			m_robot.mouseMove(move.x, move.y);
			//	m_robot.mousePress(InputEvent.BUTTON1_MASK);
			

		}
		else
		{
			//m_robot.delay(200);
		}

		return false;
	}

	public void setPanel(DrawPanel panel) {
		m_drawPanel = panel;

	}

}
