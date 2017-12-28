package javaawt.image;

public interface WritableRaster extends Raster
{

	public int[] getDataElements(int i, int j, int width, int height, Object object);

}
