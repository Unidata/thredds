/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.ma2.DataType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.*;

import java.util.*;
import java.io.IOException;

/**
 * CDM direct, used when we are implementing the IOSP.
 *
 * @author caron
 * @since Dec 30, 2010
 */
public class CdmDirect extends TableConfigurerImpl {

  private static final String Convention = "CDM";

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    boolean ok = false;
    String conv = ds.findAttValueIgnoreCase(null, CDM.CONVENTIONS, null);
    if (conv == null) return false;
    if (conv.equals(Convention)) ok = true;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equals(Convention))
        ok = true;
    }
    if (!ok) return false;

    CF.FeatureType ftype = CF.FeatureType.getFeatureTypeFromGlobalAttribute(ds);
    if (ftype == null) ftype = CF.FeatureType.point;

    return (ftype == CF.FeatureType.timeSeries) || (ftype == CF.FeatureType.timeSeriesProfile);
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {

    CF.FeatureType ftype = CF.FeatureType.getFeatureTypeFromGlobalAttribute(ds);
    if (ftype == null) ftype = CF.FeatureType.point;

    switch (ftype) {
      case point:
        return null; // use default handler
      case timeSeries:
        /* if (wantFeatureType == FeatureType.POINT)
          return getStationAsPointConfig(ds, errlog);
        else  */
          return getStationConfig(ds, errlog);
      case timeSeriesProfile:
          return getStationProfileConfig(ds, errlog);
      default:
        throw new IllegalStateException("unimplemented feature ftype= " + ftype);
    }
  }

  protected TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("CdmDirect: Must have a Latitude coordinate%n");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("CdmDirect: Must have a Longitude coordinate%n");
      return null;
    }

    if (lat.getRank() != lon.getRank()) {
      errlog.format("CdmDirect: Lat and Lon coordinate must have same rank");
      return null;
    }

    // should be a top level struct or sequence
    TableConfig stnTable = new TableConfig(Table.Type.Structure, "station");
    stnTable.featureType = FeatureType.STATION;
    stnTable.structureType = TableConfig.StructureType.Structure;

    stnTable.lat= lat.getShortName();
    stnTable.lon= lon.getShortName();

    // optional alt coord
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (alt != null)
      stnTable.stnAlt = alt.getShortName();

    // station id
    stnTable.stnId = Evaluator.findNameOfVariableWithAttributeValue(ds, CF.CF_ROLE, CF.STATION_ID);
    if (stnTable.stnId == null)
      stnTable.stnId = Evaluator.findNameOfVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.STATION_ID); // old way
    if (stnTable.stnId == null) {
      errlog.format("Must have a Station id variable with standard name station_id%n");
      return null;
    }

    // other station
    stnTable.stnDesc = Evaluator.findNameOfVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.PLATFORM_NAME);
    if (stnTable.stnDesc == null)
      stnTable.stnDesc = Evaluator.findNameOfVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.STATION_DESC);
    stnTable.stnWmoId = Evaluator.findNameOfVariableWithAttributeValue(ds, CF.STANDARD_NAME, CF.STATION_WMOID);

    // obs table
    Structure stnv = (Structure) ds.findVariable("station");
    Structure obsv = null;
    for (Variable v : stnv.getVariables()) {
      if (v.getDataType() == DataType.SEQUENCE)
        obsv = (Structure) v;
    }
    if (obsv == null)  {
      errlog.format("Must have a SEQUENCE variable%n");
      return null;
    }
    TableConfig obs = new TableConfig(Table.Type.NestedStructure, obsv.getFullName());
    obs.nestedTableName = obsv.getShortName();
    obs.time = CoordSysEvaluator.findCoordShortNameByType(ds, AxisType.Time);
     if (obs.time == null) {
      errlog.format("Must have a time coordinate%n");
      return null;
    }
    stnTable.addChild(obs);

    return stnTable;
  }

  protected TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    TableConfig stnTable = getStationConfig(ds, errlog);
    if (stnTable == null) return null;

    stnTable.featureType = FeatureType.STATION_PROFILE;
    TableConfig timeSeries = stnTable.children.get(0);
    Structure obsv = (Structure) ds.findVariable(timeSeries.name);
    Structure profile = null;
    for (Variable v : obsv.getVariables()) {
      if (v.getDataType() == DataType.SEQUENCE)
        profile = (Structure) v;
    }
    if (profile == null) {
      errlog.format("getStationProfileConfig: must have Sequence varibale for profile%n");
      return null;
    }
    TableConfig profileTc = new TableConfig(Table.Type.NestedStructure, profile.getFullName());
    profileTc.nestedTableName = profile.getShortName();
    Variable elev = findZAxisNotStationAlt(ds);
    if (elev == null) {
      errlog.format("getStationProfileConfig: must have ZAxis that is not the StationAlt%n");
      return null;
    }

    profileTc.elev = elev.getShortName();
     if (profileTc.elev == null) {
      errlog.format("Must have a level coordinate%n");
      return null;
    }
    timeSeries.addChild(profileTc);

    return stnTable;
  }


 /* protected TableConfig getStationAsPointConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    boolean needFinish = false;

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("CdmDirect: Must have a Latitude coordinate");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("CdmDirect: Must have a Longitude coordinate");
      return null;
    }

    if (lat.getRank() != lon.getRank()) {
      errlog.format("CdmDirect: Lat and Lon coordinate must have same rank");
      return null;
    }

    Table.Type obsTableType = Table.Type.Structure;
    TableConfig obs = new TableConfig(obsTableType, "all_data");
    obs.dimName = "all_data";
    obs.structName = "all_data";
    obs.structureType = TableConfig.StructureType.Structure;
    obs.featureType = FeatureType.POINT;

    obs.lat= lat.getName();
    obs.lon= lon.getName();
    obs.time= "time";

    // optional alt coord
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (alt != null)
       obs.elev = alt.getName();

    List<String> vars = new ArrayList<String>(30);
    for (Variable v : ds.getVariables()) {
      if ((v.getDimension(0) == stationDim) &&
          ((v.getRank() == 1) || ((v.getRank() == 2) && (v.getDataType() == DataType.CHAR))))
          vars.add(v.getShortName());
    }

    StructureDS s = new StructurePseudoDS(ds, null, "stnStruct", vars, stationDim);
    obs.addJoin(new JoinMuiltdimStructure(s, obsDim.getLength()));
    obs.addJoin(new JoinArray( time, JoinArray.Type.modulo, obsDim.getLength()));

    if (needFinish) ds.finish();
    return obs;
  }  


  protected TableConfig makeStationTable(NetcdfDataset ds, Formatter errlog) throws IOException {
    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("CdmDirect: Must have a Latitude coordinate");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("CdmDirect: Must have a Longitude coordinate");
      return null;
    }

    if (lat.getRank() != lon.getRank()) {
      errlog.format("CdmDirect: Lat and Lon coordinate must have same rank");
      return null;
    }

    // check dimensions
    Dimension stationDim = null;

    if (lat.getDimension(0) != lon.getDimension(0)) {
      errlog.format("CdmDirect: Lat and Lon coordinate must have same size");
      return null;
    }
    stationDim = lat.getDimension(0);

    Table.Type stationTableType = Table.Type.Structure;
    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.structureType = TableConfig.StructureType.PsuedoStructure;
    stnTable.dimName = stationDim.getName();

    stnTable.lat= lat.getName();
    stnTable.lon= lon.getName();

    stnTable.stnId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ID, stationDim, errlog);
    stnTable.stnDesc = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_DESC, stationDim, errlog);
    stnTable.stnWmoId = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_WMOID, stationDim, errlog);
    stnTable.stnAlt = findNameVariableWithStandardNameAndDimension(ds, CF.STATION_ALTITUDE, stationDim, errlog);

    if (stnTable.stnId == null) {
      errlog.format("Must have a Station id variable with standard name station_id");
      return null;
    }
    Variable stnId = ds.findVariable(stnTable.stnId);
    if (!stnId.getDimension(0).equals(stationDim)) {
      errlog.format("CdmDirect: Station id (%s) outer dimension must match latitude/longitude dimension (%s)", stnTable.stnId, stationDim);
      return null;
    }
    return stnTable;
  } */
}
