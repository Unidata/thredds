/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

