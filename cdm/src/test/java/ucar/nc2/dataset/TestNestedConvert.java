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
package ucar.nc2.dataset;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.Structure;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.ma2.StructureData;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayStructure;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Test that nested structures get enhanced.
 * @author caron
 * @since Jul 5, 2008
 */
public class TestNestedConvert extends TestCase {

  public TestNestedConvert( String name) {
    super(name);
  }

  public void testNestedTable() throws IOException, InvalidRangeException {
    String filename = TestAll.cdmTestDataDir + "dataset/nestedTable.bufr";
    NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDataset.openFile(filename, null);
    Structure outer = (Structure) ncfile.findVariable("obsRecord");
    StructureData data = outer.readStructure(0);
    //NCdumpW.printStructureData( new PrintWriter(System.out), data);

    assert data.getScalarShort("Latitude") == 32767;

    ArrayStructure as = data.getArrayStructure("struct1");
    assert as != null;
    assert as.getScalarShort(0, as.findMember("Wind_speed")) == 61;

    ncfile.close();
  }

  public void testNestedTableEnhanced() throws IOException, InvalidRangeException {
    String filename = TestAll.cdmTestDataDir + "dataset/nestedTable.bufr";
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    Structure outer = (Structure) ncd.findVariable("obsRecord");
    StructureData data = outer.readStructure(0);
    //NCdumpW.printStructureData( new PrintWriter(System.out), data);

    assert Double.isNaN( data.getScalarFloat("Latitude"));

    ArrayStructure as = data.getArrayStructure("struct1");
    assert as != null;
    assert TestAll.closeEnough(as.getScalarFloat(0, as.findMember("Wind_speed")),6.1);

    ncd.close();
  }
}
