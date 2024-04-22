package org.jogamp.java3d;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

import compressedtexture.ASTCImage;
import compressedtexture.CompressedBufferedImage;
import compressedtexture.DDSImage;
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
		if (_byRefImage instanceof CompressedBufferedImage.ASTC)
		{
			ASTCImage astcImage = ((CompressedBufferedImage.ASTC) _byRefImage).astcImage;
			if (astcImage.hdr.blockdim_z == 1)
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
			}

			System.out.println("Bad ASTC format (for now) " + astcImage.hdr + " in " + _byRefImage.getImageName());
			return -1;
		}
		else if (_byRefImage instanceof CompressedBufferedImage.DDS)
		{
			DDSImage ddsImage = ((CompressedBufferedImage.DDS) _byRefImage).ddsImage;
			if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT1)
			{
				return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
			}
			else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT3)
			{
				return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
			}
			else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT5)
			{
				return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
			}
			else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_A8R8G8B8)
			{
				return GL2.GL_RGBA_S3TC;
			}
			else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_ATI2 || ddsImage.getPixelFormat() == DDSImage.D3DFMT_BC5U)
			{
				//seen in textures\shared\flatflat_n.dds
				// more info here https://www.panda3d.org/reference/cxx/texture_8cxx_source.html
				// normal with rg seems right
				//case 0x32495441:   // 'ATI2'
			    //case 0x55354342:   // 'BC5U'
			    //   compression = CM_rgtc;
			    //   func = read_dds_level_bc5;
			    //   format = F_rg;
			    //   break;
				//System.out.println("GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT image type, is this fallout4?");
				return GL2.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;
			}
			else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_R8G8B8 || //
					ddsImage.getPixelFormat() == DDSImage.D3DFMT_X8R8G8B8 || //
					ddsImage.getPixelFormat() == DDSImage.DDS_A16B16G16R16F)
			{
				//not yet supported
			}
			System.out.println("Bad DXT format (for now) " + ddsImage.getPixelFormat() + " in " + _byRefImage.getImageName());
			return -1;
		}
		else if (_byRefImage instanceof CompressedBufferedImage.KTX)
		{

			return ((CompressedBufferedImage.KTX) _byRefImage).ktxImage.headers.getGLInternalFormat();
		}

		return -1;
	}
}
