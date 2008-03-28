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

import ucar.nc2.Variable;
import ucar.nc2.Dimension;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt2.point.UnidataPointDatasetHelper;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;

import java.util.*;
import java.io.IOException;

/**
 * @author caron
 * @since Mar 20, 2008
 */
public class CoordSysAnalyzer {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordSysAnalyzer.class);

  private NetcdfDataset ds;
  private Map<String,Table> tables = new HashMap<String,Table>();
  private List<Join> joins = new ArrayList<Join>();

  private List<FlatTable> leaves = new ArrayList<FlatTable>();

  public CoordSysAnalyzer(NetcdfDataset ds) {
    this.ds = ds;
    makeTables();

    // link the tables together with joins
    for (Join join : joins) {
      Table parent = join.fromTable;
      Table child = join.toTable;

      if (child.parent != null) throw new IllegalStateException("Multiple parents");
      child.parent = parent;

      if (parent.children == null) parent.children = new ArrayList<Join>();
      parent.children.add(join);
    }

    // find the leaves
    for (Table table : tables.values()) {
      if (table.children == null) { // its a leaf
        FlatTable flatTable = new FlatTable( table);
        if (flatTable.isOk) { // it has lat,lon,time coords
          leaves.add(flatTable);
        }
      }
    }

  }

  public List<FlatTable> getTables() { return leaves; }

  public void showCoordSys() {
    List<CoordinateSystem> csys = ds.getCoordinateSystems();
    System.out.println("Coordinate Systems");
    for (CoordinateSystem cs : csys) {
      System.out.println(" " + cs);
    }
    System.out.println();
  }

  public void showCoordAxes() {
    List<CoordinateAxis> axes = ds.getCoordinateAxes();
    System.out.println("Axes");
    for (CoordinateAxis axis : axes) {
      System.out.println(" " + axis.getAxisType() + " " + axis.getNameAndDimensions());
    }
    System.out.println();
  }

  public void showTables() {
    for (Table t : tables.values())
      System.out.println(t);

    System.out.println("Joins");
    for (Join j : joins)
      System.out.println(" "+j);
  }

  public void showFlatTables() {
    System.out.println("FlatTables");
    for (FlatTable t : leaves)
      System.out.println(t);
  }

  public void showAll() {
    System.out.println("Dataset "+ds.getLocation());
    showCoordSys();
    showCoordAxes();
    showTables();
    showFlatTables();
  }

  private void makeTables() {
    List<Dimension> dims = ds.getDimensions();
    for (Dimension dim : dims) {
      List<Variable> vars = getStructVars(dim);
      if (vars.size() > 0)
        tables.put(dim.getName(), new Table(vars, dim));
    }

    UnidataConvention unidataConvention = new UnidataConvention();
    if (unidataConvention.isMine(ds)) {
      unidataConvention.findJoins();
    }
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

  // a leaf with all of its parents
  public class FlatTable {
    public CoordinateAxis timeAxis, latAxis, lonAxis, heightAxis;

    Table leaf;
    boolean isOk;
    int nestedLevels;
    Join useJoin;

    FlatTable(Table leaf) {
      this.leaf = leaf;
      timeAxis = findCoordinateAxis(AxisType.Time, leaf);
      latAxis = findCoordinateAxis(AxisType.Lat, leaf);
      lonAxis = findCoordinateAxis(AxisType.Lon, leaf);
      heightAxis = findCoordinateAxis(AxisType.Height, leaf);
      isOk = (timeAxis != null) && (latAxis != null) && (lonAxis != null);

      // count nesting
      nestedLevels = 1;
      Table t = leaf;
      while (t.parent != null) {
        t = t.parent;
        nestedLevels++;
      }
    }

    private CoordinateAxis findCoordinateAxis(AxisType axisType, Table t) {
      if (t == null) return null;
      CoordinateAxis axis = t.findCoordinateAxis(axisType);
      return (axis != null) ? axis : findCoordinateAxis(axisType, t.parent);
    }

    public FeatureType getFeatureType() {
      if (nestedLevels == 1) return FeatureType.POINT;

      Table parent = leaf.parent;
      if (parent.children.size() > 0) {
        Collections.sort(parent.children);
          useJoin = parent.children.get(0);
      }

      if (useJoin != null)
        return FeatureType.STATION;

      return null;
    }

    public DateUnit getTimeUnit() {
      try {
        return new DateUnit(timeAxis.getUnitsString());
      } catch (Exception e) {
        throw new IllegalArgumentException("Error on time string = " + timeAxis.getUnitsString() + " == " + e.getMessage());
      }
    }

    public List<Variable> getDataVariables() {
      return leaf.cols;
    } // LOOK not right - can be at any level

    public Dimension getObsDimension() {
      return leaf.dim;
    }

    // stations
    public Dimension getStationDimension() {
      return leaf.parent.dim; // LOOK
    }

    public String getLinkedFirstRecVarName() {
       return useJoin.start.getName();
    }

    public String getLinkedNextRecVarName() {
       return useJoin.next.getName();
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("FlatTable = ");
      sbuff.append(leaf.getName());

      Table t = leaf;
      while (t.parent != null) {
        t = t.parent;
        sbuff.append("/");
        sbuff.append(t.getName());
      }
      sbuff.append("\n");

      sbuff.append(" Time= ").append(timeAxis).append("\n");
      sbuff.append(" Lat= ").append(latAxis).append("\n");
      sbuff.append(" Lon= ").append(lonAxis).append("\n");
      sbuff.append(" Height= ").append(heightAxis).append("\n");
      sbuff.append(" UseJoin= ").append(useJoin).append("\n");

      return sbuff.toString();
    }

  }

  private class Table {
    Dimension dim;
    List<Variable> cols;
    List<CoordinateAxis> coordVars = new ArrayList<CoordinateAxis>();
    Table parent;
    List<Join> children;

    Table(List<Variable> cols, Dimension dim) {
      this.dim = dim;
      this.cols = new ArrayList<Variable>();
      for (Variable v : cols) {
        if (v instanceof CoordinateAxis) {
          coordVars.add((CoordinateAxis) v);
        } else {
          this.cols.add(v);
        }
      }
    }

    public String getName() { return dim.getName(); }

    public CoordinateAxis findCoordinateAxis( AxisType axisType) {
      for (CoordinateAxis axis : coordVars) {
        if (axis.getAxisType() == axisType)
          return axis;
      }
      return null;
    }

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

  public enum JoinType {ContiguousList, ForwardLinkedList, BackwardLinkedList, ParentLink, MultiDim}

  private class Join implements Comparable<Join> {
    Table fromTable, toTable;
    JoinType joinType;
    Variable start, next;

    Join(JoinType joinType) {
      this.joinType = joinType;
    }

    void setTables(String fromName, String toName) {
      if (null != fromName)
        fromTable = tables.get(fromName);
      if (null != toName)
        toTable = tables.get(toName);
    }

    void setJoinVariables(Variable start, Variable next) {
      this.start = start;
      this.next = next;
    }

    public String toString() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append(joinType);
      if (fromTable != null)
        sbuff.append(" from ").append(fromTable.getName());
      if (toTable != null)
        sbuff.append(" to ").append(toTable.getName());

      return sbuff.toString();
    }

    public int compareTo(Join o) {
      return joinType.compareTo( o.joinType);
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
      if (isContiguousList) {
        join = new Join( JoinType.ContiguousList);
        join.setTables( firstVar.getDimension(0).getName(), obsDim.getName());
        join.setJoinVariables(firstVar, numChildrenVar);
        joins.add(join);
      }

      if (isForwardLinkedList) {
        join = new Join(JoinType.ForwardLinkedList);
        join.setTables( firstVar.getDimension(0).getName(), nextVar.getDimension(0).getName());
        join.setJoinVariables(firstVar, nextVar);
        joins.add(join);
      }

      if (isBackwardLinkedList) {
        join = new Join( JoinType.BackwardLinkedList);
        join.setTables( lastVar.getDimension(0).getName(), prevVar.getDimension(0).getName());
        join.setJoinVariables(lastVar, prevVar);
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

  ////////////////////////////////////////////////////////////////////////////////////////////////


  static void doit(String filename) throws IOException {
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    CoordSysAnalyzer csa = new CoordSysAnalyzer(ncd);
    csa.showAll();
    System.out.println("-----------------");
  }

  static public void main(String args[]) throws IOException {
    //doit("C:/data/dt2/station/ndbc.nc");
    //doit("C:/data/dt2/station/UnidataMultidim.ncml");
    doit("R:/testdata/point/bufr/data/050391800.iupt01");
    //doit("C:/data/rotatedPole/eu.mn.std.fc.d00z20070820.ncml");
    //doit("C:/data/dt2/station/madis2.sao.gz");
    //doit("C:/data/dt2/station/solrad_point_pearson.ncml");
  }
}
