package ucar.ma2;

import junit.framework.*;

/** Test ma2 section methods in the JUnit framework. */

public class TestIterator extends TestCase {

  public TestIterator( String name) {
    super(name);
  }

  int m = 4, n = 3, p = 2;
  int [] sA = { m, n, p };
  int stride1 = p;
  int stride2 = n * p;

  ArrayDouble A = new ArrayDouble(sA);
  Index ima;
  int i,j,k;

  ArrayDouble secA;
  int m1 = 2;
  int m2 = 3;
  int mlen = (m2 - m1 + 1);

  public void setUp() {
    ima = A.getIndex();

    // write
    for (int i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        for (k=0; k<p; k++) {
          ima.set2(k);
          A.setDouble(ima, (double) (i*100+j*10+k));
        }
      }
    }

  }

  public void testFastIter() {

    int count = 0;
    IndexIterator iter = A.getIndexIterator();
    //System.out.println(iter);
    while(iter.hasNext()) {
      iter.getDoubleNext();
      count++;
    }
    assert(count == n*m*p);

    iter = A.getIndexIterator();
    for (i=0; i<m; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = iter.getDoubleNext();
          double myVal = (double) (i*100+j*10+k);
          assert (val == myVal);
        }
      }
    }
  }


  public void testScalerIter() {

    ArrayDouble.D0 scalar = new ArrayDouble.D0();
    scalar.set( 10.0);

    int count = 0;
    double sum = 0;
    IndexIterator iter = scalar.getIndexIterator();
    while(iter.hasNext()) {
      sum += iter.getDoubleNext();
      count++;
    }
    assert(count == 1);
    assert(sum == 10.0);

    System.out.println(" testScalerIter");
  }

  public void testSectionIter() {

    try {
      secA = (ArrayDouble) A.section( Range.parseSpec(m1+":"+m2+",:,:") );
    } catch (InvalidRangeException e) {
      System.out.println("testMAsection failed == "+ e);
      return;
    }

    int count = 0;
    IndexIterator iter = secA.getIndexIterator();
    //System.out.println(iter);
    while(iter.hasNext()) {
      iter.getDoubleNext();
      count++;
    }
    assert(count == n*mlen*p);

    iter = secA.getIndexIterator();
    for (i=0; i<mlen; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = iter.getDoubleNext();
          double myVal = (double) (i+m1)*100+j*10+k;
          assert (val == (i+m1)*100+j*10+k);
        }
      }
    }
  }


  public void testSectionRank2() {
    ArrayDouble secA2;
    try {
      secA2 = (ArrayDouble) A.section( Range.parseSpec("2,:,:") );
    } catch (InvalidRangeException e) {
      System.out.println("testMAsectionrank2 failed == "+ e);
      return;
    }

    IndexIterator iter = secA2.getIndexIterator();
    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          double val = iter.getDoubleNext();
          //double myVal = (double) (i+m1)*100+j*10+k;
          //System.out.println(val+ " "+myVal);
          assert (val == 200+i*10+j);
      }
    }
  }

}
