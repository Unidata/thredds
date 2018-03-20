/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Map;

/**
 * Type-safe enumeration of Earth Ellipsoids. Follows EPSG.
 *
 * @author john caron
 * @see <a href="http://www.epsg.org/">http://www.epsg.org/</a>
 * @see "http://www.epsg.org/"
 */
@Immutable
public class EarthEllipsoid extends Earth {

  // must be done first, as other static fields depend on it
  private static final Map<String, EarthEllipsoid> hash = new java.util.LinkedHashMap<>(10);

  /**
   * Ellipsoid for WGS84 (edavis - correct 1/f as per EPSG database ver 6.14)
   */
  public static final EarthEllipsoid WGS84 = new EarthEllipsoid("WGS84", 7030, 6378137.0, 298.257223563);

  /**
   * Airy 1830 ellipsoid from EPSG database version 6.14.
   */
  public static final EarthEllipsoid Airy1830 = new EarthEllipsoid("Airy 1830", 7001, 6377563.396, 299.3249646);

  // From Grib1: IAU in 1965 (6378.160 km, 6356.775 km, f = 1/297.0)
  // From Grib2: 2 oblate spheroid with size as determined by IAU in 1965 (major axis = 6 378 160.0 m, minor axis = 6 356 775.0 m, f = 1/297.0)
  public static final EarthEllipsoid IAU = new EarthEllipsoid("IAU 1965", -1, 6378160.0, 297.0);

  // From Grib2: 4: oblate spheroid as defined in IAG-GRS80 model (major axis = 6 378 137.0 m, minor axis = 6 356 752.314 m, f = 1/298.257 222 101)
  public static final EarthEllipsoid IAG_GRS80 = new EarthEllipsoid("IIAG-GRS80", -1, 6378137.0, 298.257222101);


  /**
   * get a collection of all defined EarthEllipsoid objects
   *
   * @return all defined EarthEllipsoid objects
   */
  public static Collection<EarthEllipsoid> getAll() {
    return hash.values();
  }


  /**
   * Find the EarthEllipsoid that matches this name.
   *
   * @param name : name to match
   * @return EarthEllipsoid or null if no match.
   */
  public static EarthEllipsoid getType(String name) {
    if (name == null)
      return null;
    return hash.get(name);
  }

  /**
   * Find the EarthEllipsoid that matches this EPSG Id.
   *
   * @param epsgId : epsg Id to match
   * @return EarthEllipsoid or null if no match.
   */
  public static EarthEllipsoid getType(int epsgId) {
    Collection<EarthEllipsoid> all = getAll();
    for (EarthEllipsoid ellipsoid : all) {
      if (ellipsoid.epsgId == epsgId) {
        return ellipsoid;
      }
    }
    return null;
  }

  ///////////////////////////////////////////////////////////
  private final int epsgId;

  /**
   * Constructor.
   *
   * @param name   EPSG name
   * @param epsgId EPSG id
   * @param a      semimajor (equatorial) radius, in meters.
   * @param invF   inverse flattening.
   */
  private EarthEllipsoid(String name, int epsgId, double a, double invF) {
    this(name, epsgId, a, 0, invF);
  }

  /**
   * Constructor.
   *
   * @param name   EPSG name
   * @param epsgId EPSG id
   * @param a      semimajor (equatorial) radius, in meters.
   * @param b      semiminor (polar) radius, in meters.
   * @param invF   inverse flattening.
   */
  public EarthEllipsoid(String name, int epsgId, double a, double b, double invF) {
    super(a, b, invF, name);
    this.epsgId = epsgId;
    hash.put(name, this);
  }


  /**
   * EPSG name
   *
   * @return the EPSG name
   */
  public String getName() {
    return name;
  }

  /**
   * EPSG id
   *
   * @return the EPSG id
   */
  public int getEpsgId() {
    return epsgId;
  }

  /**
   * Same as EPSG name
   *
   * @return the EPSG name
   */
  public String toString() {
    return name;
  }

  /**
   * Override Object.hashCode() to be consistent with this equals.
   *
   * @return hashCode
   */
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * Objects with same name are equal.
   *
   * @param o test this for equals
   * @return +,0,-
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof EarthEllipsoid))
      return false;
    EarthEllipsoid oe = (EarthEllipsoid) o;
    return oe.name.equals(name);
  }
}

