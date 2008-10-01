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

package ucar.nc2.ncml;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.NCdumpW;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;
import java.io.StringReader;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestOffAggExistingTimeUnitsChange extends TestCase {

  public TestOffAggExistingTimeUnitsChange( String name) {
    super(name);
  }
  //String location = "file:R:/testdata/ncml/nc/narr/narr.ncml";

  public void testNamExtract() throws IOException, InvalidRangeException {
    String location = "file:"+ TestAll.upcShareTestDataDir + "ncml/nc/namExtract/test_agg.ncml";
    System.out.println(" TestOffAggExistingTimeUnitsChange.open "+ location);

    NetcdfFile ncfile = NetcdfDataset.openFile(location, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.INT;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equals("hours since 2006-09-25T06:00:00Z");

    int count = 0;
    Array data = v.read();
    NCdumpW.printArray(data, "time", new PrintWriter(System.out), null);
    while (data.hasNext()) {
      assert data.nextInt() == (count + 1) * 3;
      count++;
    }

    ncfile.close();
  }

  public void testNarrGrib() throws IOException, InvalidRangeException {
    String ncml =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                    " <aggregation type='joinExisting' dimName='time' timeUnitsChange='true' >\n" +
                    "  <netcdf location='narr-a_221_20070411_0000_000.grb'/>\n" +
                    "  <netcdf location='narr-a_221_20070411_0300_000.grb'/>\n" +
                    "  <netcdf location='narr-a_221_20070411_0600_000.grb'/>\n" +
                    " </aggregation>\n" +
                    "</netcdf>";

    String location = "file:"+ TestAll.upcShareTestDataDir + "ncml/nc/narr/";
    System.out.println(" TestOffAggExistingTimeUnitsChange.testNarrGrib=\n"+ ncml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.INT;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equals("hour since 2007-04-11T00:00:00Z");

    int count = 0;
    Array data = v.read();
    NCdumpW.printArray(data, "time", new PrintWriter(System.out), null);
    while (data.hasNext()) {
      assert data.nextInt() == count * 3;
      count++;
    }

    ncfile.close();
  }
}
