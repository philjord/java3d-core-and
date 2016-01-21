package javax.media.j3d;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

/**
 * Concrete implementation of Pipeline class for the JOGL rendering
 * pipeline.
 */
abstract class JoglesDEPPipeline extends Pipeline
{

	/**
	 * Constructor for singleton JoglPipeline instance
	 */
	protected JoglesDEPPipeline()
	{

	}

	// ---------------------------------------------------------------------

	//
	// GeometryArrayRetained methods
	//

	// used for GeometryArrays by Copy or interleaved
	//NOT USED BY MORROWIND - but drawTrivial used it
	@Override
	@Deprecated
	void execute(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texUnitOffset, int numActiveTexUnitState, int vertexAttrCount, int[] vertexAttrSizes,
			float[] varray, float[] carray, int cDirty)
	{
		throw new UnsupportedOperationException();
	}

	// used by GeometryArray by Reference with java arrays
	// NOT USED BY MORROWIND  
	@Override
	@Deprecated
	void executeVA(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean ignoreVertexColors, int vcount,
			int vformat, int vdefined, int initialCoordIndex, float[] vfcoords, double[] vdcoords, int initialColorIndex, float[] cfdata,
			byte[] cbdata, int initialNormalIndex, float[] ndata, int vertexAttrCount, int[] vertexAttrSizes, int[] vertexAttrIndices,
			float[][] vertexAttrData, int texCoordMapLength, int[] texcoordoffset, int numActiveTexUnitState, int[] texIndex, int texstride,
			Object[] texCoords, int cdirty)
	{
		throw new UnsupportedOperationException();
	}

	// used by GeometryArray by Reference in interleaved format with NIO buffer
	// NOT USED BY MORROWIND
	@Override
	@Deprecated
	void executeInterleavedBuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texUnitOffset, int numActiveTexUnit, FloatBuffer varray, float[] cdata, int cdirty)
	{
		throw new UnsupportedOperationException();
	}

	// used for GeometryArrays
	// NOT IN USE BY MORROWIND  
	@Override
	@Deprecated
	void buildGA(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha, float alpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texCoordSetMapOffset, int vertexAttrCount, int[] vertexAttrSizes, double[] xform, double[] nxform,
			float[] varray)
	{
		throw new UnsupportedOperationException();
	}

	// used to Build Dlist GeometryArray by Reference with java arrays
	// NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void buildGAForByRef(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha, float alpha,
			boolean ignoreVertexColors, int vcount, int vformat, int vdefined, int initialCoordIndex, float[] vfcoords, double[] vdcoords,
			int initialColorIndex, float[] cfdata, byte[] cbdata, int initialNormalIndex, float[] ndata, int vertexAttrCount,
			int[] vertexAttrSizes, int[] vertexAttrIndices, float[][] vertexAttrData, int texCoordMapLength, int[] tcoordsetmap,
			int[] texIndices, int texStride, Object[] texCoords, double[] xform, double[] nxform)
	{
		throw new UnsupportedOperationException();
	}

	//----------------------------------------------------------------------
	// Private helper methods for GeometryArrayRetained
	//
	//NOT IN USE BY MORROWIND - as interleaved support dropped
	@Deprecated
	private void testForInterleavedArrays(int vformat, boolean[] useInterleavedArrays, int[] iaFormat)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND - odd name looks like it should be used?
	@Deprecated
	private void executeTexture(int texCoordSetMapLen, int texSize, int bstride, int texCoordoff, int[] texCoordSetMapOffset,
			int numActiveTexUnit, FloatBuffer verts, GL2 gl)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Deprecated
	private void executeGeometryArray(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texCoordSetMapOffset, int numActiveTexUnitState, int vertexAttrCount, int[] vertexAttrSizes,
			float[] varray, FloatBuffer varrayBuffer, float[] carray, int cDirty)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Deprecated
	private String getVertexDescription(int vformat)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Deprecated
	private String getGeometryDescription(int geo_type)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// IndexedGeometryArrayRetained methods
	//

	// by-copy or interleaved, by reference, Java arrays
	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void executeIndexedGeometry(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int initialIndexIndex, int indexCount, int vertexCount, int vformat, int vertexAttrCount,
			int[] vertexAttrSizes, int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen, int[] texCoordSetOffset,
			int numActiveTexUnitState, float[] varray, float[] carray, int cdirty, int[] indexCoord)
	{
		throw new UnsupportedOperationException();
	}

	// interleaved, by reference, nio buffer
	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void executeIndexedGeometryBuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int initialIndexIndex, int indexCount, int vertexCount, int vformat, int texCoordSetCount,
			int[] texCoordSetMap, int texCoordSetMapLen, int[] texCoordSetOffset, int numActiveTexUnitState, FloatBuffer vdata,
			float[] carray, int cDirty, int[] indexCoord)
	{
		throw new UnsupportedOperationException();
	}

	// by-copy geometry
	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void buildIndexedGeometry(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha,
			float alpha, boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount, int vertexCount, int vformat,
			int vertexAttrCount, int[] vertexAttrSizes, int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen,
			int[] texCoordSetMapOffset, double[] xform, double[] nxform, float[] varray, int[] indexCoord)
	{
		throw new UnsupportedOperationException();
	}

	//----------------------------------------------------------------------
	//
	// Helper routines for IndexedGeometryArrayRetained
	//
	//NOT IN USE BY MORROWIND
	@Deprecated
	private void executeIndexedGeometryArray(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
			boolean useAlpha, boolean ignoreVertexColors, int initialIndexIndex, int indexCount, int vertexCount, int vformat,
			int vertexAttrCount, int[] vertexAttrSizes, int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen,
			int[] texCoordSetOffset, int numActiveTexUnitState, float[] varray, FloatBuffer vdata, float[] carray, int cDirty,
			int[] indexCoord)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// GraphicsContext3D methods
	//

	// Native method for readRaster
	// REMOVE FOR SIMPLICITY, POSSIBLY ADD BACK LATER
	@Override
	@Deprecated
	void readRaster(Context ctx, int type, int xSrcOffset, int ySrcOffset, int width, int height, int hCanvas, int imageDataType,
			int imageFormat, Object imageBuffer, int depthFormat, Object depthBuffer)
	{

		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// ModelClipRetained methods
	//
	@Override
	@Deprecated
	void updateModelClip(Context ctx, int planeNum, boolean enableFlag, double A, double B, double C, double D)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// PointAttributesRetained methods
	//
	//NOT IN USE BY MORROWIND - 
	@Override
	@Deprecated
	void updatePointAttributes(Context ctx, float pointSize, boolean pointAntialiasing)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// TexCoordGenerationRetained methods
	//

	/**
	 * This method updates the native context:
	 * trans contains eyeTovworld transform in d3d
	 * trans contains vworldToEye transform in ogl
	 */
	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexCoordGeneration(Context ctx, boolean enable, int genMode, int format, float planeSx, float planeSy, float planeSz,
			float planeSw, float planeTx, float planeTy, float planeTz, float planeTw, float planeRx, float planeRy, float planeRz,
			float planeRw, float planeQx, float planeQy, float planeQz, float planeQw, double[] vworldToEc)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// TextureAttributesRetained methods
	//

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateRegisterCombiners(Context absCtx, double[] transform, boolean isIdentity, int textureMode, int perspCorrectionMode,
			float textureBlendColorRed, float textureBlendColorGreen, float textureBlendColorBlue, float textureBlendColorAlpha,
			int textureFormat, int combineRgbMode, int combineAlphaMode, int[] combineRgbSrc, int[] combineAlphaSrc, int[] combineRgbFcn,
			int[] combineAlphaFcn, int combineRgbScale, int combineAlphaScale)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTextureColorTable(Context ctx, int numComponents, int colorTableSize, int[] textureColorTable)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND 
	@Override
	@Deprecated
	void updateCombiner(Context ctx, int combineRgbMode, int combineAlphaMode, int[] combineRgbSrc, int[] combineAlphaSrc,
			int[] combineRgbFcn, int[] combineAlphaFcn, int combineRgbScale, int combineAlphaScale)
	{
		throw new UnsupportedOperationException();
	}

	// Helper routines for above
	//IN USE BY MORROWIND 
	@Deprecated
	private void getGLCombineMode(GL gl, int combineRgbMode, int combineAlphaMode, int[] GLrgbMode, int[] GLalphaMode)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// TextureRetained methods
	// Texture2DRetained methods

	//IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture2DLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void updateTexture2DSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void updateTexture2DFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Deprecated
	private void updateTextureLodOffset(Context ctx, int target, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// Texture3DRetained methods
	//
	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void bindTexture3D(Context ctx, int objectId, boolean enable)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DImage(Context ctx, int numLevels, int level, int textureFormat, int imageFormat, int width, int height, int depth,
			int boundaryWidth, int dataType, Object data, boolean useAutoMipMap)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DSubImage(Context ctx, int level, int xoffset, int yoffset, int zoffset, int textureFormat, int imageFormat,
			int imgXOffset, int imgYOffset, int imgZOffset, int tilew, int tileh, int width, int height, int depth, int dataType,
			Object data, boolean useAutoMipMap)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DLodRange(Context ctx, int baseLevel, int maximumLevel, float minimumLod, float maximumLod)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DBoundary(Context ctx, int boundaryModeS, int boundaryModeT, int boundaryModeR, float boundaryRed,
			float boundaryGreen, float boundaryBlue, float boundaryAlpha)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DFilterModes(Context ctx, int minFilter, int magFilter)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts)
	{
		throw new UnsupportedOperationException();
	}

	//NOT IN USE BY MORROWIND
	@Override
	@Deprecated
	void updateTexture3DAnisotropicFilter(Context ctx, float degree)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// TextureCubeMapRetained methods

	@Override
	@Deprecated
	void updateTextureCubeMapLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void updateTextureCubeMapSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void updateTextureCubeMapFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts)
	{
		throw new UnsupportedOperationException();
	}

	@Deprecated
	private void updateTextureSharpenFunc(Context ctx, int target, int numPts, float[] pts)
	{
		throw new UnsupportedOperationException();

	}

	@Deprecated
	private void updateTextureFilter4Func(Context ctx, int target, int numPts, float[] pts)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

	//
	// Canvas3D methods - native wrappers
	//

	@Override
	@Deprecated
	//NOT IN USE BY MORROWIND
	void createQueryContext(Canvas3D cv, Drawable drawable, boolean offScreen, int width, int height)
	{
		throw new UnsupportedOperationException();
	}

	// This is the native for creating an offscreen buffer
	@Override
	@Deprecated
	//NOT IN USE BY MORROWIND
	Drawable createOffScreenBuffer(Canvas3D cv, Context ctx, int width, int height)
	{
		throw new UnsupportedOperationException();
	}

	// 'destroyContext' is called first if context exists
	@Override
	@Deprecated
	//NOT IN USE BY MORROWIND
	void destroyOffScreenBuffer(Canvas3D cv, Context ctx, Drawable drawable)
	{
		throw new UnsupportedOperationException();
	}

	// This is the native for reading the image from the offscreen buffer
	@Override
	@Deprecated
	//NOT IN USE BY MORROWIND
	void readOffScreenBuffer(Canvas3D cv, Context ctx, int format, int dataType, Object data, int width, int height)
	{
		throw new UnsupportedOperationException();
	}

	// This is the native method for doing accumulation.
	//I BELIEVE THIS IS ABOUT FULL SCREEN AA
	@Override
	@Deprecated
	void accum(Context ctx, float value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void accumReturn(Context ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void clearAccum(Context ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	boolean decal1stChildSetup(Context ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void decalNthChildSetup(Context ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void decalReset(Context ctx, boolean depthBufferEnable)
	{
		throw new UnsupportedOperationException();
	}

	// The following three methods are used in multi-pass case

	@Override
	@Deprecated
	void textureFillBackground(Context ctx, float texMinU, float texMaxU, float texMinV, float texMaxV, float mapMinX, float mapMaxX,
			float mapMinY, float mapMaxY, boolean useBilinearFilter)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	@Deprecated
	void textureFillRaster(Context ctx, float texMinU, float texMaxU, float texMinV, float texMaxV, float mapMinX, float mapMaxX,
			float mapMinY, float mapMaxY, float mapZ, float alpha, boolean useBilinearFilter)
	{

		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	void executeRasterDepth(Context ctx, float posX, float posY, float posZ, int srcOffsetX, int srcOffsetY, int rasterWidth,
			int rasterHeight, int depthWidth, int depthHeight, int depthFormat, Object depthData)
	{
		throw new UnsupportedOperationException();

	}

	// used for display Lists
	@Override
	@Deprecated
	// GLES  NIO buffers prevent use (possibly by ref also)
	void newDisplayList(Context ctx, int displayListId)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	// GLES  NIO buffers prevent use
	void endDisplayList(Context ctx)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	// GLES  NIO buffers prevent use
	void callDisplayList(Context ctx, int id, boolean isNonUniformScale)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	// GLES  NIO buffers prevent use
	void freeDisplayList(Context ctx, int id)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	// GLES nothing seems to call this in Canvas3D either
	void texturemapping(Context ctx, int px, int py, int minX, int minY, int maxX, int maxY, int texWidth, int texHeight, int rasWidth,
			int format, int objectId, byte[] imageYdown, int winWidth, int winHeight)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	// GLES and nothing seems to call this in Canvas3D either
	boolean initTexturemapping(Context ctx, int texWidth, int texHeight, int objectId)
	{
		throw new UnsupportedOperationException();
	}

	// GLES used by texturemapping which is dead code and textureFillBackground, which can be dropped
	@Deprecated
	private void disableAttribFor2D(GL gl)
	{
		throw new UnsupportedOperationException();
	}

	// GLES only called by textureFillRaster which will be dropped 
	@Deprecated
	private void disableAttribForRaster(GL gl)
	{
		throw new UnsupportedOperationException();
	}

	// ---------------------------------------------------------------------

}
