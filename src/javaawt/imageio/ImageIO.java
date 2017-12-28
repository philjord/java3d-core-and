package javaawt.imageio;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

}
