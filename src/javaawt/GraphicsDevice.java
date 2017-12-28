package javaawt;

import com.jogamp.newt.Screen;

public class GraphicsDevice
{
	private Screen screen;

	public GraphicsDevice(Screen screen)
	{
		this.screen = screen;
	}

	public String getIDstring()
	{
		return screen.getFQName();
	}

	public DisplayMode getDisplayMode()
	{		
		return new DisplayMode(screen);
	}

	public void setFullScreenWindow(Object object)
	{
		// TODO Auto-generated method stub
		
	}

	public Object getFullScreenWindow()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isDisplayChangeSupported()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public void setDisplayMode(DisplayMode desiredMode)
	{
		// TODO Auto-generated method stub
		
	}

	public GraphicsConfiguration[] getConfigurations()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public DisplayMode[] getDisplayModes()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isFullScreenSupported()
	{
		// TODO Auto-generated method stub
		return false;
	}

}
