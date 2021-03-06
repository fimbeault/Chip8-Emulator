package Chip8;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Random;

import javax.swing.JFrame;

public class Chip8 extends Canvas implements Runnable, KeyListener {

	private static final long serialVersionUID = 1042099242508261715L;
	protected boolean running = false;
	private static int pixelSize = 8;

	public InputStream game = null;

	private int opcode = 0;
	protected byte[] memory = new byte[4096];
	protected int[] V = new int[16]; // Registers
	protected int I = 0; // Index register
	protected int PC = 0; // Program Counter

	private boolean[] gfx = new boolean[64 * 32];
	private int delayTimer = 0;
	private int soundTimer = 0;

	protected int[] stack = new int[16];
	protected int SP = 0; // Stack Pointer

	private byte[] key = new byte[16];
	private int lastKeyPressed = -1;
	
	private Random rand = new Random();
	
	private JFrame mainWindow = null;

	private int KPS = 0;
	
	private byte[] chip8_fontset =
		{ 
			0xF, 0x9, 0x9, 0x9, 0xF, // 0
			0x2, 0x6, 0x2, 0x2, 0x7, // 1
			0xF, 0x1, 0xF, 0x8, 0xF, // 2
			0xF, 0x1, 0xF, 0x1, 0xF, // 3
			0x9, 0x9, 0xF, 0x1, 0x1, // 4
			0xF, 0x8, 0xF, 0x1, 0xF, // 5
			0xF, 0x8, 0xF, 0x9, 0xF, // 6
			0xF, 0x1, 0x2, 0x4, 0x4, // 7
			0xF, 0x9, 0xF, 0x9, 0xF, // 8
			0xF, 0x9, 0xF, 0x1, 0xF, // 9
			0xF, 0x9, 0xF, 0x9, 0x9, // A
			0xE, 0x9, 0xE, 0x9, 0xE, // B
			0xF, 0x8, 0x8, 0x8, 0xF, // C
			0xE, 0x9, 0x9, 0x9, 0xE, // D
			0xF, 0x8, 0xF, 0x8, 0xF, // E
			0xF, 0x8, 0xF, 0x8, 0x8  // F
		};

	private boolean drawFlag = true;

	public Chip8()
	{
		addKeyListener(this);
	}

	public void start(JFrame window)
	{
		mainWindow = window;
		requestFocus();
		initialize();
		running = true;
	}

	protected void initialize()
	{
		PC = 0x200;
		opcode = 0;
		I = 0;
		SP = 0;

		// Clear everything
		for(int i = 0; i < 4096; i++)
			memory[i] = 0;

		for(int i = 0; i < 16; i++)
			V[i] = 0;

		for(int i = 0; i < 16; i++)
			stack[i] = 0;

		for(int i = 0; i < 16; i++)
			key[i] = 0;

		for(int i = 0; i < 2048; i++)
			gfx[i] = false;

		// Load fontset
		for(int i = 0; i < 80; i++)
			memory[i] = chip8_fontset[i];
	}

	public void loadGame(String filename)
	{
		try {
			File file = new File(filename);
			game = new FileInputStream(file);

			long length = file.length();
			long memoryLength = 4096 - 0x200;

			if(length > memoryLength)
			{
				System.out.println("File too large");
				return;
			}

			int offset = 0x200;
			int numRead = 0;

			while(offset < 4096 && (numRead = game.read(memory, offset, (int)length)) >= 0 )
			{
				offset += numRead;
			}

			if (offset < length)
			{
				System.out.println("Could not read file completely");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (game != null) {
				try {
					game.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run()
	{		
		long lastTime = System.nanoTime();
		double unprocessed = 0;
		double framerate = 600;
		double nsPerTick = 1000000000.0 / framerate;
		
		int IPS = 0;
		
		long ipsTimer = System.currentTimeMillis();

		while(running)
		{
			long now = System.nanoTime();
			unprocessed += (now - lastTime) / nsPerTick;
			lastTime = now;

			for (int i = 0; i < (int)unprocessed; i++)
			{
				EmulateCycle();
				IPS++;
			}

			unprocessed -= (int)unprocessed;

			if(drawFlag == true)
			{
				DrawGraphics();
				drawFlag = false;
			}
			
			if(System.currentTimeMillis() - ipsTimer >= 1000)
			{
				mainWindow.setTitle("IPS : " + IPS + "  |  KPS : " + KPS);
				ipsTimer = System.currentTimeMillis();
				IPS = 0;
				KPS = 0;
			}

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void EmulateCycle()
	{
		opcode = (short) (ByteToShort(memory[PC]) << 8 | ByteToShort(memory[PC + 1]));

		InterpretOpcode(opcode);

		if(delayTimer > 0)
			delayTimer--;

		if(soundTimer > 0)
		{
			soundTimer--;

			if(soundTimer == 0)
			{
				System.out.println("BEEP!!!!!!!!!!");
			}
		}
	}

	private void InterpretOpcode(int opcode)
	{
		System.out.println("Interpreting : " + IntToHexString(opcode));

		int x = 0;
		int y = 0;
		int result = 0;

		switch(opcode & 0xF000)
		{
		case 0x0000:
			switch(opcode & 0x0FFF)
			{
			case 0x00E0: // Clears the screen.
				for(int i = 0; i < 2048; i++)
					gfx[i] = false;
				
				PC += 2;
				drawFlag = true;
				break;

			case 0x00EE: // Returns from a subroutine.
				if(SP > 0)
				{
					PC = stack[SP];
					SP--;
					PC += 2;
				}
				break;

			default : // Calls RCA 1802 program at address NNN.
			}
			break;

		case 0x1000: // 0x1NNN : Jumps to address NNN.
			PC = opcode & 0x0FFF;
			break;

		case 0x2000: // 0x2NNN : Calls subroutine at NNN.
			SP++;
			stack[SP] = PC;
			PC = opcode & 0x0FFF;
			break;

		case 0x3000: // 0x3XNN : Skips the next instruction if VX equals NN.
			if(V[(opcode & 0x0F00) >> 8] == (opcode & 0x00FF))
				PC += 4;

			else
				PC += 2;
			break;

		case 0x4000: // 0x4XNN : Skips the next instruction if VX doesn't equal NN.
			if(V[(opcode & 0x0F00) >> 8] != (opcode & 0x00FF))
				PC += 4;
			
			else
				PC += 2;
			break;

		case 0x5000: // 0x5XY0 : Skips the next instruction if VX equals VY.
			if(V[(opcode & 0x0F00) >> 8] == V[(opcode & 0x00F0) >> 4])
				PC += 4;
			
			else
				PC += 2;
			break;

		case 0x6000: // 0x6XNN : Sets VX to NN.
			V[(opcode & 0x0F00) >> 8] = (opcode & 0x00FF);
			PC += 2;
			break;

		case 0x7000: // 0x7XNN : Adds NN to VX.
			x = (opcode & 0x0F00) >> 8;
			V[x] = 0xFF & (V[x] + (opcode & 0x00FF));
			PC += 2;
			break;

		case 0x8000:
			switch(opcode & 0x000F)
			{
			case 0x0000: // 0x8XY0 : Sets VX to the value of VY.
				V[(opcode & 0x0F00) >> 8] = V[(opcode & 0x00F0) >> 4];
				PC += 2;
				break;

			case 0x0001: // 0x8XY1 : Sets VX to VX or VY.
				V[(opcode & 0x0F00) >> 8] |= V[(opcode & 0x00F0) >> 4];
				PC += 2;
				break;

			case 0x0002: // 0x8XY2 : Sets VX to VX and VY.
				V[(opcode & 0x0F00) >> 8] &= V[(opcode & 0x00F0) >> 4];
				PC += 2;
				break;

			case 0x0003: // 0x8XY3 : Sets VX to VX xor VY.
				V[(opcode & 0x0F00) >> 8] ^= V[(opcode & 0x00F0) >> 4];
				PC += 2;
				break;

			case 0x0004: // 0x8XY4 : Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't.
				x = (opcode & 0x0F00) >> 8;
				y = (opcode & 0x00F0) >> 4;
				
				result = V[x] + V[y];

				if((result & 0x100) != 0)
				{
					V[x] = result - 0x100;
					V[0xF] = 1;
				}

				else
				{
					V[x] = result;
					V[0xF] = 0;
				}

				
				PC += 2;
				break;

			case 0x0005: // 0x8XY5 : VY is subtracted from VX. VF = NOT borrow
				x = (opcode & 0x0F00) >> 8;
				y = (opcode & 0x00F0) >> 4;
				
				if(V[x] > V[y])
					V[0xF] = 1;
				
				else
					V[0xF] = 0;
				
				V[x] = (V[x] - V[y]) & 0xFF;
				
				PC += 2;
				break;

			case 0x0006: // 0x8XY6 : Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
				x = (opcode & 0x0F00) >> 8;

				V[0xF] = (V[x] & 0x1);
				V[x] >>= 1;
				PC += 2;
				break;

			case 0x0007: // 0x8XY7 : Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
				x = (opcode & 0x0F00) >> 8;
				y = (opcode & 0x00F0) >> 4;
				
				if(V[y] > V[x])
					V[0xF] = 1;
				
				else
					V[0xF] = 0;
				
				V[x] = (V[y] - V[x]) & 0xFF;
				
				PC += 2;
				break;

			case 0x000E: // 0x8XYE : Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
				x = (opcode & 0x0F00) >> 8;

				V[0xF] = (V[x] & 0x80);
				V[x] <<= 1;
				PC += 2;
				break;
			}
			break;

		case 0x9000: // 0x9XY0 : Skips the next instruction if VX doesn't equal VY.
			if(V[(opcode & 0x0F00) >> 8] != V[(opcode & 0x00F0) >> 4])
				PC += 4;
			
			else
				PC += 2;
			break;

		case 0xA000: // 0xANNN : Sets I to the address NNN.
			I = opcode & 0x0FFF;
			PC += 2;
			break;

		case 0xB000: // 0xBNNN : Jumps to the address NNN plus V0.
			PC = (opcode & 0x0FFF) + V[0];
			break;

		case 0xC000: // 0xCXNN : Sets VX to a random number and NN.
			V[(opcode& 0x0F00) >> 8] = (byte)(rand.nextInt(255)) & (opcode & 0xFF);
			PC += 2;
			break;

		case 0xD000: // 0xDXYN : (See Documentation) Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels
			x = V[(opcode & 0x0F00) >> 8];
			y = V[(opcode & 0x00F0) >> 4];
			int height = opcode & 0x000F;
			int pixel = 0;

			V[0xF] = 0;
			for(int yLine = 0; yLine < height; yLine++)
			{
				pixel = memory[I + yLine];

				for(int xLine = 0; xLine < 8; xLine++)
				{
					if((pixel & (0x80 >> xLine)) != 0) // Collision detection
					{
						int position = (x + xLine) + (y + yLine) * 64;
						
						if(position < 0 || position > 2047)
							break;
						
						if(gfx[x + y * 64] == true)
							V[0xF] = 1;

						gfx[(x + xLine) + (y + yLine) * 64] ^= true;
					}
				}
			}

			drawFlag = true;
			PC += 2;
			break;

		case 0xE000:
			switch(opcode & 0x00FF)
			{
			case 0x009E: // 0xEX9E : Skips the next instruction if the key stored in VX is pressed.
				KPS++;
				if(key[V[(opcode & 0x0F00) >> 8]] == 1)
					PC += 4;
				
				else
					PC += 2;
				
				//ResetKeys();
				break;

			case 0x00A1: // 0xEXA1 : Skips the next instruction if the key stored in VX isn't pressed.
				if(key[V[(opcode & 0x0F00) >> 8]] == 0)
					PC += 4;
				
				else
					PC += 2;
				
				//ResetKeys();
				break;
			}
			break;

		case 0xF000:
			switch(opcode & 0x00FF)
			{
			case 0x0007: // 0xFX07 : Sets VX to the value of the delay timer.
				V[(opcode & 0x0F00) >> 8] = (byte)delayTimer;
				PC += 2;
				break;

			case 0x000A: // 0xFX0A : A key press is awaited, and then stored in VX.
				if(lastKeyPressed != -1)
				{
					V[(opcode & 0x0F00) >> 8] = lastKeyPressed;
					//ResetKeys();
					PC += 2;
				}
				break;

			case 0x0015: // 0xFX15 : Sets the delay timer to VX.
				delayTimer = V[(opcode & 0x0F00) >> 8];
				PC += 2;
				break;

			case 0x0018: // 0xFX18 : Sets the sound timer to VX.
				soundTimer = V[(opcode & 0x0F00) >> 8];
				PC += 2;
				break;

			case 0x001E: // 0xFX1E : Adds VX to I.
				I = (I + V[(opcode & 0x0F00) >> 8]) & 0xFFF;
				PC += 2;
				break;

			case 0x0029: // 0xFX29 : Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are represented by a 4x5 font.
				x = (opcode & 0x0F00) >> 8;
				I = (V[x] * 5);
				PC += 2;
				break;

			case 0x0033: // 0xFX33 : Stores the Binary-coded decimal representation of VX, with the most significant of three digits at the address in I, the middle digit at I plus 1, and the least significant digit at I plus 2.
				memory[I] = (byte) (V[(opcode & 0x0F00) >> 8] / 100);
				memory[I + 1] = (byte) ((V[(opcode & 0x0F00) >> 8] / 10) % 10);
				memory[I + 2] = (byte) (V[(opcode & 0x0F00) >> 8] % 10);
				PC += 2;
				break;

			case 0x0055: // 0xFX55 : Stores V0 to VX in memory starting at address I.
				x = (opcode & 0x0F00) >> 8;
				
				for(int i = 0; i <= x; i++)
					memory[I++] = (byte) (V[i] & 0x00FF);		
				
				PC += 2;
				break;

			case 0x0065: // 0xFX65 : Fills V0 to VX with values from memory starting at address I.
				x = (opcode & 0x0F00) >> 8;

				for(int i = 0; i <= x; i++)
					V[i] = memory[I++] & 0xFF;

				PC += 2;
				break;
			}
			break;

		default:
			System.out.println("Unrecognized opcode : " + opcode);
		}
	}

	private void DrawGraphics()
	{
		// BufferStrategy creation
		BufferStrategy bs = getBufferStrategy();
		if (bs == null)
		{
			createBufferStrategy(3);
			return;
		}

		// Prepare frame to render
		Graphics g = bs.getDrawGraphics();

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.setColor(Color.WHITE);

		for(int i = 0; i < 2048; i++)
		{
			if(gfx[i] == true)
			{
				g.fillRect((i % 64) * pixelSize, (i / 64) * pixelSize, pixelSize, pixelSize);
			}
		}

		if (bs != null) {
			bs.show();
		}
	}

	private void DrawFontSet()
	{
		int x = 0;
		int y = 0;

		for(int i = 0; i < 16; i++)
		{
			for(int j = 0; j < 5; j++)
			{
				x = ((i % 4) * 5) + (j / 5);
				y = ((i / 4) * 6) + (j % 5);

				for(int b = 0; b < 4; b++)
				{
					if(BigInteger.valueOf(memory[i * 5 + j]).testBit(b))
					{
						gfx[y * 64 + x + (3-b)] = true;
					}
				}
			}
		}
	}

	private short ByteToShort(byte b)
	{
		return (short)(b & 0xff);
	}

	private String IntToHexString(int i)
	{
		return "0x" + String.format("%02X", i & 0x0000FFFF);
	}

	private String ShortToHexString(short s)
	{
		return "0x" + String.format("%02X", s);
	}
	
	private void ResetKeys()
	{
		for(int i = 0; i < 16; i++)
			key[i] = 0;
		
		lastKeyPressed = -1;
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		//KPS++;
		switch(arg0.getKeyChar())
		{
		case '1': key[0] = 1; lastKeyPressed = 0; break;
		case '2': key[1] = 1; lastKeyPressed = 1;  break;
		case '3': key[2] = 1; lastKeyPressed = 2;  break;
		case '4': key[3] = 1; lastKeyPressed = 3;  break;
		case 'q': key[4] = 1; lastKeyPressed = 4;  break;
		case 'w': key[5] = 1; lastKeyPressed = 5;  break;
		case 'e': key[6] = 1; lastKeyPressed = 6;  break;
		case 'r': key[7] = 1; lastKeyPressed = 7;  break;
		case 'a': key[8] = 1; lastKeyPressed = 8;  break;
		case 's': key[9] = 1; lastKeyPressed = 9;  break;
		case 'd': key[10] = 1; lastKeyPressed = 10;  break;
		case 'f': key[11] = 1; lastKeyPressed = 11;  break;
		case 'z': key[12] = 1; lastKeyPressed = 12;  break;
		case 'x': key[13] = 1; lastKeyPressed = 13;  break;
		case 'c': key[14] = 1; lastKeyPressed = 14;  break;
		case 'v': key[15] = 1; lastKeyPressed = 15;  break;
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		
		switch(arg0.getKeyChar())
		{
		case '1': key[0] = 0; break;
		case '2': key[1] = 0; break;
		case '3': key[2] = 0; break;
		case '4': key[3] = 0; break;
		case 'q': key[4] = 0; break;
		case 'w': key[5] = 0; break;
		case 'e': key[6] = 0; break;
		case 'r': key[7] = 0; break;
		case 'a': key[8] = 0; break;
		case 's': key[9] = 0; break;
		case 'd': key[10] = 0; break;
		case 'f': key[11] = 0; break;
		case 'z': key[12] = 0; break;
		case 'x': key[13] = 0; break;
		case 'c': key[14] = 0; break;
		case 'v': key[15] = 0; break;
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}
}
