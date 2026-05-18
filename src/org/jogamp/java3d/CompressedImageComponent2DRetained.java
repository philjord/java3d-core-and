package org.jogamp.java3d;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import compressedtexture.ASTCImage;
import compressedtexture.CompressedBufferedImage;
import compressedtexture.DDSImage;
import compressedtexture.KTXImage;
import javaawt.image.RenderedImage;

/**
 * Stealth class to get DXT ByteBuffers handed to the pipeline along with a type that
 * gets them loaded compressed
 * @author philip
 *
 */
public class CompressedImageComponent2DRetained extends ImageComponent2DRetained
{
	protected CompressedBufferedImage _byRefImage;

	public CompressedImageComponent2DRetained()
	{
	}

	@Override
	ImageData createRenderedImageDataObject(RenderedImage byRefImage)
	{
		if (byRefImage instanceof CompressedBufferedImage)
		{
			this._byRefImage = (CompressedBufferedImage) byRefImage;
			return new CompressedImageData(ImageDataType.TYPE_BYTE_BUFFER, width, height, _byRefImage);
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}

	class CompressedImageData extends ImageData
	{
		private CompressedBufferedImage bi;

		private ImageDataType imageDataType;

		private int dataWidth, dataHeight;

		CompressedImageData(ImageDataType imageDataType, int dataWidth, int dataHeight, CompressedBufferedImage byRefImage)
		{
			// no impact super constructor
			super(imageDataType, 0, 0, 0);
			this.imageDataType = imageDataType;
			this.dataWidth = dataWidth;
			this.dataHeight = dataHeight;
			bi = byRefImage;
		}

		/**
		* Returns the type of this DataBuffer.
		*/
		@Override
		ImageDataType getType()
		{
			return imageDataType;
		}

		/**
		 * Returns the width of this DataBuffer.
		 */
		@Override
		int getWidth()
		{
			return dataWidth;
		}

		/**
		 * Returns the height of this DataBuffer.
		 */
		@Override
		int getHeight()
		{
			return dataHeight;
		}

		/**
		* Returns is this data is byRef. No internal data is made.
		*/
		@Override
		boolean isDataByRef()
		{
			return true;
		}

		/**
		 * Returns this DataBuffer as an Object.
		 */
		@Override
		Object get()
		{
			return bi.getBuffer();
		}

		@Override
		int length()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		byte[] getAsByteArray()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		int[] getAsIntArray()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		ByteBuffer getAsByteBuffer()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		IntBuffer getAsIntBuffer()
		{
			throw new UnsupportedOperationException();
		}
	}

	@Override
	ImageData createRenderedImageDataObject(RenderedImage byRefImage, int dataWidth, int dataHeight)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	ImageData createNioImageBufferDataObject(NioImageBuffer nioImageBuffer)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	boolean isImageTypeSupported(NioImageBuffer nioImgBuf)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	void createBlankImageData()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Note this does NOT return a ImageComponentRetained enum value
	 * But the value returned does find its way into the pipeline and cause the compressed image load call.
	 * @param powerOfTwoData
	 * @return
	 */
	@Override
	int getImageFormatTypeIntValue(boolean powerOfTwoData)
	{
		// - as of jogl 2.6 ASTC removed from the GL2ES3 header https://community.khronos.org/t/astc-is-dead-for-now/105056/2
		
		if (_byRefImage instanceof CompressedBufferedImage.ASTC)
		{		
			ASTCImage astcImage = ((CompressedBufferedImage.ASTC) _byRefImage).astcImage;
			/*if (astcImage.hdr.blockdim_z == 1)
			{
				if (astcImage.hdr.blockdim_x == 4)
				{
					if (astcImage.hdr.blockdim_y == 4)
						return GL3.GL_COMPRESSED_RGBA_ASTC_4x4_KHR;
				}
				else if (astcImage.hdr.blockdim_x == 5)
				{
					if (astcImage.hdr.blockdim_y == 4)
						return GL3.GL_COMPRESSED_RGBA_ASTC_5x4_KHR;
					else if (astcImage.hdr.blockdim_y == 5)
						return GL3.GL_COMPRESSED_RGBA_ASTC_5x5_KHR;
				}
				else if (astcImage.hdr.blockdim_x == 6)
				{
					if (astcImage.hdr.blockdim_y == 5)
						return GL3.GL_COMPRESSED_RGBA_ASTC_6x5_KHR;
					else if (astcImage.hdr.blockdim_y == 6)
						return GL3.GL_COMPRESSED_RGBA_ASTC_6x6_KHR;
				}
				else if (astcImage.hdr.blockdim_x == 8)
				{
					if (astcImage.hdr.blockdim_y == 5)
						return GL3.GL_COMPRESSED_RGBA_ASTC_8x5_KHR;
					else if (astcImage.hdr.blockdim_y == 6)
						return GL3.GL_COMPRESSED_RGBA_ASTC_8x6_KHR;
					else if (astcImage.hdr.blockdim_y == 8)
						return GL3.GL_COMPRESSED_RGBA_ASTC_8x8_KHR;
				}
				else if (astcImage.hdr.blockdim_x == 10)
				{
					if (astcImage.hdr.blockdim_y == 5)
						return GL3.GL_COMPRESSED_RGBA_ASTC_10x5_KHR;
					else if (astcImage.hdr.blockdim_y == 6)
						return GL3.GL_COMPRESSED_RGBA_ASTC_10x6_KHR;
					else if (astcImage.hdr.blockdim_y == 8)
						return GL3.GL_COMPRESSED_RGBA_ASTC_10x8_KHR;
					else if (astcImage.hdr.blockdim_y == 10)
						return GL3.GL_COMPRESSED_RGBA_ASTC_10x10_KHR;
				}
				else if (astcImage.hdr.blockdim_x == 12)
				{
					if (astcImage.hdr.blockdim_y == 10)
						return GL3.GL_COMPRESSED_RGBA_ASTC_12x10_KHR;
					else if (astcImage.hdr.blockdim_y == 12)
						return GL3.GL_COMPRESSED_RGBA_ASTC_12x12_KHR;
				}
			}*/

			System.out.println("Bad ASTC format (for now) " + astcImage.hdr + " in " + _byRefImage.getImageName());
			return -1;
		} else if (_byRefImage instanceof CompressedBufferedImage.DDS) {
			DDSImage ddsImage = ((CompressedBufferedImage.DDS)_byRefImage).ddsImage;
			int glInternalFormat = ddsImage.getGLInternalFormat();
			if (glInternalFormat != -1) {
				return glInternalFormat;
			}
			System.out.println("Bad DXT format (for now) "	+ ddsImage.getPixelFormat() + " GL=" + glInternalFormat
								+ " in " + _byRefImage.getImageName());
			return -1;
		} else if (_byRefImage instanceof CompressedBufferedImage.KTX) {
			KTXImage ktxImage = ((CompressedBufferedImage.KTX)_byRefImage).ktxImage;
			int glInternalFormat = ktxImage.headers.getGLInternalFormat();
			if (glInternalFormat != -1) {
				return glInternalFormat;
			}
			System.out.println("Bad KTX format (for now) GL=" + glInternalFormat + " in " + _byRefImage.getImageName());
			return -1;
		}

		return -1;
	}
}
