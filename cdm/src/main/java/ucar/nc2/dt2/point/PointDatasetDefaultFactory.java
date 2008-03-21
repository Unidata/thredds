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

package ucar.nc2.dt2.point;

import ucar.nc2.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateRange;
import ucar.nc2.dt2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.DataType;
import ucar.nc2.constants.AxisType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Date;
import java.util.List;
import java.io.IOException;
import java.text.ParseException;

/**
 * Default handler for PointFeatureDataset
 *
 * @author caron
 */
public class PointDatasetDefaultFactory implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetDefaultFactory.class);

  // FeatureDatasetFactory
  public boolean isMine(NetcdfDataset ds) {

    boolean hasTime = false;
    boolean hasLat = false;
    boolean hasLon = false;
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if ((axis.getAxisType() == AxisType.Time) && (axis.getRank() == 1))
        hasTime = true;
      if ((axis.getAxisType() == AxisType.Lat) && (axis.getRank() == 1))
        hasLat = true;
      if ((axis.getAxisType() == AxisType.Lon) && (axis.getRank() == 1))
        hasLon = true;
    }

    return hasTime && hasLon && hasLat;
  }

  public FeatureDataset open(NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new PointDatasetDefault(ncd, errlog);
  }

  public DataType getFeatureDataType() {
    return DataType.POINT;
  }

  /////////////////////////////////////////////////////////////////////
  private static class PointDatasetDefault extends PointFeatureDatasetImpl {

    private String timeVName, latVName, lonVName, altVName;
    private DateUnit timeUnit;
    private Structure obsStruct;
    private DateFormatter formatter;
    private boolean needBB = true;

    PointDatasetDefault(NetcdfDataset ds, StringBuffer errlog) throws IOException {
      super(ds, PointFeature.class);
      parseInfo.append(" PointFeatureDatasetImpl=").append(getClass().getName()).append("\n");

      CoordinateAxis time = null;
      for (CoordinateAxis axis : ds.getCoordinateAxes()) {
        if ((axis.getAxisType() == AxisType.Time) && (axis.getRank() == 1)) {
          timeVName = axis.getName();
          time = axis;
          try {
            timeUnit = new DateUnit(axis.getUnitsString());
          } catch (Exception e) {
            if (null != errlog)
              errlog.append("Error on string = ").append(axis.getUnitsString()).append(" == ").append(e.getMessage()).append("\n");
            timeUnit = (DateUnit) SimpleUnit.factory("seconds since 1970-01-01");
          }
        }
        if ((axis.getAxisType() == AxisType.Lat) && (axis.getRank() == 1))
          latVName = axis.getName();
        if ((axis.getAxisType() == AxisType.Lon) && (axis.getRank() == 1))
          lonVName = axis.getName();
        if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) && (axis.getRank() == 1))
          altVName = axis.getName();
      }

      Dimension timeDim = time.getDimension(0);
      if (timeDim.isUnlimited()) {
        ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
        obsStruct = (Structure) ds.getRootGroup().findVariable("record");
        timeDim = obsStruct.getDimension(0);
      } else {
        obsStruct = new StructurePseudo(ds, null, "obsStruct", timeDim);
      }

      // create member variables
      List<Variable> recordMembers = ds.getVariables();
      for (Variable v : recordMembers) {
        if (v == obsStruct) continue;
        if (v instanceof CoordinateAxis) continue;
        if (v.isScalar()) continue;
        if (v.getDimension(0) == timeDim)
          dataVariables.add(v);
      }

      // the collection is defined by the Iterator
      setPointFeatureCollection(new PointFeatureCollectionImpl() {
        public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
          return new DefaultPointFeatureIterator(obsStruct);
        }
      });

    }

    // calculate bounding box, date range when all the data is iterated
    private LatLonRect calcBB = null;
    private double minTime = Double.MAX_VALUE;
    private double maxTime = -Double.MAX_VALUE;

    private void startCalcBB() {
      calcBB = null;
    }

    private void calcBB(PointFeature pf) {
      if (calcBB == null)
        calcBB = new LatLonRect(pf.getLocation().getLatLon(), .001, .001);
      else
        calcBB.extend(pf.getLocation().getLatLon());

      double obsTime = pf.getObservationTime();
      minTime = Math.min(minTime, obsTime);
      maxTime = Math.max(maxTime, obsTime);
    }

    private void finishCalcBB() {
      if (calcBB.crossDateline() && calcBB.getWidth() > 350.0) { // call it global - less confusing
        double lat_min = calcBB.getLowerLeftPoint().getLatitude();
        double deltaLat = calcBB.getUpperLeftPoint().getLatitude() - lat_min;
        boundingBox = new LatLonRect(new LatLonPointImpl(lat_min, -180.0), deltaLat, 360.0);
      } else {
        boundingBox = calcBB;
      }

      dateRange = new DateRange(timeUnit.makeDate(minTime), timeUnit.makeDate(maxTime));
      needBB = false;
    }

    // the iterator over the observations
    private class DefaultPointFeatureIterator extends StructureDataIterator {
      DefaultPointFeatureIterator(Structure struct) throws IOException {
        super(struct, -1, null);
        if (needBB) startCalcBB();
      }

      protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
        PointFeature result = new MyPointFeature(recnum, sdata);

        // try to calculate bounding box, date range when data is fully iterated
        if (needBB) calcBB(result);

        return result;
      }

      // decorate hasNext to know when the iteraton is complete
      public boolean hasNext() throws IOException {
        boolean r = super.hasNext();
        if (needBB && !r) finishCalcBB();
        return r;
      }
    }

    // the PointFeature for the observation
    private class MyPointFeature extends PointFeatureImpl {
      protected int recno;
      protected StructureData sdata;

      public MyPointFeature(int recnum, StructureData sdata) {
        super(PointDatasetDefault.this.timeUnit);
        this.sdata = sdata;
        this.recno = recnum;

        StructureMembers members = sdata.getStructureMembers();
        obsTime = getTime(members.findMember(timeVName), sdata);
        nomTime = obsTime;

        // this assumes the lat/lon/alt is stored in the obs record
        double lat = sdata.convertScalarDouble(members.findMember(latVName));
        double lon = sdata.convertScalarDouble(members.findMember(lonVName));
        double alt = (altVName == null) ? 0.0 : sdata.convertScalarDouble(members.findMember(altVName));
        location = new EarthLocationImpl(lat, lon, alt);
      }

      public String getId() {
        return Integer.toString(recno);
      }

      public StructureData getData() {
        return sdata;
      }

      private double getTime(StructureMembers.Member timeVar, StructureData sdata) {
        if (timeVar == null) return 0.0;

        if ((timeVar.getDataType() == ucar.ma2.DataType.CHAR) || (timeVar.getDataType() == ucar.ma2.DataType.STRING)) {
          String time = sdata.getScalarString(timeVar);
          if (null == formatter) formatter = new DateFormatter();
          Date date;
          try {
            date = formatter.isoDateTimeFormat(time);
          } catch (ParseException e) {
            log.error("Cant parse date - not ISO formatted, = " + time);
            return 0.0;
          }
          return date.getTime() / 1000.0;

        } else {
          return sdata.convertScalarDouble(timeVar);
        }
      }

    }
  }

}
