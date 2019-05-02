package ucar.nc2.grib.coord;

import javax.annotation.concurrent.Immutable;

/**
 * Encapsulate the semantics in GRIB level types  (Grib1 table 3, Grib2 code table 4.5).
 */
@Immutable
public class VertCoordType {
  private final int code;
  private final String desc;
  private final String abbrev;
  private final String units;
  private final String datum;
  private final boolean isPositiveUp;
  private final boolean isLayer;

  // LOOK for Grib2Utils - CHANGE THIS
  public VertCoordType(int code, String units, String datum, boolean isPositiveUp) {
    this.code = code;
    this.desc = null;
    this.abbrev = null;
    this.units = units;
    this.datum = datum;
    this.isPositiveUp = isPositiveUp;
    this.isLayer = false;
  }

  public VertCoordType(int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer) {
    this.code = code;
    this.desc = desc;
    this.abbrev = abbrev;
    this.units = units;
    this.datum = datum;
    this.isPositiveUp = isPositiveUp;
    this.isLayer = isLayer;
  }

  public int getCode() {
    return code;
  }

  public String getDesc() {
    return desc;
  }

  public String getAbbrev() {
    return abbrev;
  }

  public String getUnits() {
    return units;
  }

  public String getDatum() {
    return datum;
  }

  public boolean isPositiveUp() {
    return isPositiveUp;
  }

  public boolean isVerticalCoordinate() {
    return getUnits() != null;
  }

  public boolean isLayer() {
    return isLayer;
  }
}

