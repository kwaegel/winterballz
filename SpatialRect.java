import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;


public class SpatialRect extends Rectangle{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2685940766376607493L;

	
	public enum Type
	{
		RABBIT,
		BELL,
		BIRD
	}
	
	private Type m_type;
	private Color m_color;
	
	public SpatialRect (Rectangle r)
	{
		super(r);
		m_type = Type.BELL;
	}
	
	public void setType (Type t)
	{
		m_type = t;
	}
	
	public void setColor (Color c)
	{
		m_color = c;
	}
	
	public Type getType ()
	{
		return (m_type);
	}
	
	public void draw (Graphics2D g2d)
	{
		g2d.setColor(m_color);
		g2d.drawRect(x, y, width, height);
	}
}
