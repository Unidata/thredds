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
package thredds.tds;

import junit.framework.*;

import thredds.catalog.*;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.*;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Formatter;

public class TestTdsNcml extends TestCase {

  public TestTdsNcml( String name) {
    super(name);
  }

  public void testNcMLinDataset() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(null);

    InvDataset ds = cat.findDatasetByID("ExampleNcMLModified");
    assert (ds != null) : "cant find dataset 'ExampleNcMLModified'";
    assert ds.getDataType() == FeatureType.GRID : ds.getDataType();

    // ncml should not be sent to the client
    assert null == ((InvDatasetImpl)ds).getNcmlElement();

    ThreddsDataFactory fac = new ThreddsDataFactory();
    Formatter log = new Formatter();

    NetcdfDataset ncd = fac.openDataset( ds, false, null, log);

    assert ncd != null : log.toString();

    Variable v = ncd.findVariable("record");
    assert v != null;

    assert ncd.findAttValueIgnoreCase(null,  "name", "").equals("value");

    assert ncd.findVariable("Temperature") != null;
    assert ncd.findVariable("T") == null;

    v = ncd.findVariable("ReletiveHumidity");
    assert v != null;
    Attribute att = v.findAttribute("long_name");
    assert att != null;
    assert att.getStringValue().equals("relatively humid");
    assert null == v.findAttribute("description");

    ncd.close();
  }

  public void testNcMLinDatasetScan() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(null);

    InvDataset parent = cat.findDatasetByID("ModifyDatasetScan");
    assert (parent != null) : "cant find dataset 'ModifyDatasetScan'";
    InvDataset ds = parent.findDatasetByName("example1.nc");

    assert ds.getDataType() == FeatureType.GRID : ds.getDataType();

    // ncml should not be sent to the client
    assert null == ((InvDatasetImpl)ds).getNcmlElement();

    ThreddsDataFactory fac = new ThreddsDataFactory();
    Formatter log = new Formatter();

    NetcdfDataset ncd = fac.openDataset( ds, false, null, log);

    assert ncd != null : log.toString();

    Variable v = ncd.findVariable("record");
    assert v != null;

    assert ncd.findAttValueIgnoreCase(null,  "name", "").equals("value");

    assert ncd.findVariable("Temperature") != null;
    assert ncd.findVariable("T") == null;

    v = ncd.findVariable("ReletiveHumidity");
    assert v != null;
    Attribute att = v.findAttribute("long_name");
    assert att != null;
    assert att.getStringValue().equals("relatively humid");
    assert null == v.findAttribute("description");

    ncd.close();
  }

  public void testAggExisting() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openFile("http://localhost:8080/thredds/dodsC/ExampleNcML/Agg.nc", null);
    System.out.printf("%s%n", ncfile);

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
      assert TestAll.closeEnough(data.nextInt(), (count + 1) * 3);
      count++;
    }

     // test attributes added in NcML
    String testAtt = ncfile.findAttValueIgnoreCase(null, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("stuff");

    v = ncfile.findVariable("lat");
    assert v != null;
    testAtt = ncfile.findAttValueIgnoreCase(v, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("lat_stuff");

    ncfile.close();
  }

  public void utestAggNew() throws IOException, InvalidRangeException {
    NetcdfFile ncd = NetcdfDataset.openFile("http://localhost:8080/thredds/dodsC/aggNewTest/SUPER-NATIONAL_8km_WV.gini", null);
    assert ncd != null;

    Variable v = ncd.findVariable("time");
    assert v != null;

    String testAtt = ncd.findAttValueIgnoreCase(v, "units", null);
    assert testAtt != null;
    assert testAtt.equals("minutes since 2000-6-16 6:00");

    Array data = v.read();
    assert data.getSize() == v.getSize();
    assert data.getSize() == 12;

    int count = 0;
    int[] want = new int[]    {0, 15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165};
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      int val = ii.getIntNext();
      assert val == want[count++];
    }

    // test strided access on an agg variable
    v = ncd.findVariable("IR_WV");
    assert v != null;

    data = v.read("0:11:2,0,0");
    assert data.getSize() == 6;

    count = 0;
    byte[] wantb = new byte[]    {-75, -74, -74, -73, -71, -72};
    ii = data.getIndexIterator();
    while (ii.hasNext()) {
      byte val = ii.getByteNext();
      assert val == wantb[count++];
    }

    data = v.read("1:10:2,1,0");
    NCdump.printArray(data, null, System.out, null);
    assert data.getSize() == 5;

    count = 0;
    wantb = new byte[] {-75, -74, -74, -73, -71};
    ii = data.getIndexIterator();
    while (ii.hasNext()) {
      byte val = ii.getByteNext();
      assert val == wantb[count] : count+": "+val+" != "+ wantb[count];
      count++;
    }


    ncd.close();
  }

}