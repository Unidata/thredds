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
import java.util.List;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.PrintStream;

import ucar.nc2.util.CancelTask;
import ucar.nc2.NCdump;
import ucar.nc2.NCdumpW;

/** Test ma2 section methods in the JUnit framework. */

public class TestMAMath extends TestCase {

  public TestMAMath( String name) {
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
      secA = (ArrayDouble) A.section( new Section(m1+":"+m2+",:,:").getRanges());
    } catch (InvalidRangeException e) {
      fail("testMAsection failed == "+ e);
      return;
    }

  }

  public void testConformable() {
    System.out.println("test MAMath conformable ");

    int [] a1 = {1, 3, 1, 5, 1, 1};
    int [] b1 = {3, 5};
    assert( MAMath.conformable(b1, a1));
    assert( MAMath.conformable(a1, b1));

    int [] a2 = {1, 3, 1, 5, 1, 1};
    int [] b2 = {1, 5, 3};
    assert( !MAMath.conformable(b2, a2));
    assert( !MAMath.conformable(a2, b2));

    int [] a3 = {1, 3, 1, 5, 1, 1};
    int [] b3 = {3, 5, 1, 1, 1, 2};
    assert( !MAMath.conformable(b3, a3));
    assert( !MAMath.conformable(a3, b3));

    int [] a4 = {1, 3, 1, 5, 1, 1, 7};
    int [] b4 = {1, 1, 3, 5, 1, 7, 1, 1};
    assert( MAMath.conformable(b4, a4));
    assert( MAMath.conformable(a4, b4));
  }

  public void testSet() {
    System.out.println("test MAMath set/sum ");

    try {
      MAMath.setDouble(A, 1.0);
      //System.out.println(MAMath.sumDouble(A)+ " "+ (m*n*p));
      assert( MAMath.sumDouble(A) == ((double) m*n*p));
    } catch (Exception e) {
      fail("testSet Exception"+e);
    }
    try {
      assert( MAMath.sumDouble(secA) == ((double) mlen*n*p));
    } catch (Exception e) {
      fail("testSet2 Exception"+e);
    }
  }

  public void testAdd() {
    System.out.println("test MAMath add ");

    int dim0 = 2; int dim1 = 3;
    int [] shape = {dim0, dim1};
    ArrayDouble A = new ArrayDouble(shape);
    ArrayDouble B = new ArrayDouble(shape);

    try {
      MAMath.setDouble(A, 1.0);
      MAMath.setDouble(B, 22.0F);
      MAMath.addDouble(A, A, A);
      Array result = MAMath.add( A, B);

      IndexIterator iterR = result.getIndexIterator();
      while (iterR.hasNext()) {
        double vala = iterR.getDoubleNext();
        assert( vala == 24.0);
        //System.out.println(vala);
      }
    } catch (Exception e) {
      fail("testAdd Exception"+e);
    }
  }

  public void testSectionCopy() {
    System.out.println("test MAMath section copy ");

    int dim0 = 2;
    int dim1 = 3;
    int dim2 = 4;
    int [] shape = {dim0, dim1, dim2};
    ArrayDouble A = new ArrayDouble(shape);

    try {
        // test section
      MAMath.setDouble(A, 1.0);
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

      List<Range> ranges = Range.parseSpec(":,:,1");
      Array secA = A.section(ranges);
      MAMath.setDouble(secA, 0.0);

      assert( MAMath.sumDouble(secA) == 0.0);
      assert( MAMath.sumDouble(A) == (double) (dim0*dim1*dim2 - dim0*dim1));

        // test copy
      MAMath.setDouble(A, 1.0);
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

      Array copyA = A.section(ranges).copy();
      MAMath.setDouble(copyA, 0.0);

      //System.out.println(MAMath.sumDouble(A)+ " "+ (dim0*dim1*dim2));
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

    } catch (InvalidRangeException e) {
      fail("testSectionCopy InvalidRangeException "+e);
    } catch (Exception e) {
      fail("testSectionCopy Exception"+e);
    }
  }

  /*
    barronh@gmail.com
    MAMath uses getIndexIteratorFast when it should use getIndexIterator.
    getIndexIterator will use getIndexIteratorFast when appropriate. This
    causes bugs with data_sections.

    Array data = <some data>;
    Section section = <some time slice>;
    Array data_section = data.sectionNoReduce(section.getRanges();
    Double test = MAMath.sumDouble(datasection);

    The value of test will always be equal to the sum of the first data
    slice. */

  public void testSectionIterator() throws InvalidRangeException, IOException {
    Array data = A;
    NCdumpW.printArray(A, "full", new PrintWriter(System.out), null);

    Section section = new Section("1,:,:");
    Array datasection = data.sectionNoReduce(section.getRanges());
    NCdumpW.printArray(datasection, "section", new PrintWriter(System.out), null);

    double sum = MAMath.sumDouble(datasection);
    System.out.printf(" sum=%f%n ", sum);
    assert sum == 663.0;
  }


}
