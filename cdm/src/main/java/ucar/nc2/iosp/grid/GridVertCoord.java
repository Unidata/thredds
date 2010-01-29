/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

// $Id: GridVertCoord.java 63 2006-07-12 21:50:51Z edavis $

package ucar.nc2.iosp.grid;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;
import ucar.grid.GridRecord;
import ucar.grid.GridTableLookup;
import ucar.grid.GridDefRecord;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.grib.grib1.Grib1GDSVariables;

import java.util.*;


/**
 * A Vertical Coordinate variable for a Grid variable.
 *
 * @author caron
 * @version $Revision: 63 $ $Date: 2006-07-12 15:50:51 -0600 (Wed, 12 Jul 2006) $
 */
public class GridVertCoord implements Comparable {

  /**
   * logger
   */
  static private org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(GridVertCoord.class);

  /**
   * typical record for this vertical coordinate
   */
  private GridRecord typicalRecord;

  /**
   * level name
   */
  private String levelName;

  /**
   * lookup table
   */
  private GridTableLookup lookup;

  /**
   * sequence #
   */
  private int seq = 0;

  /**
   * coord values
   */
  private double[] coordValues = null;

  /**
   * uses bounds flag
   */
  boolean usesBounds = false;

  /**
   * don't use vertical flag
   */
  boolean dontUseVertical = false;

  /**
     * vertical pressure factors
     */
  double[] factors = null;

  /**
   * positive  direction
   */
  String positive = "up";

  /**
   * units
   */
  String units;

  /**
   * levels
   */
  private List<LevelCoord> levels = new ArrayList<LevelCoord>();  // LevelCoord

  /**
   * Create a new GridVertCoord with the given name
   *
   * @param name name
   */
  GridVertCoord(String name) {
    this.levelName = name;
    dontUseVertical = true;
  }

  /**
   * Create a new GridVertCoord with the appropriate params
   *
   * @param records   list of GridRecords that make up this coord
   * @param levelName the name of the level
   * @param lookup    the lookup table
   * @param hcs Horizontal coordinate
   */
  GridVertCoord(List<GridRecord> records, String levelName,
                GridTableLookup lookup, GridHorizCoordSys hcs) {
    this.typicalRecord = records.get(0);
    this.levelName = levelName;
    this.lookup = lookup;

    dontUseVertical = !lookup.isVerticalCoordinate(typicalRecord);
    positive = lookup.isPositiveUp(typicalRecord)
        ? "up"
        : "down";
    units = lookup.getLevelUnit(typicalRecord);

    usesBounds = lookup.isLayer(this.typicalRecord);
    addLevels(records);

    if (typicalRecord.getLevelType1() == 109 && lookup instanceof Grib1GridTableLookup )
      checkForPressureLevels( records, hcs );

    if (GridServiceProvider.debugVert) {
      System.out.println("GribVertCoord: " + getVariableName() + "("
          + typicalRecord.getLevelType1()
          + ") useVertical= " + (!dontUseVertical)
          + " positive=" + positive + " units=" + units);
    }
  }

  /**
   * Create a new GridVertCoord for a layer
   *
   * @param record    layer record
   * @param levelName name of this level
   * @param lookup    lookup table
   * @param level1    level 1
   * @param level2    level 2
   */
  GridVertCoord(GridRecord record, String levelName,
                GridTableLookup lookup, double[] level1, double[] level2) {
    this.typicalRecord = record;
    this.levelName = levelName;
    this.lookup = lookup;

    //dontUseVertical    = !lookup.isVerticalCoordinate(record);
    positive = lookup.isPositiveUp(record)
        ? "up"
        : "down";
    units = lookup.getLevelUnit(record);
    usesBounds = lookup.isLayer(this.typicalRecord);

    levels = new ArrayList<LevelCoord>(level1.length);
    for (int i = 0; i < level1.length; i++) {
      levels.add(new LevelCoord(level1[i], (level2 == null)
          ? 0.0
          : level2[i]));
    }

    Collections.sort(levels);
    if (positive.equals("down")) {
      Collections.reverse(levels);
    }
    dontUseVertical = (levels.size() == 1);
  }

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
  String getVariableName() {
    return (seq == 0)
        ? levelName
        : levelName + seq;  // more than one with same levelName
  }

  /**
   * Get the number of levels
   *
   * @return number of levels
   */
  int getNLevels() {
    return dontUseVertical
        ? 1
        : levels.size();
  }

  /**
   * Add levels
   *
   * @param records GridRecords, one for each level
   */
  void addLevels(List<GridRecord> records) {
    for (int i = 0; i < records.size(); i++) {
      GridRecord record = records.get(i);

      if (coordIndex(record) < 0) {
        levels.add(new LevelCoord(record.getLevel1(),
            record.getLevel2()));
        if (dontUseVertical && (levels.size() > 1)) {
          if (GridServiceProvider.debugVert) {
            logger.warn(
                "GribCoordSys: unused level coordinate has > 1 levels = "
                    + levelName + " " + record.getLevelType1() + " "
                    + levels.size());
          }
        }
      }
    }
    Collections.sort(levels);
    if (positive.equals("down")) {
      Collections.reverse(levels);
    }
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
      LevelCoord lc = new LevelCoord(record.getLevel1(),
          record.getLevel2());
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
   * check for Sigma Pressure Levels
   */

  boolean checkForPressureLevels( List<GridRecord> records, GridHorizCoordSys hcs ) {
      GridDefRecord gdr = hcs.getGds();
      Grib1GDSVariables g1dr = (Grib1GDSVariables) gdr.getGdsv();
      if( g1dr == null || ! g1dr.hasVerticalPressureLevels() )
        return false;

      // add hybrid numbers
      coordValues = new double[ levels.size()];
        for (int i = 0; i < levels.size(); i++ ) {
          LevelCoord lc = levels.get( i );
          coordValues[ i ] =   lc.value1  ;
        }
      int NV = g1dr.getNV();
      // add new variables
      if (  NV > 2 && NV < 255 ) { // Some data doesn't add Pressure Level values
         factors = g1dr.getVerticalPressureLevels();
      }
      return true;
  }

  /**
   * Add this coord as a dimension to the netCDF file
   *
   * @param ncfile file to add to
   * @param g      group in the file
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
    if (dontUseVertical) {
      return;
    }
    int nlevs = levels.size();
    if ( coordValues != null )
      nlevs = coordValues.length;
    ncfile.addDimension(g, new Dimension(getVariableName(), nlevs, true));
  }

  /**
   * Add this coord as a variable in the netCDF file
   *
   * @param ncfile netCDF file to add to
   * @param g      group in file
   */
  void addToNetcdfFile(NetcdfFile ncfile, Group g) {
    if (dontUseVertical) {
      typicalRecord = null;
      return;
    }

    if (g == null) {
      g = ncfile.getRootGroup();
    }

    // coordinate axis
    Variable v = new Variable(ncfile, g, null, getVariableName());
    v.setDataType(DataType.DOUBLE);

    String desc = lookup.getLevelDescription(typicalRecord);
    if (lookup instanceof Grib2GridTableLookup && usesBounds) {
      desc = "Layer between " + desc;
    }

    v.addAttribute(new Attribute("long_name", desc));
    v.addAttribute(new Attribute("units",
        lookup.getLevelUnit(typicalRecord)));

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

      if (lookup instanceof Grib2GridTableLookup ||
          lookup instanceof Grib1GridTableLookup) {
        v.addAttribute(new Attribute("GRIB_level_type",
            Integer.toString(typicalRecord.getLevelType1())));
      } else {
        v.addAttribute(new Attribute("level_type",
            Integer.toString(typicalRecord.getLevelType1())));
      }
      v.addAttribute(new Attribute(_Coordinate.AxisType,
          axisType.toString()));
    }

    if (coordValues == null) {
      coordValues = new double[levels.size()];
      for (int i = 0; i < levels.size(); i++) {
        LevelCoord lc = (LevelCoord) levels.get(i);
        coordValues[i] = lc.mid;
      }
    }
    Array dataArray = Array.factory(DataType.DOUBLE,
        new int[]{coordValues.length},
        coordValues);

    v.setDimensions(getVariableName());
    v.setCachedData(dataArray, true);

    ncfile.addVariable(g, v);

    if (usesBounds) {
      String boundsDimName = "bounds_dim";
      if (g.findDimension(boundsDimName) == null) {
        ncfile.addDimension(g, new Dimension(boundsDimName, 2, true));
      }

      String bname = getVariableName() + "_bounds";
      v.addAttribute(new Attribute("bounds", bname));
      v.addAttribute(new Attribute(_Coordinate.ZisLayer, "true"));

      Variable b = new Variable(ncfile, g, null, bname);
      b.setDataType(DataType.DOUBLE);
      b.setDimensions(getVariableName() + " " + boundsDimName);
      b.addAttribute(new Attribute("long_name",
          "bounds for " + v.getName()));
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
  }

  void empty() {
    // let all references to Index go, to reduce retained size
    typicalRecord = null;
  }

  /**
   * Get the index of the particular record
   *
   * @param record record in question
   * @return the index or -1 if not found
   */
  int getIndex(GridRecord record) {
    if (dontUseVertical) {
      return 0;
    }
    return coordIndex(record);
  }

  /**
   * Compare this to another
   *
   * @param o the other GridVertCoord
   * @return the comparison
   */
  public int compareTo(Object o) {
    GridVertCoord gv = (GridVertCoord) o;
    return getLevelName().compareToIgnoreCase(gv.getLevelName());
  }

  /**
   * A level coordinate
   *
   * @author IDV Development Team
   * @version $Revision: 1.3 $
   */
  private class LevelCoord implements Comparable {

    /**
     * midpoint
     */
    double mid;

    /**
     * top/bottom values
     */
    double value1, value2;

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
      mid = usesBounds
          ? (value1 + value2) / 2
          : value1;
    }

    /**
     * Compare to another LevelCoord
     *
     * @param o another LevelCoord
     * @return the comparison
     */
    public int compareTo(Object o) {
      LevelCoord other = (LevelCoord) o;
      // if (closeEnough(value1, other.value1) && closeEnough(value2, other.value2)) return 0;
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
      return (ucar.nc2.util.Misc.closeEnough(value1, other.value1)
          && ucar.nc2.util.Misc.closeEnough(value2, other.value2));
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
        if (ucar.nc2.util.Misc.closeEnough(lc.value1, val)
            && ucar.nc2.util.Misc.closeEnough(lc.value2, val2)) {
          return i;
        }
      } else {
        if (ucar.nc2.util.Misc.closeEnough(lc.value1, val)) {
          return i;
        }
      }
    }
    return -1;
  }

}
