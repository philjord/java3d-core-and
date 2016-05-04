package javax.media.j3d;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class JoglesIndexedTriangleArrayRetained extends IndexedTriangleArrayRetained
{
	public ShortBuffer indBuf;

	public int geoToCoordOffset;
	public int interleavedStride;
	public int geoToColorsOffset;
	public int geoToNormalsOffset;
	public int[] geoToTexCoordOffset = new int[1];
	public int[] geoToVattrOffset = new int[2];

	public ByteBuffer interleavedBuffer;

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
		this.indexCoord = new int[0];// execute has a null check
		if (isLive)
		{
			geomLock.unLock();
		}
		if (!inUpdater && isLive)
		{
			sendDataChangedMessage(true);
		}
	}

	public void setInterleavedVertexBuffer(int geoToCoordOffset, int interleavedStride, int geoToColorsOffset, int geoToNormalsOffset,
			int[] geoToTexCoordOffset, int[] geoToVattrOffset, ByteBuffer interleavedBuffer)
	{
		this.geoToCoordOffset = geoToCoordOffset;
		this.interleavedStride = interleavedStride;
		this.geoToColorsOffset = geoToColorsOffset;
		this.geoToNormalsOffset = geoToNormalsOffset;
		this.geoToTexCoordOffset = geoToTexCoordOffset;
		this.geoToVattrOffset = geoToVattrOffset;

		this.interleavedBuffer = interleavedBuffer;

	}

}
