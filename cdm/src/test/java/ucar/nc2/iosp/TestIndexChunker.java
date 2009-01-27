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

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 2, 2008
 */
public class TestIndexChunker extends TestCase {

  public TestIndexChunker( String name) {
    super(name);
  }

  public void testFull() throws InvalidRangeException {
    int[] shape = new int[] {123,22,92,12};
    Section section = new Section(shape);
    IndexChunker index = new IndexChunker(shape, section);
    assert index.getTotalNelems() == section.computeSize();
    IndexChunker.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize();
    assert !index.hasNext();
  }

  public void testPart() throws InvalidRangeException {
    int[] full = new int[] {2, 10, 20};
    int[] part = new int[] {2, 5, 20};
    Section section = new Section(part);
    IndexChunker index = new IndexChunker(full, section);
    assert index.getTotalNelems() == section.computeSize();
    IndexChunker.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize()/2;
  }

  public void testChunkerTiled() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 20:39,  0:1353 ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    while(index.hasNext()) {
      Layout.Chunk chunk = index.next();
      System.out.println(" "+chunk);
    }
  }

  public void testChunkerTiled2() throws InvalidRangeException {
    Section dataSection = new Section("0:0, 40:59,  0:1353  ");
    Section wantSection = new Section("0:2, 22:3152,0:1350");
    IndexChunkerTiled index = new IndexChunkerTiled(dataSection, wantSection);
    while(index.hasNext()) {
      Layout.Chunk chunk = index.next();
      System.out.println(" "+chunk);
    }
  }

}
