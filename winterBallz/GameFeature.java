package winterBallz;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

@SuppressWarnings("serial")
public abstract class GameFeature extends Rectangle {

	protected Color oldDrawColor;
	public boolean updated = true;
	
	public GameFeature(Rectangle r){
		super(r);
	}

	public final Point getCenter()
	{
		return new Point((int) this.getCenterX(), (int) this.getCenterY());
	}
	
	public void move(Rectangle r)
	{
		this.x = r.x;
		this.y = r.y;
		this.width = r.width;
		this.height = r.height;
		
		updated = true;
	}
	
	public abstract void draw(Graphics2D g2d);
	                                 
}
