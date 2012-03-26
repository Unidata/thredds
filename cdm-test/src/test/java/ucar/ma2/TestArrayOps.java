/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.ma2;

import junit.framework.*;

/** Test ma2 get/put methods in the JUnit framework. */

public class TestArrayOps extends TestCase {

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