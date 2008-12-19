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
public class Madis implements TableConfigurer {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if ((wantFeatureType != FeatureType.ANY_POINT) && (wantFeatureType != FeatureType.STATION) && (wantFeatureType != FeatureType.POINT))
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
      errlog.format("Must have an Observation dimension: named recNum");
      return null;
    }
    VNames vn = getVariableNames(ds, errlog);

    FlattenedTable.TableType obsStructureType = obsDim.isUnlimited() ? FlattenedTable.TableType.Structure : FlattenedTable.TableType.PseudoStructure;
    FeatureType ft = Evaluator.getFeatureType(ds, ":thredds_data_type", errlog);
    if (null == ft) ft = FeatureType.POINT;

    //if ((wantFeatureType == FeatureType.POINT) || (ft == FeatureType.POINT)) {
      TableConfig ptTable = new TableConfig(obsStructureType, "record");
      ptTable.featureType = FeatureType.POINT;

      ptTable.dim = obsDim;
      ptTable.time = vn.obsTime;
      ptTable.timeNominal = vn.nominalTime;
      ptTable.lat = vn.lat;
      ptTable.lon = vn.lon;
      ptTable.elev = vn.elev;

      return ptTable;
    //}

    /*
    // dont use station for now
    TableConfig nt = new TableConfig(FlattenedTable.TableType.PseudoStructure, "station");
    nt.featureType = ft;

    nt.dim = Evaluator.getDimension(ds, "maxStaticIds", errlog);
    nt.limit = Evaluator.getVariableName(ds, "nStaticIds", errlog);

    TableConfig obs = new TableConfig(FlattenedTable.TableType.Structure, "record");
    obs.dim = Evaluator.getDimension(ds, "recNum", errlog);
    obs.time = vn.obsTime;
    obs.timeNominal = vn.nominalTime;

    obs.stnId = Evaluator.getVariableName(ds, ":stationIdVariable", errlog);
    obs.stnDesc = Evaluator.getVariableName(ds, ":stationDescriptionVariable", errlog);
    obs.lat = vn.lat;
    obs.lon = vn.lon;
    obs.elev = vn.elev;

    TableConfig.JoinConfig join = new TableConfig.JoinConfig(Join.Type.BackwardLinkedList);
    join.start = Evaluator.getVariableName(ds, "lastRecord", errlog);
    join.next = Evaluator.getVariableName(ds, "prevRecord", errlog);
    obs.join = join;

    nt.addChild(obs);
    return nt;   */
  }

  private class VNames {
    String lat, lon, elev, obsTime, nominalTime;
  }

  private VNames getVariableNames(NetcdfDataset ds, Formatter errlog) {
    VNames vn = new VNames();

    String val = ds.findAttValueIgnoreCase(null, "stationLocationVariables", null);
    if (val == null) {
      if (errlog != null) errlog.format(" Cant find global attribute stationLocationVariables\n");
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
      if (errlog != null) errlog.format(" Cant find global attribute timeVariables\n");
      vn.obsTime = "observationTime";
      vn.nominalTime = "reportTime";
    } else {
      String[] vals = val.split(",");
      if (vals.length > 0) vn.obsTime = vals[0];
      if (vals.length > 1) vn.nominalTime = vals[1];
    }

    return vn;
  }

}
