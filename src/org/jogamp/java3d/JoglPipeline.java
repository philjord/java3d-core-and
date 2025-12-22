/*
 * Copyright (c) 2016 JogAmp Community. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE
 * HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2 only, as published by the Free Software Foundation. Sun designates this particular file as subject
 * to the "Classpath" exception as provided by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for
 * more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to
 * the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package org.jogamp.java3d;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import javaawt.GraphicsConfiguration;
import javaawt.GraphicsDevice;

/**
 * In order to ensure AWT classes don't get referenced by the android jar, it's required to dummy off unused parts of
 * Java3d-core like so...
 *
 */
public class JoglPipeline extends Pipeline {

	protected JoglPipeline() {

	}

	@Override
	void initialize(Pipeline.Type pipelineType) {
	}

	@Override
	void execute(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
					boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount,
					int[] texCoordSetMap, int texCoordSetMapLen, int[] texUnitOffset, int numActiveTexUnitState,
					int vertexAttrCount, int[] vertexAttrSizes, float[] varray, float[] carray, int cDirty) {
	}

	@Override
	void executeVA(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
					boolean ignoreVertexColors, int vcount, int vformat, int vdefined, int initialCoordIndex,
					float[] vfcoords, double[] vdcoords, int initialColorIndex, float[] cfdata, byte[] cbdata,
					int initialNormalIndex, float[] ndata, int vertexAttrCount, int[] vertexAttrSizes,
					int[] vertexAttrIndices, float[][] vertexAttrData, int texCoordMapLength, int[] texcoordoffset,
					int numActiveTexUnitState, int[] texIndex, int texstride, Object[] texCoords, int cdirty) {
	}

	@Override
	void executeVABuffer(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
							boolean ignoreVertexColors, int vcount, int vformat, int vdefined, int initialCoordIndex,
							Buffer vcoords, int initialColorIndex, Buffer cdataBuffer, float[] cfdata, byte[] cbdata,
							int initialNormalIndex, FloatBuffer ndata, int vertexAttrCount, int[] vertexAttrSizes,
							int[] vertexAttrIndices, FloatBuffer[] vertexAttrData, int texCoordMapLength,
							int[] texcoordoffset, int numActiveTexUnitState, int[] texIndex, int texstride,
							Object[] texCoords, int cdirty) {
	}

	@Override
	void executeInterleavedBuffer(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
									boolean useAlpha, boolean ignoreVertexColors, int startVIndex, int vcount,
									int vformat, int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen,
									int[] texUnitOffset, int numActiveTexUnit, FloatBuffer varray, float[] cdata,
									int cdirty) {
	}

	@Override
	void setVertexFormat(	Context ctx, GeometryArrayRetained geo, int vformat, boolean useAlpha,
							boolean ignoreVertexColors) {
	}

	@Override
	void buildGA(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha,
					float alpha, boolean ignoreVertexColors, int startVIndex, int vcount, int vformat,
					int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen, int[] texCoordSetMapOffset,
					int vertexAttrCount, int[] vertexAttrSizes, double[] xform, double[] nxform, float[] varray) {
	}

	@Override
	void buildGAForByRef(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
							boolean updateAlpha, float alpha, boolean ignoreVertexColors, int vcount, int vformat,
							int vdefined, int initialCoordIndex, float[] vfcoords, double[] vdcoords,
							int initialColorIndex, float[] cfdata, byte[] cbdata, int initialNormalIndex, float[] ndata,
							int vertexAttrCount, int[] vertexAttrSizes, int[] vertexAttrIndices,
							float[][] vertexAttrData, int texCoordMapLength, int[] tcoordsetmap, int[] texIndices,
							int texStride, Object[] texCoords, double[] xform, double[] nxform) {
	}

	@Override
	void executeIndexedGeometry(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
								boolean useAlpha, boolean ignoreVertexColors, int initialIndexIndex, int indexCount,
								int vertexCount, int vformat, int vertexAttrCount, int[] vertexAttrSizes,
								int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen,
								int[] texCoordSetOffset, int numActiveTexUnitState, float[] varray, float[] carray,
								int cdirty, int[] indexCoord) {
	}

	@Override
	void executeIndexedGeometryBuffer(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
										boolean useAlpha, boolean ignoreVertexColors, int initialIndexIndex,
										int indexCount, int vertexCount, int vformat, int texCoordSetCount,
										int[] texCoordSetMap, int texCoordSetMapLen, int[] texCoordSetOffset,
										int numActiveTexUnitState, FloatBuffer vdata, float[] carray, int cDirty,
										int[] indexCoord) {
	}

	@Override
	void executeIndexedGeometryVA(	Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
									boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount,
									int vertexCount, int vformat, int vdefined, float[] vfcoords, double[] vdcoords,
									float[] cfdata, byte[] cbdata, float[] ndata, int vertexAttrCount,
									int[] vertexAttrSizes, float[][] vertexAttrData, int texCoordMapLength,
									int[] texcoordoffset, int numActiveTexUnitState, int texStride, Object[] texCoords,
									int cdirty, int[] indexCoord) {
	}

	@Override
	void executeIndexedGeometryVABuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
										boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount,
										int vertexCount, int vformat, int vdefined, Buffer vcoords, Buffer cdataBuffer,
										float[] cfdata, byte[] cbdata, FloatBuffer ndata, int vertexAttrCount,
										int[] vertexAttrSizes, FloatBuffer[] vertexAttrData, int texCoordMapLength,
										int[] texcoordoffset, int numActiveTexUnitState, int texStride,
										Object[] texCoords, int cdirty, int[] indexCoord) {
	}

	@Override
	void buildIndexedGeometry(	Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
								boolean updateAlpha, float alpha, boolean ignoreVertexColors, int initialIndexIndex,
								int validIndexCount, int vertexCount, int vformat, int vertexAttrCount,
								int[] vertexAttrSizes, int texCoordSetCount, int[] texCoordSetMap,
								int texCoordSetMapLen, int[] texCoordSetMapOffset, double[] xform, double[] nxform,
								float[] varray, int[] indexCoord) {
	}


	@Override
	void readRaster(Context ctx, int type, int xSrcOffset, int ySrcOffset, int width, int height, int hCanvas,
					int imageDataType, int imageFormat, Object imageBuffer, int depthFormat, Object depthBuffer) {
	}

	@Override
	ShaderError setGLSLUniform1i(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									int value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform1f(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									float value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform2i(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform2f(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform3i(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform3f(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform4i(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform4f(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
									float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix3f(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix4f(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform1iArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform1fArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform2iArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform2fArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform3iArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform3fArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform4iArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, int[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniform4fArray(	Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
										int numElements, float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix3fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
											int numElements, float[] value) {
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix4fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation,
											int numElements, float[] value) {
		return null;
	}

	@Override
	ShaderError createGLSLShader(Context ctx, int shaderType, ShaderId[] shaderId) {
		return null;
	}

	@Override
	ShaderError destroyGLSLShader(Context ctx, ShaderId shaderId) {
		return null;
	}

	@Override
	ShaderError compileGLSLShader(Context ctx, ShaderId shaderId, String program) {
		return null;
	}

	@Override
	ShaderError createGLSLShaderProgram(Context ctx, ShaderProgramId[] shaderProgramId) {
		return null;
	}

	@Override
	ShaderError destroyGLSLShaderProgram(Context ctx, ShaderProgramId shaderProgramId) {
		return null;
	}

	@Override
	ShaderError linkGLSLShaderProgram(Context ctx, ShaderProgramId shaderProgramId, ShaderId[] shaderIds) {
		return null;
	}

	@Override
	ShaderError bindGLSLVertexAttrName(Context ctx, ShaderProgramId shaderProgramId, String attrName, int attrIndex) {
		return null;
	}

	@Override
	void lookupGLSLShaderAttrNames(	Context ctx, ShaderProgramId shaderProgramId, int numAttrNames, String[] attrNames,
									ShaderAttrLoc[] locArr, int[] typeArr, int[] sizeArr, boolean[] isArrayArr) {
	}

	@Override
	ShaderError useGLSLShaderProgram(Context ctx, ShaderProgramId shaderProgramId) {
		return null;
	}

	@Override
	void updateColoringAttributes(	Context ctx, float dRed, float dGreen, float dBlue, float red, float green,
									float blue, float alpha, boolean lightEnable, int shadeModel) {
	}

	@Override
	void updateDirectionalLight(Context ctx, int lightSlot, float red, float green, float blue, float dirx, float diry,
								float dirz) {
	}

	@Override
	void updatePointLight(	Context ctx, int lightSlot, float red, float green, float blue, float attenx, float atteny,
							float attenz, float posx, float posy, float posz) {
	}

	@Override
	void updateSpotLight(	Context ctx, int lightSlot, float red, float green, float blue, float attenx, float atteny,
							float attenz, float posx, float posy, float posz, float spreadAngle, float concentration,
							float dirx, float diry, float dirz) {
	}

	@Override
	void updateExponentialFog(Context ctx, float red, float green, float blue, float density) {
	}

	@Override
	void updateLinearFog(Context ctx, float red, float green, float blue, double fdist, double bdist) {
	}

	@Override
	void updateLineAttributes(	Context ctx, float lineWidth, int linePattern, int linePatternMask,
								int linePatternScaleFactor, boolean lineAntialiasing) {
	}

	@Override
	void updateMaterial(Context ctx, float red, float green, float blue, float alpha, float aRed, float aGreen,
						float aBlue, float eRed, float eGreen, float eBlue, float dRed, float dGreen, float dBlue,
						float sRed, float sGreen, float sBlue, float shininess, int colorTarget, boolean lightEnable) {
	}

	@Override
	void updateModelClip(Context ctx, int planeNum, boolean enableFlag, double A, double B, double C, double D) {
	}

	@Override
	void updatePointAttributes(Context ctx, float pointSize, boolean pointAntialiasing) {
	}

	@Override
	void updatePolygonAttributes(	Context ctx, int polygonMode, int cullFace, boolean backFaceNormalFlip,
									float polygonOffset, float polygonOffsetFactor) {
	}

	@Override
	void updateRenderingAttributes(	Context ctx, boolean depthBufferWriteEnableOverride,
									boolean depthBufferEnableOverride, boolean depthBufferEnable,
									boolean depthBufferWriteEnable, int depthTestFunction, float alphaTestValue,
									int alphaTestFunction, boolean ignoreVertexColors, boolean rasterOpEnable,
									int rasterOp, boolean userStencilAvailable, boolean stencilEnable,
									int stencilFailOp, int stencilZFailOp, int stencilZPassOp, int stencilFunction,
									int stencilReferenceValue, int stencilCompareMask, int stencilWriteMask) {
	}

	@Override
	void updateTexCoordGeneration(	Context ctx, boolean enable, int genMode, int format, float planeSx, float planeSy,
									float planeSz, float planeSw, float planeTx, float planeTy, float planeTz,
									float planeTw, float planeRx, float planeRy, float planeRz, float planeRw,
									float planeQx, float planeQy, float planeQz, float planeQw, double[] vworldToEc) {
	}

	@Override
	void updateTransparencyAttributes(	Context ctx, float alpha, int geometryType, int polygonMode, boolean lineAA,
										boolean pointAA, int transparencyMode, int srcBlendFunction,
										int dstBlendFunction) {
	}

	@Override
	void updateTextureAttributes(	Context ctx, double[] transform, boolean isIdentity, int textureMode,
									int perspCorrectionMode, float textureBlendColorRed, float textureBlendColorGreen,
									float textureBlendColorBlue, float textureBlendColorAlpha, int textureFormat) {
	}

	@Override
	void updateRegisterCombiners(	Context absCtx, double[] transform, boolean isIdentity, int textureMode,
									int perspCorrectionMode, float textureBlendColorRed, float textureBlendColorGreen,
									float textureBlendColorBlue, float textureBlendColorAlpha, int textureFormat,
									int combineRgbMode, int combineAlphaMode, int[] combineRgbSrc,
									int[] combineAlphaSrc, int[] combineRgbFcn, int[] combineAlphaFcn,
									int combineRgbScale, int combineAlphaScale) {
	}

	@Override
	void updateTextureColorTable(Context ctx, int numComponents, int colorTableSize, int[] textureColorTable) {
	}

	@Override
	void updateCombiner(Context ctx, int combineRgbMode, int combineAlphaMode, int[] combineRgbSrc,
						int[] combineAlphaSrc, int[] combineRgbFcn, int[] combineAlphaFcn, int combineRgbScale,
						int combineAlphaScale) {
	}

	@Override
	void updateTextureUnitState(Context ctx, int index, boolean enable) {
	}

	@Override
	void bindTexture2D(Context ctx, int objectId, boolean enable) {
	}

	@Override
	void updateTexture2DImage(	Context ctx, int numLevels, int level, int textureFormat, int imageFormat, int width,
								int height, int boundaryWidth, int dataType, Object data, boolean useAutoMipMap) {
	}

	@Override
	void updateTexture2DSubImage(	Context ctx, int level, int xoffset, int yoffset, int textureFormat, int imageFormat,
									int imgXOffset, int imgYOffset, int tilew, int width, int height, int dataType,
									Object data, boolean useAutoMipMap) {
	}

	@Override
	void updateTexture2DLodRange(Context ctx, int baseLevel, int maximumLevel, float minimumLOD, float maximumLOD) {
	}

	@Override
	void updateTexture2DLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR) {
	}

	@Override
	void updateTexture2DBoundary(	Context ctx, int boundaryModeS, int boundaryModeT, float boundaryRed,
									float boundaryGreen, float boundaryBlue, float boundaryAlpha) {
	}

	@Override
	void updateTexture2DFilterModes(Context ctx, int minFilter, int magFilter) {
	}

	@Override
	void updateTexture2DSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts) {
	}

	@Override
	void updateTexture2DFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts) {
	}

	@Override
	void updateTexture2DAnisotropicFilter(Context ctx, float degree) {
	}

	@Override
	void bindTexture3D(Context ctx, int objectId, boolean enable) {
	}

	@Override
	void updateTexture3DImage(	Context ctx, int numLevels, int level, int textureFormat, int imageFormat, int width,
								int height, int depth, int boundaryWidth, int dataType, Object data,
								boolean useAutoMipMap) {
	}

	@Override
	void updateTexture3DSubImage(	Context ctx, int level, int xoffset, int yoffset, int zoffset, int textureFormat,
									int imageFormat, int imgXOffset, int imgYOffset, int imgZOffset, int tilew,
									int tileh, int width, int height, int depth, int dataType, Object data,
									boolean useAutoMipMap) {
	}

	@Override
	void updateTexture3DLodRange(Context ctx, int baseLevel, int maximumLevel, float minimumLod, float maximumLod) {
	}

	@Override
	void updateTexture3DLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR) {
	}

	@Override
	void updateTexture3DBoundary(	Context ctx, int boundaryModeS, int boundaryModeT, int boundaryModeR,
									float boundaryRed, float boundaryGreen, float boundaryBlue, float boundaryAlpha) {
	}

	@Override
	void updateTexture3DFilterModes(Context ctx, int minFilter, int magFilter) {
	}

	@Override
	void updateTexture3DSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts) {
	}

	@Override
	void updateTexture3DFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts) {
	}

	@Override
	void updateTexture3DAnisotropicFilter(Context ctx, float degree) {
	}

	@Override
	void bindTextureCubeMap(Context ctx, int objectId, boolean enable) {
	}

	@Override
	void updateTextureCubeMapImage(	Context ctx, int face, int numLevels, int level, int textureFormat, int imageFormat,
									int width, int height, int boundaryWidth, int dataType, Object data,
									boolean useAutoMipMap) {
	}

	@Override
	void updateTextureCubeMapSubImage(	Context ctx, int face, int level, int xoffset, int yoffset, int textureFormat,
										int imageFormat, int imgXOffset, int imgYOffset, int tilew, int width,
										int height, int dataType, Object data, boolean useAutoMipMap) {
	}

	@Override
	void updateTextureCubeMapLodRange(	Context ctx, int baseLevel, int maximumLevel, float minimumLod,
										float maximumLod) {
	}

	@Override
	void updateTextureCubeMapLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR) {
	}

	@Override
	void updateTextureCubeMapBoundary(	Context ctx, int boundaryModeS, int boundaryModeT, float boundaryRed,
										float boundaryGreen, float boundaryBlue, float boundaryAlpha) {
	}

	@Override
	void updateTextureCubeMapFilterModes(Context ctx, int minFilter, int magFilter) {
	}

	@Override
	void updateTextureCubeMapSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts) {
	}

	@Override
	void updateTextureCubeMapFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts) {
	}

	@Override
	void updateTextureCubeMapAnisotropicFilter(Context ctx, float degree) {
	}

	@Override
	void resizeOffscreenLayer(Canvas3D cv, int cvWidth, int cvHeight) {
	}

	@Override
	Context createNewContext(Canvas3D cv, Drawable drawable, Context shareCtx, boolean isSharedCtx, boolean offScreen) {
		return null;
	}

	@Override
	void createQueryContext(Canvas3D cv, Drawable drawable, boolean offScreen, int width, int height) {
	}

	@Override
	Drawable createOffScreenBuffer(Canvas3D cv, Context ctx, int width, int height) {
		return null;
	}

	@Override
	void destroyOffScreenBuffer(Canvas3D cv, Context ctx, Drawable drawable) {
	}

	@Override
	void readOffScreenBuffer(Canvas3D cv, Context ctx, int format, int dataType, Object data, int width, int height) {
	}

	@Override
	void swapBuffers(Canvas3D cv, Context ctx, Drawable drawable) {
	}

	@Override
	void updateMaterialColor(Context ctx, float r, float g, float b, float a) {
	}

	@Override
	void destroyContext(Drawable drawable, Context ctx) {
	}

	@Override
	void accum(Context ctx, float value) {
	}

	@Override
	void accumReturn(Context ctx) {
	}

	@Override
	void clearAccum(Context ctx) {
	}

	@Override
	int getNumCtxLights(Context ctx) {
		return 0;
	}

	@Override
	boolean decal1stChildSetup(Context ctx) {
		return false;
	}

	@Override
	void decalNthChildSetup(Context ctx) {
	}

	@Override
	void decalReset(Context ctx, boolean depthBufferEnable) {
	}

	@Override
	void ctxUpdateEyeLightingEnable(Context ctx, boolean localEyeLightingEnable) {
	}

	@Override
	void setBlendColor(Context ctx, float red, float green, float blue, float alpha) {
	}

	@Override
	void setBlendFunc(Context ctx, int srcBlendFunction, int dstBlendFunction) {
	}

	@Override
	void setFogEnableFlag(Context ctx, boolean enable) {
	}

	@Override
	void setFullSceneAntialiasing(Context absCtx, boolean enable) {
	}

	@Override
	void updateSeparateSpecularColorEnable(Context ctx, boolean enable) {
	}

	@Override
	boolean validGraphicsMode() {
		return false;
	}

	@Override
	void setLightEnables(Context ctx, long enableMask, int maxLights) {
	}

	@Override
	void setSceneAmbient(Context ctx, float red, float green, float blue) {
	}

	@Override
	void disableFog(Context ctx) {
	}

	@Override
	void disableModelClip(Context ctx) {
	}

	@Override
	void resetRenderingAttributes(	Context ctx, boolean depthBufferWriteEnableOverride,
									boolean depthBufferEnableOverride) {
	}

	@Override
	void resetTextureNative(Context ctx, int texUnitIndex) {
	}

	@Override
	void activeTextureUnit(Context ctx, int texUnitIndex) {
	}

	@Override
	void resetTexCoordGeneration(Context ctx) {
	}

	@Override
	void resetTextureAttributes(Context ctx) {
	}

	@Override
	void resetPolygonAttributes(Context ctx) {
	}

	@Override
	void resetLineAttributes(Context ctx) {
	}

	@Override
	void resetPointAttributes(Context ctx) {
	}

	@Override
	void resetTransparency(Context ctx, int geometryType, int polygonMode, boolean lineAA, boolean pointAA) {
	}

	@Override
	void resetColoringAttributes(Context ctx, float r, float g, float b, float a, boolean enableLight) {
	}

	@Override
	void syncRender(Context ctx, boolean wait) {
	}

	@Override
	boolean useCtx(Context ctx, Drawable drawable) {
		return false;
	}

	@Override
	boolean releaseCtx(Context ctx) {
		return true;
	}

	@Override
	void clear(Context ctx, float r, float g, float b, boolean clearStencil) {
	}

	@Override
	void textureFillBackground(	Context ctx, float texMinU, float texMaxU, float texMinV, float texMaxV, float mapMinX,
								float mapMaxX, float mapMinY, float mapMaxY, boolean useBilinearFilter) {
	}

	@Override
	void textureFillRaster(	Context ctx, float texMinU, float texMaxU, float texMinV, float texMaxV, float mapMinX,
							float mapMaxX, float mapMinY, float mapMaxY, float mapZ, float alpha,
							boolean useBilinearFilter) {
	}

	@Override
	void executeRasterDepth(Context ctx, float posX, float posY, float posZ, int srcOffsetX, int srcOffsetY,
							int rasterWidth, int rasterHeight, int depthWidth, int depthHeight, int depthFormat,
							Object depthData) {
	}

	@Override
	void setModelViewMatrix(Context ctx, double[] viewMatrix, double[] modelMatrix) {
	}

	@Override
	void setProjectionMatrix(Context ctx, double[] projMatrix) {
	}

	@Override
	void setViewport(Context ctx, int x, int y, int width, int height) {
	}

	@Override
	void newDisplayList(Context ctx, int displayListId) {
	}

	@Override
	void endDisplayList(Context ctx) {
	}

	@Override
	void callDisplayList(Context ctx, int id, boolean isNonUniformScale) {
	}

	@Override
	void freeDisplayList(Context ctx, int id) {
	}

	@Override
	void freeTexture(Context ctx, int id) {
	}

	@Override
	int generateTexID(Context ctx) {
		return 0;
	}

	@Override
	void texturemapping(Context ctx, int px, int py, int minX, int minY, int maxX, int maxY, int texWidth,
						int texHeight, int rasWidth, int format, int objectId, byte[] imageYdown, int winWidth,
						int winHeight) {
	}

	@Override
	boolean initTexturemapping(Context ctx, int texWidth, int texHeight, int objectId) {
		return false;
	}

	@Override
	void setRenderMode(Context ctx, int mode, boolean doubleBuffer) {
	}

	@Override
	void setDepthBufferWriteEnable(Context ctx, boolean mode) {
	}

	@Override
	GraphicsConfiguration getGraphicsConfig(GraphicsConfiguration gconfig) {
		return null;
	}

	@Override
	GraphicsConfiguration getBestConfiguration(GraphicsConfigTemplate3D gct, GraphicsConfiguration[] gc) {
		return null;
	}

	@Override
	boolean isGraphicsConfigSupported(GraphicsConfigTemplate3D gct, GraphicsConfiguration gc) {
		return false;
	}

	@Override
	boolean hasDoubleBuffer(Canvas3D cv) {
		return false;
	}

	@Override
	boolean hasStereo(Canvas3D cv) {
		return false;
	}

	@Override
	int getStencilSize(Canvas3D cv) {
		return 0;
	}

	@Override
	boolean hasSceneAntialiasingMultisample(Canvas3D cv) {
		return false;
	}

	@Override
	boolean hasSceneAntialiasingAccum(Canvas3D cv) {
		return false;
	}

	@Override
	int getScreen(final GraphicsDevice graphicsDevice) {
		return 0;
	}

	@Override
	DrawingSurfaceObject createDrawingSurfaceObject(Canvas3D cv) {
		return null;
	}

	@Override
	void freeDrawingSurface(Canvas3D cv, DrawingSurfaceObject drawingSurfaceObject) {
	}

	@Override
	void freeDrawingSurfaceNative(Object o) {

	}

	@Override
	int getMaximumLights() {
		return 0;
	}
}
