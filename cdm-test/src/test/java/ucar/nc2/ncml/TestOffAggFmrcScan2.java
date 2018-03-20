/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: $


package ucar.nc2.ncml;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcScan2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
