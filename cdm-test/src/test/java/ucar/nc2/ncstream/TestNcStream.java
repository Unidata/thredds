/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ncstream;


import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.stream.NcStreamWriter;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * test ncstream, write to temp file then reading back and comparing to original.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestNcStream {

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/station/Surface_METAR_20080205_0000.nc"});
    // result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/grid/RUC2_CONUS_40km_20070709_1800.nc"});
    // result.add(new Object[]{TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB"});
    result.add(new Object[]{TestDir.cdmUnitTestDir + "formats/netcdf4/compound/tst_comp.nc4"});

    return result;
  }

  String filename;

  public TestNcStream(String filename) {
    this.filename = filename;
  }

  @Test
  public void testNcStreamWriter() throws IOException, InvalidRangeException {
    System.out.println("\nFile= " + filename + " size=" + new File(filename).length());

    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      long start = System.currentTimeMillis();
      int pos = filename.lastIndexOf("/");
      if (pos < 0) pos = filename.lastIndexOf("\\");
      String name = filename.substring(pos);
      String fileOut = TestDir.temporaryLocalDataDir + name+".ncs";
      NcStreamWriter writer = new NcStreamWriter(ncfile, filename);

      try (FileOutputStream fos = new FileOutputStream(fileOut)) {
        writer.streamAll(fos);
      }

      long took = System.currentTimeMillis() - start;
      System.out.println("N3streamWriter took " + took + " msecs");

      try (NetcdfFile file2 = NetcdfFile.open(fileOut)) {
        assert ucar.unidata.test.util.CompareNetcdf.compareFiles(ncfile, file2, true, false, false);
      }

    }
  }

}

