package javaawt;

import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

public class GraphicsEnvironment
{

	private GLWindow win;

	public GraphicsEnvironment(GLWindow win)
	{
		this.win = win;
	}

	public static GraphicsEnvironment getLocalGraphicsEnvironment(GLWindow win)
	{
		return new GraphicsEnvironment(win);
	}

	public GraphicsDevice[] getScreenDevices()
	{
		final Object upObj = win.getUpstreamWidget();

		if (upObj instanceof Window)
		{
			Window upWin = (Window) upObj;
			return new GraphicsDevice[] { new GraphicsDevice(upWin.getScreen()) };
		}
		else
		{
			throw new RuntimeException("getUpstreamWidget not instanceof Window!");
		}

	}

	public static GraphicsEnvironment getLocalGraphicsEnvironment()
	{
		final GLProfile pro = GLProfile.get(GLProfile.GL2GL3);
		final GLCapabilities cap = new GLCapabilities(pro);
		return getLocalGraphicsEnvironment(GLWindow.create(cap));
	}

	public GraphicsDevice getDefaultScreenDevice()
	{
		final Object upObj = win.getUpstreamWidget();

		if (upObj instanceof Window)
		{
			Window upWin = (Window) upObj;
			return new GraphicsDevice(upWin.getScreen());
		}
		else
		{
			throw new RuntimeException("getUpstreamWidget not instanceof Window!");
		}

	}

	public static boolean isHeadless()
	{
		return false;
	}

}
