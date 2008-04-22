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
package ucar.nc2.ft.point.standard;

import ucar.nc2.*;
import ucar.nc2.ft.EarthLocation;
import ucar.nc2.ft.EarthLocationImpl;
import ucar.nc2.ft.Station;
import ucar.nc2.ft.StationImpl;
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

  private TableAnalyzer cs;
  private Table leaf;

  private CoordVarExtractor timeVE, latVE, lonVE, altVE;
  private int nestedLevels;

  private DateFormatter dateFormatter = new DateFormatter();

  // A Nested Table is created after the Tables have been joined, and the leaves identified.
  NestedTable(TableAnalyzer cs, Table leaf) {
    this.cs = cs;
    this.leaf = leaf;

    // will find the first one, starting at the leaf and going up
    timeVE = findCoordinateAxis(AxisType.Time, leaf, 0);
    latVE = findCoordinateAxis(AxisType.Lat, leaf, 0);
    lonVE = findCoordinateAxis(AxisType.Lon, leaf, 0);
    altVE = findCoordinateAxis(AxisType.Height, leaf, 0);

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

    double getCoordValue(List<StructureData> structList) {
      StructureData struct = structList.get( nestingLevel);
      StructureMembers members = struct.getStructureMembers();
      StructureMembers.Member m = members.findMember(axis.getShortName());
      return struct.convertScalarDouble( m);
    }

    String getCoordValueString(List<StructureData> structList) {
      StructureData struct = structList.get( nestingLevel);
      StructureMembers members = struct.getStructureMembers();
      StructureMembers.Member m = members.findMember(axis.getShortName());
      return struct.getScalarString( m);
    }

    public String toString() { return axis.getName()+" tableIndex= "+ nestingLevel; }
  }

  public FeatureType getFeatureType() {
    if (cs.ft != null) return cs.ft;

    // bogus
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

  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> data = new ArrayList<VariableSimpleIF>();
    addDataVariables(data, leaf);
    return data;
  }

  // use recursion so that parent variables come first
  private void addDataVariables(List<VariableSimpleIF> list, Table t) {
    if (t.parent != null) addDataVariables( list, t.parent);
    list.addAll(t.cols);
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
    return leaf.getStructureDataIterator(bufferSize);
  }

  //Station
  public StructureDataIterator getStationDataIterator(int bufferSize) throws IOException {
    if (getFeatureType() == FeatureType.STATION)
       return leaf.parent.getStructureDataIterator( bufferSize);

    if (getFeatureType() == FeatureType.STATION_PROFILE)
       return leaf.parent.parent.getStructureDataIterator(bufferSize);

    throw new UnsupportedOperationException("Not a StationFeatureCollection or StationProfileFeatureCollection");
  }

  public StructureDataIterator getStationObsDataIterator(StructureData stationData, int bufferSize) throws IOException {
    return leaf.join.getStructureDataIterator(stationData, bufferSize);
  }

  // Station Profile
  public StructureDataIterator getStationProfileDataIterator(StructureData stationData, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    return leaf.parent.join.getStructureDataIterator(stationData, bufferSize);
  }

  // ft.getStationProfileObsDataIterator(sdataList, bufferSize)
  public StructureDataIterator getStationProfileObsDataIterator(List<StructureData> structList, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    StructureData profileData = structList.get(1);
    return leaf.join.getStructureDataIterator(profileData, bufferSize);
  }

  /* get the StructureDataIterator for the child table contained by the given parent
  private StructureDataIterator join(StructureData parentStruct, Table child) throws IOException {

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

      case MultiDim: {
        ArrayStructureMA asma = new ArrayStructureMA( child.sm, new int[] {child.dim.getLength()});
        for (VariableSimpleIF v : child.cols) {
          Array data = parentStruct.getArray( v.getShortName());
          StructureMembers.Member childm = child.sm.findMember(v.getShortName());
          childm.setDataArray(data);
        }

        return asma.getStructureDataIterator();
      }

      case Index: {
        
      }

    }

    throw new IllegalStateException("Join type = "+child.join.joinType);
  } */

  ///////////////////////////////////////////////////////////////////////////////

  public double getTime(List<StructureData> structList) {

    if ((timeVE.axis.getDataType() == ucar.ma2.DataType.CHAR) || (timeVE.axis.getDataType() == ucar.ma2.DataType.STRING)) {
      String timeString = timeVE.getCoordValueString(structList);
      Date date;
      try {
        date = dateFormatter.isoDateTimeFormat(timeString);
      } catch (ParseException e) {
        log.error("Cant parse date - not ISO formatted, = " + timeString);
        return 0.0;
      }
      return date.getTime() / 1000.0;

    } else {
      return timeVE.getCoordValue(structList);
    }
  }

  public EarthLocation getEarthLocation(List<StructureData> structList) {
    double lat = latVE.getCoordValue(structList);
    double lon = lonVE.getCoordValue(structList);
    double alt = (altVE == null) ? 0.0 : altVE.getCoordValue(structList);
    return new EarthLocationImpl(lat, lon, alt);
  }

  public Station makeStation(StructureData stationData) {
    // LOOK this assumes that the data is in the Station structure - one may have to read an obs to get it
    TableAnalyzer.StationInfo info = cs.getStationInfo();

    String stationName = getCoordValueString(stationData, info.stationId);
    String stationDesc = (info.stationDesc == null) ? stationName : getCoordValueString(stationData, info.stationDesc);
    double lat = getCoordValue(stationData, info.latName);
    double lon = getCoordValue(stationData, info.lonName);
    double alt = (info.elevName == null) ? Double.NaN : getCoordValue(stationData, info.elevName);

    return new StationImpl(stationName, stationDesc, lat, lon, alt);
  }

  double getCoordValue(StructureData struct, String memberName) {
    StructureMembers members = struct.getStructureMembers();
    StructureMembers.Member m = members.findMember( memberName);
    if (m == null)
      System.out.println("HEY");
    return struct.convertScalarDouble( m);
  }

  String getCoordValueString(StructureData struct, String memberName) {
    StructureMembers members = struct.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);
    if ((m.getDataType() == DataType.CHAR) || (m.getDataType() == DataType.STRING))
      return struct.getScalarString( m);
    else {
      int no = (int) struct.convertScalarDouble( m);
      return "#"+no;
    }
  }

  public StructureData makeObsStructureData(List<StructureData> structList) {
    return structList.get(0);
  }


  ///////////////////////////////////////////////////////////////////////////////////////////

  public enum TableType {
    Structure, PseudoStructure, ArrayStructure, MultiDim
  }

  /**
   * A generalization of a Structure. We search among all the possible Tables in a dataset for joins, and coordinate
   * variables. Based on those, we form "interesting" sets and make them into NestedTables.
   */
  public static class Table {
    TableType type;
    String name;
    Dimension dim;       // common dimension

    List<VariableSimpleIF> cols = new ArrayList<VariableSimpleIF>();    // all variables
    List<CoordinateAxis> coordVars = new ArrayList<CoordinateAxis>(); // just the coord axes

    Structure struct;    // Structure, PseudoStructure
    ArrayStructure as;   // ArrayStructure
    StructureMembers sm; // MultiDim

    Table parent;
    Join join; // the join to its parent
    List<Join> children;

    // make a table based on a Structure
    public Table(Structure struct) {
      this.struct = struct;
      this.type = TableType.Structure;
      this.name = struct.getShortName();

      this.dim = struct.getDimension(0);

      for (Variable v : struct.getVariables()) {
        if (v instanceof CoordinateAxis)
          coordVars.add((CoordinateAxis) v);

        this.cols.add(v);
      }
    }

    // make a table based on a given list of variables with a common outer dimension
    public Table(NetcdfDataset ds, List<Variable> cols, Dimension dim) {
      this.type = TableType.PseudoStructure;
      this.dim = dim;
      this.name = dim.getName();

      for (Variable v : cols) {
        if (v instanceof CoordinateAxis)
          coordVars.add((CoordinateAxis) v);
        this.cols.add(v);
      }
      struct = new StructurePseudo(ds, null, getName(), cols, dim);
    }

    // make a table based on a given list of variables with 2 common dimensions
    public Table(List<Variable> cols, Dimension outer, Dimension inner) {
      this.type = TableType.MultiDim;
      this.dim = inner;
      this.name = inner.getName();

      sm = new StructureMembers(name);
      for (Variable v : cols) {
        if (v instanceof CoordinateAxis)
          coordVars.add((CoordinateAxis) v);
        this.cols.add(v);

        int rank = v.getRank();
        int[] shape = new int[rank-2];
        System.arraycopy(v.getShape(), 2, shape, 0, rank-2);
        sm.addMember( v.getShortName(), v.getDescription(),  v.getUnitsString(), v.getDataType(), shape);
      }
    }

    // make a table based on memory resident ArrayStructure
    public Table(String name, ArrayStructure as) {
      this.type = TableType.ArrayStructure;
      this.as = as;
      this.name = name;
      this.dim = new Dimension(name, (int) as.getSize(), false);
      for (StructureMembers.Member m : as.getStructureMembers().getMembers()) {
        cols.add( new VariableSimpleAdapter(m)); 
      }
    }

    StructureDataIterator getStructureDataIterator(int bufferSize) throws IOException {

      switch (type) {
        case Structure:
        case PseudoStructure:
          return struct.getStructureIterator(bufferSize);

        case ArrayStructure:
          return as.getStructureDataIterator();
      }

      throw new IllegalStateException("Table type = "+type);
    }

    public String getName() {
      return name;
    }

    public Structure getStruct() {
      return struct;
    }

    public CoordinateAxis findCoordinateAxis(AxisType axisType) {
      for (CoordinateAxis axis : coordVars) {
        if (axis.getAxisType() == axisType)
          return axis;
      }
      return null;
    }

    public List<VariableSimpleIF> getDataVariables() { return cols; }

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
      for (VariableSimpleIF v : cols) {
        sbuff.append("  ").append(v.getName()).append("\n");
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
      for (VariableSimpleIF v : cols) {
        if (!(v instanceof CoordinateAxis))
          f.format("%s  %s \n", s, v.getName());
      }
      return indent + 2;
    }
    String indent(int n) {
      StringBuffer sbuff = new StringBuffer();
      for (int i=0; i<n; i++) sbuff.append(' ');
      return sbuff.toString();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////


}
