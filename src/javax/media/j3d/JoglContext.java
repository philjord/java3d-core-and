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

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLContext;

/**
 * Graphics context objects for Jogl rendering pipeline.
 */
class JoglContext implements Context
{
	protected GLContext context;

	// Properties we need to keep track of for efficiency
	private int maxTexCoordSets;
	private float alphaClearValue;
	private int currentTextureUnit;
	private int currentCombinerUnit;
	private boolean hasMultisample;

	// Needed for vertex attribute implementation
	private JoglShaderObject shaderProgram;

	// Only used when GLSL shader library is active
	private int glslVertexAttrOffset;

	JoglContext(GLContext context)
	{
		this.context = context;
	}

	GLContext getGLContext()
	{
		return context;
	}

	int getMaxTexCoordSets()
	{
		return maxTexCoordSets;
	}

	void setMaxTexCoordSets(int val)
	{
		maxTexCoordSets = val;
	}

	float getAlphaClearValue()
	{
		return alphaClearValue;
	}

	void setAlphaClearValue(float val)
	{
		alphaClearValue = val;
	}

	int getCurrentTextureUnit()
	{
		return currentTextureUnit;
	}

	void setCurrentTextureUnit(int val)
	{
		currentTextureUnit = val;
	}

	int getCurrentCombinerUnit()
	{
		return currentCombinerUnit;
	}

	void setCurrentCombinerUnit(int val)
	{
		currentCombinerUnit = val;
	}

	boolean getHasMultisample()
	{
		return hasMultisample;
	}

	void setHasMultisample(boolean val)
	{
		hasMultisample = val;
	}

	void vertexAttrPointer(GL2ES2 gl, int index, int size, int type, int stride, Buffer pointer, GeometryArrayRetained geo)
	{
		if (this instanceof JoglesContext)
		{
			JoglesContext joglesContext = (JoglesContext) this;
			HashMap<Integer, Integer> bufIds = joglesContext.geoToVertAttribBuf.get(geo);
			if (bufIds == null)
			{
				bufIds = new HashMap<Integer, Integer>();
				joglesContext.geoToVertAttribBuf.put(geo, bufIds);
			}

			Integer bufId = bufIds.get(index);
			if (bufId == null)
			{
				int[] tmp = new int[1];
				gl.glGenBuffers(1, tmp, 0);
				bufId = new Integer(tmp[0]);
				bufIds.put(index, bufId);
				
				//TODO: I just made vertex attributes static is that ok??
				gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufId.intValue());
				gl.glBufferData(GL.GL_ARRAY_BUFFER, size, pointer, GL.GL_STATIC_DRAW);			
			}
			
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufId.intValue());
			gl.glVertexAttribPointer(index + glslVertexAttrOffset, size, type, false, stride, 0);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		}
		else
		{
			// this is madness, not sparta!
			int[] tmp = new int[1];
			gl.glGenBuffers(1, tmp, 0);
			int bufId = tmp[0];

			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufId);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, size, pointer, GL.GL_STATIC_DRAW);
			
			gl.glVertexAttribPointer(index + glslVertexAttrOffset, size, type, false, stride, 0);
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
		}
		
	}

	void enableVertexAttrArray(GL2ES2 gl, int index)
	{
		gl.glEnableVertexAttribArray(index + glslVertexAttrOffset);
	}

	void disableVertexAttrArray(GL2ES2 gl, int index)
	{
		gl.glDisableVertexAttribArray(index + glslVertexAttrOffset);
	}

	void vertexAttr1fv(GL2ES2 gl, int index, FloatBuffer buf)
	{
		gl.glVertexAttrib1fv(index + glslVertexAttrOffset, buf);
	}

	void vertexAttr2fv(GL2ES2 gl, int index, FloatBuffer buf)
	{
		gl.glVertexAttrib2fv(index + glslVertexAttrOffset, buf);
	}

	void vertexAttr3fv(GL2ES2 gl, int index, FloatBuffer buf)
	{
		gl.glVertexAttrib3fv(index + glslVertexAttrOffset, buf);
	}

	void vertexAttr4fv(GL2ES2 gl, int index, FloatBuffer buf)
	{
		gl.glVertexAttrib4fv(index + glslVertexAttrOffset, buf);
	}

	// Used in vertex attribute implementation
	JoglShaderObject getShaderProgram()
	{
		return shaderProgram;
	}

	void setShaderProgram(JoglShaderObject object)
	{
		shaderProgram = object;
	}

	// Only used when GLSL shaders are in use
	int getGLSLVertexAttrOffset()
	{
		return glslVertexAttrOffset;
	}

	void setGLSLVertexAttrOffset(int offset)
	{
		glslVertexAttrOffset = offset;
	}
}
