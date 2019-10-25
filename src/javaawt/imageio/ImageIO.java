package javaawt.imageio;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;


import javaawt.image.BufferedImage;

public class ImageIO
{
	private static Class<?> delegateClass = null;

	//Class<?> newClass = Class.forName("nif.niobject.interpolator." + objectType);
	public static void installBufferedImageImpl(Class<?> newDelegateClass)
	{
		delegateClass = newDelegateClass;

	}

	public static BufferedImage read(InputStream in)
	{

		if (delegateClass != null)
		{
			try
			{
				Method m = delegateClass.getDeclaredMethod("read", InputStream.class);
				return (BufferedImage) m.invoke(null, in);
			}
			catch (NoSuchMethodException e)
			{

				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}

		return null;

	}
	
    public static BufferedImage read(URL input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }

        InputStream istream = null;
        try {
            istream = input.openStream();
        } catch (IOException e) {
            throw new IOException("Can't get input stream from URL!", e);
        }
        
        BufferedImage bi;
        try {
            bi = read(istream);            
        } finally {
            istream.close();
        }
        return bi;
    }
}
