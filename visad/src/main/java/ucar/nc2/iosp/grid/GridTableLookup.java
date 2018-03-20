/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: GridTableLookup.java,v 1.13 2006/08/03 22:32:59 rkambic Exp $

package ucar.nc2.iosp.grid;


/**
 * Abstracts lookup functionality for subclasses of Grid IOSP
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
   * Rotated Latitude Longitude
   */
    public static final int RotatedLatLon = 10;

  /**
   * NCEP Curvilinear - needs 2D lat/lon
   */
    public static final int Curvilinear = 100;

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
    public String getTimeRangeUnitName( int tunit );

    /**
     * Is this a lat/lon grid
     * @param gds  Grid definition record
     * @return is this a LatLon Grid
     */
    public boolean isLatLon(GridDefRecord gds);

    /**
     * If vertical level should be made into a coordinate; 
     * dont do for surface, or levels without a meaningful coordinate value
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

    // CF-conventions Global variables

    /**
     * Title for CF conventions
     *
     * @return Title
     */
    public String getTitle();

    /**
     * Institution for CF conventions
     *
     * @return Institution
     */
    public String getInstitution();

    /**
     * Generating Process of model for CF conventions
     *
     * @return source
     */
    public String getSource();

    /**
     * Comment for CF conventions
     *
     * @return comment
     */
    public String getComment();

    /**
     * Get the grid type (GRIB, GEMPAK, McIDAS, GRADS) for labelling
     *
     * @return the grid type
     */
    public String getGridType();

}

