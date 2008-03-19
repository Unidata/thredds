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
import ucar.nc2.dt2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.DataType;
import ucar.nc2.constants.AxisType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import java.util.Date;
import java.io.IOException;
import java.text.ParseException;

/**
 * Default handler for PointFeatureDataset
 *
 * @author caron
 */
public class PointDatasetDefaultHandler implements FeatureDatasetFactory {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointDatasetDefaultHandler.class);

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

  public FeatureDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    featureDataset = new PointFeatureDatasetImpl( ncd, PointFeature.class);
    init(ncd, errlog);
    return featureDataset;
  }

  public DataType getFeatureDataType() { return DataType.POINT; }
  

  /////////////////////////////////////////////////////////////////////
  private PointFeatureDatasetImpl featureDataset;

  private String timeVName, latVName, lonVName, altVName;
  private DateUnit timeUnit;

  public PointDatasetDefaultHandler() {}


  private void init(NetcdfDataset ds, StringBuffer errlog) throws IOException {

    CoordinateAxis time = null;
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if ((axis.getAxisType() == AxisType.Time) && (axis.getRank() == 1)) {
        timeVName = axis.getName();
        time = axis;
        try {
          timeUnit = new DateUnit( axis.getUnitsString());
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

    Dimension dim = time.getDimension(0);
    StructurePseudo struct = new StructurePseudo(ds, null, "pstruct", dim);
    PointFeatureIterator siter = new DefaultPointFeatureIterator(struct);
    featureDataset.setPointFeatureCollection(new CollectionOfPointFeatures( featureDataset.getDataVariables(), siter));

    // LOOK: could set the date by reading first and last.
    // LOOK: could get Bounding Box when all data is read
  }

  private DateFormatter formatter;
  private double getTime(StructureMembers.Member timeVar, StructureData sdata) {
    if (timeVar == null) return 0.0;

    if ((timeVar.getDataType() == ucar.ma2.DataType.CHAR) || (timeVar.getDataType() == ucar.ma2.DataType.STRING)) {
      String time = sdata.getScalarString(timeVar);
      if (null == formatter) formatter = new DateFormatter();
      Date date;
      try {
        date = formatter.isoDateTimeFormat(time);
      } catch (ParseException e) {
        log.error("Cant parse date - not ISO formatted, = "+time);
        return 0.0;
      }
      return date.getTime() / 1000.0;

    } else {
      return sdata.convertScalarDouble(timeVar);
    }
  }

  private class DefaultPointFeatureIterator extends StructureDataIterator {
    DefaultPointFeatureIterator(Structure struct) throws IOException {
      super(struct, -1, null);
    }

    protected PointFeature makeFeature(int recnum, StructureData sdata) throws IOException {
      return new MyPointFeature(sdata);
    }
  }

  // LOOK should be merged with RecordDatasetHelper
  private class MyPointFeature extends PointFeatureImpl {
    protected int recno;
    protected StructureData sdata;

    public MyPointFeature(StructureData sdata) {
      super( PointDatasetDefaultHandler.this.timeUnit);
      this.sdata = sdata;
      this.recno = recno;

      StructureMembers members = sdata.getStructureMembers();
      obsTime = getTime(members.findMember(timeVName), sdata);
      nomTime = obsTime;

      // this assumes the lat/lon/alt is stored in the obs record
      double lat = sdata.convertScalarDouble( members.findMember(latVName));
      double lon = sdata.convertScalarDouble( members.findMember(lonVName));
      double alt = (altVName == null) ? 0.0 : sdata.convertScalarDouble(members.findMember(altVName));
      location = new EarthLocationImpl(lat, lon, alt);
    }

    public String getId() {
      return Integer.toString(recno);
    }

    public StructureData getData() {
      return sdata;
    }
  }

}
