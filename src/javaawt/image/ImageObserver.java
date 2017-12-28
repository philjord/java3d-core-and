package javaawt.image;

import javaawt.Image;

public interface ImageObserver
{
	public static final int ALLBITS = 1;

	public static final int ABORT = 2;

	public static final int ERROR = 3;
	
	boolean imageUpdate(Image img, int flags, int x, int y, int w, int h);
	
	public Object getDelegate();

}
