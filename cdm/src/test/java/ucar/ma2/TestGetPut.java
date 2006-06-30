package ucar.ma2;

import junit.framework.*;

/** Test ma2 get/put methods in the JUnit framework. */

public class TestGetPut extends TestCase {

  public TestGetPut( String name) {
    super(name);
  }

  int m = 4, n = 3, p = 2;
  int [] sA = { m, n, p };
  int stride1 = p;
  int stride2 = n * p;

  ArrayDouble A = new ArrayDouble(sA);
  int i,j,k;
  Index ima = A.getIndex();

  public void setUp() {

    // write
    for (i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        for (k=0; k<p; k++) {
          A.setDouble(ima.set2(k), (double) (i*1000000+j*1000+k));
        }
      }
    }
  }


  public void testGetPut() {
    System.out.println("test Set/Get:  seti()");

    // read
    for (i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        for (k=0; k<p; k++) {
          double val = A.getDouble(ima.set2(k));
          assert (val == i*1000000+j*1000+k);
        }
      }
    }
  }

  public void testDim() {
    System.out.println("test Set/Get: setDim() ");

    Index index = A.getIndex();
    for (i=0; i<m; i++) {
      index.setDim(0,i);
      for (j=0; j<n; j++) {
        index.setDim(1,j);
        for (k=0; k<p; k++) {
          index.setDim(2,k);
          double val = A.getDouble(index);
          assert (val == i*1000000+j*1000+k);
        }
      }
    }
  }


  public void testIndexSet3() {
    System.out.println("test Set/Get: set(i,j,k) ");

    Index index = A.getIndex();
    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = A.getDouble(index.set(i,j,k));
          assert (val == i*1000000+j*1000+k);
        }
      }
    }
  }


  public void testIter() {
    System.out.println("test Set/Get: IndexIterator ");

    IndexIterator iter = A.getIndexIterator();
    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = iter.getDoubleNext();
          //System.out.println( val+ " "+ (i*1000000+j*1000+k));
          assert (val == i*1000000+j*1000+k);
        }
      }
    }
  }

}
