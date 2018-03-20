/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.tds;

import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestOnLocalServer;
import ucar.httpservices.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test restricted datasets
 *
 * @author caron
 * @since 3/16/2015
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestRestrictDataset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name="{0}")
  public static Collection<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][]{
            // These first 6 actually don't require cdmUnitTest/. Could be broken out into separate class that can
            // run on Travis.
            // explicit services
            {"/dodsC/testRestrictedDataset/testData2.nc.dds"},
            {"/cdmremote/testRestrictedDataset/testData2.nc?req=header"},
            {"/fileServer/testRestrictedDataset/testData2.nc"},
            // default services
            {"/dodsC/testRestrictedDataset/testData.nc.dds"},
            {"/cdmremote/testRestrictedDataset/testData.nc?req=header"},
            {"/fileServer/testRestrictedDataset/testData.nc"},
         //   {"/wms/testRestrictedDataset/testData2.nc?service=WMS&version=1.3.0&request=GetCapabilities"},

            // restricted DatasetScan
            {"/dodsC/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc.html"},
            {"/cdmremote/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc?req=header"},
            {"/fileServer/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc"},
        //    {"/wms/testRestrictedScan/20131102/PROFILER_wind_06min_20131102_2354.nc?service=WMS&version=1.3.0&request=GetCapabilities"},

            // restricted GRIB collections
            {"/dodsC/restrictCollection/GFS_CONUS_80km/TwoD.dds"},
            {"/ncss/grid/restrictCollection/GFS_CONUS_80km/TwoD/dataset.html"},
            {"/cdmremote/restrictCollection/GFS_CONUS_80km/TwoD?req=header"},
        });
    }

    String path, query;

    public TestRestrictDataset(String path) {
      this.path = path;
    }

  @Test
  public void testFailNoAuth() {
    String endpoint = TestOnLocalServer.withHttpPath(path);
    logger.info(String.format("testRestriction req = '%s'", endpoint));

    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();

      Assert.assertTrue(statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN);

    } catch (ucar.httpservices.HTTPException e) {
      Assert.fail(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testFailBadUser() {
    String endpoint = TestOnLocalServer.withHttpPath(path);
    logger.info(String.format("testRestriction req = '%s'", endpoint));

    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      session.setCredentials(new UsernamePasswordCredentials("baadss", "changeme"));

      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      Assert.assertTrue(statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN);

    } catch (ucar.httpservices.HTTPException e) {
      Assert.fail(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testFailBadPassword() {
    String endpoint = TestOnLocalServer.withHttpPath(path);
    logger.info(String.format("testRestriction req = '%s'", endpoint));

    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      session.setCredentials(new UsernamePasswordCredentials("tiggeUser", "changeme"));

      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();

      //if (statusCode != HttpStatus.SC_UNAUTHORIZED && statusCode != HttpStatus.SC_FORBIDDEN)
      //  assert false;
      Assert.assertTrue(statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN);

    } catch (ucar.httpservices.HTTPException e) {
      Assert.fail(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSuccess() {
    String endpoint = TestOnLocalServer.withHttpPath(path);
    logger.info(String.format("testRestriction req = '%s'", endpoint));

    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      session.setCredentials(new UsernamePasswordCredentials("tds", "secret666"));

      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();

      Assert.assertEquals(200, statusCode);

    } catch (ucar.httpservices.HTTPException e) {
      Assert.fail(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  // from 4.6
  @Test
  public void testRestriction() {
    String endpoint = TestOnLocalServer.withHttpPath(path);
    logger.info(String.format("testRestriction req = '%s'", endpoint));
    try {
      try (HTTPMethod method = HTTPFactory.Get(endpoint)) {
        int statusCode = method.execute();
        if (statusCode != HttpStatus.SC_UNAUTHORIZED && statusCode != HttpStatus.SC_FORBIDDEN) {
          logger.error(String.format("statuscode=%d expected HttpStatus.SC_UNAUTHORIZED or HttpStatus.SC_FORBIDDEN", statusCode));
          assert false;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

}

