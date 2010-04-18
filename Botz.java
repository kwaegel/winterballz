

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.Timer;


public class Botz  {

	BufferedImage m_currentImage;
	Dimension m_gameDimension;
	DrawPanel m_drawPanel;
	Point m_gameLocation;
	Robot m_robot;

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



	public void playGame ()
	{

		int i  = 0;
		Timer timer = new Timer ();
		while (i < 500)
		{
			Point move;
			
			m_currentImage = getCapture ();

			m_currentImage = processImage (m_currentImage);
			
			
			
			
			move = determineMove (m_currentImage);
		
			
			m_drawPanel.setImage(m_currentImage);
			//m_drawPanel.repaint();
			m_drawPanel.paintImmediately(0, 0, m_currentImage.getWidth(), m_currentImage.getHeight());




			i++;

			movePlayer (move);
		}

		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	private BufferedImage processImage (BufferedImage image)
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
		return image;
	}

	private Point determineMove (BufferedImage image)
	{


		int zone_start = image.getHeight() - 100;
		int zone_end  = (2 * image.getHeight()) / 3;
		
		Graphics2D g2d = (Graphics2D) image.getGraphics();
		g2d.setColor(Color.GREEN);
		
		g2d.drawRect(0, zone_end, image.getWidth(),  zone_start - zone_end);

	
		for (int x = 0; x < image.getWidth(); x++)
		{

			for (int y = zone_start; y > zone_end; y--)
			{

				if (image.getRGB(x, y) == Color.WHITE.getRGB())
				{
					
					image.getGraphics().drawOval(x, y, 20, 20);
					return (new Point (m_gameLocation.x + x, m_gameLocation.y+y));
				}
			}
		}
	
	
		return null;


	}
	
	

	private boolean discardPixel (int c)
	{
		return (c != Color.WHITE.getRGB());
	}

	private Boolean movePlayer (Point move)
	{
		if (move != null)
		{
			m_robot.mouseMove(move.x, move.y);
			m_robot.mousePress(InputEvent.BUTTON1_MASK);
		}

		return false;
	}

	public void setPanel(DrawPanel panel) {
		m_drawPanel = panel;

	}

}
