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


package ucar.nc2.iosp.grid;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;

import java.util.*;


/**
 * A Coordinate System for a Grid variable.
 * @author john
 */
public class GridCoordSys {

    /** horizontal coordinate system */
    private GridHorizCoordSys hcs;

    /** a grid record */
    private GridRecord record;

    /** vertical name */
    private String verticalName;

    /** lookup table */
    private GridTableLookup lookup;

    /** list of levels */
    private List levels;

    /** flag for not using the vertical */
    boolean dontUseVertical = false;

    /** postive direction */
    String positive = "up";

    /** units */
    String units;

    /**
     * Create a coordinate system 
     *
     * @param hcs      horizontal coord sys
     * @param record   grid record
     * @param name     name of the vertical dimension
     * @param lookup   lookuptable
     */
    GridCoordSys(GridHorizCoordSys hcs, GridRecord record, String name,
                 GridTableLookup lookup) {
        this.hcs          = hcs;
        this.record       = record;
        this.verticalName = name;
        this.lookup       = lookup;
        this.levels       = new ArrayList();

        dontUseVertical   = !lookup.isVerticalCoordinate(record);
        positive          = lookup.isPositiveUp(record)
                            ? "up"
                            : "down";
        units             = lookup.getLevelUnit(record);

        if (GridServiceProvider.debugVert) {
            System.out.println("GribCoordSys: " + getVerticalDesc()
                               + " useVertical= " + ( !dontUseVertical)
                               + " positive=" + positive + " units=" + units);
        }
    }

    /**
     * Get the coordinate system name
     *
     * @return the coordinate system name
     */
    String getCoordSysName() {
        return verticalName + "_CoordSys";
    }

    /**
     * Get the vertical name
     *
     * @return the vertical name
     */
    String getVerticalName() {
        return verticalName;
    }

    /**
     * Get the vertical description
     *
     * @return the vertical description
     */
    String getVerticalDesc() {
        return verticalName + "(" + record.getLevelType1() + ")";
    }

    /**
     * Get the number of levels
     *
     * @return the number of levels
     */
    int getNLevels() {
        return dontUseVertical
               ? 1
               : levels.size();
    }

    /**
     * Add levels from the GridRecords
     *
     * @param records  the records
     */
    void addLevels(List records) {
        for (int i = 0; i < records.size(); i++) {
            GridRecord record = (GridRecord) records.get(i);
            Double     d      = new Double(record.getLevel1());
            if ( !levels.contains(d)) {
                levels.add(d);
            }
            if (dontUseVertical && (levels.size() > 1)) {
                if (GridServiceProvider.debugVert) {
                    System.out.println(
                        "GribCoordSys: unused level coordinate has > 1 levels = "
                        + verticalName + " " + record.getLevelType1() + " "
                        + levels.size());
                }
            }
        }
        Collections.sort(levels);
        if (positive.equals("down")) {
            Collections.reverse(levels);
            /* for( int i = 0; i < (levels.size()/2); i++ ){
                Double tmp = (Double) levels.get( i );
                levels.set( i, levels.get(levels.size() -i -1));
                levels.set(levels.size() -i -1, tmp );
             } */
        }
    }

    /**
     * Match levels
     *
     * @param records list of GridRecords
     *
     * @return true if they match
     */
    boolean matchLevels(List records) {

        // first create a new list
        ArrayList levelList = new ArrayList(records.size());
        for (int i = 0; i < records.size(); i++) {
            GridRecord record = (GridRecord) records.get(i);
            Double     d      = new Double(record.getLevel1());
            if ( !levelList.contains(d)) {
                levelList.add(d);
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
     * Add dimensions to the netcdf file
     *
     * @param ncfile  the netCDF file
     * @param g       the group to add to
     */
    void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
        if (dontUseVertical) {
            return;
        }
        int nlevs = levels.size();
        ncfile.addDimension(g, new Dimension(verticalName, nlevs, true));
    }

    /**
     * Add this coordinate system to the netCDF file
     *
     * @param ncfile the netCDF file
     * @param g the group to add to
     */
    void addToNetcdfFile(NetcdfFile ncfile, Group g) {
        if (dontUseVertical) {
            return;
        }

        if (g == null) {
            g = ncfile.getRootGroup();
        }

        String dims = "time";
        if ( !dontUseVertical) {
            dims = dims + " " + verticalName;
        }
        if (hcs.isLatLon()) {
            dims = dims + " lat lon";
        } else {
            dims = dims + " y x";
        }

        //Collections.sort( levels);
        int nlevs = levels.size();
        // ncfile.addDimension(g, new Dimension(verticalName, nlevs, true));

        // coordinate axis and coordinate system Variable
        Variable v = new Variable(ncfile, g, null, verticalName);
        v.setDataType(DataType.DOUBLE);

        v.addAttribute(new Attribute("long_name",
                                     lookup.getLevelDescription(record)));
        v.addAttribute(new Attribute("units", lookup.getLevelUnit(record)));

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

            v.addAttribute(
                new Attribute(
                    "grid_level_type",
                    Integer.toString(record.getLevelType1())));

            v.addAttribute(new Attribute(_Coordinate.AxisType,
                                         axisType.toString()));
            v.addAttribute(new Attribute(_Coordinate.Axes, dims));
            if ( !hcs.isLatLon()) {
                v.addAttribute(new Attribute(_Coordinate.Transforms,
                                             hcs.getGridName()));
            }
        }

        double[] data = new double[nlevs];
        for (int i = 0; i < levels.size(); i++) {
            Double d = (Double) levels.get(i);
            data[i] = d.doubleValue();
        }
        Array dataArray = Array.factory(DataType.DOUBLE, new int[] { nlevs }, data);

        v.setDimensions(verticalName);
        v.setCachedData(dataArray, false);

        ncfile.addVariable(g, v);

        // look for vertical transforms
        if (record.getLevelType1() == 109) {
            findCoordinateTransform(g, "Pressure", record.getLevelType1());
        }
    }

    /**
     * Find the coordinate transform
     *
     * @param g     group to check
     * @param nameStartsWith   beginning of name
     * @param levelType  type of level
     */
    void findCoordinateTransform(Group g, String nameStartsWith,
                                 int levelType) {
        // look for variable that uses this coordinate
        List vars = g.getVariables();
        for (int i = 0; i < vars.size(); i++) {
            Variable v = (Variable) vars.get(i);
            if (v.getName().equals(nameStartsWith)) {
                Attribute att = v.findAttribute("grid_level_type");
                if ((att == null)
                        || (att.getNumericValue().intValue() != levelType)) {
                    continue;
                }

                v.addAttribute(new Attribute(_Coordinate.TransformType,
                                             "Vertical"));
                v.addAttribute(new Attribute("transform_name",
                                             "Existing3DField"));
            }
        }
    }

    /**
     * Get the index of a particular  GridRecord
     *
     * @param record  record to find
     *
     * @return the index or -1
     */
    int getIndex(GridRecord record) {
        Double d = new Double(record.getLevel1());
        return levels.indexOf(d);
    }
}

