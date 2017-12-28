package javaawt.image;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import javaawt.Graphics;
import javaawt.Graphics2D;
import javaawt.Image;
import javaawt.Point;
import javaawt.Rectangle;
import javaawt.Transparency;

public class BufferedImage extends Image implements WritableRenderedImage, Transparency
{
	/**
	 * SO think I want a 3 way split of this bad boy
	 * On desktop for boring buffered Images extend with BufferedImage itself (make a wrapper)
	 * 
	 * On android for boring buffered images go this way
	 * ((ImageView)view).setImageBitmap(BitmapFactory.decodeFile("/data/data/com.myapp/files/someimage.jpg"));
	 * and for graphics2d https://developer.android.com/reference/android/graphics/Canvas.html
	 * 
	 * For compressed do as I am doing now by extending and just using the smallest parts as need now
	 * 
	 * 
	 * 
	 * https://github.com/trashcutter/AnkiStats/tree/master/app/src/main/java/com/wildplot/android/rendering/graphics/wrapper
	 */
	public static final int TYPE_CUSTOM = 0;

	public static final int TYPE_INT_RGB = 1;

	public static final int TYPE_INT_ARGB = 2;

	public static final int TYPE_INT_ARGB_PRE = 3;

	public static final int TYPE_INT_BGR = 4;

	public static final int TYPE_3BYTE_BGR = 5;

	public static final int TYPE_4BYTE_ABGR = 6;

	public static final int TYPE_4BYTE_ABGR_PRE = 7;

	public static final int TYPE_USHORT_565_RGB = 8;

	public static final int TYPE_USHORT_555_RGB = 9;

	public static final int TYPE_BYTE_GRAY = 10;

	public static final int TYPE_USHORT_GRAY = 11;

	public static final int TYPE_BYTE_BINARY = 12;

	public static final int TYPE_BYTE_INDEXED = 13;

 

	private static Class<?> delegateClass = null;
	private BufferedImage delegate = null;

	//Class<?> newClass = Class.forName("nif.niobject.interpolator." + objectType);
	public static void installBufferedImageDelegate(Class<?> newDelegateClass)
	{
		delegateClass = newDelegateClass;
	}

	protected BufferedImage()
	{
		//For use by DDS bufferedImage, and any sub class that doesn't need the delegate system
	}
	 
	
	public BufferedImage(int i, int j, int typeIntArgb)
	{
		if (delegateClass != null)
		{
		//	Constructor<?>[] consss = delegateClass.getConstructors();
			Constructor<?> cons = delegateClass.getConstructors()[0];
			try
			{
				Object obj = cons.newInstance(new Integer(i), new Integer(j), new Integer(typeIntArgb));
				delegate = (BufferedImage) obj;
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}

		}

	}

	@Override
	public Object getDelegate()
	{
		//just trust it, delegate of desktopbi return bi
		if (delegate != null)
			return delegate.getDelegate();
		else
			throw new UnsupportedOperationException();
	}

	public BufferedImage(ColorModel cm, WritableRaster wRaster, boolean alphaPremultiplied, Object object)
	{
		throw new UnsupportedOperationException();
	}

	public int getTransparency()
	{
		if (delegate != null)
			return delegate.getTransparency();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public void releaseWritableTile(int tileX, int tileY)
	{
		if (delegate != null)
			delegate.releaseWritableTile(tileX, tileY);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public WritableRaster getWritableTile(int tileX, int tileY)
	{
		if (delegate != null)
			return delegate.getWritableTile(tileX, tileY);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasTileWriters()
	{
		if (delegate != null)
			return delegate.hasTileWriters();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public Point[] getWritableTileIndices()
	{
		if (delegate != null)
			return delegate.getWritableTileIndices();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTileWritable(int tileX, int tileY)
	{
		if (delegate != null)
			return delegate.isTileWritable(tileX, tileY);
		else
			throw new UnsupportedOperationException();
	}

	public void coerceData(boolean isAlphaPremultiplied)
	{
		if (delegate != null)
			delegate.coerceData(isAlphaPremultiplied);
		else
			throw new UnsupportedOperationException();
	}

	public boolean isAlphaPremultiplied()
	{
		if (delegate != null)
			return delegate.isAlphaPremultiplied();
		else
			throw new UnsupportedOperationException();
	}

	public Graphics2D createGraphics()
	{
		if (delegate != null)
			return delegate.createGraphics();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public Graphics getGraphics()
	{
		if (delegate != null)
			return delegate.getGraphics();
		else
			throw new UnsupportedOperationException();

	}

	public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		if (delegate != null)
			delegate.setRGB(startX, startY, w, h, rgbArray, offset, scansize);
		else
			throw new UnsupportedOperationException();

	}

	public void setRGB(int x, int y, int rgb)
	{
		if (delegate != null)
			delegate.setRGB(x, y, rgb);
		else
			throw new UnsupportedOperationException();

	}

	public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		if (delegate != null)
			return delegate.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
		else
			throw new UnsupportedOperationException();

	}

	public int getRGB(int x, int y)
	{
		if (delegate != null)
			return delegate.getRGB(x, y);
		else
			throw new UnsupportedOperationException();

	}

	public WritableRaster getAlphaRaster()
	{
		if (delegate != null)
			return delegate.getAlphaRaster();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public Raster getData(Rectangle rect)
	{
		if (delegate != null)
			return delegate.getData(rect);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public Raster getData()
	{
		if (delegate != null)
			return delegate.getData();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public Raster getTile(int tileX, int tileY)
	{
		if (delegate != null)
			return delegate.getTile(tileX, tileY);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getTileGridYOffset()
	{
		if (delegate != null)
			return delegate.getTileGridYOffset();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getTileGridXOffset()
	{
		if (delegate != null)
			return delegate.getTileGridXOffset();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getTileHeight()
	{
		if (delegate != null)
			return delegate.getTileHeight();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getTileWidth()
	{
		if (delegate != null)
			return delegate.getTileWidth();
		else
			throw new UnsupportedOperationException();

	}

	public BufferedImage getSubimage(int x, int y, int w, int h)
	{
		if (delegate != null)
			return delegate.getSubimage(x, y, w, h);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public String[] getPropertyNames()
	{
		if (delegate != null)
			return delegate.getPropertyNames();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public Object getProperty(String name)
	{
		if (delegate != null)
			return delegate.getProperty(name);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public Vector<RenderedImage> getSources()
	{
		if (delegate != null)
			return delegate.getSources();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getMinTileY()
	{
		if (delegate != null)
			return delegate.getMinTileY();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getMinTileX()
	{
		if (delegate != null)
			return delegate.getMinTileX();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getNumYTiles()
	{
		if (delegate != null)
			return delegate.getNumYTiles();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getNumXTiles()
	{
		if (delegate != null)
			return delegate.getNumXTiles();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getMinY()
	{
		if (delegate != null)
			return delegate.getMinY();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getMinX()
	{
		if (delegate != null)
			return delegate.getMinX();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public SampleModel getSampleModel()
	{
		if (delegate != null)
			return delegate.getSampleModel();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public ColorModel getColorModel()
	{
		if (delegate != null)
			return delegate.getColorModel();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getHeight()
	{
		if (delegate != null)
			return delegate.getHeight();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public int getWidth()
	{
		if (delegate != null)
			return delegate.getWidth();
		else
			throw new UnsupportedOperationException();

	}

	public int getType()
	{
		if (delegate != null)
			return delegate.getType();
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public void setData(Raster r)
	{
		if (delegate != null)
			delegate.setData(r);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public void addTileObserver(TileObserver to)
	{
		if (delegate != null)
			delegate.addTileObserver(to);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public void removeTileObserver(TileObserver to)
	{
		if (delegate != null)
			delegate.removeTileObserver(to);
		else
			throw new UnsupportedOperationException();

	}

	@Override
	public WritableRaster copyData(WritableRaster raster)
	{
		if (delegate != null)
			return delegate.copyData(raster);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public int getHeight(ImageObserver observer)
	{
		if (delegate != null)
			return delegate.getHeight(observer);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public Object getProperty(String name, ImageObserver observer)
	{
		if (delegate != null)
			return delegate.getProperty(name, observer);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public ImageProducer getSource()
	{
		if (delegate != null)
			return delegate.getSource();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public int getWidth(ImageObserver observer)
	{
		if (delegate != null)
			return delegate.getWidth(observer);
		else
			throw new UnsupportedOperationException();
	}

	public WritableRaster getRaster()
	{
		if (delegate != null)
			return delegate.getRaster();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public void flush()
	{
		if (delegate != null)
			delegate.flush();

		//else nothing
	}

	@Override
	public float getAccelerationPriority()
	{
		if (delegate != null)
			return delegate.getAccelerationPriority();
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public Image getScaledInstance(int width, int height, int hints)
	{
		if (delegate != null)
			return delegate.getScaledInstance(width, height, hints);
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public void setAccelerationPriority(float priority)
	{
		if (delegate != null)
			delegate.setAccelerationPriority(priority);
		else
			throw new UnsupportedOperationException();
	}

	/*public ImageCapabilities getCapabilities(GraphicsConfiguration gc)
	{
		throw new UnsupportedOperationException();
	}*/

}
