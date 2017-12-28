package javaawt.image;

import javaawt.color.ColorSpace;

public interface ColorModel
{
	public Object getDelegate();

	public boolean isAlphaPremultiplied();

	public int getRed(Object pixel);

	public int getGreen(Object pixel);

	public int getBlue(Object pixel);

	public int getAlpha(Object pixel);

	public ColorSpace getColorSpace();

}
