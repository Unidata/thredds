/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grid;

import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;

import java.util.*;

/**
 * A Vertical Coordinate variable for a Grid variable.
 *
 * @author caron
 */
public class GridVertCoord implements Comparable<GridVertCoord> {

  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GridVertCoord.class);

  /**
   * typical record for this vertical coordinate
   */
  protected GridRecord typicalRecord;

  protected String levelName;

  protected GridTableLookup lookup;

  protected int seq = 0;

  protected double[] coordValues = null;

  protected boolean usesBounds = false;

  protected boolean isVerticalCoordinate = false;

  /**
     * vertical pressure factors
     */
  protected double[] factors = null;

  /**
   * positive  direction
   */
  private String positive = "up";

  /**
   * units
   */
  protected String units;

  /**
   * levels
   */
  protected List<LevelCoord> levels = new ArrayList<LevelCoord>();  // LevelCoord

  /**
   * Create a new GridVertCoord with the given name.
   * Used by deprecated GridIndex2NC.makeDefinedCoord()
   *
   * @param name name
   */
  protected GridVertCoord(String name) {
    this.levelName = name;
  }

  /**
   * Create a new GridVertCoord with the appropriate params
   *
   * @param records   list of GridRecords that make up this coord
   * @param levelName the name of the level
   * @param lookup    the lookup table
   * @param hcs Horizontal coordinate
   */
  protected GridVertCoord(List<GridRecord> records, String levelName, GridTableLookup lookup, GridHorizCoordSys hcs) {
    this.typicalRecord = records.get(0);
    this.levelName = levelName;
    this.lookup = lookup;
    this.isVerticalCoordinate = lookup.isVerticalCoordinate(typicalRecord);

    //isVerticalCoordinate = lookup.isVerticalCoordinate(typicalRecord);
    positive = lookup.isPositiveUp(typicalRecord) ? "up" : "down";
    units = lookup.getLevelUnit(typicalRecord);

    usesBounds = lookup.isLayer(this.typicalRecord);

    for (GridRecord record : records) {

      if (coordIndex(record) < 0) {
        levels.add(new LevelCoord(record.getLevel1(), record.getLevel2()));

        /* check if assumption violated
       if (!isVerticalCoordinate && (levels.size() > 1)) {
         if (GridServiceProvider.debugVert) {
           logger.warn( "GribCoordSys: unused level coordinate has > 1 levels = "
                   + levelName + " " + record.getLevelType1() + " "
                   + levels.size());
         }
       } */
      }
    }

    Collections.sort(levels);
      if (positive.equals("down")) {
        Collections.reverse(levels);
      }

    if (GridServiceProvider.debugVert) {
      System.out.println("GribVertCoord: " + getVariableName() + "("
          + typicalRecord.getLevelType1()
          + ") isVertDimensionUsed= " + isVertDimensionUsed()
          + " positive=" + positive + " units=" + units);
    }
  }

  /*
   * Create a new GridVertCoord for a layer
   * Used by deprecated GridIndex2NC.makeDefinedCoord()
   *
   * @deprecated
   * @param record    layer record
   * @param levelName name of this level
   * @param lookup    lookup table
   * @param level1    level 1
   * @param level2    level 2
   *
  GridVertCoord(GridRecord record, String levelName, GridTableLookup lookup, double[] level1, double[] level2) {
    this.typicalRecord = record;
    this.levelName = levelName;
    this.lookup = lookup;

    //dontUseVertical    = !lookup.isVerticalCoordinate(record);
    positive = lookup.isPositiveUp(record) ? "up" : "down";
    units = lookup.getLevelUnit(record);
    usesBounds = lookup.isLayer(this.typicalRecord);

    levels = new ArrayList<LevelCoord>(level1.length);
    for (int i = 0; i < level1.length; i++) {
      levels.add(new LevelCoord(level1[i], (level2 == null) ? 0.0  : level2[i]));
    }

    Collections.sort(levels);
    if (positive.equals("down")) {
      Collections.reverse(levels);
    }
    //isVerticalCoordinate = (levels.size() > 1);
  } */

  /**
   * Set the sequence number
   *
   * @param seq the sequence number
   */
  void setSequence(int seq) {
    this.seq = seq;
  }

  /**
   * Set the level name
   *
   * @return the level name
   */
  String getLevelName() {
    return levelName;
  }

  /**
   * Get the variable name
   *
   * @return the variable name
   */
  public String getVariableName() {
    return (seq == 0) ? levelName : levelName + seq;  // more than one with same levelName
  }

  /**
   * Get the number of levels
   *
   * @return number of levels
   */
  int getNLevels() {
    return levels.size();
  }

  /**
   * vert coordinates are used when nlevels > 1, otherwise use isVerticalCoordinate
   * @return if vert dimension should be used
   */
  boolean isVertDimensionUsed() {
    return (getNLevels() == 1) ? isVerticalCoordinate : true;
  }

  /**
   * Match levels
   *
   * @param records records to match
   * @return true if they have the same levels
   */
  boolean matchLevels(List<GridRecord> records) {

    // first create a new list
    List<LevelCoord> levelList = new ArrayList<LevelCoord>(records.size());
    for (GridRecord record : records) {
      LevelCoord lc = new LevelCoord(record.getLevel1(), record.getLevel2());
      if (!levelList.contains(lc)) {
        levelList.add(lc);
      }
    }

    Collections.sort(levelList);
    if (positive.equals("down")) {
      Collections.reverse(levelList);
    }

    // gotta equal existing list
    return levelList.equals(levels);
  }


  /**
   * Add this coord as a dimension to the netCDF file
   *
   * @param ncfile file to add to
   * @param g      group in the file
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
    if (!isVertDimensionUsed())
      return;
    
    int nlevs = levels.size();
    if ( coordValues != null )
      nlevs = coordValues.length;
    ncfile.addDimension(g, new Dimension(getVariableName(), nlevs, true));
  }

  protected String getLevelDesc() {
    return lookup.getLevelDescription(typicalRecord);
  }

  protected void addExtraAttributes(Variable v) {
    v.addAttribute(new Attribute("level_type", Integer.toString(typicalRecord.getLevelType1())));
  }

  /**
   * Add this coord as a variable in the netCDF file
   *
   * @param ncfile netCDF file to add to
   * @param g      group in file
   */
  void addToNetcdfFile(NetcdfFile ncfile, Group g) {
    if (!isVertDimensionUsed()) {
      typicalRecord = null; // allow gc
      return;
    }

    if (g == null) {
      g = ncfile.getRootGroup();
    }

    // coordinate axis
    Variable v = new Variable(ncfile, g, null, getVariableName());
    v.setDataType(DataType.DOUBLE);

    String desc =  getLevelDesc();
    v.addAttribute(new Attribute("long_name", desc));
    v.addAttribute(new Attribute("units", lookup.getLevelUnit(typicalRecord)));

    // positive attribute needed for CF-1 Height and Pressure
    if (positive != null) {
      v.addAttribute(new Attribute("positive", positive));
    }

    if (units != null) {
      AxisType axisType;
      if (SimpleUnit.isCompatible("millibar", units)) {
        axisType = AxisType.Pressure;
      } else if (SimpleUnit.isCompatible("m", units)) {
        axisType = AxisType.Height;
      } else {
        axisType = AxisType.GeoZ;
      }

      addExtraAttributes(v);
      v.addAttribute(new Attribute(_Coordinate.AxisType, axisType.toString()));
    }

    if (coordValues == null) {
      coordValues = new double[levels.size()];
      for (int i = 0; i < levels.size(); i++) {
        LevelCoord lc = (LevelCoord) levels.get(i);
        coordValues[i] = lc.mid;
      }
    }
    Array dataArray = Array.factory(DataType.DOUBLE, new int[]{coordValues.length}, coordValues);

    v.setDimensions(getVariableName());
    v.setCachedData(dataArray, true);

    ncfile.addVariable(g, v);

    if (usesBounds) {
      Dimension bd = ucar.nc2.dataset.DatasetConstructor.getBoundsDimension(ncfile);

      String bname = getVariableName() + "_bounds";
      v.addAttribute(new Attribute("bounds", bname));
      v.addAttribute(new Attribute(_Coordinate.ZisLayer, "true"));

      Variable b = new Variable(ncfile, g, null, bname);
      b.setDataType(DataType.DOUBLE);
      b.setDimensions(getVariableName() + " " + bd.getShortName());
      b.addAttribute(new Attribute("long_name",
          "bounds for " + v.getFullName()));
      b.addAttribute(new Attribute("units",
          lookup.getLevelUnit(typicalRecord)));

      Array boundsArray = Array.factory(DataType.DOUBLE,
          new int[]{coordValues.length,
              2});
      ucar.ma2.Index ima = boundsArray.getIndex();
      for (int i = 0; i < coordValues.length; i++) {
        LevelCoord lc = (LevelCoord) levels.get(i);
        boundsArray.setDouble(ima.set(i, 0), lc.value1);
        boundsArray.setDouble(ima.set(i, 1), lc.value2);
      }
      b.setCachedData(boundsArray, true);

      ncfile.addVariable(g, b);
    }

    if (factors != null) {
      // check if already created
      if (g == null) {
        g = ncfile.getRootGroup();
      }
      if ( g.findVariable ( "hybrida" ) != null)
        return ;
      v.addAttribute(new Attribute("standard_name", "atmosphere_hybrid_sigma_pressure_coordinate" ));
      v.addAttribute(new Attribute("formula_terms", "ap: hybrida b: hybridb ps: Pressure" ));
      // create  hybrid factor variables
      // add hybrida variable
      Variable ha = new Variable(ncfile, g, null, "hybrida");
      ha.setDataType(DataType.DOUBLE);
      ha.addAttribute(new Attribute("long_name",  "level_a_factor" ));
      ha.addAttribute(new Attribute("units", ""));
      ha.setDimensions(getVariableName());
      // add data
      int middle = factors.length / 2;
      double[] adata;
      double[] bdata;
      if( levels.size() < middle ) { // only partial data wanted
        adata = new double[ levels.size() ];
        bdata = new double[ levels.size() ];
      } else {
        adata = new double[ middle ];
        bdata = new double[ middle ];
      }
      for( int i = 0; i < middle && i < levels.size(); i++ )
        adata[ i ] = factors[ i ];
      Array haArray = Array.factory(DataType.DOUBLE, new int[]{adata.length},adata);
      ha.setCachedData(haArray, true);
      ncfile.addVariable(g, ha);

      // add hybridb variable
      Variable hb = new Variable(ncfile, g, null, "hybridb");
      hb.setDataType(DataType.DOUBLE);
      hb.addAttribute(new Attribute("long_name",  "level_b_factor" ));
      hb.addAttribute(new Attribute("units", ""));
      hb.setDimensions(getVariableName());
      // add data
      for( int i = 0; i < middle && i < levels.size(); i++ )
        bdata[ i ] = factors[ i + middle ];
      Array hbArray = Array.factory(DataType.DOUBLE, new int[]{bdata.length},bdata);
      hb.setCachedData(hbArray, true);
      ncfile.addVariable(g, hb);


      /*  // TODO: delete next time modifying code
      double[] adata = new double[ middle ];
      for( int i = 0; i < middle; i++ )
        adata[ i ] = factors[ i ];
      Array haArray = Array.factory(DataType.DOUBLE, new int[]{adata.length}, adata);
      ha.setCachedData(haArray, true);
      ncfile.addVariable(g, ha);

      // add hybridb variable
      Variable hb = new Variable(ncfile, g, null, "hybridb");
      hb.setDataType(DataType.DOUBLE);
      hb.addAttribute(new Attribute("long_name",  "level_b_factor" ));
      //hb.addAttribute(new Attribute("standard_name", "atmosphere_hybrid_sigma_pressure_coordinate" ));
      hb.addAttribute(new Attribute("units", ""));
      hb.setDimensions(getVariableName());
      // add data
      double[] bdata = new double[ middle ];
      for( int i = 0; i < middle; i++ )
        bdata[ i ] = factors[ i + middle ];
      Array hbArray = Array.factory(DataType.DOUBLE, new int[]{bdata.length}, bdata);
      hb.setCachedData(hbArray, true);
      ncfile.addVariable(g, hb);
      */
    }

    // allow gc
    // typicalRecord = null;
  }

  /**
   * Get the index of the particular record
   *
   * @param record record in question
   * @return the index or -1 if not found
   */
  int getIndex(GridRecord record) {
    if (!isVertDimensionUsed())
      return 0;

    return coordIndex(record);
  }

  /**
   * Compare this to another
   *
   * @param gv the other GridVertCoord
   * @return the comparison
   */
  public int compareTo(GridVertCoord gv) {
    return getLevelName().compareToIgnoreCase(gv.getLevelName());
  }

  public double getCoord(int i) {
    return (coordValues == null) ? 0.0 : coordValues[i];
  }

  /**
   * A level coordinate
   */
  protected class LevelCoord implements Comparable {

    /**
     * midpoint
     */
    public double mid;

    /**
     * top/bottom values
     */
    public double value1, value2;

    /**
     * Create a new LevelCoord
     *
     * @param value1 top
     * @param value2 bottom
     */
    LevelCoord(double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      if (usesBounds && (value1 > value2)) {
        this.value1 = value2;
        this.value2 = value1;
      }
      mid = usesBounds ? (value1 + value2) / 2 : value1;
    }

    /**
     * Compare to another LevelCoord
     *
     * @param o another LevelCoord
     * @return the comparison
     */
    public int compareTo(Object o) {
      LevelCoord other = (LevelCoord) o;
      // if (nearlyEquals(value1, other.value1) && nearlyEquals(value2, other.value2)) return 0;
      if (mid < other.mid) {
        return -1;
      }
      if (mid > other.mid) {
        return 1;
      }
      return 0;
    }

    /**
     * Check for equality
     *
     * @param oo object in question
     * @return true if equal
     */
    public boolean equals(Object oo) {
      if (this == oo) {
        return true;
      }
      if (!(oo instanceof LevelCoord)) {
        return false;
      }
      LevelCoord other = (LevelCoord) oo;
      return (ucar.nc2.util.Misc.nearlyEquals(value1, other.value1)
          && ucar.nc2.util.Misc.nearlyEquals(value2, other.value2));
    }

    /**
     * Generate a hashcode
     *
     * @return the hashcode
     */
    public int hashCode() {
      return (int) (value1 * 100000 + value2 * 100);
    }
  }

  /**
   * Get the coordinate index for the record
   *
   * @param record record in question
   * @return index or -1 if not found
   */
  private int coordIndex(GridRecord record) {
    double val = record.getLevel1();
    double val2 = record.getLevel2();
    if (usesBounds && (val > val2)) {
      val = record.getLevel2();
      val2 = record.getLevel1();
    }

    for (int i = 0; i < levels.size(); i++) {
      LevelCoord lc = (LevelCoord) levels.get(i);
      if (usesBounds) {
        if (ucar.nc2.util.Misc.nearlyEquals(lc.value1, val) && ucar.nc2.util.Misc.nearlyEquals(lc.value2, val2)) {
          return i;
        }
      } else {
        if (ucar.nc2.util.Misc.nearlyEquals(lc.value1, val)) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public String toString() {
    return "GridVertCoord{" +
            "levelName='" + levelName + '\'' +
            ", seq=" + seq +
            '}';
  }
}
