import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class DrawPanel extends JPanel {

	BufferedImage m_gameImage;
	
	public DrawPanel ()
	{
		
	}
	
	public void setImage (BufferedImage image)
	{
		m_gameImage = image;
	}
	
	public void paintComponent(Graphics g) {
		
		Graphics2D g2d = (Graphics2D) g;
	    
		if (m_gameImage != null)
		{
			this.setPreferredSize(new Dimension (m_gameImage.getWidth(), m_gameImage.getHeight()));
			g2d.drawImage(m_gameImage, null, 0, 0);
			
		}
		
	  
	  }
	
}
