import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;


public class Botz {
	
	Dimension m_gameDimension;
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

}
