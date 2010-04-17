import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class Windowz {

	JFrame m_frame;
	JPanel m_buttonPanel;
	JButton m_calibrateButton;
	
	
	public Windowz ()
	{
		initialize ();
		build ();
		display ();
	}
	
	public void initialize ()
	{
		m_frame = new JFrame ("Winter Ballz");
		m_frame.setSize(400, 200);
		
		m_buttonPanel = new JPanel ();
		
		m_calibrateButton = new JButton ("Calibrate");
		m_calibrateButton.addActionListener( new ActionListener ()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				getScreenCalibration ();
				
			}
			
		});
		
	}
	
	public void build ()
	{
		
		m_frame.setLayout(new BorderLayout ());
		
		m_buttonPanel.add(m_calibrateButton);
		
		m_frame.add(m_buttonPanel, BorderLayout.CENTER);
		
	}
	
	public void display ()
	{
		//m_frame.pack();
		m_frame.setVisible(true);
		
	}
	
	public void getScreenCalibration ()
	{
		System.out.println("ENTER SOMETHING");
	}
}
