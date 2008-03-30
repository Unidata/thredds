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

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt2.point.UnidataPointDatasetHelper;
import ucar.nc2.constants.FeatureType;

import java.util.*;
import java.io.IOException;

/**
 * @author caron
 * @since Mar 20, 2008
 */
public class CoordSysAnalyzer {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordSysAnalyzer.class);

  private NetcdfDataset ds;
  private Map<String, NestedTable.Table> tableFind = new HashMap<String, NestedTable.Table>();
  private Set<NestedTable.Table> tableSet = new HashSet<NestedTable.Table>();
  private List<NestedTable.Join> joins = new ArrayList<NestedTable.Join>();
  private List<NestedTable> leaves = new ArrayList<NestedTable>();
  private List<Attribute> atts = new ArrayList<Attribute>();

  public CoordSysAnalyzer(NetcdfDataset ds) {
    this.ds = ds;

    makeTables();

    // link the tables together with joins
    for (NestedTable.Join join : joins) {
      NestedTable.Table parent = join.fromTable;
      NestedTable.Table child = join.toTable;

      if (child.parent != null) throw new IllegalStateException("Multiple parents");
      child.parent = parent;
      child.join = join;

      if (parent.children == null) parent.children = new ArrayList<NestedTable.Join>();
      parent.children.add(join);
    }

    // find the leaves
    for (NestedTable.Table table : tableSet) {
      if (table.children == null) { // its a leaf
        NestedTable flatTable = new NestedTable(this, table);
        if (flatTable.isOk()) { // it has lat,lon,time coords
          leaves.add(flatTable);
        }
      }
    }
  }

  public List<NestedTable> getFlatTables() {
    return leaves;
  }

  public NetcdfDataset getNetcdfDataset() { return ds; }

  public String getParam(String name) {
    for (Attribute att : atts)  {
      if (att.getName().equals(name))
        return att.getStringValue();
    }
    return null;
  }

  /////////////////////////////////////////////////////////

  private void makeTables() {

    // for netcdf-3 files, convert record dimension to structure
    boolean structAdded = (Boolean) ds.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // make Structures into a table
    List<NestedTable.Table> structs = new ArrayList<NestedTable.Table>();
    List<Variable> vars = new ArrayList<Variable>(ds.getVariables());
    Iterator<Variable> iter = vars.iterator();
    while (iter.hasNext()) {
      Variable v = iter.next();
      if (v instanceof Structure) {  // handles Sequences too
        NestedTable.Table st = new NestedTable.Table((Structure) v);
        addTable(st);
        iter.remove();
        structs.add(st);

      } else if (structAdded && v.isUnlimited()) {
        iter.remove();
      }
    }

    // look for top level "psuedo structures"
    List<Dimension> dims = ds.getDimensions();
    for (Dimension dim : dims) {
      List<Variable> svars = getStructVars(vars, dim);
      if (vars.size() > 0) {
        NestedTable.Table dt = new NestedTable.Table(svars, dim);
        if ((dt.cols.size() > 0) || (dt.coordVars.size() > 0))
          addTable(dt);
      }
    }

    // look for nested structures
    findNestedStructures(structs);

    // annotate, join using conventions
    UnidataPointConvention up = new UnidataPointConvention();
    if (up.isMine(ds)) {
      up.annotate();

    } else {
      UnidataConvention u = new UnidataConvention();
      if (u.isMine(ds))
        u.annotate();
    }
  }

  private void addTable(NestedTable.Table t) {
    tableFind.put(t.getName(), t);
    tableFind.put(t.dim.getName(), t);
    tableSet.add( t);
  }

  private void findNestedStructures(List<NestedTable.Table> structs) {

    List<NestedTable.Table> nestedStructs = new ArrayList<NestedTable.Table>();
    for (NestedTable.Table structTable : structs) {
      Structure s = structTable.struct;
      for (Variable v : s.getVariables()) {
        if (v instanceof Structure) {  // handles Sequences too
          NestedTable.Table nestedTable = new NestedTable.Table((Structure) v);
          addTable( nestedTable);
          nestedStructs.add(nestedTable);

          NestedTable.Join join = new NestedTable.Join(NestedTable.JoinType.NestedStructure);
          join.setTables( structTable, nestedTable);
          joins.add(join);
        }
      }
    }

    // recurse
    if (nestedStructs.size() > 0)
      findNestedStructures(nestedStructs);
  }

  private List<Variable> getStructVars(List<Variable> vars, Dimension dim) {
    List<Variable> structVars = new ArrayList<Variable>();
    for (Variable v : vars) {
      if (v.isScalar()) continue;
      if (v.getDimension(0) == dim) {
        structVars.add(v);
      }
    }
    return structVars;
  }

    private class UnidataConvention {

      public boolean isMine(NetcdfDataset ds) {
        // find datatype
        String datatype = ds.findAttValueIgnoreCase(null, "cdm_datatype", null);
        if (datatype == null)
          return false;
        if (!datatype.equalsIgnoreCase(FeatureType.STATION_PROFILE.toString()))
          return false;

        String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
        if (conv == null) return false;

        StringTokenizer stoke = new StringTokenizer(conv, ",");
        while (stoke.hasMoreTokens()) {
          String toke = stoke.nextToken().trim();
          if (toke.equalsIgnoreCase("Unidata"))
            return true;
        }
        return false;
      }

      void annotate() {
        atts.add(new Attribute("station_id", "stationName"));
        atts.add(new Attribute("station_npts", "nrecords"));
      }
    }

  private class UnidataPointConvention {

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

    void annotate() {
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
      NestedTable.Table stnTable = tableFind.get(stationDim.getName());

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

    void setTables(NestedTable.Join join, String fromName, String toName) {
      NestedTable.Table fromTable = null, toTable = null;
      fromTable = tableFind.get(fromName);
      assert fromTable != null : "cant find "+fromName;
      toTable = tableFind.get(toName);
      assert toTable != null : "cant find "+toName;      
      join.setTables(fromTable, toTable);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  public void showCoordSys(java.util.Formatter sf) {
    sf.format("\nCoordinate Systems\n");
    for (CoordinateSystem cs : ds.getCoordinateSystems()) {
      sf.format(" %s\n", cs);
    }
  }

  public void showCoordAxes(java.util.Formatter sf) {
    sf.format("\nAxes\n");
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      sf.format(" %s %s\n", axis.getAxisType(), axis.getNameAndDimensions());
    }
  }

  public void showTables(java.util.Formatter sf) {
    sf.format("\nTables\n");
    for (NestedTable.Table t : tableSet)
      sf.format(" %s\n", t);

    sf.format("\nJoins\n");
    for (NestedTable.Join j : joins)
      sf.format(" %s\n", j);
  }

  public void showNestedTables(java.util.Formatter sf) {
    sf.format("\nNestedTables");
    for (NestedTable t : leaves)
      sf.format(" %s\n", t);
  }

  public void getDetailInfo(java.util.Formatter sf) {
    sf.format("\nCoordSysAnalyzer on Dataset %s\n", ds.getLocation());
    showCoordSys(sf);
    showCoordAxes(sf);
    showTables(sf);
    showNestedTables(sf);
  }
  
  static void doit(String filename) throws IOException {
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    CoordSysAnalyzer csa = new CoordSysAnalyzer(ncd);
    // System.out.println(ncd);
    csa.getDetailInfo(new Formatter(System.out));
    System.out.println("-----------------");
  }

  static public void main(String args[]) throws IOException {
    //doit("C:/data/dt2/station/ndbc.nc");
    //doit("C:/data/dt2/station/UnidataMultidim.ncml");
    //doit("R:/testdata/point/bufr/data/050391800.iupt01");
    //doit("C:/data/rotatedPole/eu.mn.std.fc.d00z20070820.ncml");
    //doit("C:/data/cadis/tempting");
    //doit("C:/data/dt2/station/Surface_METAR_20080205_0000.nc");
    doit("C:/data/dt2/profile/PROFILER_1.bufr");
  }
}
