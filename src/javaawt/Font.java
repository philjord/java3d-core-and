package javaawt;

import javaawt.geom.Rectangle2D;

public abstract class Font
{
	public abstract Object getDelegate();
	
	public abstract int getSize();
	
	public abstract Rectangle2D getStringBounds(String text);// Note borrow from fontMetrics

	public abstract void setSize(int fontSize);
	
}
