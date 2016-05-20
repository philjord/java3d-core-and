package javax.media.j3d;

import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.j3d.JoglesContext.GeometryData;
import javax.media.j3d.JoglesContext.LightData;
import javax.media.j3d.JoglesContext.LocationData;
import javax.media.j3d.JoglesContext.ProgramData;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector4f;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLFBODrawable;
import com.jogamp.opengl.Threading;

import java2.awt.GraphicsConfiguration;
import java2.awt.GraphicsDevice;
import utils.SparseArray;

/**
 * Concrete implementation of Pipeline class for the JOGL rendering
 * pipeline.
 */
class JoglesPipeline extends JoglesDEPPipeline
{
	private static final boolean DO_OUTPUT_ERRORS = false;
	// Currently prints for entry points already implemented
	static final boolean VERBOSE = false;

	private static final boolean OUTPUT_PER_FRAME_STATS = false;

	private static final boolean MINIMISE_NATIVE_CALLS_FFP = true;// this may not help dues to equals and set calls being as costly
	private static final boolean MINIMISE_NATIVE_CALLS_TRANSPARENCY = true;
	private static final boolean MINIMISE_NATIVE_CALLS_TEXTURE = true;

	private static final boolean MINIMISE_NATIVE_SHADER = true;
	private static final boolean MINIMISE_NATIVE_CALLS_OTHER = true;

	//crazy new ffp buffer weird ness, online evidence suggest no benefit
	private static final boolean ATTEMPT_UBO = false;// if you change this, change the shaders too
	private static final boolean PRESUME_INDICES = true;// only relevant if UBO above true

	// interleave and compressed to half floats and bytes
	private static final boolean ATTEMPT_OPTIMIZED_VERTICES = true;
	private static final boolean COMPRESS_OPTIMIZED_VERTICES = true;

	//This MUST be true on android fullscreen 
	//setPostiion on NifDispaly locks up if true

	private static final boolean NEVER_RELEASE_CONTEXT = true;

	/**
	 * Constructor for singleton JoglPipeline instance
	 */
	protected JoglesPipeline()
	{

	}

	/**
	 * Initialize the pipeline
	 */
	@Override
	void initialize(Pipeline.Type pipelineType)
	{
		super.initialize(pipelineType);

		// Java3D maintains strict control over which threads perform OpenGL work
		Threading.disableSingleThreading();

		//profile = GLProfile.get(GLProfile.GL2GL3);
		//profile = GLProfile.get(GLProfile.GL2ES2);
	}

	// ---------------------------------------------------------------------

	//
	// GeometryArrayRetained methods
	//

	//FIXME: big ugly hack for buffer clearing on removal of a geometry
	public void registerClearBuffers(Context ctx, GeometryArrayRetained geo)
	{
		JoglesContext joglesctx = (JoglesContext) ctx;
		synchronized (joglesctx.geoToClearBuffers)
		{
			joglesctx.geoToClearBuffers.add(geo);
		}
	}

	private static void doClearBuffers(Context ctx)
	{
		JoglesContext joglesctx = (JoglesContext) ctx;
		GL2ES2 gl = joglesctx.gl2es2;
		if (joglesctx.geoToClearBuffers.size() > 0)
		{
			synchronized (joglesctx.geoToClearBuffers)
			{
				for (GeometryArrayRetained geo : joglesctx.geoToClearBuffers)
				{
					GeometryData gd = joglesctx.allGeometryData.get(geo.nativeId);
					joglesctx.allGeometryData.remove(geo.nativeId);
					geo.nativeId = -1;

					//TODO: why exactly is the same geo being removed twice?
					if (gd != null)
					{
						if (gd.geoToIndBuf != -1)
							gl.glDeleteBuffers(1, new int[] { gd.geoToIndBuf }, 0);
						if (gd.geoToCoordBuf != -1)
							gl.glDeleteBuffers(1, new int[] { gd.geoToCoordBuf }, 0);
						if (gd.geoToColorBuf != -1)
							gl.glDeleteBuffers(1, new int[] { gd.geoToColorBuf }, 0);
						if (gd.geoToNormalBuf != -1)
							gl.glDeleteBuffers(1, new int[] { gd.geoToNormalBuf }, 0);

						int[] bufIds = gd.geoToIndStripBuf;
						if (bufIds != null && bufIds.length > 0)
						{
							gl.glDeleteBuffers(bufIds.length, bufIds, 0);
						}

						SparseArray<Integer> tcBufIds = gd.geoToTexCoordsBuf;
						if (tcBufIds != null)
						{
							for (int i = 0; i < tcBufIds.size(); i++)
							{
								Integer tcBufId = tcBufIds.get(tcBufIds.keyAt(i));

								if (tcBufId != null)
									gl.glDeleteBuffers(1, new int[] { tcBufId.intValue() }, 0);
							}
							tcBufIds.clear();
						}

						SparseArray<Integer> vaBufIds = gd.geoToVertAttribBuf;
						if (vaBufIds != null)
						{
							for (int i = 0; i < vaBufIds.size(); i++)
							{
								Integer vaBufId = vaBufIds.get(vaBufIds.keyAt(i));
								if (vaBufId != null)
									gl.glDeleteBuffers(1, new int[] { vaBufId.intValue() }, 0);
							}
							vaBufIds.clear();
						}

						if (gd.interleavedBufId != -1)
							gl.glDeleteBuffers(1, new int[] { gd.interleavedBufId }, 0);

						if (gd.vaoId != -1)
							((GL2ES3) gl).glDeleteVertexArrays(1, new int[] { gd.vaoId }, 0);

					}

				}

				if (OUTPUT_PER_FRAME_STATS)
					joglesctx.perFrameStats.geoToClearBuffers = joglesctx.geoToClearBuffers.size();

				joglesctx.geoToClearBuffers.clear();

				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}
	}

	// used by GeometryArray by Reference with NIO buffer
	//looks like skybox is using this at least, it is cases where nio and by_ref but non indexed

	@Override
	void executeVABuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale, boolean ignoreVertexColors,
			int vcount, int vformat, int vdefined, int initialCoordIndex, Buffer vcoords, int initialColorIndex, Buffer cdataBuffer,
			float[] cfdata, byte[] cbdata, int initialNormalIndex, FloatBuffer ndata, int vertexAttrCount, int[] vertexAttrSizes,
			int[] vertexAttrIndices, FloatBuffer[] vertexAttrData, int texCoordMapLength, int[] texcoordoffset, int numActiveTexUnitState,
			int[] texIndex, int texstride, Object[] texCoords, int cdirty)
	{

		if (VERBOSE)
			System.err.println("JoglPipeline.executeVABuffer() ");

		boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
		boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
		boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
		boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
		boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
		boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
		boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

		FloatBuffer fverts = null;
		DoubleBuffer dverts = null;
		FloatBuffer fclrs = null;
		ByteBuffer bclrs = null;

		FloatBuffer norms = null;
		FloatBuffer[] vertexAttrBufs = null;

		// Get vertex attribute arrays
		if (vattrDefined)
			vertexAttrBufs = vertexAttrData;

		// get coordinate array
		if (floatCoordDefined)
		{
			fverts = (FloatBuffer) vcoords;
		}
		else if (doubleCoordDefined)
		{
			//FIXME: doubles not supported for now
			throw new UnsupportedOperationException();
			//dverts = (DoubleBuffer) vcoords;
		}

		if (fverts == null && dverts == null)
		{
			return;
		}

		// get color array
		if (floatColorsDefined)
		{
			if (cdataBuffer != null)
				fclrs = (FloatBuffer) cdataBuffer;
			else
				fclrs = getColorArrayBuffer(cfdata);
		}
		else if (byteColorsDefined)
		{
			//FIXME: doubles not supported for now
			throw new UnsupportedOperationException();
			//if (cbdata != null)
			//	bclrs = getColorArrayBuffer(cbdata);
			//else
			//	bclrs = (ByteBuffer) cdataBuffer;
		}

		// get normal array
		if (normalsDefined)
		{
			norms = ndata;
		}

		int[] sarray = null;
		int[] start_array = null;
		int strip_len = 0;
		if (geo_type == GeometryRetained.GEO_TYPE_TRI_STRIP_SET || geo_type == GeometryRetained.GEO_TYPE_TRI_FAN_SET
				|| geo_type == GeometryRetained.GEO_TYPE_LINE_STRIP_SET)
		{
			sarray = ((GeometryStripArrayRetained) geo).stripVertexCounts;
			strip_len = sarray.length;
			start_array = ((GeometryStripArrayRetained) geo).stripStartOffsetIndices;
		}

		executeGeometryArrayVA(ctx, geo, geo_type, isNonUniformScale, ignoreVertexColors, vcount, vformat, vdefined, initialCoordIndex,
				fverts, dverts, initialColorIndex, fclrs, bclrs, initialNormalIndex, norms, vertexAttrCount, vertexAttrSizes,
				vertexAttrIndices, vertexAttrBufs, texCoordMapLength, texcoordoffset, numActiveTexUnitState, texIndex, texstride, texCoords,
				cdirty, sarray, strip_len, start_array);
	}

	private void executeGeometryArrayVA(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
			boolean ignoreVertexColors, int vertexCount, int vformat, int vdefined, int initialCoordIndex, FloatBuffer fverts,
			DoubleBuffer dverts, int initialColorIndex, FloatBuffer fclrs, ByteBuffer bclrs, int initialNormalIndex, FloatBuffer norms,
			int vertexAttrCount, int[] vertexAttrSizes, int[] vertexAttrIndices, FloatBuffer[] vertexAttrBufs, int texCoordMapLength,
			int[] texCoordSetMap, int numActiveTexUnitState, int[] texindices, int texStride, Object[] texCoords, int cDirty, int[] sarray,
			int strip_len, int[] start_array)
	{
		JoglesContext ctx = (JoglesContext) absCtx;
		int shaderProgramId = ctx.shaderProgramId;

		if (shaderProgramId != -1)
		{
			GL2ES2 gl = ctx.gl2es2;
			ProgramData pd = ctx.programData;
			LocationData locs = pd.programToLocationData;

			setFFPAttributes(ctx, gl, shaderProgramId, pd, vdefined);

			//If any buffers need loading do that now and skip a render for this frame
			GeometryData gd = loadAllBuffers(ctx, gl, geo, ignoreVertexColors, vertexCount, vformat, vdefined, fverts, dverts, fclrs, bclrs,
					norms, vertexAttrCount, vertexAttrSizes, vertexAttrBufs, texCoordMapLength, texCoordSetMap, texStride, texCoords);

			boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
			boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
			boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
			boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
			boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
			boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
			boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

			// not required second time around for VAO
			boolean bindingRequired = true;
			if (ctx.gl2es3 != null)
			{
				if (gd.vaoId == -1)
				{
					int[] tmp = new int[1];
					ctx.gl2es3.glGenVertexArrays(1, tmp, 0);
					gd.vaoId = tmp[0];
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
				else
				{
					bindingRequired = false;
				}
				ctx.gl2es3.glBindVertexArray(gd.vaoId);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

			// Define the data pointers
			if (floatCoordDefined && locs.glVertex != -1)
			{
				//can it change ever? (GeometryArray.ALLOW_REF_DATA_WRITE is just my indicator of this feature)			 
				boolean morphable = ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_REF_DATA_WRITE)
						|| ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

				if (gd.geoToCoordBuf == -1)
				{
					new Throwable("Buffer load issue!").printStackTrace();
				}
				else
				{

					//a good cDirty  
					//if ((cDirty & GeometryArrayRetained.COORDINATE_CHANGED) != 0)
					if (morphable)
					{
						int coordoff = 3 * initialCoordIndex;
						fverts.position(coordoff);
						//Sometime the FloatBuffer is swapped out for bigger or smaller! or is that ok?
						if (gd.geoToCoordBufSize != fverts.remaining())
						{
							System.err.println("Morphable buffer changed " + gd.geoToCoordBufSize + " != " + fverts.remaining()
									+ " un indexed ((GeometryArray) geo.source) " + ((GeometryArray) geo.source).getName() + " "
									+ geo.source);

							int prevBufId1 = gd.geoToCoordBuf1;//record to delete after re-bind		
							int prevBufId2 = gd.geoToCoordBuf2;

							int[] tmp = new int[2];
							gl.glGenBuffers(2, tmp, 0);
							gd.geoToCoordBuf = tmp[0];
							gd.geoToCoordBuf1 = tmp[0];
							gd.geoToCoordBuf2 = tmp[1];

							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf1);							
							gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts,  GL2ES2.GL_DYNAMIC_DRAW);
							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf2);
							gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts, GL2ES2.GL_DYNAMIC_DRAW);

							gd.geoToCoordBufSize = fverts.remaining();

							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glBufferData++;

							//Notice no check for bindingRequired as we are altering the binding
							//and previously used buffer is deleted AFTER re-bind

							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf);
							gl.glVertexAttribPointer(locs.glVertex, 3, GL2ES2.GL_FLOAT, false, 0, 0);
							gl.glEnableVertexAttribArray(locs.glVertex);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glVertexAttribPointerCoord++;

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.coordCount += gd.geoToCoordBufSize;

							gl.glDeleteBuffers(1, new int[] { prevBufId1,prevBufId2 }, 0);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);
						}
						else
						{
							//work out the buffer to update and buffer to swap to
							if (gd.geoToCoordBuf == gd.geoToCoordBuf1)
							{
								// update 1 but set to draw 2
								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf1);
								gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, (fverts.remaining() * Float.SIZE / 8), fverts);
								gd.geoToCoordBuf = gd.geoToCoordBuf2;
							}
							else
							{
								// update 2 but set to draw 1
								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf2);
								gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, (fverts.remaining() * Float.SIZE / 8), fverts);
								gd.geoToCoordBuf = gd.geoToCoordBuf1;
							}

							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glBufferSubData++;
						}
					}

					if (bindingRequired)
					{
						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf);
						gl.glVertexAttribPointer(locs.glVertex, 3, GL2ES2.GL_FLOAT, false, 0, 0);
						gl.glEnableVertexAttribArray(locs.glVertex);
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glVertexAttribPointerCoord++;
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.coordCount += gd.geoToCoordBufSize;
					}
				}

			}
			else if (doubleCoordDefined)
			{
				throw new UnsupportedOperationException();
			}
			else
			{
				throw new UnsupportedOperationException("No coords!");
			}

			if (bindingRequired)
			{
				if (floatColorsDefined && locs.glColor != -1 && !ignoreVertexColors)
				{
					if (gd.geoToColorBuf == -1)
					{
						new Throwable("Buffer load issue!").printStackTrace();
					}
					else
					{
						int coloroff;
						int sz = ((vformat & GeometryArray.WITH_ALPHA) != 0) ? 4 : 3;

						coloroff = sz * initialColorIndex;
						fclrs.position(coloroff);

						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToColorBuf);
						//a good cDirty and a DYNAMIC_DRAW call needed
						/*if ((cDirty & GeometryArrayRetained.COLOR_CHANGED) != 0)
						{							
							gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, fclrs.remaining() * Float.SIZE / 8, fclrs);
						}*/

						gl.glVertexAttribPointer(locs.glColor, sz, GL2ES2.GL_FLOAT, false, 0, 0);
						gl.glEnableVertexAttribArray(locs.glColor);//must be called after Pointer above
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glVertexAttribPointerColor++;
					}
				}
				else if (byteColorsDefined && locs.glColor != -1 && !ignoreVertexColors)
				{
					//FIXME: byteColors not supported for now, but I want them a lot
					throw new UnsupportedOperationException();
					/*int coloroff;
					int sz;
					if ((vformat & GeometryArray.WITH_ALPHA) != 0)
					{
						coloroff = 4 * initialColorIndex;
						sz = 4;
					}
					else
					{
						coloroff = 3 * initialColorIndex;
						sz = 3;
					}
					bclrs.position(coloroff);
					gl.glColorPointer(sz, GL2ES2.GL_UNSIGNED_BYTE, 0, bclrs);*/
				}
				else if (locs.glColor != -1)
				{
					// ignoreVertexcolors willhave been set in FFP now as the glColors is unbound
					gl.glDisableVertexAttribArray(locs.glColor);
					if (OUTPUT_PER_FRAME_STATS)
						ctx.perFrameStats.glDisableVertexAttribArray++;
				}

				if (normalsDefined && locs.glNormal != -1)
				{
					if (gd.geoToNormalBuf == -1)
					{
						new Throwable("Buffer load issue!").printStackTrace();
					}
					else
					{
						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToNormalBuf);
						//a good cDirty and a DYNAMIC_DRAW call needed
						/*if ((cDirty & GeometryArrayRetained.NORMAL_CHANGED) != 0)
						{			
							int normoff = 3 * initialNormalIndex;
							norms.position(normoff);				
							gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, norms.remaining() * Float.SIZE / 8, norms);
						}*/

						gl.glVertexAttribPointer(locs.glNormal, 3, GL2ES2.GL_FLOAT, false, 0, 0);
						gl.glEnableVertexAttribArray(locs.glNormal);//must be called after Pointer above
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glVertexAttribPointerNormals++;
					}
				}
				else
				{
					if (locs.glNormal != -1)
					{
						gl.glDisableVertexAttribArray(locs.glNormal);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}

				if (vattrDefined)
				{
					for (int index = 0; index < vertexAttrCount; index++)
					{
						Integer attribLoc = locs.genAttIndexToLoc.get(index);
						if (attribLoc != null && attribLoc.intValue() != -1)
						{
							SparseArray<Integer> bufIds = gd.geoToVertAttribBuf;
							if (bufIds == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}

							Integer bufId = bufIds.get(index);
							if (bufId == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}
							else
							{
								int sz = vertexAttrSizes[index];

								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, bufId.intValue());
								//a good cDirty and a DYNAMIC_DRAW call needed
								/*if ((cDirty & GeometryArrayRetained.VATTR_CHANGED) != 0)
								{
									FloatBuffer vertexAttrs = vertexAttrBufs[index];
									vertexAttrs.position(0);
									gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, vertexAttrs.remaining() * Float.SIZE / 8, vertexAttrs);
								}*/

								gl.glVertexAttribPointer(attribLoc.intValue(), sz, GL2ES2.GL_FLOAT, false, 0, 0);
								gl.glEnableVertexAttribArray(attribLoc.intValue());//must be called after Pointer above
								if (DO_OUTPUT_ERRORS)
									outputErrors(ctx);

								if (OUTPUT_PER_FRAME_STATS)
									ctx.perFrameStats.glVertexAttribPointerUserAttribs++;
							}
						}
					}
				}

				if (textureDefined)
				{
					boolean[] texSetsBound = new boolean[texCoords.length];
					for (int texUnit = 0; texUnit < numActiveTexUnitState && texUnit < texCoordMapLength; texUnit++)
					{
						int texSet = texCoordSetMap[texUnit];
						if (texSet != -1 && locs.glMultiTexCoord[texSet] != -1 && !texSetsBound[texSet])
						{
							texSetsBound[texSet] = true;
							//stupid interface...
							FloatBuffer buf = (FloatBuffer) texCoords[texSet];
							buf.position(0);

							SparseArray<Integer> bufIds = gd.geoToTexCoordsBuf;
							if (bufIds == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}
							Integer bufId = bufIds.get(texUnit);
							if (bufId == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}
							else
							{

								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, bufId.intValue());
								gl.glVertexAttribPointer(locs.glMultiTexCoord[texUnit], texStride, GL2ES2.GL_FLOAT, true, 0, 0);
								gl.glEnableVertexAttribArray(locs.glMultiTexCoord[texUnit]);
								if (DO_OUTPUT_ERRORS)
									outputErrors(ctx);

								if (OUTPUT_PER_FRAME_STATS)
									ctx.perFrameStats.enableTexCoordPointer++;

							}
						}
					}

				}
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

			if (geo_type == GeometryRetained.GEO_TYPE_TRI_STRIP_SET || geo_type == GeometryRetained.GEO_TYPE_TRI_FAN_SET
					|| geo_type == GeometryRetained.GEO_TYPE_LINE_STRIP_SET)
			{
				int primType = 0;

				//<AND> need to override if polygonAttributes says so
				//FIXME: GL_LINE and GL_LINE_STRIP simply go from one vertex to the next drawing a line between
				// each pair, what I want is a line between each set of 3 (that are not jumpers)
				// so H-Physics and Outlines look a bit rubbish

				if (ctx.polygonMode == PolygonAttributes.POLYGON_LINE)
					geo_type = GeometryRetained.GEO_TYPE_LINE_STRIP_SET;

				switch (geo_type)
				{
				case GeometryRetained.GEO_TYPE_TRI_STRIP_SET:
					primType = GL2ES2.GL_TRIANGLE_STRIP;
					break;
				case GeometryRetained.GEO_TYPE_TRI_FAN_SET:
					primType = GL2ES2.GL_TRIANGLE_FAN;
					break;
				case GeometryRetained.GEO_TYPE_LINE_STRIP_SET:
					primType = GL2ES2.GL_LINE_LOOP;
					break;
				}

				for (int i = 0; i < strip_len; i++)
				{
					if (sarray[i] > 0)
					{
						gl.glDrawArrays(primType, start_array[i], sarray[i]);
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDrawStripArraysStrips++;
					}
				}
				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glDrawStripArrays++;
			}
			else
			{
				//need to override if polygonAttributes says so

				if (ctx.polygonMode == PolygonAttributes.POLYGON_LINE)
					geo_type = GeometryRetained.GEO_TYPE_LINE_SET;
				else if (ctx.polygonMode == PolygonAttributes.POLYGON_POINT)
					geo_type = GeometryRetained.GEO_TYPE_POINT_SET;

				switch (geo_type)
				{
				case GeometryRetained.GEO_TYPE_QUAD_SET:
					//gl.glDrawArrays(GL2ES2.GL_QUADS, 0, vertexCount);
					break;
				case GeometryRetained.GEO_TYPE_TRI_SET:
					gl.glDrawArrays(GL2ES2.GL_TRIANGLES, 0, vertexCount);
					break;
				case GeometryRetained.GEO_TYPE_POINT_SET:
					gl.glDrawArrays(GL2ES2.GL_POINTS, 0, vertexCount);
					break;
				case GeometryRetained.GEO_TYPE_LINE_SET:
					gl.glDrawArrays(GL2ES2.GL_LINES, 0, vertexCount);
					break;
				}
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glDrawArrays++;
			}

			/*		unbound in setRenderMode now	
			 			if (gl.isGL2ES3())
						{
							GL2ES3 gl2es3 = (GL2ES3) gl;
							gl2es3.glBindVertexArray(0);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);
						}*/

			//TODO: do I need these?
			/*
			if (vattrDefined)
			{
				for (int i = 0; i < vertexAttrCount; i++)
				{
					Integer attribLoc = locs.genAttIndexToLoc.get(i);
					if (attribLoc != null && attribLoc.intValue() != -1)
					{
						gl.glDisableVertexAttribArray(attribLoc);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}
			}
			
			if (textureDefined)
			{
				for (int i = 0; i < locs.glMultiTexCoord.length; i++)
				{
					if (locs.glMultiTexCoord[i] != -1)
					{
						gl.glDisableVertexAttribArray(locs.glMultiTexCoord[i]);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}
			}*/
		}
		else
		{
			if (!NO_PROGRAM_WARNING_GIVEN)
				System.err.println("Execute called with no shader Program in use!");
			NO_PROGRAM_WARNING_GIVEN = true;
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

	}

	// ---------------------------------------------------------------------

	//
	// IndexedGeometryArrayRetained methods
	//

	// non interleaved, by reference, Java arrays

	//Possibly not used anymore??
	@Override
	void executeIndexedGeometryVA(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
			boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount, int vertexCount, int vformat, int vdefined,
			float[] vfcoords, double[] vdcoords, float[] cfdata, byte[] cbdata, float[] ndata, int vertexAttrCount, int[] vertexAttrSizes,
			float[][] vertexAttrData, int texCoordMapLength, int[] texcoordoffset, int numActiveTexUnitState, int texStride,
			Object[] texCoords, int cdirty, int[] indexCoord)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.executeIndexedGeometryVA()");

		boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
		boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
		boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
		boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
		boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
		boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
		boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

		FloatBuffer fverts = null;
		DoubleBuffer dverts = null;
		FloatBuffer fclrs = null;
		ByteBuffer bclrs = null;
		FloatBuffer[] texCoordBufs = null;
		FloatBuffer norms = null;
		FloatBuffer[] vertexAttrBufs = null;

		// Get vertex attribute arrays
		if (vattrDefined)
		{
			vertexAttrBufs = getVertexAttrSetBuffer(vertexAttrData);
		}

		// get texture arrays
		if (textureDefined)
		{
			texCoordBufs = getTexCoordSetBuffer(texCoords);
		}

		int[] sarray = null;
		int strip_len = 0;
		if (geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_STRIP_SET || geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_FAN_SET
				|| geo_type == GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET)
		{
			sarray = ((IndexedGeometryStripArrayRetained) geo).stripIndexCounts;
			strip_len = sarray.length;
		}

		// get coordinate array
		if (floatCoordDefined)
		{
			fverts = getVertexArrayBuffer(vfcoords);
		}
		else if (doubleCoordDefined)
		{
			//FIXME: doubles not supported for now
			throw new UnsupportedOperationException();
			//dverts = getVertexArrayBuffer(vdcoords);
		}

		// get color array
		if (floatColorsDefined)
		{
			fclrs = getColorArrayBuffer(cfdata);
		}
		else if (byteColorsDefined)
		{
			//FIXME: byte colors not supported for now
			throw new UnsupportedOperationException();
			//bclrs = getColorArrayBuffer(cbdata);
		}

		// get normal array
		if (normalsDefined)
		{
			norms = getNormalArrayBuffer(ndata);
		}

		executeIndexedGeometryArrayVA(ctx, geo, geo_type, isNonUniformScale, ignoreVertexColors, initialIndexIndex, validIndexCount,
				vertexCount, vformat, vdefined, fverts, dverts, fclrs, bclrs, norms, vertexAttrCount, vertexAttrSizes, vertexAttrBufs,
				texCoordMapLength, texcoordoffset, numActiveTexUnitState, texStride, texCoordBufs, cdirty, indexCoord, sarray, strip_len);
	}

	// non interleaved, by reference, nio buffer

	@Override
	void executeIndexedGeometryVABuffer(Context ctx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
			boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount, int vertexCount, int vformat, int vdefined,
			Buffer vcoords, Buffer cdataBuffer, float[] cfdata, byte[] cbdata, FloatBuffer ndata, int vertexAttrCount,
			int[] vertexAttrSizes, FloatBuffer[] vertexAttrData, int texCoordMapLength, int[] texcoordoffset, int numActiveTexUnitState,
			int texStride, Object[] texCoords, int cdirty, int[] indexCoord)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.executeIndexedGeometryVABuffer() ");

		boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
		boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
		boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
		boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
		boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
		boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
		boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

		FloatBuffer fverts = null;
		DoubleBuffer dverts = null;
		FloatBuffer fclrs = null;
		ByteBuffer bclrs = null;

		FloatBuffer norms = null;
		FloatBuffer[] vertexAttrBufs = null;

		// Get vertex attribute arrays
		if (vattrDefined)
		{
			vertexAttrBufs = vertexAttrData;
		}

		// get coordinate array
		if (floatCoordDefined)
		{
			fverts = (FloatBuffer) vcoords;
		}
		else if (doubleCoordDefined)
		{
			//FIXME: doubles not supported for now
			throw new UnsupportedOperationException();
			//dverts = (DoubleBuffer) vcoords;
		}

		if (fverts == null && dverts == null)
		{
			return;
		}

		int[] sarray = null;
		int strip_len = 0;
		if (geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_STRIP_SET || geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_FAN_SET
				|| geo_type == GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET)
		{
			sarray = ((IndexedGeometryStripArrayRetained) geo).stripIndexCounts;
			strip_len = sarray.length;
		}

		// get color array
		if (floatColorsDefined)
		{
			if (cdataBuffer != null)
				fclrs = (FloatBuffer) cdataBuffer;
			else
				fclrs = getColorArrayBuffer(cfdata);

		}
		else if (byteColorsDefined)
		{
			//FIXME: doubles not supported for now
			throw new UnsupportedOperationException();

			//if (cbdata != null)
			//	bclrs = getColorArrayBuffer(cbdata);
			//else
			//	bclrs = (ByteBuffer) cdataBuffer;
		}

		// get normal array
		if (normalsDefined)
		{
			norms = ndata;
		}

		executeIndexedGeometryArrayVA(ctx, geo, geo_type, isNonUniformScale, ignoreVertexColors, initialIndexIndex, validIndexCount,
				vertexCount, vformat, vdefined, fverts, dverts, fclrs, bclrs, norms, vertexAttrCount, vertexAttrSizes, vertexAttrBufs,
				texCoordMapLength, texcoordoffset, numActiveTexUnitState, texStride, texCoords, cdirty, indexCoord, sarray, strip_len);
	}

	//----------------------------------------------------------------------
	//
	// Helper routines for IndexedGeometryArrayRetained
	//
	//careful - isNonUniformScale is always false regardless
	private void executeIndexedGeometryArrayVA(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
			boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount, int vertexCount, int vformat, int vdefined,
			FloatBuffer fverts, DoubleBuffer dverts, FloatBuffer fclrs, ByteBuffer bclrs, FloatBuffer norms, int vertexAttrCount,
			int[] vertexAttrSizes, FloatBuffer[] vertexAttrBufs, int texCoordMapLength, int[] texCoordSetMap, int numActiveTexUnitState,
			int texStride, Object[] texCoords, int cDirty, int[] indexCoord, int[] sarray, int strip_len)
	{

		if (ATTEMPT_OPTIMIZED_VERTICES && executeIndexedGeometryOptimized(absCtx, geo, geo_type, isNonUniformScale, ignoreVertexColors,
				initialIndexIndex, validIndexCount, vertexCount, vformat, vdefined, fverts, dverts, fclrs, bclrs, norms, vertexAttrCount,
				vertexAttrSizes, vertexAttrBufs, texCoordMapLength, texCoordSetMap, numActiveTexUnitState, texStride, texCoords, cDirty,
				indexCoord, sarray, strip_len))
		{
			// on true execute has decided it is possible
			return;
		}

		JoglesContext ctx = (JoglesContext) absCtx;
		int shaderProgramId = ctx.shaderProgramId;

		if (shaderProgramId != -1)
		{
			GL2ES2 gl = ctx.gl2es2;
			ProgramData pd = ctx.programData;
			LocationData locs = pd.programToLocationData;

			setFFPAttributes(ctx, gl, shaderProgramId, pd, vdefined);

			//If any buffers need loading do that now and skip a render for this frame
			GeometryData gd = loadAllBuffers(ctx, gl, geo, ignoreVertexColors, vertexCount, vformat, vdefined, fverts, dverts, fclrs, bclrs,
					norms, vertexAttrCount, vertexAttrSizes, vertexAttrBufs, texCoordMapLength, texCoordSetMap, texStride, texCoords);

			boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
			boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
			boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
			boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
			boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
			boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
			boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

			// not required second time around for VAO
			boolean bindingRequired = true;
			if (ctx.gl2es3 != null)
			{
				if (gd.vaoId == -1)
				{
					int[] tmp = new int[1];
					ctx.gl2es3.glGenVertexArrays(1, tmp, 0);
					gd.vaoId = tmp[0];
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
				else
				{
					bindingRequired = false;
				}
				ctx.gl2es3.glBindVertexArray(gd.vaoId);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

			// Define the data pointers
			if (floatCoordDefined && locs.glVertex != -1)
			{
				//Building of buffers etc and index buffers should really take place not on the j3d thread if possible

				if (gd.geoToCoordBuf == -1)
				{
					new Throwable("Buffer load issue!").printStackTrace();
				}
				else
				{
					//can it change ever? (GeometryArray.ALLOW_REF_DATA_WRITE is just my indicator of this feature)			 
					boolean morphable = ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_REF_DATA_WRITE)
							|| ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

					if (morphable)
					{
						fverts.position(0);

						//Sometime the FloatBuffer is swapped out for bigger or smaller! or is that ok?
						if (gd.geoToCoordBufSize != fverts.remaining())
						{
							System.err.println("Morphable buffer changed " + gd.geoToCoordBufSize + " != " + fverts.remaining()
									+ " ((GeometryArray) geo.source) " + ((GeometryArray) geo.source).getName() + " " + geo.source);

							int prevBufId1 = gd.geoToCoordBuf1;//keep to delete below
							int prevBufId2 = gd.geoToCoordBuf2;//keep to delete below

							int[] tmp = new int[2];
							gl.glGenBuffers(2, tmp, 0);
							gd.geoToCoordBuf = tmp[0];
							gd.geoToCoordBuf1 = tmp[0];
							gd.geoToCoordBuf2 = tmp[1];

							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf1);
							gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts, GL2ES2.GL_STATIC_DRAW);

							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf2);
							gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts, GL2ES2.GL_STATIC_DRAW);

							gd.geoToCoordBufSize = fverts.remaining();

							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glBufferData++;

							//Notice no check for bindingRequired as we are altering the binding
							//and previously used buffer is deleted AFTER re-bind

							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf);
							gl.glVertexAttribPointer(locs.glVertex, 3, GL2ES2.GL_FLOAT, false, 0, 0);
							gl.glEnableVertexAttribArray(locs.glVertex);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glVertexAttribPointerCoord++;

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.coordCount += gd.geoToCoordBufSize;

							gl.glDeleteBuffers(1, new int[] { prevBufId1,prevBufId2 }, 0);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);
						}
						else
						{
							//work out the buffer to update and buffer to swap to
							if (gd.geoToCoordBuf == gd.geoToCoordBuf1)
							{
								// update 1 but set to draw 2
								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf1);
								gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, (fverts.remaining() * Float.SIZE / 8), fverts);
								gd.geoToCoordBuf = gd.geoToCoordBuf2;
							}
							else
							{
								// update 2 but set to draw 1
								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf2);
								gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, (fverts.remaining() * Float.SIZE / 8), fverts);
								gd.geoToCoordBuf = gd.geoToCoordBuf1;
							}

							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glBufferSubData++;
						}

						if (bindingRequired)
						{
							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf);
							gl.glVertexAttribPointer(locs.glVertex, 3, GL2ES2.GL_FLOAT, false, 0, 0);
							gl.glEnableVertexAttribArray(locs.glVertex);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glVertexAttribPointerCoord++;

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.coordCount += gd.geoToCoordBufSize;
						}
					}

				}
			}
			else if (doubleCoordDefined)
			{
				throw new UnsupportedOperationException();
			}
			else
			{
				throw new UnsupportedOperationException("No coords!");
			}

			if (bindingRequired)
			{
				if (floatColorsDefined && locs.glColor != -1 && !ignoreVertexColors)
				{
					if (gd.geoToColorBuf == -1)
					{
						new Throwable("Buffer load issue!").printStackTrace();
					}
					else
					{

						int sz = ((vformat & GeometryArray.WITH_ALPHA) != 0) ? 4 : 3;
						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToColorBuf);

						//a good cDirty and a DYNAMIC_DRAW call needed
						/*	if ((cDirty & GeometryArrayRetained.COLOR_CHANGED) != 0)
							{
								fclrs.position(0);
								gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, fclrs.remaining() * Float.SIZE / 8, fclrs);
							}*/

						gl.glVertexAttribPointer(locs.glColor, sz, GL2ES2.GL_FLOAT, true, 0, 0);
						gl.glEnableVertexAttribArray(locs.glColor);
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glVertexAttribPointerColor++;
					}

				}
				else if (byteColorsDefined && locs.glColor != -1 && !ignoreVertexColors)
				{
					//FIXME: byteColors not supported for now
					throw new UnsupportedOperationException();

					/*bclrs.position(0);
					if ((vformat & GeometryArray.WITH_ALPHA) != 0)
					{
						gl.glColorPointer(4, GL2ES2.GL_UNSIGNED_BYTE, 0, bclrs);
					}
					else
					{
						gl.glColorPointer(3, GL2ES2.GL_UNSIGNED_BYTE, 0, bclrs);
					}*/
				}
				else if (locs.glColor != -1)
				{
					// ignoreVertexcolors will be set in FFP now as the glColors is unbound
					gl.glDisableVertexAttribArray(locs.glColor);
					if (OUTPUT_PER_FRAME_STATS)
						ctx.perFrameStats.glDisableVertexAttribArray++;
				}

				if (normalsDefined)
				{
					if (locs.glNormal != -1)
					{
						if (gd.geoToNormalBuf == -1)
						{
							new Throwable("Buffer load issue!").printStackTrace();
						}
						else
						{
							gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToNormalBuf);
							//a good cDirty and a DYNAMIC_DRAW call needed
							/*if ((cDirty & GeometryArrayRetained.NORMAL_CHANGED) != 0)
							{	
								norms.position(0);						
								gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, norms.remaining() * Float.SIZE / 8, norms);
							}*/

							gl.glVertexAttribPointer(locs.glNormal, 3, GL2ES2.GL_FLOAT, true, 0, 0);
							gl.glEnableVertexAttribArray(locs.glNormal);//must be called after Pointer above
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);

							if (OUTPUT_PER_FRAME_STATS)
								ctx.perFrameStats.glVertexAttribPointerNormals++;
						}

					}
				}
				else
				{
					if (locs.glNormal != -1)
					{
						gl.glDisableVertexAttribArray(locs.glNormal);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}

				if (vattrDefined)
				{
					for (int index = 0; index < vertexAttrCount; index++)
					{
						Integer attribLoc = locs.genAttIndexToLoc.get(index);
						if (attribLoc != null && attribLoc.intValue() != -1)
						{
							SparseArray<Integer> bufIds = gd.geoToVertAttribBuf;
							if (bufIds == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}

							Integer bufId = bufIds.get(index);
							if (bufId == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}
							else
							{
								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, bufId.intValue());
								//a good cDirty and a DYNAMIC_DRAW call needed
								/*if ((cDirty & GeometryArrayRetained.VATTR_CHANGED) != 0)
								{
									FloatBuffer vertexAttrs = vertexAttrBufs[index];								
									vertexAttrs.position(0);
									gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, vertexAttrs.remaining() * Float.SIZE / 8, vertexAttrs);
								}*/

								int sz = vertexAttrSizes[index];

								gl.glVertexAttribPointer(attribLoc.intValue(), sz, GL2ES2.GL_FLOAT, false, 0, 0);
								gl.glEnableVertexAttribArray(attribLoc.intValue());//must be called after Pointer above
								if (DO_OUTPUT_ERRORS)
									outputErrors(ctx);

								if (OUTPUT_PER_FRAME_STATS)
									ctx.perFrameStats.glVertexAttribPointerUserAttribs++;
							}
						}
					}
				}

				if (textureDefined)
				{
					boolean[] texSetsBound = new boolean[texCoords.length];
					for (int texUnit = 0; texUnit < numActiveTexUnitState && texUnit < texCoordMapLength; texUnit++)
					{
						int texSet = texCoordSetMap[texUnit];
						if (texSet != -1 && locs.glMultiTexCoord[texSet] != -1 && !texSetsBound[texSet])
						{
							texSetsBound[texSet] = true;
							//stupid interface...
							FloatBuffer buf = (FloatBuffer) texCoords[texSet];
							buf.position(0);

							SparseArray<Integer> bufIds = gd.geoToTexCoordsBuf;
							if (bufIds == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}
							Integer bufId = bufIds.get(texUnit);
							if (bufId == null)
							{
								new Throwable("Buffer load issue!").printStackTrace();
							}
							else
							{
								gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, bufId.intValue());
								gl.glVertexAttribPointer(locs.glMultiTexCoord[texUnit], texStride, GL2ES2.GL_FLOAT, true, 0, 0);
								gl.glEnableVertexAttribArray(locs.glMultiTexCoord[texUnit]);//must be called after Pointer above
								if (DO_OUTPUT_ERRORS)
									outputErrors(ctx);

								if (OUTPUT_PER_FRAME_STATS)
									ctx.perFrameStats.enableTexCoordPointer++;
							}
						}
					}

				}

				//general catch all
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

			if (geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_STRIP_SET || geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_FAN_SET
					|| geo_type == GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET)
			{
				int primType = 0;

				// need to override if polygonAttributes says so
				if (ctx.polygonMode == PolygonAttributes.POLYGON_LINE)
					geo_type = GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET;

				switch (geo_type)
				{
				case GeometryRetained.GEO_TYPE_INDEXED_TRI_STRIP_SET:
					primType = GL2ES2.GL_TRIANGLE_STRIP;
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_TRI_FAN_SET:
					primType = GL2ES2.GL_TRIANGLE_FAN;
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET:
					primType = GL2ES2.GL_LINES;
					break;
				}

				int[] stripInd = gd.geoToIndStripBuf;
				// if no index buffers build build them now
				if (stripInd == null)
				{
					stripInd = new int[strip_len];
					gl.glGenBuffers(strip_len, stripInd, 0);

					int offset = initialIndexIndex;
					ByteBuffer bb = ByteBuffer.allocateDirect(indexCoord.length * 2);
					bb.order(ByteOrder.nativeOrder());
					ShortBuffer indicesBuffer = bb.asShortBuffer();
					for (int s = 0; s < indexCoord.length; s++)
						indicesBuffer.put(s, (short) indexCoord[s]);
					for (int i = 0; i < strip_len; i++)
					{
						indicesBuffer.position(offset);
						int count = sarray[i];
						int indBufId = stripInd[i];

						gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBufId);
						gl.glBufferData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, count * Short.SIZE / 8, indicesBuffer, GL2ES2.GL_STATIC_DRAW);
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);
						offset += count;

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glBufferData++;
					}

					gd.geoToIndStripBuf = stripInd;
				}
				else
				{
					//a good cDirty and a DYNAMIC_DRAW call needed
					/*if ((cDirty & GeometryArrayRetained.INDEX_CHANGED) != 0)
					{
						int offset = initialIndexIndex;
						IntBuffer indicesBuffer = IntBuffer.wrap(indexCoord);
						for (int i = 0; i < strip_len; i++)
						{
							indicesBuffer.position(offset);
							int count = sarray[i];
							int indBufId = stripInd[i];
					
							gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBufId);
							gl.glBufferSubData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0, count * Integer.SIZE / 8, indicesBuffer);
							//gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);
							offset += count;
					}*/
				}

				for (int i = 0; i < strip_len; i++)
				{
					int count = sarray[i];
					int indBufId = stripInd[i];
					//type Specifies the type of the values in indices. Must be
					// GL_UNSIGNED_BYTE or GL_UNSIGNED_SHORT.    
					// Apparently ES3 has included this guy now, so I'm a bit commited to it
					//https://www.khronos.org/opengles/sdk/docs/man/xhtml/glDrawElements.xml
					//This restriction is relaxed when GL_OES_element_index_uint is supported. 
					//GL_UNSIGNED_INT

					gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBufId);
					gl.glDrawElements(primType, count, GL2ES2.GL_UNSIGNED_SHORT, 0);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

					if (OUTPUT_PER_FRAME_STATS)
						ctx.perFrameStats.glDrawStripElementsStrips++;

				}
				//				gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glDrawStripElements++;

				//note only the first count so multi strips is worng here, but...
				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.indexCount += gd.geoToIndBufSize;

			}
			else
			{
				// bind my indexes ready for the draw call
				if (gd.geoToIndBuf == -1)
				{
					//create and fill index buffer
					//TODO: god damn Indexes have arrived here all the way from the nif file!!!!!
					ByteBuffer bb = ByteBuffer.allocateDirect(indexCoord.length * 2);
					bb.order(ByteOrder.nativeOrder());
					ShortBuffer indBuf = bb.asShortBuffer();
					for (int s = 0; s < indexCoord.length; s++)
						indBuf.put(s, (short) indexCoord[s]);
					indBuf.position(initialIndexIndex);

					int[] tmp = new int[1];
					gl.glGenBuffers(1, tmp, 0);
					gd.geoToIndBuf = tmp[0];// about to add to map below
					gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, gd.geoToIndBuf);
					gl.glBufferData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBuf.remaining() * Short.SIZE / 8, indBuf, GL2ES2.GL_STATIC_DRAW);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

					gd.geoToIndBufSize = indBuf.remaining();

				}
				else
				{
					//a good cDirty and a DYNAMIC_DRAW call needed
					/*if ((cDirty & GeometryArrayRetained.INDEX_CHANGED) != 0)
					{
						IntBuffer indBuf = IntBuffer.wrap(indexCoord);
						indBuf.position(initialIndexIndex);
						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, indexBufId.intValue());
						gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, indBuf.remaining() * Integer.SIZE / 8, indBuf);
					}*/
				}

				gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, gd.geoToIndBuf);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.indexCount += gd.geoToIndBufSize;

				// Need to override if polygonAttributes says we should be drawing lines
				// Note these are not poly line just contiguous lines between each pair of points
				// So it looks really rubbish
				if (ctx.polygonMode == PolygonAttributes.POLYGON_LINE)
					geo_type = GeometryRetained.GEO_TYPE_INDEXED_LINE_SET;
				else if (ctx.polygonMode == PolygonAttributes.POLYGON_POINT)
					geo_type = GeometryRetained.GEO_TYPE_INDEXED_POINT_SET;

				switch (geo_type)
				{
				case GeometryRetained.GEO_TYPE_INDEXED_QUAD_SET:
					//gl.glDrawElements(GL2ES2.GL_QUADS, validIndexCount, GL2ES2.GL_UNSIGNED_INT, 0);
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_TRI_SET:
					gl.glDrawElements(GL2ES2.GL_TRIANGLES, validIndexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_POINT_SET:
					gl.glDrawElements(GL2ES2.GL_POINTS, validIndexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_LINE_SET:
					gl.glDrawElements(GL2ES2.GL_LINES, validIndexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
					break;
				}
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glDrawElements++;

			}

			/*		unbound in setRenderMode now	
			if (gl.isGL2ES3())
			{
				GL2ES3 gl2es3 = (GL2ES3) gl;
				gl2es3.glBindVertexArray(0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}*/

			//TODO: are these needed?
			/*
			if (vattrDefined)
			{
				for (int i = 0; i < vertexAttrCount; i++)
				{
					Integer attribLoc = locs.genAttIndexToLoc.get(i);
					if (attribLoc != null && attribLoc.intValue() != -1)
					{
						gl.glDisableVertexAttribArray(attribLoc);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}
			}
			
			if (textureDefined)
			{
				for (int i = 0; i < locs.glMultiTexCoord.length; i++)
				{
					if (locs.glMultiTexCoord[i] != -1)
					{
						gl.glDisableVertexAttribArray(locs.glMultiTexCoord[i]);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}
			}*/

		}
		else

		{
			if (!NO_PROGRAM_WARNING_GIVEN)
				System.err.println("Execute called with no shader Program in use!");
			NO_PROGRAM_WARNING_GIVEN = true;
		}
		if (DO_OUTPUT_ERRORS)

			outputErrors(ctx);
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

	//----------------------------------------------------------------------
	private boolean executeIndexedGeometryOptimized(Context absCtx, GeometryArrayRetained geo, int geo_type, boolean isNonUniformScale,
			boolean ignoreVertexColors, int initialIndexIndex, int validIndexCount, int vertexCount, int vformat, int vdefined,
			FloatBuffer fverts, DoubleBuffer dverts, FloatBuffer fclrs, ByteBuffer bclrs, FloatBuffer norms, int vertexAttrCount,
			int[] vertexAttrSizes, FloatBuffer[] vertexAttrBufs, int texCoordMapLength, int[] texCoordSetMap, int numActiveTexUnitState,
			int texStride, Object[] texCoords, int cDirty, int[] indexCoord, int[] sarray, int strip_len)
	{
		//skip all morphables for now

		//TODO:Imagine! then attachments go back to being transforms??
		//Another thing you can do is double buffered VBO. This means you make 2 VBOs. On frame N, you update VBO 2 and you render with VBO 1. On frame N+1,
		//you update VBO 1 and you render from VBO 2. This also gives a nice boost in performance for nVidia and ATI/AMD.
		///  a new geomtery cap of ANIMATED_COORDS

		boolean morphable = ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_REF_DATA_WRITE)
				|| ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		if (morphable)
		{
			return false;
		}

		// Ok idea is:
		// current vertex = 3f for coord 4f for color 3f for normal 2f for texcoord (6f tan/bi)
		// current  = 48 bytes (72)
		// can be 3hf coord   =8, 2hf uv = 4, 4b color=4, 3b normal = 4 (3b tan/bi)=8
		// new = 20 bytes (28)
		// the normalized gear allows me to put byte colors and normals in as a byte across 1,-1, the half floats will be harder

		JoglesContext ctx = (JoglesContext) absCtx;
		int shaderProgramId = ctx.shaderProgramId;

		if (shaderProgramId != -1)
		{
			GL2ES2 gl = ctx.gl2es2;
			ProgramData pd = ctx.programData;
			LocationData locs = pd.programToLocationData;

			setFFPAttributes(ctx, gl, shaderProgramId, pd, vdefined);

			boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
			boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
			boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
			boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
			boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
			boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
			boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

			//NOTE here we are doing a virtual loadAllBuffers
			GeometryData gd = loadInterleavedBuffer(ctx, gl, geo, ignoreVertexColors, vertexCount, vformat, vdefined, fverts, dverts, fclrs,
					bclrs, norms, vertexAttrCount, vertexAttrSizes, vertexAttrBufs, texCoordMapLength, texCoordSetMap, texStride,
					texCoords);

			// if I'm handed a jogles geom then half floats and bytes are loaded waaaaaay back from disk
			boolean optimizedGeo = (geo instanceof JoglesIndexedTriangleArrayRetained)
					|| (geo instanceof JoglesIndexedTriangleStripArrayRetained);

			// not required second time around for VAO
			boolean bindingRequired = true;
			if (ctx.gl2es3 != null)
			{
				if (gd.vaoId == -1)
				{
					int[] tmp = new int[1];
					ctx.gl2es3.glGenVertexArrays(1, tmp, 0);
					gd.vaoId = tmp[0];
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
				else
				{
					bindingRequired = false;
				}
				ctx.gl2es3.glBindVertexArray(gd.vaoId);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

			if (bindingRequired)
			{
				if (gd.coordBufId != -1)
				{
					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.coordBufId);
				}
				else
				{
					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.interleavedBufId);
				}
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (floatCoordDefined && locs.glVertex != -1)
				{
					if (COMPRESS_OPTIMIZED_VERTICES || optimizedGeo)
					{
						gl.glVertexAttribPointer(locs.glVertex, 3, GL2ES2.GL_HALF_FLOAT, false, gd.interleavedStride, gd.geoToCoordOffset);
					}
					else
					{
						gl.glVertexAttribPointer(locs.glVertex, 3, GL2ES2.GL_FLOAT, false, gd.interleavedStride, gd.geoToCoordOffset);
					}
					gl.glEnableVertexAttribArray(locs.glVertex);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
				else if (doubleCoordDefined && locs.glVertex != -1)
				{
					throw new UnsupportedOperationException();
				}
				else
				{
					throw new UnsupportedOperationException("No coords!");
				}

				// if we had bound for separate coords above, bind to normal interleave now
				if (gd.coordBufId != -1)
				{
					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.interleavedBufId);
				}

				if (floatColorsDefined && locs.glColor != -1 && !ignoreVertexColors)
				{
					int sz = ((vformat & GeometryArray.WITH_ALPHA) != 0) ? 4 : 3;
					if (COMPRESS_OPTIMIZED_VERTICES || optimizedGeo)
					{
						gl.glVertexAttribPointer(locs.glColor, sz, GL2ES2.GL_UNSIGNED_BYTE, true, gd.interleavedStride,
								gd.geoToColorsOffset);
					}
					else
					{
						gl.glVertexAttribPointer(locs.glColor, sz, GL2ES2.GL_FLOAT, true, gd.interleavedStride, gd.geoToColorsOffset);

					}
					gl.glEnableVertexAttribArray(locs.glColor);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

				}
				else if (byteColorsDefined && locs.glColor != -1 && !ignoreVertexColors)
				{
					throw new UnsupportedOperationException();
				}
				else if (locs.glColor != -1)
				{
					// ignoreVertexcolors will be set in FFP now as the glColors is unbound
					gl.glDisableVertexAttribArray(locs.glColor);
				}

				if (normalsDefined && locs.glNormal != -1)
				{
					if (COMPRESS_OPTIMIZED_VERTICES || optimizedGeo)
					{
						gl.glVertexAttribPointer(locs.glNormal, 3, GL2ES2.GL_BYTE, true, gd.interleavedStride, gd.geoToNormalsOffset);
					}
					else
					{
						gl.glVertexAttribPointer(locs.glNormal, 3, GL2ES2.GL_FLOAT, true, gd.interleavedStride, gd.geoToNormalsOffset);
					}
					gl.glEnableVertexAttribArray(locs.glNormal);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
				else if (locs.glNormal != -1)
				{
					gl.glDisableVertexAttribArray(locs.glNormal);
				}

				if (vattrDefined)
				{
					for (int index = 0; index < vertexAttrCount; index++)
					{
						Integer attribLoc = locs.genAttIndexToLoc.get(index);
						if (attribLoc != null && attribLoc.intValue() != -1)
						{
							int sz = vertexAttrSizes[index];

							if (COMPRESS_OPTIMIZED_VERTICES || optimizedGeo)
							{
								gl.glVertexAttribPointer(attribLoc.intValue(), sz, GL2ES2.GL_BYTE, true, gd.interleavedStride,
										gd.geoToVattrOffset[index]);
							}
							else
							{
								gl.glVertexAttribPointer(attribLoc.intValue(), sz, GL2ES2.GL_FLOAT, true, gd.interleavedStride,
										gd.geoToVattrOffset[index]);
							}

							gl.glEnableVertexAttribArray(attribLoc.intValue());
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);
						}
					}
				}

				if (textureDefined)
				{
					boolean[] texSetsLoaded = new boolean[texCoords.length];
					for (int texUnit = 0; texUnit < numActiveTexUnitState && texUnit < texCoordMapLength; texUnit++)
					{
						int texSet = texCoordSetMap[texUnit];
						if (texSet != -1 && locs.glMultiTexCoord[texSet] != -1 && !texSetsLoaded[texSet])
						{
							texSetsLoaded[texSet] = true;
							if (COMPRESS_OPTIMIZED_VERTICES || optimizedGeo)
							{
								gl.glVertexAttribPointer(locs.glMultiTexCoord[texSet], texStride, GL2ES2.GL_HALF_FLOAT, true,
										gd.interleavedStride, gd.geoToTexCoordOffset[texSet]);
							}
							else
							{
								gl.glVertexAttribPointer(locs.glMultiTexCoord[texSet], texStride, GL2ES2.GL_FLOAT, true,
										gd.interleavedStride, gd.geoToTexCoordOffset[texSet]);
							}
							gl.glEnableVertexAttribArray(locs.glMultiTexCoord[texSet]);
							if (DO_OUTPUT_ERRORS)
								outputErrors(ctx);
						}
					}
				}

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glVertexAttribPointerInterleaved++;

				//general catch all
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

			if (geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_STRIP_SET || geo_type == GeometryRetained.GEO_TYPE_INDEXED_TRI_FAN_SET
					|| geo_type == GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET)
			{
				int primType = 0;

				// need to override if polygonAttributes says so
				if (ctx.polygonMode == PolygonAttributes.POLYGON_LINE)
					geo_type = GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET;

				switch (geo_type)
				{
				case GeometryRetained.GEO_TYPE_INDEXED_TRI_STRIP_SET:
					primType = GL2ES2.GL_TRIANGLE_STRIP;
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_TRI_FAN_SET:
					primType = GL2ES2.GL_TRIANGLE_FAN;
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_LINE_STRIP_SET:
					primType = GL2ES2.GL_LINES;
					break;
				}

				int[] stripInd = gd.geoToIndStripBuf;
				// if no index buffers build build them now
				if (stripInd == null)
				{

					stripInd = new int[strip_len];
					gl.glGenBuffers(strip_len, stripInd, 0);

					int indexOffset = initialIndexIndex;
					ShortBuffer indicesBuffer = null;

					if (geo instanceof JoglesIndexedTriangleStripArrayRetained)
					{
						indicesBuffer = ((JoglesIndexedTriangleStripArrayRetained) geo).indBuf;
					}
					else
					{
						ByteBuffer bb = ByteBuffer.allocateDirect(indexCoord.length * 2);
						bb.order(ByteOrder.nativeOrder());
						indicesBuffer = bb.asShortBuffer();
						for (int s = 0; s < indexCoord.length; s++)
							indicesBuffer.put(s, (short) indexCoord[s]);
					}

					for (int i = 0; i < strip_len; i++)
					{
						indicesBuffer.position(indexOffset);
						int count = sarray[i];
						int indBufId = stripInd[i];

						gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBufId);
						gl.glBufferData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, count * Short.SIZE / 8, indicesBuffer, GL2ES2.GL_STATIC_DRAW);
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);
						indexOffset += count;

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glBufferData++;
					}

					gd.geoToIndStripBuf = stripInd;
				}
				else
				{
					//a good cDirty and a DYNAMIC_DRAW call needed
					/*if ((cDirty & GeometryArrayRetained.INDEX_CHANGED) != 0)
					{
						int offset = initialIndexIndex;
						IntBuffer indicesBuffer = IntBuffer.wrap(indexCoord);
						for (int i = 0; i < strip_len; i++)
						{
							indicesBuffer.position(offset);
							int count = sarray[i];
							int indBufId = stripInd[i];
					
							gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBufId);
							gl.glBufferSubData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0, count * Integer.SIZE / 8, indicesBuffer);
							//gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);
							offset += count;
					}*/
				}

				for (int i = 0; i < strip_len; i++)
				{
					int count = sarray[i];
					int indBufId = stripInd[i];
					//type Specifies the type of the values in indices. Must be
					// GL_UNSIGNED_BYTE or GL_UNSIGNED_SHORT.    
					// Apparently ES3 has included this guy now, so I'm a bit commited to it
					//https://www.khronos.org/opengles/sdk/docs/man/xhtml/glDrawElements.xml
					//This restriction is relaxed when GL_OES_element_index_uint is supported. 
					//GL_UNSIGNED_INT

					gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBufId);
					gl.glDrawElements(primType, count, GL2ES2.GL_UNSIGNED_SHORT, 0);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

					if (OUTPUT_PER_FRAME_STATS)
						ctx.perFrameStats.glDrawStripElementsStrips++;

				}
				//				gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glDrawStripElements++;

				//note only the first count so multi strips is wrong here, but...
				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.indexCount += gd.geoToIndBufSize;

			}
			else
			{
				// bind my indexes ready for the draw call
				if (gd.geoToIndBuf == -1)
				{
					ShortBuffer indBuf = null;
					if (geo instanceof JoglesIndexedTriangleArrayRetained)
					{
						indBuf = ((JoglesIndexedTriangleArrayRetained) geo).indBuf;
					}
					else
					{
						//create and fill index buffer
						//TODO: god damn Indexes have arrived here all the way from the nif file!!!!!
						ByteBuffer bb = ByteBuffer.allocateDirect(indexCoord.length * 2);
						bb.order(ByteOrder.nativeOrder());
						indBuf = bb.asShortBuffer();
						for (int s = 0; s < indexCoord.length; s++)
							indBuf.put(s, (short) indexCoord[s]);
						indBuf.position(initialIndexIndex);
					}

					int[] tmp = new int[1];
					gl.glGenBuffers(1, tmp, 0);
					gd.geoToIndBuf = tmp[0];// about to add to map below
					gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, gd.geoToIndBuf);
					gl.glBufferData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, indBuf.remaining() * Short.SIZE / 8, indBuf, GL2ES2.GL_STATIC_DRAW);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

					gd.geoToIndBufSize = indBuf.remaining();

				}
				else
				{
					//a good cDirty and a DYNAMIC_DRAW call needed
					/*if ((cDirty & GeometryArrayRetained.INDEX_CHANGED) != 0)
					{
						IntBuffer indBuf = IntBuffer.wrap(indexCoord);
						indBuf.position(initialIndexIndex);
						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, indexBufId.intValue());
						gl.glBufferSubData(GL2ES2.GL_ARRAY_BUFFER, 0, indBuf.remaining() * Integer.SIZE / 8, indBuf);
					}*/
				}
				if (bindingRequired)
				{
					gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, gd.geoToIndBuf);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

					if (OUTPUT_PER_FRAME_STATS)
						ctx.perFrameStats.indexCount += gd.geoToIndBufSize;
				}

				// Need to override if polygonAttributes says we should be drawing lines
				// Note these are not poly line just contiguos lines between each pair of points
				// So it looks really rubbish
				if (ctx.polygonMode == PolygonAttributes.POLYGON_LINE)
					geo_type = GeometryRetained.GEO_TYPE_INDEXED_LINE_SET;
				else if (ctx.polygonMode == PolygonAttributes.POLYGON_POINT)
					geo_type = GeometryRetained.GEO_TYPE_INDEXED_POINT_SET;

				switch (geo_type)
				{
				case GeometryRetained.GEO_TYPE_INDEXED_QUAD_SET:
					//gl.glDrawElements(GL2ES2.GL_QUADS, validIndexCount, GL2ES2.GL_UNSIGNED_INT, 0);
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_TRI_SET:
					gl.glDrawElements(GL2ES2.GL_TRIANGLES, validIndexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_POINT_SET:
					gl.glDrawElements(GL2ES2.GL_POINTS, validIndexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
					break;
				case GeometryRetained.GEO_TYPE_INDEXED_LINE_SET:
					gl.glDrawElements(GL2ES2.GL_LINES, validIndexCount, GL2ES2.GL_UNSIGNED_SHORT, 0);
					break;
				}
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glDrawElements++;

			}

			/*		unbound in setRenderMode now	
			if (gl.isGL2ES3())
			{
				GL2ES3 gl2es3 = (GL2ES3) gl;
				gl2es3.glBindVertexArray(0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}*/

			//TODO: are these needed??
			/*
			if (vattrDefined)
			{
				for (int i = 0; i < vertexAttrCount; i++)
				{
					Integer attribLoc = locs.genAttIndexToLoc.get(i);
					if (attribLoc != null && attribLoc.intValue() != -1)
					{
						gl.glDisableVertexAttribArray(attribLoc);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}
			}
			
			if (textureDefined)
			{
				for (int i = 0; i < locs.glMultiTexCoord.length; i++)
				{
					if (locs.glMultiTexCoord[i] != -1)
					{
						gl.glDisableVertexAttribArray(locs.glMultiTexCoord[i]);
						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glDisableVertexAttribArray++;
					}
				}
			}*/

		}
		else
		{
			if (!NO_PROGRAM_WARNING_GIVEN)
				System.err.println("Execute called with no shader Program in use!");
			NO_PROGRAM_WARNING_GIVEN = true;
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

		return true;
	}

	/**
	 * Over time we have had things recorded and in FFP they are considered current state
	 * in programmable we have to push them across manually each time recorded in JoglesContext
	 * @param gl
	 * @param vdefined 
	 */

	private static void setFFPAttributes(JoglesContext ctx, GL2ES2 gl, int shaderProgramId, ProgramData pd, int vdefined)
	{

		LocationData locs = pd.programToLocationData;

		//boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
		//boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
		boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
		boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
		//boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
		//boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
		//boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

		// vertex colors MUST be ignored if no glColors set
		boolean ignoreVertexColors = (!floatColorsDefined && !byteColorsDefined) || ctx.renderingData.ignoreVertexColors;

		if (OUTPUT_PER_FRAME_STATS)
			ctx.perFrameStats.setFFPAttributes++;

		if (ATTEMPT_UBO && gl.isGL2ES3())
		{
			//TODO: p v m norm all needs to be transpose somehow?
			GL2ES3 gl2es3 = (GL2ES3) gl;

			if (locs.blockIndex != -1)
			{
				ByteBuffer uboBB = pd.programToUBOBB;

				if (locs.glProjectionMatrixOffset != -1)
				{
					uboBB.position(locs.glProjectionMatrixOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentProjMat));
				}
				if (locs.glProjectionMatrixInverseOffset != -1)
				{
					// Expensive, only calc if required
					ctx.currentProjMatInverse.set(ctx.currentProjMat);
					ctx.matrixUtil.invert(ctx.currentProjMatInverse);

					uboBB.position(locs.glProjectionMatrixInverseOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentProjMatInverse));
				}
				if (locs.glViewMatrixOffset != -1)
				{
					uboBB.position(locs.glViewMatrixOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentViewMat));
				}
				if (locs.glModelMatrixOffset != -1)
				{
					uboBB.position(locs.glModelMatrixOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentModelMat));
				}
				if (locs.glModelViewMatrixOffset != -1)
				{
					ctx.currentModelViewMat.mul(ctx.currentViewMat, ctx.currentModelMat);

					uboBB.position(locs.glModelViewMatrixOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentModelViewMat));
				}
				if (locs.glModelViewMatrixInverseOffset != -1)
				{
					// Expensive, only if required
					ctx.currentModelViewMat.mul(ctx.currentViewMat, ctx.currentModelMat);
					ctx.currentModelViewMatInverse.set(ctx.currentModelViewMat);
					ctx.matrixUtil.invert(ctx.currentModelViewMatInverse);

					uboBB.position(locs.glModelViewMatrixInverseOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentModelViewMatInverse));
				}
				if (locs.glModelViewProjectionMatrixOffset != -1)
				{
					ctx.currentModelViewMat.mul(ctx.currentViewMat, ctx.currentModelMat);
					ctx.currentModelViewProjMat.mul(ctx.currentProjMat, ctx.currentModelViewMat);

					uboBB.position(locs.glModelViewProjectionMatrixOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray(ctx.currentModelViewProjMat));
				}
				//3x3 are stored as 3xvec4 (48 bytes before next offset) note use of toArray3x4
				if (locs.glNormalMatrixOffset != -1)
				{
					ctx.currentModelViewMat.mul(ctx.currentViewMat, ctx.currentModelMat);
					JoglesMatrixUtil.transposeInvert(ctx.currentModelViewMat, ctx.currentNormalMat);

					uboBB.position(locs.glNormalMatrixOffset);
					uboBB.asFloatBuffer().put(ctx.matrixUtil.toArray3x4(ctx.currentNormalMat));
				}
				if (locs.glFrontMaterialdiffuseOffset != -1)
				{
					uboBB.position(locs.glFrontMaterialdiffuseOffset);
					uboBB.asFloatBuffer().put(ctx.materialData.diffuse.x).put(ctx.materialData.diffuse.y).put(ctx.materialData.diffuse.z)
							.put(ctx.materialData.diffuse.w);
				}
				if (locs.glFrontMaterialemissionOffset != -1)
				{
					uboBB.position(locs.glFrontMaterialemissionOffset);
					uboBB.asFloatBuffer().put(ctx.materialData.emission.x).put(ctx.materialData.emission.y).put(ctx.materialData.emission.z)
							.put(1f); //note extra alpha value to avoid errors
				}
				if (locs.glFrontMaterialspecularOffset != -1)
				{
					uboBB.position(locs.glFrontMaterialspecularOffset);
					uboBB.asFloatBuffer().put(ctx.materialData.specular.x).put(ctx.materialData.specular.y)
							.put(ctx.materialData.specular.z);
				}
				if (locs.glFrontMaterialshininessOffset != -1)
				{
					uboBB.position(locs.glFrontMaterialshininessOffset);
					uboBB.asFloatBuffer().put(ctx.materialData.shininess);
				}
				// if set one of the 2 colors below should be used by the shader (material for lighting)
				//   old uniform system appears wrong?					
				if (locs.ignoreVertexColorsOffset != -1)
				{
					uboBB.position(locs.ignoreVertexColorsOffset);
					uboBB.asIntBuffer().put(ignoreVertexColors ? 1 : 0);// note local variable used
				}

				if (locs.glLightModelambientOffset != -1)
				{
					uboBB.position(locs.glLightModelambientOffset);
					uboBB.asFloatBuffer().put(ctx.currentAmbientColor.x).put(ctx.currentAmbientColor.y).put(ctx.currentAmbientColor.z)
							.put(ctx.currentAmbientColor.w);
				}
				if (locs.objectColorOffset != -1)
				{
					uboBB.position(locs.objectColorOffset);
					uboBB.asFloatBuffer().put(ctx.objectColor.x).put(ctx.objectColor.y).put(ctx.objectColor.z).put(ctx.objectColor.w);
				}

				//For now using first point light, but gonna need to put em all in
				LightData l0 = null;
				if (ctx.pointLight[0] != null)
					l0 = ctx.pointLight[0];
				else if (ctx.dirLight[0] != null)
					l0 = ctx.dirLight[0];

				if (l0 != null)
				{
					if (locs.glLightSource0positionOffset != -1)
					{
						uboBB.position(locs.glLightSource0positionOffset);
						uboBB.asFloatBuffer().put(l0.pos.x).put(l0.pos.y).put(l0.pos.z).put(l0.pos.w);
					}
					if (locs.glLightSource0diffuseOffset != -1)
					{
						uboBB.position(locs.glLightSource0diffuseOffset);
						uboBB.asFloatBuffer().put(l0.diffuse.x).put(l0.diffuse.y).put(l0.diffuse.z).put(l0.diffuse.w);
					}
				}

				// TODO: test the transpose
				if (locs.textureTransformOffset != -1)
				{
					uboBB.position(locs.textureTransformOffset);
					uboBB.asFloatBuffer().put(JoglesMatrixUtil.transposeInPlace(ctx.matrixUtil.toArray(ctx.textureTransform)));
				}

				if (locs.alphaTestEnabledOffset != -1)
				{
					uboBB.position(locs.alphaTestEnabledOffset);
					uboBB.asIntBuffer().put(ctx.renderingData.alphaTestEnabled ? 1 : 0);

					if (ctx.renderingData.alphaTestEnabled == true)
					{
						if (locs.alphaTestFunctionOffset != -1)
						{
							uboBB.position(locs.alphaTestFunctionOffset);
							uboBB.asIntBuffer().put(ctx.renderingData.alphaTestFunction);
						}
						if (locs.alphaTestValueOffset != -1)
						{
							uboBB.position(locs.alphaTestValueOffset);
							uboBB.asFloatBuffer().put(ctx.renderingData.alphaTestValue);
						}
					}
				}

				uboBB.position(0);// very important!

				//gl2es3.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, locs.uboBufId);
				gl2es3.glBufferSubData(GL2ES3.GL_UNIFORM_BUFFER, 0, uboBB.limit(), uboBB);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				// we have done our ffp work don't fall through to normal system						
				return;
			}

		}

		//if shader hasn't changed location of uniform I don't need to reset these (they are cleared to -1 at the start of each swap)
		if (locs.glProjectionMatrix != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram))
			{
				gl.glUniformMatrix4fv(locs.glProjectionMatrix, 1, true, ctx.matrixUtil.toArray(ctx.currentProjMat), 0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}
		if (locs.glProjectionMatrixInverse != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram))
			{
				//EXPENSIVE!!!!! only calc if asked for, and even then...
				try
				{
					ctx.currentProjMatInverse.set(ctx.currentProjMat);
					ctx.matrixUtil.invert(ctx.currentProjMatInverse);
				}
				catch (SingularMatrixException e)
				{
					System.err.println("" + e);
				}

				gl.glUniformMatrix4fv(locs.glProjectionMatrixInverse, 1, true, ctx.matrixUtil.toArray(ctx.currentProjMatInverse), 0);

				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}
		if (locs.glViewMatrix != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram))
			{
				gl.glUniformMatrix4fv(locs.glViewMatrix, 1, true, ctx.matrixUtil.toArray(ctx.currentViewMat), 0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}

		if (locs.glModelMatrix != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.modelMatrix.equals(ctx.currentModelMat)))
			{
				gl.glUniformMatrix4fv(locs.glModelMatrix, 1, true, ctx.matrixUtil.toArray(ctx.currentModelMat), 0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.modelMatrix.set(ctx.currentModelMat);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.modelMatrixUpdated++;
			}
			else if (OUTPUT_PER_FRAME_STATS)
			{
				ctx.perFrameStats.modelMatrixSkipped++;
			}
		}

		if (locs.glModelViewMatrix != -1)
		{
			// minimise not working due to late calc of matrix
			//if (!MINIMISE_NATIVE_CALLS_FFP
			//			|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.glModelViewMatrix.equals(ctx.currentModelViewMat)))
			//	{
			ctx.currentModelViewMat.mul(ctx.currentViewMat, ctx.currentModelMat);

			gl.glUniformMatrix4fv(locs.glModelViewMatrix, 1, true, ctx.matrixUtil.toArray(ctx.currentModelViewMat), 0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			if (MINIMISE_NATIVE_CALLS_FFP)
				ctx.gl_state.glModelViewMatrix.set(ctx.currentModelViewMat);
			if (OUTPUT_PER_FRAME_STATS)
				ctx.perFrameStats.glModelViewMatrixUpdated++;
			//	}
			//	else if (OUTPUT_PER_FRAME_STATS)
			//	{
			//		ctx.perFrameStats.glModelViewMatrixSkipped++;
			//	}
		}
		if (locs.glModelViewMatrixInverse != -1)
		{// minimise not working due to late calc of matrix
			//if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram
			//		|| !ctx.gl_state.glModelViewMatrixInverse.equals(ctx.currentModelViewMatInverse)))
			//{
			// Expensive, only calc if required
			ctx.currentModelViewMatInverse.mul(ctx.currentViewMat, ctx.currentModelMat);
			ctx.matrixUtil.invert(ctx.currentModelViewMatInverse);

			//gl.glUniformMatrix4fv(locs.glModelViewMatrixInverse, 1, false, ctx.toFB(ctx.currentModelViewMatInverse));
			gl.glUniformMatrix4fv(locs.glModelViewMatrixInverse, 1, true, ctx.matrixUtil.toArray(ctx.currentModelViewMatInverse), 0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			if (MINIMISE_NATIVE_CALLS_FFP)
				ctx.gl_state.glModelViewMatrixInverse.set(ctx.currentModelViewMatInverse);
			if (OUTPUT_PER_FRAME_STATS)
				ctx.perFrameStats.glModelViewMatrixInverseUpdated++;
			//	}
			//	else if (OUTPUT_PER_FRAME_STATS)
			//	{
			//		ctx.perFrameStats.glModelViewMatrixInverseSkipped++;
			//	}
		}

		if (locs.glModelViewProjectionMatrix != -1)
		{
			// minimise not working due to late calc of matrix
			//	if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram
			//			|| !ctx.gl_state.glModelViewProjectionMatrix.equals(ctx.currentModelViewProjMat)))
			//	{
			ctx.currentModelViewMat.mul(ctx.currentViewMat, ctx.currentModelMat);
			ctx.currentModelViewProjMat.mul(ctx.currentProjMat, ctx.currentModelViewMat);

			//gl.glUniformMatrix4fv(locs.glModelViewProjectionMatrix, 1, false, ctx.toFB(ctx.currentModelViewProjMat));
			gl.glUniformMatrix4fv(locs.glModelViewProjectionMatrix, 1, true, ctx.matrixUtil.toArray(ctx.currentModelViewProjMat), 0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			if (MINIMISE_NATIVE_CALLS_FFP)
				ctx.gl_state.glModelViewProjectionMatrix.set(ctx.currentModelViewProjMat);
			if (OUTPUT_PER_FRAME_STATS)
				ctx.perFrameStats.glModelViewProjectionMatrixUpdated++;
			//	}
			//	else if (OUTPUT_PER_FRAME_STATS)
			//	{
			//		ctx.perFrameStats.glModelViewProjectionMatrixSkipped++;
			//	}
		}

		if (locs.glNormalMatrix != -1)
		{
			// minimise not working due to late calc of matrix
			//if (!MINIMISE_NATIVE_CALLS_FFP
			//		|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.glNormalMatrix.equals(ctx.currentNormalMat)))
			//{
			ctx.currentModelViewMat.mul(ctx.matrixUtil.deburnV, ctx.matrixUtil.deburnM);
			JoglesMatrixUtil.transposeInvert(ctx.currentModelViewMat, ctx.currentNormalMat);

			//gl.glUniformMatrix3fv(locs.glNormalMatrix, 1, false, ctx.toFB(ctx.currentNormalMat));
			gl.glUniformMatrix3fv(locs.glNormalMatrix, 1, true, ctx.matrixUtil.toArray(ctx.currentNormalMat), 0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_CALLS_FFP)
				ctx.gl_state.glNormalMatrix.set(ctx.currentNormalMat);
			if (OUTPUT_PER_FRAME_STATS)
				ctx.perFrameStats.glNormalMatrixUpdated++;
			//}
			//else if (OUTPUT_PER_FRAME_STATS)
			//{
			//	ctx.perFrameStats.glNormalMatrixSkipped++;
			//}
		}

		// if set one of the 2 colors below should be used by the shader (material for lighting)

		if (locs.ignoreVertexColors != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || ctx.gl_state.ignoreVertexColors != ignoreVertexColors))
			{
				gl.glUniform1i(locs.ignoreVertexColors, ignoreVertexColors ? 1 : 0);// note local variable used

				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.ignoreVertexColors = ignoreVertexColors;
			}
		}

		//send material data through
		if (locs.glFrontMaterialdiffuse != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.glFrontMaterialdiffuse.equals(ctx.materialData.diffuse)))
			{
				gl.glUniform4f(locs.glFrontMaterialdiffuse, ctx.materialData.diffuse.x, ctx.materialData.diffuse.y,
						ctx.materialData.diffuse.z, ctx.materialData.diffuse.w);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.glFrontMaterialdiffuse.set(ctx.materialData.diffuse);
			}
		}
		if (locs.glFrontMaterialemission != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram
					|| !ctx.gl_state.glFrontMaterialemission.equals(ctx.materialData.emission)))
			{
				gl.glUniform4f(locs.glFrontMaterialemission, ctx.materialData.emission.x, ctx.materialData.emission.y,
						ctx.materialData.emission.z, 1f); //note extra alpha value to avoid errors
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.glFrontMaterialemission.set(ctx.materialData.emission);
			}
		}

		if (locs.glFrontMaterialspecular != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP || (shaderProgramId != ctx.prevShaderProgram
					|| !ctx.gl_state.glFrontMaterialspecular.equals(ctx.materialData.specular)))
			{
				gl.glUniform3f(locs.glFrontMaterialspecular, ctx.materialData.specular.x, ctx.materialData.specular.y,
						ctx.materialData.specular.z);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.glFrontMaterialspecular.set(ctx.materialData.specular);
			}
		}
		if (locs.glFrontMaterialshininess != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || ctx.gl_state.glFrontMaterialshininess != ctx.materialData.shininess))
			{
				gl.glUniform1f(locs.glFrontMaterialshininess, ctx.materialData.shininess);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.glFrontMaterialshininess = ctx.materialData.shininess;
			}
		}

		//ambient does not come from material notice
		if (locs.glLightModelambient != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.glLightModelambient.equals(ctx.currentAmbientColor)))
			{
				gl.glUniform4f(locs.glLightModelambient, ctx.currentAmbientColor.x, ctx.currentAmbientColor.y, ctx.currentAmbientColor.z,
						ctx.currentAmbientColor.w);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.glLightModelambient.set(ctx.currentAmbientColor);
			}
		}

		// always bind object color, the shader can decide to use it if it's no lighting and no vertex colors
		if (locs.objectColor != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.objectColor.equals(ctx.objectColor)))
			{
				gl.glUniform4f(locs.objectColor, ctx.objectColor.x, ctx.objectColor.y, ctx.objectColor.z, ctx.objectColor.w);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.objectColor.set(ctx.objectColor);
			}
		}

		/*dirLight
		pointLight
		spotLight*/
		//currentEnabledLights

		//For now using first point light, but gonna need to put em all in
		LightData l0 = null;
		if (ctx.pointLight[0] != null)
			l0 = ctx.pointLight[0];
		else if (ctx.dirLight[0] != null)
			l0 = ctx.dirLight[0];

		if (l0 != null)
		{
			if (locs.glLightSource0position != -1)
			{
				gl.glUniform4f(locs.glLightSource0position, l0.pos.x, l0.pos.y, l0.pos.z, l0.pos.w);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
			if (locs.glLightSource0diffuse != -1)
			{
				gl.glUniform4f(locs.glLightSource0diffuse, l0.diffuse.x, l0.diffuse.y, l0.diffuse.z, l0.diffuse.w);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}

		//TODO: particles and points etc
		//currentPointSize needs to be handed into the particles shader

		//TODO: look at walkway grill in diamond city looks like I've got these wrong
		// but playing doesn't help
		// also a plant in morrowind is not doing alpha testing properly
		if (locs.alphaTestEnabled != -1)
		{
			gl.glUniform1i(locs.alphaTestEnabled, ctx.renderingData.alphaTestEnabled ? 1 : 0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			if (ctx.renderingData.alphaTestEnabled == true)
			{
				gl.glUniform1i(locs.alphaTestFunction, getFunctionValue(ctx.renderingData.alphaTestFunction));
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				gl.glUniform1f(locs.alphaTestValue, ctx.renderingData.alphaTestValue);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}

		if (locs.textureTransform != -1)
		{
			if (!MINIMISE_NATIVE_CALLS_FFP
					|| (shaderProgramId != ctx.prevShaderProgram || !ctx.gl_state.textureTransform.equals(ctx.textureTransform)))
			{
				//gl.glUniformMatrix4fv(locs.textureTransform, 1, true, ctx.toFB(ctx.textureTransform));
				gl.glUniformMatrix4fv(locs.textureTransform, 1, true, ctx.matrixUtil.toArray(ctx.textureTransform), 0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_FFP)
					ctx.gl_state.textureTransform.set(ctx.textureTransform);
			}
		}

		//TODO: needs to be handled ,
		//ctx.fogData

		//NOTE water app shows multiple light calculations

		// record for the next loop through FFP
		ctx.prevShaderProgram = shaderProgramId;
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

		// happens on the first call sometimes, for background or clear or something

	}

	private boolean NO_PROGRAM_WARNING_GIVEN = false;

	//----------------------------------------------------------------------
	// Private helper methods for GeometryArrayRetained and IndexedGeometryArrayRetained
	//

	private static void loadLocs(JoglesContext ctx, GL2ES2 gl)
	{
		ProgramData pd = ctx.programData;
		int shaderProgramId = ctx.shaderProgramId;
		if (pd.programToLocationData == null)
		{
			LocationData locs = new LocationData();

			if (OUTPUT_PER_FRAME_STATS)
				ctx.perFrameStats.programToLocationData++;

			if (ATTEMPT_UBO && gl.isGL2ES3())
			{
				GL2ES3 gl2es3 = (GL2ES3) gl;

				int UBOBlockIndex = gl2es3.glGetUniformBlockIndex(shaderProgramId, "FFP_Uniform_Block");
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (UBOBlockIndex != -1)
				{
					locs.blockIndex = UBOBlockIndex;
					int[] blockSize = new int[1];

					gl2es3.glGetActiveUniformBlockiv(shaderProgramId, UBOBlockIndex, GL2ES3.GL_UNIFORM_BLOCK_DATA_SIZE, blockSize, 0);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
					locs.blockSize = blockSize[0];

					ByteBuffer uboBB = ByteBuffer.allocateDirect(blockSize[0]);
					uboBB.order(ByteOrder.nativeOrder());
					pd.programToUBOBB = uboBB;

					// set up single buffer for ffp data, jesus...
					if (ctx.globalUboBufId == -1)
					//if (locs.uboBufId == -1)
					{
						int[] tmp = new int[1];
						gl.glGenBuffers(1, tmp, 0);
						int uboBufId = tmp[0];

						ctx.globalUboBufId = uboBufId;
						//locs.uboBufId = uboBufId;

						gl2es3.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, uboBufId);
						gl2es3.glBufferData(GL2ES3.GL_UNIFORM_BUFFER, uboBB.limit(), uboBB, GL2ES3.GL_DYNAMIC_DRAW);
						gl2es3.glBindBufferBase(GL2ES3.GL_UNIFORM_BUFFER, UBOBlockIndex, uboBufId);
					}

					// Query for the offsets of each block variable
					String[] names = { "glProjectionMatrix", "glProjectionMatrixInverse", //
							"glViewMatrix", "glModelMatrix", //
							"glModelViewMatrix", "glModelViewMatrixInverse", //
							"glModelViewProjectionMatrix", "glNormalMatrix", //
							"glFrontMaterialdiffuse", "glFrontMaterialemission", //
							"glFrontMaterialspecular", "glFrontMaterialshininess", //
							"ignoreVertexColors", "glLightModelambient", //
							"objectColor", //
							"glLightSource0position", "glLightSource0diffuse", //
							"textureTransform", //
							"alphaTestEnabled", "alphaTestFunction", "alphaTestValue" };

					IntBuffer indices = IntBuffer.allocate(names.length);

					if (PRESUME_INDICES)
					{
						indices.put(0, 16);
						indices.put(1, 17);
						indices.put(2, 18);
						indices.put(3, 11);
						indices.put(4, 12);
						indices.put(5, 13);
						indices.put(6, 14);
						indices.put(7, 15);
						indices.put(8, 4);
						indices.put(9, 5);
						indices.put(10, 7);
						indices.put(11, 6);
						indices.put(12, 19);
						indices.put(13, 8);
						indices.put(14, 20);
						indices.put(15, 10);
						indices.put(16, 9);
						indices.put(17, 21);
						indices.put(18, 1);
						indices.put(19, 2);
						indices.put(20, 3);

						locs.glProjectionMatrixOffset = 0;
						locs.glProjectionMatrixInverseOffset = 64;
						locs.glViewMatrixOffset = 128;
						locs.glModelMatrixOffset = 192;
						locs.glModelViewMatrixOffset = 256;
						locs.glModelViewMatrixInverseOffset = 320;
						locs.glModelViewProjectionMatrixOffset = 384;
						locs.glNormalMatrixOffset = 448;
						locs.glFrontMaterialdiffuseOffset = 496;
						locs.glFrontMaterialemissionOffset = 512;
						locs.glFrontMaterialspecularOffset = 528;
						locs.glFrontMaterialshininessOffset = 540;
						locs.ignoreVertexColorsOffset = 544;
						locs.glLightModelambientOffset = 560;
						locs.objectColorOffset = 576;
						locs.glLightSource0positionOffset = 592;
						locs.glLightSource0diffuseOffset = 608;
						locs.textureTransformOffset = 624;
						locs.alphaTestEnabledOffset = 688;
						locs.alphaTestFunctionOffset = 692;
						locs.alphaTestValueOffset = 696;
					}
					else
					{
						gl2es3.glGetUniformIndices(shaderProgramId, names.length, names, indices);

						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);
						//ok so including a -1 in the indices cause everything to come back at 0 after it
						// possibly android throws a fit if it's compiled away or something
						IntBuffer offset = IntBuffer.allocate(1);
						if (indices.get(0) != -1)
						{
							indices.position(0);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glProjectionMatrixOffset = offset.get(0);
						}
						if (indices.get(1) != -1)
						{
							indices.position(1);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glProjectionMatrixInverseOffset = offset.get(0);
						}
						if (indices.get(2) != -1)
						{
							indices.position(2);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glViewMatrixOffset = offset.get(0);
						}
						if (indices.get(3) != -1)
						{
							indices.position(3);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glModelMatrixOffset = offset.get(0);
						}
						if (indices.get(4) != -1)
						{
							indices.position(4);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glModelViewMatrixOffset = offset.get(0);
						}
						if (indices.get(5) != -1)
						{
							indices.position(5);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glModelViewMatrixInverseOffset = offset.get(0);
						}
						if (indices.get(6) != -1)
						{
							indices.position(6);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glModelViewProjectionMatrixOffset = offset.get(0);
						}
						if (indices.get(7) != -1)
						{
							indices.position(7);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glNormalMatrixOffset = offset.get(0);
						}
						if (indices.get(8) != -1)
						{
							indices.position(8);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glFrontMaterialdiffuseOffset = offset.get(0);
						}
						if (indices.get(9) != -1)
						{
							indices.position(9);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glFrontMaterialemissionOffset = offset.get(0);
						}
						if (indices.get(10) != -1)
						{
							indices.position(10);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glFrontMaterialspecularOffset = offset.get(0);
						}
						if (indices.get(11) != -1)
						{
							indices.position(11);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glFrontMaterialshininessOffset = offset.get(0);
						}
						if (indices.get(12) != -1)
						{
							indices.position(12);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.ignoreVertexColorsOffset = offset.get(0);
						}
						if (indices.get(13) != -1)
						{
							indices.position(13);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glLightModelambientOffset = offset.get(0);
						}
						if (indices.get(14) != -1)
						{
							indices.position(14);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.objectColorOffset = offset.get(0);
						}
						if (indices.get(15) != -1)
						{
							indices.position(15);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glLightSource0positionOffset = offset.get(0);
						}
						if (indices.get(16) != -1)
						{
							indices.position(16);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.glLightSource0diffuseOffset = offset.get(0);
						}
						if (indices.get(17) != -1)
						{
							indices.position(17);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.textureTransformOffset = offset.get(0);
						}
						if (indices.get(18) != -1)
						{
							indices.position(18);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.alphaTestEnabledOffset = offset.get(0);
						}
						if (indices.get(19) != -1)
						{
							indices.position(19);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.alphaTestFunctionOffset = offset.get(0);
						}
						if (indices.get(20) != -1)
						{
							indices.position(20);
							gl2es3.glGetActiveUniformsiv(shaderProgramId, 1, indices, GL2ES3.GL_UNIFORM_OFFSET, offset);
							locs.alphaTestValueOffset = offset.get(0);
						}
					}

					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
			}

			locs.glProjectionMatrix = gl.glGetUniformLocation(shaderProgramId, "glProjectionMatrix");
			locs.glProjectionMatrixInverse = gl.glGetUniformLocation(shaderProgramId, "glProjectionMatrixInverse");
			locs.glModelMatrix = gl.glGetUniformLocation(shaderProgramId, "glModelMatrix");
			locs.glViewMatrix = gl.glGetUniformLocation(shaderProgramId, "glViewMatrix");
			locs.glModelViewMatrix = gl.glGetUniformLocation(shaderProgramId, "glModelViewMatrix");
			locs.glModelViewMatrixInverse = gl.glGetUniformLocation(shaderProgramId, "glModelViewMatrixInverse");
			locs.glModelViewProjectionMatrix = gl.glGetUniformLocation(shaderProgramId, "glModelViewProjectionMatrix");
			locs.glNormalMatrix = gl.glGetUniformLocation(shaderProgramId, "glNormalMatrix");
			locs.ignoreVertexColors = gl.glGetUniformLocation(shaderProgramId, "ignoreVertexColors");
			locs.glFrontMaterialdiffuse = gl.glGetUniformLocation(shaderProgramId, "glFrontMaterialdiffuse");
			locs.glFrontMaterialemission = gl.glGetUniformLocation(shaderProgramId, "glFrontMaterialemission");
			locs.glFrontMaterialspecular = gl.glGetUniformLocation(shaderProgramId, "glFrontMaterialspecular");
			locs.glFrontMaterialshininess = gl.glGetUniformLocation(shaderProgramId, "glFrontMaterialshininess");
			locs.glLightModelambient = gl.glGetUniformLocation(shaderProgramId, "glLightModelambient");
			locs.objectColor = gl.glGetUniformLocation(shaderProgramId, "objectColor");
			locs.glLightSource0position = gl.glGetUniformLocation(shaderProgramId, "glLightSource0position");
			locs.glLightSource0diffuse = gl.glGetUniformLocation(shaderProgramId, "glLightSource0diffuse");
			locs.alphaTestEnabled = gl.glGetUniformLocation(shaderProgramId, "alphaTestEnabled");
			locs.alphaTestFunction = gl.glGetUniformLocation(shaderProgramId, "alphaTestFunction");
			locs.alphaTestValue = gl.glGetUniformLocation(shaderProgramId, "alphaTestValue");
			locs.textureTransform = gl.glGetUniformLocation(shaderProgramId, "textureTransform");

			//attributes
			locs.glVertex = gl.glGetAttribLocation(shaderProgramId, "glVertex");
			locs.glColor = gl.glGetAttribLocation(shaderProgramId, "glColor");
			locs.glNormal = gl.glGetAttribLocation(shaderProgramId, "glNormal");

			// tex coords, notice the vertex attribute is made of a string concat
			for (int i = 0; i < locs.glMultiTexCoord.length; i++)
			{
				locs.glMultiTexCoord[i] = gl.glGetAttribLocation(shaderProgramId, "glMultiTexCoord" + i);
			}

			//generic attributes, notice allocated on a program basis not per geom				 
			HashMap<String, Integer> attToIndex = pd.progToGenVertAttNameToGenVertAttIndex;
			if (attToIndex != null)
			{
				for (String attrib : attToIndex.keySet())
				{
					int index = attToIndex.get(attrib);
					int attribLoc = gl.glGetAttribLocation(shaderProgramId, attrib);
					locs.genAttIndexToLoc.put(index, new Integer(attribLoc));
				}
			}

			pd.programToLocationData = locs;
		}
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	private static GeometryData loadAllBuffers(JoglesContext ctx, GL2ES2 gl, GeometryArrayRetained geo, boolean ignoreVertexColors,
			int vertexCount, int vformat, int vdefined, FloatBuffer fverts, DoubleBuffer dverts, FloatBuffer fclrs, ByteBuffer bclrs,
			FloatBuffer norms, int vertexAttrCount, int[] vertexAttrSizes, FloatBuffer[] vertexAttrBufs, int texCoordMapLength,
			int[] texCoordSetMap, int texStride, Object[] texCoords)
	{
		if (VERBOSE)
			System.err.println("private static GeometryData loadAllBuffers");

		GeometryData gd = ctx.allGeometryData.get(geo.nativeId);
		if (gd == null)
		{
			gd = new GeometryData();
			geo.nativeId = gd.nativeId;
			ctx.allGeometryData.put(geo.nativeId, gd);
		}

		boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
		boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
		boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
		boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
		boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
		boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
		boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

		if (floatCoordDefined)
		{
			if (gd.geoToCoordBuf == -1)
			{
				//can it change ever? (GeometryArray.ALLOW_REF_DATA_WRITE is just my indicator of this feature)			 
				boolean morphable = ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_REF_DATA_WRITE)
						|| ((GeometryArray) geo.source).getCapability(GeometryArray.ALLOW_COORDINATE_WRITE);

				fverts.position(0);

				if (morphable)
				{
					int[] tmp = new int[2];
					gl.glGenBuffers(2, tmp, 0);
					gd.geoToCoordBuf = tmp[0];
					gd.geoToCoordBuf1 = tmp[0];
					gd.geoToCoordBuf2 = tmp[1];
					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf1);
					int usage = morphable ? GL2ES2.GL_DYNAMIC_DRAW : GL2ES2.GL_STATIC_DRAW;
					gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts, usage);

					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf2);
					gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts, usage);
				}
				else
				{
					int[] tmp = new int[1];
					gl.glGenBuffers(1, tmp, 0);
					gd.geoToCoordBuf = tmp[0];

					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToCoordBuf);
					int usage = morphable ? GL2ES2.GL_DYNAMIC_DRAW : GL2ES2.GL_STATIC_DRAW;
					gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (fverts.remaining() * Float.SIZE / 8), fverts, usage);
				}
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				gd.geoToCoordBufSize = fverts.remaining();

				if (ctx.allGeometryData.size() % 500 == 0)
				{
					System.out.println("Coord buffer count " + ctx.allGeometryData.size());
				}

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glBufferData++;

			}
		}

		if (floatColorsDefined && !ignoreVertexColors)
		{
			if (gd.geoToColorBuf == -1)
			{
				fclrs.position(0);
				int[] tmp = new int[1];
				gl.glGenBuffers(1, tmp, 0);
				gd.geoToColorBuf = tmp[0];

				gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToColorBuf);
				gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, fclrs.remaining() * Float.SIZE / 8, fclrs, GL2ES2.GL_STATIC_DRAW);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glBufferData++;

			}
		}

		if (normalsDefined)
		{
			if (gd.geoToNormalBuf == -1)
			{
				norms.position(0);

				int[] tmp = new int[1];
				gl.glGenBuffers(1, tmp, 0);
				gd.geoToNormalBuf = tmp[0];

				gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.geoToNormalBuf);
				gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, norms.remaining() * Float.SIZE / 8, norms, GL2ES2.GL_STATIC_DRAW);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (OUTPUT_PER_FRAME_STATS)
					ctx.perFrameStats.glBufferData++;

			}
		}

		if (vattrDefined)
		{
			for (int index = 0; index < vertexAttrCount; index++)
			{
				FloatBuffer vertexAttrs = vertexAttrBufs[index];
				vertexAttrs.position(0);

				SparseArray<Integer> bufIds = gd.geoToVertAttribBuf;
				if (bufIds == null)
				{
					bufIds = new SparseArray<Integer>();
					gd.geoToVertAttribBuf = bufIds;
				}

				Integer bufId = bufIds.get(index);
				if (bufId == null)
				{
					int[] tmp2 = new int[1];
					gl.glGenBuffers(1, tmp2, 0);
					bufId = new Integer(tmp2[0]);
					bufIds.put(index, bufId);

					gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, bufId.intValue());
					gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, vertexAttrs.remaining() * Float.SIZE / 8, vertexAttrs, GL2ES2.GL_STATIC_DRAW);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);

					if (OUTPUT_PER_FRAME_STATS)
						ctx.perFrameStats.glBufferData++;

				}
			}
		}

		if (textureDefined)
		{
			boolean[] texSetsLoaded = new boolean[texCoords.length];
			for (int texUnit = 0; texUnit < texCoordMapLength; texUnit++)
			{
				int texSet = texCoordSetMap[texUnit];
				if (texSet != -1 && !texSetsLoaded[texSet])
				{
					texSetsLoaded[texSet] = true;
					//stupid interface...
					FloatBuffer buf = (FloatBuffer) texCoords[texSet];
					buf.position(0);

					SparseArray<Integer> bufIds = gd.geoToTexCoordsBuf;
					if (bufIds == null)
					{
						bufIds = new SparseArray<Integer>();
						gd.geoToTexCoordsBuf = bufIds;
					}

					Integer bufId = bufIds.get(texUnit);
					if (bufId == null)
					{
						int[] tmp = new int[1];
						gl.glGenBuffers(1, tmp, 0);
						bufId = new Integer(tmp[0]);

						gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, bufId.intValue());
						gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, buf.remaining() * Float.SIZE / 8, buf, GL2ES2.GL_STATIC_DRAW);
						if (DO_OUTPUT_ERRORS)
							outputErrors(ctx);
						bufIds.put(texUnit, bufId);

						if (OUTPUT_PER_FRAME_STATS)
							ctx.perFrameStats.glBufferData++;
					}
				}
			}
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

		return gd;
	}

	private static GeometryData loadInterleavedBuffer(JoglesContext ctx, GL2ES2 gl, GeometryArrayRetained geo, boolean ignoreVertexColors,
			int vertexCount, int vformat, int vdefined, FloatBuffer fverts, DoubleBuffer dverts, FloatBuffer fclrs, ByteBuffer bclrs,
			FloatBuffer norms, int vertexAttrCount, int[] vertexAttrSizes, FloatBuffer[] vertexAttrBufs, int texCoordMapLength,
			int[] texCoordSetMap, int texStride, Object[] texCoords)
	{
		if (VERBOSE)
			System.err.println("private static GeometryData loadInterleavedBuffer");

		GeometryData gd = ctx.allGeometryData.get(geo.nativeId);
		if (gd == null)
		{
			gd = new GeometryData();
			geo.nativeId = gd.nativeId;
			ctx.allGeometryData.put(geo.nativeId, gd);
		}

		if (gd.interleavedBufId == -1)
		{
			boolean floatCoordDefined = ((vdefined & GeometryArrayRetained.COORD_FLOAT) != 0);
			boolean doubleCoordDefined = ((vdefined & GeometryArrayRetained.COORD_DOUBLE) != 0);
			boolean floatColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_FLOAT) != 0);
			boolean byteColorsDefined = ((vdefined & GeometryArrayRetained.COLOR_BYTE) != 0);
			boolean normalsDefined = ((vdefined & GeometryArrayRetained.NORMAL_FLOAT) != 0);
			boolean vattrDefined = ((vdefined & GeometryArrayRetained.VATTR_FLOAT) != 0);
			boolean textureDefined = ((vdefined & GeometryArrayRetained.TEXCOORD_FLOAT) != 0);

			//NOTE interleaving is only done on the data inside the geometry
			// we don't consider the shader slots at all, as one geometry
			// can be used with any shader, e.g. physics appearance with a NiTriShape as in morrowind
			// notice also building the interleaved ignore numActiveTextureUnits
			ByteBuffer interleavedBuffer = null;
			ByteBuffer coordBuffer = null;

			if (geo instanceof JoglesIndexedTriangleArrayRetained)
			{
				JoglesIndexedTriangleArrayRetained src = (JoglesIndexedTriangleArrayRetained) geo;
				gd.interleavedStride = src.interleavedStride;
				gd.geoToCoordOffset = src.geoToCoordOffset;
				gd.geoToColorsOffset = src.geoToColorsOffset;
				gd.geoToNormalsOffset = src.geoToNormalsOffset;
				gd.geoToVattrOffset = src.geoToVattrOffset;
				gd.geoToTexCoordOffset = src.geoToTexCoordOffset;
				interleavedBuffer = src.interleavedBuffer;
				coordBuffer = src.coordBuffer;
			}
			else if (geo instanceof JoglesIndexedTriangleStripArrayRetained)
			{
				JoglesIndexedTriangleStripArrayRetained src = (JoglesIndexedTriangleStripArrayRetained) geo;
				gd.interleavedStride = src.interleavedStride;
				gd.geoToCoordOffset = src.geoToCoordOffset;
				gd.geoToColorsOffset = src.geoToColorsOffset;
				gd.geoToNormalsOffset = src.geoToNormalsOffset;
				gd.geoToVattrOffset = src.geoToVattrOffset;
				gd.geoToTexCoordOffset = src.geoToTexCoordOffset;
				interleavedBuffer = src.interleavedBuffer;
				coordBuffer = src.coordBuffer;
			}
			else
			{
				//TODO: morphables can come in here too, just reduce stride and set up the coordBuffer  

				// how big are we going to require?
				gd.interleavedStride = 0;
				int offset = 0;
				if (floatCoordDefined)
				{
					gd.geoToCoordOffset = offset;
					if (COMPRESS_OPTIMIZED_VERTICES)
					{
						offset += 8;// 3 half float = 6 align on 4						
					}
					else
					{
						offset += 4 * 3;
					}
					fverts.position(0);
				}

				if (floatColorsDefined && !ignoreVertexColors)
				{
					gd.geoToColorsOffset = offset;

					int sz = ((vformat & GeometryArray.WITH_ALPHA) != 0) ? 4 : 3;
					if (COMPRESS_OPTIMIZED_VERTICES)
					{
						offset += 4;// minimum alignment
					}
					else
					{
						offset += 4 * sz;
					}
					fclrs.position(0);
				}

				if (normalsDefined)
				{
					gd.geoToNormalsOffset = offset;
					if (COMPRESS_OPTIMIZED_VERTICES)
					{
						offset += 4;// minimum alignment
					}
					else
					{
						offset += 4 * 3;
					}
					norms.position(0);
				}

				if (vattrDefined)
				{
					for (int index = 0; index < vertexAttrCount; index++)
					{
						gd.geoToVattrOffset[index] = offset;

						int sz = vertexAttrSizes[index];
						if (COMPRESS_OPTIMIZED_VERTICES)
						{
							offset += 4 * (int) Math.ceil(sz / 4.0);// minimum alignment maths to make it 4 aligned
						}
						else
						{
							offset += 4 * sz;
						}

						FloatBuffer vertexAttrs = vertexAttrBufs[index];
						vertexAttrs.position(0);

					}
				}

				if (textureDefined)
				{
					boolean[] texSetsLoaded = new boolean[texCoords.length];
					for (int texUnit = 0; texUnit < texCoordMapLength; texUnit++)
					{
						int texSet = texCoordSetMap[texUnit];
						if (texSet != -1 && !texSetsLoaded[texSet])
						{
							texSetsLoaded[texSet] = true;
							gd.geoToTexCoordOffset[texSet] = offset;
							if (COMPRESS_OPTIMIZED_VERTICES)
							{
								// note half floats sized
								int stride = (texStride == 2 ? 4 : 8);// minimum alignment 4 
								offset += stride;
							}
							else
							{
								offset += 4 * texStride;
							}
							FloatBuffer buf = (FloatBuffer) texCoords[texSet];
							buf.position(0);
						}
					}
				}

				gd.interleavedStride = offset;

				interleavedBuffer = ByteBuffer.allocateDirect(vertexCount * gd.interleavedStride);
				interleavedBuffer.order(ByteOrder.nativeOrder());

				for (int i = 0; i < vertexCount; i++)
				{
					interleavedBuffer.position(i * gd.interleavedStride);
					if (floatCoordDefined)
					{
						if (COMPRESS_OPTIMIZED_VERTICES)
						{
							int startPos = interleavedBuffer.position();
							for (int c = 0; c < 3; c++)
							{
								short hf = (short) JoglesMatrixUtil.halfFromFloat(fverts.get());
								interleavedBuffer.putShort(hf);
							}

							interleavedBuffer.position(startPos + 8);// minimum alignment of 2*3 is 8
						}
						else
						{
							FloatBuffer fb = interleavedBuffer.asFloatBuffer();
							for (int c = 0; c < 3; c++)
								fb.put(fverts.get());

							interleavedBuffer.position(interleavedBuffer.position() + (4 * 3));
						}
					}

					if (floatColorsDefined && !ignoreVertexColors)
					{
						int sz = ((vformat & GeometryArray.WITH_ALPHA) != 0) ? 4 : 3;
						if (COMPRESS_OPTIMIZED_VERTICES)
						{
							int startPos = interleavedBuffer.position();
							for (int c = 0; c < sz; c++)
								interleavedBuffer.put((byte) (fclrs.get() * 255));

							interleavedBuffer.position(startPos + 4);// minimum alignment
						}
						else
						{
							FloatBuffer fb = interleavedBuffer.asFloatBuffer();
							for (int c = 0; c < sz; c++)
								fb.put(fclrs.get());

							interleavedBuffer.position(interleavedBuffer.position() + (4 * sz));
						}

					}
					if (normalsDefined)
					{
						if (COMPRESS_OPTIMIZED_VERTICES)
						{
							int startPos = interleavedBuffer.position();
							for (int c = 0; c < 3; c++)
								interleavedBuffer.put((byte) (((norms.get() * 255) - 1) / 2f));

							interleavedBuffer.position(startPos + 4);// minimum alignment
						}
						else
						{
							FloatBuffer fb = interleavedBuffer.asFloatBuffer();
							for (int c = 0; c < 3; c++)
								fb.put(norms.get());

							interleavedBuffer.position(interleavedBuffer.position() + (4 * 3));
						}
					}

					if (vattrDefined)
					{
						for (int index = 0; index < vertexAttrCount; index++)
						{
							int sz = vertexAttrSizes[index];
							FloatBuffer vertexAttrs = vertexAttrBufs[index];
							if (COMPRESS_OPTIMIZED_VERTICES)
							{
								int startPos = interleavedBuffer.position();
								for (int va = 0; va < sz; va++)
									interleavedBuffer.put((byte) (((vertexAttrs.get() * 255) - 1) / 2f));

								interleavedBuffer.position(startPos + (4 * (int) Math.ceil(sz / 4.0)));// minimum alignment
							}
							else
							{
								FloatBuffer fb = interleavedBuffer.asFloatBuffer();
								for (int va = 0; va < sz; va++)
									fb.put(vertexAttrs.get());

								interleavedBuffer.position(interleavedBuffer.position() + (4 * sz));
							}
						}
					}

					if (textureDefined)
					{
						boolean[] texSetsLoaded = new boolean[texCoords.length];
						for (int texUnit = 0; texUnit < texCoordMapLength; texUnit++)
						{
							int texSet = texCoordSetMap[texUnit];
							if (texSet != -1 && !texSetsLoaded[texSet])
							{
								texSetsLoaded[texSet] = true;
								FloatBuffer tcBuf = (FloatBuffer) texCoords[texSet];

								if (COMPRESS_OPTIMIZED_VERTICES)
								{
									int startPos = interleavedBuffer.position();
									for (int c = 0; c < texStride; c++)
									{
										short hf = (short) JoglesMatrixUtil.halfFromFloat(tcBuf.get());
										interleavedBuffer.putShort(hf);
									}

									interleavedBuffer.position(startPos + (texStride == 2 ? 4 : 8));// minimum alignment
								}
								else
								{
									FloatBuffer fb = interleavedBuffer.asFloatBuffer();
									for (int tc = 0; tc < texStride; tc++)
										fb.put(tcBuf.get());

									interleavedBuffer.position(interleavedBuffer.position() + (4 * texStride));
								}
							}
						}
					}

				}
			}
			interleavedBuffer.position(0);
			int[] tmp = new int[1];
			gl.glGenBuffers(1, tmp, 0);
			gd.interleavedBufId = tmp[0];

			gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.interleavedBufId);
			gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, interleavedBuffer.remaining(), interleavedBuffer, GL2ES2.GL_STATIC_DRAW);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			if (OUTPUT_PER_FRAME_STATS)
				ctx.perFrameStats.interleavedBufferCreated++;

			if (coordBuffer != null)
			{
				coordBuffer.position(0);
				int[] tmp2 = new int[1];
				gl.glGenBuffers(1, tmp2, 0);
				gd.coordBufId = tmp2[0];

				gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, gd.coordBufId);
				gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, coordBuffer.remaining(), coordBuffer, GL2ES2.GL_DYNAMIC_DRAW);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}

		}

		return gd;

	}

	//--Noop
	@Override
	void setVertexFormat(Context ctx, GeometryArrayRetained geo, int vformat, boolean useAlpha, boolean ignoreVertexColors)
	{
		//if (VERBOSE)
		//	System.err.println("JoglPipeline.setVertexFormat()");
	}

	// called by the 2 executes above

	// ---------------------------------------------------------------------

	//
	// GLSLShaderProgramRetained methods
	//

	// ShaderAttributeValue methods

	@Override
	ShaderError setGLSLUniform1i(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform1i(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value=" + value + ")");

		JoglesContext joglesctx = (JoglesContext) ctx;
		GL2ES2 gl = joglesctx.gl2es2;
		int loc = unbox(uniformLocation);
		if (!MINIMISE_NATIVE_SHADER || joglesctx.gl_state.setGLSLUniform1i[loc] != value)
		{
			gl.glUniform1i(loc, value);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_SHADER)
				joglesctx.gl_state.setGLSLUniform1i[loc] = value;
		}
		return null;
	}

	@Override
	ShaderError setGLSLUniform1f(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, float value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform1f(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value=" + value + ")");

		JoglesContext joglesctx = (JoglesContext) ctx;
		GL2ES2 gl = joglesctx.gl2es2;
		int loc = unbox(uniformLocation);
		if (!MINIMISE_NATIVE_SHADER || joglesctx.gl_state.setGLSLUniform1f[loc] != value)
		{
			gl.glUniform1f(loc, value);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_SHADER)
				joglesctx.gl_state.setGLSLUniform1f[loc] = value;
		}
		return null;
	}

	@Override
	ShaderError setGLSLUniform2i(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform2i(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform2i(unbox(uniformLocation), value[0], value[1]);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform2f(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform2f(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform2f(unbox(uniformLocation), value[0], value[1]);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform3i(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform3i(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform3i(unbox(uniformLocation), value[0], value[1], value[2]);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform3f(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform3f(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform3f(unbox(uniformLocation), value[0], value[1], value[2]);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform4i(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform4i(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform4i(unbox(uniformLocation), value[0], value[1], value[2], value[3]);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform4f(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform4f(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform4f(unbox(uniformLocation), value[0], value[1], value[2], value[3]);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix3f(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniformMatrix3f(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		// Load attribute
		// transpose is true : each matrix is supplied in row major order
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniformMatrix3fv(unbox(uniformLocation), 1, false, ((JoglesContext) ctx).matrixUtil.toFB3(value));
		//gl.glUniformMatrix3fv(unbox(uniformLocation), 1, true, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix4f(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniformMatrix4f(shaderProgramId = " + unbox(shaderProgramId) + ",uniformLocation="
					+ unbox(uniformLocation) + ",value[0]=" + value[0] + ")");

		// Load attribute
		// transpose is true : each matrix is supplied in row major order
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniformMatrix4fv(unbox(uniformLocation), 1, false, ((JoglesContext) ctx).matrixUtil.toFB4(value));
		//gl.glUniformMatrix4fv(unbox(uniformLocation), 1, true, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	// ShaderAttributeArray methods

	@Override
	ShaderError setGLSLUniform1iArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform1iArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform1iv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform1fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform1fArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform1fv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform2iArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform2iArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform2iv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform2fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform2fArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform2fv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform3iArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform3iArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform3iv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform3fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform3fArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform3fv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform4iArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			int[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform4iArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform4iv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniform4fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniform4fArray()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniform4fv(unbox(uniformLocation), numElements, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix3fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniformMatrix3fArray()");

		// Load attribute
		// transpose is true : each matrix is supplied in row major order
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniformMatrix3fv(unbox(uniformLocation), numElements, true, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError setGLSLUniformMatrix4fArray(Context ctx, ShaderProgramId shaderProgramId, ShaderAttrLoc uniformLocation, int numElements,
			float[] value)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setGLSLUniformMatrix4fArray()");

		// Load attribute
		// transpose is true : each matrix is supplied in row major order
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glUniformMatrix4fv(unbox(uniformLocation), numElements, true, value, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	// interfaces for shader compilation, etc.
	@Override
	ShaderError createGLSLShader(Context ctx, int shaderType, ShaderId[] shaderId)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.createGLSLShader()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.createGLSLShader++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		int shaderHandle = 0;
		if (shaderType == Shader.SHADER_TYPE_VERTEX)
		{
			shaderHandle = (int) gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER);
		}
		else if (shaderType == Shader.SHADER_TYPE_FRAGMENT)
		{
			shaderHandle = (int) gl.glCreateShader(GL2ES2.GL_FRAGMENT_SHADER);
		}
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		if (shaderHandle == 0)
		{
			return new ShaderError(ShaderError.COMPILE_ERROR, "Unable to create native shader object");
		}

		shaderId[0] = new JoglShaderObject(shaderHandle);

		return null;
	}

	@Override
	ShaderError destroyGLSLShader(Context ctx, ShaderId shaderId)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.destroyGLSLShader()");

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.destroyGLSLShader++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glDeleteShader(unbox(shaderId));
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		return null;
	}

	@Override
	ShaderError compileGLSLShader(Context ctx, ShaderId shaderId, String program)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.compileGLSLShader()");

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.compileGLSLShader++;

		int id = unbox(shaderId);
		if (id == 0)
		{
			throw new AssertionError("shaderId == 0");
		}

		if (program == null)
		{
			throw new AssertionError("shader program string is null");
		}

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		gl.glShaderSource(id, 1, new String[] { program }, null, 0);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		gl.glCompileShader(id);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		int[] status = new int[1];
		gl.glGetShaderiv(id, GL2ES2.GL_COMPILE_STATUS, status, 0);
		if (status[0] == 0)
		{
			String detailMsg = getShaderInfoLog(gl, id);
			ShaderError res = new ShaderError(ShaderError.COMPILE_ERROR, "GLSL shader compile error");
			res.setDetailMessage(detailMsg);
			return res;
		}
		return null;
	}

	@Override
	ShaderError createGLSLShaderProgram(Context ctx, ShaderProgramId[] shaderProgramId)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.createGLSLShaderProgram()");

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.createGLSLShaderProgram++;

		int shaderProgramHandle = (int) gl.glCreateProgram();
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		if (shaderProgramHandle == 0)
		{
			return new ShaderError(ShaderError.LINK_ERROR, "Unable to create native shader program object");
		}
		shaderProgramId[0] = new JoglShaderObject(shaderProgramHandle);

		return null;
	}

	@Override
	ShaderError destroyGLSLShaderProgram(Context ctx, ShaderProgramId shaderProgramId)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.destroyGLSLShaderProgram()");

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.destroyGLSLShaderProgram++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glDeleteShader(unbox(shaderProgramId));
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

		// just dump data
		((JoglesContext) ctx).allProgramData.remove(unbox(shaderProgramId));

		return null;
	}

	@Override
	ShaderError linkGLSLShaderProgram(Context ctx, ShaderProgramId shaderProgramId, ShaderId[] shaderIds)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.linkGLSLShaderProgram()");

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.linkGLSLShaderProgram++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		int id = unbox(shaderProgramId);
		for (int i = 0; i < shaderIds.length; i++)
		{
			gl.glAttachShader(id, unbox(shaderIds[i]));
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}
		gl.glLinkProgram(id);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		int[] status = new int[1];
		gl.glGetProgramiv(id, GL2ES2.GL_LINK_STATUS, status, 0);
		if (status[0] == 0)
		{
			String detailMsg = getProgramInfoLog(gl, id);
			ShaderError res = new ShaderError(ShaderError.LINK_ERROR, "GLSL shader program link error");
			res.setDetailMessage(detailMsg);
			return res;
		}
		return null;
	}

	@Override
	ShaderError bindGLSLVertexAttrName(Context ctx, ShaderProgramId shaderProgramId, String attrName, int attrIndex)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.bindGLSLVertexAttrName()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.bindGLSLVertexAttrName++;

		//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
		//gl.glBindAttribLocation(unbox(shaderProgramId), attrIndex + VirtualUniverse.mc.glslVertexAttrOffset, attrName);

		// record this for later, we'll get real locations in the locationData setup
		int progId = unbox(shaderProgramId);
		JoglesContext joglesContext = (JoglesContext) ctx;
		ProgramData pd = joglesContext.allProgramData.get(progId);
		if (pd == null)
		{
			pd = new ProgramData();
			joglesContext.allProgramData.put(progId, pd);
		}

		HashMap<String, Integer> attToIndex = pd.progToGenVertAttNameToGenVertAttIndex;
		if (attToIndex == null)
		{
			attToIndex = new HashMap<String, Integer>();
			pd.progToGenVertAttNameToGenVertAttIndex = attToIndex;
		}

		attToIndex.put(attrName, attrIndex);

		return null;
	}

	@Override
	void lookupGLSLShaderAttrNames(Context ctx, ShaderProgramId shaderProgramId, int numAttrNames, String[] attrNames,
			ShaderAttrLoc[] locArr, int[] typeArr, int[] sizeArr, boolean[] isArrayArr)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.lookupGLSLShaderAttrNames()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.lookupGLSLShaderAttrNames++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		// set the loc, type, and size arrays to out-of-bound values
		for (int i = 0; i < attrNames.length; i++)
		{
			locArr[i] = null;
			typeArr[i] = -1;
			sizeArr[i] = -1;
		}

		// Loop through the list of active uniform variables, one at a
		// time, searching for a match in the attrNames array.
		//
		// NOTE: Since attrNames isn't sorted, and we don't have a
		// hashtable of names to index locations, we will do a
		// brute-force, linear search of the array. This leads to an
		// O(n^2) algorithm (actually O(n*m) where n is attrNames.length
		// and m is the number of uniform variables), but since we expect
		// N to be small, we will not optimize this at this time.
		int id = unbox(shaderProgramId);
		int[] tmp = new int[1];
		int[] tmp2 = new int[1];
		int[] tmp3 = new int[1];

		gl.glGetProgramiv(id, GL2ES2.GL_ACTIVE_UNIFORMS, tmp, 0);
		int numActiveUniforms = tmp[0];
		gl.glGetProgramiv(id, GL2ES2.GL_ACTIVE_UNIFORM_MAX_LENGTH, tmp, 0);
		int maxStrLen = tmp[0];
		byte[] nameBuf = new byte[maxStrLen];

		for (int i = 0; i < numActiveUniforms; i++)
		{
			gl.glGetActiveUniform(id, i, maxStrLen, tmp3, 0, tmp, 0, tmp2, 0, nameBuf, 0);
			int size = tmp[0];
			int type = tmp2[0];
			String name = null;
			try
			{
				name = new String(nameBuf, 0, tmp3[0], "US-ASCII");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}

			// Issue 247 - we need to workaround an ATI bug where they erroneously
			// report individual elements of arrays rather than the array itself
			if (name.length() >= 3 && name.endsWith("]"))
			{
				if (name.endsWith("[0]"))
				{
					name = name.substring(0, name.length() - 3);
				}
				else
				{
					// Ignore this name
					continue;
				}
			}

			// Now try to find the name
			for (int j = 0; j < numAttrNames; j++)
			{
				if (name.equals(attrNames[j]))
				{
					sizeArr[j] = size;
					isArrayArr[j] = (size > 1);
					typeArr[j] = glslToJ3dType(type);
					break;
				}
			}
		}

		// Now lookup the location of each name in the attrNames array
		for (int i = 0; i < numAttrNames; i++)
		{
			// Get uniform attribute location
			int loc = gl.glGetUniformLocation(id, attrNames[i]);
			locArr[i] = new JoglShaderObject(loc);
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	// good ideas about uniform debugging
	//http://stackoverflow.com/questions/26164602/unexplainable-gl-invalid-operation-from-gluniform1i-opengl-thinks-an-int-is-a-f
	private boolean USE_NULL_SHADER_WARNING_GIVEN = false;

	@Override
	ShaderError useGLSLShaderProgram(Context ctx, ShaderProgramId shaderProgramId)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.useGLSLShaderProgram(shaderProgramId=" + unbox(shaderProgramId) + ")");

		JoglesContext joglesContext = (JoglesContext) ctx;
		if (OUTPUT_PER_FRAME_STATS)
		{
			if (joglesContext.gl_state.currentProgramId == unbox(shaderProgramId))
			{
				joglesContext.perFrameStats.redundantUseProgram++;
			}
			else
			{
				joglesContext.perFrameStats.useGLSLShaderProgram++;
				joglesContext.perFrameStats.usedPrograms.add(shaderProgramId);
			}
		}

		if (!MINIMISE_NATIVE_SHADER || joglesContext.gl_state.currentProgramId != unbox(shaderProgramId))
		{
			if (shaderProgramId == null)
			{
				if (!USE_NULL_SHADER_WARNING_GIVEN)
					System.err.println("Null shader passed for use");
				USE_NULL_SHADER_WARNING_GIVEN = true;
			}

			GL2ES2 gl = joglesContext.gl2es2;

			gl.glUseProgram(unbox(shaderProgramId));
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			joglesContext.setShaderProgram((JoglShaderObject) shaderProgramId);
			loadLocs(joglesContext, gl);

			if (MINIMISE_NATIVE_SHADER)
				joglesContext.gl_state.currentProgramId = unbox(shaderProgramId);

		}
		return null;
	}

	//----------------------------------------------------------------------
	// Helper methods for above shader routines
	//
	private static int unbox(ShaderAttrLoc loc)
	{
		if (loc == null)
			return 0;
		return ((JoglShaderObject) loc).getValue();
	}

	private static int unbox(ShaderProgramId id)
	{
		if (id == null)
			return 0;
		return ((JoglShaderObject) id).getValue();
	}

	private static int unbox(ShaderId id)
	{
		if (id == null)
			return 0;
		return ((JoglShaderObject) id).getValue();
	}

	private static String getShaderInfoLog(GL2ES2 gl, int id)
	{
		int[] infoLogLength = new int[1];
		gl.glGetShaderiv(id, GL2ES2.GL_INFO_LOG_LENGTH, infoLogLength, 0);
		if (infoLogLength[0] > 0)
		{
			byte[] storage = new byte[infoLogLength[0]];
			int[] len = new int[1];
			gl.glGetShaderInfoLog(id, infoLogLength[0], len, 0, storage, 0);
			try
			{
				return new String(storage, 0, len[0], "US-ASCII");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private static String getProgramInfoLog(GL2ES2 gl, int id)
	{
		int[] infoLogLength = new int[1];
		gl.glGetProgramiv(id, GL2ES2.GL_INFO_LOG_LENGTH, infoLogLength, 0);
		if (infoLogLength[0] > 0)
		{
			byte[] storage = new byte[infoLogLength[0]];
			int[] len = new int[1];
			gl.glGetProgramInfoLog(id, infoLogLength[0], len, 0, storage, 0);
			try
			{
				return new String(storage, 0, len[0], "US-ASCII");
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private static int glslToJ3dType(int type)
	{
		switch (type)
		{
		case GL2ES2.GL_BOOL:
		case GL2ES2.GL_INT:
		case GL2ES2.GL_SAMPLER_2D:
		case GL2ES2.GL_SAMPLER_3D:
		case GL2ES2.GL_SAMPLER_CUBE:
			return ShaderAttributeObjectRetained.TYPE_INTEGER;

		case GL2ES2.GL_FLOAT:
			return ShaderAttributeObjectRetained.TYPE_FLOAT;

		case GL2ES2.GL_INT_VEC2:
		case GL2ES2.GL_BOOL_VEC2:
			return ShaderAttributeObjectRetained.TYPE_TUPLE2I;

		case GL2ES2.GL_FLOAT_VEC2:
			return ShaderAttributeObjectRetained.TYPE_TUPLE2F;

		case GL2ES2.GL_INT_VEC3:
		case GL2ES2.GL_BOOL_VEC3:
			return ShaderAttributeObjectRetained.TYPE_TUPLE3I;

		case GL2ES2.GL_FLOAT_VEC3:
			return ShaderAttributeObjectRetained.TYPE_TUPLE3F;

		case GL2ES2.GL_INT_VEC4:
		case GL2ES2.GL_BOOL_VEC4:
			return ShaderAttributeObjectRetained.TYPE_TUPLE4I;

		case GL2ES2.GL_FLOAT_VEC4:
			return ShaderAttributeObjectRetained.TYPE_TUPLE4F;

		// case GL2ES2.GL_FLOAT_MAT2:

		case GL2ES2.GL_FLOAT_MAT3:
			return ShaderAttributeObjectRetained.TYPE_MATRIX3F;

		case GL2ES2.GL_FLOAT_MAT4:
			return ShaderAttributeObjectRetained.TYPE_MATRIX4F;

		// Java 3D does not support the following sampler types:
		//
		// case GL2ES2.GL_SAMPLER_1D_ARB:
		// case GL2ES2.GL_SAMPLER_1D_SHADOW_ARB:
		// case GL2ES2.GL_SAMPLER_2D_SHADOW_ARB:
		// case GL2ES2.GL_SAMPLER_2D_RECT_ARB:
		// case GL2ES2.GL_SAMPLER_2D_RECT_SHADOW_ARB:
		}

		return -1;
	}

	// ---------------------------------------------------------------------

	//
	// DirectionalLightRetained methods
	//

	private static final Vector4f black = new Vector4f();

	@Override
	void updateDirectionalLight(Context ctx, int lightSlot, float red, float green, float blue, float dirx, float diry, float dirz)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateDirectionalLight()");

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateDirectionalLight++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			// OK ES2 requires lights to be handed across manually
			// so once again just force it in there and assume shader has it
		
			int lightNum = GL2ES2.GL_LIGHT0 + lightSlot;
			float[] values = new float[4];
		
			values[0] = red;
			values[1] = green;
			values[2] = blue;
			values[3] = 1.0f;
			gl.glLightfv(lightNum, GL2ES2.GL_DIFFUSE, values, 0);
			gl.glLightfv(lightNum, GL2ES2.GL_SPECULAR, values, 0);
			values[0] = -dirx;
			values[1] = -diry;
			values[2] = -dirz;
			values[3] = 0.0f;
			gl.glLightfv(lightNum, GL2ES2.GL_POSITION, values, 0);
			gl.glLightfv(lightNum, GL2ES2.GL_AMBIENT, black, 0);
			gl.glLightf(lightNum, GL2ES2.GL_POSITION, 1.0f);
			gl.glLightf(lightNum, GL2ES2.GL_LINEAR_ATTENUATION, 0.0f);
			gl.glLightf(lightNum, GL2ES2.GL_QUADRATIC_ATTENUATION, 0.0f);
			gl.glLightf(lightNum, GL2ES2.GL_SPOT_EXPONENT, 0.0f);
			gl.glLightf(lightNum, GL2ES2.GL_SPOT_CUTOFF, 180.0f);*/

		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (joglesctx.dirLight[lightSlot] == null)
			joglesctx.dirLight[lightSlot] = new LightData();

		joglesctx.dirLight[lightSlot].diffuse.x = red;
		joglesctx.dirLight[lightSlot].diffuse.y = green;
		joglesctx.dirLight[lightSlot].diffuse.z = blue;
		joglesctx.dirLight[lightSlot].diffuse.w = 1.0f;
		joglesctx.dirLight[lightSlot].specular.x = red;
		joglesctx.dirLight[lightSlot].specular.y = green;
		joglesctx.dirLight[lightSlot].specular.z = blue;
		joglesctx.dirLight[lightSlot].specular.w = 1.0f;
		joglesctx.dirLight[lightSlot].pos.x = -dirx;
		joglesctx.dirLight[lightSlot].pos.y = -diry;
		joglesctx.dirLight[lightSlot].pos.z = -dirz;
		joglesctx.dirLight[lightSlot].pos.w = 0.0f;//0 means directional light
		joglesctx.dirLight[lightSlot].ambient = black;// odd		 
		//joglesctx.dirLight[lightSlot].GL_POSITION = 1.0f; // what is this?
		joglesctx.dirLight[lightSlot].GL_CONSTANT_ATTENUATION = 1.0f;
		joglesctx.dirLight[lightSlot].GL_LINEAR_ATTENUATION = 0.0f;
		joglesctx.dirLight[lightSlot].GL_QUADRATIC_ATTENUATION = 0.0f;
		joglesctx.dirLight[lightSlot].GL_SPOT_EXPONENT = 0.0f;
		joglesctx.dirLight[lightSlot].GL_SPOT_CUTOFF = 180.0f;
	}

	// ---------------------------------------------------------------------

	//
	// PointLightRetained methods
	//

	@Override
	void updatePointLight(Context ctx, int lightSlot, float red, float green, float blue, float attenx, float atteny, float attenz,
			float posx, float posy, float posz)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updatePointLight()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updatePointLight++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			// OK ES2 requires lights to be handed across manually		
			// so once again just force it in there and assume shader has it
		
			int lightNum = GL2ES2.GL_LIGHT0 + lightSlot;
			float[] values = new float[4];
		
			values[0] = red;
			values[1] = green;
			values[2] = blue;
			values[3] = 1.0f;
			gl.glLightfv(lightNum, GL2ES2.GL_DIFFUSE, values, 0);
			gl.glLightfv(lightNum, GL2ES2.GL_SPECULAR, values, 0);
			gl.glLightfv(lightNum, GL2ES2.GL_AMBIENT, black, 0);
			values[0] = posx;
			values[1] = posy;
			values[2] = posz;
			gl.glLightfv(lightNum, GL2ES2.GL_POSITION, values, 0);
			gl.glLightf(lightNum, GL2ES2.GL_CONSTANT_ATTENUATION, attenx);
			gl.glLightf(lightNum, GL2ES2.GL_LINEAR_ATTENUATION, atteny);
			gl.glLightf(lightNum, GL2ES2.GL_QUADRATIC_ATTENUATION, attenz);
			gl.glLightf(lightNum, GL2ES2.GL_SPOT_EXPONENT, 0.0f);
			gl.glLightf(lightNum, GL2ES2.GL_SPOT_CUTOFF, 180.0f);*/

		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (joglesctx.pointLight[lightSlot] == null)
			joglesctx.pointLight[lightSlot] = new LightData();

		joglesctx.pointLight[lightSlot].diffuse.x = red;
		joglesctx.pointLight[lightSlot].diffuse.y = green;
		joglesctx.pointLight[lightSlot].diffuse.z = blue;
		joglesctx.pointLight[lightSlot].diffuse.w = 1.0f;
		joglesctx.pointLight[lightSlot].specular.x = red;
		joglesctx.pointLight[lightSlot].specular.y = green;
		joglesctx.pointLight[lightSlot].specular.z = blue;
		joglesctx.pointLight[lightSlot].specular.w = 1.0f;
		joglesctx.pointLight[lightSlot].pos.x = posx;
		joglesctx.pointLight[lightSlot].pos.y = posy;
		joglesctx.pointLight[lightSlot].pos.z = posz;
		joglesctx.pointLight[lightSlot].pos.w = 1.0f;// 1 mean pos not dir
		joglesctx.pointLight[lightSlot].ambient = black;// odd				
		joglesctx.pointLight[lightSlot].GL_CONSTANT_ATTENUATION = attenx;
		joglesctx.pointLight[lightSlot].GL_LINEAR_ATTENUATION = atteny;
		joglesctx.pointLight[lightSlot].GL_QUADRATIC_ATTENUATION = attenz;
		joglesctx.pointLight[lightSlot].GL_SPOT_EXPONENT = 0.0f;
		joglesctx.pointLight[lightSlot].GL_SPOT_CUTOFF = 180.0f;
	}

	// ---------------------------------------------------------------------

	//
	// SpotLightRetained methods
	//

	@Override
	void updateSpotLight(Context ctx, int lightSlot, float red, float green, float blue, float attenx, float atteny, float attenz,
			float posx, float posy, float posz, float spreadAngle, float concentration, float dirx, float diry, float dirz)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateSpotLight()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateSpotLight++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			// OK ES2 requires lights to be handed across manually		
			// so once again just force it in there and assume shader has it
		
			int lightNum = GL2ES2.GL_LIGHT0 + lightSlot;
			float[] values = new float[4];
		
			values[0] = red;
			values[1] = green;
			values[2] = blue;
			values[3] = 1.0f;
			gl.glLightfv(lightNum, GL2ES2.GL_DIFFUSE, values, 0);
			gl.glLightfv(lightNum, GL2ES2.GL_SPECULAR, values, 0);
			gl.glLightfv(lightNum, GL2ES2.GL_AMBIENT, black, 0);
			values[0] = posx;
			values[1] = posy;
			values[2] = posz;
			gl.glLightfv(lightNum, GL2ES2.GL_POSITION, values, 0);
			gl.glLightf(lightNum, GL2ES2.GL_CONSTANT_ATTENUATION, attenx);
			gl.glLightf(lightNum, GL2ES2.GL_LINEAR_ATTENUATION, atteny);
			gl.glLightf(lightNum, GL2ES2.GL_QUADRATIC_ATTENUATION, attenz);
			values[0] = dirx;
			values[1] = diry;
			values[2] = dirz;
			gl.glLightfv(lightNum, GL2ES2.GL_SPOT_DIRECTION, values, 0);
			gl.glLightf(lightNum, GL2ES2.GL_SPOT_EXPONENT, concentration);
			gl.glLightf(lightNum, GL2ES2.GL_SPOT_CUTOFF, (float) (spreadAngle * 180.0f / Math.PI));*/

		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (joglesctx.spotLight[lightSlot] == null)
			joglesctx.spotLight[lightSlot] = new LightData();

		joglesctx.spotLight[lightSlot].diffuse.x = red;
		joglesctx.spotLight[lightSlot].diffuse.y = green;
		joglesctx.spotLight[lightSlot].diffuse.z = blue;
		joglesctx.spotLight[lightSlot].diffuse.w = 1.0f;
		joglesctx.spotLight[lightSlot].specular.x = red;
		joglesctx.spotLight[lightSlot].specular.y = green;
		joglesctx.spotLight[lightSlot].specular.z = blue;
		joglesctx.spotLight[lightSlot].specular.w = 1.0f;
		joglesctx.spotLight[lightSlot].pos.x = posx;
		joglesctx.spotLight[lightSlot].pos.y = posy;
		joglesctx.spotLight[lightSlot].pos.z = posz;
		joglesctx.spotLight[lightSlot].pos.w = 1.0f;// 1 mean pos not dir
		joglesctx.spotLight[lightSlot].ambient = black;// odd		 
		joglesctx.spotLight[lightSlot].GL_CONSTANT_ATTENUATION = attenx;
		joglesctx.spotLight[lightSlot].GL_LINEAR_ATTENUATION = atteny;
		joglesctx.spotLight[lightSlot].GL_QUADRATIC_ATTENUATION = attenz;
		joglesctx.spotLight[lightSlot].spotDir.x = dirx;
		joglesctx.spotLight[lightSlot].spotDir.y = diry;
		joglesctx.spotLight[lightSlot].spotDir.z = dirz;
		joglesctx.spotLight[lightSlot].spotDir.w = 1.0f;
		joglesctx.spotLight[lightSlot].GL_SPOT_EXPONENT = concentration;
		joglesctx.spotLight[lightSlot].GL_SPOT_CUTOFF = (float) (spreadAngle * 180.0f / Math.PI);

	}

	// ---------------------------------------------------------------------

	//
	// ExponentialFogRetained methods
	//

	@Override
	void updateExponentialFog(Context ctx, float red, float green, float blue, float density)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateExponentialFog()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateExponentialFog++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
		
			// apparently also lost... I don't know...
			//https://www.opengl.org/discussion_boards/showthread.php/178629-How-to-create-fog-using-Open-GL-ES-2-0-or-WebGL
		
			float[] color = new float[3];
			color[0] = red;
			color[1] = green;
			color[2] = blue;
			gl.glFogi(GL2ES2.GL_FOG_MODE, GL2ES2.GL_EXP);
			gl.glFogfv(GL2ES2.GL_FOG_COLOR, color, 0);
			gl.glFogf(GL2ES2.GL_FOG_DENSITY, density);
			gl.glEnable(GL2ES2.GL_FOG);*/

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.fogData.expColor.x = red;
		joglesctx.fogData.expColor.y = green;
		joglesctx.fogData.expColor.z = blue;
		joglesctx.fogData.expDensity = density;
		joglesctx.fogData.enable = true;
	}

	// ---------------------------------------------------------------------

	//
	// LinearFogRetained methods
	//

	@Override
	void updateLinearFog(Context ctx, float red, float green, float blue, double fdist, double bdist)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateLinearFog()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateLinearFog++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			//gone
		
			float[] color = new float[3];
			color[0] = red;
			color[1] = green;
			color[2] = blue;
			gl.glFogi(GL2ES2.GL_FOG_MODE, GL2ES2.GL_LINEAR);
			gl.glFogfv(GL2ES2.GL_FOG_COLOR, color, 0);
			gl.glFogf(GL2ES2.GL_FOG_START, (float) fdist);
			gl.glFogf(GL2ES2.GL_FOG_END, (float) bdist);
			gl.glEnable(GL2ES2.GL_FOG);*/

		//see
		//https://www.opengl.org/discussion_boards/showthread.php/151415-Fog-with-pixel-shader-%28arb_fragment_program%29

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.fogData.linearColor.x = red;
		joglesctx.fogData.linearColor.y = green;
		joglesctx.fogData.linearColor.z = blue;
		joglesctx.fogData.linearStart = (float) fdist;
		joglesctx.fogData.linearEnd = (float) bdist;
		joglesctx.fogData.enable = true;
	}

	// native method for disabling fog
	@Override

	void disableFog(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.disableFog()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.disableFog++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			// bound to be not supported as definitely shader work now
		
			//gl.glDisable(GL2ES2.GL_FOG);*/
		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.fogData.enable = false;
	}

	// native method for setting fog enable flag

	@Override
	void setFogEnableFlag(Context ctx, boolean enable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setFogEnableFlag()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setFogEnableFlag++;

		/*GL2 gl = context(ctx).getGL().getGL2();
		//fog gone from ES2, done by passng it in as values
		
		if (enable)
			gl.glEnable(GL2ES2.GL_FOG);
		else
			gl.glDisable(GL2ES2.GL_FOG);*/

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.fogData.enable = enable;
	}
	// ---------------------------------------------------------------------

	//
	// LineAttributesRetained methods
	//
	//  used by H physics
	@Override
	void updateLineAttributes(Context ctx, float lineWidth, int linePattern, int linePatternMask, int linePatternScaleFactor,
			boolean lineAntialiasing)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateLineAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateLineAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glLineWidth(lineWidth);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

	}

	// native method for setting default LineAttributes
	@Override

	void resetLineAttributes(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetLineAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetLineAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		gl.glLineWidth(1.0f);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	// ---------------------------------------------------------------------

	//
	// MaterialRetained methods
	//

	@Override
	void updateMaterial(Context ctx, float red, float green, float blue, float alpha, float aRed, float aGreen, float aBlue, float eRed,
			float eGreen, float eBlue, float dRed, float dGreen, float dBlue, float sRed, float sGreen, float sBlue, float shininess,
			int colorTarget, boolean lightEnable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateMaterial()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateMaterial++;

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.objectColor.x = red;
		joglesctx.objectColor.y = green;
		joglesctx.objectColor.z = blue;
		joglesctx.objectColor.w = alpha;
		joglesctx.materialData.lightEnabled = lightEnable;
		joglesctx.materialData.shininess = shininess;
		joglesctx.materialData.emission.x = eRed;
		joglesctx.materialData.emission.y = eGreen;
		joglesctx.materialData.emission.z = eBlue;
		joglesctx.materialData.ambient.x = aRed;
		joglesctx.materialData.ambient.y = aGreen;
		joglesctx.materialData.ambient.z = aBlue;
		joglesctx.materialData.specular.x = sRed;
		joglesctx.materialData.specular.y = sGreen;
		joglesctx.materialData.specular.z = sBlue;
		joglesctx.materialData.diffuse.x = dRed;
		joglesctx.materialData.diffuse.y = dGreen;
		joglesctx.materialData.diffuse.z = dBlue;
		joglesctx.materialData.diffuse.w = alpha;

	}

	// native method for setting Material when no material is present
	@Override

	void updateMaterialColor(Context ctx, float r, float g, float b, float a)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateMaterialColor()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateMaterialColor++;

		// update single color in case where material has color and there are no coloring attributes
		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.objectColor.x = r;
		joglesctx.objectColor.y = g;
		joglesctx.objectColor.z = b;
		joglesctx.objectColor.w = a;

	}
	// ---------------------------------------------------------------------

	//
	// ColoringAttributesRetained methods
	//used by H physics
	@Override
	void updateColoringAttributes(Context ctx, float dRed, float dGreen, float dBlue, float red, float green, float blue, float alpha,
			boolean lightEnable, int shadeModel)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateColoringAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateColoringAttributes++;

		JoglesContext joglesctx = ((JoglesContext) ctx);
		// note we ignore lightEnabled and always pass the object color to the shader if it wants it
		joglesctx.objectColor.x = red;
		joglesctx.objectColor.y = green;
		joglesctx.objectColor.z = blue;
		joglesctx.objectColor.w = alpha;

	}

	// native method for setting default ColoringAttributes
	@Override

	void resetColoringAttributes(Context ctx, float r, float g, float b, float a, boolean enableLight)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetColoringAttributes() " + r + " " + g + " " + b + " " + a + " " + enableLight);
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetColoringAttributes++;

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.objectColor.x = r;
		joglesctx.objectColor.y = g;
		joglesctx.objectColor.z = b;
		joglesctx.objectColor.w = a;
	}

	// ---------------------------------------------------------------------

	//
	// PointAttributesRetained methods
	//
	//interesting as Points are how particles are done properly!!
	//http://stackoverflow.com/questions/3497068/textured-points-in-opengl-es-2-0
	//http://stackoverflow.com/questions/7237086/opengl-es-2-0-equivalent-for-es-1-0-circles-using-gl-point-smooth

	@Override
	void updatePointAttributes(Context ctx, float pointSize, boolean pointAntialiasing)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updatePointAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updatePointAttributes++;

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.pointSize = pointSize;

	}

	// native method for setting default PointAttributes
	@Override

	void resetPointAttributes(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetPointAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetPointAttributes++;

		JoglesContext joglesctx = ((JoglesContext) ctx);
		joglesctx.pointSize = 1.0f;
	}
	// ---------------------------------------------------------------------

	//
	// PolygonAttributesRetained methods
	//

	@Override
	void updatePolygonAttributes(Context ctx, int polygonMode, int cullFace, boolean backFaceNormalFlip, float polygonOffset,
			float polygonOffsetFactor)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updatePolygonAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updatePolygonAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (joglesctx.gl_state.cullFace != cullFace)
		{
			if (cullFace == PolygonAttributes.CULL_NONE)
			{
				gl.glDisable(GL2ES2.GL_CULL_FACE);
			}
			else
			{
				if (cullFace == PolygonAttributes.CULL_BACK)
				{
					gl.glCullFace(GL2ES2.GL_BACK);
				}
				else
				{
					gl.glCullFace(GL2ES2.GL_FRONT);
				}
				gl.glEnable(GL2ES2.GL_CULL_FACE);
			}
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);

			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.cullFace = cullFace;
		}

		if (joglesctx.gl_state.polygonOffsetFactor != polygonOffsetFactor || joglesctx.gl_state.polygonOffset != polygonOffset)
		{
			gl.glPolygonOffset(polygonOffsetFactor, polygonOffset);

			if ((polygonOffsetFactor != 0.0f) || (polygonOffset != 0.0f))
			{
				gl.glEnable(GL2ES2.GL_POLYGON_OFFSET_FILL);
			}
			else
			{
				gl.glDisable(GL2ES2.GL_POLYGON_OFFSET_FILL);
			}
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.polygonOffsetFactor = polygonOffsetFactor;
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.polygonOffset = polygonOffset;
		}
		joglesctx.polygonMode = polygonMode;
	}

	// native method for setting default PolygonAttributes
	@Override

	void resetPolygonAttributes(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetPolygonAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetPolygonAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (joglesctx.gl_state.cullFace != PolygonAttributes.CULL_BACK)
		{
			gl.glCullFace(GL2ES2.GL_BACK);
			gl.glEnable(GL2ES2.GL_CULL_FACE);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.cullFace = PolygonAttributes.CULL_BACK;
		}

		if (joglesctx.gl_state.polygonOffsetFactor != 0.0f || joglesctx.gl_state.polygonOffset != 0.0f)
		{
			gl.glPolygonOffset(0.0f, 0.0f);
			gl.glDisable(GL2ES2.GL_POLYGON_OFFSET_FILL);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.polygonOffsetFactor = 0.0f;
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.polygonOffset = 0.0f;
		}

		joglesctx.polygonMode = PolygonAttributes.POLYGON_FILL;
	}

	// ---------------------------------------------------------------------

	//
	// RenderingAttributesRetained methods
	//

	@Override
	void updateRenderingAttributes(Context ctx, boolean depthBufferWriteEnableOverride, boolean depthBufferEnableOverride,
			boolean depthBufferEnable, boolean depthBufferWriteEnable, int depthTestFunction, float alphaTestValue, int alphaTestFunction,
			boolean ignoreVertexColors, boolean rasterOpEnable, int rasterOp, boolean userStencilAvailable, boolean stencilEnable,
			int stencilFailOp, int stencilZFailOp, int stencilZPassOp, int stencilFunction, int stencilReferenceValue,
			int stencilCompareMask, int stencilWriteMask)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateRenderingAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateRenderingAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (joglesctx.gl_state.depthBufferEnableOverride != depthBufferEnable || joglesctx.gl_state.depthBufferEnable != depthBufferEnable
				|| joglesctx.gl_state.depthTestFunction != depthTestFunction)
		{
			if (!depthBufferEnableOverride)
			{
				if (depthBufferEnable)
				{
					gl.glEnable(GL2ES2.GL_DEPTH_TEST);
					gl.glDepthFunc(getFunctionValue(depthTestFunction));
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
				else
				{
					gl.glDisable(GL2ES2.GL_DEPTH_TEST);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
				}
			}

			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.depthBufferEnableOverride = depthBufferEnable;
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.depthBufferEnable = depthBufferEnable;
			if (MINIMISE_NATIVE_CALLS_OTHER)
				joglesctx.gl_state.depthTestFunction = depthTestFunction;
		}

		if (!depthBufferWriteEnableOverride)
		{
			if (depthBufferWriteEnable)
			{
				if (joglesctx.gl_state.glDepthMask != true)
				{
					gl.glDepthMask(true);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
					if (MINIMISE_NATIVE_CALLS_OTHER)
						joglesctx.gl_state.glDepthMask = true;
				}
			}
			else
			{
				if (joglesctx.gl_state.glDepthMask != false)
				{
					gl.glDepthMask(false);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
					if (MINIMISE_NATIVE_CALLS_OTHER)
						joglesctx.gl_state.glDepthMask = true;
				}
			}
		}

		if (alphaTestFunction == RenderingAttributes.ALWAYS)
		{
			joglesctx.renderingData.alphaTestEnabled = false;
		}
		else
		{
			//TODO: simple test use alpha blending instead of testing
			joglesctx.renderingData.alphaTestEnabled = true;
			joglesctx.renderingData.alphaTestFunction = getFunctionValue(alphaTestFunction);
			joglesctx.renderingData.alphaTestValue = alphaTestValue;
		}

		joglesctx.renderingData.ignoreVertexColors = ignoreVertexColors;

		if (rasterOpEnable)
		{
			System.err.println("rasterOpEnable!!!! no no no!");
		}

		if (userStencilAvailable)
		{
			if (stencilEnable)
			{
				//TODO: be more specific with native call pre-test here
				// currently causes major trouble
				//if (joglesctx.gl_state.glEnableGL_STENCIL_TEST == false)
				{
					gl.glEnable(GL2ES2.GL_STENCIL_TEST);
					gl.glStencilOp(getStencilOpValue(stencilFailOp), getStencilOpValue(stencilZFailOp), getStencilOpValue(stencilZPassOp));
					gl.glStencilFunc(getFunctionValue(stencilFunction), stencilReferenceValue, stencilCompareMask);
					gl.glStencilMask(stencilWriteMask);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
					//if (MINIMISE_NATIVE_CALLS_OTHER)
					//	joglesctx.gl_state.glEnableGL_STENCIL_TEST = true;
				}

			}
			else
			{
				//if (joglesctx.gl_state.glEnableGL_STENCIL_TEST == true)
				{
					gl.glDisable(GL2ES2.GL_STENCIL_TEST);
					if (DO_OUTPUT_ERRORS)
						outputErrors(ctx);
					//if (MINIMISE_NATIVE_CALLS_OTHER)
					//	joglesctx.gl_state.glEnableGL_STENCIL_TEST = false;
				}
			}
		}

	}

	// native method for setting default RenderingAttributes
	@Override

	void resetRenderingAttributes(Context ctx, boolean depthBufferWriteEnableOverride, boolean depthBufferEnableOverride)
	{

		if (VERBOSE)
			System.err.println("JoglPipeline.resetRenderingAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetRenderingAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (!depthBufferWriteEnableOverride)
		{
			//if (joglesctx.gl_state.glDepthMask != true)
			{
				gl.glDepthMask(true);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				//if (MINIMISE_NATIVE_CALLS_OTHER)
				//	joglesctx.gl_state.glDepthMask = true;
			}
		}
		if (!depthBufferEnableOverride)
		{
			//if (joglesctx.gl_state.depthBufferEnable != true)
			{
				gl.glEnable(GL2ES2.GL_DEPTH_TEST);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				//if (MINIMISE_NATIVE_CALLS_OTHER)
				//	joglesctx.gl_state.depthBufferEnable = true;
			}
		}
		//if (joglesctx.gl_state.depthTestFunction != RenderingAttributes.LESS_OR_EQUAL)
		{
			gl.glDepthFunc(GL2ES2.GL_LEQUAL);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			//if (MINIMISE_NATIVE_CALLS_OTHER)
			//	joglesctx.gl_state.depthTestFunction = RenderingAttributes.LESS_OR_EQUAL;
		}

		joglesctx.renderingData.alphaTestEnabled = false;
		joglesctx.renderingData.alphaTestFunction = RenderingAttributes.ALWAYS;
		joglesctx.renderingData.alphaTestValue = 0;
		joglesctx.renderingData.ignoreVertexColors = false;

		//RAISE_BUG: yep only called on a null RenderingAttributes
		//FIXME: this call does not set stencil test, so possibly this is why the rendering attributes 
		//caused such a mess when not present??

		//if (joglesctx.gl_state.glEnableGL_STENCIL_TEST == true)
		{
			gl.glDisable(GL2ES2.GL_STENCIL_TEST);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			//if (MINIMISE_NATIVE_CALLS_OTHER)
			//	joglesctx.gl_state.glEnableGL_STENCIL_TEST = false;
		}

	}

	@Override

	void updateTransparencyAttributes(Context ctx, float alpha, int geometryType, int polygonMode, boolean lineAA, boolean pointAA,
			int transparencyMode, int srcBlendFunction, int dstBlendFunction)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTransparencyAttributes() " + alpha + " " + geometryType + " " + polygonMode + " "
					+ lineAA + " " + pointAA + " " + transparencyMode + " " + srcBlendFunction + " " + dstBlendFunction);
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateTransparencyAttributes++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		JoglesContext joglesctx = ((JoglesContext) ctx);

		if ((transparencyMode < TransparencyAttributes.SCREEN_DOOR)
				|| ((((geometryType & RenderMolecule.LINE) != 0) || (polygonMode == PolygonAttributes.POLYGON_LINE)) && lineAA)
				|| ((((geometryType & RenderMolecule.POINT) != 0) || (polygonMode == PolygonAttributes.POLYGON_POINT)) && pointAA))
		{
			if (!MINIMISE_NATIVE_CALLS_TRANSPARENCY || (joglesctx.gl_state.glEnableGL_BLEND != true
					|| joglesctx.gl_state.srcBlendFunction != srcBlendFunction || joglesctx.gl_state.dstBlendFunction != dstBlendFunction))
			{
				gl.glEnable(GL2ES2.GL_BLEND);
				// valid range of blendFunction 0..3 is already verified in shared code.
				gl.glBlendFunc(blendFunctionTable[srcBlendFunction], blendFunctionTable[dstBlendFunction]);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.glEnableGL_BLEND = true;
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.srcBlendFunction = srcBlendFunction;
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.dstBlendFunction = dstBlendFunction;
			}

		}
		else
		{
			if (!MINIMISE_NATIVE_CALLS_TRANSPARENCY || (joglesctx.gl_state.glEnableGL_BLEND != false))
			{
				gl.glDisable(GL2ES2.GL_BLEND);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.glEnableGL_BLEND = false;
			}
		}

	}

	// native method for setting default TransparencyAttributes
	@Override

	void resetTransparency(Context ctx, int geometryType, int polygonMode, boolean lineAA, boolean pointAA)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetTransparency()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetTransparency++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		JoglesContext joglesctx = ((JoglesContext) ctx);
		if (((((geometryType & RenderMolecule.LINE) != 0) || (polygonMode == PolygonAttributes.POLYGON_LINE)) && lineAA)
				|| ((((geometryType & RenderMolecule.POINT) != 0) || (polygonMode == PolygonAttributes.POLYGON_POINT)) && pointAA))
		{
			if (!MINIMISE_NATIVE_CALLS_TRANSPARENCY || (joglesctx.gl_state.glEnableGL_BLEND != true
					|| joglesctx.gl_state.srcBlendFunction != TransparencyAttributes.BLEND_SRC_ALPHA
					|| joglesctx.gl_state.dstBlendFunction != TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA))
			{
				gl.glEnable(GL2ES2.GL_BLEND);
				gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.glEnableGL_BLEND = true;
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.srcBlendFunction = TransparencyAttributes.BLEND_SRC_ALPHA;
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.dstBlendFunction = TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA;
			}
		}
		else
		{
			if (!MINIMISE_NATIVE_CALLS_TRANSPARENCY || (joglesctx.gl_state.glEnableGL_BLEND != false))
			{
				gl.glDisable(GL2ES2.GL_BLEND);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
					joglesctx.gl_state.glEnableGL_BLEND = false;
			}

		}

	}

	//
	// TextureAttributesRetained methods
	//
	@Override
	void updateTextureAttributes(Context ctx, double[] transform, boolean isIdentity, int textureMode, int perspCorrectionMode,
			float textureBlendColorRed, float textureBlendColorGreen, float textureBlendColorBlue, float textureBlendColorAlpha,
			int textureFormat)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureAttributes() " + lineString(transform));
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateTextureAttributes++;

		JoglesContext joglesctx = (JoglesContext) ctx;
		joglesctx.textureTransform.set(transform);
	}

	// native method for setting default TextureAttributes
	@Override

	// part of TUS updateNative
	void resetTextureAttributes(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetTextureAttributes()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetTextureAttributes++;

		// set Identity
		JoglesContext joglesctx = (JoglesContext) ctx;
		joglesctx.textureTransform.setIdentity();

	}

	// ---------------------------------------------------------------------

	// native method for setting default TexCoordGeneration -Noop
	@Override
	void resetTexCoordGeneration(Context ctx)
	{
		//if (VERBOSE)
		//	System.err.println("JoglPipeline.resetTexCoordGeneration()");
		//if (OUTPUT_PER_FRAME_STATS)
		//	((JoglesContext) ctx).perFrameStats.resetTexCoordGeneration++;

	}

	// ---------------------------------------------------------------------

	//
	// TextureUnitStateRetained methods
	//

	@Override
	void updateTextureUnitState(Context ctx, int index, boolean enable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureUnitState(index=" + index + ",enable=" + enable + ")");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.updateTextureUnitState++;

		JoglesContext joglesContext = (JoglesContext) ctx;
		GL2ES2 gl = joglesContext.gl2es2;

		if (index >= 0)
		{
			if (!MINIMISE_NATIVE_CALLS_TEXTURE || (joglesContext.gl_state.glActiveTexture != (index + GL2ES2.GL_TEXTURE0)))
			{
				gl.glActiveTexture(index + GL2ES2.GL_TEXTURE0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_TEXTURE)
					joglesContext.gl_state.glActiveTexture = (index + GL2ES2.GL_TEXTURE0);
			}
		}

	}

	// ---------------------------------------------------------------------

	//
	// TextureRetained methods
	// Texture2DRetained methods
	//

	@Override
	void bindTexture2D(Context ctx, int objectId, boolean enable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.bindTexture2D(objectId=" + objectId + ",enable=" + enable + ")");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.bindTexture2D++;

		JoglesContext joglesContext = (JoglesContext) ctx;
		GL2ES2 gl = joglesContext.gl2es2;

		if (enable)
		{
			//Morrowind land shows problems, but need texture unit above turn off as well
			// possibly only the 2 glActiveTexture need turning off?
			if (!MINIMISE_NATIVE_CALLS_TEXTURE
					|| (joglesContext.gl_state.glBindTextureGL_TEXTURE_2D[joglesContext.gl_state.glActiveTexture] != objectId))
			{
				gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, objectId);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);

				if (MINIMISE_NATIVE_CALLS_TEXTURE)
					joglesContext.gl_state.glBindTextureGL_TEXTURE_2D[joglesContext.gl_state.glActiveTexture] = objectId;
			}

		}
	}

	@Override
	void updateTexture2DImage(Context ctx, int numLevels, int level, int textureFormat, int imageFormat, int width, int height,
			int boundaryWidth, int dataType, Object data, boolean useAutoMipMap)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTexture2DImage(width=" + width + ",height=" + height + ",level=" + level + ")");

		updateTexture2DImage(ctx, GL2ES2.GL_TEXTURE_2D, numLevels, level, textureFormat, imageFormat, width, height, boundaryWidth,
				dataType, data, useAutoMipMap);
	}

	//oddly in use when I press escape twice???
	@Override
	void updateTexture2DSubImage(Context ctx, int level, int xoffset, int yoffset, int textureFormat, int imageFormat, int imgXOffset,
			int imgYOffset, int tilew, int width, int height, int dataType, Object data, boolean useAutoMipMap)
	{

		/* Note: useAutoMipMap is not use for SubImage in the jogl pipe */

		if (VERBOSE)
			System.err.println("JoglPipeline.updateTexture2DSubImage()");

		updateTexture2DSubImage(ctx, GL2ES2.GL_TEXTURE_2D, level, xoffset, yoffset, textureFormat, imageFormat, imgXOffset, imgYOffset,
				tilew, width, height, dataType, data);
	}

	@Override
	void updateTexture2DLodRange(Context ctx, int baseLevel, int maximumLevel, float minimumLOD, float maximumLOD)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTexture2DLodRange()");

		updateTextureLodRange(ctx, GL2ES2.GL_TEXTURE_2D, baseLevel, maximumLevel, minimumLOD, maximumLOD);
	}

	@Override
	void updateTexture2DBoundary(Context ctx, int boundaryModeS, int boundaryModeT, float boundaryRed, float boundaryGreen,
			float boundaryBlue, float boundaryAlpha)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTexture2DBoundary()");

		updateTextureBoundary(ctx, GL2ES2.GL_TEXTURE_2D, boundaryModeS, boundaryModeT, -1, boundaryRed, boundaryGreen, boundaryBlue,
				boundaryAlpha);
	}

	@Override
	void updateTexture2DFilterModes(Context ctx, int minFilter, int magFilter)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTexture2DFilterModes()");

		updateTextureFilterModes(ctx, GL2ES2.GL_TEXTURE_2D, minFilter, magFilter);
	}

	@Override
	void updateTexture2DAnisotropicFilter(Context ctx, float degree)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTexture2DAnisotropicFilter()");

		updateTextureAnisotropicFilter(ctx, GL2ES2.GL_TEXTURE_2D, degree);
	}

	private static void updateTextureLodRange(Context ctx, int target, int baseLevel, int maximumLevel, float minimumLOD, float maximumLOD)
	{
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		//I notice these 4 parameters don't appear under GL2ES2

		// target is good, pname can't have any of the 4 given, though I wonder if this is an extension somehow?
		// confirmed here http://stackoverflow.com/questions/34499219/correct-way-to-unbind-an-open-gl-es-texture 

		//target        Specifies the target texture of the active texture unit,
		//which must be either GL_TEXTURE_2D or   GL_TEXTURE_CUBE_MAP.
		//pname        Specifies the symbolic name of a single-valued texture parameter.
		//pname can be one of the following:        GL_TEXTURE_MIN_FILTER,        GL_TEXTURE_MAG_FILTER,
		//GL_TEXTURE_WRAP_S, or        GL_TEXTURE_WRAP_T.

		// checking of the availability of the extension is already done
		// in the shared code
		//Apparently these are available in ES3

		//gl.glTexParameteri(target, GL2ES3.GL_TEXTURE_BASE_LEVEL, baseLevel);

		//http://stackoverflow.com/questions/12767917/is-using-gl-nearest-mipmap-or-gl-linear-mipmap-for-gl-texture-min-filter-con
		// so hopefully ES2 just assumes the mip maps given are correct and ignores this call

		gl.glTexParameteri(target, GL2ES3.GL_TEXTURE_MAX_LEVEL, maximumLevel);
		//gl.glTexParameterf(target, GL2ES3.GL_TEXTURE_MIN_LOD, minimumLOD);
		//gl.glTexParameterf(target, GL2ES3.GL_TEXTURE_MAX_LOD, maximumLOD);

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	private static void updateTextureAnisotropicFilter(Context ctx, int target, float degree)
	{
		//FIXME: is this a true thing to send in?
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		//it appears GL_TEXTURE_MAX_ANISOTROPY_EXT is still part of ES2 
		// but not allowed for glTexParameterf
		// here http://www.informit.com/articles/article.aspx?p=770639&seqNum=2 suggest the 
		// parameter may still be passed if the EXT is enabled

		// checking of the availability of anisotropic filter functionality
		// is already done in the shared code
		gl.glTexParameterf(target, GL2ES2.GL_TEXTURE_MAX_ANISOTROPY_EXT, degree);

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	// ---------------------------------------------------------------------

	//
	// TextureCubeMapRetained methods
	//

	@Override
	void bindTextureCubeMap(Context ctx, int objectId, boolean enable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.bindTextureCubeMap()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.bindTextureCubeMap++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		// TextureCubeMap will take precedents over 3D Texture so
		// there is no need to disable 3D Texture here.
		if (!enable)
		{
			//gl.glDisable(GL2ES2.GL_TEXTURE_CUBE_MAP);
		}
		else
		{
			gl.glBindTexture(GL2ES2.GL_TEXTURE_CUBE_MAP, objectId);
			//gl.glEnable(GL2ES2.GL_TEXTURE_CUBE_MAP);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}
	}

	@Override
	void updateTextureCubeMapImage(Context ctx, int face, int numLevels, int level, int textureFormat, int imageFormat, int width,
			int height, int boundaryWidth, int dataType, Object data, boolean useAutoMipMap)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureCubeMapImage()");

		updateTexture2DImage(ctx, _gl_textureCubeMapFace[face], numLevels, level, textureFormat, imageFormat, width, height, boundaryWidth,
				dataType, data, useAutoMipMap);
	}

	@Override
	void updateTextureCubeMapSubImage(Context ctx, int face, int level, int xoffset, int yoffset, int textureFormat, int imageFormat,
			int imgXOffset, int imgYOffset, int tilew, int width, int height, int dataType, Object data, boolean useAutoMipMap)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	void updateTextureCubeMapLodRange(Context ctx, int baseLevel, int maximumLevel, float minimumLod, float maximumLod)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureCubeMapLodRange()");

		updateTextureLodRange(ctx, GL2ES2.GL_TEXTURE_CUBE_MAP, baseLevel, maximumLevel, minimumLod, maximumLod);
	}

	@Override
	void updateTextureCubeMapBoundary(Context ctx, int boundaryModeS, int boundaryModeT, float boundaryRed, float boundaryGreen,
			float boundaryBlue, float boundaryAlpha)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureCubeMapBoundary()");

		updateTextureBoundary(ctx, GL2ES2.GL_TEXTURE_CUBE_MAP, boundaryModeS, boundaryModeT, -1, boundaryRed, boundaryGreen, boundaryBlue,
				boundaryAlpha);
	}

	@Override
	void updateTextureCubeMapFilterModes(Context ctx, int minFilter, int magFilter)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureCubeMapFilterModes()");

		updateTextureFilterModes(ctx, GL2ES2.GL_TEXTURE_CUBE_MAP, minFilter, magFilter);
	}

	@Override
	void updateTextureCubeMapAnisotropicFilter(Context ctx, float degree)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateTextureCubeMapAnisotropicFilter()");

		updateTextureAnisotropicFilter(ctx, GL2ES2.GL_TEXTURE_CUBE_MAP, degree);
	}

	//----------------------------------------------------------------------
	//
	// Helper routines for above texture methods
	//

	private void updateTexture2DImage(Context ctx, int target, int numLevels, int level, int textureFormat, int imageFormat, int width,
			int height, int boundaryWidth, int dataType, Object data, boolean useAutoMipMap)
	{
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		// FIXME: there is a new call glGenerateMipmap() which is only in ES2 not GL2 so on pure ES2 
		// add back in checking for mipmap support under properties, then add that call after bind texture
		// which mean the gl.glTexParameteri(target, GL2ES2.GL_GENERATE_MIPMAP, GL2ES2.GL_TRUE); are gone
		//the extensions need to have an ES2 version
		// the glPixelTransferf alpha madness which I don't understand can be deleted along with the texture formats
		// the other look like they are ok, though the supported compressed will be different

		/*	from ES2 spec
		  format
		    Specifies the format of the pixel data.
		    The following symbolic values are accepted:
		    GL_ALPHA,
		    GL_RGB,
		    GL_RGBA,
		    GL_LUMINANCE, and
		    GL_LUMINANCE_ALPHA.
		type
		    Specifies the data type of the pixel data.
		    The following symbolic values are accepted:
		    GL_UNSIGNED_BYTE,
		    GL_UNSIGNED_SHORT_5_6_5,
		    GL_UNSIGNED_SHORT_4_4_4_4, and
		    GL_UNSIGNED_SHORT_5_5_5_1.*/

		int internalFormat = 0;

		switch (textureFormat)
		{
		case Texture.INTENSITY:
			new Throwable("Texture.INTENSITY not supported").printStackTrace();
			//internalFormat = GL2.GL_INTENSITY;
			break;
		case Texture.LUMINANCE:
			internalFormat = GL2ES2.GL_LUMINANCE;
			break;
		case Texture.ALPHA:
			internalFormat = GL2ES2.GL_ALPHA;
			break;
		case Texture.LUMINANCE_ALPHA:
			internalFormat = GL2ES2.GL_LUMINANCE_ALPHA;
			break;
		case Texture.RGB:
			internalFormat = GL2ES2.GL_RGB;
			break;
		case Texture.RGBA:
			internalFormat = GL2ES2.GL_RGBA;
			break;
		default:
			assert false;
		}

		//FIXME: morrowind sky goes black if this is the case (no mipmap)
		// so to disable automipmap, must set it to false in texture I guess?
		// see above glGenMipMap once on pure ES2 (if on pure ES2?)
		if (useAutoMipMap)
		{
			throw new UnsupportedOperationException("Disable auto mip map generation!");
			//gl.glTexParameteri(target, GL2ES2.GL_GENERATE_MIPMAP, GL2ES2.GL_TRUE);
		}
		else
		{
			// should default to false
			//gl.glTexParameteri(target, GL2ES2.GL_GENERATE_MIPMAP, GL2ES2.GL_FALSE);
		}

		int format = 0;

		if ((dataType == ImageComponentRetained.IMAGE_DATA_TYPE_BYTE_ARRAY)
				|| (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_BYTE_BUFFER))
		{

			switch (imageFormat)
			{
			case ImageComponentRetained.TYPE_BYTE_BGR:
				format = GL2ES2.GL_BGR;
				break;
			case ImageComponentRetained.TYPE_BYTE_RGB:
				format = GL2ES2.GL_RGB;
				break;
			case ImageComponentRetained.TYPE_BYTE_ABGR:
				if (isExtensionAvailable.GL_EXT_abgr(gl))
				{ // If its zero, should never come here!
					format = GL2.GL_ABGR_EXT;
				}
				else
				{
					assert false;
					return;
				}
				break;
			case ImageComponentRetained.TYPE_BYTE_RGBA:
				// all RGB types are stored as RGBA
				format = GL2ES2.GL_RGBA;
				break;
			case ImageComponentRetained.TYPE_BYTE_LA:
				// all LA types are stored as LA8
				format = GL2ES2.GL_LUMINANCE_ALPHA;
				break;
			case ImageComponentRetained.TYPE_BYTE_GRAY:
				if (internalFormat == GL2ES2.GL_ALPHA)
				{
					format = GL2ES2.GL_ALPHA;
				}
				else
				{
					format = GL2ES2.GL_LUMINANCE;
				}
				break;
			///////////////////////////////////////////////////PJPJPJ////////////////////
			//DXT   uncompressed D3DFMT_A8R8G8B8 indicator
			case GL2.GL_RGBA_S3TC:
				internalFormat = GL2ES2.GL_RGBA;
				format = GL2ES2.GL_RGBA;
				break;
			// notice fall through
			//DXT    
			case GL2ES2.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
			case GL2ES2.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
			case GL2ES2.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
			case GL2.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT:
				//ETC2 https://www.khronos.org/opengles/sdk/docs/man3/html/glCompressedTexImage2D.xhtml
			case GL3.GL_COMPRESSED_RGBA8_ETC2_EAC:
			case GL3.GL_COMPRESSED_RGB8_ETC2:
			case GL3.GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2:
			case GL3.GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC:
			case GL3.GL_COMPRESSED_SRGB8_ETC2:
			case GL3.GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2:
				//ASTC
			case GL3.GL_COMPRESSED_RGBA_ASTC_4x4_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_5x4_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_5x5_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_6x5_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_6x6_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_8x5_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_8x6_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_8x8_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_10x5_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_10x6_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_10x8_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_10x10_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_12x10_KHR:
			case GL3.GL_COMPRESSED_RGBA_ASTC_12x12_KHR:
				internalFormat = imageFormat;
				format = -1;// indicate compressed
				break;
			/////////////////////////////////////PJPJPJ//////////////////////////////
			case ImageComponentRetained.TYPE_USHORT_GRAY:
			case ImageComponentRetained.TYPE_INT_BGR:
			case ImageComponentRetained.TYPE_INT_RGB:
			case ImageComponentRetained.TYPE_INT_ARGB:
			default:
				assert false;
				return;
			}

			if (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_BYTE_ARRAY)
			{
				gl.glTexImage2D(target, level, internalFormat, width, height, boundaryWidth, format, GL2ES2.GL_UNSIGNED_BYTE,
						ByteBuffer.wrap((byte[]) data));
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
			else
			{

				if (format == -1)
				{
					ByteBuffer bb = (ByteBuffer) data;

					gl.glCompressedTexImage2D(target, level, internalFormat, width, height, boundaryWidth, bb.limit(), bb);

					if (DO_OUTPUT_ERRORS)
					{
						int err = gl.glGetError();
						if (err != GL2ES2.GL_NO_ERROR)
						{
							System.out.println("glCompressedTexImage2D Error " + err + " target " + target + " level " + level
									+ " internalFormat " + internalFormat);
							System.out.println("width " + width + " height " + height + " boundaryWidth " + boundaryWidth + " bb.limit() "
									+ bb.limit());
							//https://www.khronos.org/opengles/sdk/docs/man3/html/glCompressedTexImage2D.xhtml
						}
					}
				}
				else
				{
					gl.glTexImage2D(target, level, internalFormat, width, height, boundaryWidth, format, GL2ES2.GL_UNSIGNED_BYTE,
							(Buffer) data);
					if (DO_OUTPUT_ERRORS)
					{
						int err = gl.glGetError();
						if (err != GL2ES2.GL_NO_ERROR)
						{
							System.out.println("glTexImage2D Error " + err + " target " + target + " level " + level + " internalFormat "
									+ internalFormat);
							System.out.println("width " + width + " height " + height + " boundaryWidth " + boundaryWidth + " format "
									+ format + " bb.limit() " + ((Buffer) data).limit());
							//https://www.khronos.org/opengles/sdk/docs/man3/html/glCompressedTexImage2D.xhtml
						}
					}
				}

			}
		}
		else if ((dataType == ImageComponentRetained.IMAGE_DATA_TYPE_INT_ARRAY)
				|| (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_INT_BUFFER))

		{

			//FIXME: I suspect I will only support byte buffer images so perhaps the INT type can be deprecated?
			System.out.println("IMAGE_DATA_TYPE_INT_ in use!");
			int type = GL2.GL_UNSIGNED_INT_8_8_8_8;
			boolean forceAlphaToOne = false;
			switch (imageFormat)
			{
			/* GL_BGR */
			case ImageComponentRetained.TYPE_INT_BGR: /* Assume XBGR format */
				format = GL2ES2.GL_RGBA;
				type = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
				forceAlphaToOne = true;
				break;
			case ImageComponentRetained.TYPE_INT_RGB: /* Assume XRGB format */
				forceAlphaToOne = true;
				/* Fall through to next case */
			case ImageComponentRetained.TYPE_INT_ARGB:
				format = GL2ES2.GL_BGRA;
				type = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
				break;
			/* This method only supports 3 and 4 components formats and INT types. */
			case ImageComponentRetained.TYPE_BYTE_LA:
			case ImageComponentRetained.TYPE_BYTE_GRAY:
			case ImageComponentRetained.TYPE_USHORT_GRAY:
			case ImageComponentRetained.TYPE_BYTE_BGR:
			case ImageComponentRetained.TYPE_BYTE_RGB:
			case ImageComponentRetained.TYPE_BYTE_RGBA:
			case ImageComponentRetained.TYPE_BYTE_ABGR:
			default:
				assert false;
				return;
			}

			if (forceAlphaToOne)
			{
				new Throwable("forceAlphaToOne").printStackTrace();
			}

			if (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_INT_ARRAY)
			{
				gl.glTexImage2D(target, level, internalFormat, width, height, boundaryWidth, format, type, IntBuffer.wrap((int[]) data));
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
			else
			{
				gl.glTexImage2D(target, level, internalFormat, width, height, boundaryWidth, format, type, (Buffer) data);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
			}
		}
		else
		{
			assert false;
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	// only used when I press escape twice
	private void updateTexture2DSubImage(Context ctx, int target, int level, int xoffset, int yoffset, int textureFormat, int imageFormat,
			int imgXOffset, int imgYOffset, int tilew, int width, int height, int dataType, Object data)
	{
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		if (imgXOffset > 0 || (width < tilew))
		{
			new Throwable("forceAlphaToOne").printStackTrace();
		}

		int internalFormat = 0;

		switch (textureFormat)
		{
		case Texture.INTENSITY:
			//internalFormat = GL2.GL_INTENSITY;
			new Throwable("Texture.INTENSITY not supported").printStackTrace();
			break;
		case Texture.LUMINANCE:
			internalFormat = GL2ES2.GL_LUMINANCE;
			break;
		case Texture.ALPHA:
			internalFormat = GL2ES2.GL_ALPHA;
			break;
		case Texture.LUMINANCE_ALPHA:
			internalFormat = GL2ES2.GL_LUMINANCE_ALPHA;
			break;
		case Texture.RGB:
			internalFormat = GL2ES2.GL_RGB;
			break;
		case Texture.RGBA:
			internalFormat = GL2ES2.GL_RGBA;
			break;
		default:
			assert false;
		}

		if ((dataType == ImageComponentRetained.IMAGE_DATA_TYPE_BYTE_ARRAY)
				|| (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_BYTE_BUFFER))
		{
			int format = 0;
			int numBytes = 0;

			switch (imageFormat)
			{
			case ImageComponentRetained.TYPE_BYTE_BGR:
				format = GL2ES2.GL_BGR;
				numBytes = 3;
				break;
			case ImageComponentRetained.TYPE_BYTE_RGB:
				format = GL2ES2.GL_RGB;
				numBytes = 3;
				break;
			case ImageComponentRetained.TYPE_BYTE_ABGR:
				if (isExtensionAvailable.GL_EXT_abgr(gl))
				{ // If its zero, should never come here!
					format = GL2.GL_ABGR_EXT;
					numBytes = 4;
				}
				else
				{
					assert false;
					return;
				}
				break;
			case ImageComponentRetained.TYPE_BYTE_RGBA:
				// all RGB types are stored as RGBA
				format = GL2ES2.GL_RGBA;
				numBytes = 4;
				break;
			case ImageComponentRetained.TYPE_BYTE_LA:
				// all LA types are stored as LA8
				format = GL2ES2.GL_LUMINANCE_ALPHA;
				numBytes = 2;
				break;
			case ImageComponentRetained.TYPE_BYTE_GRAY:
				if (internalFormat == GL2ES2.GL_ALPHA)
				{
					format = GL2ES2.GL_ALPHA;
					numBytes = 1;
				}
				else
				{
					format = GL2ES2.GL_LUMINANCE;
					numBytes = 1;
				}
				break;
			case ImageComponentRetained.TYPE_USHORT_GRAY:
			case ImageComponentRetained.TYPE_INT_BGR:
			case ImageComponentRetained.TYPE_INT_RGB:
			case ImageComponentRetained.TYPE_INT_ARGB:
			default:
				assert false;
				return;
			}

			ByteBuffer buf = null;
			if (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_BYTE_ARRAY)
			{
				buf = ByteBuffer.wrap((byte[]) data);
			}
			else
			{
				buf = (ByteBuffer) data;
			}

			// offset by the imageOffset
			buf.position((tilew * imgYOffset + imgXOffset) * numBytes);
			gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, GL2ES2.GL_UNSIGNED_BYTE, buf);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}
		else if ((dataType == ImageComponentRetained.IMAGE_DATA_TYPE_INT_ARRAY)
				|| (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_INT_BUFFER))
		{
			int format = 0;
			int type = GL2.GL_UNSIGNED_INT_8_8_8_8;
			boolean forceAlphaToOne = false;
			switch (imageFormat)
			{
			/* GL_BGR */
			case ImageComponentRetained.TYPE_INT_BGR: /* Assume XBGR format */
				format = GL2ES2.GL_RGBA;
				type = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
				forceAlphaToOne = true;
				break;
			case ImageComponentRetained.TYPE_INT_RGB: /* Assume XRGB format */
				forceAlphaToOne = true;
				/* Fall through to next case */
			case ImageComponentRetained.TYPE_INT_ARGB:
				format = GL2ES2.GL_BGRA;
				type = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
				break;
			/* This method only supports 3 and 4 components formats and INT types. */
			case ImageComponentRetained.TYPE_BYTE_LA:
			case ImageComponentRetained.TYPE_BYTE_GRAY:
			case ImageComponentRetained.TYPE_USHORT_GRAY:
			case ImageComponentRetained.TYPE_BYTE_BGR:
			case ImageComponentRetained.TYPE_BYTE_RGB:
			case ImageComponentRetained.TYPE_BYTE_RGBA:
			case ImageComponentRetained.TYPE_BYTE_ABGR:
			default:
				assert false;
				return;
			}

			if (forceAlphaToOne)
			{
				new Throwable("forceAlphaToOne").printStackTrace();
			}

			IntBuffer buf = null;
			if (dataType == ImageComponentRetained.IMAGE_DATA_TYPE_INT_ARRAY)
			{
				buf = IntBuffer.wrap((int[]) data);
			}
			else
			{
				buf = (IntBuffer) data;
			}

			// offset by the imageOffset
			buf.position(tilew * imgYOffset + imgXOffset);
			gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, buf);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}
		else
		{
			assert false;
			return;
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);

	}

	private static void updateTextureFilterModes(Context ctx, int target, int minFilter, int magFilter)
	{
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		// amazingly this should be fine unchanged 

		// FIXME: unclear whether we really need to set up the enum values
		// in the JoglContext as is done in the native code depending on
		// extension availability; maybe this is the defined fallback
		// behavior of the various Java3D modes

		// set texture min filter
		switch (minFilter)
		{
		case Texture.FASTEST:
		case Texture.BASE_LEVEL_POINT:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MIN_FILTER, GL2ES2.GL_NEAREST);
			break;
		case Texture.BASE_LEVEL_LINEAR:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MIN_FILTER, GL2ES2.GL_LINEAR);
			break;
		case Texture.MULTI_LEVEL_POINT:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MIN_FILTER, GL2ES2.GL_NEAREST_MIPMAP_NEAREST);
			break;
		case Texture.NICEST:
		case Texture.MULTI_LEVEL_LINEAR:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MIN_FILTER, GL2ES2.GL_LINEAR_MIPMAP_LINEAR);
			break;
		case Texture.FILTER4:
			// We should never get here as we've disabled the FILTER4 feature
			//                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MIN_FILTER,
			//                        GL2ES2.GL_FILTER4_SGIS);
			break;
		}
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		// set texture mag filter
		switch (magFilter)
		{
		case Texture.FASTEST:
		case Texture.BASE_LEVEL_POINT:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER, GL2ES2.GL_NEAREST);
			break;
		case Texture.NICEST:
		case Texture.BASE_LEVEL_LINEAR:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER, GL2ES2.GL_LINEAR);
			break;
		/*		case Texture.LINEAR_SHARPEN:
					// We should never get here as we've disabled the TEXTURE_SHARPEN feature
					//                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_LINEAR_SHARPEN_SGIS);
					break;
				case Texture.LINEAR_SHARPEN_RGB:
					// We should never get here as we've disabled the TEXTURE_SHARPEN feature
					//                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_LINEAR_SHARPEN_COLOR_SGIS);
					break;
				case Texture.LINEAR_SHARPEN_ALPHA:
					// We should never get here as we've disabled the TEXTURE_SHARPEN feature
					//                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_LINEAR_SHARPEN_ALPHA_SGIS);
					break;
				case Texture2D.LINEAR_DETAIL:
					// We should never get here as we've disabled the TEXTURE_DETAIL feature
					//            	gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_LINEAR_DETAIL_SGIS);
					break;
				case Texture2D.LINEAR_DETAIL_RGB:
					// We should never get here as we've disabled the TEXTURE_DETAIL feature
					//            	gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_LINEAR_DETAIL_COLOR_SGIS);
					break;
				case Texture2D.LINEAR_DETAIL_ALPHA:
					// We should never get here as we've disabled the TEXTURE_DETAIL feature
					//            	gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_LINEAR_DETAIL_ALPHA_SGIS);
					break;
				case Texture.FILTER4:
					// We should never get here as we've disabled the FILTER4 feature
					//                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER,
					//                        GL2ES2.GL_FILTER4_SGIS);
					break;*/
		}

		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	void updateTextureBoundary(Context ctx, int target, int boundaryModeS, int boundaryModeT, int boundaryModeR, float boundaryRed,
			float boundaryGreen, float boundaryBlue, float boundaryAlpha)
	{
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		// except the R gear at bottom and boundary color
		// but I'm dropping 3dtexture support so no probs and who cares about boundary color
		// CLAMP_TO_BOUNDARYand GL_CLAMP are gone so now just set as GL_CLAMP_TO_EDGE
		// FIXME: GL_MIRRORED_REPEAT needs to be added

		// set texture wrap parameter
		switch (boundaryModeS)
		{
		case Texture.WRAP:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_REPEAT);
			break;
		case Texture.CLAMP:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_EDGE);
			break;
		case Texture.CLAMP_TO_EDGE:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_EDGE);
			break;
		case Texture.CLAMP_TO_BOUNDARY:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_EDGE);
			break;
		}
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		switch (boundaryModeT)
		{
		case Texture.WRAP:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_REPEAT);
			break;
		case Texture.CLAMP:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_EDGE);
			break;
		case Texture.CLAMP_TO_EDGE:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_EDGE);
			break;
		case Texture.CLAMP_TO_BOUNDARY:
			gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_EDGE);
			break;
		}
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
		// applies to Texture3D only
		/*		if (boundaryModeR != -1)
				{
					switch (boundaryModeR)
					{
					case Texture.WRAP:
						gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R, GL2ES2.GL_REPEAT);
						break;
		
					case Texture.CLAMP:
						gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R, GL2ES2.GL_CLAMP);
						break;
					case Texture.CLAMP_TO_EDGE:
						gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R, GL2ES2.GL_CLAMP_TO_EDGE);
						break;
					case Texture.CLAMP_TO_BOUNDARY:
						gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R, GL2ES2.GL_CLAMP_TO_BORDER);
						break;
					}
				}*/

		//http://stackoverflow.com/questions/913801/glteximage2d
		// texture border no longer supported on ES2

		/*if (boundaryModeS == Texture.CLAMP || boundaryModeT == Texture.CLAMP || boundaryModeR == Texture.CLAMP)
		{
			// set texture border color
			float[] color = new float[4];
			color[0] = boundaryRed;
			color[1] = boundaryGreen;
			color[2] = boundaryBlue;
			color[3] = boundaryAlpha;
			gl.glTexParameterfv(target, GL2ES2.GL_TEXTURE_BORDER_COLOR, color, 0);
		}*/

	}

	/*	private static final String getFilterName(int filter)
		{
			switch (filter)
			{
			case Texture.FASTEST:
				return "Texture.FASTEST";
			case Texture.NICEST:
				return "Texture.NICEST";
			case Texture.BASE_LEVEL_POINT:
				return "Texture.BASE_LEVEL_POINT";
			case Texture.BASE_LEVEL_LINEAR:
				return "Texture.BASE_LEVEL_LINEAR";
			case Texture.MULTI_LEVEL_POINT:
				return "Texture.MULTI_LEVEL_POINT";
			case Texture.MULTI_LEVEL_LINEAR:
				return "Texture.MULTI_LEVEL_LINEAR";
			case Texture.FILTER4:
				return "Texture.FILTER4";
			case Texture.LINEAR_SHARPEN:
				return "Texture.LINEAR_SHARPEN";
			case Texture.LINEAR_SHARPEN_RGB:
				return "Texture.LINEAR_SHARPEN_RGB";
			case Texture.LINEAR_SHARPEN_ALPHA:
				return "Texture.LINEAR_SHARPEN_ALPHA";
			case Texture2D.LINEAR_DETAIL:
				return "Texture.LINEAR_DETAIL";
			case Texture2D.LINEAR_DETAIL_RGB:
				return "Texture.LINEAR_DETAIL_RGB";
			case Texture2D.LINEAR_DETAIL_ALPHA:
				return "Texture.LINEAR_DETAIL_ALPHA";
			default:
				return "(unknown)";
			}
		}*/

	// mapping from java enum to gl enum
	private static final int[] _gl_textureCubeMapFace = { GL2ES2.GL_TEXTURE_CUBE_MAP_POSITIVE_X, GL2ES2.GL_TEXTURE_CUBE_MAP_NEGATIVE_X,
			GL2ES2.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, GL2ES2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, GL2ES2.GL_TEXTURE_CUBE_MAP_POSITIVE_Z,
			GL2ES2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, };

	// The following three methods are used in multi-pass case

	// native method for setting blend color
	@Override
	// part of TUS updateNative, though not in fact used
	void setBlendColor(Context ctx, float red, float green, float blue, float alpha)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setBlendColor()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setBlendColor++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		if (isExtensionAvailable.GL_ARB_imaging(gl))
		{
			gl.glBlendColor(red, green, blue, alpha);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}

	}

	// native method for setting blend func
	@Override
	// part of TUS updateNative
	void setBlendFunc(Context ctx, int srcBlendFunction, int dstBlendFunction)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setBlendFunc()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setBlendFunc++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		JoglesContext joglesctx = ((JoglesContext) ctx);

		if (!MINIMISE_NATIVE_CALLS_TRANSPARENCY || (joglesctx.gl_state.glEnableGL_BLEND != true
				|| joglesctx.gl_state.srcBlendFunction != srcBlendFunction || joglesctx.gl_state.dstBlendFunction != dstBlendFunction))
		{
			gl.glEnable(GL2ES2.GL_BLEND);
			gl.glBlendFunc(blendFunctionTable[srcBlendFunction], blendFunctionTable[dstBlendFunction]);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
			if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
				joglesctx.gl_state.glEnableGL_BLEND = true;
			if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
				joglesctx.gl_state.srcBlendFunction = srcBlendFunction;
			if (MINIMISE_NATIVE_CALLS_TRANSPARENCY)
				joglesctx.gl_state.dstBlendFunction = dstBlendFunction;
		}
	}

	// native method for setting light enables
	@Override
	void setLightEnables(Context ctx, long enableMask, int maxLights)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setLightEnables()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setLightEnables++;

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			// bound to be not supported as definately shader work now		
		
			for (int i = 0; i < maxLights; i++)
			{
					if ((enableMask & (1 << i)) != 0)
					{
						gl.glEnable(GL2ES2.GL_LIGHT0 + i);
					}
					else
					{
						gl.glDisable(GL2ES2.GL_LIGHT0 + i);
					}
			}*/

		JoglesContext joglesctx = (JoglesContext) ctx;
		for (int i = 0; i < maxLights; i++)
		{
			joglesctx.enabledLights[i] = ((enableMask & (1 << i)) != 0);
		}
	}

	// native method for setting scene ambient
	@Override
	void setSceneAmbient(Context ctx, float red, float green, float blue)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setSceneAmbient()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setSceneAmbient++;
		JoglesContext joglesctx = (JoglesContext) ctx;
		joglesctx.currentAmbientColor.x = red;
		joglesctx.currentAmbientColor.y = green;
		joglesctx.currentAmbientColor.z = blue;
		joglesctx.currentAmbientColor.w = 1.0f;

	}

	// native method for disabling modelClip
	@Override
	//this is called as a reset
	void disableModelClip(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.disableModelClip()");

		/*GL2 gl = context(ctx).getGL().getGL2();
		//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
		// not supported
		
		gl.glDisable(GL2ES2.GL_CLIP_PLANE0);
		gl.glDisable(GL2ES2.GL_CLIP_PLANE1);
		gl.glDisable(GL2ES2.GL_CLIP_PLANE2);
		gl.glDisable(GL2ES2.GL_CLIP_PLANE3);
		gl.glDisable(GL2ES2.GL_CLIP_PLANE4);
		gl.glDisable(GL2ES2.GL_CLIP_PLANE5);*/
	}

	// native method for activating a particular texture unit
	@Override
	void activeTextureUnit(Context ctx, int texUnitIndex)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.activeTextureUnit(texUnitIndex= " + texUnitIndex + ")");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.activeTextureUnit++;

		JoglesContext joglesContext = (JoglesContext) ctx;
		GL2ES2 gl = joglesContext.gl2es2;

		if (texUnitIndex >= 0)
		{
			if (!MINIMISE_NATIVE_CALLS_TEXTURE || (joglesContext.gl_state.glActiveTexture != (texUnitIndex + GL2ES2.GL_TEXTURE0)))
			{
				gl.glActiveTexture(texUnitIndex + GL2ES2.GL_TEXTURE0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_TEXTURE)
					joglesContext.gl_state.glActiveTexture = (texUnitIndex + GL2ES2.GL_TEXTURE0);
			}
		}
	}

	// native method for setting default texture
	@Override

	void resetTextureNative(Context ctx, int texUnitIndex)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.resetTextureNative(texUnitIndex=" + texUnitIndex + ")");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.resetTextureNative++;

		JoglesContext joglesContext = (JoglesContext) ctx;
		GL2ES2 gl = joglesContext.gl2es2;

		if (texUnitIndex >= 0)
		{
			if (!MINIMISE_NATIVE_CALLS_TEXTURE || (joglesContext.gl_state.glActiveTexture != (texUnitIndex + GL2ES2.GL_TEXTURE0)))
			{
				gl.glActiveTexture(texUnitIndex + GL2ES2.GL_TEXTURE0);
				//TODO: should I enable these?  
				//gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, 0);//-1 is no texture , 0 is default
				//gl.glBindTexture(GL2ES2.GL_TEXTURE_CUBE_MAP, 0);
				if (DO_OUTPUT_ERRORS)
					outputErrors(ctx);
				if (MINIMISE_NATIVE_CALLS_TEXTURE)
					joglesContext.gl_state.glActiveTexture = (texUnitIndex + GL2ES2.GL_TEXTURE0);
			}
		}

		/*  
		gl.glDisable(GL2.GL_TEXTURE_1D);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
		*/
	}

	// The native method for setting the ModelView matrix.
	@Override
	void setModelViewMatrix(Context ctx, double[] viewMatrix, double[] modelMatrix)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setModelViewMatrix(viewMatrix= " + lineString(viewMatrix) + " modelMatrix= "
					+ lineString(modelMatrix) + ")");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setModelViewMatrix++;

		JoglesContext joglesctx = (JoglesContext) ctx;

		//joglesctx.matrixUtil.deburnV.set(viewMatrix);
		//joglesctx.matrixUtil.deburnV.transpose();// now done in ffp by call to native
		joglesctx.currentViewMat.set(viewMatrix);

		//joglesctx.matrixUtil.deburnM.set(modelMatrix);
		//joglesctx.matrixUtil.deburnM.transpose();// now done in ffp by call to native
		joglesctx.currentModelMat.set(modelMatrix);

		//Moved up into setffp and only calc'ed if requested
		/*
		
				joglesctx.currentModelViewMat.mul(joglesctx.matrixUtil.deburnV, joglesctx.matrixUtil.deburnM);
		
				
				joglesctx.currentModelViewProjMat.mul(joglesctx.currentProjMat, joglesctx.currentModelViewMat);
		
				// use only the upper left as it is a 3x3 rotation matrix
				JoglesMatrixUtil.transposeInvert(joglesctx.currentModelViewMat, joglesctx.currentNormalMat);				
			*/

	}

	// The native method for setting the Projection matrix.
	@Override
	void setProjectionMatrix(Context ctx, double[] projMatrix)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setProjectionMatrix()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setProjectionMatrix++;

		JoglesContext joglesctx = (JoglesContext) ctx;

		// Invert the Z value in clipping coordinates because OpenGL uses
		// left-handed clipping coordinates, while Java3D defines right-handed
		// coordinates everywhere.
		projMatrix[8] *= -1.0;
		projMatrix[9] *= -1.0;
		projMatrix[10] *= -1.0;
		projMatrix[11] *= -1.0;

		joglesctx.currentProjMat.set(projMatrix);
		//joglesctx.currentProjMat.transpose(); // done in set ffp now

		//reverse it back in case others use it
		projMatrix[8] *= -1.0;
		projMatrix[9] *= -1.0;
		projMatrix[10] *= -1.0;
		projMatrix[11] *= -1.0;

	}

	// The native method for setting the Viewport.
	@Override
	void setViewport(Context ctx, int x, int y, int width, int height)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setViewport()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setViewport++;
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setViewportTime = System.nanoTime();

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		gl.glViewport(x, y, width, height);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	private static String lineString(double[] da)
	{
		String ret = "double[";
		for (double d : da)
			ret += " " + d;
		return ret + "]";
	}

	private static String lineString(float[] fa)
	{
		String ret = "float[";
		for (float f : fa)
			ret += " " + f;
		return ret + "]";
	}

	@Override
	void freeTexture(Context ctx, int id)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.freeTexture()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.freeTexture++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		if (id > 0)
		{
			int[] tmp = new int[1];
			tmp[0] = id;
			gl.glDeleteTextures(1, tmp, 0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}
		else
		{
			System.err.println("tried to delete tex with texid <= 0");
		}
	}

	@Override
	int generateTexID(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.generateTexID()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.generateTexID++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		int[] tmp = new int[] { -1 };
		gl.glGenTextures(1, tmp, 0);

		if (tmp[0] < 1)
			return -1;

		return tmp[0];
	}

	// Set glDepthMask.
	@Override
	void setDepthBufferWriteEnable(Context ctx, boolean mode)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setDepthBufferWriteEnable()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setDepthBufferWriteEnable++;

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		if (mode)
		{
			gl.glDepthMask(true);
		}
		else
		{
			gl.glDepthMask(false);
		}
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	// Set internal render mode to one of FIELD_ALL, FIELD_LEFT or
	// FIELD_RIGHT.  Note that it is up to the caller to ensure that
	// stereo is available before setting the mode to FIELD_LEFT or
	// FIELD_RIGHT.  The boolean doubleBuffer is TRUE for double buffered mode, FALSE
	// for single buffering.
	@Override
	void setRenderMode(Context ctx, int mode, boolean doubleBuffer)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setRenderMode()");

		//UGLY HACK the render mode is set to Canvas3D.FIELD_ALL after all geoms are drawn
		// so I take the opportunity to unbind the vertex array

		GL2ES3 gl2es3 = ((JoglesContext) ctx).gl2es3;
		if (gl2es3 != null)
		{
			gl2es3.glBindVertexArray(0);
			if (DO_OUTPUT_ERRORS)
				outputErrors(ctx);
		}

		//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
		//no no drawBuffer, possibly just skip it for now
		// ES2 is much more complex with buffers https://www.khronos.org/registry/gles/extensions/EXT/EXT_draw_buffers.txt

		/*	int drawBuf = 0;
			if (doubleBuffer)
			{
				drawBuf = GL2ES2.GL_BACK;
				switch (mode)
				{
				case Canvas3D.FIELD_LEFT:
					drawBuf = GL2ES2.GL_BACK_LEFT;
					break;
				case Canvas3D.FIELD_RIGHT:
					drawBuf = GL2ES2.GL_BACK_RIGHT;
					break;
				case Canvas3D.FIELD_ALL:
					drawBuf = GL2ES2.GL_BACK;
					break;
				}
			}
			else
			{
				drawBuf = GL2ES2.GL_FRONT;
				switch (mode)
				{
				case Canvas3D.FIELD_LEFT:
					drawBuf = GL2ES2.GL_FRONT_LEFT;
					break;
				case Canvas3D.FIELD_RIGHT:
					drawBuf = GL2ES2.GL_FRONT_RIGHT;
					break;
				case Canvas3D.FIELD_ALL:
					drawBuf = GL2ES2.GL_FRONT;
					break;
				}
			}
		
			gl.glDrawBuffer(drawBuf);*/
	}

	private static int getFunctionValue(int func)
	{
		switch (func)
		{
		case RenderingAttributes.ALWAYS:
			func = GL2ES2.GL_ALWAYS;
			break;
		case RenderingAttributes.NEVER:
			func = GL2ES2.GL_NEVER;
			break;
		case RenderingAttributes.EQUAL:
			func = GL2ES2.GL_EQUAL;
			break;
		case RenderingAttributes.NOT_EQUAL:
			func = GL2ES2.GL_NOTEQUAL;
			break;
		case RenderingAttributes.LESS:
			func = GL2ES2.GL_LESS;
			break;
		case RenderingAttributes.LESS_OR_EQUAL:
			func = GL2ES2.GL_LEQUAL;
			break;
		case RenderingAttributes.GREATER:
			func = GL2ES2.GL_GREATER;
			break;
		case RenderingAttributes.GREATER_OR_EQUAL:
			func = GL2ES2.GL_GEQUAL;
			break;
		}

		return func;
	}

	private static int getStencilOpValue(int op)
	{
		switch (op)
		{
		case RenderingAttributes.STENCIL_KEEP:
			op = GL2ES2.GL_KEEP;
			break;
		case RenderingAttributes.STENCIL_ZERO:
			op = GL2ES2.GL_ZERO;
			break;
		case RenderingAttributes.STENCIL_REPLACE:
			op = GL2ES2.GL_REPLACE;
			break;
		case RenderingAttributes.STENCIL_INCR:
			op = GL2ES2.GL_INCR;
			break;
		case RenderingAttributes.STENCIL_DECR:
			op = GL2ES2.GL_DECR;
			break;
		case RenderingAttributes.STENCIL_INVERT:
			op = GL2ES2.GL_INVERT;
			break;
		}

		return op;
	}

	// ---------------------------------------------------------------------

	//
	// TransparencyAttributesRetained methods
	//

	private static final int[] blendFunctionTable = new int[TransparencyAttributes.MAX_BLEND_FUNC_TABLE_SIZE];

	static
	{
		blendFunctionTable[TransparencyAttributes.BLEND_ZERO] = GL2ES2.GL_ZERO;
		blendFunctionTable[TransparencyAttributes.BLEND_ONE] = GL2ES2.GL_ONE;
		blendFunctionTable[TransparencyAttributes.BLEND_SRC_ALPHA] = GL2ES2.GL_SRC_ALPHA;
		blendFunctionTable[TransparencyAttributes.BLEND_ONE_MINUS_SRC_ALPHA] = GL2ES2.GL_ONE_MINUS_SRC_ALPHA;
		blendFunctionTable[TransparencyAttributes.BLEND_DST_COLOR] = GL2ES2.GL_DST_COLOR;
		blendFunctionTable[TransparencyAttributes.BLEND_ONE_MINUS_DST_COLOR] = GL2ES2.GL_ONE_MINUS_DST_COLOR;
		blendFunctionTable[TransparencyAttributes.BLEND_SRC_COLOR] = GL2ES2.GL_SRC_COLOR;
		blendFunctionTable[TransparencyAttributes.BLEND_ONE_MINUS_SRC_COLOR] = GL2ES2.GL_ONE_MINUS_SRC_COLOR;
		blendFunctionTable[TransparencyAttributes.BLEND_CONSTANT_COLOR] = GL2ES2.GL_CONSTANT_COLOR;
	}

	//----------------------------------------------------------------------
	// Helper private functions for Canvas3D
	//
	//USED BY CONTEXT QUERIER BELOW which is used for create new context
	private static boolean getPropertiesFromCurrentContext(JoglContext ctx, GL2ES2 gl)
	{
		// FIXME: this is a heavily abridged set of the stuff in Canvas3D.c;
		// probably need to pull much more in
		int[] tmp = new int[1];
		gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, tmp, 0);
		ctx.setMaxTexCoordSets(tmp[0]);
		if (VirtualUniverse.mc.transparentOffScreen)
		{
			ctx.setAlphaClearValue(0.0f);
		}
		else
		{
			ctx.setAlphaClearValue(1.0f);
		}
		/*if (gl.isExtensionAvailable("GL_ARB_vertex_shader"))
		{
			gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_COORDS_ARB, tmp, 0);
			ctx.setMaxTexCoordSets(tmp[0]);
		}*/
		return true;
	}

	//Used by createNewContext above
	private static int[] extractVersionInfo(String versionString)
	{
		//FIXME: use the second flash regex system to get the first number out
		//examples
		//OpenGL ES 3.0 V@136.0 AU@ (GIT@I3fa967cfef)
		//4.5.0 NVIDIA 353.82
		//System.err.println("versionString: " + versionString);
		if (versionString.startsWith("OpenGL ES "))
			versionString = versionString.substring("OpenGL ES ".length());
		StringTokenizer tok = new StringTokenizer(versionString, ". ");
		int major = Integer.valueOf(tok.nextToken()).intValue();
		int minor = Integer.valueOf(tok.nextToken()).intValue();

		// See if there's vendor-specific information which might
		// imply a more recent OpenGL version
		tok = new StringTokenizer(versionString, " ");
		if (tok.hasMoreTokens())
		{
			tok.nextToken();
			if (tok.hasMoreTokens())
			{
				Pattern p = Pattern.compile("\\D*(\\d+)\\.(\\d+)\\.?(\\d*).*");
				Matcher m = p.matcher(tok.nextToken());
				if (m.matches())
				{
					int altMajor = Integer.valueOf(m.group(1)).intValue();
					int altMinor = Integer.valueOf(m.group(2)).intValue();
					// Avoid possibly confusing situations by requiring
					// major version to match
					if (altMajor == major && altMinor > minor)
					{
						minor = altMinor;
					}
				}
			}
		}
		return new int[] { major, minor };
	}

	//Used by createNewContext above
	private static void checkTextureExtensions(Canvas3D cv, JoglContext ctx, GL2ES2 gl, boolean gl13)
	{
		if (gl13)
		{

			//FIXME: setting this to cv.maxTexCoordSets = 8; and cutting the rest out doesn't work!
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_MULTI_TEXTURE;
			cv.multiTexAccelerated = true;
			int[] tmp = new int[1];
			gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, tmp, 0);
			cv.maxTextureUnits = tmp[0];
			cv.maxTexCoordSets = cv.maxTextureUnits;
		}

		//Combine function gone as shader not FFP
		/*if (gl.isExtensionAvailable("GL_ARB_texture_env_combine"))
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_COMBINE;
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_COMBINE_SUBTRACT;
		}
		else if (gl.isExtensionAvailable("GL_EXT_texture_env_combine"))
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_COMBINE;
		}
		
		if (gl.isExtensionAvailable("GL_ARB_texture_env_dot3") || gl.isExtensionAvailable("GL_EXT_texture_env_dot3"))
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_COMBINE_DOT3;
		}*/

		if (gl13)
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_CUBE_MAP;
		}

		if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic"))
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_ANISOTROPIC_FILTER;
			float[] tmp = new float[1];
			gl.glGetFloatv(GL2ES2.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, tmp, 0);
			cv.anisotropicDegreeMax = tmp[0];
		}

		if (!VirtualUniverse.mc.enforcePowerOfTwo && gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two"))
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_NON_POWER_OF_TWO;
		}

		//autoMipMapGeneration disabled
		/*if (gl.isExtensionAvailable("GL_SGIS_generate_mipmap"))
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_AUTO_MIPMAP_GENERATION;
		}*/

	}

	//Used by createNewContext above
	private static void checkGLSLShaderExtensions(Canvas3D cv, JoglContext ctx, GL2ES2 gl, boolean hasgl13)
	{

		// Force shaders to be disabled, since no multitexture support
		if (!hasgl13)
			return;

		if ((gl.isExtensionAvailable("GL_ARB_shader_objects") //
				&& gl.isExtensionAvailable("GL_ARB_shading_language_100")) //
				|| gl.isExtensionAvailable("GL_AMD_program_binary_Z400"))
		{

			// FIXME: this isn't complete and would need to set up the
			// JoglContext for dispatch of various routines such as those
			// related to vertex attributes
			int[] tmp = new int[1];
			gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, tmp, 0);
			cv.maxTextureImageUnits = tmp[0];
			gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, tmp, 0);
			cv.maxVertexTextureImageUnits = tmp[0];
			gl.glGetIntegerv(GL2ES2.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, tmp, 0);
			cv.maxCombinedTextureImageUnits = tmp[0];
			int vertexAttrOffset = VirtualUniverse.mc.glslVertexAttrOffset;
			ctx.setGLSLVertexAttrOffset(vertexAttrOffset);
			gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_ATTRIBS, tmp, 0);
			cv.maxVertexAttrs = tmp[0];
			// decr count to allow for reserved vertex attrs
			cv.maxVertexAttrs -= vertexAttrOffset;
			if (cv.maxVertexAttrs < 0)
			{
				cv.maxVertexAttrs = 0;
			}
			cv.shadingLanguageGLSL = true;
		}
	}

	//Used by createNewContext above
	private static void setupCanvasProperties(Canvas3D cv, JoglContext ctx, GL2ES2 gl)
	{
		// Note: this includes relevant portions from both the
		// NativePipeline's getPropertiesFromCurrentContext and setupCanvasProperties

		// Reset all fields
		cv.multiTexAccelerated = false;
		cv.maxTextureUnits = 1;
		cv.maxTexCoordSets = 1;
		cv.maxTextureImageUnits = 0;
		cv.maxVertexTextureImageUnits = 0;
		cv.maxCombinedTextureImageUnits = 0;
		cv.maxVertexAttrs = 0;
		cv.extensionsSupported = 0;
		cv.textureExtendedFeatures = 0;
		cv.textureColorTableSize = 0;
		cv.anisotropicDegreeMax = 0;
		cv.textureBoundaryWidthMax = 0;
		cv.textureWidthMax = 0;
		cv.textureHeightMax = 0;
		cv.texture3DWidthMax = 0;
		cv.texture3DHeightMax = 0;
		cv.texture3DDepthMax = 0;
		cv.shadingLanguageGLSL = false;

		// Now make queries and set up these fields
		String glVersion = gl.glGetString(GL2ES2.GL_VERSION);
		String glVendor = gl.glGetString(GL2ES2.GL_VENDOR);
		String glRenderer = gl.glGetString(GL2ES2.GL_RENDERER);
		cv.nativeGraphicsVersion = glVersion;
		cv.nativeGraphicsVendor = glVendor;
		cv.nativeGraphicsRenderer = glRenderer;

		//PJPJPJPJ just for debug
		//      System.out.println("***glVersion " +glVersion);
		//      System.out.println("***glVendor " +glVendor);
		//      System.out.println("***glRenderer " +glRenderer);
		//      System.out.println( ctx.getGLContext().toString());

		// find out the version, major and minor version number
		int[] versionNumbers = extractVersionInfo(glVersion);
		int major = versionNumbers[0];
		int minor = versionNumbers[1];

		///////////////////////////////////////////
		// setup the graphics context properties //

		// NOTE: Java 3D now requires OpenGL 1.3 for full functionality.
		// For backwards compatibility with certain older graphics cards and
		// drivers (e.g., the Linux DRI driver for older ATI cards),
		// we will try to run on OpenGL 1.2 in an unsupported manner. However,
		// we will not attempt to use OpenGL extensions for any features that
		// are available in OpenGL 1.3, specifically multitexture, multisample,
		// and cube map textures.

		if (major < 1 || (major == 1 && minor < 2))
		{
			throw new IllegalRenderingStateException(
					"Java 3D ERROR : OpenGL 1.2 or better is required (GL_VERSION=" + major + "." + minor + ")");
		}

		boolean gl20 = false;
		boolean gl14 = false;
		boolean gl13 = false;

		if (major == 1)
		{
			if (minor == 2)
			{
				System.err.println("JAVA 3D: OpenGL 1.2 detected; will run with reduced functionality");
			}
			if (minor >= 3)
			{
				gl13 = true;
			}
			if (minor >= 4)
			{
				gl14 = true;
			}
		}
		else
		// major >= 2 
		{
			gl13 = true;
			gl14 = true;
			gl20 = true;
		}

		if (gl20)
		{
			assert gl13;
			assert gl14;
			assert gl.isExtensionAvailable("GL_VERSION_2_0");
		}

		if (gl14)
		{
			assert gl13;
			assert gl.isExtensionAvailable("GL_VERSION_1_4");
		}

		if (gl13)
		{
			assert gl.isExtensionAvailable("GL_VERSION_1_3");
		}

		// Set up properties for OpenGL 1.3
		//cv.textureExtendedFeatures |= Canvas3D.TEXTURE_3D;

		// Note that we don't query for GL_ARB_imaging here

		cv.textureExtendedFeatures |= Canvas3D.TEXTURE_LOD_RANGE;

		/*
		if (gl14)
		{
			cv.textureExtendedFeatures |= Canvas3D.TEXTURE_AUTO_MIPMAP_GENERATION;
		}*/

		// look for OpenGL 2.0 features
		// Fix to Issue 455 : Need to disable NPOT textures for older cards that claim to support it.
		// Some older cards (e.g., Nvidia fx500 and ATI 9800) claim to support OpenGL 2.0.
		// This means that these cards have to support non-power-of-two (NPOT) texture,
		// but their lack the necessary HW force the vendors the emulate this feature in software.
		// The result is a ~100x slower down compare to power-of-two textures.
		// Do not check for gl20 but instead check of GL_ARB_texture_non_power_of_two extension string
		// if (gl20) {
		//    if(!VirtualUniverse.mc.enforcePowerOfTwo) {
		//        cv.textureExtendedFeatures |= Canvas3D.TEXTURE_NON_POWER_OF_TWO;
		//    }
		// }

		// Setup GL_EXT_abgr
		if (gl.isExtensionAvailable("GL_EXT_abgr"))
		{
			cv.extensionsSupported |= Canvas3D.EXT_ABGR;
		}

		// GL_BGR is always supported
		cv.extensionsSupported |= Canvas3D.EXT_BGR;

		// Setup multisample
		// FIXME: this is not correct for the Windows platform yet
		//FIXME: this might be tricky, if I screw around the accum calls turn on again
		// ES2 has new enable/disable on GL_SAMPLE_ALPHA_TO_COVERAGE and GL_SAMPLE_COVERAGE
		// and GL2 and ES2 both have glSampleCoverage calls
		// Renderer line 1158 is teh guy that goes for accum if this is not set
		if (gl13)
		{
			cv.extensionsSupported |= Canvas3D.MULTISAMPLE;
			ctx.setHasMultisample(true);
		}

		if ((cv.extensionsSupported & Canvas3D.MULTISAMPLE) != 0 && !VirtualUniverse.mc.implicitAntialiasing)
		{
			//with a bit of luck ES2 will ignore this call and leave sampling on
			//gl.glDisable(GL2ES2.GL_MULTISAMPLE);
		}

		// Check texture extensions
		checkTextureExtensions(cv, ctx, gl, gl13);

		// Check shader extensions
		checkGLSLShaderExtensions(cv, ctx, gl, gl13);

		cv.textureBoundaryWidthMax = 1;

		int[] tmp = new int[1];
		gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_SIZE, tmp, 0);
		cv.textureWidthMax = tmp[0];
		cv.textureHeightMax = tmp[0];

		/*tmp[0] = -1;
		gl.glGetIntegerv(GL2ES2.GL_MAX_3D_TEXTURE_SIZE, tmp, 0);
		cv.texture3DWidthMax = tmp[0];
		cv.texture3DHeightMax = tmp[0];
		cv.texture3DDepthMax = tmp[0];*/

	}

	// Not needed generally as transpose can be called on the inteface with gl
	private static void copyTranspose(double[] src, double[] dst)
	{
		dst[0] = src[0];
		dst[1] = src[4];
		dst[2] = src[8];
		dst[3] = src[12];
		dst[4] = src[1];
		dst[5] = src[5];
		dst[6] = src[9];
		dst[7] = src[13];
		dst[8] = src[2];
		dst[9] = src[6];
		dst[10] = src[10];
		dst[11] = src[14];
		dst[12] = src[3];
		dst[13] = src[7];
		dst[14] = src[11];
		dst[15] = src[15];
	}

	@Override
	void clear(Context ctx, float r, float g, float b, boolean clearStencil)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.clear()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.clear++;

		JoglesContext jctx = (JoglesContext) ctx;
		GL2ES2 gl = jctx.gl2es2;

		// Mask of which buffers to clear, this always includes color & depth
		int clearMask = GL2ES2.GL_DEPTH_BUFFER_BIT | GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_STENCIL_BUFFER_BIT;

		//NOTE stencil always cleared

		gl.glDepthMask(true);
		gl.glClearColor(r, g, b, jctx.getAlphaClearValue());
		gl.glClear(clearMask);
		if (DO_OUTPUT_ERRORS)
			outputErrors(ctx);
	}

	/**
	 *  This native method makes sure that the rendering for this canvas
	 *  gets done now.
	 */
	@Override
	// render is it's own thread so finish stops nothing
	void syncRender(Context ctx, boolean wait)
	{

		if (VERBOSE)
			System.err.println("JoglPipeline.syncRender()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.syncRenderTime = System.nanoTime();

		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;

		// clean up any buffers that need freeing
		doClearBuffers(ctx);

		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).outputPerFrameData();

		// also seems to be ok, just do it as well
		if (!NEVER_RELEASE_CONTEXT)
		{
			//if (wait)
			//	gl.glFinish();
			//else
			gl.glFlush();
		}

	}

	// The native method for swapBuffers - onscreen only
	@Override
	void swapBuffers(Canvas3D cv, Context ctx, Drawable drawable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.swapBuffers()");

		GLDrawable draw = drawable(drawable);
		draw.swapBuffers();

		((JoglesContext) ctx).gl_state.clear();
	}

	private static void outputErrors(Context ctx)
	{
		if (DO_OUTPUT_ERRORS)
		{
			GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
			int err = gl.glGetError();
			if (err != GL2ES2.GL_NO_ERROR)
			{
				//GL_NO_ERROR = 0x0            
				//GL_INVALID_ENUM = 0x500; 1280
				//GL_INVALID_VALUE = 0x501; 1281
				//GL_INVALID_OPERATION = 0x502; 1282
				//GL_INVALID_FRAMEBUFFER_OPERATION = 0x506; 1286
				//GL_OUT_OF_MEMORY= 0x505; 1285            
				//GL_STACK_UNDERFLOW 503?
				//GL_STACK_OVERFLOW 504?

				//check for no current shader program (likely a switch between scenes or something)
				if (err == GL2ES2.GL_INVALID_OPERATION)
				{
					int[] res = new int[1];
					gl.glGetIntegerv(GL2ES2.GL_CURRENT_PROGRAM, res, 0);
					// 0 is no current program
					if (res[0] == 0)
						return;
				}

				System.err.println("JoglesPipeline GL error reported " + err);
				StackTraceElement[] st = new Throwable().getStackTrace();
				if (st.length > 1)
					System.err.println("Occured in " + st[1]);

				// seems to produce heaps?
				/*err = gl.glGetError();
				if (err != GL2ES2.GL_NO_ERROR)
				{
					System.err.println("woooh second error too! "+ err);
					err = gl.glGetError();
					if (err != GL2ES2.GL_NO_ERROR)
					{
						System.err.println("woooh third error too! "+ err);
					}
				}*/
			}
		}
	}

	// The native method that sets this ctx to be the current one
	@Override
	boolean useCtx(Context ctx, Drawable drawable)
	{
		if (!NEVER_RELEASE_CONTEXT || !currently_current)
		{
			if (VERBOSE)
				System.err.println("JoglPipeline.useCtx()**********************************");
			if (OUTPUT_PER_FRAME_STATS)
				((JoglesContext) ctx).perFrameStats.useCtx++;

			GLContext context = context(ctx);

			if (context.getGLDrawable() == null)
				System.out.println("context.getGLDrawable() == null!");

			currently_current = true;
			int res = context.makeCurrent();
			return (res != GLContext.CONTEXT_NOT_CURRENT);
		}
		return true;
	}

	public static boolean currently_current = false;

	// Optionally release the context. Returns true if the context was released.
	@Override
	boolean releaseCtx(Context ctx)
	{
		if (!NEVER_RELEASE_CONTEXT)
		{
			if (VERBOSE)
				System.err.println("JoglPipeline.releaseCtx()");
			if (OUTPUT_PER_FRAME_STATS)
				((JoglesContext) ctx).perFrameStats.releaseCtx++;
			GLContext context = context(ctx);

			if (context.isCurrent())
				context.release();
		}
		return true;
	}

	// ---------------------------------------------------------------------

	//
	// MasterControl methods
	//

	// Maximum lights supported by the native API
	@Override
	int getMaximumLights()
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.getMaximumLights()");

		// FIXME: this isn't quite what the NativePipeline returns but
		// is probably close enough
		return 8;
	}

	//As offscreen is disable this should always return false
	private static boolean isOffscreenLayerSurfaceEnabled(Canvas3D cv)
	{
		/*if (cv.drawable == null || cv.offScreen)
			return false;
		
		JoglDrawable joglDrawble = (JoglDrawable) cv.drawable;
		JAWTWindow jawtwindow = (JAWTWindow) joglDrawble.getNativeWindow();
		if (jawtwindow == null)
			return false;
		
		return jawtwindow.isOffscreenLayerSurfaceEnabled();*/
		return false;
	}

	//Off screen usage only
	/*	private static boolean hasFBObjectSizeChanged(JoglDrawable jdraw, int width, int height)
		{
			if (!(jdraw.getGLDrawable() instanceof GLFBODrawable))
				return false;
	
			FBObject fboBack = ((GLFBODrawable) jdraw.getGLDrawable()).getFBObject(GL2ES2.GL_BACK);
			if (fboBack == null)
				return false;
	
			return (width != fboBack.getWidth() || height != fboBack.getHeight());
		}*/

	// Setup the full scene antialising in D3D and ogl when GL_ARB_multisamle supported
	@Override
	// looks like one time call in renderer.doWork
	void setFullSceneAntialiasing(Context ctx, boolean enable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.setFullSceneAntialiasing()");
		if (OUTPUT_PER_FRAME_STATS)
			((JoglesContext) ctx).perFrameStats.setFullSceneAntialiasing++;

		JoglContext joglctx = (JoglContext) ctx;
		GL2ES2 gl = ((JoglesContext) ctx).gl2es2;
		//PERF:GL2ES2 gl = context(ctx).getGL().getGL2ES2();
		// not supported in ES2, possibly just part of context generally
		//http://stackoverflow.com/questions/27035893/antialiasing-in-opengl-es-2-0
		//FIXME: This is working under GL2ES2 but will need to change I think
		//https://github.com/adrian110288/gdc2011-android-opengl/blob/master/src/com/example/gdc11/GDC11Activity.java

		if (joglctx.getHasMultisample() && !VirtualUniverse.mc.implicitAntialiasing)
		{
			if (enable)
			{
				System.out.println("I just set MULTISAMPLE just then");
				gl.glEnable(GL2ES2.GL_MULTISAMPLE);
			}
			else
			{
				gl.glDisable(GL2ES2.GL_MULTISAMPLE);
			}
		}
	}

	// Native method to update separate specular color control
	// looks like a one time call at the start of renderer.doWork
	@Override
	void updateSeparateSpecularColorEnable(Context ctx, boolean enable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.updateSeparateSpecularColorEnable()");

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			// bound to be not supported as definately shader work now
		
			if (enable)
			{
				gl.glLightModeli(GL2ES2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2ES2.GL_SEPARATE_SPECULAR_COLOR);
			}
			else
			{
				gl.glLightModeli(GL2ES2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2ES2.GL_SINGLE_COLOR);
			}*/
	}

	// ---------------------------------------------------------------------

	//
	// Canvas3D methods - native wrappers
	//

	// Mac/JRE 7; called from Renderer when resizing is detected
	// Implementation follows the approach in jogamp.opengl.GLDrawableHelper.resizeOffscreenDrawable(..)

	//this is called by renderer doWork in the setViewPort call, as offscreen is disable I can dump??
	@Override
	void resizeOffscreenLayer(Canvas3D cv, int cvWidth, int cvHeight)
	{
		if (!isOffscreenLayerSurfaceEnabled(cv))
			return;

		throw new UnsupportedOperationException("Offscreen not supported");
		/*
		JoglDrawable joglDrawable = (JoglDrawable) cv.drawable;
		if (!hasFBObjectSizeChanged(joglDrawable, cvWidth, cvHeight))
			return;
		
		int newWidth = Math.max(1, cvWidth);
		int newHeight = Math.max(1, cvHeight);
		
		GLDrawable glDrawble = joglDrawable.getGLDrawable();
		GLContext glContext = context(cv.ctx);
		
		// Assuming glContext != null
		
		final NativeSurface surface = glDrawble.getNativeSurface();
		final ProxySurface proxySurface = (surface instanceof ProxySurface) ? (ProxySurface) surface : null;
		
		final int lockRes = surface.lockSurface();
		
		try
		{
			// propagate new size - seems not relevant here
			if (proxySurface != null)
			{
				final UpstreamSurfaceHook ush = proxySurface.getUpstreamSurfaceHook();
				if (ush instanceof UpstreamSurfaceHook.MutableSize)
				{
					((UpstreamSurfaceHook.MutableSize) ush).setSurfaceSize(newWidth, newHeight);
				}
			}
			//else if(DEBUG) { // we have to assume surface contains the new size already, hence size check @ bottom
			 //     System.err.println("GLDrawableHelper.resizeOffscreenDrawable: Drawable's offscreen surface n.a. ProxySurface, but "+ns.getClass().getName()+": "+ns);
			//}
		
			GL2ES2 gl = glContext.getGL().getGL2ES2();
		
			// FBO : should be the default case on Mac OS X
			if (glDrawble instanceof GLFBODrawable)
			{
		
				// Resize GLFBODrawable
				// TODO msaa gets lost
				//				((GLFBODrawable)glDrawble).resetSize(gl);
		
				// Alternative: resize GL_BACK FBObject directly,
				// if multisampled the FBO sink (GL_FRONT) will be resized before the swap is executed
				int numSamples = ((GLFBODrawable) glDrawble).getChosenGLCapabilities().getNumSamples();
				FBObject fboObjectBack = ((GLFBODrawable) glDrawble).getFBObject(GL2ES2.GL_BACK);
				fboObjectBack.reset(gl, newWidth, newHeight, numSamples);//, false); // false = don't reset SamplingSinkFBO immediately
				fboObjectBack.bind(gl);
		
				// If double buffered without antialiasing the GL_FRONT FBObject
				// will be resized by glDrawble after the next swap-call
			}
			// pbuffer - not tested because Mac OS X 10.7+ supports FBO
			else
			{
				// Create new GLDrawable (pbuffer) and update the coresponding GLContext
		
				final GLContext currentContext = GLContext.getCurrent();
				final GLDrawableFactory factory = glDrawble.getFactory();
		
				// Ensure to sync GL command stream
				if (currentContext != glContext)
				{
					glContext.makeCurrent();
				}
				gl.glFinish();
				glContext.release();
		
				if (proxySurface != null)
				{
					proxySurface.enableUpstreamSurfaceHookLifecycle(false);
				}
		
				try
				{
					glDrawble.setRealized(false);
					// New GLDrawable
					glDrawble = factory.createGLDrawable(surface);
					glDrawble.setRealized(true);
		
					joglDrawable.setGLDrawable(glDrawble);
				}
				finally
				{
					if (proxySurface != null)
					{
						proxySurface.enableUpstreamSurfaceHookLifecycle(true);
					}
				}
		
				glContext.setGLDrawable(glDrawble, true); // re-association
		
				// make current last current context
				if (currentContext != null)
				{
					currentContext.makeCurrent();
				}
			}
		}
		finally
		{
			surface.unlockSurface();
		}*/
	}

	/**
	 * New method for preparing the pipeline with a context you prepared earlier
	 * @param cv
	 * @param glDrawable
	 * @param glContext
	 * @param shareCtx
	 * @param isSharedCtx
	 * @return
	 */
	Context createNewContext(Canvas3D cv, GLDrawable glDrawable, GLContext glContext, Context shareCtx, boolean isSharedCtx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.createNewContext()");

		/*GLDrawable glDrawable = null;
		GLContext glContext = null;
		
		 
			// determined in 'getBestConfiguration'
			GraphicsConfigInfo gcInf0 = Canvas3D.graphicsConfigTable.get(cv.graphicsConfiguration);
			AWTGraphicsConfiguration awtConfig = (AWTGraphicsConfiguration) gcInf0.getPrivateData();
		
			// JAWTWindow
			JAWTWindow nativeWindow = (JAWTWindow) NativeWindowFactory.getNativeWindow(cv, awtConfig);
			nativeWindow.lockSurface();
			try
			{
				glDrawable = GLDrawableFactory.getFactory(profile).createGLDrawable(nativeWindow);
				glContext = glDrawable.createContext(context(shareCtx));
			}
			finally
			{
				nativeWindow.unlockSurface();
			}
		
			cv.drawable = new JoglDrawable(glDrawable, nativeWindow);
			*/

		cv.drawable = new JoglDrawable(glDrawable, null);

		// assuming that this only gets called after addNotify has been called
		if (!glDrawable.isRealized())
		{
			glDrawable.setRealized(true);
		}

		// Apparently we are supposed to make the context current at this point
		// and set up a bunch of properties

		glContext.makeCurrent();

		// Work around for some low end graphics driver bug, such as Intel Chipset.
		// Issue 324 : Lockup J3D program and throw exception using JOGL renderer
		/*		boolean failed = false;
				int failCount = 0;
				int MAX_FAIL_COUNT = 5;
				do
				{
					failed = false;
					int res = glContext.makeCurrent();
					if (res == GLContext.CONTEXT_NOT_CURRENT)
					{
						// System.err.println("makeCurrent fail : " + failCount);
						failed = true;
						++failCount;
						try
						{
							Thread.sleep(100);
						}
						catch (InterruptedException e)
						{
						}
					}
				}
				while (failed && (failCount < MAX_FAIL_COUNT));
		
				if (failCount == MAX_FAIL_COUNT)
				{
					throw new IllegalRenderingStateException("Unable to make new context current after " + failCount + "tries");
				}*/

		GL2ES2 gl = glContext.getGL().getGL2ES2();

		//New context that stores information about current render pass		
		JoglesContext ctx = new JoglesContext(glContext);

		//I can't find a route to hand this back so I'm just printing it out here
		//		IntBuffer buff = IntBuffer.allocate(1);
		//		gl.glGetIntegerv(GL2ES2.GL_DEPTH_BITS, buff);
		//		if (buff.get(0) < Canvas3D.graphicsConfigTable.get(cv.graphicsConfiguration).getGraphicsConfigTemplate3D().getDepthSize())
		//			System.err.println("Warning depth buffer smaller than requested: " + buff.get(0));

		try
		{
			if (!getPropertiesFromCurrentContext(ctx, gl))
			{
				throw new IllegalRenderingStateException("Unable to fetch properties from current OpenGL context");
			}

			if (!isSharedCtx)
			{
				// Set up fields in Canvas3D
				setupCanvasProperties(cv, ctx, gl);
			}

			// Enable rescale normal
			//If enabled and no vertex shader is active...
			//gl.glEnable(GL2ES2.GL_RESCALE_NORMAL);  

			//The initial value is GL_AMBIENT_AND_DIFFUSE.            
			//gl.glColorMaterial(GL2ES2.GL_FRONT_AND_BACK, GL2ES2.GL_DIFFUSE);
			gl.glDepthFunc(GL2ES2.GL_LEQUAL);
			//gl.glEnable(GL2ES2.GL_COLOR_MATERIAL);//FIXME: once materials and gl_Color working

			/*
			OpenGL specs:
			   glReadBuffer specifies a color buffer as the source for subsequent glReadPixels.
			   This source mode is initially GL_FRONT in single-buffered and GL_BACK in double-buffered configurations.
			
			We leave this mode unchanged in on-screen rendering and adjust it in off-screen rendering. See below.
			*/
			//          gl.glReadBuffer(GL_FRONT); 		// off window, default for single-buffered non-stereo window

			// Issue 417: JOGL: Mip-mapped NPOT textures rendered incorrectly
			// J3D images are aligned to 1 byte
			gl.glPixelStorei(GL2ES2.GL_UNPACK_ALIGNMENT, 1);

			//http://filmicgames.com/archives/233 wow amazing stuff, but lighting is me so hopefully gone
			// Workaround for issue 400: Enable separate specular by default
			//gl.glLightModeli(GL2ES2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2ES2.GL_SEPARATE_SPECULAR_COLOR);

			// Mac OS X / JRE 7 : onscreen rendering = offscreen rendering
			// bind FBO
			if (glDrawable instanceof GLFBODrawable)
			{
				GLFBODrawable fboDrawable = (GLFBODrawable) glDrawable;
				// bind GLFBODrawable's drawing FBObject
				// GL_BACK returns the correct FBOObject for single/double buffering, incl. multisampling
				fboDrawable.getFBObject(GL2ES2.GL_BACK).bind(gl);
			}

		}
		finally
		{
			glContext.release();
		}

		return ctx;
	}

	// This is the native method for creating the underlying graphics context.
	//Once NewtWindow is working this becomes a simple unsupported operation
	@Override
	Context createNewContext(Canvas3D cv, Drawable drawable, Context shareCtx, boolean isSharedCtx, boolean offScreen)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	void destroyContext(Drawable drawable, Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.destroyContext()");

		JoglDrawable joglDrawable = (JoglDrawable) drawable;
		GLContext context = context(ctx);

		// possibly bug in marshmallow
		// google belwo and see its a bug on marshmallow
		//04-19 00:33:36.941 18278-18322/com.ingenieur.ese.eseandroid E/Surface: getSlotFromBufferLocked: unknown buffer: 0xb8dc6060
		//04-19 00:33:37.182 18278-18278/com.ingenieur.ese.eseandroid D/JogAmp.NEWT: onStop.0

		//after this a restart adn a swapBuffers call
		//gl_window.getDelegatedDrawable().swapBuffers();
		// gets a 
		//com.jogamp.opengl.GLException: Error swapping buffers, eglError 0x300d, jogamp.opengl.egl.EGLDrawable[realized true,

		if (joglDrawable != null)
		{
			if (GLContext.getCurrent() == context)
			{
				context.release();
			}
			context.destroy();

			// assuming this is the right point at which to make this call
			joglDrawable.getGLDrawable().setRealized(false);

			joglDrawable.destroyNativeWindow();
		}
	}

	// This is the native method for getting the number of lights the underlying
	// native library can support.
	@Override
	int getNumCtxLights(Context ctx)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.getNumCtxLights()");

		/*	GL2ES2 gl = context(ctx).getGL().getGL2ES2();
			int[] res = new int[1];
			gl.glGetIntegerv(GL2ES2.GL_MAX_LIGHTS, res, 0);
			return res[0];*/

		//lights are now me! this is not called anyway
		return 8;
	}

	// True under Solaris,
	// False under windows when display mode <= 8 bit
	@Override
	//  probably pointless?
	boolean validGraphicsMode()
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.validGraphicsMode()");

		// FIXME: believe this should do exactly what the native code
		// used to, but not 100% sure (also in theory should only run
		// this code on the Windows platform? What about Mac OS X?)
		/*		DisplayMode currentMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
				// Note: on X11 platforms, a bit depth < 0 simply indicates that
				// multiple visuals are supported on the current display mode
		
				if (VERBOSE)
					System.err.println("  Returning " + (currentMode.getBitDepth() < 0 || currentMode.getBitDepth() > 8));
		
				return (currentMode.getBitDepth() < 0 || currentMode.getBitDepth() > 8);*/

		return true;
	}

	// Native method for eye lighting
	@Override
	void ctxUpdateEyeLightingEnable(Context ctx, boolean localEyeLightingEnable)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.ctxUpdateEyeLightingEnable()");

		//lighting is entirely in shaders now so this should affect nothing?

		/*	GL2 gl = context(ctx).getGL().getGL2();
			//GL2ES2 gl = context(ctx).getGL().getGL2ES2();
		
			if (localEyeLightingEnable)
			{
				gl.glLightModeli(GL2ES2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL2ES2.GL_TRUE);
			}
			else
			{
				gl.glLightModeli(GL2ES2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL2ES2.GL_FALSE);
			}*/
	}
	// ---------------------------------------------------------------------

	//
	// DrawingSurfaceObject methods
	//

	// Method to construct a new DrawingSurfaceObject
	@Override
	DrawingSurfaceObject createDrawingSurfaceObject(Canvas3D cv)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.createDrawingSurfaceObject()");
		return new JoglDrawingSurfaceObject(cv);
	}

	// Method to free the drawing surface object
	@Override
	//NOOP
	void freeDrawingSurface(Canvas3D cv, DrawingSurfaceObject drawingSurfaceObject)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.freeDrawingSurface()");
		// This method is a no-op
	}

	// Method to free the native drawing surface object
	@Override
	//NOOP
	void freeDrawingSurfaceNative(Object o)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.freeDrawingSurfaceNative()");
		// This method is a no-op
	}

	//----------------------------------------------------------------------
	// Context-related routines
	//

	// Helper used everywhere
	//USED heaps
	private static GLContext context(Context ctx)
	{
		if (ctx == null)
			return null;
		return ((JoglContext) ctx).getGLContext();
	}

	// Helper used everywhere
	//USED a small amount
	private static GLDrawable drawable(Drawable drawable)
	{
		if (drawable == null)
			return null;
		return ((JoglDrawable) drawable).getGLDrawable();
	}

	//----------------------------------------------------------------------
	// General helper routines
	//

	private static ThreadLocal<FloatBuffer> nioVertexTemp = new ThreadLocal<FloatBuffer>();
	private static ThreadLocal<DoubleBuffer> nioVertexDoubleTemp = new ThreadLocal<DoubleBuffer>();
	private static ThreadLocal<FloatBuffer> nioColorTemp = new ThreadLocal<FloatBuffer>();
	private static ThreadLocal<ByteBuffer> nioColorByteTemp = new ThreadLocal<ByteBuffer>();
	private static ThreadLocal<FloatBuffer> nioNormalTemp = new ThreadLocal<FloatBuffer>();
	private static ThreadLocal<FloatBuffer[]> nioTexCoordSetTemp = new ThreadLocal<FloatBuffer[]>();
	private static ThreadLocal<FloatBuffer[]> nioVertexAttrSetTemp = new ThreadLocal<FloatBuffer[]>();

	//I think these are not used often as nio buffers sort it out
	// but they are used a bit mind you
	private static FloatBuffer getVertexArrayBuffer(float[] vertexArray)
	{
		return getVertexArrayBuffer(vertexArray, true);
	}

	private static FloatBuffer getVertexArrayBuffer(float[] vertexArray, boolean copyData)
	{
		return getNIOBuffer(vertexArray, nioVertexTemp, copyData);
	}

	private static DoubleBuffer getVertexArrayBuffer(double[] vertexArray)
	{
		return getVertexArrayBuffer(vertexArray, true);
	}

	private static DoubleBuffer getVertexArrayBuffer(double[] vertexArray, boolean copyData)
	{
		return getNIOBuffer(vertexArray, nioVertexDoubleTemp, true);
	}

	private static FloatBuffer getColorArrayBuffer(float[] colorArray)
	{
		return getColorArrayBuffer(colorArray, true);
	}

	private static FloatBuffer getColorArrayBuffer(float[] colorArray, boolean copyData)
	{
		return getNIOBuffer(colorArray, nioColorTemp, true);
	}

	private static ByteBuffer getColorArrayBuffer(byte[] colorArray)
	{
		return getColorArrayBuffer(colorArray, true);
	}

	private static ByteBuffer getColorArrayBuffer(byte[] colorArray, boolean copyData)
	{
		return getNIOBuffer(colorArray, nioColorByteTemp, true);
	}

	private static FloatBuffer getNormalArrayBuffer(float[] normalArray)
	{
		return getNormalArrayBuffer(normalArray, true);
	}

	private static FloatBuffer getNormalArrayBuffer(float[] normalArray, boolean copyData)
	{
		return getNIOBuffer(normalArray, nioNormalTemp, true);
	}

	private static FloatBuffer[] getTexCoordSetBuffer(Object[] texCoordSet)
	{
		return getNIOBuffer(texCoordSet, nioTexCoordSetTemp);
	}

	private static FloatBuffer[] getVertexAttrSetBuffer(Object[] vertexAttrSet)
	{
		return getNIOBuffer(vertexAttrSet, nioVertexAttrSetTemp);
	}

	private static FloatBuffer getNIOBuffer(float[] array, ThreadLocal<FloatBuffer> threadLocal, boolean copyData)
	{
		if (array == null)
		{
			return null;
		}
		FloatBuffer buf = threadLocal.get();
		if (buf == null)
		{
			buf = Buffers.newDirectFloatBuffer(array.length);
			threadLocal.set(buf);
		}
		else
		{
			buf.rewind();
			if (buf.remaining() < array.length)
			{
				int newSize = Math.max(2 * buf.remaining(), array.length);
				buf = Buffers.newDirectFloatBuffer(newSize);
				threadLocal.set(buf);
			}
		}
		if (copyData)
		{
			buf.put(array);
			buf.rewind();
		}
		return buf;
	}

	private static DoubleBuffer getNIOBuffer(double[] array, ThreadLocal<DoubleBuffer> threadLocal, boolean copyData)
	{
		if (array == null)
		{
			return null;
		}
		DoubleBuffer buf = threadLocal.get();
		if (buf == null)
		{
			buf = Buffers.newDirectDoubleBuffer(array.length);
			threadLocal.set(buf);
		}
		else
		{
			buf.rewind();
			if (buf.remaining() < array.length)
			{
				int newSize = Math.max(2 * buf.remaining(), array.length);
				buf = Buffers.newDirectDoubleBuffer(newSize);
				threadLocal.set(buf);
			}
		}
		if (copyData)
		{
			buf.put(array);
			buf.rewind();
		}
		return buf;
	}

	private static ByteBuffer getNIOBuffer(byte[] array, ThreadLocal<ByteBuffer> threadLocal, boolean copyData)
	{
		if (array == null)
		{
			return null;
		}
		ByteBuffer buf = threadLocal.get();
		if (buf == null)
		{
			buf = Buffers.newDirectByteBuffer(array.length);
			threadLocal.set(buf);
		}
		else
		{
			buf.rewind();
			if (buf.remaining() < array.length)
			{
				int newSize = Math.max(2 * buf.remaining(), array.length);
				buf = Buffers.newDirectByteBuffer(newSize);
				threadLocal.set(buf);
			}
		}
		if (copyData)
		{
			buf.put(array);
			buf.rewind();
		}
		return buf;
	}

	private static FloatBuffer[] getNIOBuffer(Object[] array, ThreadLocal<FloatBuffer[]> threadLocal)
	{
		if (array == null)
		{
			return null;
		}
		FloatBuffer[] bufs = threadLocal.get();

		// First resize array of FloatBuffers
		if (bufs == null)
		{
			bufs = new FloatBuffer[array.length];
			threadLocal.set(bufs);
		}
		else if (bufs.length < array.length)
		{
			FloatBuffer[] newBufs = new FloatBuffer[array.length];
			System.arraycopy(bufs, 0, newBufs, 0, bufs.length);
			bufs = newBufs;
			threadLocal.set(bufs);
		}

		// Now go down array of arrays, converting each into a direct FloatBuffer
		for (int i = 0; i < array.length; i++)
		{
			float[] cur = (float[]) array[i];
			FloatBuffer buf = bufs[i];
			if (buf == null)
			{
				buf = Buffers.newDirectFloatBuffer(cur.length);
				bufs[i] = buf;
			}
			else
			{
				buf.rewind();
				if (buf.remaining() < cur.length)
				{
					int newSize = Math.max(2 * buf.remaining(), cur.length);
					buf = Buffers.newDirectFloatBuffer(newSize);
					bufs[i] = buf;
				}
			}
			buf.put(cur);
			buf.rewind();
		}

		return bufs;
	}

	///PJPJPJPJ requesting caps is expensive caps are unlikely to change during live times
	private isExtensionAvailable isExtensionAvailable = new isExtensionAvailable();

	private class isExtensionAvailable
	{
		private int GL_EXT_abgr = 0;

		private boolean GL_EXT_abgr(GL2ES2 gl)
		{
			if (GL_EXT_abgr == 0)
				GL_EXT_abgr = gl.isExtensionAvailable("GL_EXT_abgr") ? 1 : -1;

			return GL_EXT_abgr == 1;
		}

		private int GL_ARB_imaging = 0;

		private boolean GL_ARB_imaging(GL2ES2 gl)
		{
			if (GL_ARB_imaging == 0)
				GL_ARB_imaging = gl.isExtensionAvailable("GL_ARB_imaging") ? 1 : -1;

			return GL_ARB_imaging == 1;
		}

	}

	// Methods to get actual capabilities from Canvas3D
	@Override
	boolean hasDoubleBuffer(Canvas3D cv)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.hasDoubleBuffer()");
		if (VERBOSE)
			System.err.println("  Returning " + caps(cv).getDoubleBuffered());
		return caps(cv).getDoubleBuffered();
	}

	@Override
	boolean hasStereo(Canvas3D cv)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.hasStereo()");
		if (VERBOSE)
			System.err.println("  Returning " + caps(cv).getStereo());
		return caps(cv).getStereo();
	}

	@Override
	int getStencilSize(Canvas3D cv)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.getStencilSize()");
		if (VERBOSE)
			System.err.println("  Returning " + caps(cv).getStencilBits());
		return caps(cv).getStencilBits();
	}

	@Override
	boolean hasSceneAntialiasingMultisample(Canvas3D cv)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.hasSceneAntialiasingMultisample()");
		if (VERBOSE)
			System.err.println("  Returning " + caps(cv).getSampleBuffers());

		return caps(cv).getSampleBuffers();
	}

	@Override
	boolean hasSceneAntialiasingAccum(Canvas3D cv)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.hasSceneAntialiasingAccum()");
		//Accum style antialiasing is gone
		return false;
		/*GLCapabilities caps = caps(cv);
		if (VERBOSE)
			System.err
					.println("  Returning " + (caps.getAccumRedBits() > 0 && caps.getAccumGreenBits() > 0 && caps.getAccumBlueBits() > 0));
		return (caps.getAccumRedBits() > 0 && caps.getAccumGreenBits() > 0 && caps.getAccumBlueBits() > 0);
		*/

	}

	//Used to get caps for the canvas3d
	private static GLCapabilities caps(Canvas3D ctx)
	{
		if (ctx.drawable != null)
		{
			// latest state for on- and offscreen drawables
			return (GLCapabilities) drawable(ctx.drawable).getChosenGLCapabilities();
		}
		else
		{
			// state at the time of 'getBestConfiguration'
			return ((JoglGraphicsConfiguration) ctx.graphicsConfiguration).getGLCapabilities();
		}
	}

	// AWT AWT AWT AWT AWT AWT AWT AWT AWT
	// ---------------------------------------------------------------------

	// Determine whether specified graphics config is supported by pipeline
	@Override
	boolean isGraphicsConfigSupported(GraphicsConfigTemplate3D gct, GraphicsConfiguration gc)
	{
		if (VERBOSE)
			System.err.println("JoglPipeline.isGraphicsConfigSupported()");

		// FIXME: it looks like this method is implemented incorrectly
		// in the existing NativePipeline in both the Windows and X11
		// ports. According to the semantics of the javadoc, it looks
		// like this method is supposed to figure out the OpenGL
		// capabilities which would be requested by the passed
		// GraphicsConfiguration object were it to be used, and see
		// whether it is possible to create a context with them.
		// Instead, on both platforms, the implementations basically set
		// up a query based on the contents of the
		// GraphicsConfigTemplate3D object, using the
		// GraphicsConfiguration object only to figure out on which
		// GraphicsDevice and screen we're making the request, and see
		// whether it's possible to choose an OpenGL pixel format based
		// on that information. This makes this method less useful and
		// we can probably just safely return true here uniformly
		// without breaking anything.
		return true;
	}
	//
	// Canvas3D / GraphicsConfigTemplate3D methods - logic dealing with
	// native graphics configuration or drawing surface
	//

	// Return a graphics config based on the one passed in. Note that we can
	// assert that the input config is non-null and was created from a
	// GraphicsConfigTemplate3D.
	// This method must return a valid GraphicsConfig, or else it must throw
	// an exception if one cannot be returned.
	@Override
	// during Canvas3D init
	GraphicsConfiguration getGraphicsConfig(GraphicsConfiguration gconfig)
	{
		throw new UnsupportedOperationException();
		/*if (VERBOSE)
			System.err.println("JoglPipeline.getGraphicsConfig()");
		
		GraphicsConfigInfo gcInf0 = Canvas3D.graphicsConfigTable.get(gconfig);
		AWTGraphicsConfiguration awtConfig = (AWTGraphicsConfiguration) gcInf0.getPrivateData();
		
		return awtConfig.getAWTGraphicsConfiguration();*/
	}

	/*private enum DisabledCaps
	{
		STEREO, AA, DOUBLE_BUFFER,
	}*/

	// Get best graphics config from pipeline
	@Override
	// during Canvas3D2D init
	GraphicsConfiguration getBestConfiguration(GraphicsConfigTemplate3D gct, GraphicsConfiguration[] gc)
	{
		throw new UnsupportedOperationException();
		/*		if (VERBOSE)
					System.err.println("JoglPipeline.getBestConfiguration()");
		
				// Create a GLCapabilities based on the GraphicsConfigTemplate3D
				final GLCapabilities caps = new GLCapabilities(profile);
		
				caps.setDoubleBuffered(gct.getDoubleBuffer() != GraphicsConfigTemplate.UNNECESSARY);
		
				caps.setStereo(gct.getStereo() != GraphicsConfigTemplate.UNNECESSARY);
		
				// Scene antialiasing only if double buffering
				if (gct.getSceneAntialiasing() != GraphicsConfigTemplate.UNNECESSARY && gct.getDoubleBuffer() != GraphicsConfigTemplate.UNNECESSARY)
				{
					caps.setSampleBuffers(true);
					caps.setNumSamples(2);
				}
				else
				{
					caps.setSampleBuffers(false);
					caps.setNumSamples(0);
				}
		
				caps.setDepthBits(gct.getDepthSize());
				caps.setStencilBits(gct.getStencilSize());
		
				caps.setRedBits(Math.max(5, gct.getRedSize()));
				caps.setGreenBits(Math.max(5, gct.getGreenSize()));
				caps.setBlueBits(Math.max(5, gct.getBlueSize()));
		
				// Issue 399: Request alpha buffer if transparentOffScreen is set
				if (VirtualUniverse.mc.transparentOffScreen)
				{
					caps.setAlphaBits(1);
				}
		
				// Add PREFERRED capabilities in order of least to highest priority and we will try disabling them
				ArrayList<DisabledCaps> capsToDisable = new ArrayList<DisabledCaps>();
		
				if (gct.getStereo() == GraphicsConfigTemplate.PREFERRED)
				{
					capsToDisable.add(DisabledCaps.STEREO);
				}
		
				if (gct.getSceneAntialiasing() == GraphicsConfigTemplate.PREFERRED)
				{
					capsToDisable.add(DisabledCaps.AA);
				}
		
				// if AA is required, so is double buffering.
				if (gct.getSceneAntialiasing() != GraphicsConfigTemplate.REQUIRED && gct.getDoubleBuffer() == GraphicsConfigTemplate.PREFERRED)
				{
					capsToDisable.add(DisabledCaps.DOUBLE_BUFFER);
				}
		
				// Pick an arbitrary graphics device.
				GraphicsDevice device = gc[0].getDevice();
				AbstractGraphicsScreen screen = (device != null) ? AWTGraphicsScreen.createScreenDevice(device, AbstractGraphicsDevice.DEFAULT_UNIT)
						: AWTGraphicsScreen.createDefault();
		
				// Create a Frame and dummy GLCanvas to perform eager pixel format selection
		
				// Note that we loop in similar fashion to the NativePipeline's
				// native code in the situation where we need to disable certain
				// capabilities which aren't required
				boolean tryAgain = true;
				CapabilitiesCapturer capturer = null;
				AWTGraphicsConfiguration awtConfig = null;
				while (tryAgain)
				{
					Frame f = new Frame();
					f.setUndecorated(true);
					f.setLayout(new BorderLayout());
					capturer = new CapabilitiesCapturer();
					try
					{
						awtConfig = createAwtGraphicsConfiguration(caps, capturer, screen);
						QueryCanvas canvas = new QueryCanvas(awtConfig, capturer);
						f.add(canvas, BorderLayout.CENTER);
						f.setSize(MIN_FRAME_SIZE, MIN_FRAME_SIZE);
						f.setVisible(true);
						canvas.doQuery();
						if (DEBUG_CONFIG)
						{
							System.err.println("Waiting for CapabilitiesCapturer");
						}
						// Try to wait for result without blocking EDT
						if (!EventQueue.isDispatchThread())
						{
							synchronized (capturer)
							{
								if (!capturer.done())
								{
									try
									{
										capturer.wait(WAIT_TIME);
									}
									catch (InterruptedException e)
									{
									}
								}
							}
						}
						disposeOnEDT(f);
						tryAgain = false;
					}
					catch (GLException e)
					{
						// Failure to select a pixel format; try switching off one
						// of the only-preferred capabilities
						if (capsToDisable.size() == 0)
						{
							tryAgain = false;
						}
						else
						{
							switch (capsToDisable.remove(0))
							{
							case STEREO:
								caps.setStereo(false);
								break;
							case AA:
								caps.setSampleBuffers(false);
								break;
							case DOUBLE_BUFFER:
								caps.setDoubleBuffered(false);
								break;
							}
							awtConfig = null;
						}
					}
				}
				int chosenIndex = capturer.getChosenIndex();
				GLCapabilities chosenCaps = null;
				if (chosenIndex < 0)
				{
					if (DEBUG_CONFIG)
					{
						System.err.println("CapabilitiesCapturer returned invalid index");
					}
					// It's possible some platforms or implementations might not
					// support the GLCapabilitiesChooser mechanism; feed in the
					// same GLCapabilities later which we gave to the selector
					chosenCaps = caps;
				}
				else
				{
					if (DEBUG_CONFIG)
					{
						System.err.println("CapabilitiesCapturer returned index=" + chosenIndex);
					}
					chosenCaps = capturer.getCapabilities();
				}
		
				// FIXME chosenIndex isn't used anymore, used -1 instead of finding it.
				JoglGraphicsConfiguration config = new JoglGraphicsConfiguration(chosenCaps, chosenIndex, device);
		
				// FIXME: because of the fact that JoglGraphicsConfiguration
				// doesn't override hashCode() or equals(), we will basically be
				// creating a new one each time getBestConfiguration() is
				// called; in theory, we should probably map the same
				// GLCapabilities on the same GraphicsDevice to the same
				// JoglGraphicsConfiguration object
		
				// Cache the GraphicsTemplate3D
				GraphicsConfigInfo gcInf0 = new GraphicsConfigInfo(gct);
				gcInf0.setPrivateData(awtConfig);
		
				synchronized (Canvas3D.graphicsConfigTable)
				{
					Canvas3D.graphicsConfigTable.put(config, gcInf0);
				}
		
				return config;*/
	}

	//private boolean checkedForGetScreenMethod = false;
	//private Method getScreenMethod = null;

	@Override
	//Screen3D class calls during init and that init is only called in the init of Canvas3D
	// Notice this is using reflection on the GraphicsDevice!
	int getScreen(final GraphicsDevice graphicsDevice)
	{
		//FIXME: this should use the GLWindow business
		/*	if (VERBOSE)
				System.err.println("JoglPipeline.getScreen()");
			// can I just do this and damn it?
			// this appear to work fine, but not if you move the screen 
			// from one monitor to the other between frame show and start render
			if (true)
				return 0;
		
			if (!checkedForGetScreenMethod)
			{
				// All of the Sun GraphicsDevice implementations have a method
				//   int getScreen();
				// which we want to call reflectively if it's available.
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					@Override
					public Object run()
					{
						try
						{
							getScreenMethod = graphicsDevice.getClass().getDeclaredMethod("getScreen", new Class[] {});
							getScreenMethod.setAccessible(true);
						}
						catch (Exception e)
						{
						}
						checkedForGetScreenMethod = true;
						return null;
					}
				});
			}
		
			if (getScreenMethod != null)
			{
				try
				{
					return ((Integer) getScreenMethod.invoke(graphicsDevice, (Object[]) null)).intValue();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}*/

		return 0;
	}

	// getBestConfiguration ONLY below here VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
	// Non pipeline interface too

	//----------------------------------------------------------------------
	// Helper classes and methods to support query context functionality
	// and pixel format selection
	// Used by Query Canvas apabilitiesCapturer and therefore only get best configuration
	//	private interface ExtendedCapabilitiesChooser extends GLCapabilitiesChooser
	//	{
	//		public void init(GLContext context);
	//	}

	// Canvas subclass to help with various query operations such as the
	// "query context" mechanism and pixel format selection.
	// Must defeat and simplify the single-threading behavior of JOGL's
	// GLCanvas in order to be able to set up a temporary pixel format
	// and OpenGL context. Apparently simply turning off the
	// single-threaded mode isn't enough to do this.

	// Used by get best configuration
	/*	private final class QueryCanvas extends Canvas
		{
	
			private GLDrawable glDrawable;
			private ExtendedCapabilitiesChooser chooser;
			private boolean alreadyRan;
	
			private AWTGraphicsConfiguration awtConfig = null;
			private JAWTWindow nativeWindow = null;
	
			private QueryCanvas(AWTGraphicsConfiguration awtConfig, ExtendedCapabilitiesChooser chooser)
			{
				// The platform-specific GLDrawableFactory will only provide a
				// non-null GraphicsConfiguration on platforms where this is
				// necessary (currently only X11, as Windows allows the pixel
				// format of the window to be set later and Mac OS X seems to
				// handle this very differently than all other platforms). On
				// other platforms this method returns null; it is the case (at
				// least in the Sun AWT implementation) that this will result in
				// equivalent behavior to calling the no-arg super() constructor
				// for Canvas.
				super(awtConfig.getAWTGraphicsConfiguration());
	
				this.awtConfig = awtConfig;
				this.chooser = chooser;
			}
	
			@Override
			public void addNotify()
			{
				super.addNotify();
	
				nativeWindow = (JAWTWindow) NativeWindowFactory.getNativeWindow(this, awtConfig);
				nativeWindow.lockSurface();
				try
				{
					glDrawable = GLDrawableFactory.getFactory(profile).createGLDrawable(nativeWindow);
				}
				finally
				{
					nativeWindow.unlockSurface();
				}
	
				glDrawable.setRealized(true);
			}
	
			// It seems that at least on Mac OS X we need to do the OpenGL
			// context-related work outside of the addNotify call because the
			// Canvas hasn't been resized to a non-zero size by that point
			private void doQuery()
			{
				if (alreadyRan)
					return;
				GLContext context = glDrawable.createContext(null);
				int res = context.makeCurrent();
				if (res != GLContext.CONTEXT_NOT_CURRENT)
				{
					try
					{
						chooser.init(context);
					}
					finally
					{
						context.release();
					}
				}
				context.destroy();
				alreadyRan = true;
	
				glDrawable.setRealized(false);
				nativeWindow.destroy();
			}
		}*/

	// Used by get best configuration
	/*	private static AWTGraphicsConfiguration createAwtGraphicsConfiguration(GLCapabilities capabilities, CapabilitiesChooser chooser,
				AbstractGraphicsScreen screen)
		{
			GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class, GLCapabilities.class);
			AWTGraphicsConfiguration awtGraphicsConfiguration = (AWTGraphicsConfiguration) factory.chooseGraphicsConfiguration(capabilities,
					capabilities, chooser, screen, VisualIDHolder.VID_UNDEFINED);
			return awtGraphicsConfiguration;
		}*/

	// Used in conjunction with IndexCapabilitiesChooser in pixel format
	// selection -- see getBestConfiguration

	//Used by getBestConfiguration
	/*private static class CapabilitiesCapturer extends DefaultGLCapabilitiesChooser implements ExtendedCapabilitiesChooser
	{
		private boolean done;
		private GLCapabilities capabilities;
		private int chosenIndex = -1;
	
		public boolean done()
		{
			return done;
		}
	
		public GLCapabilities getCapabilities()
		{
			return capabilities;
		}
	
		public int getChosenIndex()
		{
			return chosenIndex;
		}
	
		public int chooseCapabilities(GLCapabilities desired, GLCapabilities[] available, int windowSystemRecommendedChoice)
		{
			int res = super.chooseCapabilities(desired, Arrays.asList(available), windowSystemRecommendedChoice);
			capabilities = available[res];
			chosenIndex = res;
			markDone();
			return res;
		}
	
		@Override
		public void init(GLContext context)
		{
			// Avoid hanging things up for several seconds
			kick();
		}
	
		private void markDone()
		{
			synchronized (this)
			{
				done = true;
				notifyAll();
			}
		}
	
		private void kick()
		{
			synchronized (this)
			{
				notifyAll();
			}
		}
	}*/

	// Used to support the query context mechanism -- needs to be more
	// than just a GLCapabilitiesChooser

	//ONLY used by createQuerycontext above, hence unused
	//What possibly invoked via some sort of crazy reflect, do not delete
	// can't seem to get it invoked now?
	/*private final class ContextQuerier extends DefaultGLCapabilitiesChooser implements ExtendedCapabilitiesChooser
	{
		private Canvas3D canvas;
		private boolean done;
	
		public ContextQuerier(Canvas3D canvas)
		{
			this.canvas = canvas;
		}
	
		public boolean done()
		{
			return done;
		}
	
		@Override
		public void init(GLContext context)
		{
			// This is basically a temporary, NOTE not JoglesContext either
			JoglContext jctx = new JoglContext(context);
			GL2 gl = context.getGL().getGL2();
			//GL2ES2 gl = context.getGL().getGL2ES2();
			// Set up various properties
			if (getPropertiesFromCurrentContext(jctx, gl))
			{
				setupCanvasProperties(canvas, jctx, gl);
			}
			markDone();
		}
	
		private void markDone()
		{
			synchronized (this)
			{
				done = true;
				notifyAll();
			}
		}
	}*/

	//used by getBestConfiguration above
	/*private static void disposeOnEDT(final Frame f)
	{
		Runnable r = new Runnable() {
			@Override
			public void run()
			{
				f.setVisible(false);
				f.dispose();
			}
		};
		if (!EventQueue.isDispatchThread())
		{
			EventQueue.invokeLater(r);
		}
		else
		{
			r.run();
		}
	}*/

}
