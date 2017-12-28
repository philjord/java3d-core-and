package javaawt.image;

public interface ComponentColorModel extends ColorModel
{

	public int[] getComponentSize();

	@Override
	public Object getDelegate();

}
