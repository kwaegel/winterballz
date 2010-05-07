package winterBallz;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

@SuppressWarnings("serial")
public class Rabbit extends GameFeature {
	
	public Rabbit(Rectangle r) {
		super(r);
	}

	private static final Color rabbitColor = Color.yellow;

	public void draw(Graphics2D g2d)
	{
		g2d.setColor(rabbitColor);
		g2d.drawRect(x-1, y-1, width+1, height+1);
	}

}
