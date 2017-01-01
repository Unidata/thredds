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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Formatter;

@Category(NeedsCdmUnitTest.class)
public class TestTdsNcml {

  @Test
  public void testNcMLinDataset() throws IOException {
    Catalog cat = TdsLocalCatalog.open(null);

    Dataset ds = cat.findDatasetByID("ExampleNcMLModified");
    assert (ds != null) : "cant find dataset 'ExampleNcMLModified'";
    assert ds.getFeatureType() == FeatureType.GRID : ds.getFeatureType();

    // ncml should not be sent to the client
    // assert null == ds.getNcmlElement();

    DataFactory fac = new DataFactory();
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

  @Test
  public void testNcMLinDatasetScan() throws IOException {
    Catalog cat = TdsLocalCatalog.open(null);

    Dataset catref = cat.findDatasetByID("ModifyDatasetScan");
    Assert.assertNotNull("cant find dataset by id 'ModifyDatasetScan'", catref);
    catref.getDatasetsLogical(); // reads in the referenced catalog
    Dataset ds = catref.findDatasetByName("example1.nc");
    Assert.assertNotNull("cant find dataset by name 'example1'", ds);

    assert ds.getFeatureType() == FeatureType.GRID : ds.getFeatureType();

    // ncml should not be sent to the client
    assert null == ds.getNcmlElement();

    DataFactory fac = new DataFactory();
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

  @Test
  public void testAggExisting() throws IOException, InvalidRangeException {
    String endpoint = TestWithLocalServer.withPath("dodsC/ExampleNcML/Agg.nc");
    System.out.printf("%s%n", endpoint);

    NetcdfFile ncfile = NetcdfDataset.openFile(endpoint, null);

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

  @Test
  public void testAddMetadataToScan() throws IOException, InvalidRangeException {
    String endpoint = TestWithLocalServer.withPath("cdmremote/testGridScan/GFS_CONUS_80km_20120229_1200.grib1");
    System.out.printf("%s%n", endpoint);

    try (NetcdfFile ncd = NetcdfDataset.openFile(endpoint, null)) {
      Assert.assertNotNull(ncd);

      Attribute att = ncd.findGlobalAttribute("ncmlAdded");
      Assert.assertNotNull(att);
      Assert.assertEquals("stuff", att.getStringValue());
    }
  }

}
