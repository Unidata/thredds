/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.ma2.*;

/**
 * @author caron
 * @since Jan 25, 2008
 */
public class TestStructureIterator extends TestCase {

  public TestStructureIterator(String name) {
    super(name);
  }

  public void testStructureIterator() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestNC2.open("C:/data/metars/Surface_METAR_20070331_0000.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    Structure v = (Structure) ncfile.findVariable("record");
    assert v != null;
    assert (v.getDataType() == DataType.STRUCTURE);

    int count = 0;
    Structure.Iterator si = v.getStructureIterator();
    while (si.hasNext()) {
      StructureData sd = si.next();
      count++;
    }
    assert count == v.getSize();
    
    ncfile.close();
  }
}

