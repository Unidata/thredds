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
package ucar.nc2.iosp.hdf5;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.iosp.Indexer;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Group;

import java.io.IOException;

public class TestChunkIndexer extends TestCase {

  public TestChunkIndexer(String name) {
    super(name);
  }

  public void test() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("IASI.h5");
    Group root = ncfile.getRootGroup();
    Group g1 = root.findGroup("U-MARF");
    Group g2 = g1.findGroup("EPS");
    Group g3 = g2.findGroup("IASI_xxx_1C");
    Group g4 = g3.findGroup("DATA");
    Variable v2 = g4.findVariable("IMAGE_DATA");

    int[] origin = new int[]{99, 122};
    int[] shape = new int[]{10, 12};
    Section section = new Section(origin, shape);

    Array data = v2.read(section); // force btree to be filled
    assert data.getSize() == section.computeSize();

    H5chunkLayout index = new H5chunkLayout(v2, section);
    assert index.getTotalNelems() == section.computeSize();
    int count = 0;
    while (index.hasNext()) {
      Indexer.Chunk chunk = index.next();
      count++;
      if (count > 100)
      System.out.println("hay");
    }
  }

  public void test2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("support/uvlstr.h5");
    Group root = ncfile.getRootGroup();
    Variable v2 = root.findVariable("Space1");
    assert v2 != null;

    Array data = v2.read();
  }
}
