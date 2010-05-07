package winterBallz;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

@SuppressWarnings("serial")
public class Bell extends GameFeature {
	
	public Bell(Rectangle r) {
		super(r);
	}

	private static final Color bellColor = Color.red;

	public void draw(Graphics2D g2d)
	{
		g2d.setColor(bellColor);
		g2d.drawRect(x-1, y-1, width+1, height+1);
	}

}
