package java2.awt;

import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;

import javaawt.Rectangle;

public class GraphicsConfiguration
{
	private GLWindow win;
	private Screen s;
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
				s = upWin.getScreen();
				d = new GraphicsDevice(s);
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
		return new Rectangle(0, 0, s.getWidth(), s.getHeight());
	}

}
