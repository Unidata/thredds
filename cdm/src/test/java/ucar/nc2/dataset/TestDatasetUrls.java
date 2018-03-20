/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Test DatasetUrls protocol parsing
 *
 * @author caron
 * @since 10/20/2015.
 */
public class TestDatasetUrls {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static final boolean show = false;
  
  protected void protocheck(String path, String expected) {
    if (expected == null)
      expected = "";

    List<String> protocols = DatasetUrl.getProtocols(path);
    StringBuilder buf = new StringBuilder();
    for (String s : protocols) {
      buf.append(s);
      buf.append(":");
    }
    String result = buf.toString();
    boolean ok = expected.equals(result);
    if (show || !ok) System.out.printf(" path=%s; result=%s; pass=%s\n", path, result, ok);
    Assert.assertEquals(path, expected, result);
  }

  @Test
  public void
  testGetProtocols() {
    System.out.printf("TestMisc.testGetProtocols%n");
    protocheck("http://server/thredds/dodsC/", "http:");
    protocheck("dods://thredds-test.unidata.ucar.edu/thredds/dodsC/grib/NCEP/NAM/CONUS_12km/best", "dods:");
    protocheck("dap4://ucar.edu:8080/x/y/z", "dap4:");
    protocheck("dap4:https://ucar.edu:8080/x/y/z", "dap4:https:");
    protocheck("file:///x/y/z", "file:");
    protocheck("file://c:/x/y/z", "file:");
    protocheck("file:c:/x/y/z", "file:");
    protocheck("file:/blah/blah/some_file_2014-04-13_16:00:00.nc.dds", "file:");
    protocheck("/blah/blah/some_file_2014-04-13_16:00:00.nc.dds", "");
    protocheck("c:/x/y/z", null);
    protocheck("x::a/y/z", null);
    protocheck("x::/y/z", null);
    protocheck("::/y/z", "");
    protocheck("dap4:&/y/z", null);
    protocheck("file:x/z::a", "file:");
    protocheck("x/z::a", null);

    protocheck("thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly", "thredds:http:");
    // protocheck("thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData", "thredds:file:");
    protocheck("thredds:resolve:http://thredds.ucar.edu:8080/thredds/catalog/model/NCEP/NAM/CONUS_12km/latest.xml", "thredds:resolve:http:");
    protocheck("cdmremote:http://server:8080/thredds/cdmremote/data.nc", "cdmremote:http:");
    protocheck("dap4:http://thredds.ucar.edu:8080/thredds/fmrc/NCEP/GFS/CONUS_95km/files/GFS_CONUS_95km_20070319_0600.grib1", "dap4:http:");
  }
}
