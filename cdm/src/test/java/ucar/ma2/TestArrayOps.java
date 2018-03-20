/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/** Test ma2 get/put methods in the JUnit framework. */

public class TestArrayOps extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestArrayOps( String name) {
    super(name);
  }

  int m = 4, n = 3, p = 2;
  int [] sA = { m, n, p };
  ArrayDouble A = new ArrayDouble(sA);
  int i,j,k;
  Index ima = A.getIndex();

  public void setUp() {

    // write
    int count = 0;
    for (i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        for (k=0; k<p; k++) {
          A.setDouble(ima.set2(k), (double) (count++));
        }
      }
    }
  }

  public void testReshape() {
    System.out.println("test reshape");

    Array Ar = A.reshape( new int[] {4, 6} );
    IndexIterator ita = Ar.getIndexIterator();
    int count = 0;
    while (ita.hasNext())
      assertEquals(ita.getDoubleNext(), (double) (count++), 1.0E-10);

    try {
      Ar = A.reshape( new int[] {12} );
      assert (false);
    } catch (IllegalArgumentException e) {
      assert (true);
    }

    Ar = A.reshape( new int[] {24} );
    ita = Ar.getIndexIterator();
    count = 0;
    while (ita.hasNext())
      assertEquals(ita.getDoubleNext(), (double) (count++), 1.0E-10);

    Ar = A.reshape( new int[] {2,2,3,2} );
    ita = Ar.getIndexIterator();
    count = 0;
    while (ita.hasNext())
      assertEquals(ita.getDoubleNext(), (double) (count++), 1.0E-10);
  }

}
