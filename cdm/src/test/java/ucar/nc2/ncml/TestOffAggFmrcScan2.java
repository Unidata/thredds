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

import junit.framework.TestCase;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import java.io.StringReader;
import java.io.IOException;

public class TestOffAggFmrcScan2 extends TestCase {

  public TestOffAggFmrcScan2( String name) {
    super(name);
  }


  public void testOpen() throws Exception {
    String dataDir = TestAll.cdmUnitTestDir + "rtmodels/";
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunSingleCollection' timeUnitsChange='true' >\n" +
      "    <scanFmrc location='"+dataDir+"' regExp='.*_nmm\\.GrbF[0-9]{5}$'\n" +
      "           runDateMatcher='yyMMddHH#_nmm.GrbF#'\n" +
      "           forecastOffsetMatcher='#_nmm.GrbF#HHH'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";

    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcScan2.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);
    
    TestAll.readAll(ncfile);

    ncfile.close();
  }

  public void testOpenNomads() throws Exception {
    String dataDir = TestAll.cdmUnitTestDir + "fmrc/nomads/";
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunSingleCollection' timeUnitsChange='true' >\n" +
      "    <scanFmrc location='"+dataDir+"' suffix='.grb'\n" +
      "           runDateMatcher='#gfs_3_#yyyyMMdd_HH'\n" +
      "           forecastOffsetMatcher='HHH#.grb#'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";
    
    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcNomads.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);

    TestAll.readAll(ncfile);

    ncfile.close();
  }

  public void utestMargolis() throws IOException, InvalidRangeException {
    String dataDir = TestAll.cdmUnitTestDir + "nomads/gfs-hi/";
    GridDataset gridDataset = GridDataset.open( "D:/AStest/margolis/grib2ncdf.ncml" );
    GeoGrid grid = gridDataset.findGridByName( "Turbulence_SIGMET_AIRMET" );
    System.out.println("Grid= "+grid+" section="+ new Section(grid.getShape()));
    System.out.println(" coordSys= "+grid.getCoordinateSystem());

    GeoGrid subset = (GeoGrid) grid.makeSubset(new Range(0, 0), null, new Range(1,1), null, null, null);
    System.out.println("subset= "+subset+" section="+ new Section(subset.getShape()));
    System.out.println(" coordSys= "+subset.getCoordinateSystem());

    gridDataset.close();
  }

  public static void main(String[] args) throws IOException {
    String fname = "D:/work/signell/rtofs/rtofs.ncml";
    NetcdfFile ncfile = NetcdfDataset.openDataset(fname);
    TestAll.readAll(ncfile);
    ncfile.close();
  }


}
