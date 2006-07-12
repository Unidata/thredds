package ucar.ma2;

import junit.framework.*;

/** Test ma2 get/put methods in the JUnit framework. */

public class TestTxCompose extends TestCase {

  public TestTxCompose( String name) {
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
      Range [] ranges = {new Range(m1, m2), null, null};
      secA = (ArrayDouble) A.section( Range.parseSpec(m1+":"+m2+",:,:"));
    } catch (InvalidRangeException e) {
      fail("testMAsection failed == "+ e);
      return;
    }

  }


  public void testFlip() {
    ArrayDouble flipA = (ArrayDouble) A.flip(1);
    Index ima = flipA.getIndex();

    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = flipA.getDouble(ima.set(i,j,k));
          double myVal = i*100+(n-j-1)*10+k;

          //System.out.println(val+" "+myVal);
          assert (val == myVal);
        }
      }
    }
    System.out.println("testFlip");
  }

  public void testTranspose() {
    ArrayDouble trA = (ArrayDouble) A.transpose(0,1);
    Index ima = trA.getIndex();

    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = trA.getDouble(ima.set(j,i,k));
          double myVal = i*100+j*10+k;

          //System.out.println(val+" "+myVal);
          assert (val == myVal);
        }
      }
    }
    System.out.println("testTranspose");
  }

  public void testPermute() {
    ArrayDouble pA = (ArrayDouble) A.permute(new int[] {2,0,1});
    Index ima = pA.getIndex();

    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = pA.getDouble(ima.set(k,i,j));
          double myVal = i*100+j*10+k;

          //System.out.println(val+" "+myVal);
          assert (val == myVal);
        }
      }
    }
    System.out.println("testPermute");
  }

  public void testCompose() {
    ArrayDouble trA = (ArrayDouble) A.transpose(0,1);
    ArrayDouble composeA = (ArrayDouble) trA.flip(1);
    Index ima = composeA.getIndex();

    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = composeA.getDouble(ima.set(j,i,k));
          double myVal = (m-i-1)*100+j*10+k;

          //System.out.println(val+" "+myVal);
          assert (val == myVal);
        }
      }
    }
    System.out.println("testCompose transpose/flip");
  }

  public void testCompose2() {
    ArrayDouble flipA = (ArrayDouble) A.flip(1);
    ArrayDouble composeA = (ArrayDouble) flipA.transpose(0,1);
    Index ima = composeA.getIndex();

    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = composeA.getDouble(ima.set(j,i,k));
          double myVal = i*100+(n-j-1)*10+k;

          //System.out.println(val+" "+myVal);
          assert (val == myVal);
        }
      }
    }
    System.out.println("testCompose flip/transpose");
  }

  public void testCompose3() {
    ArrayDouble secA2;
    int ival = 2;

    try {
      secA2 = (ArrayDouble) A.section( Range.parseSpec(ival+",:,:") );
    } catch (InvalidRangeException e) {
      System.out.println("testDoubleRange2 failed == "+ e);
      return;
    }

    ArrayDouble composeA = (ArrayDouble) secA2.permute(new int[]{1,0});
    Index ima = composeA.getIndex();

      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = composeA.getDouble(ima.set(k,j));
          double myVal = ival*100+j*10+k;
          assert (val == myVal);
        }
      }
    System.out.println("testCompose section/permute");
  }

}
