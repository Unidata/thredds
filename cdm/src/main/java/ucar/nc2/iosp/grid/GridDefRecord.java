/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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

