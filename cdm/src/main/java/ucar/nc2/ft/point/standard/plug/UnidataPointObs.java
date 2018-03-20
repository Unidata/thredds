/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.CDM;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructurePseudoDS;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Dimension;

import java.util.Formatter;
import java.util.StringTokenizer;

/**
 * "Unidata Observation Dataset v1.0" point or station or trajectory.
 * This convention is deprecated in favor of CF-1.6.
 *
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

    String conv = ds.findAttValueIgnoreCase(null, CDM.CONVENTIONS, null);
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
    boolean hasStruct = Evaluator.hasNetcdf3RecordStructure(ds);

    FeatureType ft = Evaluator.getFeatureType(ds, ":cdm_datatype", null);
    if (ft == null )
      ft = Evaluator.getFeatureType(ds, ":cdm_data_type", null);

    // its really a point
    if (ft == FeatureType.POINT) {
      TableConfig obsTable = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName());
      obsTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      obsTable.featureType = FeatureType.POINT;
      obsTable.structName = "record";

      obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time, obsDim);
      obsTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, obsDim);
      obsTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, obsDim);
      obsTable.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, obsDim);

      obsTable.dimName = obsDim.getShortName();
      return obsTable;
    }

    //  we want a point dataset, but its really a station
    // iterate over obs struct, in file order
    // extra join on station structure
    if ((ft == FeatureType.STATION) && (wantFeatureType == FeatureType.POINT)) {
      TableConfig obsTable = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName() );
      obsTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      obsTable.featureType = FeatureType.POINT;
      obsTable.dimName = obsDim.getShortName();
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
      obsTable.parentIndex = parentIndexVar;

      Dimension stationDim = UnidataPointDatasetHelper.findDimension(ds, "station");
      if (stationDim == null) {
        errlog.format("Must have a station dimension");
        return null;
      }

      // TableConfig stationTable = new TableConfig(Table.Type.Structure, "station");
      //stationTable.isPsuedoStructure = true;
      //stationTable.dim = stationDim;
      obsTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, stationDim);
      obsTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, stationDim);
      obsTable.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, stationDim);

      StructureDS stns = new StructurePseudoDS(ds, null, "stationPsuedoStructure", null, stationDim);
      obsTable.addJoin( new JoinParentIndex(stns, parentIndexVar));

      return obsTable;
    }


    // its really a trajectory
    if (ft == FeatureType.TRAJECTORY) {
      TableConfig obsTable = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName());
      obsTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      obsTable.featureType = FeatureType.TRAJECTORY;

      obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time, obsDim);
      obsTable.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat, obsDim);
      obsTable.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon, obsDim);
      obsTable.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height, obsDim);
      obsTable.dimName = obsDim.getShortName();

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
    stationTable.structureType = TableConfig.StructureType.PsuedoStructure;
    stationTable.featureType = FeatureType.STATION;
    stationTable.dimName = stationDim.getShortName();
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
      obsTable = new TableConfig(Table.Type.MultidimInner, "obs");
      obsTable.outerName = stationDim.getShortName();
      obsTable.innerName = obsDim.getShortName();
      obsTable.dimName = obsDim.getShortName();

    } else {

      Table.Type obsType =  isForwardLinkedList || isBackwardLinkedList ? Table.Type.LinkedList : Table.Type.Contiguous;

      obsTable = new TableConfig(obsType, hasStruct ? "record" : obsDim.getShortName());
      obsTable.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
      obsTable.structName = "record";

      if (isForwardLinkedList) {
        stationTable.start = firstVar; // this will remove it from list of data vars
        obsTable.start = firstVar;
        obsTable.next = nextVar;

      } else if (isBackwardLinkedList) {
        stationTable.start = lastVar; // this will remove it from list of data vars
        obsTable.start = lastVar;
        obsTable.next = prevVar;

      } else if (isContiguousList) {
        stationTable.start = firstVar; // this will remove it from list of data vars
        stationTable.numRecords = numChildrenVar; // this will remove it from list of data vars
        obsTable.start = firstVar;
      }

      obsTable.numRecords = numChildrenVar;
      obsTable.parentIndex = Evaluator.getVariableName(ds, "parent_index", null);
    }

    obsTable.dimName = obsDim.getShortName();
    obsTable.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time);
    obsTable.timeNominal = Evaluator.getVariableName(ds, "time_nominal", null);

    stationTable.addChild(obsTable);
    return stationTable;
  }

}
