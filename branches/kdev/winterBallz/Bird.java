package winterBallz;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

@SuppressWarnings("serial")
public class Bird extends GameFeature {
	
	public Bird(Rectangle r) {
		super(r);
	}

	private static final Color birdColor = Color.cyan;

	public void draw(Graphics2D g2d)
	{
		g2d.setColor(birdColor);
		g2d.drawRect(x-1, y-1, width+1, height+1);
	}

}
