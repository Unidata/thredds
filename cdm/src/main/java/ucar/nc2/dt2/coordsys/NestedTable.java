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

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.StructurePseudo;
import ucar.nc2.dt2.EarthLocation;
import ucar.nc2.dt2.EarthLocationImpl;
import ucar.nc2.dt2.Station;
import ucar.nc2.dt2.StationImpl;
import ucar.nc2.dt2.point.StructureDataIteratorLinked;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;
import java.text.ParseException;

/**
 * Implements "nested table" views of point feature datasets.
 * <p>
 * A nested (aka flattened) table starts with a leaf table (no children), plus all of its parents.
 * There is a "join" for each child and parent.
 * <p>
 * Assumes that we have Tables that can be iterated over with a StructureDataIterator.
 * A parent-child join assumes that for each row of the parent, a StructureDataIterator exists that
 *  iterates over the rows of the child table for that parent.
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class NestedTable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NestedTable.class);

  private CoordSysAnalyzer cs;
  private Table leaf;

  private CoordVarExtractor timeVE, latVE, lonVE, altVE;
  private int nestedLevels;

  private DateFormatter dateFormatter = new DateFormatter();

  NestedTable(CoordSysAnalyzer cs, Table leaf) {
    this.cs = cs;
    this.leaf = leaf;

    timeVE = findCoordinateAxis(AxisType.Time, leaf, 0);
    latVE = findCoordinateAxis(AxisType.Lat, leaf, 0);
    lonVE = findCoordinateAxis(AxisType.Lon, leaf, 0);
    altVE = findCoordinateAxis(AxisType.Height, leaf, 0);

    // count nesting
    nestedLevels = 1;
    Table t = leaf;
    while (t.parent != null) {
      t = t.parent;
      nestedLevels++;
    }
  }

  Table getTopParent() {
    Table p = leaf;
    while (p.parent != null) p = p.parent;
    return p;
  }

  // look for a coord axis of the given type in the table and its parents
  private CoordVarExtractor findCoordinateAxis(AxisType axisType, Table t, int nestingLevel) {
    if (t == null) return null;

    CoordinateAxis axis = t.findCoordinateAxis(axisType);
    if (axis != null) {
      CoordVarExtractor ve = new CoordVarExtractor();
      ve.axis = axis;
      ve.nestingLevel = nestingLevel;
      return ve;
    }

    // look in the parent
    return findCoordinateAxis(axisType, t.parent, nestingLevel+1);
  }

  // knows how to get specific coordinate data from a table or its parents
  private class CoordVarExtractor {
    CoordinateAxis axis;
    int nestingLevel;       // leaf == 0, each parent adds one
    StructureMembers.Member member;

    void findMember(List<StructureData> structList) {
      if (axis == null) return;

      StructureData sdata = structList.get(nestingLevel);
      StructureMembers members = sdata.getStructureMembers();
      member = members.findMember(axis.getShortName());
      if (member == null)
        throw new IllegalStateException("cant find "+axis.getShortName());
    }

    public String toString() { return axis.getName()+" tableIndex= "+ nestingLevel; }
  }

  // bogus
  public FeatureType getFeatureType() {
    if (nestedLevels == 1) return FeatureType.POINT;
    if (nestedLevels == 2) return FeatureType.STATION;
    if (nestedLevels == 3) return FeatureType.STATION_PROFILE;
    return null;
  }

  public boolean isOk() {
    return (timeVE != null) && (latVE != null) && (lonVE != null);
  }

  public DateUnit getTimeUnit() {
    try {
      return new DateUnit( timeVE.axis.getUnitsString());
    } catch (Exception e) {
      throw new IllegalArgumentException("Error on time string = " + timeVE.axis.getUnitsString() + " == " + e.getMessage());
    }
  }

  public List<Variable> getDataVariables() {
    return leaf.cols;
  } // LOOK not right - can be at any level

  public String getName() {
    Formatter formatter = new Formatter();
    formatter.format("%s", leaf.getName());

    Table t = leaf;
    while (t.parent != null) {
      t = t.parent;
      formatter.format("/%s", t.getName());
    }
    return formatter.toString();
  }

  public String toString() {
    Formatter formatter = new Formatter();
    formatter.format("NestedTable = %s\n", getName());
    formatter.format("  Time= %s\n", timeVE);
    formatter.format("  Lat= %s\n", latVE);
    formatter.format("  Lon= %s\n", lonVE);
    formatter.format("  Height= %s\n", altVE);

    return formatter.toString();
  }

  public void show(Formatter formatter) {
    formatter.format("NestedTable = %s\n", getName());
    leaf.show(formatter, 2);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // not clear these methods should be here

  // Point
  public StructureDataIterator getObsDataIterator(int bufferSize) throws IOException {
    return getStructureDataIterator(leaf, bufferSize);
  }

  private StructureDataIterator getStructureDataIterator(Table table, int bufferSize) throws IOException {

    switch (table.type) {
      case Structure:
        return table.getStructureDataIterator(bufferSize);

      case PseudoStructure: {
        StructurePseudo sp = new StructurePseudo(cs.getNetcdfDataset(), null, getName(), table.dim);
        return sp.getStructureIterator(bufferSize);
      }
    }
    return null;
  }

  //Station
  public StructureDataIterator getStationDataIterator(int bufferSize) throws IOException {
    if (getFeatureType() == FeatureType.STATION)
       return getStructureDataIterator(leaf.parent, bufferSize);

    if (getFeatureType() == FeatureType.STATION_PROFILE)
       return getStructureDataIterator(leaf.parent.parent, bufferSize);

    throw new UnsupportedOperationException("Not a StationFeatureCollection or StationProfileFeatureCollection");
  }

  public StructureDataIterator getStationObsDataIterator(StructureData stationData, int bufferSize) throws IOException {
    return getStructureDataIterator(leaf, stationData);
  }

  // Station Profile
  public StructureDataIterator getStationProfileDataIterator(StructureData stationData, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    return getStructureDataIterator(leaf.parent, stationData);
  }

  // ft.getStationProfileObsDataIterator(sdataList, bufferSize)
  public StructureDataIterator getStationProfileObsDataIterator(List<StructureData> structList, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    StructureData profileData = structList.get(1);
    return getStructureDataIterator(leaf, profileData);
  }

  private StructureDataIterator getStructureDataIterator(Table child, StructureData parentStruct) throws IOException {

    switch (child.join.joinType) {
      case NestedStructure: {
        String name = child.getName();
        StructureMembers members = parentStruct.getStructureMembers();
        StructureMembers.Member m = members.findMember(name);
        if (m.getDataType() == DataType.SEQUENCE) {
          ArraySequence2 seq = parentStruct.getArraySequence(m);
          return seq.getStructureDataIterator();

        } else if (m.getDataType() == DataType.STRUCTURE) {
          ArrayStructure as = parentStruct.getArrayStructure(m);
          return as.getStructureDataIterator();
        }

        return null;
      }

      case ForwardLinkedList:
      case BackwardLinkedList: {
        int firstRecno = parentStruct.getScalarInt( child.join.start.getName());
        return new StructureDataIteratorLinked(child.struct, firstRecno, -1, child.join.next.getName());
      }

      case ContiguousList: {
        int firstRecno = parentStruct.getScalarInt( child.join.start.getName());
        int numRecords = parentStruct.getScalarInt( child.join.numRecords.getName());
        return new StructureDataIteratorLinked(child.struct, firstRecno, numRecords, null);
      }
    }

    return null;
  }

  ///////////////////////////////////////////////////////////////////////////////

  //private StructureMembers.Member timeMember, latMember, lonMember, altMember;

  public double getTime(List<StructureData> structList) {

    if (timeVE.member == null)
      timeVE.findMember(structList);

    StructureData obsData = structList.get( timeVE.nestingLevel);

    if ((timeVE.member.getDataType() == ucar.ma2.DataType.CHAR) || (timeVE.member.getDataType() == ucar.ma2.DataType.STRING)) {
      String timeString = obsData.getScalarString(timeVE.member);
      Date date;
      try {
        date = dateFormatter.isoDateTimeFormat(timeString);
      } catch (ParseException e) {
        log.error("Cant parse date - not ISO formatted, = " + timeString);
        return 0.0;
      }
      return date.getTime() / 1000.0;

    } else {
      return obsData.convertScalarDouble(timeVE.member);
    }
  }


  public EarthLocation getEarthLocation(List<StructureData> structList) {
    StructureData obsData = structList.get(0); // LOOK - assumes that lat, lon, alt is member of obsData

    if (latVE.member == null) {
      latVE.findMember(structList);
      lonVE.findMember(structList);
      if (altVE != null)
        altVE.findMember(structList);
    }

    double lat = obsData.convertScalarDouble(latVE.member);
    double lon = obsData.convertScalarDouble(lonVE.member);
    double alt = (altVE == null) ? 0.0 : obsData.convertScalarDouble(altVE.member);

    return new EarthLocationImpl(lat, lon, alt);
  }

  private StructureMembers.Member stnNameMember, stnDescMember;
  public Station makeStation(StructureData stationData) {
    // LOOK - assumes that lat, lon, alt, stnid, stddesc is member of stationData

    if (latVE.member == null) {
      StructureMembers members = stationData.getStructureMembers();
      latVE.member = members.findMember(latVE.axis.getShortName());
      lonVE.member = members.findMember(lonVE.axis.getShortName());
      if (altVE != null)
        altVE.member = members.findMember(altVE.axis.getShortName());
    }

    if (stnNameMember == null) {
      StructureMembers members = stationData.getStructureMembers();
      stnNameMember = members.findMember(cs.getParam("station_id"));
      stnDescMember = members.findMember(cs.getParam("station_desc"));
    }

    String stationName = stationData.getScalarString(stnNameMember);
    String stationDesc = (stnDescMember == null) ? stationName : stationData.getScalarString(stnDescMember);
    double lat = stationData.convertScalarDouble(latVE.member);
    double lon = stationData.convertScalarDouble(lonVE.member);
    double alt = (altVE.member == null) ? 0.0 : stationData.convertScalarDouble(altVE.member);

    return new StationImpl(stationName, stationDesc, lat, lon, alt);
  }

  public StructureData makeObsStructureData(List<StructureData> structList) {
    return structList.get(0);
  }


  ///////////////////////////////////////////////////////////////////////////////////////////

  public enum TableType {
    Structure, PseudoStructure, ArrayStructure
  }

  /**
   * A generalization of a Structure. We search among all the possible Tables in a dataset for joins, and coordinate
   * variables. Based on those we form "interesting" sets and make them into NestedTables.
   */
  public static class Table {
    TableType type;
    Dimension dim;          // common outer dimension
    String name;

    List<Variable> cols = new ArrayList<Variable>();    // data variables
    List<CoordinateAxis> coordVars = new ArrayList<CoordinateAxis>(); // coord axes
    Structure struct;
    ArrayStructure as;

    Table parent;
    Join join; // the join to its parent
    List<Join> children;

    Table() {
    }

    // make a table based on a Structure
    Table(Structure struct) {
      this.struct = struct;
      this.type = TableType.Structure;
      this.name = struct.getShortName();

      this.dim = struct.getDimension(0);

      for (Variable v : struct.getVariables()) {
        if (v instanceof CoordinateAxis) {
          coordVars.add((CoordinateAxis) v);
        } else {
          this.cols.add(v);
        }
      }
    }

    // make a table based on a variables with same outer dimension
    Table(NetcdfDataset ds, List<Variable> cols, Dimension dim) {
      this.type = TableType.PseudoStructure;
      this.dim = dim;
      this.name = dim.getName();

      for (Variable v : cols) {
        if (v instanceof CoordinateAxis) {
          coordVars.add((CoordinateAxis) v);
        } else {
          this.cols.add(v);
        }
      }

      struct = new StructurePseudo(ds, null, getName(), dim);
    }

    // make a table based on memory resident ArrayStructure
    Table(String name, ArrayStructure as) {
      this.type = TableType.ArrayStructure;
      this.as = as;
      this.name = name;
    }

    StructureDataIterator getStructureDataIterator(int bufferSize) throws IOException {

      switch (type) {
        case Structure:
        case PseudoStructure:
          return struct.getStructureIterator(bufferSize);

        case ArrayStructure:
          return as.getStructureDataIterator();
      }

      return null;
    }

    public String getName() {
      return name;
    }

    public CoordinateAxis findCoordinateAxis(AxisType axisType) {
      for (CoordinateAxis axis : coordVars) {
        if (axis.getAxisType() == axisType)
          return axis;
      }
      return null;
    }

    public String toString() {
      Formatter formatter = new Formatter();
      formatter.format(" Table %s on dimension %s type=%s\n", getName(), (dim == null) ? "null" : dim.getName(), type);
      formatter.format("  Coordinates=");
      for (CoordinateAxis axis : coordVars)
        formatter.format(" %s (%s)", axis.getName(), axis.getAxisType());
      formatter.format("\n  Data Variables= %d\n", cols.size());
      formatter.format("  Parent= %s join = %s\n", ((parent == null) ? "none" : parent.getName()), join);
      return formatter.toString();
    }

    public String showAll() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("Table on dimension ").append(dim.getName()).append("\n");
      for (CoordinateAxis axis : coordVars) {
        sbuff.append(" Coordinate= ").append(axis.getAxisType()).append(": ").append(axis.getNameAndDimensions()).append("\n");
      }
      for (Variable v : cols) {
        sbuff.append("  ").append(v.getNameAndDimensions()).append("\n");
      }
      return sbuff.toString();
    }

    public int show(Formatter f, int indent) {
      if (parent != null) {
        indent = parent.show(f, indent);
      }

      String s = indent(indent);
      String joinDesc = (join == null) ? "" : "joinType=" + join.joinType.toString();
      String dimDesc = (dim == null) ? "*" : dim.getName()+"="+dim.getLength() + (dim.isUnlimited() ? " unlim" : "");
      f.format("\n%sTable %s: type=%s(%s) %s\n", s, getName(),type, dimDesc, joinDesc);
      for (CoordinateAxis axis : coordVars)
        f.format("%s  %s [%s]\n", s, axis.getNameAndDimensions(), axis.getAxisType());
      for (Variable v : cols)
        f.format("%s  %s \n", s, v.getNameAndDimensions());
      return indent + 2;
    }
    String indent(int n) {
      StringBuffer sbuff = new StringBuffer();
      for (int i=0; i<n; i++) sbuff.append(' ');
      return sbuff.toString();
    }
  }

  public enum JoinType {
    ContiguousList, ForwardLinkedList, BackwardLinkedList, ParentLink, MultiDim, NestedStructure, Identity
  }

  public static class Join implements Comparable<Join> {
    Table fromTable, toTable;
    JoinType joinType;
    Variable start, next, numRecords;  // for linked and contiguous lists

    Join(JoinType joinType) {
      this.joinType = joinType;
    }

    void setTables(Table parent, Table child) {
      assert parent != null;
      assert child != null;
      this.fromTable = parent;
      this.toTable = child;
    }

    // for linked/contiguous lists
    void setJoinVariables(Variable start, Variable next, Variable numRecords) {
      this.start = start;
      this.next = next;
      this.numRecords = numRecords;
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
      return joinType.compareTo(o.joinType);
    }
  }
}
