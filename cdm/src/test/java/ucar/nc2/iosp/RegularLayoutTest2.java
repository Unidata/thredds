// $Id: RegularIndexer.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp;

import ucar.ma2.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Indexer into a regular array.
 * This calculates byte lengths and offsets of the wanted data into the entire data.
 * Assumes that the data is stored "regularly", like netcdf arrays and hdf5 continuous storage.
 * Also handles netcdf3 record dimensions.
 *
 * @author caron
 */
public class RegularLayoutTest2 {
  RegularLayout layout;
  RegularIndexer index;

  private boolean debug = false;

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
  private RegularLayoutTest2(long startPos, int elemSize, int recSize, int[] varShape, Section want) throws InvalidRangeException {

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
    layout = new RegularLayout(startPos, elemSize, recSize, varShape, want.getRanges());

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


  static public void compare(RegularLayoutTest2 rlayout) throws InvalidRangeException {

    while (rlayout.layout.hasNext() && rlayout.index.hasNext()) {
      Indexer.Chunk chunk = rlayout.layout.next();
      Indexer.Chunk chunk2 = rlayout.index.next();
      assert chunk.getFilePos() == chunk2.getFilePos();
      assert chunk.getIndexPos() == chunk2.getIndexPos();
      assert chunk.getNelems() == chunk2.getNelems();
    }
    assert !rlayout.layout.hasNext();
    assert !rlayout.index.hasNext();

  }


  static public void main(String args[]) throws InvalidRangeException {
    // RegularLayoutTest2(long startPos, int elemSize, int recSize, int[] varShape, Section want) throws InvalidRangeException {

    // 4D
    Section var = new Section().appendRange(10).appendRange(20).appendRange(30).appendRange(4);
    Section want = new Section().appendRange(2).appendRange(10).appendRange(30).appendRange(4);
    compare(new RegularLayoutTest2(0, 1, 0, var.getShape(), want));
    compare(new RegularLayoutTest2(1000, 8, 4000, var.getShape(), want));

    want = new Section().appendRange(5,5).appendRange(0,900,20).appendRange(3,3).appendRange(0,16);
    compare(new RegularLayoutTest2(1000, 4, 888, new int[] {13,1000,4,17}, want));
    compare(new RegularLayoutTest2(1000, 4, 0, new int[] {13,1000,4,17}, want));

    // 3D
    want = new Section().appendRange(5,5).appendRange().appendRange();
    compare(new RegularLayoutTest2(1000, 4, 888, new int[] {13,40,5}, want));
    compare(new RegularLayoutTest2(1000, 4, 0, new int[] {13,40,5}, want));

    want = new Section().appendRange(2).appendRange(0,55,11);
    compare(new RegularLayoutTest2(200000, 3, 123999, new int[] {5,56}, want));
    compare(new RegularLayoutTest2(200000, 3, 0, new int[] {5,56}, want));

    want = new Section().appendRange(0,55,19).appendRange();
    compare(new RegularLayoutTest2(4000, 4000, 4000, new int[] {60,5}, want));
    compare(new RegularLayoutTest2(4000, 4000, 0, new int[] {60,5}, want));

    // 2D
    want = new Section().appendRange(6,55,2).appendRange();
    compare(new RegularLayoutTest2(4000, 4000, 4000, new int[] {60,5}, want));
    compare(new RegularLayoutTest2(4000, 4000, 0, new int[] {60,5}, want));

    // 1D
    want = new Section().appendRange(0,55,9);
    compare(new RegularLayoutTest2(4000, 4000, 4000, new int[] {60}, want));
    compare(new RegularLayoutTest2(4000, 4000, 0, new int[] {60}, want));

    want = new Section().appendRange(19,55,3);
    compare(new RegularLayoutTest2(4000, 4000, 4000, new int[] {60}, want));
    compare(new RegularLayoutTest2(4000, 4000, 0, new int[] {60}, want));

    // scaler */
    want = new Section();
    compare(new RegularLayoutTest2(4000, 4000, 4000, new int[] {}, want));  // */
    compare(new RegularLayoutTest2(4000, 4000, 0, new int[] {}, want));

  }


}