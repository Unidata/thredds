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


package ucar.nc2.iosp.grid;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;
import ucar.grid.GridRecord;
import ucar.grid.GridTableLookup;

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
    private List<Double> levels;

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
        this.levels       = new ArrayList<Double>();

        dontUseVertical   = !lookup.isVerticalCoordinate(record);
        positive          = lookup.isPositiveUp(record)
                            ? "up"
                            : "down";
        units             = lookup.getLevelUnit(record);

        if (GridServiceProvider.debugVert) {
            System.out.println("GridCoordSys: " + getVerticalDesc()
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
    void addLevels(List<GridRecord> records) {
        for (GridRecord record : records) {
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
            // TODO: delete
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
    boolean matchLevels(List<GridRecord> records) {

        // first create a new list
    List<Double> levelList = new ArrayList<Double>( records.size());
    for ( GridRecord record : records) {
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
    List<Variable> vars = g.getVariables();
    for (Variable v : vars) {
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

