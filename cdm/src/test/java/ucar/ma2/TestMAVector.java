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

public class TestMAVector extends TestCase {

  public TestMAVector(String name) {
    super(name);
  }

  public void testDot() {
    System.out.println("testDot");

    MAVector v1 = new MAVector(5);
    MAVector v2 = new MAVector(5);

    for (int i=0; i<5; i++) {
      v1.setDouble(i, 1.0);
      v2.setDouble(i, 1.0);
    }

    assert( tolerance( v1.dot(v2) - 5.0));
    assert( tolerance( v1.norm() - Math.sqrt(5.0)) );
    v1.normalize();
    assert( tolerance( v1.norm() - 1.0) );
  }

  public void testCos() {
    System.out.println("testVectorCos");

    MAVector v1 = new MAVector(5);
    MAVector v2 = new MAVector(5);

    for (int i=0; i<5; i++) {
      v1.setDouble(i, 1.0);
      v2.setDouble(i, 1.0);
    }
    //System.out.println(v1.cos(v2)+" "+v1.dot(v2)+" "+v1.norm()+" "+v2.norm());
    assert( tolerance( v1.cos(v2) - 1.0));

    MAVector v3 = new MAVector(2);
    MAVector v4 = new MAVector(2);

    v3.setDouble(0, 1.0);
    v3.setDouble(1, 1.0);

    v4.setDouble(0, 1.0);
    v4.setDouble(1, -1.0);
    assert( tolerance( v3.cos(v4)));

    v4.setDouble(0, -1.0);
    v4.setDouble(1, -1.0);
    assert( tolerance( v3.cos(v4) + 1.0));

    v4.setDouble(0, -1.0);
    v4.setDouble(1, 1.0);
    assert( tolerance( v3.cos(v4)));

  }

  private boolean tolerance( double val) {
    return Math.abs(val) < 1.0e-10;
  }


}