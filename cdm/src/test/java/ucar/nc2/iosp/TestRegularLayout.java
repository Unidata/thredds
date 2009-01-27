// $Id: RegularIndexer.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.iosp;

import ucar.ma2.*;
import junit.framework.TestCase;

/**
 * Compare RegularLayout to RegularInderer
 */
public class TestRegularLayout extends TestCase {
  RegularLayout layout;
  RegularIndexer index;

  public TestRegularLayout( String name) {
    super(name);  
  }

  /**
   * Constructor.
   *
   * @param startPos starting address of the entire data array.
   * @param elemSize size of on element in bytes.
   * @param recSize  if > 0, then size of outer stride in bytes, else ignored
   * @param varShape shape of the entire data array.
   * @param want     the wanted section of data, contains a List of Range objects,
   *                 corresponding to each Dimension, else null means all.
   * @throws InvalidRangeException is ranges are misformed
   */
  private TestRegularLayout(long startPos, int elemSize, int recSize, int[] varShape, Section want) throws InvalidRangeException {

    // clean up and check want Section
    if (want == null)
      want = new Section(varShape);
    else
      want.setDefaults(varShape);
    String err = want.checkInRange(varShape);
    if (err != null) {
      printa("section= "+want+"\nvarShape=",varShape);
      throw new InvalidRangeException(err);
    }

    // public RegularLayout(long startPos, int elemSize, int recSize, int[] varShape, List<Range> rangeList) throws InvalidRangeException {
    layout = new RegularLayout(startPos, elemSize, recSize, varShape, want);

    //   public RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize) throws InvalidRangeException {
    index = new RegularIndexer(varShape, elemSize, startPos, want.getRanges(), recSize);

    /* if (debug) {
      System.out.println("*************************");
      System.out.println("elemSize= "+elemSize+" startPos= "+startPos+" recSize= "+recSize);
      printa(" varShape=", varShape);
      System.out.println(" varShape total= " + Index.computeSize(varShape));
      System.out.println(" want= " + want);
      System.out.println(" want total= " + total);
      System.out.println(" isRecord= " + isRecord);
      System.out.println();

      System.out.println(" mergeRank= " + rank + " varRank= " + varRank);
      System.out.println(" nelems read at a time= " + nelems);
      printa(" finalShape=", finalShape);
      printa(" finalStride", finalStride, rank);
      System.out.println(" offset= " + offset);
    } */
  }

  private void printa(String name, int[] a, int rank) {
    System.out.print(name + "= ");
    for (int i = 0; i < rank; i++) System.out.print(a[i] + " ");
    System.out.println();
  }

  private void printa(String name, int[] a) {
    System.out.print(name + "= ");
    for (int i = 0; i < a.length; i++) System.out.print(a[i] + " ");
    System.out.println();
  }


  static public void compare(TestRegularLayout rlayout) throws InvalidRangeException {

    while (rlayout.layout.hasNext() && rlayout.index.hasNext()) {
      Indexer.Chunk chunk = rlayout.layout.next();
      Indexer.Chunk chunk2 = rlayout.index.next();
      assert chunk.getFilePos() == chunk2.getFilePos();
      assert chunk.getStartElem() == chunk2.getStartElem();
      assert chunk.getNelems() == chunk2.getNelems();
    }
    assert !rlayout.layout.hasNext();
    assert !rlayout.index.hasNext();

  }


  public void test() throws InvalidRangeException {
    // TestRegularLayout(long startPos, int elemSize, int recSize, int[] varShape, Section want) throws InvalidRangeException {

    // 4D
    Section var = new Section().appendRange(10).appendRange(20).appendRange(30).appendRange(4);
    Section want = new Section().appendRange(2).appendRange(10).appendRange(30).appendRange(4);
    compare(new TestRegularLayout(0, 1, 0, var.getShape(), want));
    compare(new TestRegularLayout(1000, 8, 4000, var.getShape(), want));

    want = new Section().appendRange(5,5).appendRange(0,900,20).appendRange(3,3).appendRange(0,16);
    compare(new TestRegularLayout(1000, 4, 888, new int[] {13,1000,4,17}, want));
    compare(new TestRegularLayout(1000, 4, 0, new int[] {13,1000,4,17}, want));

    // 3D
    want = new Section().appendRange(5,5).appendRange().appendRange();
    compare(new TestRegularLayout(1000, 4, 888, new int[] {13,40,5}, want));
    compare(new TestRegularLayout(1000, 4, 0, new int[] {13,40,5}, want));

    want = new Section().appendRange(2).appendRange(0,55,11);
    compare(new TestRegularLayout(200000, 3, 123999, new int[] {5,56}, want));
    compare(new TestRegularLayout(200000, 3, 0, new int[] {5,56}, want));

    want = new Section().appendRange(0,55,19).appendRange();
    compare(new TestRegularLayout(4000, 4000, 4000, new int[] {60,5}, want));
    compare(new TestRegularLayout(4000, 4000, 0, new int[] {60,5}, want));

    // 2D
    want = new Section().appendRange(6,55,2).appendRange();
    compare(new TestRegularLayout(4000, 4000, 4000, new int[] {60,5}, want));
    compare(new TestRegularLayout(4000, 4000, 0, new int[] {60,5}, want));

    // 1D
    want = new Section().appendRange(0,55,9);
    compare(new TestRegularLayout(4000, 4000, 4000, new int[] {60}, want));
    compare(new TestRegularLayout(4000, 4000, 0, new int[] {60}, want));

    want = new Section().appendRange(19,55,3);
    compare(new TestRegularLayout(4000, 4000, 4000, new int[] {60}, want));
    compare(new TestRegularLayout(4000, 4000, 0, new int[] {60}, want));

    // scaler */
    want = new Section();
    compare(new TestRegularLayout(4000, 4000, 4000, new int[] {}, want));  // */
    compare(new TestRegularLayout(4000, 4000, 0, new int[] {}, want));

  }


}