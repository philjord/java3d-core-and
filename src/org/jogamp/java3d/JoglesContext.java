package org.jogamp.java3d;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.jogamp.vecmath.Matrix3d;
import org.jogamp.vecmath.Matrix4d;
import org.jogamp.vecmath.Vector3f;
import org.jogamp.vecmath.Vector4f;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLContext;

import utils.SparseArray;

public class JoglesContext extends JoglContext
{
	
	//NOTES to convert to core


	// Step to convert
	// 1. replace all SpareArray< with HashMap<Integer, JoglesContext and JoglesPipeline
	// 2. GeometryArrayRetained must add context tracking for buffers
	// 3. remove all blocks relating to ATTEMPT_UBO
	// 4. remove all calls relating to ATTEMPT_OPTIMIZED_VERTICES
	// 5. swap tcBufIds.keyAt(i) for a key foreach of hashmap vaBufIds 
	// 6. JoglContext ctx = new JoglContext(glContext); in createNewContext becomes JoglesContext ctx = new JoglesContext(glContext);
	// 7. and again for createQueryContext
	// 8. rename jogles* to jogl2es2* and delete the optomized triangle array 
	// 9. (((GeometryArray) geo.source).capabilityBits & (1L << GeometryArray.ALLOW_REF_DATA_WRITE)) != 0L becomes  geo.source.getCapability(GeometryArray.ALLOW_REF_DATA_WRITE)
	// 10. update Pipeline to include the new type GL2ES2
	// 11. update MasterControl to include jogl2es2 pipeline 
	// 12. swap the use of teh gl2es2 and gl2es3 variables in context to getters and re-obtain each time
	//
	
	
	//TODO: heaps of lights appears to kill performance, why?

	//pre-casting for speed
	public GL2ES2 gl2es2 = null;
	public GL2ES3 gl2es3 = null;

	public JoglesContext(GLContext context)
	{
		super(context);
		gl2es2 = context.getGL().getGL2ES2();
		if (context.getGL().isGL2ES3())
			gl2es3 = (GL2ES3) context.getGL();
	}

	public JoglShaderObject shaderProgram;
	public int shaderProgramId = -1;
	public ProgramData programData;

	@Override
	void setShaderProgram(JoglShaderObject object)
	{
		super.setShaderProgram(object);
		shaderProgram = object;
		shaderProgramId = object == null ? -1 : object.getValue();
		programData = allProgramData.get(shaderProgramId);
		if (programData == null)
		{
			programData = new ProgramData();
			allProgramData.put(shaderProgramId, programData);
		}

	}

	// all buffers created are recorded for each render pass, and for cleanup
	public ArrayList<GeometryArrayRetained> geoToClearBuffers = new ArrayList<GeometryArrayRetained>();

	public SparseArray<GeometryData> allGeometryData = new SparseArray<GeometryData>();

	public static class GeometryData
	{
		public int nativeId = -1;

		public int geoToIndBuf = -1;
		public int geoToIndBufSize = -1;
		public int[] geoToIndStripBuf = null;
		//public int geoToIndStripSwappedSize = -1;// removed into j3dnotristrips
		public int geoToCoordBuf = -1;
		public int geoToCoordBuf1 = -2;// double buffered for updates
		public int geoToCoordBuf2 = -3;
		public int geoToCoordBuf3 = -3;
		public int geoToCoordBufSize = -1;
		public int geoToColorBuf = -1;
		public int geoToNormalBuf = -1;
		public SparseArray<Integer> geoToTexCoordsBuf = new SparseArray<Integer>();
		public SparseArray<Integer> geoToVertAttribBuf = new SparseArray<Integer>();

		//Every thing below relates to interleaved data
		public int coordBufId = -1; // if separate
		public int interleavedBufId = -1;
		public int interleavedStride = 0;
		public int geoToCoordOffset = -1;
		public int geoToColorsOffset = -1;
		public int geoToNormalsOffset = -1;
		public int[] geoToVattrOffset = new int[10];
		public int[] geoToTexCoordOffset = new int[10];

		// vertex array object id for this geom
		public int vaoId = -1;

		//used to identify each geometry as we see it
		private static int nextNativeId = 0;

		public GeometryData()
		{
			nativeId = nextNativeId++;
			nextNativeId = nextNativeId > Integer.MAX_VALUE - 10 ? 0 : nextNativeId;// desperate loop
		}

	}

	public SparseArray<ProgramData> allProgramData = new SparseArray<ProgramData>();

	public static class ProgramData
	{
		public HashMap<String, Integer> progToGenVertAttNameToGenVertAttIndex = new HashMap<String, Integer>();
		public LocationData programToLocationData = null;// null to indicate need to load
		public ByteBuffer programToUBOBB = null;
		public int programToUBOBuf = -1;
	}

	//Light data recorded to be handed into shader as uniform on next update
	//see https://www.opengl.org/sdk/docs/man2/ glLight
	// for usage details
	public static class LightData
	{
		public Vector4f ambient = new Vector4f();
		public Vector4f diffuse = new Vector4f();
		public Vector4f specular = new Vector4f();
		public Vector4f pos = new Vector4f();
		public Vector4f spotDir = new Vector4f();
		public float GL_CONSTANT_ATTENUATION;
		public float GL_LINEAR_ATTENUATION;
		public float GL_QUADRATIC_ATTENUATION;
		public float GL_SPOT_EXPONENT;
		public float GL_SPOT_CUTOFF;
	}

	// should use getMaximumLights() in pipeline? though it's up to the shader
	public static int MAX_LIGHTS = 8;
	public LightData[] dirLight = new LightData[MAX_LIGHTS];
	public LightData[] pointLight = new LightData[MAX_LIGHTS];
	public LightData[] spotLight = new LightData[MAX_LIGHTS];

	public static class FogData
	{
		public boolean enable = false;
		public Vector3f expColor = new Vector3f();
		public float expDensity = 0;
		public Vector3f linearColor = new Vector3f();
		public float linearStart = 0;
		public float linearEnd = 0;
	}

	public FogData fogData = new FogData();

	public static class MaterialData
	{
		public boolean lightEnabled = true;
		public Vector3f emission = new Vector3f();
		public Vector3f ambient = new Vector3f();
		public Vector3f specular = new Vector3f();
		public Vector4f diffuse = new Vector4f();
		public float shininess;
	}

	public MaterialData materialData = new MaterialData();

	//See here http://download.java.net/media/java3d/javadoc/1.3.2/javax/media/j3d/RenderingAttributes.html
	// For coloring implementation details

	//only for no lighting, materialDiffuse or vertex colors otherwise
	public Vector4f objectColor = new Vector4f();

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

	public boolean[] enabledLights = new boolean[MAX_LIGHTS];

	public Vector4f currentAmbientColor = new Vector4f();

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

	/**
	 * On shader creation the various FFP locations are discovered and recorded for use later
	 * @author phil
	 *
	 */

	//TODO: maybe many bufers allows buffersubdata to run faster 
	public int globalUboBufId = -1; // the one buffer is bound once then reused, dear god

	public static class LocationData
	{
		//UBO data
		public int uboBufId = -1;
		public int blockIndex = -1;
		public int blockSize = -1;
		public int glProjectionMatrixOffset = -1;
		public int glProjectionMatrixInverseOffset = -1;
		public int glViewMatrixOffset = -1;
		public int glModelMatrixOffset = -1;
		public int glModelViewMatrixOffset = -1;
		public int glModelViewMatrixInverseOffset = -1;
		public int glModelViewProjectionMatrixOffset = -1;
		public int glNormalMatrixOffset = -1;
		public int glFrontMaterialdiffuseOffset = -1;
		public int glFrontMaterialemissionOffset = -1;
		public int glFrontMaterialspecularOffset = -1;
		public int glFrontMaterialshininessOffset = -1;
		public int ignoreVertexColorsOffset = -1;
		public int glLightModelambientOffset = -1;
		public int objectColorOffset = -1;
		public int glLightSource0positionOffset = -1;
		public int glLightSource0diffuseOffset = -1;
		public int textureTransformOffset = -1;
		public int alphaTestEnabledOffset = -1;
		public int alphaTestFunctionOffset = -1;
		public int alphaTestValueOffset = -1;

		//normal uniform data
		public int glProjectionMatrix = -1;
		public int glProjectionMatrixInverse = -1;
		public int glModelMatrix = -1;
		public int glViewMatrix = -1;
		public int glModelViewMatrix = -1;
		public int glModelViewMatrixInverse = -1;
		public int glModelViewProjectionMatrix = -1;
		public int glNormalMatrix = -1;
		public int ignoreVertexColors = -1;
		public int glFrontMaterialambient = -1;
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
		public int fogEnabled = -1;
		public int expColor = -1;
		public int expDensity = -1;
		public int linearColor = -1;
		public int linearStart = -1;
		public int linearEnd = -1;

		public int glVertex = -1;
		public int glColor = -1;
		public int glNormal = -1;

		public int[] glMultiTexCoord = new int[16];
		public SparseArray<Integer> genAttIndexToLoc = new SparseArray<Integer>();

	}

	/**
	 *  below here are openGL state tracking to reduce unnecessary native calls
	 *  Note this is NOT like the "new" or so called current staet above taht needs to be st in the FFP
	 *  call, this is the old or previously set data, that might not need to be updated
	 * @author phil
	 *
	 */
	public static class GL_State
	{
		public boolean depthBufferEnableOverride;
		public boolean depthBufferEnable;
		public int depthTestFunction;
		public boolean depthBufferWriteEnableOverride;
		public boolean depthBufferWriteEnable;
		public boolean userStencilAvailable;
		public boolean stencilEnable;
		public boolean glDepthMask;
		public boolean glEnableGL_STENCIL_TEST;
		public int stencilFailOp;
		public int stencilZFailOp;
		public int stencilZPassOp;
		public int stencilFunction;
		public int stencilReferenceValue;
		public int stencilCompareMask;
		public int stencilWriteMask;
		public int[] setGLSLUniform1i = new int[500];
		public int[] clearer1 = new int[500];
		public float[] setGLSLUniform1f = new float[500];
		public float[] clearer2 = new float[500];
		public boolean glEnableGL_BLEND;
		public int srcBlendFunction;
		public int dstBlendFunction;
		public int glActiveTexture;
		public int currentProgramId;
		public int[] glBindTextureGL_TEXTURE_2D = new int[35000];// indexed based on current glActiveTexture
		public int[] clearer3 = new int[35000];
		public int cullFace;
		public float polygonOffsetFactor;
		public float polygonOffset;

		public boolean ignoreVertexColors;
		public Vector4f glFrontMaterialambient= new Vector4f();
		public Vector4f glFrontMaterialdiffuse = new Vector4f();
		public Vector3f glFrontMaterialemission = new Vector3f();
		public Vector3f glFrontMaterialspecular = new Vector3f();
		public float glFrontMaterialshininess;
		public Vector4f glLightModelambient = new Vector4f();
		public Vector4f objectColor = new Vector4f();
		public Matrix4d textureTransform = new Matrix4d();
		public Matrix4d modelMatrix = new Matrix4d();
		public Matrix4d glModelViewMatrix = new Matrix4d();
		public Matrix4d glModelViewMatrixInverse = new Matrix4d();
		public Matrix4d glModelViewProjectionMatrix = new Matrix4d();
		public Matrix3d glNormalMatrix = new Matrix3d();
		public Vector4f glLightSource0position = new Vector4f();
		public Vector4f glLightSource0diffuse = new Vector4f();
		public boolean alphaTestEnabled = false;
		public int alphaTestFunction;
		public float alphaTestValue;
		public boolean fogEnabled = false;
		public Vector3f expColor = new Vector3f();
		public float expDensity;
		public Vector3f linearColor = new Vector3f();
		public float linearStart;
		public float linearEnd;

		public void clear()
		{
			depthBufferEnableOverride = false;
			depthBufferEnable = false;
			depthTestFunction = -1;
			depthBufferWriteEnableOverride = false;
			depthBufferWriteEnable = false;
			userStencilAvailable = false;
			stencilEnable = false;
			glDepthMask = false;
			glEnableGL_STENCIL_TEST = false;
			stencilFailOp = -1;
			stencilZFailOp = -1;
			stencilZPassOp = -1;
			stencilFunction = -1;
			stencilReferenceValue = -1;
			stencilCompareMask = -1;
			stencilWriteMask = -1;
			System.arraycopy(clearer1, 0, setGLSLUniform1i, 0, setGLSLUniform1i.length);
			System.arraycopy(clearer2, 0, setGLSLUniform1f, 0, setGLSLUniform1f.length);
			glEnableGL_BLEND = false;
			srcBlendFunction = -1;
			dstBlendFunction = -1;
			glActiveTexture = -1;
			currentProgramId = -1;
			System.arraycopy(clearer3, 0, glBindTextureGL_TEXTURE_2D, 0, glBindTextureGL_TEXTURE_2D.length);
			cullFace = -1;
			polygonOffsetFactor = -1;
			polygonOffset = -1;
			ignoreVertexColors = false;
			glFrontMaterialambient.set(-999f, -999f, -999f, -999f);
			glFrontMaterialdiffuse.set(-999f, -999f, -999f, -999f);
			glFrontMaterialemission.set(-999f, -999f, -999f);
			glFrontMaterialspecular.set(-999f, -999f, -999f);
			glFrontMaterialshininess = -99;
			glLightModelambient.set(-999f, -999f, -999f, -999f);
			objectColor.set(-999f, -999f, -999f, -999f);
			textureTransform.setIdentity();
			modelMatrix.setIdentity();
			glModelViewMatrix.setIdentity();
			glModelViewMatrixInverse.setIdentity();
			glModelViewProjectionMatrix.setIdentity();
			glNormalMatrix.setIdentity();
			glLightSource0position.set(-999f, -999f, -999f, -999f);
			glLightSource0diffuse.set(-999f, -999f, -999f, -999f);
			alphaTestEnabled = false;
			alphaTestFunction = -1;
			alphaTestValue = -99f;
			fogEnabled = false;
			expColor.set(-999f, -999f, -999f);
			expDensity = -99f;
			linearColor.set(-999f, -999f, -999f);
			linearStart = -99f;
			linearEnd = -99f;
		}
	}

	public GL_State gl_state = new GL_State();

	/**
	 *  program used on last run through of FFP, so nearly like gl_state above, just a desperate attempt
	 *  to see if the uniform locations have changed even if a new shader is being used
	 */
	public int prevShaderProgram;

	public static class ShaderFFPLocations
	{
		public int glProjectionMatrix;
		public int glProjectionMatrixInverse;
		public int modelMatrix;
		public int viewMatrix;
		public int glModelViewMatrix;
		public int glModelViewMatrixInverse;
		public int glModelViewProjectionMatrix;
		public int glNormalMatrix;
		public int glFrontMaterialdiffuse;
		public int glFrontMaterialemission;
		public int glFrontMaterialspecular;
		public int glFrontMaterialshininess;
		public int ignoreVertexColors;
		public int glLightModelambient;
		public int glLightSource0position;
		public int glLightSource0diffuse;
		public int textureTransform;
	}

	//a plce to put teh previous shader locations for FFP minimize calls
	public ShaderFFPLocations[] shaderFFPLocations = new ShaderFFPLocations[100];

	// The per frame stats
	public JoglesPerFrameStats perFrameStats = new JoglesPerFrameStats();

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
		perFrameStats = new JoglesPerFrameStats();
		perFrameStats.endOfPrevFrameTime = System.nanoTime();
	}

	// just a singleton of the handy matrix/array operations
	public JoglesMatrixUtil matrixUtil = new JoglesMatrixUtil();

}
