/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataFactory;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureDataIteratorLimited;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.point.StationFeature;
import ucar.nc2.ft.point.StationFeatureImpl;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.EarthLocationImpl;

/**
 * Implements "nested table" views of point feature datasets.
 * A NestedTable is initialized with a TableConfig.
 * A NestedTable Table is created after the Tables have been joined, and the leaves identified.
 * It is a single chain of Table objects from child to parent. Highest parent is root. Lowest child is leaf
 * <p>
 * A nested table starts with a leaf table (no children), plus all of its parents.
 * There is a "join" for each child and parent.
 * <p>
 * Assumes that we have Tables that can be iterated over with a StructureDataIterator.
 * A parent-child join assumes that for each row of the parent, a StructureDataIterator exists that
 * iterates over the rows of the child table for that parent.
 * <p>
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
  private CoordVarExtractor stnVE, stnDescVE, wmoVE, stnAltVE, idVE, missingVE;
  private List<Variable> extras;

  private int nlevels;

  NestedTable(NetcdfDataset ds, TableConfig config, Formatter errlog) {
    this.ds = ds;
    this.errlog = errlog;

    this.leaf = Table.factory(ds, config);
    this.root = getRoot();

    // use the featureType from the highest level table
    nlevels = 0;
    Table t = leaf;
    while (t != null) {
      if (t.getFeatureType() != null) featureType = t.getFeatureType();
      t = t.parent;
      // if (!(t instanceof Table.TableTop)) // LOOK using nlevels is fishy
      nlevels++;
    }
    if (featureType == null)
      featureType = FeatureDatasetFactoryManager.findFeatureType(ds);

    /* find joins with extra variables
    t = leaf;
    while (t != null) {
      if (t.extraJoins != null) {
        for (Join j : t.extraJoins) {
          addExtraVariable(j.getExtraVariable());
        }
      }
      t = t.parent; // recurse upwards
    } */

    // will find the first one, starting at the leaf and going up
    timeVE = findCoordinateAxis(Table.CoordName.Time, leaf, 0);
    latVE = findCoordinateAxis(Table.CoordName.Lat, leaf, 0);
    lonVE = findCoordinateAxis(Table.CoordName.Lon, leaf, 0);
    altVE = findCoordinateAxis(Table.CoordName.Elev, leaf, 0);
    nomTimeVE = findCoordinateAxis(Table.CoordName.TimeNominal, leaf, 0);

    // search for station info
    stnVE = findCoordinateAxis(Table.CoordName.StnId, leaf, 0);
    stnDescVE = findCoordinateAxis(Table.CoordName.StnDesc, leaf, 0);
    wmoVE = findCoordinateAxis(Table.CoordName.WmoId, leaf, 0);
    stnAltVE = findCoordinateAxis(Table.CoordName.StnAlt, leaf, 0);

    missingVE = findCoordinateAxis(Table.CoordName.MissingVar, leaf, 0);
    idVE = findCoordinateAxis(Table.CoordName.FeatureId, root, nlevels - 1); // LOOK start at root ??

    // LOOK: Major kludge
    if (featureType == null) {
      if (nlevels == 1) featureType = FeatureType.POINT;
      if (nlevels == 2) featureType = FeatureType.STATION;
      if (nlevels == 3) featureType = FeatureType.STATION_PROFILE;
    }

    // find coordinates that are not part of the extras
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if (!isCoordinate(axis) && !isExtra(axis)
              && axis.getDimensionsAll().size() <= 1)  // Only permit 0-D and 1-D axes as extra variables.
        addExtraVariable(axis);
    }

    /* check for singleton
    if (((nlevels == 1) && (featureType == FeatureType.STATION) || (featureType == FeatureType.PROFILE) || (featureType == FeatureType.TRAJECTORY)) ||
            ((nlevels == 2) && (featureType == FeatureType.STATION_PROFILE) || (featureType == FeatureType.TRAJECTORY_PROFILE))) {

      // singleton. use file name as feature name, so aggregation will work
      StructureData sdata = StructureDataFactory.make(featureVariableName, ds.getLocation());
      TableConfig parentConfig = new TableConfig(Table.Type.Singleton, featureType.toString());
      parentConfig.sdata = sdata;
      root = Table.factory(ds, parentConfig);

      nlevels++;
    } // */
  }

  Table getRoot() {
    Table p = leaf;
    while (p.parent != null) p = p.parent;
    return p;
  }

  Table getLeaf() {
    return leaf;
  }

  List<Variable> getExtras() {
    return extras;
  }

  private void addExtraVariable(Variable v) {
    if (v == null) return;
    if (extras == null) extras = new ArrayList<>();
    extras.add(v);
  }

  // Has v already been added to the set of extra variables?
  private boolean isExtra(Variable v) {
    return v != null && extras != null && extras.contains(v);
  }

  // Is v a coordinate axis for this feature type?
  private boolean isCoordinate(Variable v) {
    if (v == null) return false;
    String name = v.getShortName();
    return (latVE != null && latVE.axisName.equals(name)) ||
            (lonVE != null && lonVE.axisName.equals(name)) ||
            (altVE != null && altVE.axisName.equals(name)) ||
            (stnAltVE != null && stnAltVE.axisName.equals(name)) ||
            (timeVE != null && timeVE.axisName.equals(name)) ||
            (nomTimeVE != null && nomTimeVE.axisName.equals(name));
  }

  // find a coord axis of the given type in the table and its parents
  private CoordVarExtractor findCoordinateAxis(Table.CoordName coordName, Table t, int nestingLevel) {
    if (t == null) return null;

    String axisName = t.findCoordinateVariableName(coordName);

    if (axisName != null) {
      VariableDS v = t.findVariable(axisName);
      if (v != null)
        return new CoordVarExtractorVariable(v, axisName, nestingLevel);

      if (t.extraJoins != null) {
        for (Join j : t.extraJoins) {
          v = j.findVariable(axisName);
          if (v != null)
            return new CoordVarExtractorVariable(v, axisName, nestingLevel);
        }
      }

      // see if its in the StructureData
      if (t instanceof Table.TableSingleton) {
        Table.TableSingleton ts = (Table.TableSingleton) t;
        return new CoordVarStructureData(axisName, ts.sdata);
      }

      // see if its at the top level
      if (t instanceof Table.TableTop) {
        v = (VariableDS) ds.findVariable(axisName);

        if (v != null)
          return new CoordVarTop(v);
        else
          return new CoordVarConstant(coordName.toString(), "", axisName); // assume its the actual value
      }

      errlog.format("NestedTable: cant find variable '%s' for coordinate type %s %n", axisName, coordName);
    }

    // check the parent
    return findCoordinateAxis(coordName, t.parent, nestingLevel + 1);
  }

  /////////////////////////////////////////////////////////////////////////
  // knows how to get specific coordinate data from a table or its parents
  private static class CoordVarExtractorVariable extends CoordVarExtractor {
    protected VariableDS coordVar;

    CoordVarExtractorVariable(VariableDS v, String axisName, int nestingLevel) {
      super(axisName, nestingLevel);
      this.coordVar = v;
    }

    @Override
    public String getCoordValueString(StructureData sdata) {
      if (coordVar.getDataType().isString())
        return sdata.getScalarString(memberName);
      else if (coordVar.getDataType().isIntegral())
        return Integer.toString(sdata.convertScalarInt(memberName));
      else
        return Double.toString(sdata.convertScalarDouble(memberName));
    }

    @Override
    public String getUnitsString() {
      return coordVar.getUnitsString();
    }

    @Override
    public boolean isString() {
      return coordVar.getDataType().isString();
    }

    @Override
    public boolean isMissing(StructureData sdata) {
      if (isString()) {
        String s = getCoordValueString(sdata);
        double test = (s.length() == 0) ? 0 : (double) s.charAt(0);
        return coordVar.isMissing(test);
      } else {
        double val = getCoordValue(sdata);
        return coordVar.isMissing(val);
      }
    }

    @Override
    public double getCoordValue(StructureData sdata) {
      return sdata.convertScalarDouble(memberName);
    }

    @Override
    public boolean isInt() {
      return coordVar.getDataType().isIntegral();
    }

    @Override
    public long getCoordValueLong(StructureData sdata) {
      return sdata.convertScalarLong(memberName);
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // knows how to get specific coordinate data from a table or its parents
  private static class CoordVarTop extends CoordVarExtractor {
    protected VariableDS varTop;

    CoordVarTop(VariableDS v) {
      super(v.getFullName(), 0);
      this.varTop = v;
    }

    @Override
    public double getCoordValue(StructureData sdata) {
      try {
        return varTop.readScalarDouble();
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    @Override
    public String getUnitsString() {
      return varTop.getUnitsString();
    }

    @Override
    public boolean isString() {
      return varTop.getDataType().isString();
    }

    @Override
    public boolean isInt() {
      return varTop.getDataType().isIntegral();
    }

    @Override
    public long getCoordValueLong(StructureData sdata) {
      try {
        return varTop.readScalarLong();
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    @Override
    public String getCoordValueString(StructureData sdata) {
      try {
        return varTop.readScalarString();
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    @Override
    public boolean isMissing(StructureData sdata) {
      if (isString()) return false;
      double val = getCoordValue(sdata);
      return varTop.isMissing(val);
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // knows how to get specific coordinate data from a table or its parents
  private static class CoordVarStructureData extends CoordVarExtractor {
    protected StructureData sdata;

    CoordVarStructureData(String axisName, StructureData sdata) {
      super(axisName, 0);
      this.sdata = sdata;
    }

    @Override
    public double getCoordValue(StructureData ignore) {
      return sdata.convertScalarDouble(memberName);
    }

    @Override
    public String getCoordValueString(StructureData ignore) {
      return sdata.getScalarString(memberName);
    }

    @Override
    public String getUnitsString() {
      StructureMembers.Member m = sdata.findMember(memberName);
      return m.getUnitsString();
    }

    @Override
    public boolean isString() {
      StructureMembers.Member m = sdata.findMember(memberName);
      return m.getDataType().isString();
    }

    @Override
    public boolean isInt() {
      StructureMembers.Member m = sdata.findMember(memberName);
      return m.getDataType().isIntegral();
    }

    @Override
    public long getCoordValueLong(StructureData sdata) {
      return sdata.convertScalarLong(memberName);
    }

    @Override
    public boolean isMissing(StructureData sdata) {
      return false;
    }

  }

  /////////////////////////////////////////////////////////////////////////
  // a constant coordinate variable
  private static class CoordVarConstant extends CoordVarExtractor {
    String units, value;

    CoordVarConstant(String name, String units, String value) {
      super(name, 0);
      this.units = units;
      this.value = value;
    }

    @Override
    public double getCoordValue(StructureData sdata) {
      return Double.parseDouble(value);
    }

    @Override
    public long getCoordValueLong(StructureData sdata) {
      return Long.parseLong(value);
    }

    @Override
    public String getCoordValueString(StructureData sdata) {
      return value;
    }

    @Override
    public String getUnitsString() {
      return units;
    }

    @Override
    public boolean isString() {
      return true;
    }

    @Override
    public boolean isInt() {
      return false;
    }

    @Override
    public boolean isMissing(StructureData sdata) {
      return false;
    }

    @Override
    public String toString() {
      return "CoordVarConstant value= " + value;
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

  public CalendarDateUnit getTimeUnit() {
    try {
      return CalendarDateUnit.of(null, timeVE.getUnitsString()); // LOOK dont know the calendar
    } catch (Exception e) {
      throw new IllegalArgumentException("Error on time string = " + timeVE.getUnitsString() + " == " + e.getMessage());
    }
  }

  public String getAltUnits() {
    if (altVE != null) return altVE.getUnitsString();         // fishy
    if (stnAltVE != null) return stnAltVE.getUnitsString();
    return null;
  }

  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> data = new ArrayList<>();
    addDataVariables(data, leaf);
    Collections.sort(data);
    return data;
  }

  // use recursion so that parent variables come first
  private void addDataVariables(List<VariableSimpleIF> list, Table t) {
    if (t.parent != null) addDataVariables(list, t.parent);
    for (VariableSimpleIF col : t.cols.values()) {
      if (t.nondataVars.contains(col.getFullName())) continue;
      if (t.nondataVars.contains(col.getShortName())) continue;  // fishy
      list.add(col);
    }
  }

  public String getName() {
    Formatter formatter = new Formatter();
    formatter.format("%s", root.getName());

    Table t = root;
    while (t.child != null) {
      t = t.child;
      String name = t.getName() != null ? t.getName() : "anon";
      formatter.format("/%s", name);
    }
    return formatter.toString();
  }

  public String toString() {
    Formatter formatter = new Formatter();
    formatter.format("NestedTable = %s%n", getName());
    formatter.format("  Time= %s%n", timeVE);
    formatter.format("  Lat= %s%n", latVE);
    formatter.format("  Lon= %s%n", lonVE);
    formatter.format("  Height= %s%n", altVE);

    return formatter.toString();
  }

  public void show(Formatter formatter) {
    formatter.format(" NestedTable = %s%n", getName());
    formatter.format("   nlevels = %d%n", nlevels);
    leaf.show(formatter, 2);
  }

  ///////////////////////////////////////////////////////////////////////////////

  public double getObsTime(Cursor cursor) {
    return getTime(timeVE, cursor.tableData);
  }

  public double getNomTime(Cursor cursor) {
    return getTime(nomTimeVE, cursor.tableData);
  }

  private double getTime(CoordVarExtractor cve, StructureData[] tableData) {
    if (cve == null) return Double.NaN;
    if (tableData[cve.nestingLevel] == null) return Double.NaN;

    if (cve.isString()) {
      String timeString = timeVE.getCoordValueString(tableData);
      CalendarDate date = CalendarDateFormatter.isoStringToCalendarDate(null, timeString);
      if (date == null) {
        log.error("Cant parse date - not ISO formatted, = " + timeString);
        return 0.0;
      }
      return date.getMillis();

    } else {
      return cve.getCoordValue(tableData);
    }
  }

  public double getLatitude(Cursor cursor) {
    return latVE.getCoordValue(cursor.tableData);
  }

  public double getLongitude(Cursor cursor) {
    return lonVE.getCoordValue(cursor.tableData);
  }

  public EarthLocation getEarthLocation(Cursor cursor) {
    double lat = latVE.getCoordValue(cursor.tableData);
    double lon = lonVE.getCoordValue(cursor.tableData);
    double alt = (altVE == null) ? Double.NaN : altVE.getCoordValue(cursor.tableData);
    if (stnAltVE != null) {
      double stnElev = stnAltVE.getCoordValue(cursor.tableData);
      if (altVE == null)
        alt = stnElev;
      else
        alt += stnElev;
    }
    return new EarthLocationImpl(lat, lon, alt);
  }

  public String getFeatureName(Cursor cursor) {
    int count = 0;
    Table t = leaf;
    while (count++ < cursor.currentIndex)
      t = t.parent;

    if (t.feature_id == null) return "unknown";
    StructureData sdata = cursor.getParentStructure();
    if (sdata == null) return "unknown";
    StructureMembers.Member m = sdata.findMember(t.feature_id);
    if (m == null) return "unknown";

    if (m.getDataType().isString())
      return sdata.getScalarString(m);
    else if (m.getDataType().isIntegral())
      return Integer.toString(sdata.convertScalarInt(m));
    else
      return Double.toString(sdata.convertScalarDouble(m));
  }


  public boolean isFeatureMissing(StructureData sdata) {
    return idVE != null && idVE.isMissing(sdata);
  }

  public boolean isTimeMissing(Cursor cursor) {
    return timeVE != null && timeVE.isMissing(cursor.tableData);
  }

  public boolean isAltMissing(Cursor cursor) {
    return altVE != null && altVE.isMissing(cursor.tableData);
  }

  public boolean isMissing(Cursor cursor) {
    return missingVE != null && missingVE.isMissing(cursor.tableData);
  }


  //////////////////////////////////////////////////

  public StructureData makeObsStructureData(Cursor cursor) {
    return StructureDataFactory.make(cursor.tableData);
  }

  public StructureData makeObsStructureData(Cursor cursor, int nest) {
    return cursor.tableData[nest];
  }

  /* public void addParentJoin(Cursor cursor) throws IOException {
    Table t = leaf;
    int level = 0;
    while (t != null) {
      addParentJoin(t, level, cursor);
      level++;
      t = t.parent;
    }
  }  */

  // add table join to this cursor level
  void addParentJoin(Cursor cursor) throws IOException {
    int level = cursor.currentIndex;
    Table t = getTable(level);
    if (t.extraJoins != null) {
      List<StructureData> sdata = new ArrayList<>(3);
      sdata.add(cursor.tableData[level]);
      for (Join j : t.extraJoins) {
        sdata.add(j.getJoinData(cursor));
      }
      cursor.tableData[level] = StructureDataFactory.make(sdata.toArray(new StructureData[sdata.size()]));  // LOOK should try to consolidate
    }
  }

  private Table getTable(int level) {
    Table t = leaf;
    int count = 0;
    while (count < level) {
      count++;
      t = t.parent;
    }
    return t;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////
  // not clear these methods should be here

  //// Point

  public StructureDataIterator getObsDataIterator(Cursor cursor) throws IOException {
    return root.getStructureDataIterator(cursor);
  }

  //// Station or Station_Profile
  public StructureDataIterator getStationDataIterator() throws IOException {
    Table stationTable = root;
    StructureDataIterator siter = stationTable.getStructureDataIterator(null);

    if (stationTable.limit != null) {
      Variable limitV = ds.findVariable(stationTable.limit);
      int limit = limitV.readScalarInt();
      return new StructureDataIteratorLimited(siter, limit);
    }

    return siter;
  }

  //// Trajectory, Profile, Section
  public StructureDataIterator getRootFeatureDataIterator() throws IOException {
    return root.getStructureDataIterator(null);
  }

  public StructureDataIterator getLeafFeatureDataIterator(Cursor cursor) throws IOException {
    return leaf.getStructureDataIterator(cursor);
  }

  public StructureDataIterator getMiddleFeatureDataIterator(Cursor cursor) throws IOException {
    return leaf.parent.getStructureDataIterator(cursor);  // the middle table
  }

  // also called from StandardPointFeatureIterator
  StationFeature makeStation(StructureData stationData) {
    if (stnVE.isMissing(stationData)) return null;
    String stationName = stnVE.getCoordValueAsString(stationData);

    String stationDesc = (stnDescVE == null) ? "" : stnDescVE.getCoordValueAsString(stationData);
    String stnWmoId = (wmoVE == null) ? "" : wmoVE.getCoordValueAsString(stationData);

    double lat = latVE.getCoordValue(stationData);
    double lon = lonVE.getCoordValue(stationData);
    double elev = (stnAltVE == null) ? Double.NaN : stnAltVE.getCoordValue(stationData);

    // missing lat, lon means skip this station
    if (Double.isNaN(lat) || Double.isNaN(lon)) return null;

    return new StationFeatureImpl(stationName, stationDesc, stnWmoId, lat, lon, elev, -1, stationData);
  }

  /////////////////////////////////////////////////////////
  // Table.Construct: stations get constructed by reading the obs and extracting

  /* private List<Station> constructStations(Table.TableConstruct stationTable) throws IOException {
    Map<String, StationConstruct> stnMap = new HashMap<String, StationConstruct>();
    ArrayList<Station> result = new ArrayList<Station>();
    StructureDataIterator iter = stationTable.getStructureDataIterator(null, -1); // this will be the obs structure
    int recno = 0;
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      String stationName = stnVE.getCoordValueString(sdata);
      StationConstruct s = stnMap.get(stationName);
      if (s == null) {
        s = makeStationConstruct(stationName, stationTable.getObsStructure(), sdata);
        stnMap.put(stationName, s);
        result.add(s);
      }
      double obsTime = timeVE.getCoordValue(sdata);
      s.addIndex(recno, obsTime);
      recno++;
    }

    return result;
  }

  private StationConstruct makeStationConstruct(String stationName, Structure obsStruct, StructureData stationData) {
    String stationDesc = (stnDescVE == null) ? "" : stnDescVE.getCoordValueString(stationData);
    String stnWmoId = (wmoVE == null) ? "" : wmoVE.getCoordValueString(stationData);

    double lat = latVE.getCoordValue(stationData);
    double lon = lonVE.getCoordValue(stationData);
    double elev = (stnAltVE == null) ? Double.NaN : stnAltVE.getCoordValue(stationData);

    return new StationConstruct(stationName, obsStruct, stationDesc, stnWmoId, lat, lon, elev);
  }

  private class StationConstruct extends StationImpl {
    List<Index> index;
    Structure obsStruct;

    StationConstruct(String name, Structure obsStruct, String desc, String wmoId, double lat, double lon, double alt) {
      super(name, desc, wmoId, lat, lon, alt);
      this.obsStruct = obsStruct;
    }

    void addIndex(int recno, double time) {
      if (index == null)
        index = new ArrayList<Index>();

      Index i = new Index();
      i.recno = recno;
      i.time = time;
      index.add(i);
    }

    class Index {
      int recno;
      double time;
    }

    StructureDataIterator getStructureDataIterator(int bufferSize) {
      return new IndexedStructureDataIterator();
    }

    private class IndexedStructureDataIterator implements ucar.ma2.StructureDataIterator {
      private int count = 0;

      public boolean hasNext() throws IOException {
        return count < index.size();
      }

      public StructureData next() throws IOException {
        Index i = index.get(count++);
        try {
          return obsStruct.readStructure(i.recno);
        } catch (InvalidRangeException e) {
          throw new IllegalStateException("bad recnum " + i.recno, e);
        }
      }

      public void setBufferSize(int bytes) {
      }

      public StructureDataIterator reset() {
        count = 0;
        return this;
      }
    }

  } */

}
