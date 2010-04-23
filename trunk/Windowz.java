

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class Windowz extends JFrame implements KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2224732343525761156L;

	private JFrame m_sizeFrame;
	private JPanel m_buttonPanel;
	private DrawPanel m_drawPanel;
	private JButton m_calibrateButton;
	private JButton m_beginButton;
	private Dimension m_gameDimension;
	private Point m_location;
	
	public Windowz ()
	{
		super("Winter Ballz");
		initialize ();
		build ();
		display ();
	}
	
	public void initialize ()
	{
		
		this.setPreferredSize(new Dimension (800, 600));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addKeyListener(this);
	
		
		m_buttonPanel = new JPanel ();
		m_buttonPanel.setFocusable(false);
		
		m_drawPanel = new DrawPanel ();
		
		
		m_drawPanel.setPreferredSize(new Dimension(500, 500));
		
		m_calibrateButton = new JButton ("Calibrate");
		m_calibrateButton.setFocusable(false);
		m_calibrateButton.addActionListener( new ActionListener ()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				
				Runnable r = new Runnable ()
				{

					public void run ()
					{
						getScreenCalibration ();
					}
				};

				new Thread (r).start ();
				
				
				
				
				
			}
			
		});
		
		m_beginButton = new JButton ("Begin");
		m_beginButton.setFocusable(false);
		m_beginButton.addActionListener( new ActionListener ()
		{

			@Override
			public void actionPerformed(ActionEvent e) {
				Runnable r = new Runnable ()
				{

					public void run ()
					{
						startRobot ();
					}
				};

				new Thread (r).start ();
				
				
				
			}
			
		});
		
	}
	
	private void startRobot ()
	{
		Botz bot = new Botz (m_gameDimension, m_location);
		bot.setPanel(m_drawPanel);
		
		while (true)
		{
			bot.update();
			//bot.delay(100);
		}
	}
	
	private void build ()
	{
		
		this.setLayout(new BorderLayout ());
		
		m_buttonPanel.add(m_calibrateButton);
		m_buttonPanel.add(m_beginButton);
		
		this.add(m_buttonPanel, BorderLayout.CENTER);
		this.add(m_drawPanel,BorderLayout.SOUTH);
		
	}
	
	private void display ()
	{
		this.pack();
		this.setVisible(true);
		
	}
	
	
	
	private void getScreenCalibration ()
	{
		
		
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
				//System.out.println(m_gameDimension);
			}
			
		});
		
		bPanel.add(finished);
		
		m_sizeFrame.setLayout(new BorderLayout ());
		m_sizeFrame.add(bPanel,BorderLayout.CENTER);
		m_sizeFrame.setVisible(true);
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			System.exit(0);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
		
	}
}
