package javaawt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class EventQueue
{
	private static Class<?> delegateClass = null;

	public static void installEventQueueImpl(Class<?> newDelegateClass)
	{
		delegateClass = newDelegateClass;

	}

	public static void invokeAndWait(Runnable runnable) throws InvocationTargetException, InterruptedException
	{
		if (delegateClass != null)
		{
			try
			{
				Method m = delegateClass.getDeclaredMethod("invokeAndWait", Runnable.class);
				m.invoke(null, runnable);
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
				if(e.getCause() !=null)
					e.getCause().printStackTrace();
			}
		}
	}

	public static boolean isDispatchThread()
	{
		if (delegateClass != null)
		{
			try
			{
				Method m = delegateClass.getDeclaredMethod("isDispatchThread");
				return (Boolean) m.invoke(null);
			}
			catch (NoSuchMethodException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void invokeLater(Runnable runnable)
	{
		if (delegateClass != null)
		{
			try
			{
				Method m = delegateClass.getDeclaredMethod("invokeLater", Runnable.class);
				m.invoke(null, runnable);
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
				if(e.getCause() !=null)
					e.getCause().printStackTrace();
			}
		}
	}
}
