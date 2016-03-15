package javax.media.j3d;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.SingularMatrixException;

/** class that demands single threading and uses deburners, don't touch if you don't understand, it will be bad...
 * 
 * @author phil
 *
 */
class JoglesMatrixInverter
{
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
}