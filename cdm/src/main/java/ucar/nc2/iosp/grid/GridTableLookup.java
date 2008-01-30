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

// $Id: GridTableLookup.java,v 1.13 2006/08/03 22:32:59 rkambic Exp $

package ucar.nc2.iosp.grid;


/**
 * A table for gathering metadata about grids.  Specific Grid IOSP's
 * should implement.
 */
public interface GridTableLookup {

    /**
     * Polar Sterographic
     */
    public static final int PolarStereographic = 1;

    /**
     * Lambert Conformal
     */
    public static final int LambertConformal = 2;

    /**
     * Mercator
     */
    public static final int Mercator = 3;

    /**
     * Universal Transverse Mercator
     */
    public static final int UTM = 4;

    /**
     * Albers Equal Area
     */
    public static final int AlbersEqualArea = 5;

    /**
     * Lambert Azimuth Equal Area
     */
    public static final int LambertAzimuthEqualArea = 6;

    /**
     * Orthographic
     */
    public static final int Orthographic = 7;

    /**
     * Gausian Lat/Lon
     */
    public static final int GaussianLatLon = 8;

    /**
     * Get the grid name
     * @param gds  Grid definition record
     * @return GridName.
     */
    public String getGridName(GridDefRecord gds);

    /**
     * Get the grid shape name
     * @param gds  Grid definition record
     * @return ShapeName.
     */
    public String getShapeName(GridDefRecord gds);

    /**
     * Get the grid discipline name
     * @param gr  record to check
     * @return DisciplineName.
     */
    public String getDisciplineName(GridRecord gr);

    /**
     * Get the grid category name
     * @param gr  record to check
     * @return CategoryName.
     */
    public String getCategoryName(GridRecord gr);

    /**
     * Get the grid parameter that corresponds to this record
     * @param gr  record to check
     * @return Parameter.
     */
    public GridParameter getParameter(GridRecord gr);

    /**
     * Get the level name
     * @param gr  record to check
     * @return LevelName.
     */
    public String getLevelName(GridRecord gr);

    /**
     * Get the level description 
     * @param gr  record to check
     * @return LevelDescription.
     */
    public String getLevelDescription(GridRecord gr);

    /**
     * Get the level unit
     * @param gr GridRecord with metadata
     * @return LevelUnit.
     */
    public String getLevelUnit(GridRecord gr);

    /**
     * Get the first base time
     * @return FirstBaseTime.
     */
    public java.util.Date getFirstBaseTime();

    /**
     * Get the first time range unit name
     * @return the first time range unit name
     */
    public String getFirstTimeRangeUnitName();

    /**
     * Is this a lat/lon grid
     * @param gds  Grid definition record
     * @return is this a LatLon Grid
     */
    public boolean isLatLon(GridDefRecord gds);

    /**
     * If vertical level should be made into a coordinate; 
     * dont do for surface, 1D levels.
     * @param gr GridRecord with metadata
     * @return is this a VerticalCoordinate
     */
    public boolean isVerticalCoordinate(GridRecord gr);

    /**
     * Is postitive up for the vertical coordinate
     *
     * @param gr GridRecord with metadata
     * @return is this positive up level
     */
    public boolean isPositiveUp(GridRecord gr);

    /**
     * Get the projection type
     * @param gds  Grid definition record
     * @return one of the enumerated types
     */
    public int getProjectionType(GridDefRecord gds);

    /**
     * .
     * @return FirstMissingValue.
     */
    public float getFirstMissingValue();

    /**
     * Check to see if this grid is a layer variable
     *
     * @param gr  record to check
     *
     * @return  true if a layer
     */
    public boolean isLayer(GridRecord gr);

    /**
     * Get the grid type (GRIB, GEMPAK, McIDAS, GRADS) for labelling
     * @return the grid type
     */
    public String getGridType();

}

