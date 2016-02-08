package java2.awt;

import java.util.List;

import com.jogamp.newt.MonitorDevice;
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
			MonitorDevice mm = upWin.getMainMonitor();
			List<MonitorDevice> mds = mm.getScreen().getMonitorDevices();
			GraphicsDevice[] ret = new GraphicsDevice[mds.size()];
			int i = 0;
			for (MonitorDevice md : mds)
				ret[i++] = new GraphicsDevice(md);
			return ret;
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
			MonitorDevice mm = upWin.getMainMonitor();
			return new GraphicsDevice(mm.getScreen().getPrimaryMonitor());
		}
		else
		{
			throw new RuntimeException("getUpstreamWidget not instanceof Window!");
		}

	}

}
