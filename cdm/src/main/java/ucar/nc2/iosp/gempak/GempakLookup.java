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


package ucar.nc2.iosp.gempak;


import ucar.grid.GridDefRecord;
import ucar.grid.GridParameter;
import ucar.grid.GridRecord;

//import ucar.nc2.iosp.grid.GridDefRecord;
//import ucar.nc2.iosp.grid.GridParameter;
//import ucar.nc2.iosp.grid.GridRecord;
//import ucar.nc2.iosp.grid.GridTableLookup;
import ucar.grid.GridTableLookup;

import java.io.IOException;


/**
 * GempakLookup
 * get all the information about a GEMPAK file.
 */
public final class GempakLookup implements GridTableLookup {

    /** sample grid */
    private GempakGridRecord sample;

    /**
     *
     * Gets a representative grid for this lookup
     * @param sample
     */
    public GempakLookup(GempakGridRecord sample) {
        this.sample = sample;
    }

    /**
     * .
     * @param gds
     * @return ShapeName.
     */
    public String getShapeName(GridDefRecord gds) {
        return "Spherical";
    }

    /**
     * gets the grid type.
     * @param gds
     * @return GridName
     */
    public final String getGridName(GridDefRecord gds) {
        return getProjectionName(gds);
    }

    /**
     * gets parameter table, then grib1 parameter based on number.
     * @param gr GridRecord
     * @return Parameter
     */
    public final GridParameter getParameter(GridRecord gr) {
        String        name = gr.getParameterName();
        GridParameter gp   = GempakGridParameterTable.getParameter(name);
        if (gp != null) {
            return gp;
        }
        return new GridParameter(0, name, name, "");
    }

    /**
     * gets the DisciplineName.
     * @param  gr
     * @return DisciplineName
     */
    public final String getDisciplineName(GridRecord gr) {
        // all disciplines are the same in GEMPAK
        return "Meteorological Products";
    }

    /**
     * gets the CategoryName.
     * @param  gr
     * @return CategoryName
     */
    public final String getCategoryName(GridRecord gr) {
        // no categories in GEMPAK
        return "Meteorological Parameters";
    }

    /**
     * gets the LevelName.
     * @param  gr
     * @return LevelName
     */
    public final String getLevelName(GridRecord gr) {
        // TODO:  flesh this out
        String levelName = GempakUtil.LV_CCRD(gr.getLevelType1());
        return levelName;
    }

    /**
     * gets the LevelDescription.
     * @param  gr
     * @return LevelDescription
     */
    public final String getLevelDescription(GridRecord gr) {
        // TODO:  flesh this out
        String levelName = getLevelName(gr);
        if (levelName.equals("PRES")) {
            return "pressure";
        } else if (levelName.equals("NONE")) {
            return "surface";
        } else if (levelName.equals("HGHT")) {
            return "height_above_ground";
        } else if (levelName.equals("THTA")) {
            return "isentropic";
        } else if (levelName.equals("SGMA")) {
            return "sigma";
        } else if (levelName.equals("DPTH")) {
            return "depth";
        } else if (levelName.equals("PDLY")) {
            return "layer_between_two_pressure_difference_from_ground";
        } else if (levelName.equals("FRZL")) {
            return "zeroDegC_isotherm";
        } else if (levelName.equals("TROP")) {
            return "tropopause";
        } else if (levelName.equals("CLDL")) {
            return "cloud_base";
        } else if (levelName.equals("CLDT")) {
            return "cloud_tops";
        } else if (levelName.equals("MWSL")) {
            return "maximum_wind_level";
        }
        return levelName;
    }

    /**
     * gets the LevelUnit.
     * @param  gr
     * @return LevelUnit
     */
    public final String getLevelUnit(GridRecord gr) {
        // TODO:  flesh this out
        String levelName = getLevelName(gr);
        if (levelName.equals("PRES")) {
            return "hPa";
        } else if (levelName.equals("HGHT")) {
            return "m";
        } else if (levelName.equals("THTA")) {
            return "K";
        } else if (levelName.equals("SGMA")) {
            return "";
        } else if (levelName.equals("DPTH")) {
            return "m";
        } else if (levelName.equals("PDLY")) {
            return "hPa";
        }
        return "";
    }

    /**
     * gets the TimeRangeUnitName.
     * @return TimeRangeUnitName
     */
    public final String getFirstTimeRangeUnitName() {
        return "minute";
    }
    public final String getTimeRangeUnitName( int tunit ) {
        return "minute";
    }

    /**
     * gets the BaseTime Forecastime.
     * @return BaseTime
     */
    public final java.util.Date getFirstBaseTime() {
        return sample.getReferenceTime();
    }

    /**
     * is this a LatLon grid.
     * @param  gds
     * @return isLatLon
     */
    public final boolean isLatLon(GridDefRecord gds) {
        return getProjectionName(gds).equals("CED");
    }

    /**
     * gets the ProjectionType.
     * @param  gds
     * @return ProjectionType
     */
    public final int getProjectionType(GridDefRecord gds) {
        String name = getProjectionName(gds).trim();
        if (name.equals("CED")) {
            return -1;
        } else if (name.equals("MER")) {
            return Mercator;
        } else if (name.equals("MCD")) {
            return Mercator;
        } else if (name.equals("LCC")) {
            return LambertConformal;
        } else if (name.equals("SCC")) {
            return LambertConformal;
        } else if (name.equals("PS")) {
            return PolarStereographic;
        } else if (name.equals("STR")) {
            return PolarStereographic;
        } else {
            return -1;
        }
    }

    /**
     * is this a VerticalCoordinate.
     * @param  gr
     * @return isVerticalCoordinate
     */
    public final boolean isVerticalCoordinate(GridRecord gr) {
        int type = gr.getLevelType1();
        if ((type > GempakUtil.vertCoords.length)
                || !GempakUtil.vertCoords[type].equals("NONE")) {
            return true;
        }
        return false;
    }

    /**
     * is this a PositiveUp VerticalCoordinate.
     * @param  gr
     * @return isPositiveUp
     */
    public final boolean isPositiveUp(GridRecord gr) {
        int type = gr.getLevelType1();
        if ((type == 1) || (type == 5)) {
            return false;
        }
        return true;
    }

    /**
     * gets the MissingValue.
     * @return MissingValue
     */
    public final float getFirstMissingValue() {
        return GempakConstants.RMISSD;
    }

    /**
     * Is this a layer?
     *
     * @param gr  record to check
     *
     * @return true if a layer
     */
    public boolean isLayer(GridRecord gr) {
        if (gr.getLevel2() == -1) {
            return false;
        }
        return true;
    }

    /**
     * Get the projection name
     *
     * @param gds  the projection name
     *
     * @return the name or null if not set
     */
    private String getProjectionName(GridDefRecord gds) {
        return gds.getParam(gds.PROJ);
    }

    // CF Conventions Global Attributes

    /**
     * gets the CF title.
     *
     * @return title
     */
    public final String getTitle() {
      return "GRID data";
    }

    /**
     * Institution for CF conventions
     *
     * @return Institution
     */
    public String getInstitution() {
      return null;

    }

    /**
     * gets the Source, Generating Process or Model.
     *
     * @return source
     */
    public final String getSource() {
      return null;

    }

    /**
     * comment for CF conventions.
     *
     * @return comment
     */
    public final String getComment() {
      return null;
    }

    /**
     * Get the grid type for labelling
     *
     * @return the grid type
     */
    public String getGridType() {
      return "GEMPAK";
    }

}

