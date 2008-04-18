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
package ucar.nc2.dt2.coordsys;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.dt2.point.UnidataPointDatasetHelper;

import java.util.StringTokenizer;
import java.io.IOException;

/**
 * Unidata Observation Dataset v1.0 Conventions
 *
 * @author caron
 * @since Apr 18, 2008
 */
public class UnidataPointObsConvention extends CoordSysAnalyzer {

  static public boolean isMine(NetcdfDataset ds) {
    // find datatype
    String datatype = ds.findAttValueIgnoreCase(null, "cdm_datatype", null);
    if (datatype == null)
      datatype = ds.findAttValueIgnoreCase(null, "cdm_data_type", null);
    if (datatype == null)
      return false;
    if (!datatype.equalsIgnoreCase(FeatureType.POINT.toString()) && !datatype.equalsIgnoreCase(FeatureType.STATION.toString()))
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

  @Override
  public void makeJoins() throws IOException {
    super.makeJoins();

    atts.add(new Attribute("station_id", "station_id"));
    atts.add(new Attribute("station_desc", "station_description"));

    Variable lastVar = UnidataPointDatasetHelper.findVariable(ds, "lastChild");
    Variable prevVar = UnidataPointDatasetHelper.findVariable(ds, "prevChild");
    Variable firstVar = UnidataPointDatasetHelper.findVariable(ds, "firstChild");
    Variable nextVar = UnidataPointDatasetHelper.findVariable(ds, "nextChild");
    Variable numChildrenVar = UnidataPointDatasetHelper.findVariable(ds, "numChildren");
    if (numChildrenVar != null)
      atts.add(new Attribute("station_npts", numChildrenVar.getShortName()));

    // not implemented
    // Variable stationIndexVar = UnidataPointDatasetHelper.findVariable(ds, "parent_index");

    Dimension stationDim = UnidataPointDatasetHelper.findDimension(ds, "station");
    if (stationDim == null) return;

    // annotate station table
    // NestedTable.Table stnTable = tableFind.get(stationDim.getName());

    Dimension obsDim = UnidataPointDatasetHelper.findDimension(ds, "obs");
    if (obsDim == null)
      obsDim = ds.getUnlimitedDimension();

    boolean isForwardLinkedList = (firstVar != null) && (nextVar != null);
    boolean isBackwardLinkedList = (lastVar != null) && (prevVar != null);
    boolean isContiguousList = !isForwardLinkedList && !isBackwardLinkedList && (firstVar != null) && (numChildrenVar != null);

    NestedTable.Join join;
    if (isContiguousList) {
      join = new NestedTable.Join(NestedTable.JoinType.ContiguousList);
      setTables(join, firstVar.getDimension(0).getName(), obsDim.getName());
      join.setJoinVariables(firstVar, null, numChildrenVar);
      joins.add(join);
    }

    if (isForwardLinkedList) {
      join = new NestedTable.Join(NestedTable.JoinType.ForwardLinkedList);
      setTables(join, firstVar.getDimension(0).getName(), nextVar.getDimension(0).getName());
      join.setJoinVariables(firstVar, nextVar, null);
      joins.add(join);
    }

    if (isBackwardLinkedList) {
      join = new NestedTable.Join(NestedTable.JoinType.BackwardLinkedList);
      setTables(join, lastVar.getDimension(0).getName(), prevVar.getDimension(0).getName());
      join.setJoinVariables(lastVar, prevVar, null);
      joins.add(join);
    }

    /* if (stationIndexVar != null) {
     join = new Join("ParentIndex", JoinType.Index);
     join.setTables( stationIndexVar.getDimension(0).getName(), stationDim.getName());
     join.setJoinVariables(stationIndexVar, null);
     joins.add(join);
   } */
  }
}
