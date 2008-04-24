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
import ucar.nc2.ft.point.StructureDataIteratorLimited;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;
import java.text.ParseException;

/**
 * Implements "nested table" views of point feature datasets.
 * <p/>
 * A nested (aka flattened) table starts with a leaf table (no children), plus all of its parents.
 * There is a "join" for each child and parent.
 * <p/>
 * Assumes that we have Tables that can be iterated over with a StructureDataIterator.
 * A parent-child join assumes that for each row of the parent, a StructureDataIterator exists that
 * iterates over the rows of the child table for that parent.
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class NestedTable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NestedTable.class);

  private NetcdfDataset ds;
  private Formatter errlog;
  private Table leaf, root;

  private CoordVarExtractor timeVE, latVE, lonVE, altVE;
  private int nestedLevels;

  private DateFormatter dateFormatter = new DateFormatter();

  // A Nested Table is created after the Tables have been joined, and the leaves identified.
  NestedTable(NetcdfDataset ds, TableConfig config, Formatter errlog) {
    this.ds = ds;
    this.errlog = errlog;
    
    this.leaf = new Table(config);
    this.root = getRoot();

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

  Table getRoot() {
    Table p = leaf;
    while (p.parent != null) p = p.parent;
    return p;
  }

  // look for a coord axis of the given type in the table and its parents
  private CoordVarExtractor findCoordinateAxis(AxisType axisType, Table t, int nestingLevel) {
    if (t == null) return null;

    String axisName = null;
    if (axisType == AxisType.Lat)
      axisName = t.config.lat;
    else if (axisType == AxisType.Lon)
      axisName = t.config.lon;
    else if (axisType == AxisType.Height)
      axisName = t.config.elev;
    else if (axisType == AxisType.Time)
      axisName = t.config.time;


    if (axisName != null) {
      Variable v;
      if (t.struct != null)
        v = t.struct.findVariable(axisName);
      else
        v = ds.findVariable(axisName);

      if (v != null)
        return new CoordVarExtractor(v, axisName, nestingLevel);
      else
        errlog.format("NestedTable: cant find variable %s for coordinate type "+axisName, axisType);
    }

    // look in the parent
    return findCoordinateAxis(axisType, t.parent, nestingLevel + 1);
  }

  // knows how to get specific coordinate data from a table or its parents
  private class CoordVarExtractor {
    Variable axis;
    String axisName;
    int nestingLevel;       // leaf == 0, each parent adds one

    CoordVarExtractor(Variable v,  String axisName, int nestingLevel) {
      this.axis = v;
      this.axisName = axisName;
      this.nestingLevel = nestingLevel;
    }

    double getCoordValue(List<StructureData> structList) {
      StructureData struct = structList.get(nestingLevel);
      StructureMembers members = struct.getStructureMembers();
      StructureMembers.Member m = members.findMember(axisName);
      return struct.convertScalarDouble(m);
    }

    String getCoordValueString(List<StructureData> structList) {
      StructureData struct = structList.get(nestingLevel);
      StructureMembers members = struct.getStructureMembers();
      StructureMembers.Member m = members.findMember(axisName);
      return struct.getScalarString(m);
    }

    public String toString() {
      return axisName + " tableIndex= " + nestingLevel;
    }
  }

  public FeatureType getFeatureType() {
    // if (cs.ft != null) return cs.ft;

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
      return new DateUnit(timeVE.axis.getUnitsString());
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
    if (t.parent != null) addDataVariables(list, t.parent);
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
    formatter.format(" NestedTable = %s\n", getName());
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
    Table stationTable;

    if (getFeatureType() == FeatureType.STATION)
      stationTable = leaf.parent;

    else if (getFeatureType() == FeatureType.STATION_PROFILE)
      stationTable = leaf.parent.parent;

    else
      throw new UnsupportedOperationException("Not a StationFeatureCollection or StationProfileFeatureCollection");

    StructureDataIterator siter = stationTable.getStructureDataIterator(bufferSize);

    if (stationTable.config.limit != null) {
      Variable limitV = ds.findVariable(stationTable.config.limit);
      int limit = limitV.readScalarInt();
      return new StructureDataIteratorLimited(siter, limit);
    }

    return siter;
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
    TableConfig info = root.config;

    String stationName = getCoordValueString(stationData, info.stnId);
    String stationDesc = (info.stnDesc == null) ? stationName : getCoordValueString(stationData, info.stnDesc);
    double lat = getCoordValue(stationData, info.lat);
    double lon = getCoordValue(stationData, info.lon);
    double alt = (info.elev == null) ? Double.NaN : getCoordValue(stationData, info.elev);

    return new StationImpl(stationName, stationDesc, lat, lon, alt);
  }

  double getCoordValue(StructureData struct, String memberName) {
    StructureMembers members = struct.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);
    if (m == null)
      System.out.println("HEY");
    return struct.convertScalarDouble(m);
  }

  String getCoordValueString(StructureData struct, String memberName) {
    StructureMembers members = struct.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);
    if ((m.getDataType() == DataType.CHAR) || (m.getDataType() == DataType.STRING))
      return struct.getScalarString(m);
    else {
      int no = (int) struct.convertScalarDouble(m);
      return "#" + no;
    }
  }

  public StructureData makeObsStructureData(List<StructureData> structList) {
    return structList.get(0);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  public enum TableType {
    Structure, PseudoStructure, MultiDim, ArrayStructure, Singleton, Construct
  }

  /**
   * A generalization of a Structure. Main function is to return a StructureIterator
   */
  public class Table {
    TableConfig config;

    List<VariableSimpleIF> cols = new ArrayList<VariableSimpleIF>();    // all variables
    List<String> coordVars = new ArrayList<String>(); // just the coord axes

    Structure struct;    // Structure, PseudoStructure
    //ArrayStructure as;   // ArrayStructure
    StructureMembers sm; // MultiDim

    Table parent;
    Join join; // the join to its parent
    List<Join> children;

    // make a table based on a Structure
    public Table(TableConfig config) {
      this.config = config;

      if (config.type == TableType.Structure) {
        if ((config.parent != null) && (config.parent.type == TableType.Structure)) {
          Structure parent = (Structure) ds.findVariable(config.parent.name);
          struct = (Structure) parent.findVariable(config.name);

        } else {
          struct = (Structure) ds.findVariable(config.name);
        }

        assert struct != null : "cant find "+config.name;
        config.dim = struct.getDimension(0);
        for (Variable v : struct.getVariables())
          this.cols.add(v);

      } else if (config.type == TableType.PseudoStructure) {
        struct = new StructurePseudo(ds, null, config.name, config.dim);
        for (Variable v : struct.getVariables())
          this.cols.add(v);

      } else if (config.type == TableType.MultiDim) {
        sm = new StructureMembers(config.name);
        for (Variable v : ds.getVariables()) {
          if (v.getRank() < 2) continue;
          if (v.getDimension(0).equals(config.outer) && v.getDimension(1).equals(config.dim)) {
            cols.add(v);
            // make member
            int rank = v.getRank();
            int[] shape = new int[rank - 2];
            System.arraycopy(v.getShape(), 2, shape, 0, rank - 2);
            sm.addMember(v.getShortName(), v.getDescription(), v.getUnitsString(), v.getDataType(), shape);
          }
        }

      } else if (config.type == TableType.ArrayStructure) {
        config.dim = new Dimension(config.name, (int) config.as.getSize(), false);
        for (StructureMembers.Member m : config.as.getStructureMembers().getMembers())
          cols.add(new VariableSimpleAdapter(m));
      }

      if (config.parent != null)
        parent = new Table(config.parent);

      if (config.join != null) {
        if (config.join.override != null)
          join = config.join.override;
        else
          join = new Join(config.join);
        join.setTables(parent, this);
      }
    }

    StructureDataIterator getStructureDataIterator(int bufferSize) throws IOException {

      switch (config.type) {
        case Structure:
        case PseudoStructure:
          return struct.getStructureIterator(bufferSize);

        case ArrayStructure:
          return config.as.getStructureDataIterator();
      }

      throw new IllegalStateException("Table type = " + config.type);
    }

    public String getName() {
      return config.name;
    }

    public Structure getStruct() {
      return struct;
    }

    public List<? super VariableSimpleIF> getDataVariables() {
      return cols;
    }

    public String toString() {
      Formatter formatter = new Formatter();
      formatter.format(" Table %s on dimension %s type=%s\n", getName(), (config.dim == null) ? "null" : config.dim.getName(), config.type);
      formatter.format("  Coordinates=");
      formatter.format("\n  Data Variables= %d\n", cols.size());
      formatter.format("  Parent= %s join = %s\n", ((parent == null) ? "none" : parent.getName()), join);
      return formatter.toString();
    }

    public String showAll() {
      StringBuffer sbuff = new StringBuffer();
      sbuff.append("Table on dimension ").append(config.dim.getName()).append("\n");
      for (VariableSimpleIF v : cols) {
        sbuff.append("  ").append(v.getName()).append("\n");
      }
      return sbuff.toString();
    }

    public int show(Formatter f, int indent) {
      if (parent != null)
        indent = parent.show(f, indent);

      String s = indent(indent);
      String ftDesc = (config.featureType == null) ? "" : "featureType=" + config.featureType.toString();
      String joinDesc = (join == null) ? "" : "joinType=" + join.config.joinType.toString();
      String dimDesc = (config.dim == null) ? "*" : config.dim.getName() + "=" + config.dim.getLength() + (config.dim.isUnlimited() ? " unlim" : "");
      f.format("\n%sTable %s: type=%s(%s) %s %s\n", s, getName(), config.type, dimDesc, joinDesc, ftDesc);
      showCoords(f, s);
      for (VariableSimpleIF v : cols) {
        f.format("%s  %s %s\n", s, v.getName(), getKind(v.getShortName()));
      }
      return indent + 2;
    }

    String indent(int n) {
      StringBuffer sbuff = new StringBuffer();
      for (int i = 0; i < n; i++) sbuff.append(' ');
      return sbuff.toString();
    }

    private String getKind(String v) {
      if (v.equals(config.lat)) return "[Lat]";
      if (v.equals(config.lon)) return "[Lon]";
      if (v.equals(config.elev)) return "[Elev]";
      if (v.equals(config.time)) return "[Time]";
      if (v.equals(config.timeNominal)) return "[timeNominal]";
      if (v.equals(config.stnId)) return "[stnId]";
      if (v.equals(config.stnDesc)) return "[stnDesc]";
      if (v.equals(config.stnNpts)) return "[stnNpts]";
      if (v.equals(config.stnWmoId)) return "[stnWmoId]";
      if (v.equals(config.limit)) return "[limit]";

      return "";
    }

    private void showCoords(Formatter out, String indent ) {
      boolean gotSome;
      gotSome = showCoord(out, config.lat, indent);
      gotSome |= showCoord(out, config.lon, indent);
      gotSome |= showCoord(out, config.elev, indent);
      gotSome |= showCoord(out, config.time, indent);
      gotSome |= showCoord(out, config.timeNominal, indent);
      gotSome |= showCoord(out, config.stnId, indent);
      gotSome |= showCoord(out, config.stnDesc, indent);
      gotSome |= showCoord(out, config.stnNpts, indent);
      gotSome |= showCoord(out, config.stnWmoId, indent);
      gotSome |= showCoord(out, config.limit, indent);
      if (gotSome) out.format("\n");
    }

    private boolean showCoord(Formatter out, String name, String indent) {
      if (name != null) {
        out.format(" %s Coord %s %s\n", indent, name, getKind(name));
        return true;
      }
      return false;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////


}
