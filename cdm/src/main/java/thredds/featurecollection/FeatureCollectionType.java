package thredds.featurecollection;

import ucar.nc2.constants.FeatureType;

/**
 * FeatureCollection Types
 *
 * @author caron
 * @since 11/8/12
 */
public enum FeatureCollectionType {
    GRIB, FMRC, Point, Station, Station_Profile;

  public FeatureType getFeatureType() {
    switch (this) {
      case GRIB: return FeatureType.GRID;
      case FMRC: return FeatureType.FMRC;
      case Point: return FeatureType.POINT;
      case Station: return FeatureType.STATION;
      case Station_Profile: return FeatureType.STATION_PROFILE;
    }
    return null;
  }
}
