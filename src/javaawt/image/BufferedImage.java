package javaawt.image;

import java.util.Vector;

import javaawt.Graphics;
import javaawt.Graphics2D;
import javaawt.Point;
import javaawt.Rectangle;
import javaawt.image.BufferedImage;
import javaawt.image.ColorModel;
import javaawt.image.ImageObserver;
import javaawt.image.ImageProducer;
import javaawt.image.Raster;
import javaawt.image.RenderedImage;
import javaawt.image.SampleModel;
import javaawt.image.TileObserver;
import javaawt.image.WritableRaster;
import javaawt.image.WritableRenderedImage;

public class BufferedImage extends Image implements WritableRenderedImage
{
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

	public BufferedImage(int i, int j, int typeIntArgb)
	{
		//For use by DDS bufferedImage
	}

	public BufferedImage(ColorModel cm, WritableRaster wRaster, boolean alphaPremultiplied, Object object)
	{
		throw new UnsupportedOperationException();
	}

	public int getTransparency()
	{
		throw new UnsupportedOperationException();
	}

	public void releaseWritableTile(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	public WritableRaster getWritableTile(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	public boolean hasTileWriters()
	{
		throw new UnsupportedOperationException();
	}

	public Point[] getWritableTileIndices()
	{
		throw new UnsupportedOperationException();
	}

	public boolean isTileWritable(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	public void coerceData(boolean isAlphaPremultiplied)
	{
		throw new UnsupportedOperationException();
	}

	public boolean isAlphaPremultiplied()
	{
		throw new UnsupportedOperationException();
	}

	public Graphics2D createGraphics()
	{
		throw new UnsupportedOperationException();
	}

	public Graphics getGraphics()
	{
		throw new UnsupportedOperationException();

	}

	public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		throw new UnsupportedOperationException();

	}

	public void setRGB(int x, int y, int rgb)
	{
		throw new UnsupportedOperationException();

	}

	public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		throw new UnsupportedOperationException();

	}

	public int getRGB(int x, int y)
	{
		throw new UnsupportedOperationException();

	}

	public WritableRaster getAlphaRaster()
	{
		throw new UnsupportedOperationException();

	}

	public Raster getData(Rectangle rect)
	{
		throw new UnsupportedOperationException();

	}

	public Raster getData()
	{
		throw new UnsupportedOperationException();

	}

	public Raster getTile(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();

	}

	public int getTileGridYOffset()
	{
		throw new UnsupportedOperationException();

	}

	public int getTileGridXOffset()
	{
		throw new UnsupportedOperationException();

	}

	public int getTileHeight()
	{
		throw new UnsupportedOperationException();

	}

	public int getTileWidth()
	{
		throw new UnsupportedOperationException();

	}

	public BufferedImage getSubimage(int x, int y, int w, int h)
	{
		throw new UnsupportedOperationException();

	}

	public String[] getPropertyNames()
	{
		throw new UnsupportedOperationException();

	}

	public Object getProperty(String name)
	{
		throw new UnsupportedOperationException();

	}

	public Vector<RenderedImage> getSources()
	{
		throw new UnsupportedOperationException();

	}

	public int getMinTileY()
	{
		throw new UnsupportedOperationException();

	}

	public int getMinTileX()
	{
		throw new UnsupportedOperationException();

	}

	public int getNumYTiles()
	{
		throw new UnsupportedOperationException();

	}

	public int getNumXTiles()
	{
		throw new UnsupportedOperationException();

	}

	public int getMinY()
	{
		throw new UnsupportedOperationException();

	}

	public int getMinX()
	{
		throw new UnsupportedOperationException();

	}

	public SampleModel getSampleModel()
	{
		throw new UnsupportedOperationException();

	}

	public ColorModel getColorModel()
	{
		throw new UnsupportedOperationException();

	}

	public int getHeight()
	{
		throw new UnsupportedOperationException();

	}

	public int getWidth()
	{
		throw new UnsupportedOperationException();

	}

	public int getType()
	{
		throw new UnsupportedOperationException();

	}

	public void setData(Raster r)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void addTileObserver(TileObserver to)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void removeTileObserver(TileObserver to)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public WritableRaster copyData(WritableRaster raster)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHeight(ImageObserver observer)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getProperty(String name, ImageObserver observer)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ImageProducer getSource()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getWidth(ImageObserver observer)
	{
		throw new UnsupportedOperationException();
	}

	public WritableRaster getRaster()
	{
		throw new UnsupportedOperationException();
	}

}
