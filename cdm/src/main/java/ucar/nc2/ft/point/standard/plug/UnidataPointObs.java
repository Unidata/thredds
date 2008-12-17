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
import ucar.nc2.ft.point.standard.plug.UnidataPointDatasetHelper;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Dimension;

import java.util.Formatter;
import java.util.StringTokenizer;

/**
 * "Unidata Observation Dataset v1.0" point or station
 * @author caron
 * @since Apr 23, 2008
 */
public class UnidataPointObs implements TableConfigurer {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if ((wantFeatureType != FeatureType.ANY_POINT) && (wantFeatureType != FeatureType.STATION) && (wantFeatureType != FeatureType.POINT))
      return false;

    FeatureType ft = Evaluator.getFeatureType(ds, ":cdm_datatype", null);
    if (ft == null )
      ft = Evaluator.getFeatureType(ds, ":cdm_data_type", null);

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
    NestedTable.TableType obsStructureType = obsDim.isUnlimited() ? NestedTable.TableType.Structure : NestedTable.TableType.PseudoStructure;

    FeatureType ft = Evaluator.getFeatureType(ds, ":cdm_datatype", null);
    if (ft == null )
      ft = Evaluator.getFeatureType(ds, ":cdm_data_type", null);

    if ((wantFeatureType == FeatureType.POINT) || (ft == FeatureType.POINT)) {
      TableConfig result = new TableConfig(obsStructureType, "record");
      result.featureType = FeatureType.POINT;

      result.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time);
      result.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat);
      result.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon);
      result.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);

      result.dim = obsDim;
      return result;
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

    TableConfig nt = new TableConfig(NestedTable.TableType.PseudoStructure, "station");
    nt.featureType = Evaluator.getFeatureType(ds, ":cdm_datatype", errlog);
    nt.dim = stationDim;
    nt.limit = Evaluator.getVariableName(ds, "number_stations", errlog);

    nt.stnId = Evaluator.getVariableName(ds, "station_id", errlog);
    nt.stnDesc = Evaluator.getVariableName(ds, "station_description", errlog);
    nt.stnWmoId = Evaluator.getVariableName(ds, "wmo_id", errlog);

    nt.lat = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lat);
    nt.lon = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Lon);
    nt.elev = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Height);

    TableConfig obs;
    if (isMultiDim) {
      obs = new TableConfig(NestedTable.TableType.MultiDim, "obs");
      obs.outer = stationDim;
      obs.dim = obsDim;
      obs.join = new TableConfig.JoinConfig(Join.Type.MultiDim);

    } else {

      if (obsDim.isUnlimited())
        obs = new TableConfig(NestedTable.TableType.Structure, "record");
      else
        obs = new TableConfig(NestedTable.TableType.PseudoStructure, obsDim.getName());

      TableConfig.JoinConfig join;
      if (isForwardLinkedList) {
        join = new TableConfig.JoinConfig(Join.Type.ForwardLinkedList);
        join.start = firstVar;
        join.next = nextVar;

      } else if (isBackwardLinkedList) {
        join = new TableConfig.JoinConfig(Join.Type.BackwardLinkedList);
        join.start = lastVar;
        join.next = prevVar;

      } else {
        join = new TableConfig.JoinConfig(Join.Type.ContiguousList);
        join.start = firstVar;
      }

      join.numRecords = numChildrenVar;
      join.parentIndex = Evaluator.getVariableName(ds, "parent_index", errlog);
      obs.join = join;
    }

    obs.dim = obsDim;
    obs.time = UnidataPointDatasetHelper.getCoordinateName(ds, AxisType.Time);
    obs.timeNominal = Evaluator.getVariableName(ds, "time_nominal", errlog);

    nt.addChild(obs);
    return nt;
  }

  /*

     if (isContiguousList) {
     join = new Join(Join.Type.ContiguousList);
     join.setTables(stnTable, obsTable);
     join.setJoinVariables(firstVar, null, numChildrenVar);
     joins.add(join);

   } else if (isForwardLinkedList) {
     join = new Join(Join.Type.ForwardLinkedList);
     join.setTables(stnTable, obsTable);
     join.setJoinVariables(firstVar, nextVar, null);
     joins.add(join);

   } else if (isBackwardLinkedList) {
     join = new Join(Join.Type.BackwardLinkedList);
     join.setTables(stnTable, obsTable);
     join.setJoinVariables(lastVar, prevVar, null);
     joins.add(join);

   } else if (obsTable == null) {  // multidim

     // create the obsTable - all variables with (stationDim,obsDim,...)
     List<Variable> obsVariables = new ArrayList<Variable>();
     StructureMembers structureMembers = new StructureMembers("obs");
     for (Variable v : ds.getVariables()) {
       if (v.getRank() < 2) continue;
       if (v.getDimension(0).equals( stationDim) && v.getDimension(1).equals( obsDim)) {
         obsVariables.add(v);
         int[] shape = v.getShape();
         shape[0] = 1;
         shape[1] = 1;
         structureMembers.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
       }
     }
     obsTable = new NestedTable.Table(obsVariables, stationDim, obsDim);
     addTable( obsTable);

     // make join
     join = new Join(Join.Type.MultiDim);
     join.setTables(stnTable, obsTable);
     joins.add(join);
   }
  */
}
