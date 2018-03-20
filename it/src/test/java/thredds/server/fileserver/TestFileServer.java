/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.fileserver;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.util.ContentType;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 3/8/2016.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestFileServer {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{"fileServer/rdaTest/ds094.2_dt/files/flxf01.gdas.A_PCP.SFC.01Z.grb2.gbx9", ContentType.binary});
    result.add(new Object[]{"fileServer/testStationFeatureCollection/files/Surface_METAR_20060325_0000.nc", ContentType.netcdf});
    result.add(new Object[]{"fileServer/scanLocal/2004050312_eta_211.nc", ContentType.netcdf});
    result.add(new Object[]{"fileServer/scanLocal/esfgTest.html", ContentType.html});
    result.add(new Object[]{"fileServer/testNAMfmrc/files/20060925_0600.nc", ContentType.netcdf});
    result.add(new Object[]{"fileServer/scanCdmUnitTests/formats/netcdf3/files/ctest0.nc", ContentType.netcdf}); // make sure files doesnt get removed

    return result;
  }

  String path;
  ContentType type;

  public TestFileServer(String path, ContentType type) {
    this.path = path;
    this.type = type;
  }

  @Test
  public void downloadFile() {
    String endpoint = TestOnLocalServer.withHttpPath(path);
    TestOnLocalServer.getContent(endpoint, 200, type);
  }
}
