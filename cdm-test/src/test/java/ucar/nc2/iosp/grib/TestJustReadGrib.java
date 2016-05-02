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
package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/** Test reading grib files */
@Category(NeedsCdmUnitTest.class)
public class TestJustReadGrib  {
  private boolean show = false;

  @Test
  public void readGrib1Files() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "formats/grib1", null, false);
  }

  @Test
  public void readGrib2Files() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "formats/grib2", null, false);
  }

  @Test
  public void readNcepFiles() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "tds/ncep", null, true);
  }

  @Test
  public void readFnmocFiles() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "tds/fnmoc", null, true);
  }

  void readAllDir(String dirName, String suffix, boolean recurse) throws Exception {
    TestDir.actOnAll(dirName, new GribFilter(), new GribAct(), recurse);
  }

  @Test
  public void testProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/testproj2.grb";
    System.out.println("read file= "+filename);
    NetcdfFile ncfile = NetcdfDataset.openFile( filename, null);
    ncfile.close();
  }

  class GribFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
      if (file.isDirectory()) return false;
      String path = file.getPath();
      if (path.endsWith(".gbx")) return false;
      if (path.endsWith(".gbx8")) return false;
      if (path.endsWith(".gbx9")) return false;
      if (path.endsWith(".ncx")) return false;
      if (path.endsWith(".ncx2")) return false;
      if (path.endsWith(".ncx3")) return false;
      try {
        System.out.printf("opening %s%n", file.getCanonicalPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
      return true;
    }
  }

  class GribAct implements TestDir.Act {

    @Override
    public int doAct(String filename) throws IOException {
      System.out.println("read file= "+filename);
      try (NetcdfFile ncfile = NetcdfDataset.openFile( filename, null)) {
        return 1;
      }
    }
  }
}
