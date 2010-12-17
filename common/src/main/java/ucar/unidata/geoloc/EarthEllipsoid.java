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
package ucar.unidata.geoloc;

//import net.jcip.annotations.Immutable;

import java.util.Collection;
import java.util.Map;

/**
 * Type-safe enumeration of Earth Ellipsoids. Follows EPSG.
 *
 * @author john caron
 * @see <a href="http://www.epsg.org/">http://www.epsg.org/</a>
 * @see "http://www.epsg.org/"
 */
public final class EarthEllipsoid extends Earth {

  /**
   * Ellipsoid for WGS84 (edavis - correct 1/f as per EPSG database ver 6.14)
   */
  public final static EarthEllipsoid WGS84 = new EarthEllipsoid("WGS84", 7030, 6378137.0, 298.257223563);

  /**
   * Airy 1830 ellipsoid from EPSG database version 6.14.
   */
  public final static EarthEllipsoid Airy1830 = new EarthEllipsoid("Airy 1830", 7001, 6377563.396, 299.3249646);

  private static Map<String, EarthEllipsoid> hash;

  /**
   * get a collection of all defined EarthEllipsoid objects
   *
   * @return all defined EarthEllipsoid objects
   */
  public static Collection<EarthEllipsoid> getAll() {
    if (hash == null) {
      hash = new java.util.LinkedHashMap<String, EarthEllipsoid>(10);
    }
    return hash.values();
  }


  /**
   * Find the EarthEllipsoid that matches this name.
   *
   * @param name : name to match
   * @return EarthEllipsoid or null if no match.
   */
  public static EarthEllipsoid getType(String name) {
    if (name == null) {
      return null;
    }
    if (hash == null) {
      hash = new java.util.LinkedHashMap<String, EarthEllipsoid>(10);
    }
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
  private String name;
  private int epsgId;

  /**
   * Constructor.
   *
   * @param name   EPSG name
   * @param epsgId EPSG id
   * @param a      semimajor (equatorial) radius, in meters.
   * @param invF   inverse flattening.
   */
  private EarthEllipsoid(String name, int epsgId, double a, double invF) {
    super(a, 0, invF);
    this.name = name;
    this.epsgId = epsgId;
    if (hash == null) {
      hash = new java.util.LinkedHashMap<String, EarthEllipsoid>(10);
    }
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

