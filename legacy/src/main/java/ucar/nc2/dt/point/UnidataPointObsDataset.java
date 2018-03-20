/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id:UnidataPointObsDataset.java 51 2006-07-12 17:13:13Z caron $


package ucar.nc2.dt.point;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.dt.DatatypeIterator;
import ucar.nc2.dt.DataIterator;
import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;
import java.io.IOException;

import ucar.nc2.constants.FeatureType;

/**
 * This handles point datasets in "Unidata Observation Dataset v1.0"
 *
 * @deprecated use ucar.nc2.ft.point
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public class UnidataPointObsDataset extends PointObsDatasetImpl implements TypedDatasetFactoryIF {

  static public boolean isValidFile(NetcdfFile ds) {
    if ( !ds.findAttValueIgnoreCase(null, "cdm_data_type", "").equalsIgnoreCase(FeatureType.POINT.toString()) &&
            !ds.findAttValueIgnoreCase(null, "cdm_datatype", "").equalsIgnoreCase(FeatureType.POINT.toString()))
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
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    return new UnidataPointObsDataset( ncd);
  }
  
  public UnidataPointObsDataset() {}


  private Variable latVar, lonVar, altVar, timeVar, timeNominalVar;
  // private Structure recordVar;
  private RecordDatasetHelper recordHelper;
  private ArrayList allData;

  public UnidataPointObsDataset(NetcdfDataset ds) throws IOException {
    super(ds);

    // coordinate variables
    latVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lat);
    lonVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Lon);
    timeVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Time);

    if (latVar == null)
      throw new IllegalStateException("Missing latitude variable");
    if (lonVar == null)
      throw new IllegalStateException("Missing longitude coordinate variable");
    if  (timeVar == null)
      throw new IllegalStateException("Missing time coordinate variable");

    altVar = UnidataObsDatasetHelper.getCoordinate(ds, AxisType.Height);
    timeNominalVar = UnidataObsDatasetHelper.findVariable( ds, "record.time_nominal");
    String recDimName = ds.findAttValueIgnoreCase(null, "observationDimension", null);

    // fire up the record helper
    recordHelper = new RecordDatasetHelper(ds, timeVar.getShortName(), timeNominalVar == null ? null : timeNominalVar.getShortName(),
        dataVariables, recDimName, parseInfo);
    recordHelper.setLocationInfo(latVar.getShortName(), lonVar.getShortName(), altVar == null ? null : altVar.getShortName());
    recordHelper.setShortNames(latVar.getShortName(), lonVar.getShortName(), altVar == null ? null : altVar.getShortName(), 
            timeVar.getShortName(), timeNominalVar == null ? null : timeNominalVar.getShortName());
    allData = recordHelper.readAllCreateObs( null);

    removeDataVariable(timeVar.getShortName());
    if (timeNominalVar != null)
      removeDataVariable(timeNominalVar.getShortName());
    removeDataVariable(latVar.getShortName());
    removeDataVariable(lonVar.getShortName());
    if (altVar != null)
      removeDataVariable(altVar.getShortName());

    //recordVar = recordHelper.recordVar;
    timeUnit = recordHelper.timeUnit;

    // we are reading through all records anyway, to get the lat/lon locations!
    try {
      startDate = UnidataObsDatasetHelper.getStartDate( ds);
      endDate = UnidataObsDatasetHelper.getEndDate( ds);
    } catch (IllegalArgumentException e) {
      parseInfo.append("Missing time_coverage_start or end attributes");
    }

    try {
      boundingBox = UnidataObsDatasetHelper.getBoundingBox( ds);
    } catch (IllegalArgumentException e) {
      parseInfo.append("Missing geospatial_lat(lon)_min(max) attributes");
    }

    setTimeUnits();
    //setStartDate();
    //setEndDate();
    //setBoundingBox();


    title = ds.findAttValueIgnoreCase(null, "title", null);
    desc = ds.findAttValueIgnoreCase(null, "description", null);
  }

  protected void setTimeUnits() { timeUnit = recordHelper.timeUnit;}
  protected void setStartDate() { startDate = timeUnit.makeDate( recordHelper.minDate);}
  protected void setEndDate() { endDate = timeUnit.makeDate( recordHelper.maxDate);}
  protected void setBoundingBox() { boundingBox = recordHelper.boundingBox;}

  public List getData(CancelTask cancel) throws IOException {
    return allData;
  }

  public int getDataCount() {
    return (int) recordHelper.getRecordVar().getSize();
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return recordHelper.getData(allData, boundingBox, cancel);
  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    double startTime = timeUnit.makeValue( start);
    double endTime = timeUnit.makeValue( end);
    return recordHelper.getData( allData, boundingBox, startTime, endTime, cancel);
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new PointDatatypeIterator(recordHelper.recordVar, bufferSize);
  }

//  public DataIterator getDataIterator( ucar.unidata.geoloc.LatLonRect boundingBox, int bufferSize) throws IOException;

//  public DataIterator getDataIterator( ucar.unidata.geoloc.LatLonRect boundingBox, Date start, Date end, int bufferSize) throws IOException;


  private class PointDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return recordHelper.new RecordPointObs( recnum, sdata);
    }

    PointDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  }

}

