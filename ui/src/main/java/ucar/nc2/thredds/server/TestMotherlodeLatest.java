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

import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.fmrc.FmrcDefinition;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDataset;
import ucar.ma2.Section;

import java.util.*;
import java.io.IOException;

public class TestMotherlodeLatest extends TimerTask {
  static private final String server1 = "http://motherlode.ucar.edu:8080/";
  //static private final String server1 = "http://thredds.cise-nsf.gov:8080/";
  static private final String server2 = "http://motherlode.ucar.edu:9080/";

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
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  void doAll() throws IOException {

    for (String model : FmrcDefinition.fmrcDatasets) {
      doOne(model, suffix);
    }

  }

  void doOne(String model, String suffix) throws IOException {

    GridDataset gds1 = getDataset(makeDatasetURL( server1, model, suffix));
    GridDataset gds2 = getDataset(makeDatasetURL( server2, model, suffix));
    System.out.printf(" compare 1 to 2%n");
    compare(gds1, gds2);
    System.out.printf(" compare 2 to 1%n");
    compare(gds2, gds1);
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

  private GridDataset getDataset(String ds) throws IOException {

    Formatter errlog = new Formatter();
    FeatureDataset result = FeatureDatasetFactoryManager.open(FeatureType.GRID, ds, null, errlog);
    System.out.printf(" %s result errlog= %s%n", ds, errlog);
    assert result != null;
    assert result instanceof GridDataset;

    GridDataset dataset = (GridDataset) result;
    System.out.printf(" dataset=%s%n", dataset.getLocationURI());

    return dataset;
  }



  private void compare(GridDataset gds1, GridDataset gds2) {

    for (GridDatatype grid1 : gds1.getGrids()) {

      try {
        GridDatatype grid2 = gds2.findGridDatatype(grid1.getName());
        assert grid2 != null : "cant find " + grid1.getName();
        long size1 = new Section(grid1.getShape()).computeSize();
        long size2 = new Section(grid2.getShape()).computeSize();
        if (size1 != size2) {
          System.out.printf("%s size mismatch: %s != %s%n", grid1.getName(), show(grid1), show(grid2));
          throw new RuntimeException();
        }
      } catch (Throwable e) {
        // e.printStackTrace();
        System.out.printf(" *** %s %n", e.getMessage());
      }

    }


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
      map.put(time.getName(), time);
    }

    for (CoordinateAxis1DTime time : map.values()) {
      System.out.printf(" %s len = %d%n", time.getName(), time.getSize());
    }

    return map;
  }


  private void compare(Map<String, CoordinateAxis1DTime> map1,
          Map<String, CoordinateAxis1DTime> map2) throws Exception {

    for (CoordinateAxis1DTime time1 : map1.values()) {
      CoordinateAxis1DTime time2 = map2.get(time1.getName());
      assert time2 != null;
      assert time1.getSize() == time2.getSize();
    }

  }


  public static void main(String args[]) throws Exception {
    Timer timer = new Timer();

    // http://motherlode.ucar.edu:9080/thredds/catalog/model/NCEP/RUC2/CONUS_20km/pressure/latest.xml

    TestMotherlodeLatest test1 = new TestMotherlodeLatest("NCEP/RUC2/CONUS_20km/pressure", bestSuffix);
    timer.schedule(test1, 0, 1000 * 60 * 2); // 2 mins   */

    TestMotherlodeLatest test2 = new TestMotherlodeLatest("NCEP/RUC2/CONUS_20km/pressure", latestSuffix);
    timer.schedule(test2, 0, 1000 * 60 * 2); // 2 mins  */

    TestMotherlodeLatest test3 = new TestMotherlodeLatest("NCEP/NAM/CONUS_12km", latestSuffix);
    timer.schedule(test3, 0, 1000 * 60 * 10); // 10 mins  */

   TestMotherlodeLatest test4 = new TestMotherlodeLatest("NCEP/NAM/CONUS_12km", bestSuffix);
    timer.schedule(test4, 0, 1000 * 60 * 10); // 10 mins  */

    test2.doAll();
  }

}
