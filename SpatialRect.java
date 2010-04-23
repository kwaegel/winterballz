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
	
	public SpatialRect (Rectangle r)
	{
		super(r);
		m_type = Type.BELL;
	}
	
	public void setType (Type t)
	{
		m_type = t;
	}
	
	public Type getType ()
	{
		return (m_type);
	}
	
}
