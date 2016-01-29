package javax.media.j3d;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLContext;

public class JoglesContext extends JoglContext
{
	public GL2ES2 gl2es2;

	public JoglesContext(GLContext context)
	{
		super(context);
		gl2es2 = context.getGL().getGL2ES2();
	}

	/**
	 * This is for speed attempts, the getGL2ES2() checks intanceof so it's expensive
	 * @return
	 */
	public GL2ES2 gl2es2()
	{
		// Morrowind crashes on window size change but it's not this
		//if (context.getGL() == gl2es2)
		return gl2es2;
		//else
		//	return context.getGL().getGL2ES2();
	}

	public ArrayList<GeometryArrayRetained> geoToClearBuffers = new ArrayList<GeometryArrayRetained>();
	//Dirty dirty buffer gen holder thing

	public HashMap<GeometryArrayRetained, Integer> geoToIndBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, int[]> geoToIndStripBuf = new HashMap<GeometryArrayRetained, int[]>();

	public HashMap<GeometryArrayRetained, Integer> geoToCoordBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, Integer> geoToColorBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, Integer> geoToNormalBuf = new HashMap<GeometryArrayRetained, Integer>();

	public HashMap<GeometryArrayRetained, HashMap<Integer, Integer>> geoToTexCoordsBuf = new HashMap<GeometryArrayRetained, HashMap<Integer, Integer>>();

	public HashMap<Object, LocationData> geoToLocationData = new HashMap<Object, LocationData>();

	public HashMap<GeometryArrayRetained, HashMap<Integer, Integer>> geoToVertAttribBuf = new HashMap<GeometryArrayRetained, HashMap<Integer, Integer>>();

	public HashMap<Integer, HashMap<String, Integer>> progToGenVertAttNameToGenVertAttIndex = new HashMap<Integer, HashMap<String, Integer>>();

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

	//various ffp matrixes
	public Matrix4d currentModelMat = new Matrix4d();
	public Matrix4d currentViewMat = new Matrix4d();
	public Matrix4d currentModelViewMat = new Matrix4d();
	public Matrix4d currentModelViewMatInverse = new Matrix4d();
	public Matrix4d currentModelViewProjMat = new Matrix4d();
	public Matrix3d currentNormalMat = new Matrix3d();
	public Matrix4d currentProjMat = new Matrix4d();
	public Matrix4d currentProjMatInverse = new Matrix4d();

	public static class LocationData
	{

		public int glProjectionMatrix = -1;
		public int glProjectionMatrixInverse = -1;
		public int modelMatrix = -1;
		public int viewMatrix = -1;
		public int glModelViewMatrix = -1;
		public int glModelViewMatrixInverse = -1;
		public int glModelViewProjectionMatrix = -1;
		public int glNormalMatrix = -1;
		public int ignoreVertexColors = -1;
		public int glFrontMaterialdiffuse = -1;
		public int glFrontMaterialemission = -1;
		public int glFrontMaterialspecular = -1;
		public int glFrontMaterialshininess = -1;
		public int glLightModelambient = -1;
		public int objectColor = -1;
		public int glLightSource0position = -1;
		public int glLightSource0diffuse = -1;
		public int alphaTestEnabled = -1;
		public int alphaTestFunction = -1;
		public int alphaTestValue = -1;
		public int textureTransform = -1;
		public int glVertex = -1;
		public int glColor = -1;
		public int glNormal = -1;

		public int[] glMultiTexCoord;
		public HashMap<Integer, Integer> genAttIndexToLoc = new HashMap<Integer, Integer>();

	}

	//Performance issue
	//getGL2ES2  just made full screening morrowind crash (maybe tiny method which double checks
	// unbox,  getGL2ES2, context and get GL2ES2 are expensive! (a tiny bit)
	// possibly I can stop calling bind 0?
	// maybe no call to glFinish?
	// up to full screen and back improves render performance!!! what

	///For frame stats

	public static class PerFrameStats
	{
		//public HashSet<ShaderProgramId> usedPrograms = new HashSet<ShaderProgramId>();
		public ArrayList<ShaderProgramId> usedPrograms = new ArrayList<ShaderProgramId>();
		//public HashSet<String> usedProgramNames = new HashSet<String>();
		//TODO: how do I get these?
		//public HashMap<ShaderProgramId, String> usedProgramNames = new HashMap<ShaderProgramId, String>();

		public int geoToClearBuffers;
		public int glDrawArrays;
		public int glDrawElements;
		public int setFFPAttributes;
		public int geoToLocationData;
		public int enableTexCoordPointer;
		public int createGLSLShader;
		public int createGLSLShaderProgram;
		public int compileGLSLShader;
		public int destroyGLSLShader;
		public int destroyGLSLShaderProgram;
		public int linkGLSLShaderProgram;
		public int useGLSLShaderProgram;
		public int bindGLSLVertexAttrName;
		public int lookupGLSLShaderAttrNames;
		public int updateDirectionalLight;
		public int updatePointLight;
		public int updateSpotLight;
		public int updateExponentialFog;
		public int updateLinearFog;
		public int disableFog;
		public int setFogEnableFlag;
		public int updateLineAttributes;
		public int resetLineAttributes;
		public int updateMaterial;
		public int updateMaterialColor;
		public int updateColoringAttributes;
		public int resetColoringAttributes;
		public int updatePointAttributes;
		public int resetPointAttributes;
		public int updatePolygonAttributes;
		public int resetPolygonAttributes;
		public int updateRenderingAttributes;
		public int resetRenderingAttributes;
		public int updateTransparencyAttributes;
		public int resetTransparency;
		public int updateTextureAttributes;
		public int resetTextureAttributes;
		public int resetTexCoordGeneration;
		public int updateTextureUnitState;
		public int bindTexture2D;
		public int bindTextureCubeMap;
		public int setBlendColor;
		public int setBlendFunc;
		public int setFullSceneAntialiasing;
		public int setLightEnables;
		public int setSceneAmbient;
		public int activeTextureUnit;
		public int resetTextureNative;
		public int useCtx;
		public int releaseCtx;
		public int clear;
		public int setModelViewMatrix;
		public int setProjectionMatrix;
		public int setViewport;
		public int freeTexture;
		public int generateTexID;
		public int setDepthBufferWriteEnable;
		public int redundantUseProgram;

		public void outputPerFrameData()
		{

			System.out.println("geoToClearBuffers " + geoToClearBuffers);
			System.out.println("glDrawArrays " + glDrawArrays);
			System.out.println("glDrawElements " + glDrawElements);
			System.out.println("setFFPAttributes " + setFFPAttributes);
			System.out.println("geoToLocationData " + geoToLocationData);
			System.out.println("enableTexCoordPointer " + enableTexCoordPointer);
			System.out.println("createGLSLShader " + createGLSLShader);
			System.out.println("createGLSLShaderProgram " + createGLSLShaderProgram);
			System.out.println("compileGLSLShader " + compileGLSLShader);
			System.out.println("destroyGLSLShader " + destroyGLSLShader);
			System.out.println("destroyGLSLShaderProgram " + destroyGLSLShaderProgram);
			System.out.println("linkGLSLShaderProgram " + linkGLSLShaderProgram);
			System.out.println("bindGLSLVertexAttrName " + bindGLSLVertexAttrName);
			System.out.println("lookupGLSLShaderAttrNames " + lookupGLSLShaderAttrNames);
			System.out.println("updateDirectionalLight " + updateDirectionalLight);
			System.out.println("updatePointLight " + updatePointLight);
			System.out.println("updateSpotLight " + updateSpotLight);
			System.out.println("updateExponentialFog " + updateExponentialFog);
			System.out.println("updateLinearFog " + updateLinearFog);
			System.out.println("disableFog " + disableFog);
			System.out.println("setFogEnableFlag " + setFogEnableFlag);
			System.out.println("updateLineAttributes " + updateLineAttributes);
			System.out.println("resetLineAttributes " + resetLineAttributes);
			System.out.println("updateMaterial " + updateMaterial);
			System.out.println("updateMaterialColor " + updateMaterialColor);
			System.out.println("updateColoringAttributes " + updateColoringAttributes);
			System.out.println("resetColoringAttributes " + resetColoringAttributes);
			System.out.println("updatePointAttributes " + updatePointAttributes);
			System.out.println("resetPointAttributes " + resetPointAttributes);
			System.out.println("updatePolygonAttributes " + updatePolygonAttributes);
			System.out.println("resetPolygonAttributes " + resetPolygonAttributes);
			System.out.println("updateRenderingAttributes " + updateRenderingAttributes);
			System.out.println("resetRenderingAttributes " + resetRenderingAttributes);
			System.out.println("updateTransparencyAttributes " + updateTransparencyAttributes);
			System.out.println("resetTransparency " + resetTransparency);
			System.out.println("updateTextureAttributes " + updateTextureAttributes);
			System.out.println("resetTextureAttributes " + resetTextureAttributes);
			System.out.println("resetTexCoordGeneration " + resetTexCoordGeneration);
			System.out.println("updateTextureUnitState " + updateTextureUnitState);
			System.out.println("bindTexture2D " + bindTexture2D);
			System.out.println("bindTextureCubeMap " + bindTextureCubeMap);
			System.out.println("setBlendColor " + setBlendColor);
			System.out.println("setBlendFunc " + setBlendFunc);
			System.out.println("setFullSceneAntialiasing " + setFullSceneAntialiasing);
			System.out.println("setLightEnables " + setLightEnables);
			System.out.println("setSceneAmbient " + setSceneAmbient);
			System.out.println("activeTextureUnit " + activeTextureUnit);
			System.out.println("resetTextureNative " + resetTextureNative);
			System.out.println("useCtx " + useCtx);
			System.out.println("releaseCtx " + releaseCtx);
			System.out.println("clear " + clear);
			System.out.println("setModelViewMatrix " + setModelViewMatrix);
			System.out.println("setProjectionMatrix " + setProjectionMatrix);
			System.out.println("setViewport " + setViewport);
			System.out.println("freeTexture " + freeTexture);
			System.out.println("generateTexID " + generateTexID);
			System.out.println("setDepthBufferWriteEnable " + setDepthBufferWriteEnable);
			System.out.println("useGLSLShaderProgram " + useGLSLShaderProgram);
			System.out.println("redundantUseProgram " + redundantUseProgram);

			for (ShaderProgramId id : usedPrograms)
				System.out.println("ShaderProgramId " + ((JoglShaderObject) id).getValue() );

		}
	}

	public PerFrameStats perFrameStats = new PerFrameStats();

	private int statsFrame = 0;
	private int STATS_OUTPUT_FRAME_FREQ = 50;

	public void outputPerFrameData()
	{
		statsFrame++;
		if (statsFrame % STATS_OUTPUT_FRAME_FREQ == 0)
		{
			statsFrame = 0;
			System.out.println("======================================================");
			perFrameStats.outputPerFrameData();
		}
		// clear for next frame
		perFrameStats = new PerFrameStats();
	}

	// dirty check on what useProgram does
	public int prevProgramId;

}
