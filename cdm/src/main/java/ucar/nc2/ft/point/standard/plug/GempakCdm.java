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

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructurePseudoDS;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.*;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * Gempak Point Obs data.
 *
 * @author caron
 * @since Mar 3, 2009
 */
public class GempakCdm extends TableConfigurerImpl {

  private final String Convention = "GEMPAK/CDM";

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    boolean ok = false;
    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;
    if (conv.equals(Convention)) ok = true;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equals(Convention))
        ok = true;
    }
    if (!ok) return false;

    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    CF.FeatureType ftype = (ftypeS == null) ? CF.FeatureType.point : CF.FeatureType.getFeatureType(ftypeS);
    return (ftype == CF.FeatureType.timeSeries) || (ftype == CF.FeatureType.timeSeriesProfile);
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {

    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    CF.FeatureType ftype = (ftypeS == null) ? CF.FeatureType.point : CF.FeatureType.getFeatureType(ftypeS);
    switch (ftype) {
      case point:
        return null; // use default handler
      case timeSeries:
        if (wantFeatureType == FeatureType.POINT)
          return getStationAsPointConfig(ds, errlog);
        else
          return getStationConfig(ds, errlog);
      case timeSeriesProfile:
          return getStationProfileConfig(ds, errlog);
      default:
        throw new IllegalStateException("unimplemented feature ftype= " + ftype);
    }
  }

  protected TableConfig getStationConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    boolean needFinish = false;

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("GempakCdm: Must have a Latitude coordinate");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("GempakCdm: Must have a Longitude coordinate");
      return null;
    }

    if (lat.getRank() != lon.getRank()) {
      errlog.format("GempakCdm: Lat and Lon coordinate must have same rank");
      return null;
    }

    // check dimensions
    boolean stnIsScalar = (lat.getRank() == 0);
    boolean stnIsSingle = (lat.getRank() == 1) && (lat.getSize() == 1);
    Dimension stationDim = null;

    if (!stnIsScalar) {
      if (lat.getDimension(0) != lon.getDimension(0)) {
        errlog.format("GempakCdm: Lat and Lon coordinate must have same size");
        return null;
      }
      stationDim = lat.getDimension(0);
    }

    boolean hasStruct = Evaluator.hasRecordStructure(ds);

    Table.Type stationTableType = stnIsScalar ? Table.Type.Top : Table.Type.Structure;
    TableConfig stnTable = new TableConfig(stationTableType, "station");
    stnTable.featureType = FeatureType.STATION;
    stnTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;      
    stnTable.dimName = stationDim.getName();

    stnTable.lat= lat.getName();
    stnTable.lon= lon.getName();

    // optional alt coord
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);
    if (alt != null)
      stnTable.stnAlt = alt.getName();

    // station id
    stnTable.stnId = Evaluator.getNameOfVariableWithAttribute(ds, "standard_name", "station_id");
    if (stnTable.stnId == null) {
      errlog.format("Must have a Station id variable with standard name station_id");
      return null;
    }
    Variable stnId = ds.findVariable(stnTable.stnId);

    if (!stnIsScalar) {
      if (!stnId.getDimension(0).equals(stationDim)) {
        errlog.format("GempakCdm: Station id (%s) outer dimension must match latitude/longitude dimension (%s)", stnTable.stnId, stationDim);
        return null;
      }
    }

    // obs table
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("GempakCdm: Must have a Time coordinate");
      return null;
    }
    Dimension obsDim = time.getDimension(time.getRank()-1); // may be time(time) or time(stn, obs)

    Table.Type obsTableType = null;
    Structure multidimStruct = null;
    if (obsTableType == null) {
      // Structure(station, time)
      multidimStruct = Evaluator.getStructureWithDimensions(ds, stationDim, obsDim);
      if (multidimStruct != null) {
        obsTableType = Table.Type.MultidimStructure;
      }
    }

    // multidim case
    if (obsTableType == null) {
      // time(station, time)
      if (time.getRank() == 2) {
        obsTableType = Table.Type.MultidimInner;
      }
    }

    if (obsTableType == null) {
        errlog.format("GempakCdm: Cannot figure out Station/obs table structure");
        return null;
    }

    TableConfig obs = new TableConfig(obsTableType, obsDim.getName());
    obs.dimName = obsDim.getName();
    obs.time = time.getName();
    obs.missingVar = "_isMissing";
    stnTable.addChild(obs);

    if (obsTableType == Table.Type.MultidimStructure) {
      obs.structName = multidimStruct.getName();
      obs.structureType = TableConfig.StructureType.Structure;
      // if time is not in this structure, need to join it
      if (multidimStruct.findVariable( time.getShortName()) == null) {
        obs.addJoin(new JoinArray( time, JoinArray.Type.raw, 0));
      }
    }

    if (obsTableType == Table.Type.MultidimInner) {
      obs.dimName = obsDim.getName();
    }

    if (needFinish) ds.finish();
    return stnTable;
  }

  protected TableConfig getStationAsPointConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    boolean needFinish = false;

    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("GempakCdm: Must have a Latitude coordinate");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("GempakCdm: Must have a Longitude coordinate");
      return null;
    }

    if (lat.getRank() != lon.getRank()) {
      errlog.format("GempakCdm: Lat and Lon coordinate must have same rank");
      return null;
    }

    // check dimensions
    boolean stnIsScalar = (lat.getRank() == 0);
    boolean stnIsSingle = (lat.getRank() == 1) && (lat.getSize() == 1);
    Dimension stationDim = null;

    if (!stnIsScalar) {
      if (lat.getDimension(0) != lon.getDimension(0)) {
        errlog.format("Lat and Lon coordinate must have same size");
        return null;
      }
      stationDim = lat.getDimension(0);
    }

    // optional alt coord
    Variable alt = CoordSysEvaluator.findCoordByType(ds, AxisType.Height);

    // obs table
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("GempakCdm: Must have a Time coordinate");
      return null;
    }
    Dimension obsDim = time.getDimension(time.getRank()-1); // may be time(time) or time(stn, obs)

    Table.Type obsTableType = Table.Type.Structure;
    Structure multidimStruct = Evaluator.getStructureWithDimensions(ds, stationDim, obsDim);

    if (multidimStruct == null) {
        errlog.format("GempakCdm: Cannot figure out StationAsPoint table structure");
        return null;
    }

    TableConfig obs = new TableConfig(obsTableType, obsDim.getName());
    obs.dimName = obsDim.getName();
    obs.structName = multidimStruct.getName();
    obs.structureType = TableConfig.StructureType.Structure;
    obs.featureType = FeatureType.POINT;

    obs.lat= lat.getName();
    obs.lon= lon.getName();
    obs.time= time.getName();
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

  protected TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) throws IOException {
    TableConfig stnTable = makeStationTable(ds, errlog);
    if (stnTable == null) return null;
    Dimension stationDim = ds.findDimension( stnTable.dimName);
    stnTable.featureType = FeatureType.STATION_PROFILE;

    // obs table
    VariableDS time = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
    if (time == null) {
      errlog.format("GempakCdm: Must have a Time coordinate");
      return null;
    }
    Dimension obsDim = time.getDimension(time.getRank()-1); // may be time(time) or time(stn, obs)

    Structure multidimStruct = Evaluator.getStructureWithDimensions(ds, stationDim, obsDim);
    if (multidimStruct == null) {
        errlog.format("GempakCdm: Cannot figure out Station/obs table structure");
        return null;
    }

    TableConfig timeTable = new TableConfig(Table.Type.MultidimStructure, obsDim.getName());
    timeTable.missingVar = "_isMissing";
    timeTable.structName = multidimStruct.getName();
    timeTable.structureType = TableConfig.StructureType.Structure;
    timeTable.addJoin(new JoinArray(time, JoinArray.Type.level, 1));
    timeTable.time = time.getName();
    timeTable.feature_id = time.getName();
    stnTable.addChild(timeTable);

    TableConfig obsTable = new TableConfig(Table.Type.NestedStructure, obsDim.getName());
    Structure nestedStruct = Evaluator.getNestedStructure(multidimStruct);
    if (nestedStruct == null) {
        errlog.format("GempakCdm: Cannot find nested Structure for profile");
        return null;
    }

    obsTable.structName = nestedStruct.getName();
    obsTable.nestedTableName = nestedStruct.getShortName();
    Variable elev = findZAxisNotStationAlt(ds);
     if (elev == null) {
        errlog.format("GempakCdm: Cannot find profile elevation variable");
        return null;
    }
    obsTable.elev = elev.getShortName();
    timeTable.addChild(obsTable);

    return stnTable;
  }

  protected TableConfig makeStationTable(NetcdfDataset ds, Formatter errlog) throws IOException {
    // find lat coord
    Variable lat = CoordSysEvaluator.findCoordByType(ds, AxisType.Lat);
    if (lat == null) {
      errlog.format("GempakCdm: Must have a Latitude coordinate");
      return null;
    }

    // find lon coord
    Variable lon = CoordSysEvaluator.findCoordByType(ds, AxisType.Lon);
    if (lon == null) {
      errlog.format("GempakCdm: Must have a Longitude coordinate");
      return null;
    }

    if (lat.getRank() != lon.getRank()) {
      errlog.format("GempakCdm: Lat and Lon coordinate must have same rank");
      return null;
    }

    // check dimensions
    Dimension stationDim = null;

    if (lat.getDimension(0) != lon.getDimension(0)) {
      errlog.format("GempakCdm: Lat and Lon coordinate must have same size");
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
      errlog.format("GempakCdm: Station id (%s) outer dimension must match latitude/longitude dimension (%s)", stnTable.stnId, stationDim);
      return null;
    }
    return stnTable;
  }
}
