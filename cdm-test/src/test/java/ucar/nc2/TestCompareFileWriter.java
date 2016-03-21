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
package ucar.nc2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** test FileWriting, then reading back and comparing to original. */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestCompareFileWriter {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{"formats/netcdf3/longOffset.nc", true});    // unlimited dimesnion = 0
    //result.add(new Object[]{"formats/dmsp/F14200307192230.n.OIS", true});
    result.add(new Object[]{"formats/gempak/grid/dgex_le.gem", true});
    result.add(new Object[]{"formats/gempak/surface/19580807_sao.gem", false});
    result.add(new Object[]{"formats/gini/SUPER-NATIONAL_8km_WV_20051128_2200.gini", true});
    result.add(new Object[]{"formats/grib1/radar_national.grib", true});
    result.add(new Object[]{"formats/grib2/200508041200.ngrid_gfs", true});
    // result.add(new Object[]{"formats/hdf4/17766010.hdf"});

    return result;
  }

  String filename;
  boolean same;

  public TestCompareFileWriter(String filename, boolean same) {
    this.filename = filename;
    this.same = same;
  }

  @Test
  public void doOne() throws IOException {
    File fin = new File(TestDir.cdmUnitTestDir+filename);
    File fout = new File(TestDir.temporaryLocalDataDir+fin.getName()+".nc");
    System.out.printf("Write %s %n   to %s (%s %s)%n", fin.getAbsolutePath(), fout.getAbsolutePath(), fout.exists(), fout.getParentFile().exists());

    try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(fin.getPath(), null)) {
      FileWriter2 fileWriter = new FileWriter2(ncfileIn, fout.getPath(), NetcdfFileWriter.Version.netcdf3, null);

      try (NetcdfFile ncfileOut = fileWriter.write()) {
        assert ucar.unidata.test.util.CompareNetcdf.compareFiles(ncfileIn, ncfileOut) == same;
      }
    }
    System.out.printf("%n");
  }

}
