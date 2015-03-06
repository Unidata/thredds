package thredds.featurecollection;

import ucar.nc2.constants.FeatureType;

/**
 * FeatureCollection Types
 *
 * @author caron
 * @since 11/8/12
 */
public enum FeatureCollectionType {
  GRIB1, GRIB2, GRID, FMRC, Point, Station, Station_Profile;

  public FeatureType getFeatureType() {
    switch (this) {
      case GRIB1: return FeatureType.GRID;
      case GRIB2: return FeatureType.GRID;
      case GRID: return FeatureType.GRID;
      case FMRC: return FeatureType.FMRC;
      case Point: return FeatureType.POINT;
      case Station: return FeatureType.STATION;
      case Station_Profile: return FeatureType.STATION_PROFILE;
    }
    return null;
  }
}
