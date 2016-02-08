package java2.awt;

import com.jogamp.newt.MonitorDevice;

import java2.awt.DisplayMode;
import java2.awt.GraphicsConfiguration;

public class GraphicsDevice
{
	private MonitorDevice monitorDevice;

	public GraphicsDevice(MonitorDevice monitorDevice)
	{
		this.monitorDevice = monitorDevice;
	}

	public String getIDstring()
	{
		return monitorDevice.getScreen().getFQName();
	}

	public DisplayMode getDisplayMode()
	{		
		return new DisplayMode(monitorDevice.getScreen());
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
