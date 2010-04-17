import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class Windowz {

	private JFrame m_frame;
	private JFrame m_sizeFrame;
	private JPanel m_buttonPanel;
	private DrawPanel m_drawPanel;
	private JButton m_calibrateButton;
	private JButton m_beginButton;
	private Dimension m_gameDimension;
	private Point m_location;
	
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
		m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		m_buttonPanel = new JPanel ();
		m_drawPanel = new DrawPanel ();
		m_drawPanel.setPreferredSize(new Dimension(500, 500));
		
		m_calibrateButton = new JButton ("Calibrate");
		m_calibrateButton.addActionListener( new ActionListener ()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				getScreenCalibration ();
				
				
			}
			
		});
		
		m_beginButton = new JButton ("Begin");
		m_beginButton.addActionListener( new ActionListener ()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				startRobot ();
				
			}
			
		});
		
	}
	
	private void startRobot ()
	{
		Botz bot = new Botz (m_gameDimension, m_location);
		m_drawPanel.setImage(bot.getCapture());
		m_drawPanel.validate();
		
	}
	
	private void build ()
	{
		
		m_frame.setLayout(new BorderLayout ());
		
		m_buttonPanel.add(m_calibrateButton);
		m_buttonPanel.add(m_beginButton);
		
		m_frame.add(m_buttonPanel, BorderLayout.CENTER);
		m_frame.add(m_drawPanel,BorderLayout.SOUTH);
		
	}
	
	private void display ()
	{
		m_frame.pack();
		m_frame.setVisible(true);
		
	}
	
	private void getScreenCalibration ()
	{
		System.out.println("ENTER SOMETHING");
		
		m_sizeFrame = new JFrame ("SIZE ME CORRECTLY !");
		m_sizeFrame.setSize(500, 500);
		
		JPanel bPanel = new JPanel ();
		
		JButton finished = new JButton ("Finished");
		finished.addActionListener( new ActionListener ()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				
				m_gameDimension = m_sizeFrame.getSize();
				m_location = m_sizeFrame.getLocationOnScreen();
				m_sizeFrame.setVisible(false);
				System.out.println(m_gameDimension);
			}
			
		});
		
		bPanel.add(finished);
		
		m_sizeFrame.setLayout(new BorderLayout ());
		m_sizeFrame.add(bPanel,BorderLayout.CENTER);
		m_sizeFrame.setVisible(true);
		
	}
}
