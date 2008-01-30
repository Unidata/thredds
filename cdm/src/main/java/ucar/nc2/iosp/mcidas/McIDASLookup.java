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


package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.nc2.iosp.grid.*;


/**
 * McIDASLookup
 * get all the information about a GEMPAK file.
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
        String unit = mgr.getParamUnitName();
        return new GridParameter(0, name, desc, unit);
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

    /**
     * Get the grid type for labelling
     * @return the grid type
     */
    public String getGridType() {
        return "McIDAS";
    }

}

