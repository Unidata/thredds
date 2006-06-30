// $Id: GribVertCoord.java,v 1.3 2006/01/17 23:07:14 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.grib;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.dataset.AxisType;
import ucar.grib.Index;
import ucar.grib.TableLookup;

import java.util.*;

/**
 * A Vertical Coordinate variable for a Grib variable.
 * @author john
 */
public class GribVertCoord implements Comparable {
  private Index.GribRecord record;
  private String levelName;
  private TableLookup lookup;
  private int seq = 0;

  private List levels = new ArrayList();
  boolean dontUseVertical = false;
  String positive = "up";
  String units;

  GribVertCoord(List records, String levelName, TableLookup lookup) {
    this.record = (Index.GribRecord) records.get(0);
    this.levelName = levelName;
    this.lookup = lookup;

    dontUseVertical = !lookup.isVerticalCoordinate( record);
    positive = lookup.isPositiveUp(record) ? "up" :"down";
    units = lookup.getLevelUnit( record);

    addLevels( records);

    if (GribServiceProvider.debugVert)
      System.out.println("GribVertCoord: "+getVariableName()+"("+record.levelType1+") useVertical= "+
          (!dontUseVertical)+" positive="+positive+" units="+units);
  }

  void setSequence( int seq) { this.seq = seq; }

  String getLevelName() { return levelName; }
  String getVariableName() {
    return (seq == 0) ? levelName : levelName+seq; // more than one with same levelName
  }
  int getNLevels() { return dontUseVertical ? 1 : levels.size(); }

  void addLevels( List records) {
    for (int i = 0; i < records.size(); i++) {
      Index.GribRecord record = (Index.GribRecord) records.get(i);
      Double d = new Double( record.levelValue1);
      if (!levels.contains(d))
        levels.add(d);
      if (dontUseVertical && levels.size() > 1) {
        if (GribServiceProvider.debugVert)
          System.out.println("GribCoordSys: unused level coordinate has > 1 levels = "+levelName+" "+record.levelType1+" "+levels.size());
      }
    }
    Collections.sort( levels );
    if( positive.equals( "down") ) {
      Collections.reverse( levels );
      /* for( int i = 0; i < (levels.size()/2); i++ ){
          Double tmp = (Double) levels.get( i );
          levels.set( i, levels.get(levels.size() -i -1));
          levels.set(levels.size() -i -1, tmp );
       } */
    }
  }

  boolean matchLevels( List records) {

    // first create a new list
    ArrayList levelList = new ArrayList( records.size());
    for (int i = 0; i < records.size(); i++) {
      Index.GribRecord record = (Index.GribRecord) records.get(i);
      Double d = new Double( record.levelValue1);
      if (!levelList.contains(d))
        levelList.add(d);
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
    ncfile.addDimension(g, new Dimension(getVariableName(), nlevs, true));
  }

  void addToNetcdfFile( NetcdfFile ncfile, Group g) {
    if (dontUseVertical) return;

    if (g == null)
      g = ncfile.getRootGroup();

    int nlevs = levels.size();

    // coordinate axis
    Variable v = new Variable( ncfile, g, null, getVariableName());
    v.setDataType( DataType.DOUBLE);

    v.addAttribute( new Attribute("long_name", lookup.getLevelDescription( record)));
    v.addAttribute( new Attribute("units", lookup.getLevelUnit( record)));

    // positive attribute needed for CF-1 Height and Pressure
    if (positive != null)
      v.addAttribute( new Attribute("positive", positive));

    if (units != null) {
      AxisType axis;
      if (SimpleUnit.isCompatible("millibar", units))
        axis = AxisType.Pressure;
      else if (SimpleUnit.isCompatible("m", units))
        axis = AxisType.Height;
      else
        axis = AxisType.GeoZ;

      v.addAttribute( new Attribute("GRIB_level_type", Integer.toString(record.levelType1)));
      v.addAttribute( new Attribute("_CoordinateAxisType", axis.toString()));
    }

    double[] data = new double[nlevs];
    for (int i = 0; i < levels.size(); i++) {
      Double d = (Double) levels.get(i);
      data[i] = d.doubleValue();
    }
    Array dataArray = Array.factory( DataType.DOUBLE.getClassType(), new int [] {nlevs}, data);

    v.setDimensions( getVariableName());
    v.setCachedData(dataArray, true);

    ncfile.addVariable( g, v);
  }

  int getIndex(Index.GribRecord record) {
    Double d = new Double( record.levelValue1);
    return levels.indexOf( d);
  }

  public int compareTo(Object o) {
    GribVertCoord gv = (GribVertCoord) o;
    return getLevelName().compareToIgnoreCase( gv.getLevelName());
  }
}