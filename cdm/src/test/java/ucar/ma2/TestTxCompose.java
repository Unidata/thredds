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
