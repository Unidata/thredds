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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
public class TestJustRead {
  private boolean show = false;

  @Test
  public void testReadConventionFiles() throws Exception {
    List<String> filesRead = readAllDir( TestDir.cdmUnitTestDir + "conventions", new LinkedList<String>());
    assert !filesRead.isEmpty() : String.format(
            "No files read in \"%s\". Is cdmUnitTestDir accessible?", TestDir.cdmUnitTestDir +  "conventions");
  }

  // Returns files read.
  private List<String> readAllDir(String dirName, List<String> filesRead) throws Exception {
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();

    if (allFiles != null) {
      for (File file : allFiles) {
        String path = file.getAbsolutePath();
        if (file.isDirectory()) {
          readAllDir(path, filesRead);
        } else if (path.endsWith(".nc")) {
          filesRead.add(path);
          doOne(path);
        } else {
          // Not a directory or NetCDF file. Do nothing.
        }
      }
    }

    return filesRead;
  }

  @Test
  public void testProblem() throws Exception {
    show = true;
    Aggregation.setPersistenceCache(new DiskCache2("/.unidata/aggCache", true, -1, -1));
    doOne( TestDir.cdmUnitTestDir+"/ft/grid/cg/cg.ncml");
    Aggregation.setPersistenceCache(null);
  }

  private void doOne(String filename) throws Exception {
    try (NetcdfDataset ncDataset = NetcdfDataset.openDataset(filename, true, null)) {
      if (show) ncDataset.writeNcML(System.out, null);
    }
  }
}
