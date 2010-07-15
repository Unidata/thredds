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


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.grid.GridDefRecord;
import ucar.grid.GridParameter;
import ucar.grid.GridRecord;
import ucar.grid.GridTableLookup;


/**
 * McIDASLookup
 * get all the information about a McIDAS file.
 */
public final class McIDASLookup implements GridTableLookup {

    /** sample grid */
    private McIDASGridRecord sample;

    /**
     *
     * Gets a representative grid for this lookup
     * @param sample
     */
    public McIDASLookup(McIDASGridRecord sample) {
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
        return gds.toString();
    }

    /**
     * gets parameter table, then grib1 parameter based on number.
     * @param gr GridRecord
     * @return Parameter
     */
    public final GridParameter getParameter(GridRecord gr) {
        McIDASGridRecord mgr  = (McIDASGridRecord) gr;
        String           name = mgr.getParameterName();
        String           desc = mgr.getGridDescription();
        if (desc.trim().equals("")) {
            desc = name;
        }
        String unit = visad.jmet.MetUnits.makeSymbol(mgr.getParamUnitName());
        return new GridParameter(0, name, desc, unit);
    }

    /**
     * gets the DisciplineName.
     * @param  gr
     * @return DisciplineName
     */
    public final String getDisciplineName(GridRecord gr) {
        // all disciplines are the same in McIDAS
        return "Meteorological Products";
    }

    /**
     * gets the CategoryName.
     * @param  gr
     * @return CategoryName
     */
    public final String getCategoryName(GridRecord gr) {
        // no categories in McIDAS
        return "Meteorological Parameters";
    }

    /**
     * gets the LevelName.
     * @param  gr
     * @return LevelName
     */
    public final String getLevelName(GridRecord gr) {
        // TODO:  flesh this out
        String levelUnit = getLevelUnit(gr);
        int    level1    = (int) gr.getLevel1();
        int    level2    = (int) gr.getLevel2();
        int    levelType = gr.getLevelType1();
        if (((McIDASGridRecord) gr).hasGribInfo()) {
            return ucar.grib.grib1.GribPDSLevel.getNameShort(levelType);
        } else if (levelUnit.equalsIgnoreCase("hPa")) {
            return "pressure";
        } else if (level1 == 1013) {
            return "mean sea level";
        } else if (level1 == 0) {
            return "tropopause";
        } else if (level1 == 1001) {
            return "surface";
        } else if (level2 != 0) {
            return "layer";
        }
        return "";
    }

    /**
     * gets the LevelDescription.
     * @param  gr
     * @return LevelDescription
     */
    public final String getLevelDescription(GridRecord gr) {
        // TODO:  flesh this out
        if (((McIDASGridRecord) gr).hasGribInfo()) {
            return ucar.grib.grib1.GribPDSLevel.getNameShort(
                gr.getLevelType1());
        }
        return getLevelName(gr);
    }

    /**
     * gets the LevelUnit.
     * @param  gr
     * @return LevelUnit
     */
    public final String getLevelUnit(GridRecord gr) {


        return visad.jmet.MetUnits.makeSymbol(
            ((McIDASGridRecord) gr).getLevelUnitName());
    }

    /**
     * gets the TimeRangeUnitName.
     * @return TimeRangeUnitName
     */
    public final String getFirstTimeRangeUnitName() {
        return "hour";
    }
    public final String getTimeRangeUnitName( int tunit ) {
        return "hour";
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
        return getProjectionName(gds).equals("EQUI");
    }

    /**
     * gets the ProjectionType.
     * @param  gds
     * @return ProjectionType
     */
    public final int getProjectionType(GridDefRecord gds) {
        String name = getProjectionName(gds).trim();
        if (name.equals("MERC")) {
            return Mercator;
        } else if (name.equals("CONF")) {
            return LambertConformal;
        } else if (name.equals("PS")) {
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
        if (((McIDASGridRecord) gr).hasGribInfo()) {
            if (type == 20) {
                return true;
            }
            if (type == 100) {
                return true;
            }
            if (type == 101) {
                return true;
            }
            if ((type >= 103) && (type <= 128)) {
                return true;
            }
            if (type == 141) {
                return true;
            }
            if (type == 160) {
                return true;
            }
        } else if (getLevelUnit(gr).equals("hPa")) {
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
        if (((McIDASGridRecord) gr).hasGribInfo()) {
            if (type == 103) {
                return true;
            }
            if (type == 104) {
                return true;
            }
            if (type == 105) {
                return true;
            }
            if (type == 106) {
                return true;
            }
            if (type == 111) {
                return true;
            }
            if (type == 112) {
                return true;
            }
            if (type == 125) {
                return true;
            } else if (getLevelUnit(gr).equals("hPa")) {
                return false;
            }
        }
        return true;
    }

    /**
     * gets the MissingValue.
     * @return MissingValue
     */
    public final float getFirstMissingValue() {
        return McIDASUtil.MCMISSING;
    }

    /**
     * Is this a layer?
     *
     * @param gr  record to check
     *
     * @return true if a layer
     */
    public boolean isLayer(GridRecord gr) {
        if (gr.getLevel2() == 0) {
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
      return "McIDAS";
    }

}

