/**
 * The {@code Rotation} class provides utilities for constructing and applying
 * 3x3 and 6x6 rotation matrices using Euler angles (alpha, beta, gamma).
 */
public class Rotation {
    /**
     * Can be used to test the code in JShell by typing:
     * <p>
     *     [path_to_src_folder_containing_Rotation.java]>jshell
     * <p>
     *     jhshell>open/ Rotation.java
     * <p>
     *     jhshell>Rotation.test(1, 2, 3)
     * <p>
     * where instead of `1`, `2` and `3` you can have Euler angles of your choice.
     * More about using JShell in README.txt.
     *
     * @param alpha Rotation around the z-axis (in radians)
     * @param beta  Rotation around the x-axis (in radians)
     * @param gamma Second rotation around the z-axis (in radians)
     * @return matrix
     */
    public static double[][] test(double alpha, double beta, double gamma) {
        double[][] identity = new double[][] {
            {1, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0},
            {0, 0, 0, 1, 0, 0},
            {0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 0, 1}
        };

        double[][] rotatedIdentity = rotateGivenMxNMatrix(identity, alpha, beta, gamma);

        return rotatedIdentity;
    }

    /**
     * Applies rotation to an arbitrary M×N matrix using Euler angles. This computes: 
     * <p>
     * R * A * transpose(R')
     * <p>
     * where R is MxM rotation matrix and R' NxN rotation matrix corresponding to the rotation 
     * and A is the matrix given as an argument.
     *
     * @param A     Input M×N matrix. Must be 3x3, 3x6, 6x3 or 6x6 matrix.
     * @param alpha Rotation around z-axis (first rotation)
     * @param beta  Rotation around x-axis (second rotation)
     * @param gamma Rotation around z-axis (third rotation)
     * @return Rotated M×N matrix R * A * transpose(R')
     */
    public static double[][] rotateGivenMxNMatrix(double[][] A, double alpha, double beta, double gamma) {
        int M = A.length;
        int N = A[0].length;
        double[][] rotationMatrixMxM = createRotationMatrix(M, alpha, beta, gamma);
        double[][] rotationMatrixNxN = createRotationMatrix(N, alpha, beta, gamma);

        double[][] rotatedA = new double[M][N];
        for (int i = 0; i < M; i++) {
            for (int l = 0; l < N; l++) {
                for (int j = 0; j < M; j++) {
                    for (int k = 0; k < N; k++) {
                        rotatedA[i][l] += rotationMatrixMxM[i][j] * A[j][k] * rotationMatrixNxN[l][k];
                        // Note that rotationMatrixNxN[l][k] is transpose of rotationMatrixNxN[k][l]
                    }
                }
            }
        }
        return rotatedA;
    }

    /**
     * Constructs a 3x3 or 6x6 rotation matrix and its transpose using Euler angles alpha, beta and gamma.
     * <p>
     * Rotation follows the Z-X-Z convention: R = Rz(gamma) * Rx(beta) * Rz(alpha)
     *
     * @param N     Size of the matrix (must be either 3 or 6)
     * @param alpha First z-axis rotation angle
     * @param beta  x-axis rotation angle
     * @param gamma Second z-axis rotation angle
     * @return A list containing [rotation matrix, inverse rotation matrix]
     */
    public static double[][] createRotationMatrix(int N, double alpha, double beta, double gamma) {
        double[][] rotationZAlpha = createNxNRotationAlongZAxis(N, alpha);
        double[][] rotationXBeta  = createNxNRotationAlongXAxis(N, beta);
        double[][] rotationZGamma = createNxNRotationAlongZAxis(N, gamma);

        double[][] matrix  = new double[N][N];
        for (int i = 0; i < N; i++) {
            for (int l = 0; l < N; l++) {
                for (int j = 0; j < N; j++) {
                    for (int k = 0; k < N; k++) {
                        matrix[i][l] += rotationZGamma[i][j] * rotationXBeta[j][k] * rotationZAlpha[k][l];
                    }
                }
            }
        }

        return matrix;
    }

    /**
     * Creates a rotation matrix for a given angle around the z-axis.
     * <p>
     * Supports both 3×3 (vector) and 6×6 (tensor) formulations.
     *
     * @param N     Size of the matrix (3 or 6)
     * @param alpha Angle of rotation in radians
     * @return Rotation matrix of size N×N
     */
    public static double[][] createNxNRotationAlongZAxis(int N, double alpha) {
        double cos    = Math.cos(alpha);
        double sin    = Math.sin(alpha);
        double cos2   = cos * cos;
        double sin2   = sin * sin;
        double sincos = sin * cos;

        if (N == 3) {
            double[][] R = new double[][] {
                {cos, -sin, 0},
                {sin,  cos, 0},
                {0,    0,   1}
            };
            return R;
        }
        else {
            double[][] R = new double[][] {
                {cos2,    sin2,   0,  0,   0,  -2 * sincos},
                {sin2,    cos2,   0,  0,   0,   2 * sincos},
                {0,       0,      1,  0,   0,   0         },
                {0,       0,      0,  cos, sin, 0         },
                {0,       0,      0, -sin, cos, 0         },
                {sincos, -sincos, 0,  0,   0,   2*cos2 - 1}
            };
            return R;
        }
    }

    /**
     * Creates a rotation matrix for a given angle around the x-axis.
     * <p>
     * Supports both 3×3 (vector) and 6×6 (tensor) formulations.
     *
     * @param N     Size of the matrix (3 or 6)
     * @param beta  Angle of rotation in radians
     * @return Rotation matrix of size N×N
     */
    public static double[][] createNxNRotationAlongXAxis(int N, double beta) {
        double cos    = Math.cos(beta);
        double sin    = Math.sin(beta);
        double cos2   = cos * cos;
        double sin2   = sin * sin;
        double sincos = sin * cos;

        if (N == 3) {
            double[][] R = new double[][] {
                {1, 0,    0  },
                {0, cos, -sin},
                {0, sin,  cos}
            };
            return R;
        }
        else {
            double[][] R = new double[][] {
                {1, 0,       0,      0,            0,   0  },
                {0, cos2,    sin2,  -2 * sincos,   0,   0  },
                {0, sin2,    cos2,   2 * sincos,   0,   0  },
                {0, sincos, -sincos, 2 * cos2 - 1, 0,   0  },
                {0, 0,       0,      0,            cos, sin},
                {0, 0,       0,      0,           -sin, cos}
            };
            return R;
        }
    }

    /**
     * Creates an identity matrix of size N×N. 
     * <p>
     * Here just to help testing in {@link #test(double, double, double)}.
     *
     * @param N Dimension of the square matrix
     * @return NxN identity matrix
     */
    public static double[][] createIdentityMatrix(int N) {
        double[][] identity = new double[N][N];
        for (int i = 0; i < N; i++) {
            identity[i][i] = 1;
        }
        return identity;
    }

    /**
     * Performs matrix multiplication: C = A × B
     * <p>
     * Here just to help testing in {@link #test(double, double, double)}.
     *
     * @param A Left operand matrix (m × n)
     * @param B Right operand matrix (n × p)
     * @return Product matrix C (m × p)
     */
    public static double[][] performMatrixProduct(double[][] A, double[][] B) {
        int numberOfRowsA = A.length;
        int numberOfColumnsA = A[0].length; // should be same as B.length
        int numberOfColumnsB = B[0].length;
        double[][] C = new double[numberOfRowsA][numberOfColumnsB];
        for (int i = 0; i < numberOfColumnsA; i++) {
            for (int k = 0; k < numberOfColumnsB; k++) {
                for (int j = 0; j < numberOfColumnsA; j++) {
                    C[i][k] += A[i][j] * B[j][k];
                }
            }
        }
        return C;
    }
}
