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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.*;

/** Test ma2 section methods in the JUnit framework. */

public class TestMAMath {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private int m = 4, n = 3, p = 2;
  private ArrayDouble A = new ArrayDouble(new int[] {m, n, p});

  ArrayDouble secA;
  int m1 = 2;
  int m2 = 3;


  @Before
  public void setUp() {
    Index ima = A.getIndex();

    // write
    for (int i=0; i<m; i++) {
      for (int j=0; j<n; j++) {
        for (int k=0; k<p; k++) {
          A.setDouble(ima.set(i,j,k), (double) (i*100+j*10+k));
        }
      }
    }

    // section
    try {
      secA = (ArrayDouble) A.section( new Section(m1+":"+m2+",:,:").getRanges());
    } catch (InvalidRangeException e) {
      fail("testMAsection failed == " + e);
    }
  }

  @Test
  public void testConformable() {
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

  @Test
  public void testSet() {
    try {
      MAMath.setDouble(A, 1.0);

      assert( MAMath.sumDouble(A) == ((double) m*n*p));
    } catch (Exception e) {
      fail("testSet Exception" + e);
    }
    try {
      int mlen = (m2 - m1 + 1);
      assert( MAMath.sumDouble(secA) == ((double) mlen*n*p));
    } catch (Exception e) {
      fail("testSet2 Exception" + e);
    }
  }

  @Test
  public void testAdd() {
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
      }
    } catch (Exception e) {
      fail("testAdd Exception" + e);
    }
  }

  @Test
  public void testSectionCopy() {
    int dim0 = 2;
    int dim1 = 3;
    int dim2 = 4;
    int [] shape = {dim0, dim1, dim2};
    ArrayDouble A = new ArrayDouble(shape);

    try {
      // test section
      MAMath.setDouble(A, 1.0);
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

      List<Range> ranges = new Section(":,:,1").getRanges();
      Array secA = A.section(ranges);
      MAMath.setDouble(secA, 0.0);

      assert( MAMath.sumDouble(secA) == 0.0);
      assert( MAMath.sumDouble(A) == (double) (dim0*dim1*dim2 - dim0*dim1));

      // test copy
      MAMath.setDouble(A, 1.0);
      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

      Array copyA = A.section(ranges).copy();
      MAMath.setDouble(copyA, 0.0);

      assert( MAMath.sumDouble(A) == ((double) dim0*dim1*dim2));

    } catch (InvalidRangeException e) {
      fail("testSectionCopy InvalidRangeException " + e);
    } catch (Exception e) {
      fail("testSectionCopy Exception" + e);
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
  @Test
  public void testSectionIterator() throws InvalidRangeException, IOException {
    Array data = A;

    Section section = new Section("1,:,:");
    Array datasection = data.sectionNoReduce(section.getRanges());

    double sum = MAMath.sumDouble(datasection);
    assert sum == 663.0;
  }


  @Test
  public void equalsAndHashCode() throws InvalidRangeException {
    // Create 3 different arrays that are deemed equal, even though they have different backing arrays and Indexes.

    // Index2D[shape={2,10},stride={10,1},current={0,0},offset=0]
    Index2D indexX = new Index2D(new int[]{ 2, 10 });

    int[] storageX = new int[] {
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39
    };
    Array arrayX = Array.factory(DataType.INT, indexX, storageX);


    // Index[shape={2,10},stride={20,2},current={0,0},offset=0]
    Index indexY = new Index2D(new int[] { 2, 20 });
    indexY = indexY.section(Lists.newArrayList(null, new Range(0, 19, 2)));

    int[] storageY = new int[]{
            20, 0, 21, 0, 22, 0, 23, 0, 24, 0, 25, 0, 26, 0, 27, 0, 28, 0, 29, 0,
            30, 0, 31, 0, 32, 0, 33, 0, 34, 0, 35, 0, 36, 0, 37, 0, 38, 0, 39, 0
    };
    Array arrayY = Array.factory(DataType.INT, indexY, storageY);


    // Index[shape={2,10},stride={10,1},current={0,0},offset=20]
    Index indexZ = new Index3D(new int[]{3, 2, 10});
    indexZ = indexZ.section(Lists.newArrayList(new Range(1, 1), null, null));  // First dim will be reduce()d.

    int[] storageZ = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59
    };
    Array arrayZ = Array.factory(DataType.INT, indexZ, storageZ);


    // Reflexive
    assertTrue(MAMath.equals(arrayX, arrayX));
    assertTrue(MAMath.equals(arrayY, arrayY));
    assertTrue(MAMath.equals(arrayZ, arrayZ));

    // Symmetric
    assertTrue(MAMath.equals(arrayX, arrayY));
    assertTrue(MAMath.equals(arrayY, arrayX));

    assertTrue(MAMath.equals(arrayY, arrayZ));
    assertTrue(MAMath.equals(arrayZ, arrayY));

    // Transitive
    assertTrue(MAMath.equals(arrayX, arrayZ));
    assertTrue(MAMath.equals(arrayZ, arrayX));

    // Consistent
    assertTrue(MAMath.equals(arrayX, arrayY));   // We've already tested this.

    // Null
    assertFalse(MAMath.equals(arrayX, null));

    // Here we'd like to show that two Arrays can be equal even if their Indexes are not. This is because when
    // determining Array equality, we only care about the VIEW of the data that an Array presents, not its internals.
    //
    // Since Index doesn't override Object.equals(), this is just testing reference equality.
    // TODO: We want to test value equality here; right now these assertions are pretty trivial.
    assertNotEquals(indexX, indexY);
    assertNotEquals(indexX, indexZ);
    assertNotEquals(indexY, indexZ);


    // hashCode
    assertEquals(MAMath.hashCode(arrayX),MAMath.hashCode(arrayY));
    assertEquals(MAMath.hashCode(arrayY), MAMath.hashCode(arrayZ));
    assertEquals(MAMath.hashCode(arrayX), MAMath.hashCode(arrayZ));

    // Null
    assertEquals(0, MAMath.hashCode(null));
  }

  @Test
  public void convertUnsigned() {
    Array unsignedBytes = Array.makeFromJavaArray(new byte[]  { 12, (byte) 155, -32 });
    Array widenedBytes  = Array.makeFromJavaArray(new short[] { 12, 155,        224 });
    assertTrue(MAMath.equals(widenedBytes, MAMath.convertUnsigned(unsignedBytes)));

    Array unsignedShorts = Array.makeFromJavaArray(new short[] { 3251, (short) 40000, -22222 });
    Array widenedShorts  = Array.makeFromJavaArray(new int[]   { 3251, 40000,         43314  });
    assertTrue(MAMath.equals(widenedShorts, MAMath.convertUnsigned(unsignedShorts)));

    Array unsignedInts = Array.makeFromJavaArray(new int[]  { 123456, (int) 3500000000L, -5436271    });
    Array widenedInts  = Array.makeFromJavaArray(new long[] { 123456, 3500000000L,       4289531025L });
    assertTrue(MAMath.equals(widenedInts, MAMath.convertUnsigned(unsignedInts)));

    Array unsignedLongs = Array.makeFromJavaArray(new long[] {
        // LONG.MAX_VALUE = 9223372036854775807
        3372036854775L, new BigInteger("10000000000000000000").longValue(), -123456789012345L
    });

    Array widenedLongs = ArrayObject.factory(DataType.OBJECT, BigInteger.class, false, Index.factory(new int[] {3}));
    widenedLongs.setObject(0, BigInteger.valueOf(3372036854775L));
    widenedLongs.setObject(1, new BigInteger("10000000000000000000"));
    widenedLongs.setObject(2, new BigInteger("18446620616920539271"));

    assertTrue(MAMath.equals(widenedLongs, MAMath.convertUnsigned(unsignedLongs)));
  }
}
