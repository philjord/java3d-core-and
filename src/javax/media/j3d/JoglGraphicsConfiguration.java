/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

package javax.media.j3d;

import java.awt.AWTException;
import java.awt.ImageCapabilities;
import java.awt.image.VolatileImage;

import javaawt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.opengl.GLCapabilities;

import java2.awt.DisplayMode;
import java2.awt.GraphicsConfiguration;
import java2.awt.GraphicsDevice;

/**
 * Class implementing the GraphicsConfiguration API, but not a "real"
 * GraphicsConfiguration object. Wraps a GLCapabilities object and
 * supports either immediate or deferred pixel format / visual
 * selection depending on which platform we are running.
 */

class JoglGraphicsConfiguration extends GraphicsConfiguration
{
	private GLCapabilities caps;
	private int chosenIndex;
	private GraphicsDevice device;
//	private AWTGraphicsDevice awtGraphicsDevice;
	// Needed for Screen3D
	private int width;
	private int height;

	JoglGraphicsConfiguration(GLCapabilities caps, int chosenIndex, GraphicsDevice device)
	{
		super(null);//TODO: is this class dead now?
		this.caps = caps;
		this.chosenIndex = chosenIndex;
		this.device = device;

//		this.awtGraphicsDevice = new AWTGraphicsDevice(this.device, 0);
		DisplayMode m = device.getDisplayMode();
		width = m.getWidth();
		height = m.getHeight();
	}

	GLCapabilities getGLCapabilities()
	{
		return caps;
	}

	int getChosenIndex()
	{
		return chosenIndex;
	}



	@Override
	public Rectangle getBounds()
	{
		return new Rectangle(0, 0, width, height);
	}


	@Override
	public GraphicsDevice getDevice()
	{
		return device;
	}

//	public AWTGraphicsDevice getAwtGraphicsDevice()
//	{
//		return awtGraphicsDevice;
//	}


}
