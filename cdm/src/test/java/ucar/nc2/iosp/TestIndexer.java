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

import junit.framework.*;

import ucar.ma2.*;

import java.util.*;

public class TestIndexer extends TestCase  {

  public TestIndexer( String name) {
    super(name);
  }

  public void testFull() throws InvalidRangeException {
    int[] shape = new int[] {123,22,92,12};
    Section section = new Section(shape);
    RegularLayout index = new RegularLayout(0, 1, -1, shape, section);
    assert index.getTotalNelems() == section.computeSize();
    Indexer.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize();
    assert !index.hasNext();
  }

  public void testRegularIndexerNonRecord() throws InvalidRangeException {

      RegularLayout index;

      index = makeIndexer( new int[] {29}, 1, new int[] {2}, new int[] {7}, -1);
      assert index.getTotalNelems() == 7;
      assert index.getChunkSize() == 7;
      assert index.next().getFilePos() == 2;
      assert !index.hasNext();

      index = makeIndexer( new int[] {29, 40}, 1, new int[] {0, 0}, new int[] {7, 9}, -1);
      assert index.getTotalNelems() == 63;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 40;
      assert index.next().getFilePos() == 80;
      assert index.next().getFilePos() == 120;
      assert index.next().getFilePos() == 160;
      assert index.next().getFilePos() == 200;
      assert index.next().getFilePos() == 240;
      assert !index.hasNext();

      index = makeIndexer( new int[] {29, 40}, 1, new int[] {2, 3}, new int[] {7, 33}, -1);
      assert index.getTotalNelems() == 231;
      assert index.getChunkSize() == 33;
      assert index.next().getFilePos() == 83;
      assert index.next().getFilePos() == 123;
      assert index.next().getFilePos() == 163;
      assert index.next().getFilePos() == 203;
      assert index.next().getFilePos() == 243;
      assert index.next().getFilePos() == 283;
      assert index.next().getFilePos() == 323;
      assert !index.hasNext(); //

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {2,1,0}, new int[] {2,2,3}, -1);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 6;
      assert index.next().getFilePos() == 21;
      assert index.next().getFilePos() == 30;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {2,0,0}, new int[] {2,3,3}, -1);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 18;
      assert index.next().getFilePos() == 18;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3,1}, 1, new int[] {2,0,0,0}, new int[] {2,3,3,1}, -1);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 18;
      assert index.next().getFilePos() == 18;
      assert !index.hasNext();

      index = makeIndexer( new int[] {2,4,3,3}, 1, new int[] {1,2,0,0}, new int[] {1,2,3,3}, -1);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 18;
      assert index.next().getFilePos() == 18 + 36;
      assert !index.hasNext();

      index = makeIndexer( new int[] {2,4,3,3}, 1, new int[] {1,2,0,0}, new int[] {1,2,3,2}, -1);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 2;
      assert index.next().getFilePos() == 18 + 36;
  }

  public void testRegularIndexerNonRecordElemSize() {

    try {
      RegularLayout index;

      index = makeIndexer( new int[] {29}, 2, new int[] {2}, new int[] {7}, -1);
      assert index.getTotalNelems() == 7;
      assert index.getChunkSize() == 7;
      assert index.next().getFilePos() == 4;
      assert !index.hasNext();

      index = makeIndexer( new int[] {29, 40}, 2, new int[] {0, 0}, new int[] {7, 9}, -1);
      assert index.getTotalNelems() == 63;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 80;
      assert index.next().getFilePos() == 160;
      assert index.next().getFilePos() == 240;

      index = makeIndexer( new int[] {29, 40}, 4, new int[] {2, 3}, new int[] {7, 33}, -1);
      assert index.getTotalNelems() == 231;
      assert index.getChunkSize() == 33;
      assert index.next().getFilePos() == 83 * 4;
      assert index.next().getFilePos() == 123 * 4;
      assert index.next().getFilePos() == 163 * 4;
      assert index.next().getFilePos() == 203 * 4;
      assert index.next().getFilePos() == 243 * 4;
      assert index.next().getFilePos() == 283 * 4;
      assert index.next().getFilePos() == 323 * 4;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 8, new int[] {2,1,0}, new int[] {2,2,3}, -1);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 6;
      assert index.next().getFilePos() == 21 * 8;
      assert index.next().getFilePos() == 30 * 8;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 8, new int[] {2,0,0}, new int[] {2,3,3}, -1);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 18;
      assert index.next().getFilePos() == 18 * 8;
      assert !index.hasNext();

    } catch( InvalidRangeException e) {
      e.printStackTrace();
    }
  }

  public void testRegularIndexerWithRecord() throws InvalidRangeException {

      RegularLayout index;

      index = makeIndexer( new int[] {29}, 1, new int[] {2}, new int[] {7}, 1000);
      assert index.getTotalNelems() == 7;
      assert index.getChunkSize() == 1;
      assert index.next().getFilePos() == 2000;
      assert index.next().getFilePos() == 3000;

      index = makeIndexer( new int[] {29, 40}, 1, new int[] {0, 0}, new int[] {7, 9}, 100);
      assert index.getTotalNelems() == 63;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 100;
      assert index.next().getFilePos() == 200;
      assert index.next().getFilePos() == 300;
      assert index.next().getFilePos() == 400;
      assert index.next().getFilePos() == 500;
      assert index.next().getFilePos() == 600;
      assert !index.hasNext();

      index = makeIndexer( new int[] {29, 40}, 4, new int[] {2, 3}, new int[] {3, 33}, 1000);
      assert index.getTotalNelems() == 99;
      assert index.getChunkSize() == 33;
      assert index.next().getFilePos() == 2012;
      assert index.next().getFilePos() == 3012;
      assert index.next().getFilePos() == 4012;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {0,0,0}, new int[] {2,2,3}, 1000);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 6;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 1000;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {0,1,0}, new int[] {2,2,3}, 1000);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 6;
      assert index.next().getFilePos() == 3;
      assert index.next().getFilePos() == 1003;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {2,1,0}, new int[] {2,2,3}, 1000);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 6;
      assert index.next().getFilePos() == 2003;
      assert index.next().getFilePos() == 3003;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {0,0,0}, new int[] {2,3,3}, 1000);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 1000;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[] {2,0,0}, new int[] {2,3,3}, 1000);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 2000;
      assert index.next().getFilePos() == 3000;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3,1}, 1, new int[] {2,0,0,0}, new int[] {2,3,3,1}, 100);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 200;
      assert index.next().getFilePos() == 300;
      assert !index.hasNext();

      index = makeIndexer( new int[] {2,4,3,3}, 1, new int[] {1,2,0,0}, new int[] {1,2,3,3}, 1000);
      assert index.getTotalNelems() == 18;
      assert index.getChunkSize() == 18;
      assert index.next().getFilePos() == 18 + 1000;
      assert !index.hasNext();

      index = makeIndexer( new int[] {2,4,3,3}, 1, new int[] {1,2,0,0}, new int[] {1,2,3,2}, 1000);
      assert index.getTotalNelems() == 12;
      assert index.getChunkSize() == 2;
      assert index.next().getFilePos() == 18 + 1000;

  }

  public void testRegularIndexerWhole() {

    try {
      RegularLayout index;

      index = makeIndexer( new int[] {29}, 1, new int[1], new int[] {29}, -1);
      assert index.getTotalNelems() == 29;
      assert index.getChunkSize() == 29;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

      index = makeIndexer( new int[] {3}, 4, new int[1], new int[] {3}, 10);
      assert index.getTotalNelems() == 3;
      assert index.getChunkSize() == 1;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 10;
      assert index.next().getFilePos() == 20;
      assert !index.hasNext();

      index = makeIndexer( new int[] {7, 40}, 1, new int[2], new int[] {7, 40}, -1);
      assert index.getTotalNelems() == 280;
      assert index.getChunkSize() == 280;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

      index = makeIndexer( new int[] {7, 40}, 1, new int[2], new int[] {7, 40}, 100);
      assert index.getTotalNelems() == 280;
      assert index.getChunkSize() == 40;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 100;
      assert index.next().getFilePos() == 200;
      assert index.next().getFilePos() == 300;
      assert index.next().getFilePos() == 400;
      assert index.next().getFilePos() == 500;
      assert index.next().getFilePos() == 600;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[3], new int[] {4,3,3}, -1);
      assert index.getTotalNelems() == 36;
      assert index.getChunkSize() == 36;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3}, 1, new int[3], new int[] {4,3,3}, 1000);
      assert index.getTotalNelems() == 36;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 1000;
      assert index.next().getFilePos() == 2000;
      assert index.next().getFilePos() == 3000;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3,1}, 1, new int[4], new int[] {4,3,3,1}, -1);
      assert index.getTotalNelems() == 36;
      assert index.getChunkSize() == 36;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

      index = makeIndexer( new int[] {4,3,3,1}, 1, new int[4], new int[] {4,3,3,1}, 1000);
      assert index.getTotalNelems() == 36;
      assert index.getChunkSize() == 9;
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 1000;
      assert index.next().getFilePos() == 2000;
      assert index.next().getFilePos() == 3000;
      assert !index.hasNext();

      index = makeIndexer( new int[] {1,4,3,3}, 1, new int[4], new int[] {1,4,3,3}, 100);
      assert index.getTotalNelems() == 36;
      assert index.getChunkSize() == 36;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

      index = makeIndexer( new int[] {1,4,3,3}, 1, new int[4], new int[] {1,4,3,3}, -1);
      assert index.getTotalNelems() == 36;
      assert index.getChunkSize() == 36;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

    } catch( InvalidRangeException e) {
      e.printStackTrace();
    }
  }

  public void testRegularIndexerRangeErrors() {

    try {
      RegularLayout index = makeIndexer( new int[] {29}, 1, new int[1], new int[] {30}, -1);
      assert false;
    } catch( InvalidRangeException e) {
      assert true;
    }


    try {
      RegularLayout index = makeIndexer( new int[] {29}, 1, new Section(new int[1], new int[] {30, 30}), -1);
      assert false;
    } catch( Exception e) {
      assert true;
    }

    try {
      RegularLayout index = makeIndexer( new int[] {2,3}, 1, new int[2], new int[] {30, 30}, -1);
      assert false;
    } catch( InvalidRangeException e) {
      assert true;
    }

    try {
      RegularLayout index = makeIndexer( new int[] {2,2}, 1, new int[2], new int[] {1}, -1);
      assert false;
    } catch( InvalidRangeException e) {
      assert true;
    }

    try {
      RegularLayout index = makeIndexer( new int[] {20}, 1, new int[] {10}, new int[] {15}, -1);
      assert false;
    } catch( InvalidRangeException e) {
      assert true;
    }

  }


  public void testSection() throws InvalidRangeException {
      ArrayList section;
      RegularLayout index;

      // RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize)
      section = new ArrayList();
      section.add( new Range(0, 9));
      section.add( new Range(0, 9));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 100;
      assert index.getChunkSize() == 100;
      assert index.next().getFilePos() == 0;
      assert !index.hasNext();

      section = new ArrayList();
      section.add( new Range(0, 9));
      section.add( new Range(0, 4));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 50;
      assert index.getChunkSize() == 5;
      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        assert chunk.getFilePos() == count * 10 : chunk.getFilePos();
        assert chunk.getNelems() == 5;
        assert chunk.getStartElem() == count * 5;
        count++;
      }

      section = new ArrayList();
      section.add( new Range(1, 5));
      section.add( new Range(0, 9));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 50;
      assert index.getChunkSize() == 50;
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        System.out.println(" chunk= "+chunk);
        assert chunk.getFilePos() == 10;
        assert chunk.getNelems() == index.getChunkSize();
        assert chunk.getStartElem() == count * index.getChunkSize();
        count++;
      }

  } // */

  public void testSectionStride() throws InvalidRangeException {
      ArrayList section;
      RegularLayout index;

      // RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize)
      section = new ArrayList();
      section.add( new Range(0, 9));
      section.add( new Range(0, 9, 2));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 50;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      int count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        assert chunk.getFilePos() == count * 2 : chunk.getFilePos();
        assert chunk.getNelems() == index.getChunkSize();
        assert chunk.getStartElem() == count * index.getChunkSize();
        count++;
      }

      section = new ArrayList();
      section.add( new Range(0, 9, 2));
      section.add( new Range(0, 9));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 50;
      assert index.getChunkSize() == 10 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        assert chunk.getFilePos() == count * 20 : chunk.getFilePos();
        assert chunk.getNelems() == index.getChunkSize();
        assert chunk.getStartElem() == count * index.getChunkSize();
        count++;
      }

      section = new ArrayList();
      section.add( new Range(0, 9, 2));
      section.add( new Range(0, 9, 2));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 25;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      assert index.next().getFilePos() == 0;
      assert index.next().getFilePos() == 2;
      assert index.next().getFilePos() == 4;
      assert index.next().getFilePos() == 6;
      assert index.next().getFilePos() == 8;
      assert index.next().getFilePos() == 20;
      assert index.next().getFilePos() == 22;
      assert index.next().getFilePos() == 24;
      assert index.next().getFilePos() == 26;
      assert index.next().getFilePos() == 28;
      assert index.next().getFilePos() == 40;
      assert index.next().getFilePos() == 42;

      section = new ArrayList();
      section.add( new Range(0, 9, 2));
      section.add( new Range(0, 9, 2));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 5;
        int x = count % 5;
        assert chunk.getFilePos() == 20* y + 2 * x : chunk.getFilePos()+" "+count;
        count++;
      }

  }

  public void testSectionStrideOrigin() throws InvalidRangeException {
      ArrayList section;
      RegularLayout index;
      int count;

      section = new ArrayList();
      section.add( new Range(1, 8, 2));
      section.add( new Range(1, 8, 2));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 16;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 4;
        int x = count % 4;
        assert chunk.getFilePos() == 10 + 20*y + 1+2*x : chunk.getFilePos()+" "+count;
        count++;
      }

      // RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize)
      section = new ArrayList();
      section.add( new Range(1, 8));
      section.add( new Range(1, 8, 2));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 32;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 4;
        int x = count % 4;
        assert chunk.getFilePos() == 10 * (y+1) + 1+2*x : chunk.getFilePos()+" "+count;
        count++;
      }

      section = new ArrayList();
      section.add( new Range(1, 8, 2));
      section.add( new Range(1, 8));
      index = new RegularLayout( new int[] {10, 10}, 1, 0, section, -1);
      assert index.getTotalNelems() == 32;
      assert index.getChunkSize() == 8 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        assert chunk.getFilePos() == 10 + 20*count + 1 : chunk.getFilePos()+" "+count;
        count++;
      }

  } // */

  public void testSectionStrideOriginElemsize() throws InvalidRangeException {
      ArrayList section;
      RegularLayout index;
      int count;
      int elemSize = 7;

      section = new ArrayList();
      section.add( new Range(1, 8, 2));
      section.add( new Range(1, 8, 2));
      index = new RegularLayout( new int[] {10, 10}, elemSize, 0, section, -1);
      assert index.getTotalNelems() == 16;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 4;
        int x = count % 4;
        assert chunk.getFilePos() == (10 + 20*y + 1+2*x) * elemSize : chunk.getFilePos()+" "+count;
        count++;
      }

      // RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize)
      section = new ArrayList();
      section.add( new Range(1, 8));
      section.add( new Range(1, 8, 2));
      index = new RegularLayout( new int[] {10, 10}, elemSize, 0, section, -1);
      assert index.getTotalNelems() == 32;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 4;
        int x = count % 4;
        assert chunk.getFilePos() == (10 * (y+1) + 1+2*x) * elemSize : chunk.getFilePos()+" "+count;
        count++;
      }

      section = new ArrayList();
      section.add( new Range(1, 8, 2));
      section.add( new Range(1, 8));
      index = new RegularLayout( new int[] {10, 10}, elemSize, 0, section, -1);
      assert index.getTotalNelems() == 32;
      assert index.getChunkSize() == 8 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        assert chunk.getFilePos() == (10 + 20*count + 1) * elemSize : chunk.getFilePos()+" "+count;
        count++;
      }

  } // */

  public void testSectionStrideOriginElemsizeRecord() throws InvalidRangeException {
      ArrayList section;
      RegularLayout index;
      int count;
      int elemSize = 7;
      int recSize = 1000;

      section = new ArrayList();
      section.add( new Range(1, 8, 2));
      section.add( new Range(1, 8, 2));
      index = new RegularLayout( new int[] {10, 10}, elemSize, 0, section, recSize);
      assert index.getTotalNelems() == 16;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 4;
        int x = count % 4;
        assert chunk.getFilePos() == recSize*(1+2*y) + elemSize* (1+2*x) : chunk.getFilePos()+" "+count+" "+x+" "+y;
        count++;
      }

      // RegularIndexer( int[] varShape, int elemSize, long startPos, List section, int recSize)
      section = new ArrayList();
      section.add( new Range(1, 8));
      section.add( new Range(1, 8, 2));
      index = new RegularLayout( new int[] {10, 10}, elemSize, 0, section, recSize);
      assert index.getTotalNelems() == 32;
      assert index.getChunkSize() == 1 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        int y = count / 4;
        int x = count % 4;
        assert chunk.getFilePos() == recSize*(1+y) + elemSize* (1+2*x) : chunk.getFilePos()+" "+count+" "+x+" "+y;
        count++;
      }

      section = new ArrayList();
      section.add( new Range(1, 8, 2));
      section.add( new Range(1, 8));
      index = new RegularLayout( new int[] {10, 10}, elemSize, 0, section, recSize);
      assert index.getTotalNelems() == 32;
      assert index.getChunkSize() == 8 : index.getChunkSize();
      count = 0;
      while (index.hasNext()) {
        Indexer.Chunk chunk = index.next();
        assert chunk.getFilePos() == recSize*(1+2*count) + elemSize : chunk.getFilePos()+" "+count;
        count++;
      }

  } // */

  /**
  * @param varShape shape of the entire data array.
  * @param elemSize size of on element in bytes.
  * @param recSize if > 0, then size of outer stride in bytes, else ignored
  * @throws InvalidRangeException is ranges are misformed
  */
 static private RegularLayout makeIndexer( int[] varShape, int elemSize, int[] origin,
                                            int[] shape, int recSize) throws InvalidRangeException {

   //return new RegularIndexer( diml, elemLength, 0L, Range.factory(origin, shape), recsize);
   return new RegularLayout(  0L,  elemSize, recSize, varShape, new Section(origin, shape));
   //   public RegularLayout(long startPos, int elemSize, int recSize, int[] varShape, List<Range> rangeList) throws InvalidRangeException {

 }

  /**
  * @param varShape shape of the entire data array.
  * @param elemSize size of on element in bytes.
  * @param recSize if > 0, then size of outer stride in bytes, else ignored
  * @throws InvalidRangeException is ranges are misformed
  */
 static private RegularLayout makeIndexer( int[] varShape, int elemSize, Section section, int recSize) throws InvalidRangeException {

   return new RegularLayout(  0L,  elemSize, recSize, varShape, section);

 }



}
