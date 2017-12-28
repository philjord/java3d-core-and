package javaawt;

import javaawt.image.ColorModel;
import javaawt.image.Raster;

public interface PaintContext
{
	public void dispose();

	ColorModel getColorModel();

	Raster getRaster(int x, int y, int w, int h);
}
