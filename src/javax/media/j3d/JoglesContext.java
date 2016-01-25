package javax.media.j3d;

import java.util.HashMap;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;

import com.jogamp.opengl.GLContext;

public class JoglesContext extends JoglContext
{

	//Dirty dirty buffer gen holder thing

	public HashMap<GeometryArrayRetained, Integer> geoToIndBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, int[]> geoToIndStripBuf = new HashMap<GeometryArrayRetained, int[]>();

	public HashMap<GeometryArrayRetained, Integer> geoToCoordBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, Integer> geoToColorBuf = new HashMap<GeometryArrayRetained, Integer>();

	// note anything may be reused if not updated between execute calls

	//Light data recorded to be handed into shader as uniform on next update
	//see https://www.opengl.org/sdk/docs/man2/ glLight
	// for usage details
	public static class LightData
	{
		public float[] ambient = new float[4];
		public float[] diffuse = new float[4];
		public float[] specular = new float[4];
		public float[] pos = new float[4];
		public float[] spotDir = new float[4];
		public float GL_CONSTANT_ATTENUATION;
		public float GL_LINEAR_ATTENUATION;
		public float GL_QUADRATIC_ATTENUATION;
		public float GL_SPOT_EXPONENT;
		public float GL_SPOT_CUTOFF;
	}

	public LightData[] dirLight = new LightData[8];
	public LightData[] pointLight = new LightData[8];
	public LightData[] spotLight = new LightData[8];

	public static class FogData
	{
		public boolean enable = false;
		public float[] expColor = new float[3];
		public float expDensity = 0;
		public float[] linearColor = new float[3];
		public float linearStart = 0;
		public float linearEnd = 0;
	}

	public FogData fogData = new FogData();

	public static class MaterialData
	{
		public boolean lightEnabled = true;
		public float[] emission = new float[3];
		public float[] ambient = new float[3];
		public float[] specular = new float[3];
		public float[] diffuse = new float[4];
		public float shininess;
	}

	public MaterialData materialData = new MaterialData();
	
	//See here http://download.java.net/media/java3d/javadoc/1.3.2/javax/media/j3d/RenderingAttributes.html
	// For coloring implementation details
	
	//only for no lighting, materialDiffuse or vertex colors otherwise
	public float[] objectColor = new float[4];

	public float pointSize = 0;

	public int polygonMode = PolygonAttributes.POLYGON_FILL;

	public static class RenderingData
	{
		public boolean alphaTestEnabled = false;
		public int alphaTestFunction = RenderingAttributes.ALWAYS;
		public float alphaTestValue = 0;
		public boolean ignoreVertexColors;
	}

	public RenderingData renderingData = new RenderingData();

	// should use getMaximumLights() in pipeline
	public boolean[] enabledLights = new boolean[8];

	public float[] currentAmbientColor = new float[4];

	public Matrix4d textureTransform = new Matrix4d();

	//current ModelView Matrix for use in execute
	public Matrix4d currentModelViewMat = new Matrix4d();
	public Matrix4d currentModelViewProjMat = new Matrix4d();
	//current Normal Matrix for use in execute
	public Matrix3d currentNormalMat = new Matrix3d();

	//current Projection Matrix for use in execute
	public Matrix4d currentProjMat = new Matrix4d();
	public Matrix4d currentProjMatInverse = new Matrix4d();

	JoglesContext(GLContext context)
	{
		super(context);
	}

}
