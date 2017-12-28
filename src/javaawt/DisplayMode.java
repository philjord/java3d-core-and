package javaawt;

import com.jogamp.newt.Screen;

public class DisplayMode
{
	private Screen screen;

	public DisplayMode(Screen screen)
	{
		this.screen = screen;
	}

	
	public int getWidth()
	{
		return screen.getWidth();
	}

	public int getHeight()
	{
		return screen.getHeight();
	}

	
	public DisplayMode(int parseInt, int parseInt2, int parseInt3, int parseInt4)
	{
		// TODO Auto-generated constructor stub
	}

	
	public int getRefreshRate()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int getBitDepth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
