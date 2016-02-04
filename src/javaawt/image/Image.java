package javaawt.image;

import javaawt.Graphics;
import javaawt.image.ImageObserver;
import javaawt.image.ImageProducer;

public abstract class Image
{
	public void flush()
	{

	}

	public float getAccelerationPriority()
	{
		throw new UnsupportedOperationException();
	}

/*	public ImageCapabilities getCapabilities(GraphicsConfiguration gc)
	{
		throw new UnsupportedOperationException();
	}*/

	public abstract Graphics getGraphics();

	public abstract int getHeight(ImageObserver observer);

	public abstract Object getProperty(String name, ImageObserver observer);

	public Image getScaledInstance(int width, int height, int hints)
	{
		throw new UnsupportedOperationException();
	}

	public abstract ImageProducer getSource();

	public abstract int getWidth(ImageObserver observer);

	public void setAccelerationPriority(float priority)
	{
		throw new UnsupportedOperationException();
	}

}
