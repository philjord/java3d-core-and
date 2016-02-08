package java2.awt;

import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;

import java2.awt.GraphicsDevice;
import javaawt.Rectangle;

public class GraphicsConfiguration
{
	private GLWindow win;
	private MonitorDevice md;
	private GraphicsDevice d;

	public GraphicsConfiguration(GLWindow win)
	{
		this.win = win;

	}

	public GraphicsDevice getDevice()
	{
		if (d == null)
		{
			final Object upObj = win.getUpstreamWidget();

			if (upObj instanceof Window)
			{
				Window upWin = (Window) upObj;
				md = upWin.getMainMonitor();
				d = new GraphicsDevice(md);
			}
			else
			{
				throw new RuntimeException("getUpstreamWidget not instanceof Window!");
			}
		}
		return d;
	}

	public Rectangle getBounds()
	{
		return new Rectangle(0, 0, md.getScreen().getWidth(), md.getScreen().getHeight());
	}

}
