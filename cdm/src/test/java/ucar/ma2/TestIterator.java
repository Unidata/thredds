/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;

import java.lang.invoke.MethodHandles;

/** Test ma2 section methods in the JUnit framework. */

public class TestIterator {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  @Before
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

  @Test
  public void testFastIter() {
    int count = 0;
    IndexIterator iter = A.getIndexIterator();
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
          Assert2.assertNearlyEquals(val, myVal);
        }
      }
    }
  }


  @Test
  public void testSlowIter() {

    int count = 0;
    Index index = A.getIndex();
    IndexIterator iter = index.getSlowIndexIterator(A);

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
          Assert2.assertNearlyEquals(val, myVal);
        }
      }
    }
  }


  @Test
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
  }

  @Test
  public void testSectionIter() throws InvalidRangeException {
    secA = (ArrayDouble) A.section( new Section(m1+":"+m2+",:,:").getRanges() );

    int count = 0;
    IndexIterator iter = secA.getIndexIterator();

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


  @Test
    public void testSectionRank2() throws InvalidRangeException {
      ArrayDouble secA2 = (ArrayDouble) A.section( new Section("2,:,:").getRanges() );

      IndexIterator iter = secA2.getIndexIterator();
      for (i=0; i<n; i++) {
        for (j=0; j<p; j++) {
            double val = iter.getDoubleNext();
            //double myVal = (double) (i+m1)*100+j*10+k;
            //logger.debug(val+ " "+myVal);
            assert (val == 200+i*10+j);
        }
      }
    }

  @Test
  public void testArrayObjectIter() throws InvalidRangeException {
    Array arrayObject = Array.factory(DataType.OPAQUE, new int[] {30});
    testArrayObjectIter(arrayObject, 30);

    Array arrayObjectSubset = arrayObject.sectionNoReduce(new Section("1:20").getRanges()); // subset it
    testArrayObjectIter(arrayObjectSubset, 20);

    Array arrayObjectSubsetCopy = arrayObjectSubset.copy();
    testArrayObjectIter(arrayObjectSubsetCopy, 20);
  }

  private void testArrayObjectIter(Array a, int expected) {
    int count = 0;
    IndexIterator iter = a.getIndexIterator(); // fast iterator
    while (iter.hasNext()) {
      Object result = iter.next();
      count++;
    }
    assert (count == expected);

    Index index = a.getIndex();
    IndexIterator iter2 = index.getSlowIndexIterator(a); // slow iterator
    count = 0;
    while (iter2.hasNext()) {
      Object result = iter2.next();
      count++;
    }
    assert (count == expected);
  }
}
