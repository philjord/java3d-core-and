package org.jogamp.java3d;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class JoglesIndexedTriangleArray extends IndexedTriangleArray
{

	public JoglesIndexedTriangleArray(int numVertices, int format, int texCoordCount, int[] texMap, int numTrianglePoints)
	{
		super(numVertices, format, texCoordCount, texMap, numTrianglePoints);
	}

	public JoglesIndexedTriangleArray(int numVertices, int format, int texCoordCount, int[] texMap, int vertexAttrCount,
			int[] vertexAttrSizes, int numTrianglePoints)
	{
		super(numVertices, format, texCoordCount, texMap, vertexAttrCount, vertexAttrSizes, numTrianglePoints);
	}

	/**
	* Creates the retained mode JoglesIndexedTriangleArrayRetained object that this
	* JoglesIndexedTriangleArray object will point to.
	*/
	@Override
	void createRetained()
	{
		this.retained = new JoglesIndexedTriangleArrayRetained();
		this.retained.setSource(this);
	}

	public void setCoordIndicesRefBuffer(IntBuffer indBuf)
	{
		if (isLiveOrCompiled())
			if (!this.getCapability(ALLOW_REF_DATA_WRITE))
				throw new CapabilityNotSetException(J3dI18N.getString("GeometryArray86"));

		int format = ((IndexedGeometryArrayRetained) this.retained).vertexFormat;
		if ((format & BY_REFERENCE_INDICES) == 0)
			throw new IllegalStateException(J3dI18N.getString("IndexedGeometryArray32"));

		((JoglesIndexedTriangleArrayRetained) this.retained).setCoordIndicesRefBuffer(indBuf);
	}

	public void setInterleavedVertexBuffer(int interleavedStride, int geoToCoordOffset, int geoToColorsOffset, int geoToNormalsOffset,
			int[] geoToTexCoordOffset, int[] geoToVattrOffset, ByteBuffer interleavedBuffer, ByteBuffer coordBuffer)
	{
		((JoglesIndexedTriangleArrayRetained) this.retained).setInterleavedVertexBuffer(interleavedStride, geoToCoordOffset,
				geoToColorsOffset, geoToNormalsOffset, geoToTexCoordOffset, geoToVattrOffset, interleavedBuffer, coordBuffer);

	}

	/**
	 * terrible convenience
	 * @return
	 */
	public JoglesIndexedTriangleArrayRetained getRetained()
	{
		return ((JoglesIndexedTriangleArrayRetained) this.retained);
	}
}
