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
package thredds.server.opendap;

import java.io.*;
import java.util.*;

import org.junit.*;
import thredds.TestWithLocalServer;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.util.StringUtil2;

/**
 * compare files served through netcdf-DODS server.
 */

public class TestDODScompareWithFiles {
  static boolean showCompare = false, showEach = false, showStringValues = false;
  String contentRoot = TestDir.cdmUnitTestDir + "formats";

  int fail = 0;
  int success = 0;

  @org.junit.Test
  public void testCompare() throws IOException {
    doOne("conventions/zebra/SPOL_3Volumes.nc");
    doOne("conventions/coards/inittest24.QRIDV07200.ncml"); //
    doOne("conventions/atd/rgg.20020411.000000.lel.ll.nc"); //
    doOne("conventions/awips/awips.nc"); //
    doOne("conventions/cf/ipcc/cl_A1.nc"); //
    doOne("conventions/csm/o3monthly.nc"); //
    doOne("conventions/gdv/OceanDJF.nc"); //
    doOne("conventions/gief/coamps.wind_uv.nc"); //
    doOne("conventions/mars/temp_air_01082000.nc"); //
    doOne("conventions/mm5/n040.nc"); //
    doOne("conventions/nuwg/eta.nc"); //
    doOne("conventions/nuwg/ruc.nc"); //
    doOne("conventions/wrf/wrfout_v2_Lambert.nc"); //
    doOne("conventions/mm5/n040.nc"); //

    /* doOne("grib2/ndfd.wmo");
    doOne("grib2/eta2.wmo");
    doOne("image/dmsp/F14200307192230.n.OIS");
    doOne("image/gini/n0r_20041013_1852-u");
    doOne("image/gini/n0r_20041013_1852"); //
    doOne("ldm/grib/AVN_H.wmo"); //
    doOne("AStest/wam/Atl/EPPE_WAM_Atl_200202281500.nc"); // */

    System.out.printf("success = %d fail = %d%n", success, fail);
    assert fail == 0 : "failed="+fail;
  }

  String path = "dodsC/scanCdmUnitTests/";

  public void testCompareAll() throws IOException {
    readAllDir(contentRoot + "ncml", ".ncml");
  }

  void readAllDir(String dirName, String suffix) throws IOException {
    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (allFiles == null) return;

    for (File f : allFiles) {
      if (f.isDirectory()) continue;

      String name = f.getAbsolutePath();
      if (!name.endsWith(suffix)) continue;

      doOne(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        readAllDir(f.getAbsolutePath(), suffix);
    }

  }


  private void doOne(String filename) throws IOException {
    filename = StringUtil2.replace(filename, '\\', "/");
    String dodsUrl = TestWithLocalServer.server + path + filename;
    String localPath = contentRoot + filename;
    compareDatasets(dodsUrl, localPath);
  }

  private void compareDatasets(String dodsUrl, String localPath) throws IOException {
    System.out.printf("--Compare %s to %s%n", localPath, dodsUrl);
    NetcdfDataset ncfile = null, ncremote = null;
    try {
      ncfile = NetcdfDataset.openDataset(localPath);
      ncremote = NetcdfDataset.openDataset(dodsUrl);

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, false);
      boolean ok = mind.compare(ncfile, ncremote, new DodsObjFilter(), false, false, false);
      if (!ok) {
        System.out.printf(" NOT OK%n%s%n", f);
        fail++;
      } else {
        success++;
      }

    } finally {
      if (ncfile != null) ncfile.close();
      if (ncremote != null) ncremote.close();
    }
  }

  private class DodsObjFilter implements CompareNetcdf2.ObjFilter {

    @Override
    public boolean attOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      if (name.equals(_Coordinate.Axes)) return false;

      return true;
    }

  }


}
