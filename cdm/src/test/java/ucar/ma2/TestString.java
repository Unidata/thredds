/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/** Test string access methods in the JUnit framework. */

public class TestString extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestString( String name) {
    super(name);
  }

  int m = 4, n = 3;
  int [] sA = { m, n };

  ArrayChar A = new ArrayChar(sA);
  int i,j,k;
  Index ima = A.getIndex();

  public void setUp() {

    // write
    for (i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        A.setChar(ima, 'a');
      }
    }
  }

  public void testRegular() {
    System.out.println("testString: testRegular");

    // read
    for (i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        char val = A.getChar(ima);
        assert (val == 'a');
      }
    }
  }

  public void testString() {
    System.out.println("testString: testString");

    // read
    for (i=0; i<m; i++) {
      ima.set0(i);
      String val = A.getString( ima);
      assert( val.equals("aaa"));
    }
  }

  public void testStringPutGet() {
    System.out.println("testString: testStringPutGet");

    // write
    ima.set0(0);
    A.setString(ima, "hey");
    ima.set0(1);
    A.setString(ima, "there");
    ima.set0(2);
    A.setString(ima, "yo");
    ima.set0(3);
    A.setString(ima, "I");

    // read
    ima.set0(0);
    assert(A.getString(ima).equals("hey"));
    ima.set0(1);
    assert(A.getString(ima).equals("the"));
    ima.set0(2);
    assert(A.getString(ima).equals("yo"));
    ima.set0(3);
    assert(A.getString(ima).equals("I"));
  }

}
