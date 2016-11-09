package javaawt.image;

public interface ImageObserver
{
	public static final int ALLBITS = 1;

	public static final int ABORT = 2;
	
	boolean imageUpdate(Image img, int flags, int x, int y, int w, int h);

}
