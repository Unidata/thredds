/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


import java.util.HashMap;


/**
 * Class to represent the grid definition (projection) information
 * purpose is to convert from String representation to native value.
 */
public abstract class GridDefRecord {

    /** A class to hold grid parameter definitions */
    private HashMap params = new HashMap();

    // enumerations for common variables 

    /** GDS key */
    public static final String GDS_KEY = "gdsKey";

    /** Wind flag */
    public static final String WIND_FLAG = "wind_flag";

    /** grid type */
    public static final String GRID_TYPE = "grid_type";

    /** number of points in X direction (columns) */
    public static final String NX = "Nx";

    /** number of points in Y direction (rows) */
    public static final String NY = "Ny";

    /** distance in X direction */
    public static final String DX = "Dx";

    /** distance in Y direction */
    public static final String DY = "Dy";

    /** resolution */
    public static final String RESOLUTION = "resolution";

    /** first lat */
    public static final String LATIN1 = "latin1";

    /** second lat */
    public static final String LATIN2 = "latin2";

    /** La1 */
    public static final String LA1 = "La1";

    /** Lo1 */
    public static final String LO1 = "Lo1";

    /** La2 */
    public static final String LA2 = "La2";

    /** Lo2 */
    public static final String LO2 = "Lo2";

    /** LoD */
    public static final String LAD = "LaD";

    /** LoV */
    public static final String LOV = "LoV";

    /** LoV */
    public static final String PROJ = "Proj";

    /** LoV */
    public static final String GRID_SHAPE_CODE = "grid_shape_code";

    /** Radius of spherical earth */
    public static final String RADIUS_SPHERICAL_EARTH =
        "radius_spherical_earth";

    /** major axis of earth */
    public static final String MAJOR_AXIS_EARTH = "major_axis_earth";

    /** minor axis of earth */
    public static final String MINOR_AXIS_EARTH = "minor_axis_earth";

    /* TODO:  The following are commented out from the original
     * Index.GdsRecord.  I've moved to using the hashtable to
     * store these, but would could consider what are the bare minimum
     * methods we need for all 2D grids
     *
     * public String gdsKey, winds;
     * public int grid_type, nx, ny, resolution;
     * public double dx, dy;
     * public double latin1, latin2, La1, Lo1, LaD, LoV;
     * public int grid_shape_code;
     * public double radius_spherical_earth, major_axis_earth, minor_axis_earth;
     */

    /**
     * constructor.
     */
    public GridDefRecord() {}

    /**
     * adds a param and value.
     * @param key name of the param
     * @param value of the param
     */
    public final void addParam(String key, String value) {
        //System.out.println(" adding " + key + " = " + value);
        params.put(key.trim(), value);
    }

    /**
     * adds a param and value.
     * @param key name of the param
     * @return the value or null
     */
    public final String getParam(String key) {
        return (String) params.get(key.trim());
    }

    /**
     * Get a short name for this GDSKey for the netCDF group.  
     * Subclasses should implement as a short description
     * @return short name
     */
    public abstract String getGroupName();

    /**
     * get the keySet
     * @return the set of keys
     */
    public final java.util.Set getKeys() {
        return params.keySet();
    }

    /**
     * returns the value of the param.
     * @param name
     * @return value, or NaN if value doest exist
     */
    public final double readDouble(String name) {
        String s = (String) params.get(name);
        if (s == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }
}

