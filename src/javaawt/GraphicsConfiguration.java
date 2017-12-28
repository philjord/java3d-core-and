package javaawt;

import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MonitorModeListener;
import com.jogamp.newt.opengl.GLWindow;

public class GraphicsConfiguration
{
	private GLWindow win;
	private Screen s;
	private GraphicsDevice d;

	private int width = -1;
	private int height = -1;

	public GraphicsConfiguration(GLWindow win)
	{
		if (win == null)
			throw new IllegalArgumentException("null GLWindow not allowed, use width and height constructor");

		this.win = win;
	}

	public GraphicsConfiguration(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	public GraphicsDevice getDevice()
	{
		if (d == null)
		{
			if (win != null)
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
			else
			{
				// just use the first one, no Vivante GPU no screens reported back!
				//s = Screen.getAllScreens().iterator().next();
				s = new OffScreen(width, height);
				d = new GraphicsDevice(s);
			}
		}
		return d;
	}

	public Rectangle getBounds()
	{
		return new Rectangle(0, 0, s.getWidth(), s.getHeight());
	}

	/**
	 * For off screen impls only get width and get height are needed I believe
	 * @author phil
	 *
	 */
	public static class OffScreen extends Screen
	{
		private int width = -1;
		private int height = -1;

		public OffScreen(int width, int height)
		{
			this.width = width;
			this.height = height;
		}

		@Override
		public int getWidth()
		{
			return width;
		}

		@Override
		public int getHeight()
		{
			return height;
		}
		
		@Override
		public String getFQName()
		{
			return new String("OffScreen fake Screen");
		}

		@Override
		public int hashCode()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void createNative() throws NativeWindowException
		{
			throw new UnsupportedOperationException();			
		}

		@Override
		public void destroy()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isNativeValid()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getReferenceCount()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int addReference() throws NativeWindowException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int removeReference()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public AbstractGraphicsScreen getGraphicsScreen()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getIndex()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getX()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getY()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public RectangleImmutable getViewport()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public RectangleImmutable getViewportInWindowUnits()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Display getDisplay()
		{
			throw new UnsupportedOperationException();
		}

		

		@Override
		public List<MonitorMode> getMonitorModes()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public List<MonitorDevice> getMonitorDevices()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public MonitorDevice getPrimaryMonitor()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void addMonitorModeListener(MonitorModeListener sml)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeMonitorModeListener(MonitorModeListener sml)
		{
			throw new UnsupportedOperationException();
		}

	}

}
