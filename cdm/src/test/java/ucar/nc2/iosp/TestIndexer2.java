/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 2, 2008
 */
public class TestIndexer2 extends TestCase {

  public TestIndexer2( String name) {
    super(name);
  }

  public void testFull() throws InvalidRangeException {
    int[] shape = new int[] {123,22,92,12};
    Section section = new Section(shape);
    Indexer2 index = new Indexer2(shape, section);
    assert index.getTotalNelems() == section.computeSize();
    Indexer2.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize();
    assert !index.hasNext();
  }

  public void testPart() throws InvalidRangeException {
    int[] full = new int[] {2, 10, 20};
    int[] part = new int[] {2, 5, 20};
    Section section = new Section(part);
    Indexer2 index = new Indexer2(full, section);
    assert index.getTotalNelems() == section.computeSize();
    Indexer2.Chunk chunk = index.next();
    assert chunk.getNelems() == section.computeSize()/2;
  }

}
