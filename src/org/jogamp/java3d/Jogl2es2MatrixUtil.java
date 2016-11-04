package org.jogamp.java3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.jogamp.vecmath.Matrix3d;
import org.jogamp.vecmath.Matrix4d;
import org.jogamp.vecmath.SingularMatrixException;
import org.jogamp.vecmath.Tuple4f;
import org.jogamp.vecmath.Vector4f;

/** class that demands single threading and uses deburners, don't touch if you don't understand, it will be bad...
 * 
 * @author phil
 *
 */
class Jogl2es2MatrixUtil
{

	/**
	 * Possibly faster
	 * http://stackoverflow.com/questions/983999/simple-3x3-matrix-inverse-code-c
	 */

	public static void transposeInvert(Matrix3d m, Matrix3d out)
	{
		double determinant = m.determinant();
		if (determinant > 0)
		{
			double invdet = 1 / determinant;
			out.m00 = (m.m11 * m.m22 - m.m21 * m.m12) * invdet;
			out.m10 = -(m.m01 * m.m22 - m.m02 * m.m21) * invdet;
			out.m20 = (m.m01 * m.m12 - m.m02 * m.m11) * invdet;
			out.m01 = -(m.m10 * m.m22 - m.m12 * m.m20) * invdet;
			out.m11 = (m.m00 * m.m22 - m.m02 * m.m20) * invdet;
			out.m21 = -(m.m00 * m.m12 - m.m10 * m.m02) * invdet;
			out.m02 = (m.m10 * m.m21 - m.m20 * m.m11) * invdet;
			out.m12 = -(m.m00 * m.m21 - m.m20 * m.m01) * invdet;
			out.m22 = (m.m00 * m.m11 - m.m10 * m.m01) * invdet;
		}
		else
		{
			out.setIdentity();
		}
	}

	/**
	 * Only upper left 3x3 copied and transformed
	 * @param m
	 * @param out
	 */

	public static void transposeInvert(Matrix4d m, Matrix3d out)
	{
		double determinant = m.determinant();
		if (determinant > 0)
		{
			double invdet = 1 / determinant;
			out.m00 = (m.m11 * m.m22 - m.m21 * m.m12) * invdet;
			out.m10 = -(m.m01 * m.m22 - m.m02 * m.m21) * invdet;
			out.m20 = (m.m01 * m.m12 - m.m02 * m.m11) * invdet;
			out.m01 = -(m.m10 * m.m22 - m.m12 * m.m20) * invdet;
			out.m11 = (m.m00 * m.m22 - m.m02 * m.m20) * invdet;
			out.m21 = -(m.m00 * m.m12 - m.m10 * m.m02) * invdet;
			out.m02 = (m.m10 * m.m21 - m.m20 * m.m11) * invdet;
			out.m12 = -(m.m00 * m.m21 - m.m20 * m.m01) * invdet;
			out.m22 = (m.m00 * m.m11 - m.m10 * m.m01) * invdet;
		}
		else
		{
			out.setIdentity();
		}
	}

	double result3[] = new double[9];
	int row_perm3[] = new int[3];
	double[] tmp3 = new double[9]; // scratch matrix
	//@See Matrix3d

	final void invertGeneral3(Matrix3d thisM, Matrix3d m1)
	{
		for (int i = 0; i < 3; i++)
			row_perm3[i] = 0;

		for (int i = 0; i < 9; i++)
			result3[i] = 0.0;

		// Use LU decomposition and backsubstitution code specifically
		// for floating-point 3x3 matrices.

		// Copy source matrix to t1tmp
		tmp3[0] = m1.m00;
		tmp3[1] = m1.m01;
		tmp3[2] = m1.m02;

		tmp3[3] = m1.m10;
		tmp3[4] = m1.m11;
		tmp3[5] = m1.m12;

		tmp3[6] = m1.m20;
		tmp3[7] = m1.m21;
		tmp3[8] = m1.m22;

		// Calculate LU decomposition: Is the matrix singular?
		if (!luDecomposition3(tmp3, row_perm3))
		{
			// Matrix has no inverse
			throw new SingularMatrixException("!luDecomposition(tmp, row_perm)");
		}

		// Perform back substitution on the identity matrix

		result3[0] = 1.0;
		result3[4] = 1.0;
		result3[8] = 1.0;
		luBacksubstitution3(tmp3, row_perm3, result3);

		thisM.m00 = result3[0];
		thisM.m01 = result3[1];
		thisM.m02 = result3[2];

		thisM.m10 = result3[3];
		thisM.m11 = result3[4];
		thisM.m12 = result3[5];

		thisM.m20 = result3[6];
		thisM.m21 = result3[7];
		thisM.m22 = result3[8];

	}

	double row_scale3[] = new double[3];

	//@See Matrix3d
	boolean luDecomposition3(double[] matrix0, int[] row_perm)
	{
		for (int i = 0; i < 3; i++)
			row_scale3[i] = 0.0;
		// Determine implicit scaling information by looping over rows
		{
			int i, j;
			int ptr, rs;
			double big, temp;

			ptr = 0;
			rs = 0;

			// For each row ...
			i = 3;
			while (i-- != 0)
			{
				big = 0.0;

				// For each column, find the largest element in the row
				j = 3;
				while (j-- != 0)
				{
					temp = matrix0[ptr++];
					temp = Math.abs(temp);
					if (temp > big)
					{
						big = temp;
					}
				}

				// Is the matrix singular?
				if (big == 0.0)
				{
					return false;
				}
				row_scale4[rs++] = 1.0 / big;
			}
		}

		{
			int j;
			int mtx;

			mtx = 0;

			// For all columns, execute Crout's method
			for (j = 0; j < 3; j++)
			{
				int i, imax, k;
				int target, p1, p2;
				double sum, big, temp;

				// Determine elements of upper diagonal matrix U
				for (i = 0; i < j; i++)
				{
					target = mtx + (3 * i) + j;
					sum = matrix0[target];
					k = i;
					p1 = mtx + (3 * i);
					p2 = mtx + j;
					while (k-- != 0)
					{
						sum -= matrix0[p1] * matrix0[p2];
						p1++;
						p2 += 3;
					}
					matrix0[target] = sum;
				}

				// Search for largest pivot element and calculate
				// intermediate elements of lower diagonal matrix L.
				big = 0.0;
				imax = -1;
				for (i = j; i < 3; i++)
				{
					target = mtx + (3 * i) + j;
					sum = matrix0[target];
					k = j;
					p1 = mtx + (3 * i);
					p2 = mtx + j;
					while (k-- != 0)
					{
						sum -= matrix0[p1] * matrix0[p2];
						p1++;
						p2 += 3;
					}
					matrix0[target] = sum;

					// Is this the best pivot so far?
					if ((temp = row_scale3[i] * Math.abs(sum)) >= big)
					{
						big = temp;
						imax = i;
					}
				}

				if (imax < 0)
				{
					throw new RuntimeException("imax < 0");
				}

				// Is a row exchange necessary?
				if (j != imax)
				{
					// Yes: exchange rows
					k = 3;
					p1 = mtx + (3 * imax);
					p2 = mtx + (3 * j);
					while (k-- != 0)
					{
						temp = matrix0[p1];
						matrix0[p1++] = matrix0[p2];
						matrix0[p2++] = temp;
					}

					// Record change in scale factor
					row_scale3[imax] = row_scale3[j];
				}

				// Record row permutation
				row_perm[j] = imax;

				// Is the matrix singular
				if (matrix0[(mtx + (3 * j) + j)] == 0.0)
				{
					return false;
				}

				// Divide elements of lower diagonal matrix L by pivot
				if (j != (3 - 1))
				{
					temp = 1.0 / (matrix0[(mtx + (3 * j) + j)]);
					target = mtx + (3 * (j + 1)) + j;
					i = 2 - j;
					while (i-- != 0)
					{
						matrix0[target] *= temp;
						target += 3;
					}
				}
			}
		}

		return true;
	}

	//@See Matrix3d
	void luBacksubstitution3(double[] matrix1, int[] row_perm, double[] matrix2)
	{
		int i, ii, ip, j, k;
		int rp;
		int cv, rv;

		//	rp = row_perm;
		rp = 0;

		// For each column vector of matrix2 ...
		for (k = 0; k < 3; k++)
		{
			//	    cv = &(matrix2[0][k]);
			cv = k;
			ii = -1;

			// Forward substitution
			for (i = 0; i < 3; i++)
			{
				double sum;

				ip = row_perm[rp + i];
				sum = matrix2[cv + 3 * ip];
				matrix2[cv + 3 * ip] = matrix2[cv + 3 * i];
				if (ii >= 0)
				{
					//		    rv = &(matrix1[i][0]);
					rv = i * 3;
					for (j = ii; j <= i - 1; j++)
					{
						sum -= matrix1[rv + j] * matrix2[cv + 3 * j];
					}
				}
				else if (sum != 0.0)
				{
					ii = i;
				}
				matrix2[cv + 3 * i] = sum;
			}

			// Backsubstitution
			//	    rv = &(matrix1[3][0]);
			rv = 2 * 3;
			matrix2[cv + 3 * 2] /= matrix1[rv + 2];

			rv -= 3;
			matrix2[cv + 3 * 1] = (matrix2[cv + 3 * 1] - matrix1[rv + 2] * matrix2[cv + 3 * 2]) / matrix1[rv + 1];

			rv -= 3;
			matrix2[cv + 4 * 0] = (matrix2[cv + 3 * 0] - matrix1[rv + 1] * matrix2[cv + 3 * 1] - matrix1[rv + 2] * matrix2[cv + 3 * 2])
					/ matrix1[rv + 0];

		}
	}

	double result4[] = new double[16];
	int row_perm4[] = new int[4];
	double[] tmp4 = new double[16]; // scratch matrix

	final void invertGeneral4(Matrix4d thisM, Matrix4d m1)
	{
		for (int i = 0; i < 4; i++)
			row_perm4[i] = 0;

		for (int i = 0; i < 16; i++)
			result4[i] = 0.0;

		// Use LU decomposition and backsubstitution code specifically
		// for floating-point 4x4 matrices.

		// Copy source matrix to t1tmp
		tmp4[0] = m1.m00;
		tmp4[1] = m1.m01;
		tmp4[2] = m1.m02;
		tmp4[3] = m1.m03;

		tmp4[4] = m1.m10;
		tmp4[5] = m1.m11;
		tmp4[6] = m1.m12;
		tmp4[7] = m1.m13;

		tmp4[8] = m1.m20;
		tmp4[9] = m1.m21;
		tmp4[10] = m1.m22;
		tmp4[11] = m1.m23;

		tmp4[12] = m1.m30;
		tmp4[13] = m1.m31;
		tmp4[14] = m1.m32;
		tmp4[15] = m1.m33;

		// Calculate LU decomposition: Is the matrix singular?
		if (!luDecomposition4(tmp4, row_perm4))
		{
			// Matrix has no inverse
			throw new SingularMatrixException("luDecomposition4(tmp4, row_perm4)");
		}

		// Perform back substitution on the identity matrix	 
		result4[0] = 1.0;
		result4[5] = 1.0;
		result4[10] = 1.0;
		result4[15] = 1.0;
		luBacksubstitution4(tmp4, row_perm4, result4);

		thisM.m00 = result4[0];
		thisM.m01 = result4[1];
		thisM.m02 = result4[2];
		thisM.m03 = result4[3];

		thisM.m10 = result4[4];
		thisM.m11 = result4[5];
		thisM.m12 = result4[6];
		thisM.m13 = result4[7];

		thisM.m20 = result4[8];
		thisM.m21 = result4[9];
		thisM.m22 = result4[10];
		thisM.m23 = result4[11];

		thisM.m30 = result4[12];
		thisM.m31 = result4[13];
		thisM.m32 = result4[14];
		thisM.m33 = result4[15];

	}

	double row_scale4[] = new double[4];

	boolean luDecomposition4(double[] matrix0, int[] row_perm)
	{
		for (int i = 0; i < 4; i++)
			row_scale4[i] = 0.0;
		// Determine implicit scaling information by looping over rows
		{
			int i, j;
			int ptr, rs;
			double big, temp;

			ptr = 0;
			rs = 0;

			// For each row ...
			i = 4;
			while (i-- != 0)
			{
				big = 0.0;

				// For each column, find the largest element in the row
				j = 4;
				while (j-- != 0)
				{
					temp = matrix0[ptr++];
					temp = Math.abs(temp);
					if (temp > big)
					{
						big = temp;
					}
				}

				// Is the matrix singular?
				if (big == 0.0)
				{
					return false;
				}
				row_scale4[rs++] = 1.0 / big;
			}
		}

		{
			int j;
			int mtx;

			mtx = 0;

			// For all columns, execute Crout's method
			for (j = 0; j < 4; j++)
			{
				int i, imax, k;
				int target, p1, p2;
				double sum, big, temp;

				// Determine elements of upper diagonal matrix U
				for (i = 0; i < j; i++)
				{
					target = mtx + (4 * i) + j;
					sum = matrix0[target];
					k = i;
					p1 = mtx + (4 * i);
					p2 = mtx + j;
					while (k-- != 0)
					{
						sum -= matrix0[p1] * matrix0[p2];
						p1++;
						p2 += 4;
					}
					matrix0[target] = sum;
				}

				// Search for largest pivot element and calculate
				// intermediate elements of lower diagonal matrix L.
				big = 0.0;
				imax = -1;
				for (i = j; i < 4; i++)
				{
					target = mtx + (4 * i) + j;
					sum = matrix0[target];
					k = j;
					p1 = mtx + (4 * i);
					p2 = mtx + j;
					while (k-- != 0)
					{
						sum -= matrix0[p1] * matrix0[p2];
						p1++;
						p2 += 4;
					}
					matrix0[target] = sum;

					// Is this the best pivot so far?
					if ((temp = row_scale4[i] * Math.abs(sum)) >= big)
					{
						big = temp;
						imax = i;
					}
				}

				if (imax < 0)
				{
					throw new RuntimeException("(imax < 0)");
				}

				// Is a row exchange necessary?
				if (j != imax)
				{
					// Yes: exchange rows
					k = 4;
					p1 = mtx + (4 * imax);
					p2 = mtx + (4 * j);
					while (k-- != 0)
					{
						temp = matrix0[p1];
						matrix0[p1++] = matrix0[p2];
						matrix0[p2++] = temp;
					}

					// Record change in scale factor
					row_scale4[imax] = row_scale4[j];
				}

				// Record row permutation
				row_perm[j] = imax;

				// Is the matrix singular
				if (matrix0[(mtx + (4 * j) + j)] == 0.0)
				{
					return false;
				}

				// Divide elements of lower diagonal matrix L by pivot
				if (j != (4 - 1))
				{
					temp = 1.0 / (matrix0[(mtx + (4 * j) + j)]);
					target = mtx + (4 * (j + 1)) + j;
					i = 3 - j;
					while (i-- != 0)
					{
						matrix0[target] *= temp;
						target += 4;
					}
				}
			}
		}

		return true;
	}

	void luBacksubstitution4(double[] matrix1, int[] row_perm, double[] matrix2)
	{

		int i, ii, ip, j, k;
		int rp;
		int cv, rv;

		//	rp = row_perm;
		rp = 0;

		// For each column vector of matrix2 ...
		for (k = 0; k < 4; k++)
		{
			//	    cv = &(matrix2[0][k]);
			cv = k;
			ii = -1;

			// Forward substitution
			for (i = 0; i < 4; i++)
			{
				double sum;

				ip = row_perm[rp + i];
				sum = matrix2[cv + 4 * ip];
				matrix2[cv + 4 * ip] = matrix2[cv + 4 * i];
				if (ii >= 0)
				{
					//		    rv = &(matrix1[i][0]);
					rv = i * 4;
					for (j = ii; j <= i - 1; j++)
					{
						sum -= matrix1[rv + j] * matrix2[cv + 4 * j];
					}
				}
				else if (sum != 0.0)
				{
					ii = i;
				}
				matrix2[cv + 4 * i] = sum;
			}

			// Backsubstitution
			//	    rv = &(matrix1[3][0]);
			rv = 3 * 4;
			matrix2[cv + 4 * 3] /= matrix1[rv + 3];

			rv -= 4;
			matrix2[cv + 4 * 2] = (matrix2[cv + 4 * 2] - matrix1[rv + 3] * matrix2[cv + 4 * 3]) / matrix1[rv + 2];

			rv -= 4;
			matrix2[cv + 4 * 1] = (matrix2[cv + 4 * 1] - matrix1[rv + 2] * matrix2[cv + 4 * 2] - matrix1[rv + 3] * matrix2[cv + 4 * 3])
					/ matrix1[rv + 1];

			rv -= 4;
			matrix2[cv + 4 * 0] = (matrix2[cv + 4 * 0] - matrix1[rv + 1] * matrix2[cv + 4 * 1] - matrix1[rv + 2] * matrix2[cv + 4 * 2]
					- matrix1[rv + 3] * matrix2[cv + 4 * 3]) / matrix1[rv + 0];
		}
	}

	public final void transform(Matrix4d m, Tuple4f vecOut)
	{
		float x, y, z;

		x = (float) (m.m00 * vecOut.x + m.m01 * vecOut.y + m.m02 * vecOut.z + m.m03 * vecOut.w);
		y = (float) (m.m10 * vecOut.x + m.m11 * vecOut.y + m.m12 * vecOut.z + m.m13 * vecOut.w);
		z = (float) (m.m20 * vecOut.x + m.m21 * vecOut.y + m.m22 * vecOut.z + m.m23 * vecOut.w);
		vecOut.w = (float) (m.m30 * vecOut.x + m.m31 * vecOut.y + m.m32 * vecOut.z + m.m33 * vecOut.w);
		vecOut.x = x;
		vecOut.y = y;
		vecOut.z = z;
	}

	//Oh lordy lordy yo' betta swear yo' single freadin' !!!

	public double[] deburnV2 = null;//deburners 

	public Matrix4d deburnV = new Matrix4d();//deburners 
	public Matrix4d deburnM = new Matrix4d();
	public float[] tempMat9 = new float[9];
	public float[] tempMat12 = new float[12];
	public float[] tempMat16 = new float[16];
	public double[] tempMatD9 = new double[9];

	public float[] toArray(Matrix4d m)
	{
		tempMat16[0] = (float) m.m00;
		tempMat16[1] = (float) m.m01;
		tempMat16[2] = (float) m.m02;
		tempMat16[3] = (float) m.m03;
		tempMat16[4] = (float) m.m10;
		tempMat16[5] = (float) m.m11;
		tempMat16[6] = (float) m.m12;
		tempMat16[7] = (float) m.m13;
		tempMat16[8] = (float) m.m20;
		tempMat16[9] = (float) m.m21;
		tempMat16[10] = (float) m.m22;
		tempMat16[11] = (float) m.m23;
		tempMat16[12] = (float) m.m30;
		tempMat16[13] = (float) m.m31;
		tempMat16[14] = (float) m.m32;
		tempMat16[15] = (float) m.m33;
		return tempMat16;
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
		tempMat9[0] = (float) m.m00;
		tempMat9[1] = (float) m.m01;
		tempMat9[2] = (float) m.m02;
		tempMat9[3] = (float) m.m10;
		tempMat9[4] = (float) m.m11;
		tempMat9[5] = (float) m.m12;
		tempMat9[6] = (float) m.m20;
		tempMat9[7] = (float) m.m21;
		tempMat9[8] = (float) m.m22;
		return tempMat9;

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

	public float[] toArray3x4(Matrix3d m)
	{
		return toArray3x4(m, tempMat12);
	}

	public static float[] toArray3x4(Matrix3d m, float[] a)
	{
		a[0] = (float) m.m00;
		a[1] = (float) m.m01;
		a[2] = (float) m.m02;
		a[3] = 0f;
		a[4] = (float) m.m10;
		a[5] = (float) m.m11;
		a[6] = (float) m.m12;
		a[7] = 0f;
		a[8] = (float) m.m20;
		a[9] = (float) m.m21;
		a[10] = (float) m.m22;
		a[11] = 0f;

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

	public void invert(Matrix3d m1)
	{
		try
		{
			invertGeneral3(m1, m1);
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
			invertGeneral4(m1, m1);
		}
		catch (Exception e)
		{
			//fine, move along
			m1.setIdentity();
		}
	}

	//More single threaded death-defying gear
	private Vector4f tmpV4f = new Vector4f();;

	public Vector4f transform(Matrix4d m1, Matrix4d m2, float tx, float ty, float tz, float tw)
	{
		tmpV4f.set(tx, ty, tz, tw);
		transform(m1, tmpV4f);
		transform(m2, tmpV4f);
		return tmpV4f;
	}

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

	// Not needed generally as transpose can be called on the inteface with gl
	public static float[] transposeInPlace(float[] src)
	{
		float v1 = src[1];
		float v2 = src[2];
		float v3 = src[3];
		float v6 = src[6];
		float v7 = src[7];
		float v11 = src[11];

		//src[0] = src[0];		
		src[1] = src[4];
		src[2] = src[8];
		src[3] = src[12];
		src[4] = v1;
		//src[5] = src[5];		
		src[6] = src[9];
		src[7] = src[13];
		src[8] = v2;
		src[9] = v6;
		//src[10] = src[10];		
		src[11] = src[14];
		src[12] = v3;
		src[13] = v7;
		src[14] = v11;
		//src[15] = src[15];

		return src;
	}

	// ignores the higher 16 bits
	public static float halfToFloat(int hbits)
	{
		int mant = hbits & 0x03ff; // 10 bits mantissa
		int exp = hbits & 0x7c00; // 5 bits exponent
		if (exp == 0x7c00) // NaN/Inf
			exp = 0x3fc00; // -> NaN/Inf
		else if (exp != 0) // normalized value
		{
			exp += 0x1c000; // exp - 15 + 127
			if (mant == 0 && exp > 0x1c400) // smooth transition
				return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
		}
		else if (mant != 0) // && exp==0 -> subnormal
		{
			exp = 0x1c400; // make it normal
			do
			{
				mant <<= 1; // mantissa * 2
				exp -= 0x400; // decrease exp by 1
			}
			while ((mant & 0x400) == 0); // while not normal
			mant &= 0x3ff; // discard subnormal bit
		} // else +/-0 -> +/-0
		return Float.intBitsToFloat( // combine all parts
				(hbits & 0x8000) << 16 // sign  << ( 31 - 15 )
						| (exp | mant) << 13); // value << ( 23 - 10 )
	}

	// returns all higher 16 bits as 0 for all results
	public static int halfFromFloat(float fval)
	{
		int fbits = Float.floatToIntBits(fval);
		int sign = fbits >>> 16 & 0x8000; // sign only
		int val = (fbits & 0x7fffffff) + 0x1000; // rounded value

		if (val >= 0x47800000) // might be or become NaN/Inf
		{ // avoid Inf due to rounding
			if ((fbits & 0x7fffffff) >= 0x47800000)
			{ // is or must become NaN/Inf
				if (val < 0x7f800000) // was value but too large
					return sign | 0x7c00; // make it +/-Inf
				return sign | 0x7c00 | // remains +/-Inf or NaN
						(fbits & 0x007fffff) >>> 13; // keep NaN (and Inf) bits
			}
			return sign | 0x7bff; // unrounded not quite Inf
		}
		if (val >= 0x38800000) // remains normalized value
			return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
		if (val < 0x33000000) // too small for subnormal
			return sign; // becomes +/-0
		val = (fbits & 0x7fffffff) >>> 23; // tmp exp for subnormal calc
		return sign | ((fbits & 0x7fffff | 0x800000) // add subnormal bit
				+ (0x800000 >>> val - 102) // round depending on cut off
				>>> 126 - val); // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
	}
}