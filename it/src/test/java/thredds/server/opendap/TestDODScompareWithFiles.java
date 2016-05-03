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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * compare files served through netcdf-DODS server.
 */

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestDODScompareWithFiles {
  static boolean showCompare = false, showEach = false, compareData  = false;
  static String contentRoot = TestDir.cdmUnitTestDir;

   @Parameterized.Parameters(name="{0}")
   public static List<Object[]> getTestParameters() {

    List<Object[]>  result = new ArrayList<Object[]>(20);

    result.add( new Object[]{"conventions/zebra/SPOL_3Volumes.nc"});
    result.add( new Object[]{"conventions/coards/inittest24.QRIDV07200.ncml"}); //
    result.add( new Object[]{"conventions/atd/rgg.20020411.000000.lel.ll.nc"}); //
    result.add( new Object[]{"conventions/awips/awips.nc"}); //
    result.add( new Object[]{"conventions/cf/ipcc/cl_A1.nc"}); //
    result.add( new Object[]{"conventions/csm/o3monthly.nc"}); //
    result.add( new Object[]{"conventions/gdv/OceanDJF.nc"}); //
    result.add( new Object[]{"conventions/gief/coamps.wind_uv.nc"}); //
    result.add( new Object[]{"conventions/mars/temp_air_01082000.nc"}); //
    result.add( new Object[]{"conventions/mm5/n040.nc"}); //
    result.add( new Object[]{"conventions/nuwg/eta.nc"}); //
    result.add( new Object[]{"conventions/nuwg/ruc.nc"}); //
    result.add( new Object[]{"conventions/wrf/wrfout_v2_Lambert.nc"}); //
    result.add( new Object[]{"conventions/mm5/n040.nc"}); //

    return result;
 	}


  public TestDODScompareWithFiles(String filename) {
    this.filename = filename;
  }

  String filename;
  int fail = 0;
  int success = 0;

  String path = "dodsC/scanCdmUnitTests/";

  /* public void testCompareAll() throws IOException {
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

      compare(name);
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        readAllDir(f.getAbsolutePath(), suffix);
    }

  } */

  // @Test
  public void problem() throws IOException {
    String filename = "conventions/coards/inittest24.QRIDV07200.ncml";
    String dodsUrl = TestWithLocalServer.withPath(path + filename);
    String localPath = contentRoot + filename;
    compareDatasets(dodsUrl, localPath);
  }

  @Test
  public void compare() throws IOException {
    filename = StringUtil2.replace(filename, '\\', "/");
    String dodsUrl = TestWithLocalServer.withPath(path + filename);
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
      boolean ok = mind.compare(ncfile, ncremote, new DodsObjFilter(), showCompare, showEach, compareData);
      if (!ok) {
        System.out.printf("--Compare %s%n", filename);
        System.out.printf("  %s%n", f);
        fail++;
      } else {
        success++;
      }
      Assert.assertTrue( localPath+ " != "+dodsUrl, ok);

    } finally {
      if (ncfile != null) ncfile.close();
      if (ncremote != null) ncremote.close();
    }
  }

  public static class DodsObjFilter implements CompareNetcdf2.ObjFilter {

    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      if (name.equals(_Coordinate.Axes)) return false;
      if (name.equals(CDM.UNSIGNED)) return false;

      return true;
    }

    @Override public boolean varDataTypeCheckOk(Variable v) { return true; }
  }


}
