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

import ucar.nc2.constants.FeatureType;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.ft.point.UnidataPointDatasetHelper;
import ucar.nc2.ft.point.standard.NestedTable;
import ucar.nc2.ft.point.standard.CoordSysAnalyzer;
import ucar.ma2.StructureMembers;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Unidata Observation Dataset v1.0 Conventions
 *
 * @author caron
 * @since Apr 18, 2008
 */
public class UnidataPointObsAnalyzer extends CoordSysAnalyzer {

  private FeatureType getFeatureType() {
    String datatype = ds.findAttValueIgnoreCase(null, "cdm_datatype", null);
    if (datatype == null)
      datatype = ds.findAttValueIgnoreCase(null, "cdm_data_type", null);
    return (datatype == null) ? null : FeatureType.getType(datatype);
  }

  @Override
  public void makeJoins() throws IOException {
    super.makeJoins();

    Dimension obsDim = UnidataPointDatasetHelper.findObsDimension(ds);
    if (obsDim == null) {
      userAdvice.format("Must have an Observation dimension: named by global attribute 'observationDimension', or unlimited dimension");
      return;
    }

    FeatureType ft = getFeatureType();
    if (ft == FeatureType.POINT) return;

    Variable lastVar = UnidataPointDatasetHelper.findVariable(ds, "lastChild");
    Variable prevVar = UnidataPointDatasetHelper.findVariable(ds, "prevChild");
    Variable firstVar = UnidataPointDatasetHelper.findVariable(ds, "firstChild");
    Variable nextVar = UnidataPointDatasetHelper.findVariable(ds, "nextChild");
    Variable numChildrenVar = UnidataPointDatasetHelper.findVariable(ds, "numChildren");

    Dimension stationDim = UnidataPointDatasetHelper.findDimension(ds, "station");
    if (stationDim == null) {
      userAdvice.format("Must have a dimension named station, or named by global attribute 'stationDimension'");
      return;
    }

    stationInfo.stationId = getMemberName( "station_id");
    stationInfo.stationDesc = getMemberName( "station_description");
    stationInfo.stationNpts = numChildrenVar == null ? null : numChildrenVar.getShortName();
    Variable stationMax = UnidataPointDatasetHelper.findVariable(ds, "number_station");
    if (stationMax != null)
      stationInfo.nstations = stationMax.readScalarInt();

    // annotate station table
    NestedTable.Table obsTable = tableFind.get( obsDim.getName());
    NestedTable.Table stnTable = tableFind.get( stationDim.getName());

    // not implemented
    // Variable stationIndexVar = UnidataPointDatasetHelper.findVariable(ds, "parent_index");


    boolean isForwardLinkedList = (firstVar != null) && (nextVar != null);
    boolean isBackwardLinkedList = (lastVar != null) && (prevVar != null);
    boolean isContiguousList = !isForwardLinkedList && !isBackwardLinkedList && (firstVar != null) && (numChildrenVar != null);

    NestedTable.Join join;
    if (isContiguousList) {
      join = new NestedTable.Join(NestedTable.JoinType.ContiguousList);
      join.setTables(stnTable, obsTable);
      join.setJoinVariables(firstVar, null, numChildrenVar);
      joins.add(join);

    } else if (isForwardLinkedList) {
      join = new NestedTable.Join(NestedTable.JoinType.ForwardLinkedList);
      join.setTables(stnTable, obsTable);
      join.setJoinVariables(firstVar, nextVar, null);
      joins.add(join);

    } else if (isBackwardLinkedList) {
      join = new NestedTable.Join(NestedTable.JoinType.BackwardLinkedList);
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
      join = new NestedTable.Join(NestedTable.JoinType.MultiDim);
      join.setTables(stnTable, obsTable);
      joins.add(join);
    }

    /* if (stationIndexVar != null) {
     join = new Join("ParentIndex", JoinType.Index);
     join.setTables( stationIndexVar.getDimension(0).getName(), stationDim.getName());
     join.setJoinVariables(stationIndexVar, null);
     joins.add(join);
   } */
  }

  private String getMemberName( String name) {
    Variable v = UnidataPointDatasetHelper.findVariable(ds, name);
    return (v != null) ? v.getShortName() : null;
  }

}
