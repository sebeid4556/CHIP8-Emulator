package chip8;

public class CHIP8 
{
	
	public static void main(String[] args)
	{		
		
		_Display Display = new _Display();
		_CPU CPU = new _CPU(Display);
		
		//Display.setFPS(Display.FPS_UNLIMITED);
		Display.sd.setRatio(10);
		Display.sd.setRandomizeScreen(false);
		Display.sd.enableGrid(false);		
		
		CPU.loadROM("rom/keypad.ch8");
		
		boolean loop = true;		
		while(loop)
		{
			CPU.cycle();
			Display.draw();
		}
		
	}
}
