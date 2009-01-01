/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.ft.point.standard;

import ucar.nc2.units.DateUnit;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;

/**
 * Standard handler for any Point obs dataset.
 * Registered with FeatureDatasetFactoryManager.
 *
 * @author caron
 */
public class PointDatasetStandardFactory implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetStandardFactory.class);

  private TableAnalyzer analyser;

  /**
   * Check if this is a POINT datatype. If so, a TableAnalyser is used to analyze its structure.
   * The TableAnalyser is reused when the dataset is opened.
   * <ol>
   * <li> Can handle ANY_POINT FeatureType.
   * <li> Must have time, lat, lon axis (from CoordSysBuilder)
   * <li> Call TableAnalyzer.factory() to create a TableAnalyzer
   * <li> TableAnalyzer must agree it can handle the requested FeatureType
   * </ol>
   */
  public boolean isMine(FeatureType ftype, NetcdfDataset ds) throws IOException {

    if ((ftype != null) && (ftype != FeatureType.ANY_POINT)) {
      if ((ftype != FeatureType.POINT) && (ftype != FeatureType.STATION) && (ftype != FeatureType.TRAJECTORY) &&
          (ftype != FeatureType.PROFILE) && (ftype != FeatureType.STATION_PROFILE) && (ftype != FeatureType.SECTION))
      return false;
    }

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
    if (!(hasTime && hasLon && hasLat))
      return false;

    // gotta do some work
    analyser = TableAnalyzer.factory(ftype, ds);
    return (analyser != null) && analyser.featureTypeOk( ftype);
  }

  public FeatureDatasetFactory copy() {
    PointDatasetStandardFactory copy = new PointDatasetStandardFactory();
    copy.analyser = this.analyser;
    return copy;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    if (analyser == null)
      analyser = TableAnalyzer.factory(ftype, ncd);

    return new PointDatasetDefault(ftype, analyser, ncd, errlog);
  }

  /////////////////////////////////////////////////////////////////////

  private static class PointDatasetDefault extends PointDatasetImpl {
    private TableAnalyzer analyser;
    private DateUnit timeUnit;
    private FeatureType featureType = FeatureType.POINT; // default

    PointDatasetDefault(FeatureType ftype, TableAnalyzer analyser, NetcdfDataset ds, Formatter errlog) throws IOException {
      super(ds, null);
      parseInfo.format(" PointFeatureDatasetImpl=%s\n", getClass().getName());
      this.analyser = analyser;

      List<FeatureCollection> featureCollections = new ArrayList<FeatureCollection>();
      for (FlattenedTable flatTable : analyser.getFlatTables()) { // each flat table becomes a "feature collection"

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
        dataVariables.addAll(flatTable.getDataVariables());

        featureType = flatTable.getFeatureType(); // hope they're all the same
        if (flatTable.getFeatureType() == FeatureType.POINT)
          featureCollections.add(new StandardPointCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.STATION)
          featureCollections.add(new StandardStationCollectionImpl(timeUnit, flatTable));

        else if (flatTable.getFeatureType() == FeatureType.STATION_PROFILE)
          featureCollections.add(new StandardStationProfileCollectionImpl(flatTable, timeUnit));

        else if (flatTable.getFeatureType() == FeatureType.TRAJECTORY)
          featureCollections.add(new StandardTrajectoryCollectionImpl(flatTable, timeUnit));
      }

      if (featureCollections.size() == 0)
        throw new IllegalStateException("No feature collections found");

      setPointFeatureCollection(featureCollections);
    }

    @Override
    public void getDetailInfo( java.util.Formatter sf) {
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
        return analyser.getImplementationName()  + "/" +getClass().getSimpleName();
      return super.getImplementationName();
    }
  }
}
