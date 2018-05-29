/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.fileserver;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/** Test remote netcdf over HTTP in the JUnit framework. */
public class TestHTTP  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String url = "http://" + TestDir.remoteTestServer + "/thredds/fileServer/scanLocal/mydata1.nc";

  @Test
  public void testOpenNetcdfFile() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(url)) {
      test(ncfile);
      logger.debug("*****************  Test testOpenNetcdfFile over HTTP done");
    }
  }

  @Test
  public void testOpenNetcdfDataset() throws IOException {
    try (NetcdfFile ncfile = NetcdfDataset.openDataset(url)) {
      test(ncfile);
      logger.debug("*****************  Test testOpenNetcdfDataset over HTTP done");
    }
  }

  @Test
  public void testOpenDataFactory() throws IOException {
    Formatter log = new Formatter();
    Dataset ds = Dataset.makeStandalone(url, null, DataFormatType.NETCDF.toString(), ServiceType.HTTPServer.toString());
    DataFactory tdataFactory = new DataFactory();
    try (NetcdfDataset ncfile = tdataFactory.openDataset(ds, false, null, log)) {
      test(ncfile);
      logger.debug("*****************  Test testDataFactory over HTTP done");
    }
  }

  private void test(NetcdfFile ncfile) throws IOException {
    assert ncfile != null;

    assert(null != ncfile.findDimension("lat"));
    assert(null != ncfile.findDimension("lon"));

    assert("face".equals(ncfile.findAttValueIgnoreCase(null, "yo", "barf")));

    Variable temp = ncfile.findVariable("temperature");
    assert (null != temp);
    assert("K".equals(ncfile.findAttValueIgnoreCase(temp, "units", "barf")));

    Attribute att = temp.findAttribute("scale");
    assert( null != att);
    assert( att.isArray());
    assert( 3 == att.getLength());
    assert( 3 == att.getNumericValue(2).intValue());

    att = temp.findAttribute("versionD");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    Assert2.assertNearlyEquals(1.2f, att.getNumericValue().floatValue());

    att = temp.findAttribute("versionF");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2f == att.getNumericValue().floatValue());
    Assert2.assertNearlyEquals(1.2f, att.getNumericValue().floatValue());

    att = temp.findAttribute("versionI");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1 == att.getNumericValue().intValue());

    att = temp.findAttribute("versionS");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 2 == att.getNumericValue().shortValue());

    att = temp.findAttribute("versionB");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 3 == att.getNumericValue().byteValue());

    // read
    Array A = temp.read();

    int i,j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();

    // write
    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getDouble(ima.set(i,j)) == (double) (i*1000000+j*1000));
      }
    }
  }

}
