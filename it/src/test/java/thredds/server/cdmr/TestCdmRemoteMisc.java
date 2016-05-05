/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.server.cdmr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
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
import java.util.Formatter;

/**
 * Test a single file; compare to cdmremote
 *
 * @author caron
 * @since 10/21/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestCdmRemoteMisc {
  static String contentRoot = TestDir.cdmUnitTestDir + "formats";
  static String urlPath = "cdmremote/scanCdmUnitTests/formats";


  @Test
  public void testBackslashEscaped() throws Exception {
    String url = TestWithLocalServer.withPath(urlPath + "/hdf5/grid_1_3d_xyz_aug.h5?req=data&var=HDFEOS_INFORMATION/StructMetadata\\.0");
    try (HTTPMethod m = HTTPFactory.Get(url)) {
      int statusCode = m.execute();
      System.out.printf("status = %d%n", statusCode);
    }
  }

  @Test
  public void problem() throws IOException {
    // http://localhost:8081/thredds/cdmremote/scanCdmUnitTests/formats/grib2/SingleRecordNbits0.grib2?req=header
    String problemFile = TestDir.cdmUnitTestDir + "formats/gempak/ndfd_20100913.gem";
    String name = StringUtil2.substitute(problemFile.substring(TestCdmRemoteCompareHeadersP.contentRoot.length()), "\\", "/");
    String remote = TestWithLocalServer.withPath(TestCdmRemoteCompareHeadersP.urlPath + name);
    TestCdmRemoteCompareHeadersP.compareDatasets(problemFile, remote, false);
  }

  @Test
  public void zeroLenData() throws IOException {
    // http://localhost:8081/thredds/cdmremote/scanCdmUnitTests/formats/netcdf3/longOffset.nc?req=data&var=time_whole
    try (NetcdfFile ncremote = new CdmRemote(TestWithLocalServer.withPath(urlPath + "/netcdf3/longOffset.nc"))) {
      Variable v = ncremote.findVariable(null, "time_whole");
      Array data = v.read();
      assert data.getSize() == 0;
    }
  }

  @Test
  public void backlashEscaped() throws IOException {
    // cdmremote:http://localhost:8081/thredds/cdmremote/scanCdmUnitTests/formats/hdf5/grid_1_3d_xyz_aug.h5
    try (NetcdfFile ncremote = new CdmRemote(TestWithLocalServer.withPath(urlPath + "/hdf5/grid_1_3d_xyz_aug.h5"))) {
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
      String remoteFile = TestWithLocalServer.withPath(urlPath + filename);
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
      String remoteFile = TestWithLocalServer.withPath(urlPath+filename);
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
      String remoteFile = TestWithLocalServer.withPath(urlPath+filename);
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
      String remoteFile = TestWithLocalServer.withPath(urlPath+filename);
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
      String remoteFile = TestWithLocalServer.withPath(urlPath+filename);
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
      String remoteFile = TestWithLocalServer.withPath(urlPath+filename);
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
      String remoteFile = TestWithLocalServer.withPath(urlPath+filename);
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
