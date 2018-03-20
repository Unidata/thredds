/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** test FileWriting, then reading back and comparing to original. */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestCompareFileWriter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

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
    File fout = tempFolder.newFile();
    System.out.printf("Write %s %n   to %s (%s %s)%n", fin.getAbsolutePath(), fout.getAbsolutePath(), fout.exists(), fout.getParentFile().exists());

    try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(fin.getPath(), null)) {
      FileWriter2 fileWriter = new FileWriter2(ncfileIn, fout.getPath(), NetcdfFileWriter.Version.netcdf3, null);

      try (NetcdfFile ncfileOut = fileWriter.write()) {
        assert ucar.unidata.util.test.CompareNetcdf.compareFiles(ncfileIn, ncfileOut) == same;
      }
    }
    System.out.printf("%n");
  }

}
