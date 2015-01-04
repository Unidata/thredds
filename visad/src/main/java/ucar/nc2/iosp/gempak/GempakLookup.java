/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package ucar.nc2.iosp.gempak;

import ucar.nc2.iosp.grid.*;


/**
 * GempakLookup
 * get all the information about a GEMPAK file.
 */
public final class GempakLookup implements GridTableLookup {

  /**
   * sample grid
   */
  private GempakGridRecord sample;

  /**
   * Gets a representative grid for this lookup
   */
  public GempakLookup(GempakGridRecord sample) {
    this.sample = sample;
  }

  public String getShapeName(GridDefRecord gds) {
    return "Spherical";
  }

  public final String getGridName(GridDefRecord gds) {
    return getProjectionName(gds);
  }

  /**
   * gets parameter table, then grib1 parameter based on number.
   *
   * @param gr GridRecord
   * @return Parameter
   */
  public final GridParameter getParameter(GridRecord gr) {
    String name = gr.getParameterName();
    GridParameter gp = GempakGridParameterTable.getParameter(name);
    if (gp != null) {
      return gp;
    }
    return new GridParameter(0, name, name, "");
  }

  public final String getDisciplineName(GridRecord gr) {
    // all disciplines are the same in GEMPAK
    return "Meteorological Products";
  }

  public final String getCategoryName(GridRecord gr) {
    // no categories in GEMPAK
    return "Meteorological Parameters";
  }

  public final String getLevelName(GridRecord gr) {
    // TODO:  flesh this out
    return GempakUtil.LV_CCRD(gr.getLevelType1());
  }

  public final String getLevelDescription(GridRecord gr) {
    // TODO:  flesh this out
    String levelName = getLevelName(gr);
    switch (levelName) {
      case "PRES":
        return "pressure";
      case "NONE":
        return "surface";
      case "HGHT":
        return "height_above_ground";
      case "THTA":
        return "isentropic";
      case "SGMA":
        return "sigma";
      case "DPTH":
        return "depth";
      case "PDLY":
        return "layer_between_two_pressure_difference_from_ground";
      case "FRZL":
        return "zeroDegC_isotherm";
      case "TROP":
        return "tropopause";
      case "CLDL":
        return "cloud_base";
      case "CLDT":
        return "cloud_tops";
      case "MWSL":
        return "maximum_wind_level";
    }
    return levelName;
  }

  public final String getLevelUnit(GridRecord gr) {
    // TODO:  flesh this out
    String levelName = getLevelName(gr);
    switch (levelName) {
      case "PRES":
        return "hPa";
      case "HGHT":
        return "m";
      case "THTA":
        return "K";
      case "SGMA":
        return "";
      case "DPTH":
        return "m";
      case "PDLY":
        return "hPa";
    }
    return "";
  }

  public final String getTimeRangeUnitName(int tunit) {
    return "minute";
  }

  /**
   * gets the BaseTime Forecastime.
   *
   * @return BaseTime
   */
  public final java.util.Date getFirstBaseTime() {
    return sample.getReferenceTime();
  }

  /**
   * is this a LatLon grid.
   */
  public final boolean isLatLon(GridDefRecord gds) {
    return getProjectionName(gds).equals("CED");
  }

  public final int getProjectionType(GridDefRecord gds) {
    String name = getProjectionName(gds).trim();
    switch (name) {
      case "CED":
        return -1;
      case "MER":
        return Mercator;
      case "MCD":
        return Mercator;
      case "LCC":
        return LambertConformal;
      case "SCC":
        return LambertConformal;
      case "PS":
        return PolarStereographic;
      case "STR":
        return PolarStereographic;
      default:
        return -1;
    }
  }

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
   *
   * @return MissingValue
   */
  public final float getFirstMissingValue() {
    return GempakConstants.RMISSD;
  }

  /**
   * Is this a layer?
   *
   * @param gr record to check
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
   * @param gds the projection name
   * @return the name or null if not set
   */
  private String getProjectionName(GridDefRecord gds) {
    return gds.getParam(GridDefRecord.PROJ);
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

