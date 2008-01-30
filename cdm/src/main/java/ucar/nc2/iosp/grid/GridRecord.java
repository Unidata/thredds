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


import java.util.Date;


/**
 * An interface for handling information about a 2D grid (eg:  GRIB, GEMPAK,
 * McIDAS grid)
 */
public interface GridRecord {

    /**
     * Get the first level of this GridRecord
     *
     * @return the first level value
     */
    public double getLevel1();

    /**
     * Get the second level of this GridRecord
     *
     * @return the second level value
     */
    public double getLevel2();

    /**
     * Get the level type of this GridRecord
     *
     * @return level type
     */
    public int getLevelType1();

    /**
     * Get the level type of this GridRecord
     *
     * @return level type
     */
    public int getLevelType2();

    /**
     * Get the first reference time of this GridRecord
     *
     * @return reference time
     */
    public Date getReferenceTime();

    /**
     * Get the valid time for this grid.
     *
     * @return valid time
     */
    public Date getValidTime();

    /**
     * Get valid time offset of this GridRecord
     *
     * @return time offset
     */
    public int getValidTimeOffset();

    /**
     * Get the parameter name
     *
     * @return parameter name
     */
    public String getParameterName();

    /**
     * Get the grid def record id
     *
     * @return parameter name
     */
    public String getGridDefRecordId();

    /**
     * Get the decimal scale of the values
     *
     * @return decimal scale
     */
    public int getDecimalScale();

}

