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
package ucar.nc2.dataset;

import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.dt2.point.UnidataPointDatasetHelper;
import ucar.nc2.constants.DataType;

import java.util.*;
import java.io.IOException;

/**
 * @author caron
 * @since Mar 20, 2008
 */
public class CoordSysAnalyzer {
  NetcdfDataset ds;
  Map<String,Table> tables = new HashMap<String,Table>();
  List<Join> joins = new ArrayList<Join>();

  public CoordSysAnalyzer(NetcdfDataset ds) {
    this.ds = ds;

    analyzeCoordSys();
    analyzeCoordAxes();
  }

  private void analyzeCoordSys() {
    List<CoordinateSystem> csys = ds.getCoordinateSystems();
    for (CoordinateSystem cs : csys) {
      System.out.println(" " + cs);
    }
    System.out.println();
  }


  private void analyzeCoordAxes() {
    List<CoordinateAxis> axes = ds.getCoordinateAxes();
    for (CoordinateAxis axis : axes) {
      System.out.println(" " + axis.getAxisType() + " " + axis.getNameAndDimensions());
    }
    System.out.println();

    List<Dimension> dims = ds.getDimensions();
    for (Dimension dim : dims) {
      List<Variable> vars = getStructVars(dim);
      if (vars.size() > 0)
        tables.put(dim.getName(), new Table(vars, dim));
    }

    for (Table t : tables.values())
      System.out.println(t);

    UnidataConvention unidataConvention = new UnidataConvention();
    if (unidataConvention.isMine(ds)) {
      unidataConvention.findJoins();
    }

    System.out.println("Joins");
    for (Join j : joins)
      System.out.println(" "+j);

  }

  private List<Variable> getStructVars(Dimension dim) {
    List<Variable> structVars = new ArrayList<Variable>();
    List<Variable> recordMembers = ds.getVariables();
    for (Variable v : recordMembers) {
      if (v.isScalar()) continue;
      if (v.getDimension(0) == dim) {
        structVars.add(v);
      }
    }
    return structVars;
  }

  private class Table {
    Dimension dim;
    List<Variable> cols;
    List<CoordinateAxis> coordVars = new ArrayList<CoordinateAxis>();

    Table(List<Variable> cols, Dimension dim) {
      this.dim = dim;
      this.cols = cols;
      for (Variable v : cols) {
        if (v instanceof CoordinateAxis)
          coordVars.add((CoordinateAxis) v);
      }
    }

    public String getName() { return dim.getName(); }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("Table on dimension ").append(dim.getName()).append("\n");
      for (CoordinateAxis axis : coordVars) {
        sbuff.append(" Coordinate= ").append(axis.getAxisType()).append(": ").append(axis.getNameAndDimensions()).append("\n");
      }
      sbuff.append(" Data Variables= ").append(cols.size()).append("\n");
      return sbuff.toString();
    }

    public String showAll() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("Table on dimension ").append(dim.getName()).append("\n");
      for (Variable v : cols) {
        sbuff.append("  ").append(v.getNameAndDimensions()).append("\n");
      }
      return sbuff.toString();
    }
  }

  private class Join {
    String name;
    Table fromTable, toTable;

    Join(String name) {
      this.name = name;
    }

    void setTables(String fromName, String toName) {
      if (null != fromName)
        fromTable = tables.get(fromName);
      if (null != toName)
        toTable = tables.get(toName);
    }


    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(name);
      if (fromTable != null)
        sbuff.append(" from ").append(fromTable.getName());
      if (toTable != null)
        sbuff.append(" to ").append(toTable.getName());

      return sbuff.toString();
    }

  }

  private class UnidataConvention {

    public boolean isMine(NetcdfDataset ds) {
      // find datatype
      String datatype = ds.findAttValueIgnoreCase(null, "cdm_datatype", null);
      if (datatype == null)
        datatype = ds.findAttValueIgnoreCase(null, "cdm_data_type", null);
      if (datatype == null)
        return false;
      if (!datatype.equalsIgnoreCase(DataType.POINT.toString()) && !datatype.equalsIgnoreCase(DataType.STATION.toString()))
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

    void findJoins() {
      Variable lastVar = UnidataPointDatasetHelper.findVariable(ds, "lastChild");
      Variable prevVar = UnidataPointDatasetHelper.findVariable(ds, "prevChild");
      Variable firstVar = UnidataPointDatasetHelper.findVariable(ds, "firstChild");
      Variable nextVar = UnidataPointDatasetHelper.findVariable(ds, "nextChild");
      Variable numChildrenVar = UnidataPointDatasetHelper.findVariable(ds, "numChildren");
      Variable stationIndexVar = UnidataPointDatasetHelper.findVariable(ds, "parent_index");

      Dimension stationDim = UnidataPointDatasetHelper.findDimension(ds, "station");
      if (stationDim == null) return;

      Dimension obsDim = UnidataPointDatasetHelper.findDimension(ds, "obs");
      if (obsDim == null)
        obsDim = ds.getUnlimitedDimension();

      boolean isForwardLinkedList = (firstVar != null) && (nextVar != null);
      boolean isBackwardLinkedList = (lastVar != null) && (prevVar != null);
      boolean isContiguousList = !isForwardLinkedList && !isBackwardLinkedList && (firstVar != null) && (numChildrenVar != null);

      Join join;
      if (isForwardLinkedList) {
        join = new Join("ForwardLinkedList");
        join.setTables( firstVar.getDimension(0).getName(), nextVar.getDimension(0).getName());
        joins.add(join);
      }

      if (isBackwardLinkedList) {
        join = new Join("BackwardLinkedList");
        join.setTables( lastVar.getDimension(0).getName(), prevVar.getDimension(0).getName());
        joins.add(join);
      }

      if (isContiguousList) {
        join = new Join("ContiguousList");
        join.setTables( firstVar.getDimension(0).getName(), obsDim.getName());
        joins.add(join);
      }

      if (stationIndexVar != null) {
        join = new Join("ParentIndex");
        join.setTables( stationIndexVar.getDimension(0).getName(), stationDim.getName());
        joins.add(join);
      }
    }
  }


  static void doit(String filename) throws IOException {
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    new CoordSysAnalyzer(ncd);
    System.out.println("-----------------");
  }

  static public void main(String args[]) throws IOException {
    doit("C:/data/metars/Surface_METAR_20070326_0000.nc");
    doit("C:/data/rotatedPole/eu.mn.std.fc.d00z20070820.ncml");
    doit("C:/data/dt2/station/madis2.sao.gz");
    doit("C:/data/dt2/station/solrad_point_pearson.ncml");
  }
}
