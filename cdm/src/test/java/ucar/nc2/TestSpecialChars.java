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
package ucar.nc2;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;

import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.ncml.NcMLWriter;

/**
 * @author caron
 * @since Aug 7, 2007
 */
public class TestSpecialChars extends TestCase {
  private boolean show = false;

  public TestSpecialChars( String name) {
    super(name);
  }

  String trouble = "here is a &, <, >, \', \", \n, \r, to handle";

  public void testWrite() throws IOException, InvalidRangeException {
    String filename = TestLocal.cdmTestDataDir +"testSpecialChars.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, true);
    ncfile.addGlobalAttribute("omy", trouble);

    ncfile.addDimension("t", 1);

    // define Variables
    ncfile.addVariable("t", DataType.STRING, "t");
    ncfile.addVariableAttribute("t", "yow", trouble);

    ncfile.create();

    Array data = Array.factory(DataType.STRING, new int[0]);
    data.setObject( data.getIndex(), trouble);
    ncfile.writeStringData("t", data);
    ncfile.close();
  }

  public void testRead() throws IOException {
    String filename = TestLocal.cdmTestDataDir +"testSpecialChars.nc";
    NetcdfFile ncfile = NetcdfFile.open(filename, null);
    String val = ncfile.findAttValueIgnoreCase(null, "omy", null);
    assert val != null;
    assert val.equals(trouble);

    Variable v = ncfile.findVariable("t");
    v.setCachedData( v.read(), true);

    val = ncfile.findAttValueIgnoreCase(v, "yow", null);
    assert val != null;
    assert val.equals(trouble);

    ncfile.writeCDL(System.out, false);
    ncfile.writeNcML(System.out, null);

    NcMLWriter w = new NcMLWriter();
    w.writeXML(ncfile, System.out, null);

    ncfile.close();
  }
}
