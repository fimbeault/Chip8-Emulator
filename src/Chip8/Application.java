package Chip8;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

public class Application {

	public static JFrame mainWindow;	
	private static int pixelSize = 8;
	
	private static Chip8 emulator = null;
	
	public static void main(String[] args) throws InterruptedException
	{
		emulator = new Chip8();
		emulator.setSize(64 * pixelSize, 32 * pixelSize);
		mainWindow = new JFrame();
		mainWindow.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(emulator);
		
		mainWindow.setContentPane(panel);
		mainWindow.pack();
		mainWindow.setResizable(false);
		mainWindow.setLocationRelativeTo(null);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainWindow.setVisible(true);
		start();
	}
	
	public static void start()
	{
		Thread thread = new Thread(emulator);
        thread.setPriority(Thread.MAX_PRIORITY);
        
        emulator.start(mainWindow);
        emulator.loadGame("Games/INVADERS");
        
        thread.start();
	}
}
