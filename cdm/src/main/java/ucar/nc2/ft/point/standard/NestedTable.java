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
  private CoordVarExtractor stnVE, stnDescVE, wmoVE;

  private String featureVariableName;
  private int nlevels;

  private DateFormatter dateFormatter = new DateFormatter();

  // A NestedTable Table is created after the Tables have been joined, and the leaves identified.
  NestedTable(NetcdfDataset ds, TableConfig config, Formatter errlog) {
    this.ds = ds;
    this.errlog = errlog;

    this.leaf = Table.factory(ds, config);
    this.root = getRoot();

    nlevels = 1;
    Table t = leaf;
    featureType = t.getFeatureType();
    while (t.parent != null) {
      t = t.parent;
      if (featureType == null) featureType = t.getFeatureType();
      if (!(t instanceof Table.TableTop)) // LOOK using nlevels is fishy
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

    // will find the first one, starting at the leaf and going up
    timeVE = findCoordinateAxis(Table.CoordName.Time, leaf, 0);
    timeVE = findCoordinateAxis(Table.CoordName.Time, leaf, 0);
    latVE = findCoordinateAxis(Table.CoordName.Lat, leaf, 0);
    lonVE = findCoordinateAxis(Table.CoordName.Lon, leaf, 0);
    altVE = findCoordinateAxis(Table.CoordName.Elev, leaf, 0);
    nomTimeVE = findCoordinateAxis(Table.CoordName.TimeNominal, leaf, 0);

    // look for station info
    stnVE = findCoordinateAxis(Table.CoordName.StnId, leaf, 0);
    stnDescVE = findCoordinateAxis(Table.CoordName.StnDesc, leaf, 0);
    wmoVE = findCoordinateAxis(Table.CoordName.WmoId, leaf, 0);

    // check for singleton
    if (((nlevels == 1) && (featureType == FeatureType.STATION) || (featureType == FeatureType.PROFILE) || (featureType == FeatureType.TRAJECTORY)) ||
        ((nlevels == 2) && (featureType == FeatureType.STATION_PROFILE) || (featureType == FeatureType.SECTION))) {

      // singleton. use file name as feature name, so aggregation will work
      featureVariableName = "featureName";
      StructureData sdata = StructureDataFactory.make(featureVariableName, ds.getLocation());
      TableConfig parentConfig = new TableConfig(Table.Type.Singleton, featureType.toString());
      parentConfig.sdata = sdata;
      Table newRoot = Table.factory(ds, parentConfig);

      Join join = Join.factory(new TableConfig.JoinConfig(Join.Type.Identity));
      join.joinTables(newRoot, root);

      root = newRoot;

      nlevels++;
    }
  }

  Table getRoot() {
    Table p = leaf;
    while (p.parent != null) p = p.parent;
    return p;
  }

  // look for a coord axis of the given type in the table and its parents
  private CoordVarExtractor findCoordinateAxis(Table.CoordName coordName, Table t, int nestingLevel) {
    if (t == null) return null;

    String axisName = t.findCoordinateVariableName( coordName);

    if (axisName != null) {
      Variable v = t.findVariable(axisName);
      if (v != null)
        return new CoordVarExtractor(v, axisName, nestingLevel);

      // see if its at the top level
      if (t instanceof Table.TableTop) {
        v = ds.findVariable(axisName);

        if (v != null)
          return new CoordVarTop(v);
        else
          return new CoordVarConstant(axisName); // assume its the actual value
      }

      errlog.format("NestedTable: cant find variable %s for coordinate type " + axisName, coordName);
    }

    // look in the parent
    return findCoordinateAxis(coordName, t.parent, nestingLevel + 1);
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
      return getCoordValue( tableData[nestingLevel]);
    }

    String getCoordValueString(StructureData sdata) {
      if (coordVar.getDataType().isString())
        return sdata.getScalarString(axisName);
      else if (coordVar.getDataType().isIntegral() )
        return Integer.toString(sdata.convertScalarInt(axisName));
      else
        return Double.toString(sdata.convertScalarDouble(axisName));
    }

    double getCoordValue(StructureData sdata) {
      return sdata.convertScalarDouble(axisName);
    }

    String getCoordValueString(StructureData[] tableData) {
      return getCoordValueString( tableData[nestingLevel]);
    }

    public String toString() {
      return axisName + " tableIndex= " + nestingLevel;
    }
  }

  // knows how to get specific coordinate data from a table or its parents
  private class CoordVarTop extends CoordVarExtractor {

    CoordVarTop(Variable v) {
      this.coordVar = v;
    }

    @Override
    double getCoordValue(StructureData sdata) {
      try {
        return coordVar.readScalarDouble();
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }

    @Override
    String getCoordValueString(StructureData sdata) {
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

   // knows how to get specific coordinate data from a table or its parents
  private class CoordVarConstant extends CoordVarExtractor {
    String value;

    CoordVarConstant(String value) {
      this.value = value;
    }

     double getCoordValue(StructureData sdata) {
      return Double.parseDouble(value);
    }

     String getCoordValueString(StructureData sdata) {
      return value;
    }

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
    String stationName = stnVE.getCoordValueString(stationData);
    String stationDesc = (stnDescVE == null) ? "" : stnDescVE.getCoordValueString( stationData);
    String stnWmoId = (wmoVE == null) ? "" : wmoVE.getCoordValueString( stationData);

    double lat = latVE.getCoordValue(stationData);
    double lon = lonVE.getCoordValue(stationData);
    double elev = (altVE == null) ? Double.NaN : altVE.getCoordValue( stationData);

    return new StationImpl(stationName, stationDesc, stnWmoId, lat, lon, elev);
  }

  public StructureData makeObsStructureData(StructureData[] tableData) {
    return StructureDataFactory.make(tableData);
  }

  // kludgey
  public void addParentJoin(StructureData[] tableData) throws IOException {
    if (leaf.extraJoinTable != null) {
      StructureData obsdata = tableData[0];

      Join.JoinParentIndex join = (Join.JoinParentIndex) leaf.extraJoinTable.join2parent;
      StructureData extra = join.getJoin(obsdata);

      tableData[0] = StructureDataFactory.make(obsdata, extra);
    }
  }

}
