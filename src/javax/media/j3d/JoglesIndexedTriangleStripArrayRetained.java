package javax.media.j3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class JoglesIndexedTriangleStripArrayRetained extends IndexedTriangleStripArrayRetained
{

	private static FloatBuffer dummyBuffer = FloatBuffer.allocate(0);
	ShortBuffer indBuf;

	int interleavedStride = -1;
	int geoToCoordOffset = -1;
	int geoToColorsOffset = -1;
	int geoToNormalsOffset = -1;
	int[] geoToTexCoordOffset = new int[1];
	int[] geoToVattrOffset = new int[2];

	ByteBuffer interleavedBuffer;
	ByteBuffer coordBuffer;// if not null coords are separate, ready for animation

	private int vdefined = 0;

	final void setCoordIndicesRefBuffer(ShortBuffer indBuf)
	{
		int newMax = 0;

		if (indBuf != null)
		{
			if (indBuf.limit() < validIndexCount)
			{
				throw new IllegalArgumentException(J3dI18N.getString("IndexedGeometryArray33"));
			}

			for (int i = 0; i < validIndexCount; i++)
			{
				// Throw an exception, since index is negative
				if (indBuf.get(i) < 0)
					throw new ArrayIndexOutOfBoundsException(J3dI18N.getString("IndexedGeometryArray27"));
				if (indBuf.get(i) > newMax)
				{
					newMax = indBuf.get(i);
				}
			}

			if (newMax > maxCoordIndex)
			{
				doErrorCheck(newMax);
			}
		}

		if ((vertexFormat & GeometryArray.USE_COORD_INDEX_ONLY) != 0)
		{
			if ((vertexFormat & GeometryArray.COLOR) != 0)
			{
				maxColorIndex = newMax;
			}
			if ((vertexFormat & GeometryArray.TEXTURE_COORDINATE) != 0)
			{
				for (int i = 0; i < texCoordSetCount; i++)
				{
					maxTexCoordIndices[i] = newMax;
				}
			}
			if ((vertexFormat & GeometryArray.VERTEX_ATTRIBUTES) != 0)
			{
				for (int i = 0; i < vertexAttrCount; i++)
				{
					maxVertexAttrIndices[i] = newMax;
				}
			}
			if ((vertexFormat & GeometryArray.NORMALS) != 0)
			{
				maxNormalIndex = newMax;
			}
		}

		boolean isLive = source != null && source.isLive();
		if (isLive)
		{
			geomLock.getLock();
		}
		dirtyFlag |= INDEX_CHANGED;
		maxCoordIndex = newMax;
		this.indBuf = indBuf;
		if (isLive)
		{
			geomLock.unLock();
		}
		if (!inUpdater && isLive)
		{
			sendDataChangedMessage(true);
		}
	}

	public void setInterleavedVertexBuffer(int interleavedStride, int geoToCoordOffset, int geoToColorsOffset, int geoToNormalsOffset,
			int[] geoToTexCoordOffset, int[] geoToVattrOffset, ByteBuffer interleavedBuffer, ByteBuffer coordBuffer)
	{
		this.interleavedStride = interleavedStride;
		this.geoToCoordOffset = geoToCoordOffset;
		this.geoToColorsOffset = geoToColorsOffset;
		this.geoToNormalsOffset = geoToNormalsOffset;
		this.geoToTexCoordOffset = geoToTexCoordOffset;
		this.geoToVattrOffset = geoToVattrOffset;

		this.interleavedBuffer = interleavedBuffer;
		this.coordBuffer = coordBuffer;

		// could use vertex format here, but offsets are just as good

		vertexType |= VERTEX_DEFINED;
		vdefined |= COORD_FLOAT;

		if (geoToColorsOffset != -1)
		{
			vertexType |= COLOR_DEFINED;
			vdefined |= COLOR_FLOAT;
		}
		if (geoToNormalsOffset != -1)
		{
			vertexType |= NORMAL_DEFINED;
			vdefined |= NORMAL_FLOAT;
		}
		if (geoToVattrOffset.length > 0 && geoToVattrOffset[0] > 0)
		{
			vertexType |= VATTR_DEFINED;
			vdefined |= VATTR_FLOAT;
		}
		if (geoToTexCoordOffset.length > 0 && geoToTexCoordOffset[0] > 0)
		{
			vertexType |= TEXCOORD_DEFINED;
			vdefined |= TEXCOORD_FLOAT;
		}

	}

	@Override
	void execute(Canvas3D cv, RenderAtom ra, boolean isNonUniformScale, boolean updateAlpha, float alpha, int screen,
			boolean ignoreVertexColors)
	{

		//FIXME: PJPJPJ big ugly hack for buffers
		if (cv.ctx != prevContext)
		{
			ctxExecutedOn.add(cv.ctx);
			prevContext = cv.ctx;
		}

		int cdirty = 0;// what is this for? it was color  based weirdness

		//Notice a check for having vcoords and cdatabuffer in this pipeline call, but they are not used in teh 
		//subsequent optimized call, so dummies are sent through to leave the interface alone

		Pipeline.getPipeline().executeIndexedGeometryVABuffer(cv.ctx, this, geoType, isNonUniformScale, ignoreVertexColors,
				initialIndexIndex, validIndexCount, maxCoordIndex + 1, (vertexFormat | c4fAllocated), vdefined, dummyBuffer, dummyBuffer,
				null, null, null, vertexAttrCount, vertexAttrSizes, floatBufferRefVertexAttrs,
				((texCoordSetMap == null) ? 0 : texCoordSetMap.length), texCoordSetMap, cv.numActiveTexUnit, texCoordStride, refTexCoords,
				cdirty, indexCoord);

	}

	@Override
	void computeBoundingBox()
	{

		double xmin, xmax, ymin, ymax, zmin, zmax;

		synchronized (geoBounds)
		{
			// If autobounds compute is false  then return
			if ((computeGeoBounds == 0) && (refCount > 0))
			{
				return;
			}

			if (!boundsDirty)
				return;

			int sIndex = initialCoordIndex;

			// Compute the bounding box
			xmin = ymin = zmin = Integer.MAX_VALUE;
			xmax = ymax = zmax = Integer.MIN_VALUE;

			for (int vi = sIndex; vi < validVertexCount; vi++)
			{
				int i = vi * interleavedStride + geoToCoordOffset;
				interleavedBuffer.position(i);
				ShortBuffer sb = interleavedBuffer.asShortBuffer();

				float xf = JoglesMatrixUtil.halfToFloat(sb.get(0));
				float yf = JoglesMatrixUtil.halfToFloat(sb.get(1));
				float zf = JoglesMatrixUtil.halfToFloat(sb.get(2));

				if (xf > xmax)
					xmax = xf;
				if (xf < xmin)
					xmin = xf;

				if (yf > ymax)
					ymax = yf;
				if (yf < ymin)
					ymin = yf;

				if (zf > zmax)
					zmax = zf;
				if (zf < zmin)
					zmin = zf;

			}
			geoBounds.setUpper(xmax, ymax, zmax);
			geoBounds.setLower(xmin, ymin, zmin);
			boundsDirty = false;
		}
	}
}
