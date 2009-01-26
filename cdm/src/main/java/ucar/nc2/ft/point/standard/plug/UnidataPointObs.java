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
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Dimension;
import ucar.nc2.Structure;

import java.util.Formatter;
import java.util.StringTokenizer;

/**
 * "Unidata Observation Dataset v1.0" point or station or trajectory
 * @author caron
 * @since Apr 23, 2008
 */
public class UnidataPointObs extends TableConfigurerImpl {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnidataPointObs.class);

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if ((wantFeatureType != FeatureType.ANY_POINT) && (wantFeatureType != FeatureType.STATION) && (wantFeatureType != FeatureType.POINT))
      return false;

    FeatureType ft = FeatureDatasetFactoryManager.findFeatureType( ds);
    if ((ft == null) || ((ft != FeatureType.STATION) && (ft != FeatureType.POINT)))
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

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {

    Dimension obsDim = UnidataPointDatasetHelper.findObsDimension(ds);
    if (obsDim == null) {
      errlog.format("Must have an Observation dimension: named by global attribute 'observationDimension', or unlimited dimension");
      return null;
    }
    boolean isPsuedo = !obsDim.isUnlimited();

    FeatureType ft = Evaluator.getFeatureType(ds, ":cdm_datatype", null);
    if (ft == null )
      ft = Evaluator.getFeatureType(ds, ":cdm_data_type", null);

    // its really a point
    if (ft == FeatureType.POINT) {
      TableConfig obsTable = new TableConfig(Table.Type.Structure, isPsuedo? obsDim.getName() : "record");
      obsTable.isPsuedoStructure = isPsuedo;
      obsTable.featureType = FeatureType.POINT;
      obsTable.structName = "record";

      obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time, obsDim);
      obsTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, obsDim);
      obsTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, obsDim);
      obsTable.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, obsDim);

      obsTable.dim = obsDim;
      return obsTable;
    }

    //  we want a point dataset, but its really a station
    // iterate over obs struct, in file order
    // extra join on station structure
    if ((ft == FeatureType.POINT) && (wantFeatureType == FeatureType.POINT)) {
      TableConfig obsTable = new TableConfig(Table.Type.Structure, isPsuedo? obsDim.getName() : "record");
      obsTable.isPsuedoStructure = isPsuedo;
      obsTable.featureType = FeatureType.POINT;
      obsTable.dim = obsDim;
      obsTable.structName = "record";

      obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time, obsDim);
      obsTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, obsDim);
      obsTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, obsDim);
      obsTable.stnAlt = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, obsDim);

      // if they have lat and lon in the obs, then use it
      if ((obsTable.lat != null) && (obsTable.lon != null)) {
        return obsTable;
      }

      // otherwise join it to the station with a parent_index
      String parentIndexVar = UnidataPointDatasetHelper.findVariableName(ds, "parent_index");
      if (parentIndexVar == null)  {
        errlog.format("Must have a parent_index variable");
        return null;
      }

      Dimension stationDim = UnidataPointDatasetHelper.findDimension(ds, "station");
      if (stationDim == null) {
        errlog.format("Must have a station dimension");
        return null;
      }

      TableConfig stationTable = new TableConfig(Table.Type.Structure, "station");
      stationTable.isPsuedoStructure = true;
      stationTable.dim = stationDim;
      stationTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, stationDim);
      stationTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, stationDim);
      stationTable.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, stationDim);
      stationTable.structName = "station";

      Structure stns = new ucar.nc2.StructurePseudo(ds, null, "stationPsuedoStructure", stationDim);
      obsTable.extraJoin = new JoinParentIndex(stns, parentIndexVar);

      return obsTable;
    }


    // its really a trajectory
    if (ft == FeatureType.TRAJECTORY) {
      TableConfig obsTable = new TableConfig(Table.Type.Structure, isPsuedo? obsDim.getName() : "record");
      obsTable.isPsuedoStructure = isPsuedo;
      obsTable.featureType = FeatureType.TRAJECTORY;

      obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time, obsDim);
      obsTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, obsDim);
      obsTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, obsDim);
      obsTable.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, obsDim);
      obsTable.dim = obsDim;

      Dimension trajDim = UnidataPointDatasetHelper.findDimension(ds, "trajectory");
      if (trajDim != null) {
        log.error("Ignoring trajectory structure "+ds.getLocation());
      }

      return obsTable;
    }

    // otherwise its a Station
    Dimension stationDim = UnidataPointDatasetHelper.findDimension(ds, "station");
    if (stationDim == null) {
      errlog.format("Must have a dimension named station, or named by global attribute 'stationDimension'");
      return null;
    }

    String lastVar = UnidataPointDatasetHelper.findVariableName(ds, "lastChild");
    String prevVar = UnidataPointDatasetHelper.findVariableName(ds, "prevChild");
    String firstVar = UnidataPointDatasetHelper.findVariableName(ds, "firstChild");
    String nextVar = UnidataPointDatasetHelper.findVariableName(ds, "nextChild");
    String numChildrenVar = UnidataPointDatasetHelper.findVariableName(ds, "numChildren");

    boolean isForwardLinkedList = (firstVar != null) && (nextVar != null);
    boolean isBackwardLinkedList = (lastVar != null) && (prevVar != null);
    boolean isContiguousList = !isForwardLinkedList && !isBackwardLinkedList && (firstVar != null) && (numChildrenVar != null);
    boolean isMultiDim = !isForwardLinkedList && !isBackwardLinkedList && !isContiguousList;

    // station table
    TableConfig stationTable = new TableConfig(Table.Type.Structure, "station");
    stationTable.isPsuedoStructure = true;
    stationTable.featureType = ft;
    stationTable.dim = stationDim;
    stationTable.limit = Evaluator.getVariableName(ds, "number_stations", null);

    stationTable.stnId = Evaluator.getVariableName(ds, "station_id", null);
    stationTable.stnDesc = Evaluator.getVariableName(ds, "station_description", null);
    stationTable.stnWmoId = Evaluator.getVariableName(ds, "wmo_id", null);

    stationTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat);
    stationTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon);
    stationTable.stnAlt = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);

    // obs table
    TableConfig obsTable;
    if (isMultiDim) {
      obsTable = new TableConfig(Table.Type.MultiDimInner, "obs");
      obsTable.outer = stationDim;
      obsTable.dim = obsDim;

    } else {

      obsTable = new TableConfig(Table.Type.Structure, isPsuedo? obsDim.getName() : "record");
      obsTable.isPsuedoStructure = isPsuedo;
      obsTable.structName = "record";

      if (isForwardLinkedList) {
        obsTable.start = firstVar;
        obsTable.next = nextVar;

      } else if (isBackwardLinkedList) {
        obsTable.start = lastVar;
        obsTable.next = prevVar;

      } else {
        obsTable.start = firstVar;
      }

      obsTable.numRecords = numChildrenVar;
      obsTable.parentIndex = Evaluator.getVariableName(ds, "parent_index", null);
    }

    obsTable.dim = obsDim;
    obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time);
    obsTable.timeNominal = Evaluator.getVariableName(ds, "time_nominal", null);
    obsTable.featureType = FeatureType.STATION;

    stationTable.addChild(obsTable);
    return stationTable;
  }

}
