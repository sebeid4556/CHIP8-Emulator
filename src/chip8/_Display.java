package chip8;

import javax.swing.JFrame;

public class _Display
{
	//in pixels, or bits
	public static final int WIDTH  = 64;
	public static final int HEIGHT = 32;
	
	public static final int BUFFER_SIZE = WIDTH * HEIGHT;
	public byte[] buffer = new byte[BUFFER_SIZE];
	
	public static final int FPS_UNLIMITED = -1;
	private static int FPS = 60;
	private long INTERVAL = 1000 / FPS; 
	private long before = System.currentTimeMillis();
	private long now = 0;
	
	public JFrame frame;
	public ShapeDrawing sd; 
	
	_Display()
	{		
		//zero out framebuffer
		for(int i = 0; i < BUFFER_SIZE; i++)
		{
			buffer[i] = 0;
		}		
		
		frame = new JFrame("CHIP-8 Emulator");
		frame.setSize(sd.total_width, sd.total_height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        
        sd = new ShapeDrawing(this, frame);
		System.out.println("Size: " + sd.total_width + "x" + sd.total_height);
        
        frame.getContentPane().add(sd);
        frame.setVisible(true);        
        
        System.out.println("FPS: " + FPS);
    }	
	
	public void setFPS(int f)
	{
		if(f > -1)
		{
			FPS = f;
		}
	}
		
	public void waitFrame()
	{
		if(FPS == FPS_UNLIMITED) return;
		now = System.currentTimeMillis();
		if((now - before) < INTERVAL)
		{
			try
			{
				Thread.sleep(INTERVAL - (now - before));
			}catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}	
		before = now;
	}
	
	private void _printBuffer()
	{
		
		String s = "";
		for(int i = 0; i < HEIGHT; i++)
		{
			s += "\n[";
			for(int j = 0; j < WIDTH; j++)
			{
				switch(buffer[(i * HEIGHT) + j])
				{
					case 0:
						s += " ";
						break;
					case 1:
						s += "#";
						break;
				}				
			}
			s += "]";			
		}
		System.out.println(s);
	}
	
	public void draw()
	{			
		//_printBuffer();
		waitFrame();
		frame.repaint();
	}
}