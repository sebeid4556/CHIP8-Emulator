package chip8;

import java.awt.*;
import javax.swing.JComponent;
import javax.swing.JFrame;

import java.util.Random;

public class ShapeDrawing extends JComponent
{
	public static final int MIN_PIXEL_SIZE = 2;
	public static final int MAX_PIXEL_SIZE = 20;
	public static final int DEFAULT_PIXEL_SIZE = 10;
	public static int pixel_size = DEFAULT_PIXEL_SIZE;
	
	public static Color[] COLORS = {Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.WHITE};
	
	private Random rand;
	private int r;
	private int pixel = 0;
	
	private boolean randomizeScreen = false;
	private static final boolean DEFAULT_GRID = false;
	private boolean grid = DEFAULT_GRID;
	
	public static final int DEFAULT_GRID_WEIGHT = 2;
	public static int GRID_WEIGHT = DEFAULT_GRID_WEIGHT;
	public static int total_width;
	public static int total_height;	
	public static Color GRID_COLOR = Color.GRAY;
	
	private _Display d;
	private JFrame f;
	
	ShapeDrawing(_Display dObj, JFrame fObj)
	{
		d = dObj;	
		f = fObj;
		
		if(!grid)
		{
			GRID_WEIGHT = 0;
		}
		
		rand = new Random();
		total_width = (d.WIDTH * pixel_size) + (GRID_WEIGHT * d.WIDTH) + pixel_size;
		total_height = (d.HEIGHT * pixel_size) + (GRID_WEIGHT * d.HEIGHT) + 35;		
	}
	
	public void setRatio(int size)
	{
		if(size < MIN_PIXEL_SIZE || size > MAX_PIXEL_SIZE) return;		
		pixel_size = size;
		total_width = (d.WIDTH * pixel_size) + (GRID_WEIGHT * d.WIDTH) + pixel_size;
		total_height = (d.HEIGHT * pixel_size) + (GRID_WEIGHT * d.HEIGHT) + 35;		
		f.setSize(total_width, total_height);
	}
	
	public void setRandomizeScreen(boolean f)
	{
		randomizeScreen = f;
		System.out.println("Randomize Screen: " + f);
	}
	
	public void enableGrid(boolean f)
	{
		grid = f;		
		if(GRID_WEIGHT == 0) GRID_WEIGHT = DEFAULT_GRID_WEIGHT;
		System.out.println("Grid: " + f);
	}
	
	public void setGridWeight(int w)
	{
		GRID_WEIGHT = w;
		System.out.println("Grid Weight: " + GRID_WEIGHT);
	}
	
	public void setGridColor(Color c)
	{
		GRID_COLOR = c;
	}
	
	//renders the framebuffer to the screen
	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		
		int xOffset = 0;
		int yOffset = 0;
		
		//draw grid background
		g2.setColor(GRID_COLOR);
		g2.drawRect(0, 0, total_width, total_height);
		g2.fillRect(0, 0, total_width, total_height);
		
		for(int y = 0; y < d.HEIGHT; y++)
		{
			for(int x = 0; x < d.WIDTH; x++)
			{												
				pixel = (y * d.WIDTH) + x; 
				if(d.buffer[pixel] == 0)
				{
					g2.setColor(Color.BLACK);
				}
				else if(d.buffer[pixel] == 1)
				{
					g2.setColor(Color.WHITE);
				}
				
				if(randomizeScreen)
				{
					r = rand.nextInt(5);
					g2.setColor(COLORS[r]);
				}
				g2.drawRect(pixel_size*x + xOffset, pixel_size*y + yOffset, pixel_size, pixel_size);
				g2.fillRect(pixel_size*x + xOffset, pixel_size*y + yOffset, pixel_size, pixel_size);
				
				if(grid) xOffset += GRID_WEIGHT;
			}			
			if(grid)
			{
				xOffset = 0;
				yOffset += GRID_WEIGHT;
			}
		}
	}
}