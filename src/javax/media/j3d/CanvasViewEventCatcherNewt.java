package javax.media.j3d;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;

import javaawt.Dimension;

/**
 * The CanvasViewEventCatcher class is used to track events on a Canvas3D that
 * may cause view matries to change.
 *
 */
class CanvasViewEventCatcherNewt implements WindowListener
{
	// The canvas associated with this event catcher
	private Canvas3D canvas;

	CanvasViewEventCatcherNewt(Canvas3D c)
	{
		canvas = c;
	}

	@Override
	public void windowResized(WindowEvent e)
	{
	synchronized (canvas)
		{
			synchronized (canvas.dirtyMaskLock)
			{
				canvas.cvDirtyMask[0] |= Canvas3D.MOVED_OR_RESIZED_DIRTY;
				canvas.cvDirtyMask[1] |= Canvas3D.MOVED_OR_RESIZED_DIRTY;
			}
//			canvas.resizeGraphics2D = true;
		}

		canvas.newSize = new Dimension((int) canvas.getGLWindow().getWidth(), (int) canvas.getGLWindow().getHeight());
		//canvas.newPosition = new Point(canvas.getGLWindow().getLocationOnScreen().x, canvas.getGLWindow().getLocationOnScreen().y);

	}

	@Override
	public void windowMoved(WindowEvent e)
	{

		synchronized (canvas)
		{
			synchronized (canvas.dirtyMaskLock)
			{
				canvas.cvDirtyMask[0] |= Canvas3D.MOVED_OR_RESIZED_DIRTY;
				canvas.cvDirtyMask[1] |= Canvas3D.MOVED_OR_RESIZED_DIRTY;
			}
		}

		canvas.newSize = new Dimension((int) canvas.getGLWindow().getWidth(), (int) canvas.getGLWindow().getHeight());
		//canvas.newPosition = new Point(canvas.getLocationOnScreen().x, canvas.getLocationOnScreen().y);

	}

	@Override
	public void windowDestroyNotify(WindowEvent e)
	{

	}

	@Override
	public void windowDestroyed(WindowEvent e)
	{

	}

	@Override
	public void windowGainedFocus(WindowEvent e)
	{

	}

	@Override
	public void windowLostFocus(WindowEvent e)
	{

	}

	@Override
	public void windowRepaint(WindowUpdateEvent e)
	{

	}

}
