package javaawt.image;

public interface Raster
{

	public void getDataElements(int w, int h, Object pixel);

	public int getNumDataElements();

	public int getTransferType();

	public DataBuffer getDataBuffer();

	public Object getDelegate();

}
