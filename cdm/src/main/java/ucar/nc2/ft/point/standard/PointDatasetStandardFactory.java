/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Standard handler for Point obs dataset based on a NetcdfDataset object.
 * Registered with FeatureDatasetFactoryManager.
 * The convention-specific stuff is handled by TableAnayser.
 *
 * @author caron
 */
public class PointDatasetStandardFactory implements FeatureDatasetFactory {
  // static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetStandardFactory.class);
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
  @Override
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

    try {
      // gotta do some work
      TableAnalyzer analyser = TableAnalyzer.factory(tc, wantFeatureType, ds);
      if (analyser == null)
        return null;

      if (!analyser.featureTypeOk(wantFeatureType, errlog)) {
        return null;
      }
      return analyser;

    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public FeatureDataset open(FeatureType wantFeatureType, NetcdfDataset ncd, Object analyser, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    if (analyser == null)
      analyser = TableAnalyzer.factory(null, wantFeatureType, ncd);

    return new PointDatasetStandard(wantFeatureType, (TableAnalyzer) analyser, ncd, errlog);
  }

  @Override
  public FeatureType[] getFeatureTypes() {
    return new FeatureType[]{FeatureType.ANY_POINT};
  }

  /////////////////////////////////////////////////////////////////////

  static class PointDatasetStandard extends PointDatasetImpl {
    private TableAnalyzer analyser;
    //private DateUnit timeUnit;

    PointDatasetStandard(FeatureType wantFeatureType, TableAnalyzer analyser, NetcdfDataset ds, Formatter errlog) throws IOException {
      super(ds, null);
      parseInfo.format(" PointFeatureDatasetImpl=%s%n", getClass().getName());
      this.analyser = analyser;

      List<DsgFeatureCollection> featureCollections = new ArrayList<>();
      for (NestedTable flatTable : analyser.getFlatTables()) { // each flat table becomes a "feature collection"

        CalendarDateUnit timeUnit;
        try {
          timeUnit = flatTable.getTimeUnit();
        } catch (Exception e) {
          if (null != errlog) errlog.format("%s%n", e.getMessage());
          timeUnit = CalendarDateUnit.unixDateUnit;
        }

        String altUnits = flatTable.getAltUnits();

        // create member variables
        dataVariables = new ArrayList<>(flatTable.getDataVariables());

        featureType = flatTable.getFeatureType(); // hope they're all the same
        if (flatTable.getFeatureType() == FeatureType.POINT)
          featureCollections.add(new StandardPointCollectionImpl(flatTable, timeUnit, altUnits));

        else if (flatTable.getFeatureType() == FeatureType.PROFILE)
          featureCollections.add(new StandardProfileCollectionImpl(flatTable, timeUnit, altUnits));

        else if (flatTable.getFeatureType() == FeatureType.STATION)
          featureCollections.add(new StandardStationCollectionImpl(flatTable, timeUnit, altUnits));

        else if (flatTable.getFeatureType() == FeatureType.STATION_PROFILE)
          featureCollections.add(new StandardStationProfileCollectionImpl(flatTable, timeUnit, altUnits));

        else if (flatTable.getFeatureType() == FeatureType.TRAJECTORY_PROFILE)
          featureCollections.add(new StandardSectionCollectionImpl(flatTable, timeUnit, altUnits));

        else if (flatTable.getFeatureType() == FeatureType.TRAJECTORY)
          featureCollections.add(new StandardTrajectoryCollectionImpl(flatTable, timeUnit, altUnits));
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

}
