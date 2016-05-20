// $Id: $
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

package ucar.nc2.ncml;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.StringReader;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcScan2 {

  @Ignore("Cant use Fmrc on GRIB")
  @Test
  public void testOpen() throws Exception {
    String dataDir = TestDir.cdmUnitTestDir + "ft/fmrc/rtmodels/";
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunSingleCollection' timeUnitsChange='true' >\n" +
      "    <scanFmrc location='"+dataDir+"' regExp='.*_nmm\\.GrbF[0-9]{5}$'\n" +
      "           runDateMatcher='yyMMddHH#_nmm.GrbF#'\n" +
      "           forecastOffsetMatcher='#_nmm.GrbF#HHH'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";

    String filename = "fake:TestOffAggFmrcScan2/aggFmrcScan2.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);
    
    TestDir.readAllData(ncfile);

    ncfile.close();
  }

  @Ignore("Cant use Fmrc on GRIB")
  @Test
  public void testOpenNomads() throws Exception {
    String dataDir = TestDir.cdmUnitTestDir + "ft/fmrc/nomads/";
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunSingleCollection' timeUnitsChange='true' >\n" +
      "    <scanFmrc location='"+dataDir+"' suffix='.grb'\n" +
      "           runDateMatcher='#gfs_3_#yyyyMMdd_HH'\n" +
      "           forecastOffsetMatcher='HHH#.grb#'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";
    
    String filename = "fake:TestOffAggFmrcScan2/aggFmrcNomads.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);

    TestDir.readAllData(ncfile);

    ncfile.close();
  }

}
