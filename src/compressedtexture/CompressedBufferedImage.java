package compressedtexture;

import java.nio.ByteBuffer;
import java.util.Vector;

import javaawt.Graphics;
import javaawt.Graphics2D;
import javaawt.Point;
import javaawt.Rectangle;
import javaawt.image.BufferedImage;
import javaawt.image.ColorModel;
import javaawt.image.ImageObserver;
import javaawt.image.Raster;
import javaawt.image.RenderedImage;
import javaawt.image.SampleModel;
import javaawt.image.WritableRaster;

/**
 * This is NOT A bufferedImage! you can't use it as one. If you get UnsupportedOperationException then you 
 * are trying to use it as a BufferedImage, do not. You might need {@code DDSDecompressor}.
 * This class hides as a BufferedImage inside the Texture and ImageComponent worlds to be manipulated 
 * and finally arrive at the gl.glCompressedTexImage2D call in JoglPipeline.updateTexture2DImage.
 * It is what the DDSImageComponent2D constructor requires.
 */
public class CompressedBufferedImage extends BufferedImage
{

	protected ByteBuffer buffer;

	protected int width;

	protected int height;

	protected String imageName = "";

	/**
	 * see {@code DDSTextureLoader} for example usage
	 * @param ddsImage
	 * @param mipNumber
	 * @param imageName
	 */
	public CompressedBufferedImage()
	{
		// minimal impact BufferedImage constructor
		super(1, 1, BufferedImage.TYPE_INT_ARGB);

	}

	public String getImageName()
	{
		return imageName;
	}

	/**
	 * Called by @see DDSImageComponent2DRetained.ImageData2
	 * @return
	 */
	public ByteBuffer getBuffer()
	{
		return buffer;
	}

	//Below are BufferedImage methods********
	/**
	 * This is called by various parts of Java3D
	 * but DXT are a fixed type
	 */
	@Override
	public int getType()
	{
		return BufferedImage.TYPE_INT_ARGB;
	}

	@Override
	public int getWidth()
	{
		return width;
	}

	@Override
	public int getHeight()
	{
		return height;
	}

	@Override
	public int getWidth(ImageObserver observer)
	{
		return width;
	}

	@Override
	public int getHeight(ImageObserver observer)
	{
		return height;
	}

	//Below are Unsupported BufferedImage methods********
	@Override
	public ColorModel getColorModel()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public SampleModel getSampleModel()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMinX()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMinY()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumXTiles()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumYTiles()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMinTileX()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMinTileY()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Vector<RenderedImage> getSources()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getProperty(String name)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getPropertyNames()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public BufferedImage getSubimage(int x, int y, int w, int h)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTileWidth()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTileHeight()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTileGridXOffset()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTileGridYOffset()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Raster getTile(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Raster getData()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Raster getData(Rectangle rect)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public WritableRaster copyData(WritableRaster raster)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public WritableRaster getAlphaRaster()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRGB(int x, int y)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void setRGB(int x, int y, int rgb)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Graphics getGraphics()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Graphics2D createGraphics()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAlphaPremultiplied()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void coerceData(boolean isAlphaPremultiplied)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setData(Raster r)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTileWritable(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Point[] getWritableTileIndices()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasTileWriters()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public WritableRaster getWritableTile(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void releaseWritableTile(int tileX, int tileY)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTransparency()
	{
		throw new UnsupportedOperationException();
	}
	
	
	public static class KTX extends CompressedBufferedImage
	{
		public KTXImage ktxImage;

		/**
		 * see {@code DDSTextureLoader} for example usage
		 * @param ddsImage
		 * @param mipNumber
		 * @param imageName
		 */
		public KTX(KTXImage ktxImage, int mipmapLevel, String imageName)
		{
			this.ktxImage = ktxImage;
			this.buffer = ktxImage.textureData.get(mipmapLevel);
			this.imageName = imageName;
			this.width = ktxImage.textureData.getWidth(mipmapLevel);
			this.height = ktxImage.textureData.getHeight(mipmapLevel);

			if (width < 1 || height < 1)
			{
				throw new IllegalArgumentException("Height or Width == 0");
			}

			// assume all values that get here are gonna be fine

		}

	}
	
	public static class DDS extends CompressedBufferedImage
	{
		public DDSImage ddsImage;

		private DDSImage.ImageInfo imageInfo;

		/**
		 * see {@code DDSTextureLoader} for example usage
		 * @param ddsImage
		 * @param mipNumber
		 * @param imageName
		 */
		public DDS(DDSImage ddsImage, int mipNumber, String imageName)
		{
			this.ddsImage = ddsImage;
			this.imageInfo = ddsImage.getAllMipMaps()[mipNumber];
			this.imageName = imageName;
			this.width = imageInfo.getWidth();
			this.height = imageInfo.getHeight();

			if (width < 1 || height < 1)
			{
				throw new IllegalArgumentException("Height or Width == 0");
			}
			else
			{

				if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT2)
				{
					System.out.println("DXT2 not supported; " + imageName + "; mip num = " + mipNumber);
				}
				else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT4)
				{
					System.out.println("DXT4 not supported; " + imageName + "; mip num = " + mipNumber);
				}
				else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_UNKNOWN)
				{
					System.out.println("D3DFMT_UNKNOWN not supported; " + imageName + "; mip num = " + mipNumber);
				}
				else if (ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT1 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT3 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_DXT5 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_R8G8B8 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_A8R8G8B8 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_A8B8G8R8 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_X8R8G8B8 || //
						ddsImage.getPixelFormat() == DDSImage.DDS_A16B16G16R16F || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_ATI2 || //
						ddsImage.getPixelFormat() == DDSImage.D3DFMT_BC5U)
				{
					//good
				}
				else
				{
					System.out.println("not DDS format; " + ddsImage.getPixelFormat() + "; " + imageName + "; mip num = " + mipNumber);
				}
			}
			this.buffer = imageInfo.getData();
		}
	}
	
	public static class ASTC extends CompressedBufferedImage
	{

		public ASTCImage astcImage;

		/**
		 * see {@code DDSTextureLoader} for example usage
		 * @param ddsImage
		 * @param mipNumber likely to always be 0
		 * @param imageName
		 */
		public ASTC(ASTCImage astcImage, int mipNumber, String imageName)
		{
			this.astcImage = astcImage;

			this.imageName = imageName;
			this.width = astcImage.getWidth();
			this.height = astcImage.getHeight();

			if (width < 1 || height < 1)
			{
				throw new IllegalArgumentException("Height or Width == 0");
			}
			else
			{
				// we assume all is well in all cases, how could it hurt?
			}
			this.buffer = astcImage.byteBuffer;
		}

	}
}
