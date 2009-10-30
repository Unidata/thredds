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

package ucar.nc2.ft.point.standard;

import ucar.nc2.units.DateUnit;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.VariableSimpleIF;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;

/**
 * Standard handler for Point obs dataset based on a NetcdfDataset object.
 * Registered with FeatureDatasetFactoryManager.
 * The convention-specific stuff is handled by TableAnayser.
 *
 * @author caron
 */
public class PointDatasetStandardFactory implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetStandardFactory.class);
  static boolean showTables = false;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlags) {
    showTables = debugFlags.isSet("PointDatasetStandardFactory/showTables");
  }

  /**
   * Check if this is a POINT datatype. If so, a TableAnalyser is used to analyze its structure.
   * The TableAnalyser is reused when the dataset is opened.
   * <ol>
   * <li> Can handle ANY_POINT FeatureType.
   * <li> Must have time, lat, lon axis (from CoordSysBuilder)
   * <li> Call TableAnalyzer.factory() to create a TableAnalyzer
   * <li> TableAnalyzer must agree it can handle the requested FeatureType
   * </ol>
   *
   * @param wantFeatureType desired feature type, null means FeatureType.ANY_POINT
   * @param ds              analyse this dataset
   * @param errlog          log error messages here (may not be null)
   * @return if successful, return non-null. This object is then passed back into open(), so analysis can be reused.
   * @throws IOException
   */
  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    if (wantFeatureType == null) wantFeatureType = FeatureType.ANY_POINT;
    if (wantFeatureType != FeatureType.ANY_POINT) {
      if (!wantFeatureType.isPointFeatureType())
        return null;
    }

    TableConfigurer tc = TableAnalyzer.getTableConfigurer(wantFeatureType, ds);

    // if no explicit tc, then check whatever we can before expensive analysis)
    if (tc == null) {
      boolean hasTime = false;
      boolean hasLat = false;
      boolean hasLon = false;
      for (CoordinateAxis axis : ds.getCoordinateAxes()) {
        if (axis.getAxisType() == AxisType.Time) //&& (axis.getRank() == 1))
          hasTime = true;
        if (axis.getAxisType() == AxisType.Lat) //&& (axis.getRank() == 1))
          hasLat = true;
        if (axis.getAxisType() == AxisType.Lon) //&& (axis.getRank() == 1))
          hasLon = true;
      }

      // minimum we need
      if (!(hasTime && hasLon && hasLat)) {
        errlog.format("PointDataset must have lat,lon,time");
        return null;
      }
    } else if (showTables) {
      System.out.printf("TableConfigurer = %s%n", tc.getClass().getName());
    }

    // gotta do some work
    TableAnalyzer analyser = TableAnalyzer.factory(tc, wantFeatureType, ds);
    if (analyser == null)
      return null;

    errlog.format("%s%n", analyser.getErrlog());
    if (!analyser.featureTypeOk(wantFeatureType, errlog)) {
      return null;
    }

    return analyser;
  }

  public FeatureDataset open(FeatureType wantFeatureType, NetcdfDataset ncd, Object analyser, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    if (analyser == null)
      analyser = TableAnalyzer.factory(null, wantFeatureType, ncd);

    return new PointDatasetStandard(wantFeatureType, (TableAnalyzer) analyser, ncd, errlog);
  }

  public FeatureType[] getFeatureType() {
    return new FeatureType[]{FeatureType.ANY_POINT};
  }

  /////////////////////////////////////////////////////////////////////

  static class PointDatasetStandard extends PointDatasetImpl {
    private TableAnalyzer analyser;
    private DateUnit timeUnit;

    PointDatasetStandard(FeatureType wantFeatureType, TableAnalyzer analyser, NetcdfDataset ds, Formatter errlog) throws IOException {
      super(ds, null);
      parseInfo.format(" PointFeatureDatasetImpl=%s\n", getClass().getName());
      this.analyser = analyser;

      List<FeatureCollection> featureCollections = new ArrayList<FeatureCollection>();
      for (NestedTable flatTable : analyser.getFlatTables()) { // each flat table becomes a "feature collection"

        if (timeUnit == null) {
          try {
            timeUnit = flatTable.getTimeUnit();
          } catch (Exception e) {
            if (null != errlog) errlog.format("%s\n", e.getMessage());
            try {
              timeUnit = new DateUnit("seconds since 1970-01-01");
            } catch (Exception e1) {
              log.error("Illegal time units", e1); // cant happen i hope
            }
          }
        }

        // create member variables
        dataVariables = new ArrayList<VariableSimpleIF>(flatTable.getDataVariables());

        featureType = flatTable.getFeatureType(); // hope they're all the same
        if (flatTable.getFeatureType() == FeatureType.POINT)
          featureCollections.add(new StandardPointCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.PROFILE)
          featureCollections.add(new StandardProfileCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.STATION)
          featureCollections.add(new StandardStationCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.STATION_PROFILE)
          featureCollections.add(new StandardStationProfileCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.SECTION)
          featureCollections.add(new StandardSectionCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.TRAJECTORY)
          featureCollections.add(new StandardTrajectoryCollectionImpl(flatTable, timeUnit));
      }

      if (featureCollections.size() == 0)
        throw new IllegalStateException("No feature collections found");

      setPointFeatureCollection(featureCollections);
    }

    @Override
    public void getDetailInfo(java.util.Formatter sf) {
      super.getDetailInfo(sf);
      analyser.getDetailInfo(sf);
    }

    @Override
    public FeatureType getFeatureType() {
      return featureType;
    }

    @Override
    public String getImplementationName() {
      if (analyser != null)
        return analyser.getImplementationName();
      return super.getImplementationName();
    }

    TableAnalyzer getTableAnalyzer() { return analyser; } 
  }

  static void doit(PointDatasetStandardFactory fac, String filename) throws IOException {
    System.out.println(filename);
    Formatter errlog = new Formatter(System.out);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    TableAnalyzer analysis = (TableAnalyzer) fac.isMine(FeatureType.ANY_POINT, ncd, errlog);

    fac.open(FeatureType.ANY_POINT, ncd, analysis, null, errlog);
    analysis.getDetailInfo(errlog);
    System.out.printf("\n-----------------");
    ncd.close();
  }


  public static void main(String[] args) throws IOException {
    PointDatasetStandardFactory fac = new PointDatasetStandardFactory();
    doit(fac, "Q:/cdmUnitTest/formats/gempak/surface/20090521_sao.gem");
    // doit(fac, "D:/datasets/metars/Surface_METAR_20070513_0000.nc");
  }

}
