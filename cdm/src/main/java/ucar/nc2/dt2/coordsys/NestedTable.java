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
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureData;
import ucar.ma2.ArraySequence2;

import java.util.*;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author caron
 * @since Mar 28, 2008
 */
public class NestedTable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NestedTable.class);

  private CoordSysAnalyzer cs;
  private Table leaf;

  private boolean isOk;
  private VarExtractor timeVE, latVE, lonVE, altVE;
  private int nestedLevels;
  private Join useJoin;

  private DateFormatter dateFormatter = new DateFormatter();


  NestedTable(CoordSysAnalyzer cs, Table leaf) {
    this.cs = cs;
    this.leaf = leaf;

    timeVE = findCoordinateAxis(AxisType.Time, leaf, 0);
    latVE = findCoordinateAxis(AxisType.Lat, leaf, 0);
    lonVE = findCoordinateAxis(AxisType.Lon, leaf, 0);
    altVE = findCoordinateAxis(AxisType.Height, leaf, 0);
    isOk = (timeVE != null) && (latVE != null) && (lonVE != null);

    // count nesting
    nestedLevels = 1;
    Table t = leaf;
    while (t.parent != null) {
      t = t.parent;
      nestedLevels++;
    }
  }

  private class VarExtractor {
    CoordinateAxis axis;
    int tableIndex;
    StructureMembers.Member member;

    void findMember(List<StructureData> structList) {
      if (axis == null) return;

      StructureData sdata = structList.get( tableIndex);
      StructureMembers members = sdata.getStructureMembers();
      member = members.findMember(axis.getShortName());
    }

    public String toString() { return axis.getName()+" tableIndex= "+tableIndex; }
  }

  private VarExtractor findCoordinateAxis(AxisType axisType, Table t, int tableIndex) {
    if (t == null) return null;

    CoordinateAxis axis = t.findCoordinateAxis(axisType);
    if (axis != null) {
     VarExtractor ve = new VarExtractor();
      ve.axis = axis;
      ve.tableIndex = tableIndex;
      return ve;
    }
    return findCoordinateAxis(axisType, t.parent, tableIndex+1);
  }

  public FeatureType getFeatureType() {
    if (nestedLevels == 1) return FeatureType.POINT;
    if (nestedLevels == 2) return FeatureType.STATION;
    if (nestedLevels == 3) return FeatureType.STATION_PROFILE;

    Table parent = leaf.parent;
    if (parent.children.size() > 0) {
      Collections.sort(parent.children);
      useJoin = parent.children.get(0);
    }

    if (useJoin != null)
      return FeatureType.STATION;

    return null;
  }

  public boolean isOk() {
    return isOk;
  }

  public DateUnit getTimeUnit() {
    try {
      return new DateUnit(timeVE.axis.getUnitsString());
    } catch (Exception e) {
      throw new IllegalArgumentException("Error on time string = " + timeVE.axis.getUnitsString() + " == " + e.getMessage());
    }
  }

  public List<Variable> getDataVariables() {
    return leaf.cols;
  } // LOOK not right - can be at any level

  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  // Point
  public StructureDataIterator getObsDataIterator(int bufferSize) throws IOException {
    return getStructureDataIterator(leaf, bufferSize);
  }

  private StructureDataIterator getStructureDataIterator(Table table, int bufferSize) throws IOException {
    switch (table.type) {
      case Structure:
        return table.struct.getStructureIterator(bufferSize);

      case PseudoStructure: {
        StructurePseudo sp = new StructurePseudo(cs.getNetcdfDataset(), null, getName(), table.dim);
        return sp.getStructureIterator(bufferSize);
      }
    }
    return null;
  }

  //Station
  private String getLinkedFirstRecVarName() {
    return useJoin.start.getName();
  }

  private String getLinkedNextRecVarName() {
    return useJoin.next.getName();
  }

  public StructureDataIterator getStationDataIterator(int bufferSize) throws IOException {
    if (getFeatureType() == FeatureType.STATION)
       return getStructureDataIterator(leaf.parent, bufferSize);

    if (getFeatureType() == FeatureType.STATION_PROFILE)
       return getStructureDataIterator(leaf.parent.parent, bufferSize);

    throw new UnsupportedOperationException("Not a StationFeatureCollection or StationProfileFeatureCollection");
  }

  public StructureDataIterator getStationObsDataIterator(StructureData stationData, int bufferSize) throws IOException {
    // temp kludge
    int firstRecno = stationData.getScalarInt( getLinkedFirstRecVarName());
    String linkVarName = getLinkedNextRecVarName();
    return new StructureDataIteratorLinked(leaf.struct, firstRecno, -1, linkVarName);
  }

  // Station Profile
  public StructureDataIterator getStationProfileDataIterator(StructureData stationData, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    String name = leaf.parent.getName();
    StructureMembers members = stationData.getStructureMembers();
    ArraySequence2 seq = stationData.getArraySequence(members.findMember(name));
    return seq.getStructureIterator();
  }

  // ft.getStationProfileObsDataIterator(sdataList, bufferSize)
  public StructureDataIterator getStationProfileObsDataIterator(List<StructureData> structList, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    StructureData profileData = structList.get(1);
    String name = leaf.getName();
    StructureMembers members = profileData.getStructureMembers();
    ArraySequence2 seq = profileData.getArraySequence(members.findMember(name));
    return seq.getStructureIterator();
  }

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
    formatter.format("  UseJoin= %s\n", useJoin);

    return formatter.toString();
  }

  ///////////////////////////////////////////////////////////////////////////////

  //private StructureMembers.Member timeMember, latMember, lonMember, altMember;

  public double getTime(List<StructureData> structList) {

    if (timeVE.member == null)
      timeVE.findMember(structList);

    StructureData obsData = structList.get(timeVE.tableIndex);

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
      altVE.findMember(structList);
      latVE.findMember(structList);
    }

    double lat = obsData.convertScalarDouble(latVE.member);
    double lon = obsData.convertScalarDouble(lonVE.member);
    double alt = (altVE.member == null) ? 0.0 : obsData.convertScalarDouble(altVE.member);

    return new EarthLocationImpl(lat, lon, alt);
  }

  private StructureMembers.Member stnNameMember, stnDescMember;
  public Station makeStation(StructureData stationData) {
    // LOOK - assumes that lat, lon, alt, stnid, stddesc is member of stationData

    if (latVE.member == null) {
      StructureMembers members = stationData.getStructureMembers();
      latVE.member = members.findMember(latVE.axis.getShortName());
      lonVE.member = members.findMember(lonVE.axis.getShortName());
      altVE.member = (altVE.axis == null) ? null : members.findMember(altVE.axis.getShortName());

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
    Structure, PseudoStructure
  }

  public static class Table {
    private TableType type;
    Dimension dim;
    List<Variable> cols;
    List<CoordinateAxis> coordVars = new ArrayList<CoordinateAxis>();
    Structure struct;
    private String name;

    Table parent;
    List<Join> children;

    Table(Structure struct) {
      this.struct = struct;
      this.type = TableType.Structure;
      this.name = struct.getShortName();

      this.dim = struct.getDimension(0);
      this.cols = new ArrayList<Variable>();
      for (Variable v : struct.getVariables()) {
        if (v instanceof CoordinateAxis) {
          coordVars.add((CoordinateAxis) v);
        } else {
          this.cols.add(v);
        }
      }
    }

    Table(List<Variable> cols, Dimension dim) {
      this.type = TableType.PseudoStructure;
      this.dim = dim;
      this.name = dim.getName();

      this.cols = new ArrayList<Variable>();
      for (Variable v : cols) {
        if (v instanceof CoordinateAxis) {
          coordVars.add((CoordinateAxis) v);
        } else {
          this.cols.add(v);
        }
      }
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
      formatter.format(" Table on dimension %s type=%s\n", dim.getName(), type);
      formatter.format("  Coordinates=");
      for (CoordinateAxis axis : coordVars)
        formatter.format(" %s (%s)", axis.getName(), axis.getAxisType());
      formatter.format("\n  Data Variables= %d\n", cols.size());
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
  }

  public enum JoinType {
    ContiguousList, ForwardLinkedList, BackwardLinkedList, ParentLink, MultiDim, NestedStructure
  }

  public static class Join implements Comparable<Join> {
    Table fromTable, toTable;
    private JoinType joinType;
    private Variable start, next;

    Join(JoinType joinType) {
      this.joinType = joinType;
    }

    void setTables(Table fromTable, Table toTable) {
      assert fromTable != null;
      assert toTable != null;
      this.fromTable = fromTable;
      this.toTable = toTable;
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
      return joinType.compareTo(o.joinType);
    }
  }
}
