package chip8;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.Random;

public class _CPU 
{		
	public static final int RAM_SIZE = 4096;
	public static final int STACK_SIZE = 32;
	public static final int INTERPRETER_SIZE = 0x200;
	public static final int FONT_SET_SIZE = 0x28;	
	public static final int FONT_SIZE = 0x5;
	public static final int MAX_GAME_SIZE = RAM_SIZE - INTERPRETER_SIZE;
	
	public static final int CODE_START = 0x200;
	
	//type is short in order to simulate unsigned byte
	public static short[] RAM = new short[RAM_SIZE];	//4k of 8-bit values
	public static int[] STACK = new int[STACK_SIZE/2];	//16 sets of  16-bit addresses
	public static short[] FONTS = new short[]{	//each entry is a byte
			0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
			0x20, 0x60, 0x20, 0x20, 0x70, // 1
			0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
			0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
			0x90, 0x90, 0xF0, 0x10, 0x10, // 4
			0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
			0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
			0xF0, 0x10, 0x20, 0x40, 0x40, // 7
			0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
			0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
			0xF0, 0x90, 0xF0, 0x90, 0x90, // A
			0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
			0xF0, 0x80, 0x80, 0x80, 0xF0, // C
			0xE0, 0x90, 0x90, 0x90, 0xE0, // D
			0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
			0xF0, 0x80, 0xF0, 0x80, 0x80  // F
		};
	
	//general purpose register indices for use in REGS[]
	private static final int V0 = 0x0;
	private static final int V1 = 0x1;
	private static final int V2 = 0x2;
	private static final int V3 = 0x3;
	private static final int V4 = 0x4;
	private static final int V5 = 0x5;
	private static final int V6 = 0x6;
	private static final int V7 = 0x7;
	private static final int V8 = 0x8;
	private static final int V9 = 0x9;
	private static final int VA = 0xA;
	private static final int VB = 0xB;
	private static final int VC = 0xC;
	private static final int VD = 0xD;
	private static final int VE = 0xE;
	private static final int VF = 0xF;
	
	
	//register set
	private static int PC;	//16-bit program counter
	private static short SP;	//8-bit stack pointer
	private static int I;	//16-bit address register (only use lower 12 bits)
	public static short[] REGS = new short[0x10];	//8-bit general purpose registers 0x0-0xF
	private static short DT;	//8-bit delay timer
	private static short ST;	//8-bit sound timer
		
	private int next;	//16-bit fetched instruction
	private short x;		//second nibble
	private short y;		//third nibble
	private short n;		//fourth nibble
	private short nn;		//lower byte
	private int nnn;	//lower 12-bits
	
	private int opcode = 0x0;
	private int high;
	private int low;
	
	private static final int NUM_KEYS = 0x10;
	private boolean KEYS[] = new boolean[NUM_KEYS];
	
	private Random rand;
	
	private _Display display;
	
	_CPU(_Display d)
	{
		display = d;
		
		//set up keyboard listener
		display.frame.addKeyListener(
				new KeyAdapter()
				{
					public void keyPressed(KeyEvent e)
					{
						int keyCode = e.getKeyCode();
						if(keyCode == KeyEvent.VK_0) KEYS[0] = true;
						if(keyCode == KeyEvent.VK_1) KEYS[1] = true;
						if(keyCode == KeyEvent.VK_2) KEYS[2] = true;
						if(keyCode == KeyEvent.VK_3) KEYS[3] = true;
						if(keyCode == KeyEvent.VK_4) KEYS[4] = true;
						if(keyCode == KeyEvent.VK_5) KEYS[5] = true;
						if(keyCode == KeyEvent.VK_6) KEYS[6] = true;
						if(keyCode == KeyEvent.VK_7) KEYS[7] = true;
						if(keyCode == KeyEvent.VK_8) KEYS[8] = true;
						if(keyCode == KeyEvent.VK_9) KEYS[9] = true;
						if(keyCode == KeyEvent.VK_A) KEYS[0xA] = true;
						if(keyCode == KeyEvent.VK_B) KEYS[0xB] = true;
						if(keyCode == KeyEvent.VK_C) KEYS[0xC] = true;
						if(keyCode == KeyEvent.VK_D) KEYS[0xD] = true;
						if(keyCode == KeyEvent.VK_E) KEYS[0xE] = true;
						if(keyCode == KeyEvent.VK_F) KEYS[0xF] = true;
					}
					public void keyReleased(KeyEvent e)
					{
						int keyCode = e.getKeyCode();
						if(keyCode == KeyEvent.VK_0) KEYS[0] = false;
						if(keyCode == KeyEvent.VK_1) KEYS[1] = false;
						if(keyCode == KeyEvent.VK_2) KEYS[2] = false;
						if(keyCode == KeyEvent.VK_3) KEYS[3] = false;
						if(keyCode == KeyEvent.VK_4) KEYS[4] = false;
						if(keyCode == KeyEvent.VK_5) KEYS[5] = false;
						if(keyCode == KeyEvent.VK_6) KEYS[6] = false;
						if(keyCode == KeyEvent.VK_7) KEYS[7] = false;
						if(keyCode == KeyEvent.VK_8) KEYS[8] = false;
						if(keyCode == KeyEvent.VK_9) KEYS[9] = false;
						if(keyCode == KeyEvent.VK_A) KEYS[0xA] = false;
						if(keyCode == KeyEvent.VK_B) KEYS[0xB] = false;
						if(keyCode == KeyEvent.VK_C) KEYS[0xC] = false;
						if(keyCode == KeyEvent.VK_D) KEYS[0xD] = false;
						if(keyCode == KeyEvent.VK_E) KEYS[0xE] = false;
						if(keyCode == KeyEvent.VK_F) KEYS[0xF] = false;
					}
				}
		);
				
		rand = new Random();
		
		PC = CODE_START;	
		SP = 0;
		
		DT = 0;
		ST = 0;
		
		//load FONTS into memory at 0x0
		for(int i = 0; i < FONT_SET_SIZE; i++)
		{
			RAM[i] = (short)(FONTS[i] & 0xFF);
		}
	}	
	
	//load ROM into memory
	public void loadROM(String p)
	{
		try
		{
			byte[] buffer = Files.readAllBytes(Paths.get(p));
			System.out.println("ROM size: " + buffer.length + " bytes");
			if(buffer.length > MAX_GAME_SIZE)
			{
				throw new Exception("Error: ROM size is too large.");
			}
			int i;
			int offset;
			for(i = 0;i < buffer.length; i++)
			{
				offset = CODE_START + i;
				RAM[offset] = buffer[i];
			}
		}		
		catch(IOException e)
		{
			e.printStackTrace();
		} 
		catch(Exception e) 
		{			
			e.printStackTrace();
		}		
		
	}
	
	private void _incrementPC()
	{
		PC += 2;
	}
	
	private void _fetchNextInstruction()
	{
		high = 	(RAM[PC] << 8) & 0xFF00;
		low = 	RAM[PC + 1] & 0xFF;
		next = high | low;
	}
	
	private void _parseInstruction()
	{
		opcode = ((next & 0xF000) >> 12);
		x = 	(short)(((next & 0x0F00) >> 8) & 0xF);
		y = 	(short)(((next & 0x00F0) >> 4) & 0xF);
		n = 	(short)((next & 0x000F));
		nn = 	(short)((next & 0x00FF));
		nnn = 	(int)  ((next & 0x0FFF));
	}
	
	private void _doTimers()
	{
		if(DT > 0) DT--;
		if(ST > 0) ST--;
	}
	
	//
	//INSTRUCTION SET
	//
	
	private void _CLS()	//clear screen
	{
		for(int i = 0; i < display.BUFFER_SIZE; i++)
		{
			display.buffer[i] = 0;			
		}
		_incrementPC();
	}
	
	private void _RET()	//return from subroutine
	{
		if(SP > 0) SP--;
		PC = STACK[SP];
		_incrementPC();
	}
	
	private void _JP()	//jump to nnn
	{
		PC = nnn;
	}
	
	private void _CALL()	//call subroutine at nnn
	{
		STACK[SP] = PC;
		if(SP < STACK_SIZE) SP++;
		PC = nnn;				
	}
	
	private void _SE_BYTE()	//skip next instruction if Vx == nn
	{
		if(REGS[x] == nn)_incrementPC();
		_incrementPC();
	}
	
	private void _SNE_BYTE()	//skip next instruction if Vx != nn
	{
		if(REGS[x] != nn) _incrementPC();
		_incrementPC();
	}
	
	private void _SE_REGS()	//skip next instruction if Vx == Vy
	{
		if(REGS[x] == REGS[y]) _incrementPC();
		_incrementPC();
	}
	
	private void _LD_IMM()	//set Vx to nn
	{
		REGS[x] = (short)(nn & 0xFF);
		_incrementPC();
	}
	
	private void _ADD_IMM()	//add nn to Vx and store it in Vx
	{
		REGS[x] = (short)((REGS[x] + nn) & 0xFF);
		_incrementPC();
	}
	
	private void _LD()	//set Vx to Vy
	{
		REGS[x] = (short)(REGS[y] & 0xFF);
		_incrementPC();
	}
	
	private void _OR()	//OR Vx and Vy then store result in Vx
	{
		REGS[x] = (short)((REGS[x] | REGS[y]) & 0xFF);
		REGS[VF] = 0;
		_incrementPC();
	}
	private void _AND()	//AND Vx and Vy then store result in Vx
	{
		REGS[x] = (short)((REGS[x] & REGS[y]) & 0xFF);
		REGS[VF] = 0;
		_incrementPC();
	}
	
	private void _XOR()	//XOR Vx and Vy then store result in Vx
	{
		REGS[x] = (short)((REGS[x] ^ REGS[y]) & 0xFF);
		REGS[VF] = 0;
		_incrementPC();
	}
	
	private void _ADD()	//add Vx and Vy then store the result in Vx
	{		
		int r = (REGS[x] + REGS[y]);
		
		REGS[x] = (short)(r & 0xFF);
		REGS[VF] = 0;
		if(r > 0xFF) REGS[VF] = 1;	//if result is greater than 255(0xFF), set carry		
		_incrementPC();
	}
	
	private void _SUB()	//subtract Vy from Vx then store the result in Vx
	{		
		boolean r = REGS[x] < REGS[y];
		REGS[x] = (short) ((REGS[x] - REGS[y]) & 0xFF);
		REGS[VF] = 1;
		if(r) REGS[VF] = 0;	//if Vx is greater than Vy, set carry	
		_incrementPC();
	}
	
	private void _SHR()	//shift right
	{		
		int r = (REGS[x] & 0x1);		
		REGS[x] = (short)((REGS[y] >> 1) & 0xFF);
		REGS[VF] = (short)(r & 0xFF);		
		_incrementPC();
	}
	
	private void _SUBN()	//subtract Vx from Vy then store the result in Vx
	{
		boolean r = REGS[x] > REGS[y];
		REGS[x] = (short) ((REGS[y] - REGS[x]) & 0xFF);
		REGS[VF] = 1;
		if(r) REGS[VF] = 0;	//if Vx is greater than Vy, set carry	
		_incrementPC();
	}
	
	public void _SHL()	//shift left
	{
		int r = (((REGS[x] & 0x80) >> 7) & 0x1);		
		REGS[x] = (short)((REGS[y] << 1) & 0xFF);
		REGS[VF] = (short)(r & 0xFF);
		_incrementPC();
	}
	
	private void _SNE_REGS()	//skip next instruction if Vx != Vy
	{
		if(REGS[x] != REGS[y])_incrementPC();
		_incrementPC();
	}
	
	private void _LDI()	//set I to nnn
	{
		I = nnn;
		_incrementPC();
	}
	
	private void _JP_ADDR()	//jump to nnn + V0
	{
		PC = (nnn + REGS[V0] & 0xFFF);
	}
	
	private void _RND()	//set Vx to a random number from 0-255 then AND it with nn
	{
		REGS[x] = (short)((rand.nextInt(0x100) & nn) & 0xFF);
		_incrementPC();
	}
	
	private void _DRW()	//Draw a sprite
	{		
		int _x = REGS[x] % display.WIDTH;	//wrap coordinates around
		int _y = REGS[y] % display.HEIGHT;	//
		int _n = n;
		
		int dp = 0;	//current pixel on screen
		int mp = 0;	//current pixel in memory
		int p  = 0;	//position of pixel on screen
		int r  = 0;	//hold result of xor
		
		REGS[VF] = 0;	//set VF to 0
		
		for(int row = 0; row < _n; row++)
		{
			if((_y + row) >= display.HEIGHT || (_y + row) < 0) break;	//stop if reached bottom of screen
			for(int col = 0; col < 8; col++)
			{
				
				if((_x + col) >= display.WIDTH || (_x + col) < 0) break;	//stop if reacher edge of screen
				
				//p = index into the framebuffer array of the current pixel
				p = ((_y + row) * display.WIDTH) + _x + col;
				
				dp = display.buffer[p] & 0xFF;
				
				//1. get the short at (I & 0xFFF) + row
				//2. mask with 0xFF to get lower byte
				//3. shift the value the necessary times to the first bit
				//4. mask it with 0x1 to only get the first bit
				mp = ((((RAM[(I & 0xFFF) + row]) & 0xFF) >> (7 - col)) & 0x1);
				
				r  = (dp ^ mp) & 0xFF;
				if(dp == 1 && mp == 1)	//result of XOR could be 0 if both operands are 0
					if(r == 0) REGS[VF] = 1;	//if the xor'd pixel is 0, then set VF				
				display.buffer[p] = (byte) r;			
			}
		}
		_incrementPC();
		
	}
	
	private void _SKP()	//skip next instruction if key with value of Vx is pressed
	{
		if(KEYS[REGS[x]]) _incrementPC();
		_incrementPC();
	}
	
	private void _SKNP()	//skip next instruction if key with value of Vx is NOT pressed
	{
		if(!KEYS[REGS[x]]) _incrementPC();
		_incrementPC();
	}
	
	private void _LD_FROM_DT()	//Vx = DT
	{
		REGS[x] = (short)(DT & 0xFF);
		_incrementPC();
	}
	
	private void _LDK()	//wait for key press then store the value of the key in Vx
	{
		for(int i = 0; i < NUM_KEYS; i++)
		{
			if(KEYS[i])
			{
				REGS[x] = (short)(i & 0xFF);
				_incrementPC();	//code will loop infinitely unless a key is pressed
				break;
			}
		}
	}
	
	private void _LD_TO_DT()	//set DT to Vx
	{
		DT = (short)(REGS[x] & 0xFF);
		_incrementPC();
	}
	
	private void _LD_TO_ST()	//set ST to Vx
	{
		ST = (short)(REGS[x] & 0xFF);
		_incrementPC();
	}
	
	private void _ADDI()	//I = I + Vx
	{
		if(I + REGS[x] > 0xFFF) REGS[VF] = 1;	//if I + Vx > 0xFFF then set VF (carry)
		I = I + REGS[x];
		_incrementPC();
	}
	
	private void _LDF()	//load address of font with index Vx into I
	{
		I = ((FONT_SIZE * REGS[x]) & 0xFFF);
		_incrementPC();
	}
	
	private void _LD_BCD()
	{
		int r = REGS[x];
		
		RAM[I] = (short)(r / 100);
		RAM[I+1] = (short)((r % 100) / 10);
		RAM[I+2] = (short)(r % 100 % 10 % 10);
		_incrementPC();
	}
	
	private void _LD_WRITE_UNTIL_VX()
	{		
		for(int i = 0; i <= x; i++)
		{
			//RAM[I + i] = (short)(REGS[i] & 0xFF);
			RAM[I] = (short)(REGS[i] & 0xFF);
			I++;
		}				
		_incrementPC();
	}
	
	private void _LD_READ_UNTIL_VX()
	{			
		for(int i = 0; i <= x; i++)
		{			
			//REGS[i] = (short)(RAM[I + i] & 0xFF);
			REGS[i] = (short)(RAM[I] & 0xFF);
			I++;
		}		
		_incrementPC();
	}
	
	//
	//END OF INSTRUCTION SET
	//
	
	public void cycle()	//called in the main loop
	{
		_doTimers();
		_fetchNextInstruction();
		_parseInstruction();		
		switch(opcode)
		{
			case 0x0:
				if		(next == 0x00E0) _CLS(); 
				else if	(next == 0x00EE) _RET();
				break;	
			case 0x1: _JP(); 	break; 
			case 0x2: _CALL(); 	break;
			case 0x3: _SE_BYTE(); break;
			case 0x4: _SNE_BYTE(); break;
			case 0x5: _SE_REGS(); break;
			case 0x6: _LD_IMM(); 	break;
			case 0x7: _ADD_IMM(); 	break;
			case 0x8:
				if		(n == 0x0) _LD();
				else if (n == 0x1) _OR();
				else if (n == 0x2) _AND();
				else if (n == 0x3) _XOR();
				else if (n == 0x4) _ADD();
				else if (n == 0x5) _SUB();
				else if (n == 0x6) _SHR();
				else if (n == 0x7) _SUBN();
				else if (n == 0xE) _SHL();
				break;
			case 0x9: _SNE_REGS(); break;
			case 0xA: _LDI();	break;
			case 0xB: _JP_ADDR(); break;
			case 0xC: _RND(); break;
			case 0xD: _DRW(); 	break;
			case 0xE:
				if		(nn == 0x9E) _SKP();
				else if	(nn == 0xA1) _SKNP();
				break;
			case 0xF:
				if		(nn == 0x07) _LD_FROM_DT();
				else if (nn == 0x0A) _LDK();
				else if (nn == 0x15) _LD_TO_DT();
				else if (nn == 0x18) _LD_TO_ST();
				else if (nn == 0x1E) _ADDI();
				else if (nn == 0x29) _LDF();
				else if (nn == 0x33) _LD_BCD();
				else if (nn == 0x55) _LD_WRITE_UNTIL_VX();
				else if (nn == 0x65) _LD_READ_UNTIL_VX();
				break;
		}
	}
}
