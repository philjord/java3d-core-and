package javaawt;

import javaawt.image.ImageObserver;
import javaawt.image.ImageProducer;

public abstract class Image
{	
	public static final int SCALE_DEFAULT = 1;
	
	public abstract Object getDelegate();

	public abstract void flush();

	public abstract float getAccelerationPriority();

	//public abstract ImageCapabilities getCapabilities(GraphicsConfiguration gc);

	public abstract Graphics getGraphics();

	public abstract int getHeight(ImageObserver observer);

	public abstract Object getProperty(String name, ImageObserver observer);

	public abstract Image getScaledInstance(int width, int height, int hints);

	public abstract ImageProducer getSource();

	public abstract int getWidth(ImageObserver observer);

	public abstract void setAccelerationPriority(float priority);

}
