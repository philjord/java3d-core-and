package javax.media.j3d;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class JoglesIndexedTriangleStripArray extends IndexedTriangleStripArray
{

	public JoglesIndexedTriangleStripArray(int numVertices, int format, int texCoordCount, int[] texMap, int vertexAttrCount,
			int[] vertexAttrSizes, int length, int[] stripLengths)
	{
		super(numVertices, format, texCoordCount, texMap, vertexAttrCount, vertexAttrSizes, length, stripLengths);
	}

	public JoglesIndexedTriangleStripArray(int numVertices, int format, int texCoordCount, int[] texMap, int length, int[] stripLengths)
	{
		super(numVertices, format, texCoordCount, texMap, length, stripLengths);
	}

	/**
	* Creates the retained mode JoglesIndexedTriangleArrayRetained object that this
	* JoglesIndexedTriangleArray object will point to.
	*/
	@Override
	void createRetained()
	{
		this.retained = new JoglesIndexedTriangleStripArrayRetained();
		this.retained.setSource(this);
	}

	public void setCoordIndicesRefBuffer(ShortBuffer indBuf)
	{
		if (isLiveOrCompiled())
			if (!this.getCapability(ALLOW_REF_DATA_WRITE))
				throw new CapabilityNotSetException(J3dI18N.getString("GeometryArray86"));

		int format = ((IndexedGeometryArrayRetained) this.retained).vertexFormat;
		if ((format & BY_REFERENCE_INDICES) == 0)
			throw new IllegalStateException(J3dI18N.getString("IndexedGeometryArray32"));

		((JoglesIndexedTriangleStripArrayRetained) this.retained).setCoordIndicesRefBuffer(indBuf);
	}

	public void setInterleavedVertexBuffer(int interleavedStride, int geoToCoordOffset, int geoToColorsOffset, int geoToNormalsOffset,
			int[] geoToTexCoordOffset, int[] geoToVattrOffset, ByteBuffer interleavedBuffer, ByteBuffer coordBuffer)
	{
		((JoglesIndexedTriangleStripArrayRetained) this.retained).setInterleavedVertexBuffer(interleavedStride, geoToCoordOffset,
				geoToColorsOffset, geoToNormalsOffset, geoToTexCoordOffset, geoToVattrOffset, interleavedBuffer, coordBuffer);

	}

	/**
	 * terrible convenience
	 * @return
	 */
	public JoglesIndexedTriangleStripArrayRetained getRetained()
	{
		return ((JoglesIndexedTriangleStripArrayRetained) this.retained);
	}
}
