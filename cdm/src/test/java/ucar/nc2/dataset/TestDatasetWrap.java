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
package ucar.nc2.dataset;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CompareNetcdf;

import java.io.File;

public class TestDatasetWrap extends TestCase {

  public TestDatasetWrap( String name) {
    super(name);
  }

  public void testDatasetWrap() throws Exception {
    doOne(TestAll.testdataDir + "grid/netcdf/nuwg/eta.nc");
    //readAllDir( TestAll.testdataDir+ "grid/netcdf");
  }

  void readAllDir(String dirName) throws Exception {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".nc"))
        doOne(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  private void doOne(String filename) throws Exception {
    NetcdfFile ncfile = NetcdfDataset.acquireFile(filename, null);
    NetcdfDataset ncWrap = new NetcdfDataset( ncfile, true);

    NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
    System.out.println(" dataset wraps= "+filename);

    CompareNetcdf.compareFiles(ncd, ncWrap);
    ncd.close();
    ncWrap.close();
  }
}
