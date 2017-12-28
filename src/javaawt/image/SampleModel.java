package javaawt.image;

public abstract class SampleModel
{
	public abstract Object getDelegate();
	
	public int getNumBands()
	{
		throw new UnsupportedOperationException();
	}

	public int getDataType()
	{
		throw new UnsupportedOperationException();
	}

}
