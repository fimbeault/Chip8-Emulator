package Chip8;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

public class OpcodesUnitTests extends Chip8 {

	private static final long serialVersionUID = 8936127140309764894L;
	
	private static JFrame resultsWindow;	

	public void start()
	{
		resultsWindow = new JFrame();
		resultsWindow.setSize(800, 600);
		resultsWindow.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		JPanel panel = new JPanel(new BorderLayout());
		
		resultsWindow.setContentPane(panel);
		resultsWindow.setResizable(false);
		resultsWindow.setLocationRelativeTo(null);
		//resultsWindow.setVisible(true);
		
		requestFocus();
		initialize();
		running = true;
	}
	
	protected void initialize()
	{
		super.initialize();
		
		int i = PC;
		memory[i++] = (byte) 0x70;
		memory[i++] = (byte) 0x14;
		memory[i++] = (byte) 0xD0;
		memory[i++] = (byte) 0x08;
		memory[i++] = (byte) 0x00;
		memory[i++] = (byte) 0xE0;
	}
}
