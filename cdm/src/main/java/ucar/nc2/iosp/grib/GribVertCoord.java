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
package ucar.nc2.iosp.grib;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.grib.Index;
import ucar.grib.TableLookup;
import ucar.grib.grib1.Grib1Lookup;

import java.util.*;

/**
 * A Vertical Coordinate variable for a Grib variable.
 *
 * @author caron
 * @deprecated
 */
public class GribVertCoord implements Comparable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribVertCoord.class);

  private Index.GribRecord typicalRecord; // becomes null after init
  private String levelName;
  private TableLookup lookup;
  private int seq = 0;

  private double[] coordValues;
  boolean usesBounds = false;

  boolean dontUseVertical = false;
  String positive = "up";
  String units;

  GribVertCoord(String name) {
    this.levelName = name;
    dontUseVertical = true;
  }

  GribVertCoord(List<Index.GribRecord> records, String levelName, TableLookup lookup) {
    this.typicalRecord = records.get(0);
    this.levelName = levelName;
    this.lookup = lookup;

    dontUseVertical = !lookup.isVerticalCoordinate( typicalRecord);
    positive = lookup.isPositiveUp(typicalRecord) ? "up" :"down";
    units = lookup.getLevelUnit( typicalRecord);

    usesBounds = Index2NC.isLayer(this.typicalRecord, lookup);
    addLevels( records);

    if (GribServiceProvider.debugVert)
      System.out.println("GribVertCoord: "+getVariableName()+"("+typicalRecord.levelType1+") useVertical= "+
          (!dontUseVertical)+" positive="+positive+" units="+units);
  }

  GribVertCoord(Index.GribRecord record, String levelName, TableLookup lookup, double[] level1, double[] level2) {
    this.typicalRecord = record;
    this.levelName = levelName;
    this.lookup = lookup;

    dontUseVertical = !lookup.isVerticalCoordinate(record);
    positive = lookup.isPositiveUp(record) ? "up" : "down";
    units = lookup.getLevelUnit(record);
    usesBounds = Index2NC.isLayer(this.typicalRecord, lookup);

    levels = new ArrayList<LevelCoord>(level1.length);
    for (int i = 0; i < level1.length; i++) {
      levels.add(new LevelCoord(level1[i], (level2 == null) ? 0.0 : level2[i]));
    }

    Collections.sort( levels );
    if( positive.equals( "down") ) {
      Collections.reverse( levels );
    }
  }

  void setSequence( int seq) { this.seq = seq; }

  String getLevelName() { return levelName; }
  String getVariableName() {
    return (seq == 0) ? levelName : levelName+seq; // more than one with same levelName
  }
  int getNLevels() { return dontUseVertical ? 1 : levels.size(); }

  void addLevels( List<Index.GribRecord> records) {
    for (Index.GribRecord record : records) {
      /*if (record.levelValue2 != 0) {
        if (GribServiceProvider.debugVert)
          System.out.println(levelName+" has levelType= "+record.levelType1+" levelValues="+record.levelValue1+","+record.levelValue2);
        hasTwoCoords = true;
      } */

      if (coordIndex(record) < 0) {
        levels.add(new LevelCoord(record.levelValue1, record.levelValue2));
        if (dontUseVertical && levels.size() > 1) {
          if (GribServiceProvider.debugVert)
            logger.warn("GribCoordSys: unused level coordinate has > 1 levels = " + levelName + " " + record.levelType1 + " " + levels.size());
        }
      }
    }
    Collections.sort( levels );
    if( positive.equals( "down") ) {
      Collections.reverse( levels );
    }
  }

  boolean matchLevels( List<Index.GribRecord> records) {

    // first create a new list
    List<LevelCoord> levelList = new ArrayList<LevelCoord>( records.size());
    for (Index.GribRecord record : records) {
      LevelCoord lc = new LevelCoord(record.levelValue1, record.levelValue2);
      if (!levelList.contains(lc))
        levelList.add(lc);
    }

    Collections.sort( levelList );
    if( positive.equals( "down") )
      Collections.reverse( levelList );

    // gotta equal existing list
    return levelList.equals( levels);
  }

  void addDimensionsToNetcdfFile( NetcdfFile ncfile, Group g) {
    if (dontUseVertical) return;
    int nlevs = levels.size();
    ncfile.addDimension(g, new Dimension(getVariableName(), nlevs));
  }

  void addToNetcdfFile( NetcdfFile ncfile, Group g) {
    if (dontUseVertical) {
      typicalRecord = null;      
      return;
    }

    if (g == null)
      g = ncfile.getRootGroup();

    // coordinate axis
    Variable v = new Variable( ncfile, g, null, getVariableName());
    v.setDataType( DataType.DOUBLE);

    String desc = lookup.getLevelDescription( typicalRecord);
    boolean isGrib1 = lookup instanceof Grib1Lookup;
    if (!isGrib1 && usesBounds) desc = "Layer between "+ desc;

    v.addAttribute( new Attribute("long_name", desc));
    v.addAttribute( new Attribute("units", lookup.getLevelUnit( typicalRecord)));

    // positive attribute needed for CF-1 Height and Pressure
    if (positive != null)
      v.addAttribute( new Attribute("positive", positive));

    if (units != null) {
      AxisType axisType;
      if (SimpleUnit.isCompatible("millibar", units))
        axisType = AxisType.Pressure;
      else if (SimpleUnit.isCompatible("m", units))
        axisType = AxisType.Height;
      else
        axisType = AxisType.GeoZ;

      v.addAttribute( new Attribute("GRIB_level_type", Integer.toString(typicalRecord.levelType1)));
      v.addAttribute( new Attribute(_Coordinate.AxisType, axisType.toString()));
    }

    if (coordValues == null) {
      coordValues = new double[levels.size()];
      for (int i = 0; i < levels.size(); i++) {
        LevelCoord lc = levels.get(i);
        coordValues[i] = lc.mid;
      }
    }
    Array dataArray = Array.factory( DataType.DOUBLE, new int [] {coordValues.length}, coordValues);

    v.setDimensions( getVariableName());
    v.setCachedData( dataArray, true);

    ncfile.addVariable( g, v);

    if (usesBounds) {
      String boundsDimName = "bounds_dim";
      if (g.findDimension(boundsDimName) == null)
        ncfile.addDimension(g, new Dimension(boundsDimName, 2));

      String bname = getVariableName() +"_bounds";
      v.addAttribute( new Attribute("bounds", bname));
      v.addAttribute( new Attribute(_Coordinate.ZisLayer, "true"));

      Variable b = new Variable( ncfile, g, null, bname);
      b.setDataType( DataType.DOUBLE);
      b.setDimensions( getVariableName()+" "+boundsDimName);
      b.addAttribute( new Attribute("long_name", "bounds for "+v.getName()));
      b.addAttribute( new Attribute("units", lookup.getLevelUnit( typicalRecord)));

      Array boundsArray = Array.factory( DataType.DOUBLE, new int [] {coordValues.length, 2});
      ucar.ma2.Index ima = boundsArray.getIndex();
      for (int i=0; i<coordValues.length; i++) {
        LevelCoord lc = levels.get(i);
        boundsArray.setDouble(ima.set(i,0), lc.value1);
        boundsArray.setDouble(ima.set(i,1), lc.value2);
      }
      b.setCachedData( boundsArray, true);

      ncfile.addVariable( g, b);
    }

  }

  void empty() {
    // let all references to Index go, to reduce retained size
    typicalRecord = null;
  }

  int getIndex(Index.GribRecord record) {
    if (dontUseVertical) return 0;
    return coordIndex( record);
  }

  public int compareTo(Object o) {
    GribVertCoord gv = (GribVertCoord) o;
    return getLevelName().compareToIgnoreCase( gv.getLevelName());
  }

  private List<LevelCoord> levels = new ArrayList<LevelCoord>();
  private class LevelCoord implements Comparable {
    double mid;
    double value1, value2;
    LevelCoord( double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      if (usesBounds && (value1 > value2)) {
        this.value1 = value2;
        this.value2 = value1;
      }
      mid = usesBounds ? (value1 + value2)/2 : value1;
    }

    public int compareTo(Object o) {
      LevelCoord other = (LevelCoord) o;
      // if (closeEnough(value1, other.value1) && closeEnough(value2, other.value2)) return 0;
      if (mid < other.mid) return -1;
      if (mid > other.mid) return 1;
      return 0;
    }

    public boolean equals(Object oo) {
      if (this == oo) return true;
      if ( !(oo instanceof LevelCoord)) return false;
      LevelCoord other = (LevelCoord) oo;
      return (ucar.nc2.util.Misc.closeEnough(value1, other.value1) && ucar.nc2.util.Misc.closeEnough(value2, other.value2));
    }

    public int hashCode() {
      return (int) (value1 * 100000 + value2 * 100);
    }
  }


  private int coordIndex(Index.GribRecord record) {
    double val = record.levelValue1;
    double val2 = record.levelValue2;
    if (usesBounds && (val > val2)) {
      val = record.levelValue2;
      val2 = record.levelValue1;
    }

    for (int i = 0; i < levels.size(); i++) {
      LevelCoord lc = levels.get(i);
      if (usesBounds) {
        if (ucar.nc2.util.Misc.closeEnough(lc.value1, val) && ucar.nc2.util.Misc.closeEnough(lc.value2, val2))
          return i;
      } else {
        if (ucar.nc2.util.Misc.closeEnough(lc.value1, val))
          return i;
      }
    }
    return -1;
  }

}