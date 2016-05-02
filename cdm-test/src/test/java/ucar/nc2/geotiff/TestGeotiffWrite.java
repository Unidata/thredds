/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.geotiff;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoTiffWriter2 writing geotiffs
 *
 * @author caron
 * @since 7/31/2014
 */
  @RunWith(Parameterized.class)
  @Category(NeedsCdmUnitTest.class)
  public class TestGeotiffWrite {
    static public String topdir = TestDir.cdmUnitTestDir ;

    @Parameterized.Parameters(name="{0}")
    public static List<Object[]> getTestParameters() {
      List<Object[]> result = new ArrayList<>();

      result.add(new Object[]{topdir + "formats/dmsp/F14200307192230.n.OIS", "infraredImagery", new LatLonRect(new LatLonPointImpl(-5, -52.0), new LatLonPointImpl(25, -20.0))});

      // this fails
      //result.add(new Object[]{topdir + "formats/netcdf4/ncom_relo_fukushima_1km_tmp_2011040800_t000.nc4", "surf_salt_flux", new LatLonRect(new LatLonPointImpl(43, 141), 5, 5)});

      return result;
    }


    String filename, field;
  LatLonRect llbb;
    public TestGeotiffWrite(String filename, String field, LatLonRect llbb) {
      this.filename = filename;
      this.field = field;
      this.llbb = llbb;
    }

  @Test
  public void testWrite() throws IOException {

    File f = new File(filename);

    String fileOut =  TestDir.temporaryLocalDataDir + f.getName();
    GeoTiffWriter2 writer = new GeoTiffWriter2(fileOut);
    writer.writeGrid(filename, field, 0, 0, true, llbb);
    writer.close();

    // read it back in
    try (GeoTiff geotiff = new GeoTiff(fileOut)) {
      geotiff.read();
      System.out.println("geotiff read in = " + geotiff.showInfo());
      //geotiff.testReadData();
    }
  }
}
