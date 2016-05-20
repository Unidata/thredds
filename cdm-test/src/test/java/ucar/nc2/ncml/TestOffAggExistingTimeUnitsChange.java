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

package ucar.nc2.ncml;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

/**
 * Test aggregation where timeUnitsChange='true'
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggExistingTimeUnitsChange extends TestCase {

  public TestOffAggExistingTimeUnitsChange( String name) {
    super(name);
  }
  //String location = "file:R:/testdata2/ncml/nc/narr/narr.ncml";

  public void testNamExtract() throws IOException, InvalidRangeException {
    String location = TestDir.cdmUnitTestDir + "ncml/nc/namExtract/test_agg.ncml";
    System.out.println(" TestOffAggExistingTimeUnitsChange.open "+ location);

    NetcdfFile ncfile = NetcdfDataset.openFile(location, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.DOUBLE;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equals("hours since 2006-09-25T06:00:00Z");

    int count = 0;
    Array data = v.read();
    NCdumpW.printArray(data, "time", new PrintWriter(System.out), null);
    while (data.hasNext()) {
      assert Misc.closeEnough(data.nextInt(), (count + 1) * 3);
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

    String location = "file:"+ TestDir.cdmUnitTestDir + "ncml/nc/narr/";
    System.out.printf(" TestOffAggExistingTimeUnitsChange.testNarrGrib=%s%n%s", location, ncml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.DOUBLE;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equalsIgnoreCase("Hour since 2007-04-11T00:00:00Z") : units;   // Hour since 2007-04-11T00:00:00.000Z

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
