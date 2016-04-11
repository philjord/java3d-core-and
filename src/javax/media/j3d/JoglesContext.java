package javax.media.j3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLContext;

import utils.SparseArray;

public class JoglesContext extends JoglContext
{
	//TODO: heaps of lights appears to kill performance, why?
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
	public HashMap<GeometryArrayRetained, Integer> geoToIndStripSwappedSize = new HashMap<GeometryArrayRetained, Integer>();

	public HashMap<GeometryArrayRetained, Integer> geoToCoordBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, Integer> geoToCoordBufSize = new HashMap<GeometryArrayRetained, Integer>();

	public HashMap<GeometryArrayRetained, Integer> geoToColorBuf = new HashMap<GeometryArrayRetained, Integer>();
	public HashMap<GeometryArrayRetained, Integer> geoToNormalBuf = new HashMap<GeometryArrayRetained, Integer>();

	public HashMap<GeometryArrayRetained, SparseArray<Integer>> geoToTexCoordsBuf = new HashMap<GeometryArrayRetained, SparseArray<Integer>>();

	public HashMap<Object, LocationData> geoToLocationData = new HashMap<Object, LocationData>();

	public HashMap<GeometryArrayRetained, SparseArray<Integer>> geoToVertAttribBuf = new HashMap<GeometryArrayRetained, SparseArray<Integer>>();

	public SparseArray<HashMap<String, Integer>> progToGenVertAttNameToGenVertAttIndex = new SparseArray<HashMap<String, Integer>>();

	// note anything may be reused if not updated between execute calls

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

	public LightData[] dirLight = new LightData[8];
	public LightData[] pointLight = new LightData[8];
	public LightData[] spotLight = new LightData[8];

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

	// should use getMaximumLights() in pipeline
	public boolean[] enabledLights = new boolean[8];

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

		public int[] glMultiTexCoord = new int[16];
		public SparseArray<Integer> genAttIndexToLoc = new SparseArray<Integer>();

	}

	// below here are openGL state tracking to reduce unnecessary native calls
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
		public int[] setGLSLUniform1i = new int[500];
		public int[] clearer1 = new int[500];
		public float[] setGLSLUniform1f = new float[500];
		public float[] clearer2 = new float[500];
		public boolean glEnableGL_BLEND;
		public int srcBlendFunction;
		public int dstBlendFunction;
		public int glProjectionMatrixLoc;
		public int currentProjMatInverseLoc;
		public int currentViewMatLoc;
		public int glActiveTexture;
		public int currentProgramId;
		public int[] glBindTextureGL_TEXTURE_2D = new int[35000];// indexed based on current glActiveTexture
		public int[] clearer3 = new int[35000];
		public int cullFace;
		public float polygonOffsetFactor;
		public float polygonOffset;

		public boolean ignoreVertexColors;
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

		public void clear()
		{
			depthBufferEnableOverride = false;
			depthBufferEnable = false;
			depthTestFunction = 0;
			depthBufferWriteEnableOverride = false;
			depthBufferWriteEnable = false;
			userStencilAvailable = false;
			stencilEnable = false;
			glDepthMask = false;
			glEnableGL_STENCIL_TEST = false;
			System.arraycopy(clearer1, 0, setGLSLUniform1i, 0, setGLSLUniform1i.length);
			System.arraycopy(clearer2, 0, setGLSLUniform1f, 0, setGLSLUniform1f.length);
			glEnableGL_BLEND = false;
			srcBlendFunction = 0;
			dstBlendFunction = 0;
			glProjectionMatrixLoc = 0;
			currentProjMatInverseLoc = 0;
			currentViewMatLoc = 0;
			glActiveTexture = 0;
			currentProgramId = 0;
			System.arraycopy(clearer3, 0, glBindTextureGL_TEXTURE_2D, 0, glBindTextureGL_TEXTURE_2D.length);
			cullFace = 0;
			polygonOffsetFactor = 0;
			polygonOffset = 0;
			ignoreVertexColors = false;
			glFrontMaterialdiffuse.set(0, 0, 0, 0);
			glFrontMaterialemission.set(0, 0, 0);
			glFrontMaterialspecular.set(0, 0, 0);
			glFrontMaterialshininess = 0;
			glLightModelambient.set(0, 0, 0, 0);
			objectColor.set(0, 0, 0, 0);
			textureTransform.setIdentity();
			modelMatrix.setIdentity();
			glModelViewMatrix.setIdentity();
			glModelViewMatrixInverse.setIdentity();
			glModelViewProjectionMatrix.setIdentity();
			glNormalMatrix.setIdentity();
		}
	}

	public GL_State gl_state = new GL_State();

	// program used on last run through of FFP, so nearly like gl_state
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

	//some sort of previous shader system ?
	public ShaderFFPLocations[] shaderFFPLocations = new ShaderFFPLocations[100];

	//Performance issue
	// possibly I can stop calling bind 0?
	// maybe no call to glFinish?
	// up to full screen and back improves render performance!!! what

	///For frame stats

	//1  Renderer.doWork ->
	//1  RenderBin.render -> (can be the 3 background calls or the 3 normal calls (opaque/ordered/transparent)
	//4  LightBin.render ->
	//12 EnvironmentSet.render ->
	//36 AttributeBin.render ->
	//1407 ShaderBin.render ->
	//1440 TextureBin.render -> .render -> .render -> .renderList
	//3355 RenderMolecule.render ->
	//1 VertexArrayRenderMethod.render ->
	//Canvas3D.updateState ->
	//Canvas3D.updateEnvStat ->
	//ShaderBin.updateAttributes ->
	//GLSLShaderProgramRetained.updateNative -> .enableShaderProgram -> .enableShaderProgram
	//109+592 JoglesPipeline.useGLSLShaderProgram

	public static class PerFrameStats
	{
		public long frameStartTime;

		public HashSet<ShaderProgramId> usedPrograms = new HashSet<ShaderProgramId>();
		//public ArrayList<ShaderProgramId> usedPrograms = new ArrayList<ShaderProgramId>();
		//public HashSet<String> usedProgramNames = new HashSet<String>();
		//TODO: how do I get these?
		//public HashMap<ShaderProgramId, String> usedProgramNames = new HashMap<ShaderProgramId, String>();

		public int geoToClearBuffers;
		public int glDrawStripArrays;
		public int glDrawStripArraysStrips;
		public int glDrawArrays;
		public int glDrawStripElements;
		public int glDrawStripElementsStrips;
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

		public int glVertexAttribPointerNormals;
		public int glVertexAttribPointerUserAttribs;
		public int glVertexAttribPointerColor;
		public int glVertexAttribPointerCoord;
		public int glBufferData;
		public int glBufferSubData;
		public int glDisableVertexAttribArray;

		public int modelMatrixUpdated;
		public int glModelViewMatrixUpdated;
		public int glModelViewProjectionMatrixUpdated;
		public int glNormalMatrixUpdated;
		public int glModelViewMatrixInverseUpdated;

		public int modelMatrixSkipped;
		public int glModelViewMatrixSkipped;
		public int glModelViewProjectionMatrixSkipped;
		public int glNormalMatrixSkipped;
		public int glModelViewMatrixInverseSkipped;

		public void outputPerFrameData()
		{
			boolean highInterestOnly = true;

			System.out.println("geoToClearBuffers " + geoToClearBuffers);
			System.out
					.println("glDrawStripArrays " + glDrawStripArrays + "\t made up of glDrawStripArraysStrips " + glDrawStripArraysStrips);
			System.out.println("glDrawArrays " + glDrawArrays);
			System.out.println(
					"glDrawStripElements " + glDrawStripElements + "\t made up of glDrawStripElementsStrips " + glDrawStripElementsStrips);
			System.out.println("glDrawElements " + glDrawElements);
			System.out.println("enableTexCoordPointer " + enableTexCoordPointer);
			System.out.println("glVertexAttribPointerNormals " + glVertexAttribPointerNormals);
			System.out.println("glVertexAttribPointerUserAttribs " + glVertexAttribPointerUserAttribs);
			System.out.println("glVertexAttribPointerColor " + glVertexAttribPointerColor);
			System.out.println("glVertexAttribPointerCoord " + glVertexAttribPointerCoord);
			System.out.println("glBufferData " + glBufferData);
			System.out.println("glBufferSubData " + glBufferSubData);
			System.out.println("glDisableVertexAttribArray " + glDisableVertexAttribArray + " note native called commented out, trouble?");
			System.out.println("---");
			System.out.println("setModelViewMatrix " + setModelViewMatrix);
			System.out.println("setFFPAttributes " + setFFPAttributes);
			System.out.println("modelMatrixUpdated " + modelMatrixUpdated + " modelMatrixSkipped " + modelMatrixSkipped);
			System.out.println(
					"glModelViewMatrixUpdated " + glModelViewMatrixUpdated + " glModelViewMatrixSkipped " + glModelViewMatrixSkipped);
			System.out.println("glModelViewProjectionMatrixUpdated " + glModelViewProjectionMatrixUpdated
					+ " glModelViewProjectionMatrixSkipped " + glModelViewProjectionMatrixSkipped);
			System.out.println("glNormalMatrixUpdated " + glNormalMatrixUpdated + " glNormalMatrixSkipped " + glNormalMatrixSkipped);
			System.out.println("---");
			if (!highInterestOnly)
			{
				System.out.println("geoToLocationData " + geoToLocationData);
				System.out.print("createGLSLShader " + createGLSLShader);
				System.out.print("\tcreateGLSLShaderProgram " + createGLSLShaderProgram);
				System.out.print("\tcompileGLSLShader " + compileGLSLShader);
				System.out.print("\tdestroyGLSLShader " + destroyGLSLShader);
				System.out.print("\tdestroyGLSLShaderProgram " + destroyGLSLShaderProgram);
				System.out.print("\tlinkGLSLShaderProgram " + linkGLSLShaderProgram);
				System.out.print("\tbindGLSLVertexAttrName " + bindGLSLVertexAttrName);
				System.out.println("\tlookupGLSLShaderAttrNames " + lookupGLSLShaderAttrNames);
				System.out.print("updateDirectionalLight " + updateDirectionalLight);
				System.out.print("\tupdatePointLight " + updatePointLight);
				System.out.println("\tupdateSpotLight " + updateSpotLight);
				System.out.print("updateExponentialFog " + updateExponentialFog);
				System.out.print("\tupdateLinearFog " + updateLinearFog);
				System.out.print("\tdisableFog " + disableFog);
				System.out.println("\tsetFogEnableFlag " + setFogEnableFlag);
				System.out.print("updateLineAttributes " + updateLineAttributes);
				System.out.println("\tresetLineAttributes " + resetLineAttributes);
				System.out.print("updateMaterial " + updateMaterial);
				System.out.println("\tupdateMaterialColor " + updateMaterialColor);
				System.out.print("updateColoringAttributes " + updateColoringAttributes);
				System.out.println("\tresetColoringAttributes " + resetColoringAttributes);
				System.out.print("updatePointAttributes " + updatePointAttributes);
				System.out.println("\tresetPointAttributes " + resetPointAttributes);
				System.out.print("updatePolygonAttributes " + updatePolygonAttributes);
				System.out.println("\tresetPolygonAttributes " + resetPolygonAttributes);
				System.out.print("updateRenderingAttributes " + updateRenderingAttributes);
				System.out.println("\tresetRenderingAttributes " + resetRenderingAttributes);
				System.out.println("setFullSceneAntialiasing " + setFullSceneAntialiasing);
				System.out.println("setLightEnables " + setLightEnables);
				System.out.println("setSceneAmbient " + setSceneAmbient);
				System.out.println("resetTexCoordGeneration " + resetTexCoordGeneration);
				System.out.println("useCtx " + useCtx);
				System.out.println("releaseCtx " + releaseCtx);
				System.out.println("clear " + clear);
				System.out.println("setViewport " + setViewport);
				System.out.println("setProjectionMatrix " + setProjectionMatrix);
			}

			System.out.print("updateTransparencyAttributes " + updateTransparencyAttributes);
			System.out.println("\tresetTransparency " + resetTransparency);
			System.out.print("updateTextureAttributes " + updateTextureAttributes);
			System.out.println("\tresetTextureAttributes " + resetTextureAttributes);
			System.out.println("updateTextureUnitState " + updateTextureUnitState);
			System.out.println("bindTexture2D " + bindTexture2D);
			System.out.println("bindTextureCubeMap " + bindTextureCubeMap);
			System.out.println("setBlendColor " + setBlendColor);
			System.out.println("setBlendFunc " + setBlendFunc);
			System.out.println("activeTextureUnit " + activeTextureUnit);
			System.out.println("resetTextureNative " + resetTextureNative);
			System.out.println("freeTexture " + freeTexture);
			System.out.println("generateTexID " + generateTexID);
			System.out.println("setDepthBufferWriteEnable " + setDepthBufferWriteEnable);
			System.out.println("useGLSLShaderProgram " + useGLSLShaderProgram);
			System.out.println("redundantUseProgram " + redundantUseProgram);

			//for (ShaderProgramId id : usedPrograms)
			//	System.out.println("ShaderProgramId " + ((JoglShaderObject) id).getValue());

			System.out.println("frameTime ns " + (System.nanoTime() - frameStartTime) + " = fps: "
					+ (1000 / ((System.nanoTime() - frameStartTime) / 1000000L)));
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
		perFrameStats.frameStartTime = System.nanoTime();
	}

	//Oh lordy lordy yo' betta swear yo' single freadin' !!!

	public Matrix4d deburnV = new Matrix4d();//deburners 
	public Matrix4d deburnM = new Matrix4d();
	public float[] tempMat9 = new float[9];
	public float[] tempMat16 = new float[16];
	public double[] tempMatD9 = new double[9];

	public float[] toArray(Matrix4d m)
	{
		return toArray(m, tempMat16);
	}

	public static float[] toArray(Matrix4d m, float[] a)
	{
		a[0] = (float) m.m00;
		a[1] = (float) m.m01;
		a[2] = (float) m.m02;
		a[3] = (float) m.m03;
		a[4] = (float) m.m10;
		a[5] = (float) m.m11;
		a[6] = (float) m.m12;
		a[7] = (float) m.m13;
		a[8] = (float) m.m20;
		a[9] = (float) m.m21;
		a[10] = (float) m.m22;
		a[11] = (float) m.m23;
		a[12] = (float) m.m30;
		a[13] = (float) m.m31;
		a[14] = (float) m.m32;
		a[15] = (float) m.m33;

		return a;
	}

	public float[] toArray(Matrix3d m)
	{
		return toArray(m, tempMat9);
	}

	public static float[] toArray(Matrix3d m, float[] a)
	{
		a[0] = (float) m.m00;
		a[1] = (float) m.m01;
		a[2] = (float) m.m02;
		a[3] = (float) m.m10;
		a[4] = (float) m.m11;
		a[5] = (float) m.m12;
		a[6] = (float) m.m20;
		a[7] = (float) m.m21;
		a[8] = (float) m.m22;

		return a;
	}

	public double[] toArray3x3(Matrix4d m)
	{
		return toArray3x3(m, tempMatD9);
	}

	public static double[] toArray3x3(Matrix4d m, double[] a)
	{
		a[0] = m.m00;
		a[1] = m.m01;
		a[2] = m.m02;
		a[3] = m.m10;
		a[4] = m.m11;
		a[5] = m.m12;
		a[6] = m.m20;
		a[7] = m.m21;
		a[8] = m.m22;

		return a;
	}

	private JoglesMatrixInverter matrixInverter = new JoglesMatrixInverter();

	public void invert(Matrix3d m1)
	{
		try
		{
			matrixInverter.invertGeneral3(m1, m1);
		}
		catch (Exception e)
		{
			//fine, move along
			m1.setIdentity();
		}
	}

	public void invert(Matrix4d m1)
	{
		try
		{
			matrixInverter.invertGeneral4(m1, m1);
		}
		catch (Exception e)
		{
			//fine, move along
			m1.setIdentity();
		}
	}

	//More single threaded death-defying gear

	private FloatBuffer matFB4x4;

	public FloatBuffer toFB4(float[] f)
	{
		if (matFB4x4 == null)
		{
			ByteBuffer bb = ByteBuffer.allocateDirect(16 * 4);
			bb.order(ByteOrder.nativeOrder());
			matFB4x4 = bb.asFloatBuffer();
		}
		matFB4x4.position(0);
		matFB4x4.put(f);
		matFB4x4.position(0);
		return matFB4x4;
	}

	public FloatBuffer toFB3(float[] f)
	{
		if (matFB3x3 == null)
		{
			ByteBuffer bb = ByteBuffer.allocateDirect(16 * 4);
			bb.order(ByteOrder.nativeOrder());
			matFB3x3 = bb.asFloatBuffer();
		}
		matFB3x3.position(0);
		matFB3x3.put(f);
		matFB3x3.position(0);
		return matFB3x3;
	}

	public FloatBuffer toFB(Matrix4d m)
	{
		if (matFB4x4 == null)
		{
			ByteBuffer bb = ByteBuffer.allocateDirect(16 * 4);
			bb.order(ByteOrder.nativeOrder());
			matFB4x4 = bb.asFloatBuffer();
		}
		matFB4x4.position(0);
		matFB4x4.put(toArray(m));
		matFB4x4.position(0);
		return matFB4x4;
	}

	private FloatBuffer matFB3x3;

	public FloatBuffer toFB(Matrix3d m)
	{
		if (matFB3x3 == null)
		{
			ByteBuffer bb = ByteBuffer.allocateDirect(9 * 4);
			bb.order(ByteOrder.nativeOrder());
			matFB3x3 = bb.asFloatBuffer();
		}
		matFB3x3.position(0);
		matFB3x3.put(toArray(m));
		matFB3x3.position(0);
		return matFB3x3;
	}

}
