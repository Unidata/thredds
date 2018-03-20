/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.cdmr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * Test a single file; compare to cdmremote
 *
 * @author caron
 * @since 10/21/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestCdmRemoteMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static String contentRoot = TestDir.cdmUnitTestDir + "formats";
  static String urlPath = "cdmremote/scanCdmUnitTests/formats";


  @Test
  public void testBackslashEscaped() throws Exception {
    String url = TestOnLocalServer.withHttpPath(urlPath + "/hdf5/grid_1_3d_xyz_aug.h5?req=data&var=HDFEOS_INFORMATION/StructMetadata\\.0");
    try (HTTPMethod m = HTTPFactory.Get(url)) {
      int statusCode = m.execute();
      System.out.printf("status = %d%n", statusCode);
    }
  }

  @Test
  public void problem() throws IOException {
    String problemFile = TestDir.cdmUnitTestDir + "formats/gempak/ndfd_20100913.gem";
    String name = StringUtil2.substitute(problemFile.substring(TestCdmRemoteCompareHeadersP.contentRoot.length()), "\\", "/");
    String remote = TestOnLocalServer.withHttpPath(TestCdmRemoteCompareHeadersP.urlPath + name);
    TestCdmRemoteCompareHeadersP.compareDatasets(problemFile, remote, false);
  }

  @Test
  public void zeroLenData() throws IOException {
    try (NetcdfFile ncremote = new CdmRemote(TestOnLocalServer.withHttpPath(urlPath + "/netcdf3/longOffset.nc"))) {
      Variable v = ncremote.findVariable(null, "time_whole");
      Array data = v.read();
      assert data.getSize() == 0;
    }
  }

  @Test
  public void backlashEscaped() throws IOException {
    try (NetcdfFile ncremote = new CdmRemote(TestOnLocalServer.withHttpPath(urlPath + "/hdf5/grid_1_3d_xyz_aug.h5"))) {
      Variable v = ncremote.findVariable("HDFEOS_INFORMATION/StructMetadata\\.0");
      Assert.assertNotNull(v);
      Array data = v.read();
      Assert.assertEquals(32000, data.getSize());
    }
  }

  @Test
  public void testByteOrder() {
    try {
      String filename = "/netcdf3/longOffset.nc";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath + filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), true, true, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  @Test
  public void testVlen() {
    try {
      String filename = "/netcdf4/vlen/tst_vl.nc";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath+filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), true, false, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  @Test
  public void testTopVlenInt() {
    try {
      String filename = "/netcdf4/vlen/vlenInt.nc";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath+filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), true, false, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  @Test
  public void testVlenInStructure() {
    try {
      String filename = "/netcdf4/vlen/IntTimSciSamp.nc";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath+filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), true, false, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  @Test
  public void testChardata() { // was failing on char data being sign extended
    try {
      String filename = "/netcdf3/files/c0_64.nc";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath+filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), true, false, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  @Test
  public void testStringArray() {
    try {
      String filename = "/netcdf4/files/tst_string_data.nc";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath+filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), true, false, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

  }

  @Test
  public void testVariableNameWithDot() {
    try {
      String filename = "/hdf5/grid_1_3d_xyz_aug.h5";
      String remoteFile = TestOnLocalServer.withHttpPath(urlPath+filename);
      CdmRemote ncfileRemote = new CdmRemote(remoteFile);

      String localFile = contentRoot+filename;
      NetcdfFile ncfileLocal = NetcdfFile.open(localFile);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfileLocal, ncfileRemote, new TestCdmRemoteCompareHeadersP.NcstreamObjFilter(), false, false, true);

      System.out.printf("--Compare %s to %s%n", localFile, remoteFile);
      System.out.printf("  %s%n", f);
      Assert.assertTrue(ok);

      ncfileLocal.close();
      ncfileRemote.close();

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }
  }
}
