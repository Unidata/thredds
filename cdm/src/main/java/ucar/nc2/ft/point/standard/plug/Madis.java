/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Dimension;
import ucar.nc2.constants.FeatureType;

import java.util.Formatter;

/**
 * Madis Convention
 * @author caron
 * @since Apr 23, 2008
 */
public class Madis extends TableConfigurerImpl  {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if ((wantFeatureType != FeatureType.ANY_POINT) && (wantFeatureType != FeatureType.STATION) && (wantFeatureType != FeatureType.POINT)
            && (wantFeatureType != FeatureType.STATION_PROFILE))
      return false;

    if (!ds.hasUnlimitedDimension()) return false;
    if (ds.findDimension("recNum") == null) return false;

    if (ds.findVariable("staticIds") == null) return false;
    if (ds.findVariable("nStaticIds") == null) return false;
    if (ds.findVariable("lastRecord") == null) return false;
    if (ds.findVariable("prevRecord") == null) return false;

    VNames vn = getVariableNames(ds, null);    
    if (ds.findVariable(vn.lat) == null) return false;
    if (ds.findVariable(vn.lon) == null) return false;
    if (ds.findVariable(vn.obsTime) == null) return false;

    return true;
  }

  /*
  <!-- C:/data/dt2/station/madis2.sao -->
  <stationCollection>
    <table dim="maxStaticIds" limit="nStaticIds">
      <lastLink>lastRecord</lastLink>

      <table dim="recNum">
        <stationId>:stationIdVariable</stationId>
        <stationWmoId>wmoId</stationWmoId>
        <coordAxis type="time">timeObs</coordAxis>
        <coordAxis type="lat">latitude</coordAxis>
        <coordAxis type="lon">longitude</coordAxis>
        <coordAxis type="height">elevation</coordAxis>
        <prevLink>prevRecord</prevLink>
      </table>

    </table>

    <cdmDataType>:thredds_data_type</cdmDataType>
  </stationCollection>
   */

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {

    Dimension obsDim = Evaluator.getDimension(ds, "recNum", errlog);
    if (obsDim == null) {
      errlog.format("MADIS: must have an Observation dimension: named recNum");
      return null;
    }
    VNames vn = getVariableNames(ds, errlog);

    String levVarName = null;
    String levDimName = null;
    boolean hasStruct = Evaluator.hasNetcdf3RecordStructure(ds);
    FeatureType ft = Evaluator.getFeatureType(ds, ":thredds_data_type", errlog);
    if (null == ft) {
      if ((ds.findDimension("manLevel") != null) && (ds.findVariable("prMan") != null)) {
        ft = FeatureType.STATION_PROFILE;
        levVarName = "prMan";
        levDimName = "manLevel";
      } else if ((ds.findDimension("level") != null) && (ds.findVariable("levels") != null)) {
        ft = FeatureType.STATION_PROFILE;
        levVarName = "levels";
        levDimName = "level";
      }
    }
    if (null == ft) ft = FeatureType.POINT;

    // points
    if ((wantFeatureType == FeatureType.POINT) || (ft == FeatureType.POINT)) {
      TableConfig ptTable = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName() );
      ptTable.structName = "record";
      ptTable.featureType = FeatureType.POINT;
      ptTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;      

      ptTable.dimName = obsDim.getShortName();
      ptTable.time = vn.obsTime;
      ptTable.timeNominal = vn.nominalTime;
      ptTable.lat = vn.lat;
      ptTable.lon = vn.lon;
      ptTable.elev = vn.elev;

      return ptTable;
     }

    if (ft == FeatureType.STATION) {
      TableConfig stnTable = new TableConfig(Table.Type.Construct, "station");
      stnTable.featureType = FeatureType.STATION;

      TableConfig obs = new TableConfig(Table.Type.ParentId, "record");
      obs.parentIndex = vn.stnId;
      obs.dimName = Evaluator.getDimensionName(ds, "recNum", errlog);
      obs.time = vn.obsTime;
      obs.timeNominal = vn.nominalTime;

      obs.stnId = vn.stnId;
      obs.stnDesc = vn.stnDesc;
      obs.lat = vn.lat;
      obs.lon = vn.lon;
      obs.elev = vn.elev;

      stnTable.addChild(obs);
      return stnTable;
    }

    else if (ft == FeatureType.STATION_PROFILE) {
      TableConfig stnTable = new TableConfig(Table.Type.Construct, "station");
      stnTable.featureType = FeatureType.STATION_PROFILE;

      TableConfig obs = new TableConfig(Table.Type.ParentId, "record");
      obs.parentIndex = vn.stnId;
      obs.dimName = Evaluator.getDimensionName(ds, "recNum", errlog);
      obs.time = vn.obsTime;
      obs.timeNominal = vn.nominalTime;

      obs.stnId = vn.stnId;
      obs.stnDesc = vn.stnDesc;
      obs.lat = vn.lat;
      obs.lon = vn.lon;
      obs.stnAlt = vn.elev;
  
      stnTable.addChild(obs);

      TableConfig lev = new TableConfig(Table.Type.MultidimInner, "mandatory");
      lev.elev = levVarName;
      lev.outerName = obs.dimName;
      lev.innerName = levDimName;
      obs.addChild(lev);

      return stnTable;
    }

    return null;
  }

  protected static class VNames {
    String lat, lon, elev, obsTime, nominalTime;
    String stnId, stnDesc;
  }

  protected VNames getVariableNames(NetcdfDataset ds, Formatter errlog) {
    VNames vn = new VNames();

    String val = ds.findAttValueIgnoreCase(null, "stationLocationVariables", null);
    if (val == null)
      val = ds.findAttValueIgnoreCase(null, "latLonVars", null);
    if (val == null) {
      if (errlog != null) errlog.format(" Cant find global attribute stationLocationVariables%n");
      vn.lat = "latitude";
      vn.lon = "longitude";
    } else {
      String[] vals = val.split(",");
      if (vals.length > 0) vn.lat = vals[0];
      if (vals.length > 1) vn.lon = vals[1];
      if (vals.length > 2) vn.elev = vals[2];
    }

    val = ds.findAttValueIgnoreCase(null, "timeVariables", null);
    if (val == null) {
      if (errlog != null) errlog.format(" Cant find global attribute timeVariables%n");
      vn.obsTime = "observationTime";
      vn.nominalTime = "reportTime";
    } else {
      String[] vals = val.split(",");
      if (vals.length > 0) vn.obsTime = vals[0];
      if (vals.length > 1) vn.nominalTime = vals[1];
    }

    val = ds.findAttValueIgnoreCase(null, "stationDescriptionVariable", null);
    if (val == null) {
      if (errlog != null) errlog.format(" Cant find global attribute stationDescriptionVariable%n");
      vn.stnDesc = "stationName";
    } else {
      vn.stnDesc = val;
    }

    val = ds.findAttValueIgnoreCase(null, "stationIdVariable", null);
    if (val == null)
      val = ds.findAttValueIgnoreCase(null, "idVariables", null);
    if (val == null) {
      if (errlog != null) errlog.format(" Cant find global attribute stationIdVariable%n");
      vn.stnId = "stationId";
    } else {
      vn.stnId = val;
    }

    if (null != ds.findVariable("altitude"))
      vn.elev = "altitude";
    else if (null != ds.findVariable("elevation"))
      vn.elev = "elevation";

    return vn;
  }

}
