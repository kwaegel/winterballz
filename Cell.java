import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;


public class Cell  {

	private int [] m_rgbArray;
	private int m_count;
	private int m_row;
	private int m_col;
	
	
	public Cell (int [] rgbArray)
	{
		m_rgbArray = rgbArray;
		
		calcCount ();
	}
	
	private void calcCount ()
	{
		m_count = 0;
		for (int i : m_rgbArray)
		{
			if (i == Color.WHITE.getRGB())
			{
				m_count++;
			}

		}
	}
	
	public int getCount ()
	{
		return m_count;
	}
	
	public boolean isOccupied (int low, int high)
	{
		return ( m_count > low && m_count < high );
	}

	public void setCol(int m_col) {
		this.m_col = m_col;
	}

	public int getCol() {
		return m_col;
	}

	public void setRow(int m_row) {
		this.m_row = m_row;
	}

	public int getRow() {
		return m_row;
	}
	

}
