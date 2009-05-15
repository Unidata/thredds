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
 * Type-safe enumeration of CDM Feature types, aka "Scientific Data Types".
 *
 * @author john caron
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/FeatureDatasets/Overview.html">CDM Feature Types</a>
 */

public enum FeatureType {
  /** No specific type */
  ANY,
  /** Gridded Data */
  GRID,
  /** Radial data */
  RADIAL,
  /** Swath Data */
  SWATH,
  /** Image data */
  IMAGE,

  /** Any of the point types */
  ANY_POINT,
  /** Point data */
  POINT,
  /** Profile data */
  PROFILE,
  /** Section data */
  SECTION,
  /** Station data */
  STATION,
  /** Stations of profiles */
  STATION_PROFILE,
  /** Trajectory data */
  TRAJECTORY,

  /** experimental */
  STATION_RADIAL,

  /** deprecated - use ANY */
  NONE;

  /**
   * Find the FeatureType that matches this name.
   * @param name find FeatureType with this name.
   * @return FeatureType or null if no match.
   */
  public static FeatureType getType(String name) {
    if (name == null) return null;
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) { // lame!
      return null;
    }
  }

  /**
   * Is this a Point feature type?
   * @return true if this is a Point feature type
   */
  public boolean isPointFeatureType() {
    return (this == FeatureType.POINT) || (this == FeatureType.STATION) || (this == FeatureType.TRAJECTORY) ||
          (this == FeatureType.PROFILE) || (this == FeatureType.STATION_PROFILE) || (this == FeatureType.SECTION);
  }

}
