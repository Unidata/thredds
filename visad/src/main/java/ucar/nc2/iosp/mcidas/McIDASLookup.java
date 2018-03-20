/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.mcidas;

import edu.wisc.ssec.mcidas.McIDASUtil;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.iosp.grid.*;

/**
 * McIDASLookup
 * get all the information about a McIDAS file.
 * @author dmurray
 */
public final class McIDASLookup implements GridTableLookup {

  /**
   * sample grid
   */
  private McIDASGridRecord sample;
  private Grib1Customizer cust;

  /**
   * Gets a representative grid for this lookup
   */
  public McIDASLookup(McIDASGridRecord sample) {
    this.sample = sample;
    if (sample.hasGribInfo())
      cust = Grib1Customizer.factory(0, 0, 0, null); // WMO standard? Maybe they use NCEP. haha
  }

  public String getShapeName(GridDefRecord gds) {
    return "Spherical";
  }

  /**
   * gets the grid type.
   */
  public final String getGridName(GridDefRecord gds) {
    return gds.toString();
  }

  /**
   * gets parameter table, then grib1 parameter based on number.
   *
   * @param gr GridRecord
   * @return Parameter
   */
  public final GridParameter getParameter(GridRecord gr) {
    McIDASGridRecord mgr = (McIDASGridRecord) gr;
    String name = mgr.getParameterName();
    String desc = mgr.getGridDescription();
    if (desc.trim().equals("")) {
      desc = name;
    }
    String unit = visad.jmet.MetUnits.makeSymbol(mgr.getParamUnitName());
    return new GridParameter(0, name, desc, unit);
  }

  /**
   * gets the DisciplineName.
   */
  public final String getDisciplineName(GridRecord gr) {
    // all disciplines are the same in McIDAS
    return "Meteorological Products";
  }

  /**
   * gets the CategoryName.
   */
  public final String getCategoryName(GridRecord gr) {
    // no categories in McIDAS
    return "Meteorological Parameters";
  }

  /**
   * gets the LevelName.
   */
  public final String getLevelName(GridRecord gr) {

    if (cust != null) {
      String result = cust.getLevelNameShort( gr.getLevelType1());
      if (result != null) return result;
    }

    String levelUnit = getLevelUnit(gr);
    if (levelUnit != null) {
      int level1 = (int) gr.getLevel1();
      int level2 = (int) gr.getLevel2();
      if (levelUnit.equalsIgnoreCase("hPa")) {
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
    }

    return "";
  }

  /**
   * gets the LevelDescription.
   */
  public final String getLevelDescription(GridRecord gr) {
    if (cust != null) {
      String result = cust.getLevelDescription( gr.getLevelType1());
      if (result != null) return result;
    }

    // TODO:  flesh this out
    return getLevelName(gr);
  }

  /**
   * gets the LevelUnit.
   */
  public final String getLevelUnit(GridRecord gr) {
    if (cust != null) {
      String result = cust.getLevelUnits( gr.getLevelType1());
      if (result != null) return result;
    }

    return visad.jmet.MetUnits.makeSymbol(((McIDASGridRecord) gr).getLevelUnitName());
  }

  public final String getTimeRangeUnitName(int tunit) {
    return "hour";
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
    return getProjectionName(gds).equals("EQUI");
  }

  /**
   * gets the ProjectionType.
   */
  public final int getProjectionType(GridDefRecord gds) {
    String name = getProjectionName(gds).trim();
    switch (name) {
      case "MERC":
        return Mercator;
      case "CONF":
        return LambertConformal;
      case "PS":
        return PolarStereographic;
      default:
        return -1;
    }
  }

  /**
   * is this a VerticalCoordinate.
   */
  public final boolean isVerticalCoordinate(GridRecord gr) {
    if (cust != null) {
      return cust.isVerticalCoordinate(gr.getLevelType1());
    }

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
   */
  public final boolean isPositiveUp(GridRecord gr) {
    if (cust != null) {
      return cust.isPositiveUp( gr.getLevelType1());
    }

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
   *
   * @return MissingValue
   */
  public final float getFirstMissingValue() {
    return McIDASUtil.MCMISSING;
  }

  /**
   * Is this a layer?
   *
   * @param gr record to check
   * @return true if a layer
   */
  public boolean isLayer(GridRecord gr) {
    if (cust != null) {
      return cust.isLayer( gr.getLevelType1());
    }

    if (gr.getLevel2() == 0) {
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
    return "McIDAS";
  }

}

