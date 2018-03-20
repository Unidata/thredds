/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.constants.FeatureType;
import ucar.ma2.ArrayStructure;
import ucar.ma2.StructureData;

import java.util.List;
import java.util.ArrayList;

/**
 * This encapsulates the info needed by NestedTable to handle point feature "nested table" datasets.
 * A TableAnalyzer creates these from a specific dataset convention.
 * <p> a TableConfig has a tree of TableConfigs, representing the join of parent and children tables.
 *
 * @author caron
 * @since Apr 23, 2008
 */
public class TableConfig {
  public enum StructureType {Structure, PsuedoStructure, PsuedoStructure2D}

  public Table.Type type;
  public String name;
  public TableConfig parent;
  public List<TableConfig> children;
  public List<Join> extraJoin;

  public String structName; // full name of structure
  public String nestedTableName; // short name of structure
  public StructureType structureType = StructureType.Structure; // default

  // linked, contiguous list
  public String start;  // name of variable - starting child index (in parent)
  public String next;  // name of variable - next child index (in child)
  public String numRecords;  // name of variable - number of children (in parent)

  // only the top featureType in the tree is used
  public FeatureType featureType;

  // TablePsuedoStructureList, Structure
  public List<String> vars;
  public String dimName; // outer dimension

  // multidim: outer and inner dimensions
  public String outerName, innerName;

  // Table.Type ArrayStructure
  public ArrayStructure as;

  // Table.Type Singleton
  public StructureData sdata;

  // Table.Type ParentIndex
  public String parentIndex;  // name of variable - parent index (in parent)

  // coordinate variable names
  public String lat, lon, elev, time, timeNominal, limit;

  // station info
  public String stnId, stnDesc, stnNpts, stnWmoId, stnAlt;

  // variable name holding the id
  public String feature_id;
  public String missingVar;

  /**
   * Constructor
   * @param type  type of join
   * @param name  name of table
   */
  public TableConfig(Table.Type type, String name) {
    this.type = type;
    this.name = name;
    this.structName = name;
  }

  public void addChild(TableConfig t) {
    if (children == null) children = new ArrayList<>();
    children.add(t);
    t.parent = this;
  }

  public void addJoin(Join extra) {
    if (extraJoin == null) extraJoin = new ArrayList<>(3);
    extraJoin.add(extra);
  }

  public String findCoordinateVariableName(Table.CoordName coordName) {
    switch (coordName) {
      case Elev:
        return elev;
      case Lat:
        return lat;
      case Lon:
        return lon;
      case Time:
        return time;
      case TimeNominal:
        return timeNominal;

      case StnId:
        return stnId;
      case StnDesc:
        return stnDesc;
      case WmoId:
        return stnWmoId;
      case StnAlt:
        return stnAlt;

      case FeatureId:
        return feature_id;

      case MissingVar:
        return missingVar;

    }
    return null;
  }

  public void setCoordinateVariableName(Table.CoordName coordName, String name) {
    switch (coordName) {
      case Elev:
        elev = name;
        break;
      case Lat:
        lat = name;
        break;
      case Lon:
        lon = name;
        break;
      case Time:
        time = name;
        break;
      case TimeNominal:
        timeNominal = name;
        break;
      case StnId:
        stnId = name;
        break;
      case StnDesc:
        stnDesc = name;
        break;
      case WmoId:
        stnWmoId = name;
        break;
      case StnAlt:
        stnAlt = name;
        break;

      case FeatureId:
        feature_id = name;
        break;

      case MissingVar:
        missingVar = name;
        break;
    }
  }

  String getNumRecords() {
    if (numRecords != null) return numRecords;
    if (parent != null) return parent.getNumRecords();
    return null;
  }

  String getStart() {
    if (start != null) return start;
    if (parent != null) return parent.getStart();
    return null;
  }


}
