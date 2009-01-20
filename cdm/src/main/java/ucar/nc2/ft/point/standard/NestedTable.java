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
import ucar.nc2.ft.*;
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
 * A NestedTable is initialized with a TableConfig.
 * <p/>
 * A nested table starts with a leaf table (no children), plus all of its parents.
 * There is a "join" for each child and parent.
 * <p/>
 * Assumes that we have Tables that can be iterated over with a StructureDataIterator.
 * A parent-child join assumes that for each row of the parent, a StructureDataIterator exists that
 * iterates over the rows of the child table for that parent.
 * <p/>
 * Nested Tables must be put in canonical form, based on feature type:
 * <ol>
 * <li> point : obsTable
 * <li> station : stnTable -> obsTable
 * <li> traj : trajTable -> obsTable
 * <li> profile : profileTable -> obsTable
 * <li> stationProfile : stnTable -> profileTable -> obsTable
 * <li> section : sectionTable -> trajTable -> obsTable
 * </ol>
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class NestedTable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NestedTable.class);

  private NetcdfDataset ds;
  private Formatter errlog;
  private Table leaf, root;
  private FeatureType featureType;

  private CoordVarExtractor timeVE, nomTimeVE, latVE, lonVE, altVE;
  private String featureVariableName;
  private int nlevels;

  private DateFormatter dateFormatter = new DateFormatter();

  // A NestedTable Table is created after the Tables have been joined, and the leaves identified.
  NestedTable(NetcdfDataset ds, TableConfig config, Formatter errlog) {
    this.ds = ds;
    this.errlog = errlog;

    this.leaf = Table.factory(ds, config);
    this.root = getRoot();

    // will find the first one, starting at the leaf and going up
    timeVE = findCoordinateAxis(AxisType.Time, leaf, 0);
    latVE = findCoordinateAxis(AxisType.Lat, leaf, 0);
    lonVE = findCoordinateAxis(AxisType.Lon, leaf, 0);
    altVE = findCoordinateAxis(AxisType.Height, leaf, 0);

    nomTimeVE = findNomTimeCoordinateAxis(leaf, 0);

    nlevels = 1;
    Table t = leaf;
    featureType = t.getFeatureType();
    while (t.parent != null) {
      t = t.parent;
      if (featureType == null) featureType = t.getFeatureType();
      nlevels++;
    }

    if (featureType == null) {
      featureType = FeatureDatasetFactoryManager.findFeatureType(ds);
    }

    if (featureType == null) {
      if (nlevels == 1) featureType = FeatureType.POINT;
      if (nlevels == 2) featureType = FeatureType.STATION;
      if (nlevels == 3) featureType = FeatureType.STATION_PROFILE;
    }

    // check for singleton
    if (((nlevels == 1) && (featureType == FeatureType.STATION) || (featureType == FeatureType.PROFILE) || (featureType == FeatureType.TRAJECTORY)) ||
        ((nlevels == 2) && (featureType == FeatureType.STATION_PROFILE) || (featureType == FeatureType.SECTION))) {

      // singleton. use file name as feature name, so aggregation will work
      featureVariableName = "featureName";
      StructureData sdata = StructureDataFactory.make(featureVariableName, ds.getLocation());
      TableConfig parentConfig = new TableConfig(TableType.Singleton, featureType.toString());
      parentConfig.sdata = sdata;
      root = Table.factory(ds, parentConfig);

      Join join = Join.factory(new TableConfig.JoinConfig(JoinType.Singleton));
      join.joinTables(root, leaf);

      nlevels++;
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
      axisName = t.lat;
    else if (axisType == AxisType.Lon)
      axisName = t.lon;
    else if (axisType == AxisType.Height)
      axisName = t.elev;
    else if (axisType == AxisType.Time)
      axisName = t.time;

    if (axisName != null) {
      Variable v = t.findVariable(axisName);
      if (v != null)
        return new CoordVarExtractor(v, axisName, nestingLevel);

      // see if its at the top level
      v = ds.findVariable(axisName);

      if (v == null)
        errlog.format("NestedTable: cant find variable %s for coordinate type " + axisName, axisType);
      else
        // LOOK this is fishy - whats the meaning of nestingLevel?
       return (v.getSize() == 1) ? new CoordVarScalar(v) : new CoordVarExtractor(v, axisName, nestingLevel);
    }

    // look in the parent
    return findCoordinateAxis(axisType, t.parent, nestingLevel + 1);
  }

  private CoordVarExtractor findNomTimeCoordinateAxis(Table t, int nestingLevel) {
    if (t == null) return null;
    if (t.timeNominal != null) {

      Variable v = t.findVariable(t.timeNominal);
      if (v != null)
        return new CoordVarExtractor(v, t.timeNominal, nestingLevel);

      // see if its at the top level
      v = ds.findVariable(t.timeNominal);

      if (v != null)
        // LOOK this is fishy - whats the meaning of nestingLevel?
        return (v.getSize() == 1) ? new CoordVarScalar(v) : new CoordVarExtractor(v, t.timeNominal, nestingLevel);
    }

    // look in the parent
    return findNomTimeCoordinateAxis( t.parent, nestingLevel + 1);
  }

  // knows how to get specific coordinate data from a table or its parents
  private class CoordVarExtractor {
    protected Variable coordVar;
    private String axisName;
    private int nestingLevel;       // leaf == 0, each parent adds one

    CoordVarExtractor() {
    }

    CoordVarExtractor(Variable v, String axisName, int nestingLevel) {
      this.coordVar = v;
      this.axisName = axisName;
      this.nestingLevel = nestingLevel;
    }

    double getCoordValue(StructureData[] tableData) {
      StructureData struct = tableData[nestingLevel];
      //StructureMembers members = struct.getStructureMembers();
      //StructureMembers.Member m = members.findMember(axisName);
      return struct.convertScalarDouble(axisName);
    }

    String getCoordValueString(StructureData[] tableData) {
      StructureData struct = tableData[nestingLevel];
      StructureMembers members = struct.getStructureMembers();
      StructureMembers.Member m = members.findMember(axisName);
      return struct.getScalarString(m);
    }

    public String toString() {
      return axisName + " tableIndex= " + nestingLevel;
    }
  }

  // knows how to get specific coordinate data from a table or its parents
  private class CoordVarScalar extends CoordVarExtractor {

    CoordVarScalar(Variable v) {
      this.coordVar = v;
    }

    double getCoordValue(StructureData[] tableData) {
      try {
        return coordVar.readScalarDouble();
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    String getCoordValueString(StructureData[] tableData) {
      try {
        return coordVar.readScalarString();
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    public String toString() {
      return "scalar " + coordVar.getName();
    }
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  public int getNumberOfLevels() {
    return nlevels;
  }

  public boolean hasCoords() {
    return (timeVE != null) && (latVE != null) && (lonVE != null);
  }

  public DateUnit getTimeUnit() {
    try {
      return new DateUnit(timeVE.coordVar.getUnitsString());
    } catch (Exception e) {
      throw new IllegalArgumentException("Error on time string = " + timeVE.coordVar.getUnitsString() + " == " + e.getMessage());
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
    formatter.format("   nlevels = %d\n", nlevels);
    leaf.show(formatter, 2);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // not clear these methods should be here

  // Point

  public StructureDataIterator getObsDataIterator(int bufferSize) throws IOException {
    return root.getStructureDataIterator(null, bufferSize);
  }

  //Station
  public StructureDataIterator getStationDataIterator(int bufferSize) throws IOException {

    if (!(getFeatureType() == FeatureType.STATION) && !(getFeatureType() == FeatureType.STATION_PROFILE))
      throw new UnsupportedOperationException("Not a StationFeatureCollection or StationProfileFeatureCollection");

    Table stationTable = root;
    StructureDataIterator siter = stationTable.getStructureDataIterator(null, bufferSize);

    if (stationTable.limit != null) {
      Variable limitV = ds.findVariable(stationTable.limit);
      int limit = limitV.readScalarInt();
      return new StructureDataIteratorLimited(siter, limit);
    }

    return siter;
  }

  public StructureDataIterator getStationObsDataIterator(StructureData stationData, int bufferSize) throws IOException {
    return leaf.getStructureDataIterator(stationData, bufferSize);
  }

  // Trajectory, Profile
  public StructureDataIterator getFeatureDataIterator(int bufferSize) throws IOException {
    return root.getStructureDataIterator(null, bufferSize);
  }

  public StructureDataIterator getFeatureObsDataIterator(StructureData trajData, int bufferSize) throws IOException {
    return leaf.getStructureDataIterator(trajData, bufferSize);
  }

  // Station Profile
  public StructureDataIterator getStationProfileDataIterator(StructureData stationData, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    return root.getStructureDataIterator(stationData, bufferSize);
  }

  public StructureDataIterator getStationProfileObsDataIterator(StructureData[] parents, int bufferSize) throws IOException {
    if (getFeatureType() != FeatureType.STATION_PROFILE)
      throw new UnsupportedOperationException("Not a StationProfileFeatureCollection");

    StructureData profileData = parents[1];
    return leaf.getStructureDataIterator(profileData, bufferSize);
  }

  ///////////////////////////////////////////////////////////////////////////////

  public double getObsTime(StructureData[] tableData) {
    return getTime(timeVE, tableData);
  }

  public double getNomTime(StructureData[] tableData) {
    return getTime(nomTimeVE, tableData);
  }

  private double getTime(CoordVarExtractor cve, StructureData[] tableData) {
    if (cve == null) return Double.NaN;

    if ((cve.coordVar.getDataType() == ucar.ma2.DataType.CHAR) || (cve.coordVar.getDataType() == ucar.ma2.DataType.STRING)) {
      String timeString = timeVE.getCoordValueString(tableData);
      Date date;
      try {
        date = dateFormatter.isoDateTimeFormat(timeString);
      } catch (ParseException e) {
        log.error("Cant parse date - not ISO formatted, = " + timeString);
        return 0.0;
      }
      return date.getTime() / 1000.0; // LOOK

    } else {
      return cve.getCoordValue(tableData);
    }
  }

  public double getLatitude(StructureData[] tableData) {
    return latVE.getCoordValue(tableData);
  }

  public double getLongitude(StructureData[] tableData) {
    return lonVE.getCoordValue(tableData);
  }

  public EarthLocation getEarthLocation(StructureData[] tableData) {
    double lat = latVE.getCoordValue(tableData);
    double lon = lonVE.getCoordValue(tableData);
    double alt = (altVE == null) ? Double.NaN : altVE.getCoordValue(tableData);
    return new EarthLocationImpl(lat, lon, alt);
  }

  public String getFeatureName(StructureData sdata) {
    if (featureVariableName == null) return "unknown";
    StructureMembers.Member m = sdata.findMember(featureVariableName);
    if (m == null) return "unknown";
    return sdata.getScalarString(m);
  }

  public Station makeStation(StructureData stationData) {
    //TableConfig info = root.config;

    double lat, lon, elev;
    String stationName;
    String stationDesc;
    String stnWmoId;
    if (root instanceof Table.TableSingleton) {
      stationName = root.stnId;
      stationDesc = (root.stnDesc == null) ? stationName : root.stnDesc;
      stnWmoId = root.stnWmoId;
      // must be scalars - no List<Structure>
      lat = latVE.getCoordValue(null);
      lon = lonVE.getCoordValue(null);
      elev = (altVE == null) ? Double.NaN : altVE.getCoordValue(null);

    } else {
      stationName = getCoordValueString(stationData, root.stnId);
      stationDesc = (root.stnDesc == null) ? stationName : getCoordValueString(stationData, root.stnDesc);
      stnWmoId = (root.stnWmoId == null) ? null : getCoordValueString(stationData, root.stnWmoId);
      lat = getCoordValue(stationData, root.lat);
      lon = getCoordValue(stationData, root.lon);
      elev = (root.elev == null) ? Double.NaN : getCoordValue(stationData, root.elev);
    }


    return new StationImpl(stationName, stationDesc, stnWmoId, lat, lon, elev);
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

  // LOOK - need to merge into one StructureData !!
  public StructureData makeObsStructureData(StructureData[] tableData) {
    return tableData[0];
  }

  // kludgey
  private ArrayStructure parentData = null;

  public void addParentJoin(StructureData[] tableData) throws IOException {
    if (leaf.extraJoinTable != null) {
      StructureData obsdata = tableData[0];

      Join.JoinParentIndex join = (Join.JoinParentIndex) leaf.extraJoinTable.join2parent;
      StructureData extra = join.getJoin(obsdata);

      tableData[0] = StructureDataFactory.make(obsdata, extra);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  /**
   * A generalization of a Structure. Main function is to return a StructureDataIterator
   *
  public class Table {
    TableConfig config;

    List<VariableSimpleIF> cols = new ArrayList<VariableSimpleIF>();    // all variables
    List<String> coordVars = new ArrayList<String>(); // just the coord axes

    Structure struct;    // Structure, PseudoStructure
    //ArrayStructure as;   // ArrayStructure
    StructureMembers sm; // MultiDim

    Table parent;
    Join join2parent; // the join to its parent
    List<Join> children;

    // singleton
    StructureData sdata;

    public Table(TableConfig config) {
      this.config = config;

      if (config.type == TableType.Structure) {
        if ((config.parent != null) && (config.parent.type == TableType.Structure)) {
          Structure parent = (Structure) ds.findVariable(config.parent.name);
          struct = (Structure) parent.findVariable(config.name);

        } else {
          struct = (Structure) ds.findVariable(config.name);
        }

        if (struct == null) return; // bail out
        // assert struct != null : "cant find Structure Variable = " + config.name;
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
          join2parent = config.join.override;
        else
          join2parent = Join.factory(config.join);

        join2parent.joinTables(parent, this);
      }
    }

    StructureDataIterator getStructureDataIterator(StructureData parent, int bufferSize) throws IOException {
      if (join2parent != null)
        return join2parent.getStructureDataIterator(parent, bufferSize);

      switch (config.type) {
        case Structure:
        case PseudoStructure:
          return struct.getStructureIterator(bufferSize);

        case ArrayStructure:
          return config.as.getStructureDataIterator();

        case Singleton:
          return new SingletonStructureDataIterator(sdata);

        default:
          throw new IllegalStateException("Table type = " + config.type);
      }

    }

    public String getName() {
      return config.name;
    }

    public FeatureType getFeatureType() {
      return (config == null) ? null : config.featureType;
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
      formatter.format("  Parent= %s join = %s\n", ((parent == null) ? "none" : parent.getName()), join2parent);
      return formatter.toString();
    }

    public String showAll() {
      StringBuilder sbuff = new StringBuilder();
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
      String joinDesc = (join2parent == null) ? "" : "joinType=" + join2parent.getClass().toString();
      String dimDesc = (config.dim == null) ? "*" : config.dim.getName() + "=" + config.dim.getLength() + (config.dim.isUnlimited() ? " unlim" : "");
      f.format("\n%sTable %s: type=%s(%s) %s %s\n", s, getName(), config.type, dimDesc, joinDesc, ftDesc);
      showCoords(f, s);
      for (VariableSimpleIF v : cols) {
        f.format("%s  %s %s\n", s, v.getName(), getKind(v.getShortName()));
      }
      return indent + 2;
    }

    String indent(int n) {
      StringBuilder sbuff = new StringBuilder();
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

    private void showCoords(Formatter out, String indent) {
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
  private class SingletonStructureDataIterator implements StructureDataIterator {
    private int count = 0;
    private StructureData sdata;

    SingletonStructureDataIterator(StructureData sdata) {
      this.sdata = sdata;
    }

    public boolean hasNext() throws IOException {
      return (count == 0);
    }

    public StructureData next() throws IOException {
      count++;
      return sdata;
    }

    public void setBufferSize(int bytes) {
    }

    public StructureDataIterator reset() {
      count = 0;
      return this;
    }
  } */


}
