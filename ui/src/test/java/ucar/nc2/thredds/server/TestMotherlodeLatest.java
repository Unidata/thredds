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
package ucar.nc2.thredds.server;

import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.*;

public class TestMotherlodeLatest extends TimerTask {
  static private final String server1 = "http://"+ TestDir.threddsTestServer+"/";
  //static private final String server1 = "http://thredds.cise-nsf.gov:8080/";
  static private final String server2 = "http://"+TestDir.threddsTestServer+"/";

  // fmrc
  static private final String latestPrefix = "thredds/catalog/fmrc/";
  static private final String latestSuffix = "/files/latest.xml";

  // non-fmrc
  //static private final String latestPrefix = "thredds/catalog/model/";
  //static private final String latestSuffix = "/latest.xml";

  static private final String bestPrefix = "thredds/dodsC/fmrc/";
  static private final String bestSuffix = "_best.ncd";

  // dods://motherlode.ucar.edu:9080/thredds/dodsC/fmrc/NCEP/RUC2/CONUS_20km/pressure/NCEP-RUC2-CONUS_20km-pressure_best.ncd
  // http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/DGEX/CONUS_12km/files/latest.xml;

  private String suffix;
  private String model;

  TestMotherlodeLatest(String model, String suffix) {
    this.model = model;
    this.suffix = suffix;
  }


  public void run() {
    try {
      System.out.printf("Run %s%n", new Date());
      //doAll();
      doOne(model, suffix);
    } catch (IOException e) {
      e.printStackTrace();  
    }
  }

  private boolean checkRank = false;
  private boolean checkSize = false;
  private boolean readData = true;

  void doOne(String model, String suffix) throws IOException {

    GridDataset gds1 = getDataset(makeDatasetURL( server1, model, suffix), "1");
    GridDataset gds2 = getDataset(makeDatasetURL( server2, model, suffix), "2" );
    System.out.printf(" compare 1 to 2%n");
    compare(gds1, gds2, checkRank, checkSize, readData);
    System.out.printf(" compare 2 to 1%n");
    compare(gds2, gds1, false, false, false);
    System.out.printf(" DONE%n%n");

    gds1.close();
    gds2.close();
  }

  String makeDatasetURL(String server, String model, String suffix) {
    if (suffix.endsWith(".xml"))
      return "thredds:resolve:" + server + latestPrefix + model + suffix;
    else
      return server + bestPrefix + model + suffix;
  }

  private GridDataset getDataset(String ds, String which) throws IOException {

    Formatter errlog = new Formatter();
    FeatureDataset result = FeatureDatasetFactoryManager.open(FeatureType.GRID, ds, null, errlog);
    //System.out.printf(" %s result errlog= %s%n", ds, errlog);
    assert result != null : ds;
    assert result instanceof GridDataset;

    GridDataset dataset = (GridDataset) result;
    System.out.printf(" %s dataset=%s%n", which, dataset.getLocationURI());

    return dataset;
  }

  private void compare(GridDataset gds1, GridDataset gds2, boolean checkRank, boolean checkSize, boolean readData) {

    for (GridDatatype grid1 : gds1.getGrids()) {

      try {
        GridDatatype grid2 = gds2.findGridDatatype(grid1.getFullName());
        if (grid2 == null) {
          System.out.printf("%s MISSING%n", grid1.getFullName());
          continue;
        }
        if (checkRank) {
          if (grid1.getRank() != grid2.getRank()) {
            System.out.printf("%s rank mismatch: %s != %s%n", grid1.getFullName(), show(grid1), show(grid2));
            continue;
          }
        }
        if (checkSize) {
          long size1 = new Section(grid1.getShape()).computeSize();
          long size2 = new Section(grid2.getShape()).computeSize();
          if (size1 != size2) {
            System.out.printf("%s size mismatch: %s != %s%n", grid1.getFullName(), show(grid1), show(grid2));
            continue;
          }
        }

        if (readData) {
          if (grid1.getRank() != grid2.getRank()) {
            System.out.printf("%s rank mismatch: %s != %s%n", grid1.getFullName(), show(grid1), show(grid2));
            continue;
          }

          int timeIdx = choose( grid1.getTimeDimension());
          int zIndex = choose( grid1.getZDimension());
          //System.out.printf("Compare %s %s%n", gds1.getLocationURI(), grid1.getName());
          Array data1 = grid1.readDataSlice(timeIdx, zIndex, -1, -1);
          Array data2 = grid2.readDataSlice(timeIdx, zIndex, -1, -1);
          try {
            CompareNetcdf2 cn = new CompareNetcdf2( new Formatter(System.out), true, true, true);
            cn.compareData(grid1.getFullName(), data1, data2, true);
          } catch (Throwable t) {
            System.out.printf("Failed on %s for %s (%d,%d,-1,-1)%n:%s%n", gds1.getLocationURI(), grid1.getFullName(), timeIdx, zIndex, t.getMessage());
          }
        }
        
      } catch (Throwable e) {
        // e.printStackTrace();
        System.out.printf(" *** %s %n", e.getMessage());
      }

    }

  }

  Random r = new Random();
  private int choose(Dimension d) {
    if (d == null) return -1;
    if (d.getLength() < 2) return -1;
    return r.nextInt(d.getLength());
  }

  private String show(GridDatatype grid) {
    Section s = new Section(grid.getShape());
    return s.toString();
  }

  private Map<String, CoordinateAxis1DTime> getTimes(GridDataset gds) throws Exception {
    Map<String, CoordinateAxis1DTime> map = new HashMap<String, CoordinateAxis1DTime>(5);
    for (GridDataset.Gridset gset : gds.getGridsets()) {
      GridCoordSystem gsys = gset.getGeoCoordSystem();
      CoordinateAxis1DTime time = gsys.getTimeAxis1D();
      map.put(time.getFullName(), time);
    }

    for (CoordinateAxis1DTime time : map.values()) {
      System.out.printf(" %s len = %d%n", time.getFullName(), time.getSize());
    }

    return map;
  }


  private void compare(Map<String, CoordinateAxis1DTime> map1, Map<String, CoordinateAxis1DTime> map2) throws Exception {

    for (CoordinateAxis1DTime time1 : map1.values()) {
      CoordinateAxis1DTime time2 = map2.get(time1.getFullName());
      assert time2 != null;
      assert time1.getSize() == time2.getSize();
    }

  }


  public static void main(String args[]) throws Exception {
    Timer timer = new Timer();

    // http://motherlode.ucar.edu:9080/thredds/catalog/model/NCEP/RUC2/CONUS_20km/pressure/latest.xml

    /*
    TestMotherlodeLatest test1 = new TestMotherlodeLatest("NCEP/RUC2/CONUS_20km/pressure", bestSuffix);
    timer.schedule(test1, 0, 1000 * 60 * 2); // 2 mins

    TestMotherlodeLatest test2 = new TestMotherlodeLatest("NCEP/RUC2/CONUS_20km/pressure", latestSuffix);
    timer.schedule(test2, 0, 1000 * 60 * 2); // 2 mins

    TestMotherlodeLatest test3 = new TestMotherlodeLatest("NCEP/NAM/CONUS_12km", latestSuffix);
    timer.schedule(test3, 0, 1000 * 60 * 10); // 10 mins

   TestMotherlodeLatest test4 = new TestMotherlodeLatest("NCEP/NAM/CONUS_12km", bestSuffix);
    timer.schedule(test4, 0, 1000 * 60 * 10); // 10 mins  */

    // /thredds/catalog/grib/NCEP/RAP/CONUS_40km/files/latest.html
    TestMotherlodeLatest testAll = new TestMotherlodeLatest("NCEP/RUC2/CONUS_20km/pressure", latestSuffix);
    //testAll.doAll();
  }

}
