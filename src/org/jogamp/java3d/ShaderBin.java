/*
 * Copyright 2005-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.jogamp.java3d;

import java.util.ArrayList;
import java.util.Map;

// XXXX : We should have a common Bin object that all other Bins extend from.

//class ShaderBin extends Object implements ObjectUpdate, NodeComponentUpdate {
class ShaderBin implements ObjectUpdate
{

	//PJPJPJPJPJPJPJPJPJPJ
	//For performance on modern cards we MUST rendering is shader program order
	// shader program switching is very expensive
	// Obviously we must still respect the background/opaque/order/transparent passes
	// Lights and EnvironmentSet are at the top of the render tree, they are spatially
	// configured, so they don't easily interchange with the simple attributes of shader/attribute/texture
	// below them.
	// So in order to force program order at the top of each render call for the 4 passes
	// the render call at the top will clear shaderRun, then when a ShaderBin is called to render
	// and currentPassShaderProgram is null the bin will check to see if it's ShaderProgram 
	// was run in a previous pass (is it in shadersRun?) if it has run then the ShaderBin exits early
	// if it has not run it will set currentPassShaderProgram to it's Program and add it's ShaderProgram to 
	// shadersRun and then render normally
	// If currentPassShaderProgram is not null, then a ShaderBin will simply exit if it's shader is not the
	// currentPassShaderProgram.
	// Finally the outermost loop (up at RenderBin) will start by clearing shadersRun then
	// repeatedly call the tree (setting currentPassShaderProgram to null before each pass)
	// until the currentPassShaderProgram is left at null after the pass.
	// then it will be complete

	// Nope possible because transparency jumps down to texturebin in geometry sorted order
	// and just assume path to root is correct, so shader program needs to not only go to the very 
	// top of tree but geometry sorting need to be by shader.
	// perhaps I could use the sorted system everywhere and sort by program

	/**
	 * Node component dirty mask.
	 */
	static final int SHADER_PROGRAM_DIRTY = 0x1;
	static final int SHADER_ATTRIBUTE_SET_DIRTY = 0x2;

	/**
	 * The RenderBin for this object
	 */
	RenderBin renderBin = null;

	//  is this truely needed now?  AttributeBin attributeBin = null;

	/**
	 * The EnvirionmentSet that this AttributeBin resides
	 */
	EnvironmentSet environmentSet = null;

	/**
	 * The references to the next and previous ShaderBins in the
	 * list.
	 */
	ShaderBin next = null;
	ShaderBin prev = null;

	/**
	 * The list of AttributeBins in this EnvironmentSet
	 */
	AttributeBin attributeBinList = null;
	/**
	 * List of attrributeBins to be added next Frame
	 */
	ArrayList<AttributeBin> addAttributeBins = new ArrayList<AttributeBin>();

	boolean onUpdateList = false;

	int componentDirty = 0;
	ShaderAppearanceRetained shaderAppearance = null;
	ShaderProgramRetained shaderProgram = null;
	ShaderAttributeSetRetained shaderAttributeSet = new ShaderAttributeSetRetained();

	ShaderBin(ShaderAppearanceRetained sApp, RenderBin rBin)
	{
		reset(sApp, rBin);
	}

	void reset(ShaderAppearanceRetained sApp, RenderBin rBin)
	{
		prev = null;
		next = null;
		renderBin = rBin;
		//attributeBin = null;
		attributeBinList = null;

		onUpdateList = false;
		
		if (sApp != null)
		{
			shaderProgram = sApp.shaderProgram;			
			if(sApp.shaderAttributeSet != null)
				shaderAttributeSet.getAttrs().putAll(sApp.shaderAttributeSet.getAttrs());
		}
		else
		{
			shaderProgram = null;
		}
		shaderAppearance = sApp;
	}

	void clear()
	{
		reset(null, null);
	}

	/**
	 * This tests if the qiven ra.shaderProgram  match this shaderProgram
	 */
	boolean equals(ShaderAppearanceRetained sApp)
	{

		ShaderProgramRetained sp;
		ShaderAttributeSetRetained ss;

		if (sApp == null)
		{
			sp = null;
			ss = null;
		}
		else
		{
			sp = sApp.shaderProgram;
			ss = sApp.shaderAttributeSet;
		}

		if ((shaderProgram != sp) || (shaderAttributeSet != ss))
		{
			return false;
		}
		
		return true;

	}

	@Override
	public void updateObject()
	{
		int i;
		AttributeBin a;

		if (addAttributeBins.size() > 0)
		{
			a = addAttributeBins.get(0);
			if (attributeBinList == null)
			{
				attributeBinList = a;

			}
			else
			{
				a.next = attributeBinList;
				attributeBinList.prev = a;
				attributeBinList = a;
			}
			for (i = 1; i < addAttributeBins.size(); i++)
			{
				a = addAttributeBins.get(i);
				a.next = attributeBinList;
				attributeBinList.prev = a;
				attributeBinList = a;
			}
		}

		addAttributeBins.clear();

		onUpdateList = false;
	}

	/**
	 * Adds the given AttributeBin to this EnvironmentSet.
	 */
	void addAttributeBin(AttributeBin a, RenderBin rb)
	{
		a.shaderBin = this;
		addAttributeBins.add(a);
		if (!onUpdateList)
		{
			rb.objUpdateList.add(this);
			onUpdateList = true;
		}

	}

	/**
	 * Removes the given AttributeBin from this EnvironmentSet.
	 */
	void removeAttributeBin(AttributeBin a)
	{
		a.shaderBin = null;
		// If the attributeBin being remove is contained in addAttributeBins, then
		// remove the attributeBin from the addList
		if (addAttributeBins.contains(a))
		{
			addAttributeBins.remove(addAttributeBins.indexOf(a));
		}
		else
		{
			if (a.prev == null)
			{ // At the head of the list
				attributeBinList = a.next;
				if (a.next != null)
				{
					a.next.prev = null;
				}
			}
			else
			{ // In the middle or at the end.
				a.prev.next = a.next;
				if (a.next != null)
				{
					a.next.prev = a.prev;
				}
			}
		}
		a.prev = null;
		a.next = null;

		if (a.definingRenderingAttributes != null && (a.definingRenderingAttributes.changedFrequent != 0))
			a.definingRenderingAttributes = null;
		a.onUpdateList &= ~AttributeBin.ON_CHANGED_FREQUENT_UPDATE_LIST;

		if (attributeBinList == null && addAttributeBins.size() == 0)
		{
			// Note: Removal of this shaderBin as a user of the rendering
			// atttrs is done during removeRenderAtom() in RenderMolecule.java
			environmentSet.removeShaderBin(this);
		}
	}

	/**
	 * Renders this ShaderBin
	 */
	void render(Canvas3D cv)
	{

		// include this ShaderBin to the to-be-updated list in canvas
		cv.setStateToUpdate(Canvas3D.SHADERBIN_BIT, this);

		AttributeBin a = attributeBinList;
		while (a != null)
		{
			a.render(cv);
			a = a.next;
		}
	}

	void updateAttributes(Canvas3D cv)
	{

		// System.err.println("ShaderBin.updateAttributes() shaderProgram is " + shaderProgram);
		if (shaderProgram != null)
		{
			// Compile, link, and enable shader program
			shaderProgram.updateNative(cv, true);

			if (shaderAttributeSet != null)
			{
				shaderAttributeSet.updateNative(cv, shaderProgram);
			}

		}
		else
		{
			if (cv.shaderProgram != null)
			{
				// Disable shader program
				cv.shaderProgram.updateNative(cv, false);
			}
		}

		cv.shaderBin = this;
		cv.shaderProgram = shaderProgram;
	}

	void updateNodeComponent()
	{
	 //System.err.println("ShaderBin.updateNodeComponent() ...");

		// We don't need to clone shaderProgram.
		// ShaderProgram object can't be modified once it is live,
		// so each update should be a new reference.
		if ((componentDirty & SHADER_PROGRAM_DIRTY) != 0)
		{
			//System.err.println("  - SHADER_PROGRAM_DIRTY");

			shaderProgram = shaderAppearance.shaderProgram;
		}

		// We need to clone the shaderAttributeSet.
		if ((componentDirty & SHADER_ATTRIBUTE_SET_DIRTY) != 0)
		{
			//System.err.println("  - SHADER_ATTRIBUTE_SET_DIRTY");
			 
			Map<String, ShaderAttributeRetained> attrs = shaderAttributeSet.getAttrs();
			attrs.clear();			 
			if (shaderAppearance.shaderAttributeSet != null)
			{
				attrs.putAll(shaderAppearance.shaderAttributeSet.getAttrs());
			}
		}
		
		componentDirty = 0;
	}

}
