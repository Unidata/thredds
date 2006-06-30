package ucar.ma2;

import junit.framework.*;

public class TestMAMatrix extends TestCase {

  public TestMAMatrix(String name) {
    super(name);
  }

  private int n = 5, m = 10;

  public void testDot() {
    System.out.println("testMatrixDot");

    MAVector v1 = new MAVector(n);
    for (int i=0; i<n; i++) {
      v1.setDouble(i, 1.0);
    }

    MAMatrix M = new MAMatrix(m,n);
    for (int i=0; i<m; i++) {
      for (int j=0; j<n; j++) {
        M.setDouble(i, j, 1.0);
      }
    }

    MAVector v2 = M.dot( v1);

    assert( tolerance( v2.norm() - Math.sqrt(m*n*n)) );
  }

  public void testMultiply() {
    System.out.println("testMatrixMultiply");

    MAMatrix M1 = new MAMatrix(2,3);
    MAMatrix M2 = new MAMatrix(3,2);
    for (int i=0; i<2; i++) {
      for (int j=0; j<3; j++) {
        M1.setDouble(i, j, (double) i+1);
        M2.setDouble(j, i, (double) i+1);
      }
    }

    MAMatrix r = MAMatrix.multiply( M1, M2);

    assert( tolerance( r.getDouble(0,0) - 3.0) );
    assert( tolerance( r.getDouble(0,1) - 6.0) );
    assert( tolerance( r.getDouble(1,0) - 6.0) );
    assert( tolerance( r.getDouble(1,1) - 12.0) );
  }

  public void testTranspose() {
    System.out.println("testMatrixTranspose");

    MAMatrix M1 = new MAMatrix(2,3);
    MAMatrix M2 = new MAMatrix(3,2);
    for (int i=0; i<2; i++) {
      for (int j=0; j<3; j++) {
        M1.setDouble(i, j, (double) i+j);
        M2.setDouble(j, i, (double) i+j);
      }
    }

    MAMatrix t = M2.transpose();
    assert( tolerance( t.getDouble(0,0) - M1.getDouble(0,0)) );
    assert( tolerance( t.getDouble(0,1) - M1.getDouble(0,1)) );
    assert( tolerance( t.getDouble(0,2) - M1.getDouble(0,2)) );
    assert( tolerance( t.getDouble(1,0) - M1.getDouble(1,0)) );
    assert( tolerance( t.getDouble(1,1) - M1.getDouble(1,1)) );
    assert( tolerance( t.getDouble(1,2) - M1.getDouble(1,2)) );
  }

  public void testDiag() {
    System.out.println("testMatrixMultiplyDiagonal");

    MAMatrix M1 = new MAMatrix(2,3);
    for (int i=0; i<2; i++) {
      for (int j=0; j<3; j++) {
        M1.setDouble(i, j, i+j+1.0);
      }
    }

    MAVector v1 = new MAVector(3);
    for (int i=0; i<3; i++) {
      v1.setDouble(i, i+1.0);
    }

    M1.postMultiplyDiagonal(v1);
    assert( tolerance( 1.0 - M1.getDouble(0,0)) );
    assert( tolerance( 4.0 - M1.getDouble(0,1)) );
    assert( tolerance( 9.0 - M1.getDouble(0,2)) );
    assert( tolerance( 2.0 - M1.getDouble(1,0)) );
    assert( tolerance( 6.0 - M1.getDouble(1,1)) );
    assert( tolerance( 12.0 - M1.getDouble(1,2)) );

    MAVector v2 = new MAVector(2);
    for (int i=0; i<2; i++) {
      v2.setDouble(i, i+2.0);
    }

    M1.preMultiplyDiagonal(v2);
    assert( tolerance( 2.0 - M1.getDouble(0,0)) );
    assert( tolerance( 8.0 - M1.getDouble(0,1)) );
    assert( tolerance( 18.0 - M1.getDouble(0,2)) );
    assert( tolerance( 6.0 - M1.getDouble(1,0)) );
    assert( tolerance( 18.0 - M1.getDouble(1,1)) );
    assert( tolerance( 36.0 - M1.getDouble(1,2)) );

    try {
      M1.preMultiplyDiagonal(v1);
      assert(false);
    } catch (IllegalArgumentException e) {;}

    try {
      M1.postMultiplyDiagonal(v2);
      assert(false);
    } catch (IllegalArgumentException e) {;}

  }

  public void testProjection() {
    System.out.println("testMatrixProjection");

    MAMatrix M1 = new MAMatrix(2,3);
    for (int i=0; i<2; i++) {
      for (int j=0; j<3; j++) {
        M1.setDouble(i, j, i+j+1.0);
      }
    }

    MAVector v1 = M1.row(1);
    assert( tolerance( 2.0 - v1.getDouble(0)) );
    assert( tolerance( 3.0 - v1.getDouble(1)) );
    assert( tolerance( 4.0 - v1.getDouble(2)) );

    MAVector v2 = M1.column(2);
    assert( tolerance( 3.0 - v2.getDouble(0)) );
    assert( tolerance( 4.0 - v2.getDouble(1)) );
  }

  private boolean tolerance( double val) {
    return Math.abs(val) < 1.0e-10;
  }
}