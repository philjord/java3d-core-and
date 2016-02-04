package javaawt.image;

import java.util.Vector;

import javaawt.Rectangle;
import javaawt.image.ColorModel;
import javaawt.image.Raster;
import javaawt.image.RenderedImage;
import javaawt.image.SampleModel;
import javaawt.image.WritableRaster;

 

public interface RenderedImage
{
	public WritableRaster copyData(WritableRaster raster);

	public ColorModel getColorModel();

	public Raster getData();

	public Raster getData(Rectangle rect);

	public int getHeight();

	public int getMinTileX();

	public int getMinTileY();

	public int getMinX();

	public int getMinY();

	public int getNumXTiles();

	public int getNumYTiles();

	public Object getProperty(String name);

	public String[] getPropertyNames();

	public SampleModel getSampleModel();

	public Vector<RenderedImage> getSources();

	public Raster getTile(int tileX, int tileY);

	public int getTileGridXOffset();

	public int getTileGridYOffset();

	public int getTileHeight();

	public int getTileWidth();

	public int 	getWidth();

}
