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
/**
 * User: rkambic
 * Date: Oct 22, 2009
 * Time: 3:12:19 PM
 */

package ucar.nc2.iosp.gempak;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestReadingGempak {

  @Test
  public void testCompare() throws IOException {
    doAll(TestDir.cdmUnitTestDir + "formats/gempak");
  }

  void doAll(String dirName) throws IOException {
    File dir = new File(dirName);
    System.out.printf("%nIn directory %s%n", dir.getPath());
    for (File child : dir.listFiles()) {
      if (child.isDirectory()) continue;
      NetcdfFile ncfile = null;
      try {
        // if( child.startsWith( "air"))  continue;
        System.out.printf("  Open File %s ", child.getPath());
        long start = System.currentTimeMillis();
        ncfile = NetcdfDataset.openFile(child.getPath(), null);
        String ft = ncfile.findAttValueIgnoreCase(null, "featureType", "none");
        String iosp = ncfile.getIosp().getFileTypeId();
        System.out.printf(" iosp=%s ft=%s took =%d ms%n", iosp, ft, (System.currentTimeMillis() - start));
      } catch (Throwable t) {
        System.out.printf(" FAILED =%s%n", t.getMessage());
        t.printStackTrace();
        if (ncfile != null) ncfile.close();
      }
    }

    for (File child : dir.listFiles()) {
      if (child.isDirectory()) doAll(child.getPath());
    }

  }

}
