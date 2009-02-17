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
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.dataset.*;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NCdump;
import ucar.nc2.util.IO;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.File;
import java.util.Formatter;

public class TestNcml extends TestCase {

  public TestNcml( String name) {
    super(name);
  }

  public void testNcMLinDatasetScan() throws IOException {
    InvCatalogImpl cat = TestTDSAll.open(null);

    InvDataset parent = cat.findDatasetByID("testAddRecords");
    assert (parent != null) : "cant find dataset 'testAddRecords'";

    InvDataset ds = parent.findDatasetByName("NAM_CONUS_80km_20051206_0000.nc");
    assert (ds != null) : "cant find dataset 'NAM_CONUS_80km_20051206_0000.nc'";
    assert ds.getDataType() == FeatureType.GRID;

    // ncml should not be sent to the client
    assert null == ((InvDatasetImpl)ds).getNcmlElement();

    ThreddsDataFactory fac = new ThreddsDataFactory();
    Formatter log = new Formatter();

    NetcdfDataset ncd = fac.openDataset( ds, false, null, log);

    assert ncd != null : log.toString();

    Variable v = ncd.findVariable("record");
    assert v != null;

    assert ncd.findAttValueIgnoreCase(null,  "name2", "").equals("value2");

    ncd.close();
  }

  // simple NcML wrapping
  public void testNcmlModify() throws IOException {
    InvCatalogImpl cat = TestTDSAll.open(null);

    InvDataset ds = cat.findDatasetByID("NcML-modify");
    assert (ds != null) : "cant find dataset 'NcML-modify'";

    ThreddsDataFactory fac = new ThreddsDataFactory();
    Formatter log = new Formatter();

    NetcdfDataset ncd = fac.openDataset( ds, false, null, log);
    assert ncd != null : log.toString();

    Variable v = ncd.findVariable("Temperature");
    assert v != null;
  }


  // ncml should not be sent to the client
  public void testDatasetNcml() throws IOException {
    InvCatalogImpl cat = TestTDSAll.open(null);

    InvDataset ds = cat.findDatasetByID("aggNewTest");
    assert (ds != null) : "cant find dataset 'aggNewTest'";

    // ncml should not be sent to the client
    assert null == ((InvDatasetImpl)ds).getNcmlElement();
  }


  public void testAggExisting() throws IOException {
    NetcdfFile ncd = NetcdfDataset.openFile("http://localhost:8080/thredds/dodsC/aggExistingTest/seawifs.nc", null);
    assert ncd != null;

    // test attributes added in NcML
    String testAtt = ncd.findAttValueIgnoreCase(null, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("value");

    Variable v = ncd.findVariable("latitude");
    assert v != null;
    testAtt = ncd.findAttValueIgnoreCase(v, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("lat_value");

    v = ncd.findVariable("chlorophylle_a");
    assert v != null;
    testAtt = ncd.findAttValueIgnoreCase(v, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("chlor_value");

    v = ncd.findVariable("time");
    assert v != null;
    testAtt = ncd.findAttValueIgnoreCase(v, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("time_value");

    Array data = v.read();
    assert data.getSize() == v.getSize();
    assert data.getSize() == 6;

    int count = 0;
    double[] want = new double[]  {890184.0, 890232.0, 890256.0, 890304.0, 890352.0, 890376.0};
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      double val = ii.getDoubleNext();
      assert val == want[count++];
    }

    ncd.close();
  }

  public void testAggNew() throws IOException, InvalidRangeException {
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

  public void testWcs() throws IOException {
    showGetCapabilities("http://localhost:8080/thredds/wcs/aggNewTest/SUPER-NATIONAL_8km_WV.gini");
    showDescribeCoverage("http://localhost:8080/thredds/wcs/aggNewTest/SUPER-NATIONAL_8km_WV.gini", "IR_WV");
    showGetCoverage("http://localhost:8080/thredds/wcs/aggNewTest/SUPER-NATIONAL_8km_WV.gini", "IR_WV",
            "2000-06-16T07:00:00Z", null, null);
  }

  private void showGetCapabilities(String url) throws IOException {
    showRead(url+"?request=GetCapabilities&version=1.0.0&service=WCS");
  }

  private void showDescribeCoverage(String url, String grid) throws IOException {
    showRead(url+"?request=DescribeCoverage&version=1.0.0&service=WCS&coverage="+grid);
  }

  private void showGetCoverage(String url, String grid, String time, String vert, String bb) throws IOException {
    String getURL = url+"?request=GetCoverage&version=1.0.0&service=WCS&format=NetCDF3&coverage="+grid;
    if (time != null)
      getURL = getURL + "&time="+time;
    if (vert != null)
      getURL = getURL + "&vertical="+vert;
    if (bb != null)
      getURL = getURL + "&bbox="+bb;

    File file = new File("C:/TEMP/"+grid+"3.nc");
    IO.readURLtoFile(getURL, file);
    System.out.println(" copied contents to "+file.getPath());
  }

  private void showRead(String url) throws IOException {
    System.out.println("****************\n");
    System.out.println(url+"\n");
    String contents = IO.readURLcontentsWithException( url);
    System.out.println(contents);
  }



}