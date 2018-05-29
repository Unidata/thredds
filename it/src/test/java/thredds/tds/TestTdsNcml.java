/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.tds;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
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
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

@Category(NeedsCdmUnitTest.class)
public class TestTdsNcml {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    String endpoint = TestOnLocalServer.withHttpPath("dodsC/ExampleNcML/Agg.nc");
    logger.debug("{}", endpoint);

    NetcdfFile ncfile = NetcdfDataset.openFile(endpoint, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.DOUBLE;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equals("hours since 2006-09-25T06:00:00Z");

    int count = 0;
    Array data = v.read();
    logger.debug(NCdumpW.toString(data, "time", null));

    while (data.hasNext()) {
      Assert2.assertNearlyEquals(data.nextInt(), (count + 1) * 3);
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
    String endpoint = TestOnLocalServer.withHttpPath("cdmremote/testGridScan/GFS_CONUS_80km_20120229_1200.grib1");
    logger.debug("{}", endpoint);

    try (NetcdfFile ncd = NetcdfDataset.openFile(endpoint, null)) {
      Assert.assertNotNull(ncd);

      Attribute att = ncd.findGlobalAttribute("ncmlAdded");
      Assert.assertNotNull(att);
      Assert.assertEquals("stuff", att.getStringValue());
    }
  }
}
