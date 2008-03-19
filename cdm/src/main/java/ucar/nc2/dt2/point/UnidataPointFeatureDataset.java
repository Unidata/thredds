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

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.units.DateRange;
import ucar.nc2.dt2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;
import java.io.IOException;

import ucar.nc2.constants.DataType;

/**
 * This handles point datasets in "Unidata Observation Dataset v1.0"
 *
 * @author caron
 * @since Feb 29, 2008
 */

public class UnidataPointFeatureDataset extends PointFeatureDatasetImpl {

  static public boolean isValidFile(NetcdfFile ds) {
    if ( !ds.findAttValueIgnoreCase(null, "cdm_data_type", "").equalsIgnoreCase(DataType.POINT.toString()) &&
            !ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase(DataType.POINT.toString()))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equalsIgnoreCase("Unidata Observation Dataset v1.0"))
        return true;
    }

    return false;
  }

  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public FeatureDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new UnidataPointFeatureDataset( ncd);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private RecordDatasetHelper recordHelper;
  private LatLonRect filter_bb;
  private DateRange filter_date;

  // copy constructor
  UnidataPointFeatureDataset(UnidataPointFeatureDataset from, LatLonRect filter_bb, DateRange filter_date) {
    super(from, filter_bb, filter_date);
    this.recordHelper = from.recordHelper;

    if (from.filter_bb == null)
      this.filter_bb = filter_bb;
    else
      this.filter_bb = (filter_bb == null) ? from.filter_bb : from.filter_bb.intersect( filter_bb);

    if (from.filter_date == null)
      this.filter_date = filter_date;
    else
      this.filter_date = (filter_date == null) ? from.filter_date : from.filter_date.intersect( filter_date);
  }

  public UnidataPointFeatureDataset(NetcdfDataset ds) throws IOException {
    super(ds, PointFeature.class);

    // coordinate variables
    Variable latVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Lat);
    Variable lonVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Lon);
    Variable timeVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Time);

    if (latVar == null)
      throw new IllegalStateException("Missing latitude variable");
    if (lonVar == null)
      throw new IllegalStateException("Missing longitude coordinate variable");
    if  (timeVar == null)
      throw new IllegalStateException("Missing time coordinate variable");

    Variable altVar = UnidataPointDatasetHelper.getCoordinate(ds, AxisType.Height);
    Variable timeNominalVar = UnidataPointDatasetHelper.findVariable(ds, "record.time_nominal");
    String recDimName = ds.findAttValueIgnoreCase(null, "observationDimension", null);

    // fire up the record helper
    recordHelper = new RecordDatasetHelper(ds, timeVar.getName(), timeNominalVar == null ? null : timeNominalVar.getName(),
        dataVariables, recDimName, parseInfo);
    recordHelper.setLocationInfo(latVar.getName(), lonVar.getName(), altVar == null ? null : altVar.getName());
    recordHelper.setShortNames(latVar.getShortName(), lonVar.getShortName(), altVar == null ? null : altVar.getShortName(), 
            timeVar.getShortName(), timeNominalVar == null ? null : timeNominalVar.getShortName());

    removeDataVariable(timeVar.getName());
    if (timeNominalVar != null)
      removeDataVariable(timeNominalVar.getName());
    removeDataVariable(latVar.getName());
    removeDataVariable(lonVar.getName());
    if (altVar != null)
      removeDataVariable(altVar.getName());

    Date startDate = UnidataPointDatasetHelper.getStartDate( ds, recordHelper.timeUnit);
    Date endDate = UnidataPointDatasetHelper.getEndDate( ds, recordHelper.timeUnit);
    setDateRange( new DateRange(startDate, endDate));
    setBoundingBox( UnidataPointDatasetHelper.getBoundingBox( ds));

    title = ds.findAttValueIgnoreCase(null, "title", null);
    desc = ds.findAttValueIgnoreCase(null, "description", null);

    ucar.nc2.dt2.PointFeatureIterator pfiter = new MyPointFeatureIterator(recordHelper.recordVar, -1, null);
    setPointFeatureCollection( new CollectionOfPointFeatures(dataVariables, pfiter));
  }

  /* public PointFeatureDataset subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    return new UnidataPointFeatureDataset( this, boundingBox, dateRange);
  }

  public FeatureIterator getFeatureIterator(int bufferSize) throws IOException {
    StructureDataIterator.Filter filter = null;
    
    if ((filter_bb != null) || (filter_date != null))

      filter = new StructureDataIterator.Filter() {
        public boolean filter(Feature feature) {
          PointFeature pf = (PointFeature) feature;
          if (filter_bb != null) {
            LatLonPoint pt = pf.getLocation().getLatLon();
            if (!filter_bb.contains(pt))
              return false;
          }

          if (filter_date != null) {
            Date d = pf.getObservationTimeAsDate();
            if (!filter_date.included(d))
              return false;
          }
          return true;
        }
      };

    return new PointFeatureIterator(recordHelper.recordVar, bufferSize, filter);
  }

  public DataCost getDataCost() {
    return new DataCost(recordHelper.getRecordCount(), -1);
  }   */

  private class MyPointFeatureIterator extends StructureDataIterator {

    protected PointFeature makeFeature(int recnum, StructureData sdata) {
      return recordHelper.new RecordPointObs( sdata, recnum);
    }

    MyPointFeatureIterator(Structure struct, int bufferSize, StructureDataIterator.Filter filter) throws IOException {
      super( struct, bufferSize, filter);
    }

  }

  public static void main(String args[]) throws IOException {
    //String filename = "C:/data/199707010200.CHRTOUT_DOMAIN2";
    String filename = "C:/data/metars/Surface_METAR_20070331_0000.nc";
    UnidataPointFeatureDataset upod = new UnidataPointFeatureDataset( NetcdfDataset.openDataset(filename));
    System.out.println("\n\n"+upod.getDetailInfo());
  }


}

