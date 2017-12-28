package org.jogamp.java3d;

import compressedtexture.CompressedBufferedImage;
import javaawt.image.RenderedImage;

/**
 * Class to get Compressed ByteBuffers handed to the pipeline along with a type that
 * gets them loaded onto the GPU as a compressed texture format
 * 
 * @author philip
 *
 */
public class CompressedImageComponent2D extends ImageComponent2D
{
	private static boolean byRef = true;

	private static boolean yUp = true;

	/**
	 * See CompressedTextureLoader for an example of how to use this class
	 * 
	 * Note ByRef and YUp are forced to true to ensure no image copies happen inside TextureRetained
	 * 
	 * @param format Only ImageComponent.FORMAT_RGBA supported
	 * @param image Only a DDSBufferedImage can be handed to DDSImageComponent2D
	 */
	public CompressedImageComponent2D(int format, RenderedImage image)
	{
		super(format, image, byRef, yUp);

		if (format != ImageComponent.FORMAT_RGBA)
			throw new UnsupportedOperationException("CompressedImageComponent2D only accepts ImageComponent.FORMAT_RGBA");

		if (!(image instanceof CompressedBufferedImage))
			throw new UnsupportedOperationException("CompressedImageComponent2D only accepts CompressedBufferedImage");

	}

	@Override
	void createRetained()
	{
		this.retained = new CompressedImageComponent2DRetained();
		this.retained.setSource(this);
	}

}
