// $Id:GribCoordSys.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import ucar.nc2.dataset.conv._Coordinate;
import ucar.grib.Index;
import ucar.grib.TableLookup;

import java.util.*;

/**
 * A Coordinate System for a Grib variable.
 * @author john
 */
public class GribCoordSys {
  private GribHorizCoordSys hcs;
  private Index.GribRecord record;
  private String verticalName;
  private TableLookup lookup;

  private List levels;
  boolean dontUseVertical = false;
  String positive = "up";
  String units;

  GribCoordSys(GribHorizCoordSys hcs, Index.GribRecord record, String name, TableLookup lookup) {
    this.hcs = hcs;
    this.record = record;
    this.verticalName = name;
    this.lookup = lookup;
    this.levels = new ArrayList();

    dontUseVertical = !lookup.isVerticalCoordinate( record);
    positive = lookup.isPositiveUp(record) ? "up" :"down";
    units = lookup.getLevelUnit( record);

    if (GribServiceProvider.debugVert)
      System.out.println("GribCoordSys: "+getVerticalDesc()+" useVertical= "+
          (!dontUseVertical)+" positive="+positive+" units="+units);
  }

  String getCoordSysName() { return verticalName+"_CoordSys"; }
  String getVerticalName() { return verticalName; }
  String getVerticalDesc() { return verticalName+"("+record.levelType1+")"; }
  int getNLevels() { return dontUseVertical ? 1 : levels.size(); }

  void addLevels( List records) {
    for (int i = 0; i < records.size(); i++) {
      Index.GribRecord record = (Index.GribRecord) records.get(i);
      Double d = new Double( record.levelValue1);
      if (!levels.contains(d))
        levels.add(d);
      if (dontUseVertical && levels.size() > 1) {
        if (GribServiceProvider.debugVert)
          System.out.println("GribCoordSys: unused level coordinate has > 1 levels = "+verticalName+" "+record.levelType1+" "+levels.size());
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
    ncfile.addDimension(g, new Dimension(verticalName, nlevs, true));
  }

  void addToNetcdfFile( NetcdfFile ncfile, Group g) {
    if (dontUseVertical) return;

    if (g == null)
      g = ncfile.getRootGroup();

    String dims = "time";
    if (!dontUseVertical)
      dims = dims + " " + verticalName;
    if (hcs.isLatLon())
      dims = dims + " lat lon";
    else
      dims = dims + " y x";

    //Collections.sort( levels);
    int nlevs = levels.size();
    // ncfile.addDimension(g, new Dimension(verticalName, nlevs, true));

    // coordinate axis and coordinate system Variable
    Variable v = new Variable( ncfile, g, null, verticalName);
    v.setDataType( DataType.DOUBLE);

    v.addAttribute( new Attribute("long_name", lookup.getLevelDescription( record)));
    v.addAttribute( new Attribute("units", lookup.getLevelUnit( record)));

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

      v.addAttribute( new Attribute("GRIB_level_type", Integer.toString(record.levelType1)));

      v.addAttribute( new Attribute(_Coordinate.AxisType, axisType.toString()));
      v.addAttribute( new Attribute(_Coordinate.Axes, dims));
      if (!hcs.isLatLon())
        v.addAttribute( new Attribute(_Coordinate.Transforms, hcs.getGridName()));
    }

    double[] data = new double[nlevs];
    for (int i = 0; i < levels.size(); i++) {
      Double d = (Double) levels.get(i);
      data[i] = d.doubleValue();
    }
    Array dataArray = Array.factory( DataType.DOUBLE.getClassType(), new int [] {nlevs}, data);

    v.setDimensions( verticalName);
    v.setCachedData(dataArray, false);

    ncfile.addVariable( g, v);

    // look for vertical transforms
    if (record.levelType1 == 109) {
      findCoordinateTransform (g, "Pressure", record.levelType1);
    }
  }

  void findCoordinateTransform (Group g, String nameStartsWith, int levelType) {
    // look for variable that uses this coordinate
    List vars = g.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v.getName().equals(nameStartsWith)) {
        Attribute att = v.findAttribute("GRIB_level_type");
        if ((att == null) || (att.getNumericValue().intValue() != levelType)) continue;

        v.addAttribute( new Attribute(_Coordinate.TransformType, "Vertical"));
        v.addAttribute( new Attribute("transform_name", "Existing3DField"));
      }
    }
  }

  int getIndex(Index.GribRecord record) {
    Double d = new Double( record.levelValue1);
    return levels.indexOf( d);
  }
}

/* Change History:
   $Log: GribCoordSys.java,v $
   Revision 1.7  2005/05/17 21:04:13  caron
   grib dense coordinate system

   Revision 1.6  2005/03/11 23:02:13  caron
   *** empty log message ***

   Revision 1.5  2005/03/03 20:52:27  caron
   datatype checkin

   Revision 1.4  2005/02/02 16:34:53  caron
   *** empty log message ***

   Revision 1.3  2005/01/28 15:34:32  rkambic
   sort levels according to positiveUP

   Revision 1.2  2004/12/07 01:29:31  caron
   redo convention parsing, use _Coordinate encoding.

   Revision 1.1  2004/12/01 05:53:42  caron
   ncml pass 2, new convention parsing

   Revision 1.6  2004/11/11 23:54:13  rkambic
   level stuff

   Revision 1.5  2004/10/29 00:14:11  caron
   no message

   Revision 1.4  2004/10/12 02:57:06  caron
   refactor for grib1/grib2: move common functionality up to ucar.grib
   split GribServiceProvider

   Revision 1.3  2004/10/02 20:54:41  caron
   *** empty log message ***

   Revision 1.2  2004/09/30 20:49:07  caron
   *** empty log message ***

   Revision 1.1  2004/09/30 00:33:41  caron
   *** empty log message ***

*/