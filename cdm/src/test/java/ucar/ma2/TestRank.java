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

public class TestRank extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestRank( String name) {
    super(name);
  }

  public void doit(int rank) {
    System.out.println("testRank "+rank);

    int [] shape = new int[rank];
    for (int i=0; i<rank; i++)
      shape[i] = i + 2;

    ArrayDouble A = new ArrayDouble(shape);

    IndexIterator iter = A.getIndexIterator();
    int count = 0;
    while (iter.hasNext())
      iter.setDoubleNext( (double) count++);

    iter = A.getIndexIterator();
    count = 0;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      assert (val == ((double) count++));
    }

    Index ima = A.getIndex();
    for (int i=0; i<rank; i++)
      ima.setDim(i,i+1);
    double val = A.getDouble(ima);
    double myVal = ima.currentElement();
    assert(val == myVal);
  }

  public void testRank() {
    for (int rank = 0; rank<10; rank++)
      doit(rank);
  }

}
