package ucar.ma2;

import junit.framework.*;
import java.util.List;

/** Test ma2 section methods in the JUnit framework. */

public class TestMAMath extends TestCase {

  public TestMAMath( String name) {
    super(name);
  }

  int m = 4, n = 3, p = 2;
  int [] sA = { m, n, p };
  int stride1 = p;
  int stride2 = n * p;

  ArrayDouble A = new ArrayDouble(sA);
  int i,j,k;

  ArrayDouble secA;
  int m1 = 2;
  int m2 = 3;
  int mlen = (m2 - m1 + 1);

  public void setUp() {
    Index ima = A.getIndex();

    // write
    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          A.setDouble(ima.set(i,j,k), (double) (i*100+j*10+k));
        }
      }
    }

      // section
    try {
      secA = (ArrayDouble) A.section( Range.parseSpec(m1+":"+m2+",:,:"));
    } catch (InvalidRangeException e) {
      fail("testMAsection failed == "+ e);
      return;
    }

  }

  public void testConformable() {
    System.out.println("test MAMath conformable ");

    int [] a1 = {1, 3, 1, 5, 1, 1};
    int [] b1 = {3, 5};
    assert( MAMath.conformable(b1, a1));
    assert( MAMath.conformable(a1, b1));

    int [] a2 = {1, 3, 1, 5, 1, 1};
    int [] b2 = {1, 5, 3};
    assert( !MAMath.conformable(b2, a2));
    assert( !MAMath.conformable(a2, b2));

    int [] a3 = {1, 3, 1, 5, 1, 1};
    int [] b3 = {3, 5, 1, 1, 1, 2};
    assert( !MAMath.conformable(b3, a3));
    assert( !MAMath.conformable(a3, b3));

    int [] a4 = {1, 3, 1, 5, 1, 1, 7};
    int [] b4 = {1, 1, 3, 5, 1, 7, 1, 1};
    assert( MAMath.conformable(b4, a4));
    assert( MAMath.conformable(a4, b4));
  }

  public void testSet() {
    System.out.println("test MAMath set/sum ");

    try {
      MAMath.setDouble(A, 1.0);
      //System.out.println(MAMath.sumDouble(A)+ " "+ (m*n*p));
      assert( MAMath.sumDouble(A) == ((double) m*n*p));
    } catch (Exception e) {
      fail("testSet Exception"+e);
    }
    try {
      assert( MAMath.sumDouble(secA) == ((double) mlen*n*p));
    } catch (Exception e) {
      fail("testSet2 Exception"+e);
    }
  }

  public void testAdd() {
    System.out.println("test MAMath add ");

    int dim0 = 2; int dim1 = 3;
    int [] shape = {dim0, dim1};
    ArrayDouble A = new ArrayDouble(shape);
    ArrayDouble B = new ArrayDouble(shape);

    try {
      MAMath.setDouble(A, 1.0);
      MAMath.setDouble(B, 22.0F);
      MAMath.addDouble(A, A, A);
      Array result = MAMath.add( A, B);

      IndexIterator iterR = result.getIndexIterator();
      while (iterR.hasNext()) {
        double vala = iterR.getDoubleNext();
        assert( vala == 24.0);
        //System.out.println(vala);
      }
    } catch (Exception e) {
      fail("testAdd Exception"+e);
    }
  }

  public void testSectionCopy() {
    System.out.println("test MAMath section copy ");

    int dim0 = 2;
    int dim1 = 3;
    int dim2 = 4;
    int [] shape = {dim0, dim1, dim2};
    ArrayDouble A = new ArrayDouble(shape);

    try {
        // test section
      MAMath.setDouble(A, 1.0);
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

      List ranges = Range.parseSpec(":,:,1");
      Array secA = A.section(ranges);
      MAMath.setDouble(secA, 0.0);

      assert( MAMath.sumDouble(secA) == 0.0);
      assert( MAMath.sumDouble(A) == (double) (dim0*dim1*dim2 - dim0*dim1));

        // test copy
      MAMath.setDouble(A, 1.0);
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

      Array copyA = A.section(ranges).copy();
      MAMath.setDouble(copyA, 0.0);

      //System.out.println(MAMath.sumDouble(A)+ " "+ (dim0*dim1*dim2));
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

    } catch (InvalidRangeException e) {
      fail("testSectionCopy InvalidRangeException "+e);
    } catch (Exception e) {
      fail("testSectionCopy Exception"+e);
    }
  }


}
