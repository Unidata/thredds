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

package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 13, 2009
 */
public class TestAggPromote  extends TestCase {

  public TestAggPromote( String name) {
    super(name);
  }

  public void testPromote1() throws IOException, InvalidRangeException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='time' type='joinExisting'>\n" +
      "    <promoteGlobalAttribute name='times' orgName='time_coverage_end' />\n" +
      "    <scan dateFormatMark='CG#yyyyDDD_HHmmss' location='src/test/data/ncml/nc/cg/' suffix='.nc' subdirs='false' />\n" +
      "  </aggregation>\n" +
      "</netcdf>";

    String filename = "file:./"+ TestNcML.topDir + "aggExisting1.xml";

    NetcdfFile ncfile = NcMLReader.readNcML( new StringReader(xml), null);
    System.out.println(" TestNcmlAggExisting.open "+ filename);

    Variable times = ncfile.findVariable("times");
    assert null != times;
    assert times.getRank() == 1;
    assert times.getSize() == 3;

    assert times.getDimension(0).getName().equals("time");
  }
}
