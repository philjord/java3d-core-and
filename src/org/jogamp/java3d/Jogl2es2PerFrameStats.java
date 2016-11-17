package org.jogamp.java3d;

import java.util.HashSet;

public class Jogl2es2PerFrameStats
{
	public long endOfPrevFrameTime;

	public HashSet<ShaderProgramId> usedPrograms = new HashSet<ShaderProgramId>();
	//public ArrayList<ShaderProgramId> usedPrograms = new ArrayList<ShaderProgramId>();
	//public HashSet<String> usedProgramNames = new HashSet<String>();
	//TODO: how do I get these?
	//public HashMap<ShaderProgramId, String> usedProgramNames = new HashMap<ShaderProgramId, String>();

	public int geoToClearBuffers;
	public int glDrawStripArrays;
	public int glDrawStripArraysStrips;
	public int glDrawArrays;
	public int glDrawStripElements;
	public int glDrawStripElementsStrips;
	public int glDrawElements;
	public int setFFPAttributes;
	public int programToLocationData;
	public int enableTexCoordPointer;
	public int createGLSLShader;
	public int createGLSLShaderProgram;
	public int compileGLSLShader;
	public int destroyGLSLShader;
	public int destroyGLSLShaderProgram;
	public int linkGLSLShaderProgram;
	public int useGLSLShaderProgram;
	public int bindGLSLVertexAttrName;
	public int lookupGLSLShaderAttrNames;
	public int updateDirectionalLight;
	public int updatePointLight;
	public int updateSpotLight;
	public int updateExponentialFog;
	public int updateLinearFog;
	public int disableFog;
	public int setFogEnableFlag;
	public int updateLineAttributes;
	public int resetLineAttributes;
	public int updateMaterial;
	public int updateMaterialColor;
	public int updateColoringAttributes;
	public int resetColoringAttributes;
	public int updatePointAttributes;
	public int resetPointAttributes;
	public int updatePolygonAttributes;
	public int resetPolygonAttributes;
	public int updateRenderingAttributes;
	public int resetRenderingAttributes;
	public int updateTransparencyAttributes;
	public int resetTransparency;
	public int updateTextureAttributes;
	public int resetTextureAttributes;
	public int resetTexCoordGeneration;
	public int updateTextureUnitState;
	public int bindTexture2D;
	public int bindTextureCubeMap;
	public int setBlendColor;
	public int setBlendFunc;
	public int setFullSceneAntialiasing;
	public int setLightEnables;
	public int setSceneAmbient;
	public int activeTextureUnit;
	public int resetTextureNative;
	public int useCtx;
	public int releaseCtx;
	public int clear;
	public int setModelViewMatrix;
	public int setProjectionMatrix;
	public int setViewport;
	public int freeTexture;
	public int generateTexID;
	public int setDepthBufferWriteEnable;
	public int redundantUseProgram;

	public int coordCount;
	public int indexCount;
	public int glVertexAttribPointerNormals;
	public int glVertexAttribPointerUserAttribs;
	public int glVertexAttribPointerColor;
	public int glVertexAttribPointerCoord;
	public int glBufferData;
	public int glBufferSubData;
	public int glDisableVertexAttribArray;

	public int modelMatrixUpdated;
	public int glModelViewMatrixUpdated;
	public int glModelViewProjectionMatrixUpdated;
	public int glNormalMatrixUpdated;
	public int glModelViewMatrixInverseUpdated;

	public int modelMatrixSkipped;
	public int glModelViewMatrixSkipped;
	public int glModelViewProjectionMatrixSkipped;
	public int glNormalMatrixSkipped;
	public int glModelViewMatrixInverseSkipped;

	public int interleavedBufferCreated;

	public int glVertexAttribPointerInterleaved;

	public long setViewportTime;

	public long syncRenderTime;

	public void outputPerFrameData()
	{
		boolean highInterestOnly = true;

		System.out.println("coordCount " + coordCount + " indexCount " + indexCount);
		System.out.println("glDrawStripArrays " + glDrawStripArrays + "\t made up of glDrawStripArraysStrips " + glDrawStripArraysStrips);
		System.out.println("glDrawArrays " + glDrawArrays);
		System.out.println(
				"glDrawStripElements " + glDrawStripElements + "\t made up of glDrawStripElementsStrips " + glDrawStripElementsStrips);
		System.out.println("glDrawElements " + glDrawElements);
		System.out.println("glVertexAttribPointerCoord " + glVertexAttribPointerCoord);
		System.out.println("glVertexAttribPointerNormals " + glVertexAttribPointerNormals);
		System.out.println("glVertexAttribPointerColor " + glVertexAttribPointerColor);
		System.out.println("glVertexAttribPointerUserAttribs " + glVertexAttribPointerUserAttribs);
		System.out.println("enableTexCoordPointer " + enableTexCoordPointer);
		System.out.println("glBufferData " + glBufferData + " glBufferSubData " + glBufferSubData);
		System.out.println("glVertexAttribPointerInterleaved " + glVertexAttribPointerInterleaved);
		System.out.println("interleavedBufferCreated " + interleavedBufferCreated);
		System.out.println("---");
		System.out.println("setModelViewMatrix " + setModelViewMatrix);
		System.out.println("setFFPAttributes " + setFFPAttributes);
		System.out.println("modelMatrixUpdated " + modelMatrixUpdated + " modelMatrixSkipped " + modelMatrixSkipped);
		System.out
				.println("glModelViewMatrixUpdated " + glModelViewMatrixUpdated + " glModelViewMatrixSkipped " + glModelViewMatrixSkipped);
		System.out.println("glModelViewProjectionMatrixUpdated " + glModelViewProjectionMatrixUpdated
				+ " glModelViewProjectionMatrixSkipped " + glModelViewProjectionMatrixSkipped);
		System.out.println("glNormalMatrixUpdated " + glNormalMatrixUpdated + " glNormalMatrixSkipped " + glNormalMatrixSkipped);
		System.out.println("---");
		if (!highInterestOnly)
		{
			System.out.println("glDisableVertexAttribArray " + glDisableVertexAttribArray + " note native called commented out, trouble?");
			System.out.println("geoToClearBuffers " + geoToClearBuffers);
			System.out.println("programToLocationData " + programToLocationData);
			System.out.print("createGLSLShader " + createGLSLShader);
			System.out.print("\tcreateGLSLShaderProgram " + createGLSLShaderProgram);
			System.out.print("\tcompileGLSLShader " + compileGLSLShader);
			System.out.print("\tdestroyGLSLShader " + destroyGLSLShader);
			System.out.print("\tdestroyGLSLShaderProgram " + destroyGLSLShaderProgram);
			System.out.print("\tlinkGLSLShaderProgram " + linkGLSLShaderProgram);
			System.out.print("\tbindGLSLVertexAttrName " + bindGLSLVertexAttrName);
			System.out.println("\tlookupGLSLShaderAttrNames " + lookupGLSLShaderAttrNames);
			System.out.print("updateDirectionalLight " + updateDirectionalLight);
			System.out.print("\tupdatePointLight " + updatePointLight);
			System.out.println("\tupdateSpotLight " + updateSpotLight);
			System.out.print("updateExponentialFog " + updateExponentialFog);
			System.out.print("\tupdateLinearFog " + updateLinearFog);
			System.out.print("\tdisableFog " + disableFog);
			System.out.println("\tsetFogEnableFlag " + setFogEnableFlag);
			System.out.print("updateLineAttributes " + updateLineAttributes);
			System.out.println("\tresetLineAttributes " + resetLineAttributes);
			System.out.print("updateMaterial " + updateMaterial);
			System.out.println("\tupdateMaterialColor " + updateMaterialColor);
			System.out.print("updateColoringAttributes " + updateColoringAttributes);
			System.out.println("\tresetColoringAttributes " + resetColoringAttributes);
			System.out.print("updatePointAttributes " + updatePointAttributes);
			System.out.println("\tresetPointAttributes " + resetPointAttributes);
			System.out.print("updatePolygonAttributes " + updatePolygonAttributes);
			System.out.println("\tresetPolygonAttributes " + resetPolygonAttributes);
			System.out.print("updateRenderingAttributes " + updateRenderingAttributes);
			System.out.println("\tresetRenderingAttributes " + resetRenderingAttributes);
			System.out.println("setBlendColor " + setBlendColor);
			System.out.println("setFullSceneAntialiasing " + setFullSceneAntialiasing);
			System.out.println("setLightEnables " + setLightEnables);
			System.out.println("setSceneAmbient " + setSceneAmbient);
			System.out.println("resetTexCoordGeneration " + resetTexCoordGeneration);
			System.out.println("freeTexture " + freeTexture);
			System.out.println("generateTexID " + generateTexID);
			System.out.println("useCtx " + useCtx);
			System.out.println("releaseCtx " + releaseCtx);
			System.out.println("clear " + clear);
			System.out.println("setViewport " + setViewport);
			System.out.println("setProjectionMatrix " + setProjectionMatrix);
		}

		System.out.print("updateTransparencyAttributes " + updateTransparencyAttributes);
		System.out.println("\tresetTransparency " + resetTransparency);
		System.out.print("updateTextureAttributes " + updateTextureAttributes);
		System.out.println("\tresetTextureAttributes " + resetTextureAttributes);
		System.out.println("updateTextureUnitState " + updateTextureUnitState);
		System.out.println("bindTexture2D " + bindTexture2D + "\tbindTextureCubeMap " + bindTextureCubeMap);
		System.out.println("setBlendFunc " + setBlendFunc);
		System.out.println("activeTextureUnit " + activeTextureUnit + "\tresetTextureNative " + resetTextureNative);
		System.out.println("setDepthBufferWriteEnable " + setDepthBufferWriteEnable);
		System.out.println("useGLSLShaderProgram " + useGLSLShaderProgram + " redundantUseProgram " + redundantUseProgram);

		//for (ShaderProgramId id : usedPrograms)
		//	System.out.println("ShaderProgramId " + ((JoglShaderObject) id).getValue());
		if ((syncRenderTime - setViewportTime) != 0)
		{
			System.out.println("time in frame (not in glFinish) " + (syncRenderTime - setViewportTime) + //
					" = (ms) " + ((syncRenderTime - setViewportTime) / 1000000L));// + //
				//	" = fps: " + (1000 / ((syncRenderTime - setViewportTime) / 1000000L)));
		}
		
		long now = System.nanoTime();
		System.out.println("time since end of previous frame (ns) " + (now - endOfPrevFrameTime) + //
				" = (ms) " + ((now - endOfPrevFrameTime) / 1000000L) + //
				" = fps: " + (1000 / ((now - endOfPrevFrameTime) / 1000000L)));
	}
}