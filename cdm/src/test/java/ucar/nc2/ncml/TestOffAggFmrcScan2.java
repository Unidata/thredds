// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ncml;

import junit.framework.TestCase;

import ucar.nc2.*;

import java.io.StringReader;

public class TestOffAggFmrcScan2 extends TestCase {

  public TestOffAggFmrcScan2( String name) {
    super(name);
  }

  public void testOpen() throws Exception {
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunSingleCollection' timeUnitsChange='true' >\n" +
      "    <scanFmrc location='D:/data/grib/rtmodels/' regExp='.*_nmm\\.GrbF[0-9]{5}$'\n" +
      "           runDateMatcher='yyMMddHH#_nmm.GrbF#'\n" +
      "           forecastOffsetMatcher='#_nmm.GrbF#HHH'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";

    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcScan2.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);

    ncfile.close();
  }

  public void testOpenNomads() throws Exception {
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunSingleCollection' timeUnitsChange='true' >\n" +
      "    <scanFmrc location='D:/data/nomads/gfs-hi' suffix='.grb'\n" +
      "           runDateMatcher='#gfs_3_#yyyyMMdd_HH'\n" +
      "           forecastOffsetMatcher='HHH#.grb#'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";
    
    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcNomads.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);

    ncfile.close();
  }


}
