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
package ucar.nc2.constants;

/**
 * Constants used in CF Conventions.
 *
 * @author caron
 * @since Jan 21, 2009
 */
public class CF {
  public final static String POSITIVE_UP = "up";
  public final static String POSITIVE_DOWN = "down";

  public static final String featureTypeAtt = "CF:featureType";
  public static final String featureTypeAtt2 = "CF-featureType";
  public static final String featureTypeAtt3 = "CFfeatureType";

  // CF names
  public static final String STANDARD_NAME = "standard_name";
  public static final String UNITS = "units";
  public static final String RAGGED_ROWSIZE = "ragged_row_size";
  public static final String RAGGED_PARENTINDEX = "ragged_parent_index";
  public static final String STATION_ID = "station_id";
  public static final String STATION_DESC = "station_desc";
  public static final String STATION_ALTITUDE = "station_altitude";
  public static final String STATION_WMOID = "station_WMO_id";
  public static final String TRAJ_ID = "trajectory_id";
  public static final String PROFILE_ID = "profile_id";
  public static final String SECTION_ID = "section_id";



  /**
   * Start of map from CF feature type names to our FeatureType enums.
   * Unofficial.
   */
  public enum FeatureType {
    point, stationTimeSeries, profile, trajectory, stationProfile, section;

    public static FeatureType convert(ucar.nc2.constants.FeatureType ft) {
      switch (ft) {
        case POINT:
          return CF.FeatureType.point;
        case STATION:
          return CF.FeatureType.stationTimeSeries;
        case PROFILE:
          return CF.FeatureType.profile;
        case TRAJECTORY:
          return CF.FeatureType.trajectory;
        case STATION_PROFILE:
          return CF.FeatureType.stationProfile;
        case SECTION:
          return CF.FeatureType.section;
      }
      return null;
    }
  }
  
}
