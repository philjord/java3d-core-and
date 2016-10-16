package javax.media.j3d;

import java.nio.FloatBuffer;

/**
 * Separated from the actual pipeline for clarity of features that are not supported from the 
 * Pipeline class for the JOGL rendering pipeline.
 */
abstract class JoglesDEPPipeline extends Pipeline
{

	public static final String VALID_FORMAT_MESSAGE = "The Gl2ES2 pipeline only supports a subset of the Geometry data types and formats. \n"//
			+ "You can now only pass in a TriangleArray, TriangleStripArray, TriangleFanArray, \n"//
			+ "LineArray, LineStripArray, PointArray and the 6 Indexed equivilents (effectively QuadArray is removed). \n"//
			+ "Each Geomtry must have a format of GeometryArray.BY_REFERENCE = true and GeometryArray.INTERLEAVED = false. \n"//					
			+ "Texture Coordinate generation is not supported, Texture Filter, Sharpen and combine are not supported. \n"//
			+ "Texture3D, TextureCubeMap are not supported. \n"//
			+ "Off screen buffers and decals are also not supported. \n"//
			+ "Coordinates must be defind and float type, colors if defined must be float type. \n"//
			+ "It is strongly recomended that you use the format GeometryArray.USE_NIO_BUFFER = true.";//

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
	@Override
	@Deprecated
	void execute(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texUnitOffset, int numActiveTexUnitState, int vertexAttrCount, int[] vertexAttrSizes,
			float[] varray, float[] carray, int cDirty)
	{
		throw new UnsupportedOperationException(
				"Use of GeometryArrays (un-indexed) by Copy or interleaved not allowed.\n" + VALID_FORMAT_MESSAGE);
	}

	// used by GeometryArray by Reference in interleaved format with NIO buffer
	@Override
	@Deprecated
	void executeInterleavedBuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texUnitOffset, int numActiveTexUnit, FloatBuffer varray, float[] cdata, int cdirty)
	{
		throw new UnsupportedOperationException(
				"Use of GeometryArray (un-indexed) by Reference in interleaved format with NIO buffer.\n" + VALID_FORMAT_MESSAGE);
	}

	// used for GeometryArrays (PJ - I presume this means DList usage?)
	@Override
	@Deprecated
	void buildGA(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha, float alpha,
			boolean ignoreVertexColors, int startVIndex, int vcount, int vformat, int texCoordSetCount, int[] texCoordSetMap,
			int texCoordSetMapLen, int[] texCoordSetMapOffset, int vertexAttrCount, int[] vertexAttrSizes, double[] xform, double[] nxform,
			float[] varray)
	{
		throw new UnsupportedOperationException("DLists in use!.\n" + VALID_FORMAT_MESSAGE);
	}

	// used to Build Dlist GeometryArray by Reference with java arrays
	@Override
	@Deprecated
	void buildGAForByRef(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha, float alpha,
			boolean ignoreVertexColors, int vcount, int vformat, int vdefined, int initialCoordIndex, float[] vfcoords, double[] vdcoords,
			int initialColorIndex, float[] cfdata, byte[] cbdata, int initialNormalIndex, float[] ndata, int vertexAttrCount,
			int[] vertexAttrSizes, int[] vertexAttrIndices, float[][] vertexAttrData, int texCoordMapLength, int[] tcoordsetmap,
			int[] texIndices, int texStride, Object[] texCoords, double[] xform, double[] nxform)
	{
		throw new UnsupportedOperationException("DLists in use!.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// IndexedGeometryArrayRetained methods
	//

	// by-copy or interleaved, by reference, Java arrays
	@Override
	@Deprecated
	void executeIndexedGeometry(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int initialIndexIndex, int indexCount, int vertexCount, int vformat, int vertexAttrCount,
			int[] vertexAttrSizes, int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen, int[] texCoordSetOffset,
			int numActiveTexUnitState, float[] varray, float[] carray, int cdirty, int[] indexCoord)
	{
		throw new UnsupportedOperationException(
				"Use of IndexedGeometry by-copy or interleaved, by reference, Java arrays.\n" + VALID_FORMAT_MESSAGE);
	}

	// interleaved, by reference, nio buffer
	@Override
	@Deprecated
	void executeIndexedGeometryBuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean useAlpha,
			boolean ignoreVertexColors, int initialIndexIndex, int indexCount, int vertexCount, int vformat, int texCoordSetCount,
			int[] texCoordSetMap, int texCoordSetMapLen, int[] texCoordSetOffset, int numActiveTexUnitState, FloatBuffer vdata,
			float[] carray, int cDirty, int[] indexCoord)
	{
		throw new UnsupportedOperationException("Use of IndexedGeometry interleaved, by reference, nio buffer.\n" + VALID_FORMAT_MESSAGE);
	}

	//PJ presume the word build means a draw list?
	// by-copy geometry
	@Override
	@Deprecated
	void buildIndexedGeometry(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean updateAlpha,
			float alpha, boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount, int vertexCount, int vformat,
			int vertexAttrCount, int[] vertexAttrSizes, int texCoordSetCount, int[] texCoordSetMap, int texCoordSetMapLen,
			int[] texCoordSetMapOffset, double[] xform, double[] nxform, float[] varray, int[] indexCoord)
	{
		throw new UnsupportedOperationException("DLists in use!.\n" + VALID_FORMAT_MESSAGE);
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

		throw new UnsupportedOperationException("Read Raster call not support in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// ModelClipRetained methods
	//
	@Override
	@Deprecated
	void updateModelClip(Context ctx, int planeNum, boolean enableFlag, double A, double B, double C, double D)
	{
		throw new UnsupportedOperationException("Model Clip call not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
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
	@Override
	@Deprecated
	void updateTexCoordGeneration(Context ctx, boolean enable, int genMode, int format, float planeSx, float planeSy, float planeSz,
			float planeSw, float planeTx, float planeTy, float planeTz, float planeTw, float planeRx, float planeRy, float planeRz,
			float planeRw, float planeQx, float planeQy, float planeQz, float planeQw, double[] vworldToEc)
	{
		throw new UnsupportedOperationException(
				"Texture Coordinate generation is not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// TextureAttributesRetained methods
	//
	@Override
	@Deprecated
	void updateRegisterCombiners(Context absCtx, double[] transform, boolean isIdentity, int textureMode, int perspCorrectionMode,
			float textureBlendColorRed, float textureBlendColorGreen, float textureBlendColorBlue, float textureBlendColorAlpha,
			int textureFormat, int combineRgbMode, int combineAlphaMode, int[] combineRgbSrc, int[] combineAlphaSrc, int[] combineRgbFcn,
			int[] combineAlphaFcn, int combineRgbScale, int combineAlphaScale)
	{
		throw new UnsupportedOperationException("RegisterCombiners is not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTextureColorTable(Context ctx, int numComponents, int colorTableSize, int[] textureColorTable)
	{
		throw new UnsupportedOperationException("TextureColorTable is not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateCombiner(Context ctx, int combineRgbMode, int combineAlphaMode, int[] combineRgbSrc, int[] combineAlphaSrc,
			int[] combineRgbFcn, int[] combineAlphaFcn, int combineRgbScale, int combineAlphaScale)
	{
		throw new UnsupportedOperationException("GL combine not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// TextureRetained methods
	// Texture2DRetained methods

	@Override
	@Deprecated
	void updateTexture2DLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture2DSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture2DFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// Texture3DRetained methods
	//
	@Override
	@Deprecated
	void bindTexture3D(Context ctx, int objectId, boolean enable)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DImage(Context ctx, int numLevels, int level, int textureFormat, int imageFormat, int width, int height, int depth,
			int boundaryWidth, int dataType, Object data, boolean useAutoMipMap)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DSubImage(Context ctx, int level, int xoffset, int yoffset, int zoffset, int textureFormat, int imageFormat,
			int imgXOffset, int imgYOffset, int imgZOffset, int tilew, int tileh, int width, int height, int depth, int dataType,
			Object data, boolean useAutoMipMap)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DLodRange(Context ctx, int baseLevel, int maximumLevel, float minimumLod, float maximumLod)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DBoundary(Context ctx, int boundaryModeS, int boundaryModeT, int boundaryModeR, float boundaryRed,
			float boundaryGreen, float boundaryBlue, float boundaryAlpha)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DFilterModes(Context ctx, int minFilter, int magFilter)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTexture3DAnisotropicFilter(Context ctx, float degree)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// TextureCubeMapRetained methods

	@Override
	@Deprecated
	void updateTextureCubeMapLodOffset(Context ctx, float lodOffsetS, float lodOffsetT, float lodOffsetR)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTextureCubeMapSharpenFunc(Context ctx, int numSharpenTextureFuncPts, float[] sharpenTextureFuncPts)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void updateTextureCubeMapFilter4Func(Context ctx, int numFilter4FuncPts, float[] filter4FuncPts)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// ---------------------------------------------------------------------

	//
	// Canvas3D methods - native wrappers
	//

	@Override
	@Deprecated
	void createQueryContext(Canvas3D cv, Drawable drawable, boolean offScreen, int width, int height)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// This is the native for creating an offscreen buffer
	@Override
	@Deprecated
	Drawable createOffScreenBuffer(Canvas3D cv, Context ctx, int width, int height)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// 'destroyContext' is called first if context exists
	@Override
	@Deprecated
	void destroyOffScreenBuffer(Canvas3D cv, Context ctx, Drawable drawable)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// This is the native for reading the image from the offscreen buffer
	@Override
	@Deprecated
	void readOffScreenBuffer(Canvas3D cv, Context ctx, int format, int dataType, Object data, int width, int height)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// This is the native method for doing accumulation.
	@Override
	@Deprecated
	void accum(Context ctx, float value)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void accumReturn(Context ctx)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void clearAccum(Context ctx)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	boolean decal1stChildSetup(Context ctx)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void decalNthChildSetup(Context ctx)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void decalReset(Context ctx, boolean depthBufferEnable)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	// The following three methods are used in multi-pass case

	@Override
	@Deprecated
	void textureFillBackground(Context ctx, float texMinU, float texMaxU, float texMinV, float texMaxV, float mapMinX, float mapMaxX,
			float mapMinY, float mapMaxY, boolean useBilinearFilter)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);

	}

	@Override
	@Deprecated
	void textureFillRaster(Context ctx, float texMinU, float texMaxU, float texMinV, float texMaxV, float mapMinX, float mapMaxX,
			float mapMinY, float mapMaxY, float mapZ, float alpha, boolean useBilinearFilter)
	{

		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void executeRasterDepth(Context ctx, float posX, float posY, float posZ, int srcOffsetX, int srcOffsetY, int rasterWidth,
			int rasterHeight, int depthWidth, int depthHeight, int depthFormat, Object depthData)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);

	}

	// used for display Lists
	@Override
	@Deprecated
	void newDisplayList(Context ctx, int displayListId)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void endDisplayList(Context ctx)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void callDisplayList(Context ctx, int id, boolean isNonUniformScale)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	void freeDisplayList(Context ctx, int id)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	// nothing seems to call this in Canvas3D either
	void texturemapping(Context ctx, int px, int py, int minX, int minY, int maxX, int maxY, int texWidth, int texHeight, int rasWidth,
			int format, int objectId, byte[] imageYdown, int winWidth, int winHeight)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}

	@Override
	@Deprecated
	// nothing seems to call this in Canvas3D either
	boolean initTexturemapping(Context ctx, int texWidth, int texHeight, int objectId)
	{
		throw new UnsupportedOperationException("Not supported in the GL2ES2 pipeline.\n" + VALID_FORMAT_MESSAGE);
	}
}
