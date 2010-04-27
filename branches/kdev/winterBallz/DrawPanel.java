package winterBallz;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class DrawPanel extends JPanel {

	private static final long serialVersionUID = 5638434499479432884L;

	BufferedImage m_gameImage;
	int m_zone_start;
	int m_zone_end;

	public DrawPanel() {

	}

	@Override
	public void paintComponent(Graphics g) {

		Graphics2D g2d = (Graphics2D) g;

		if (m_gameImage != null) {
			this.setPreferredSize(new Dimension(m_gameImage.getWidth(), m_gameImage.getHeight()));
			g2d.drawImage(m_gameImage, null, 0, 0);

		}

	}

	public void setImage(BufferedImage image) {
		m_gameImage = image;
		m_zone_start = (2 * m_gameImage.getHeight()) / 3;
		m_zone_end = m_gameImage.getHeight() / 2;
	}

}
