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
public class TestRegularSectionLayout extends TestCase {

  public TestRegularSectionLayout( String name) {
    super(name);
  }


  void doit(RegularSectionLayout rlayout, int[][] result) throws InvalidRangeException {
    int count = 0;
    while (rlayout.hasNext()) {
      Indexer.Chunk chunk = rlayout.next();

      if (count < result.length) {
        assert chunk.getFilePos() == result[count][0];
        assert chunk.getNelems() == result[count][1];
        assert chunk.getStartElem() == result[count][2];
        count++;
      }
    }
  }

  void doit(RegularSectionLayout rlayout, int[] start, int[] incr) throws InvalidRangeException {
    int count = 0;
    while (rlayout.hasNext()) {
      Indexer.Chunk chunk = rlayout.next();

      assert chunk.getFilePos() == start[0] + count * incr[0];
      assert chunk.getNelems() == start[1] + count * incr[1];
      assert chunk.getStartElem() == start[2] + count * incr[2];
      count++;
    }

  }



  public void test1D() throws InvalidRangeException {
    //   public RegularSectionLayout(long startFilePos, int elemSize, Section dataSection, Section wantSection)
    System.out.println("\n data ================");
    System.out.println("  want     =======");
    Section data = new Section().appendRange(10);
    Section want = new Section().appendRange(2, 5);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{2, 4, 0}});

    System.out.println("\n data ================");
    System.out.println("  want     = = = =");
    data = new Section().appendRange(10);
    want = new Section().appendRange(2, 7, 2);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{2, 1, 0}, {4, 1, 2}, {6, 1, 4}});

    want = new Section().appendRange(2, 10, 3);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{2, 1, 0}, {5, 1, 3}, {8, 1, 6}});

    System.out.println("\n data ================");
    System.out.println("  want        ================");
    data = new Section().appendRange(10);
    want = new Section().appendRange(5, 20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{5,5,0}});

    System.out.println("\n data        ================");
    System.out.println(" want  ================");
    data = new Section().appendRange(10,30);
    want = new Section().appendRange(5, 20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{0, 11, 5}});

    System.out.println("\n data    ======");
    System.out.println(" want  ================");
    data = new Section().appendRange(5,10);
    want = new Section().appendRange(3, 20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{0, 6, 2}});

    System.out.println("\n data    ======");
    System.out.println(" want  = == = = = = = = = ");
    data = new Section().appendRange(5,10);
    want = new Section().appendRange(3, 20, 2);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{0, 1, 2},{2, 1, 4},{4, 1, 6}});   // */

  }

  public void test2D() throws InvalidRangeException {
    System.out.println("\n data ================");
    System.out.println("  want     =======");
    Section data = new Section().appendRange(10).appendRange(10);
    Section want = new Section().appendRange(2, 5).appendRange(2, 5);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {22, 4, 0}, new int[] {10, 0, 4});

    System.out.println("\n data ================");
    System.out.println("  want     = = = =");
    data = new Section().appendRange(10).appendRange(10);
    want = new Section().appendRange(2, 7, 2).appendRange(2, 7, 2);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{22,1,0},{24,1,2},{26,1,4},{42,1,6},{44,1,8},{46,1,10},{62,1,12},});

    want = new Section().appendRange(3, 10, 3).appendRange(2, 10, 5);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{32, 1, 0},{37, 1, 5},{62, 1, 6},{67, 1, 11},{92, 1, 12},{97, 1, 17}}); // */

    System.out.println("\n data |================|");
    System.out.println("  want        |================|");
    data = new Section().appendRange(10).appendRange(10);
    want = new Section().appendRange(5, 20).appendRange(5, 20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {55, 5, 0}, new int[] {10, 0, 16});

    System.out.println("\n data     |================|");
    System.out.println("  want  |================|");
    data = new Section().appendRange(30).appendRange(10, 500);
    want = new Section().appendRange(15,99).appendRange(20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[]{7365, 10, 10}, new int[] {491, 0, 20});

    System.out.println("  want  |================|");
    System.out.println("\n data     |================|");
    data = new Section().appendRange(10, 20).appendRange(5, 50);
    want = new Section().appendRange(5, 12).appendRange(20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {0, 15, 105}, new int[] {46, 0, 20});

    System.out.println("  want      |================|");
    System.out.println("\n data  |================|");
    data = new Section().appendRange(10, 20).appendRange(5, 50);
    want = new Section().appendRange(5, 12).appendRange(20, 30);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {15, 11, 55}, new int[] {46, 0, 11});

    System.out.println("\n data    ======");
    System.out.println(" want  ================");
    data = new Section().appendRange(5,9).appendRange(5,9);
    want = new Section().appendRange(20).appendRange(20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {0, 5, 105}, new int[] {5, 0, 20});

    System.out.println("\n data    ======");
    System.out.println(" want  ================");
    data = new Section().appendRange(5,9).appendRange(5,9);
    want = new Section().appendRange(0,20).appendRange(0,20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {0, 5, 110}, new int[] {5, 0, 21});

    System.out.println("\n data    ======");
    System.out.println(" want  = == = = = = = = = ");
    data = new Section().appendRange(5,9).appendRange(5,9);
    want = new Section().appendRange(0,20,2).appendRange(0,20,2);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {{6,1,72},{8,1,74},{16,1,94},{18,1,96}});
  }

  public void test2Da() throws InvalidRangeException {
    Section data = new Section().appendRange(20).appendRange(20);
    Section want = new Section().appendRange(1000).appendRange(20);
    doit(new RegularSectionLayout(0, 1, data, want), new int[][] {});
  }


  public void test3D() throws InvalidRangeException {
    System.out.println("\n data ================");
    System.out.println("  want     =======");
    Section data = new Section().appendRange(10).appendRange(10).appendRange(10);
    Section want = new Section().appendRange(2, 5).appendRange(10).appendRange(10);
    doit(new RegularSectionLayout(0, 1, data, want), new int[] {200, 10, 0}, new int[] {10, 0, 10});
  }



}